package com.bharathvishal.messagecommunicationusingwearabledatalayer

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.ambient.AmbientModeSupport.AmbientCallback
import com.bharathvishal.messagecommunicationusingwearabledatalayer.databinding.ActivityMainBinding
import com.google.android.gms.wearable.*
import java.nio.charset.StandardCharsets
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.widget.TextView

class MainActivity : AppCompatActivity(), AmbientModeSupport.AmbientCallbackProvider,
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {
    private var activityContext: Context? = null

    private lateinit var binding: ActivityMainBinding

    private val TAG_MESSAGE_RECEIVED = "receive1"
    private val APP_OPEN_WEARABLE_PAYLOAD_PATH = "/APP_OPEN_WEARABLE_PAYLOAD"

    private var mobileDeviceConnected: Boolean = false
    private lateinit var sensorManager: SensorManager
    private lateinit var heartRateSensor: Sensor
    private lateinit var accelerometerSensor: Sensor
    private lateinit var rotationSensor: Sensor

    private var heartRateValue: Float = 0f
    private var velocityValues: FloatArray? = null
    private var rotationValues: FloatArray? = null


    // Payload string items
    private val wearableAppCheckPayloadReturnACK = "AppOpenWearableACK"

    private val MESSAGE_ITEM_RECEIVED_PATH: String = "/message-item-received"


    private var messageEvent: MessageEvent? = null
    private var mobileNodeUri: String? = null

    private lateinit var ambientController: AmbientModeSupport.AmbientController

    // Add these listeners

    private val heartRateListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

        override fun onSensorChanged(event: SensorEvent?) {
            heartRateValue = event?.values?.first() ?: 0f
        }
    }

    private val velocityListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

        override fun onSensorChanged(event: SensorEvent?) {
            velocityValues = event?.values
        }
    }

    private val rotationListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

        override fun onSensorChanged(event: SensorEvent?) {
            rotationValues = event?.values
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val messageClient = Wearable.getMessageClient(this)
        messageClient.addListener(this)


        activityContext = this

        // Enables Always-on
        ambientController = AmbientModeSupport.attach(this)


        //On click listener for sendmessage button
        binding.sendmessageButton.setOnClickListener {
            println("button clicked")
            if (mobileDeviceConnected) {
                if (binding.messagecontentEditText.text!!.isNotEmpty()) {
                    println("message sent")

                    val nodeId: String = messageEvent?.sourceNodeId!!
                    // Set the data of the message to be the bytes of the Uri.
                    val payload: ByteArray =
                        binding.messagecontentEditText.text.toString().toByteArray()

                    // Send the rpc
                    // Instantiates clients without member variables, as clients are inexpensive to
                    // create. (They are cached and shared between GoogleApi instances.)
                    val sendMessageTask =
                        Wearable.getMessageClient(activityContext!!)
                            .sendMessage(nodeId, MESSAGE_ITEM_RECEIVED_PATH, payload)

                    binding.deviceconnectionStatusTv.visibility = View.GONE

                    sendMessageTask.addOnCompleteListener {
                        if (it.isSuccessful) {
                            Log.d("send1", "Message sent successfully")
                            val sbTemp = StringBuilder()
                            sbTemp.append("\n")
                            sbTemp.append(binding.messagecontentEditText.text.toString())
                            sbTemp.append(" (Sent to mobile)")
                            Log.d("receive1", " $sbTemp")
                            binding.messagelogTextView.append(sbTemp)
                            mobileDeviceConnected = true

                            binding.scrollviewTextMessageLog.requestFocus()
                            binding.scrollviewTextMessageLog.post {
                                binding.scrollviewTextMessageLog.fullScroll(ScrollView.FOCUS_DOWN)
                            }
                        } else {
                            Log.d("send1", "Message failed.")
                        }
                    }
                } else {
                    Toast.makeText(
                        activityContext,
                        "Message content is empty. Please enter some message and proceed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        sensorManager.registerListener(heartRateListener, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(velocityListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(rotationListener, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL)

        // Send sensor data every second
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {

                println("1trying to send sensor data")
                if (mobileDeviceConnected && messageEvent != null) {
                    println("2trying to send sensor data")
                    binding.sensorData.text = "hello"
                    val nodeId: String = messageEvent?.sourceNodeId!!
                    if (nodeId != null) {
                        val sensorData = "HeartRate: $heartRateValue, " +
                                "Velocity: ${velocityValues?.joinToString()}, " +
                                "Rotation: ${rotationValues?.joinToString()}"

                        val payload: ByteArray = sensorData.toByteArray()

                        val sendMessageTask =
                            Wearable.getMessageClient(activityContext!!)
                                .sendMessage(nodeId, MESSAGE_ITEM_RECEIVED_PATH, payload)
                        // you can handle the result of the message here...
                    } else {
                        Log.e("send1", "Failed to send sensor data: nodeID is null.")
                    }
                }
                handler.postDelayed(this, 1000) // Re-run every 1 second
            }
        }, 1000)







    }

    override fun onDataChanged(p0: DataEventBuffer) {
    }

    override fun onCapabilityChanged(p0: CapabilityInfo) {
    }


    @SuppressLint("SetTextI18n")
    override fun onMessageReceived(p0: MessageEvent) {
        messageEvent = p0
        mobileNodeUri = p0.sourceNodeId
        try {
            Log.d(TAG_MESSAGE_RECEIVED, "onMessageReceived event received")
            val s1 = String(p0.data, StandardCharsets.UTF_8)
            val messageEventPath: String = p0.path

            Log.d(
                TAG_MESSAGE_RECEIVED,
                "onMessageReceived() A message from watch was received:"
                        + p0.requestId
                        + " "
                        + messageEventPath
                        + " "
                        + s1
            )

            //Send back a message back to the source node
            //This acknowledges that the receiver activity is open
            if (messageEventPath.isNotEmpty() && messageEventPath == APP_OPEN_WEARABLE_PAYLOAD_PATH) {
                try {
                    // Get the node id of the node that created the data item from the host portion of
                    // the uri.
                    val nodeId: String = p0.sourceNodeId.toString()
                    // Set the data of the message to be the bytes of the Uri.
                    val returnPayloadAck = wearableAppCheckPayloadReturnACK
                    val payload: ByteArray = returnPayloadAck.toByteArray()

                    // Send the rpc
                    // Instantiates clients without member variables, as clients are inexpensive to
                    // create. (They are cached and shared between GoogleApi instances.)
                    val sendMessageTask =
                        Wearable.getMessageClient(activityContext!!)
                            .sendMessage(nodeId, APP_OPEN_WEARABLE_PAYLOAD_PATH, payload)

                    Log.d(
                        TAG_MESSAGE_RECEIVED,
                        "Acknowledgement message successfully with payload : $returnPayloadAck"
                    )

                    messageEvent = p0
                    mobileNodeUri = p0.sourceNodeId

                    sendMessageTask.addOnCompleteListener {
                        if (it.isSuccessful) {
                            Log.d(TAG_MESSAGE_RECEIVED, "Message sent successfully")
                            binding.messagelogTextView.visibility = View.VISIBLE

                            val sbTemp = StringBuilder()
                            sbTemp.append("\nMobile device connected.")
                            Log.d("receive1", " $sbTemp")
                            binding.messagelogTextView.append(sbTemp)

                            mobileDeviceConnected = true

                            binding.textInputLayout.visibility = View.VISIBLE
                            binding.sendmessageButton.visibility = View.VISIBLE
                            binding.deviceconnectionStatusTv.visibility = View.VISIBLE
                            binding.deviceconnectionStatusTv.text = "Mobile device is connected"
                        } else {
                            Log.d(TAG_MESSAGE_RECEIVED, "Message failed.")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }//emd of if
            else if (messageEventPath.isNotEmpty() && messageEventPath == MESSAGE_ITEM_RECEIVED_PATH) {
                try {
                    binding.messagelogTextView.visibility = View.VISIBLE
                    binding.textInputLayout.visibility = View.VISIBLE
                    binding.sendmessageButton.visibility = View.VISIBLE
                    binding.deviceconnectionStatusTv.visibility = View.GONE

                    val sbTemp = StringBuilder()
                    sbTemp.append("\n")
                    sbTemp.append(s1)
                    sbTemp.append(" - (Received from mobile)")
                    Log.d("receive1", " $sbTemp")
                    binding.messagelogTextView.append(sbTemp)
                    mobileDeviceConnected = true


                    binding.scrollviewTextMessageLog.requestFocus()
                    binding.scrollviewTextMessageLog.post {
                        binding.scrollviewTextMessageLog.fullScroll(ScrollView.FOCUS_DOWN)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onPause() {
        super.onPause()
        try {
            Wearable.getDataClient(activityContext!!).removeListener(this)
            Wearable.getMessageClient(activityContext!!).removeListener(this)
            Wearable.getCapabilityClient(activityContext!!).removeListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onResume() {
        super.onResume()
        try {
            Wearable.getDataClient(activityContext!!).addListener(this)
            Wearable.getMessageClient(activityContext!!).addListener(this)
            Wearable.getCapabilityClient(activityContext!!)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getAmbientCallback(): AmbientCallback = MyAmbientCallback()

    private inner class MyAmbientCallback : AmbientCallback() {
        override fun onEnterAmbient(ambientDetails: Bundle) {
            super.onEnterAmbient(ambientDetails)
        }

        override fun onUpdateAmbient() {
            super.onUpdateAmbient()
        }

        override fun onExitAmbient() {
            super.onExitAmbient()
        }
    }

}
