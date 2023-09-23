package com.mujugen.mypersonaltrainer

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.ambient.AmbientModeSupport.AmbientCallback
import com.mujugen.mypersonaltrainer.databinding.ActivityMainBinding
import com.google.android.gms.wearable.*
import java.nio.charset.StandardCharsets
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import java.text.DecimalFormat
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity(), AmbientModeSupport.AmbientCallbackProvider,
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {
    private var activityContext: Context? = null

    private var exerciseStarted = false

    private val handler = Handler(Looper.getMainLooper())
    private val dataSenderRunnable = object : Runnable {
        override fun run() {
            println("sent sensor data")
            if (exerciseStarted) {
                val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                val formattedDate = sdf.format(Date())
                val sensorData = "DateTime: $formattedDate, " +
                        "HeartRate: $heartRateValue, " +
                        "Velocity: ${velocityValues?.joinToString()}, " +
                        "Rotation: ${gyroValues?.joinToString()}"
                sendMessageToSmartphone(sensorData)

            }
            if (exerciseStarted) {
                handler.postDelayed(this, 10)
            }
        }
    }

    private lateinit var messageClient: MessageClient
    private lateinit var binding: ActivityMainBinding
    private var connectedNode: Node? = null
    private lateinit var sensorManager: SensorManager
    private lateinit var heartRateSensor: Sensor
    private lateinit var accelerometerSensor: Sensor
    private lateinit var gyroSensor: Sensor

    private var connectionStatus = false
    private var heartRateValue: Float = 0f
    private var velocityValues: FloatArray? = null
    private var gyroValues: FloatArray? = null

    private lateinit var ambientController: AmbientModeSupport.AmbientController

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

    private val gyroListener: SensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

        override fun onSensorChanged(event: SensorEvent?) {
            gyroValues = event?.values
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        messageClient = Wearable.getMessageClient(this)
        messageClient.addListener(this)

        connectToSmartphone()
        activityContext = this

        // Enables Always-on
        ambientController = AmbientModeSupport.attach(this)



        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        sensorManager.registerListener(heartRateListener, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(velocityListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(gyroListener, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL)

        // Set click listener for start button
        binding.startBtn.setOnClickListener {
            binding.startPage.visibility = View.GONE
            binding.connectPage.visibility = View.VISIBLE
        }


        binding.goBtn.setOnClickListener{
            if (!exerciseStarted) {
                sendMessageToSmartphone("Go")
                exerciseStarted = true
                startSendingData()
                binding.goBtn.text = "Stop"
            } else {
                sendMessageToSmartphone("Stop")
                exerciseStarted = false
                stopSendingData()
                binding.goBtn.text = "Start"
            }
        }



    }

    private fun startSendingData() {
        // Post the initial runnable
        handler.post(dataSenderRunnable)
    }

    private fun stopSendingData() {
        // Remove the dataSenderRunnable if exercise is stopped
        handler.removeCallbacks(dataSenderRunnable)
    }
    override fun onDataChanged(p0: DataEventBuffer) {
    }

    override fun onCapabilityChanged(p0: CapabilityInfo) {
    }


    @SuppressLint("SetTextI18n")
    override fun onMessageReceived(messageEvent: MessageEvent) {
        val message = String(messageEvent.data)
        println("message $message")
        if(message == "Connect"){
            println("Received connection request")
            connectionStatus = true
            binding.connectPage.visibility = View.GONE
            binding.mainPage.visibility = View.VISIBLE
        }
        if(message == "Go"){
            if(!exerciseStarted){
                binding.goBtn.performClick()
            }
        }
        if(message == "Stop"){
            if(exerciseStarted){
                binding.goBtn.performClick()
            }
        }
    }

    private fun connectToSmartphone() {
        // Check if there are already connected nodes (smartphones)
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isNotEmpty()) {
                // Use the first connected node for simplicity, or choose the desired one
                connectedNode = nodes[0]
                connectionStatus = true
            }
        }
    }

    private fun sendMessageToSmartphone(message: String) {
        connectedNode?.let { node ->
            // Build the message
            val byteMessage = message.toByteArray()
            // Send the message to the connected node (smartphone)
            messageClient.sendMessage(node.id, "/message_path", byteMessage)
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

    override fun onDestroy() {
        messageClient.removeListener(this)
        super.onDestroy()
    }


}
