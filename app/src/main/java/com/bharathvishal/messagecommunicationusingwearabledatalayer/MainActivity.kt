package com.bharathvishal.messagecommunicationusingwearabledatalayer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bharathvishal.messagecommunicationusingwearabledatalayer.databinding.ActivityMainBinding
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.random.Random

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {
    var activityContext: Context? = null
    private val wearableAppCheckPayload = "AppOpenWearable"
    private val wearableAppCheckPayloadReturnACK = "AppOpenWearableACK"
    private var wearableDeviceConnected: Boolean = false

    private var currentAckFromWearForAppOpenCheck: String? = null
    private val APP_OPEN_WEARABLE_PAYLOAD_PATH = "/APP_OPEN_WEARABLE_PAYLOAD"

    private val MESSAGE_ITEM_RECEIVED_PATH: String = "/message-item-received"

    private val TAG_GET_NODES: String = "getnodes1"
    private val TAG_MESSAGE_RECEIVED: String = "receive1"

    private var messageEvent: MessageEvent? = null
    private var wearableNodeUri: String? = null

    private var exerciseSelected: String = "None"

    private var currentLoad: Int = 20
    private var currentReps: Int = 10
    private var numberOfSets: Int = 4
    private lateinit var fadeInAnimation: Animation
    private lateinit var fadeOutAnimation: Animation
    private lateinit var binding: ActivityMainBinding
    private var exerciseStarted: Boolean = false
    private var currentSet: Int = 1

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        activityContext = this
        wearableDeviceConnected = false

        binding.startBtn.setOnClickListener {
            binding.startPage.startAnimation(fadeOutAnimation)
            binding.startPage.visibility = View.GONE
            binding.connectPage.startAnimation(fadeInAnimation)
            binding.connectPage.visibility = View.VISIBLE
        }
        binding.connectBtn.setOnClickListener {
            if (!wearableDeviceConnected) {
                val tempAct: Activity = activityContext as MainActivity
                //Couroutine
                initialiseDevicePairing(tempAct)
            }else{
                binding.connectPage.startAnimation(fadeOutAnimation)
                binding.connectPage.visibility = View.GONE
                binding.exerciseSelectionPage.startAnimation(fadeInAnimation)
                binding.exerciseSelectionPage.visibility = View.VISIBLE

            }
        }


        setButtonListeners(binding.exerciseLayout)

        binding.plusLoadBtn.setOnClickListener {
            currentLoad += 1
            binding.loadText.text = "$currentLoad kg"
            binding.load2Text.text = "$currentLoad kg"
        }
        binding.plusSetsBtn.setOnClickListener {
            numberOfSets += 1
            binding.setsText.text = "$numberOfSets"
            binding.sets2Text.text = "$numberOfSets"
        }
        binding.plusRepsBtn.setOnClickListener {
            currentReps += 1
            binding.repsText.text = "$currentReps"
            binding.reps2Text.text = "$currentReps"
        }
        binding.minusLoadBtn.setOnClickListener {
            if(currentLoad>1){
                currentLoad -= 1
            }
            binding.loadText.text = "$currentLoad kg"
            binding.load2Text.text = "$currentLoad kg"
        }
        binding.minusSetsBtn.setOnClickListener {
            if(numberOfSets>1){
                numberOfSets -= 1
            }
            binding.setsText.text = "$numberOfSets"
            binding.sets2Text.text = "$numberOfSets"
        }
        binding.minusRepsBtn.setOnClickListener {
            if(currentReps>1) {
                currentReps -= 1
            }
            binding.repsText.text = "$currentReps"
            binding.reps2Text.text = "$currentReps"
        }
        binding.proceedBtn.setOnClickListener {
            binding.exerciseSettingsPage.startAnimation(fadeOutAnimation)
            binding.exerciseSettingsPage.visibility = View.GONE
            binding.exercisePage.startAnimation(fadeInAnimation)
            binding.exercisePage.visibility = View.VISIBLE
        }

        binding.finishBtn.setOnClickListener {
            exerciseStarted = false
            binding.goBtn.setBackgroundResource(R.drawable.circle_button)
            binding.exercisePage.startAnimation(fadeOutAnimation)
            binding.exercisePage.visibility = View.GONE
            binding.summaryPage.startAnimation(fadeInAnimation)
            binding.summaryPage.visibility = View.VISIBLE
        }
        binding.backBtn.setOnClickListener {
            binding.exercisePage.startAnimation(fadeOutAnimation)
            binding.exercisePage.visibility = View.GONE
            binding.exerciseSelectionPage.startAnimation(fadeInAnimation)
            binding.exerciseSelectionPage.visibility = View.VISIBLE
        }
        binding.finishBtn2.setOnClickListener {
            binding.summaryPage.startAnimation(fadeOutAnimation)
            binding.summaryPage.visibility = View.GONE
            binding.exerciseSelectionPage.startAnimation(fadeInAnimation)
            binding.exerciseSelectionPage.visibility = View.VISIBLE
        }
        binding.backBtn2.setOnClickListener {
            binding.summaryPage.startAnimation(fadeOutAnimation)
            binding.summaryPage.visibility = View.GONE
            binding.exercisePage.startAnimation(fadeInAnimation)
            binding.exercisePage.visibility = View.VISIBLE
        }

        binding.goBtn.setOnClickListener {
            if(exerciseStarted==false){
                binding.goBtn.setBackgroundResource(R.drawable.red_circle_button)
                exerciseStarted = true
                binding.suggestedRepsText.text = "Suggested Reps:"
                binding.suggestedLoadText.text = "Suggested Load:"
                binding.predictedRPEText.text = "Predicted RPE:"
                binding.reps2Text.text = "Reps: $currentReps"
                binding.load2Text.text = "Load: $currentLoad kg"
                binding.goBtn.text = "STOP"
            }else{
                binding.goBtn.setBackgroundResource(R.drawable.circle_button)
                exerciseStarted = false
                currentSet += 1
                binding.setNumber.text = "Set $currentSet"
                val RPE = Random.nextInt(1, 11)
                binding.predictedRPEText.text = "Predicted RPE: $RPE"
                currentLoad = calculateLoad(RPE)
                currentReps = calculateReps(RPE)

                binding.suggestedRepsText.text = "Suggested Reps: $currentReps"
                binding.suggestedLoadText.text = "Suggested Load: $currentLoad kg"
                binding.goBtn.text = "START"
            }


        }




//        binding.sendmessageButton.setOnClickListener {
//            if (wearableDeviceConnected) {
//                if (binding.messagecontentEditText.text!!.isNotEmpty()) {
//
//                    val nodeId: String = messageEvent?.sourceNodeId!!
//                    // Set the data of the message to be the bytes of the Uri.
//                    val payload: ByteArray =
//                        binding.messagecontentEditText.text.toString().toByteArray()
//
//                    // Send the rpc
//                    // Instantiates clients without member variables, as clients are inexpensive to
//                    // create. (They are cached and shared between GoogleApi instances.)
//                    val sendMessageTask =
//                        Wearable.getMessageClient(activityContext!!)
//                            .sendMessage(nodeId, MESSAGE_ITEM_RECEIVED_PATH, payload)
//
//                    sendMessageTask.addOnCompleteListener {
//                        if (it.isSuccessful) {
//                            Log.d("send1", "Message sent successfully")
//                            val sbTemp = StringBuilder()
//                            sbTemp.append("\n")
//                            sbTemp.append(binding.messagecontentEditText.text.toString())
//                            sbTemp.append(" (Sent to Wearable)")
//                            Log.d("receive1", " $sbTemp")
//                            binding.messagelogTextView.append(sbTemp)
//
//                            binding.scrollviewText.requestFocus()
//                            binding.scrollviewText.post {
//                                binding.scrollviewText.scrollTo(0, binding.scrollviewText.bottom)
//                            }
//                        } else {
//                            Log.d("send1", "Message failed.")
//                        }
//                    }
//                } else {
//                    Toast.makeText(
//                        activityContext,
//                        "Message content is empty. Please enter some message and proceed",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
    }
    private fun setButtonListeners(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)

            if (child is ViewGroup) {
                setButtonListeners(child)
            } else if (child is Button) {
                child.setOnClickListener {
                    // Handle button click here
                    val buttonText = (child as Button).text.toString()
                    exerciseSelected = buttonText
                    println("exercise selected: ${exerciseSelected}")
                    binding.exerciseType.text = exerciseSelected
                    binding.exerciseText.text = exerciseSelected
                    binding.repsText.text = "$currentReps"
                    binding.loadText.text = "$currentLoad kg"
                    binding.setsText.text = "$numberOfSets"
                    binding.reps2Text.text = "Reps: $currentReps"
                    binding.load2Text.text = "Load: $currentLoad kg"
                    binding.sets2Text.text = "Sets: $numberOfSets"
                    binding.exerciseSelectionPage.startAnimation(fadeOutAnimation)
                    binding.exerciseSelectionPage.visibility = View.GONE
                    binding.exerciseSettingsPage.startAnimation(fadeInAnimation)
                    binding.exerciseSettingsPage.visibility = View.VISIBLE
                }
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun initialiseDevicePairing(tempAct: Activity) {
        //Coroutine
        launch(Dispatchers.Default) {
            var getNodesResBool: BooleanArray? = null

            try {
                getNodesResBool =
                    getNodes(tempAct.applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            //UI Thread
            withContext(Dispatchers.Main) {
                if (getNodesResBool!![0]) {
                    //if message Acknowlegement Received
                    if (getNodesResBool[1]) {
                        Toast.makeText(
                            activityContext,
                            "Wearable device paired and app is open",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.connectionStatusText.text =
                            "Status: Connected"

                        binding.connectBtn.text = "Next"
                        wearableDeviceConnected = true
                    } else {
                        Toast.makeText(
                            activityContext,
                            "A wearable device is paired but the wearable app on your watch isn't open.",
                            Toast.LENGTH_LONG
                        ).show()
                        wearableDeviceConnected = false
                    }
                } else {
                    Toast.makeText(
                        activityContext,
                        "No wearable device paired.",
                        Toast.LENGTH_LONG
                    ).show()
                    wearableDeviceConnected = false
                }
            }
        }
    }


    private fun getNodes(context: Context): BooleanArray {
        val nodeResults = HashSet<String>()
        val resBool = BooleanArray(2)
        resBool[0] = false //nodePresent
        resBool[1] = false //wearableReturnAckReceived
        val nodeListTask =
            Wearable.getNodeClient(context).connectedNodes
        try {
            // Block on a task and get the result synchronously (because this is on a background thread).
            val nodes =
                Tasks.await(
                    nodeListTask
                )
            Log.e(TAG_GET_NODES, "Task fetched nodes")
            for (node in nodes) {
                Log.e(TAG_GET_NODES, "inside loop")
                nodeResults.add(node.id)
                try {
                    val nodeId = node.id
                    // Set the data of the message to be the bytes of the Uri.
                    val payload: ByteArray = wearableAppCheckPayload.toByteArray()
                    // Send the rpc
                    // Instantiates clients without member variables, as clients are inexpensive to
                    // create. (They are cached and shared between GoogleApi instances.)
                    val sendMessageTask =
                        Wearable.getMessageClient(context)
                            .sendMessage(nodeId, APP_OPEN_WEARABLE_PAYLOAD_PATH, payload)
                    try {
                        // Block on a task and get the result synchronously (because this is on a background thread).
                        val result = Tasks.await(sendMessageTask)
                        Log.d(TAG_GET_NODES, "send message result : $result")
                        resBool[0] = true

                        //Wait for 700 ms/0.7 sec for the acknowledgement message
                        //Wait 1
                        if (currentAckFromWearForAppOpenCheck != wearableAppCheckPayloadReturnACK) {
                            Thread.sleep(100)
                            Log.d(TAG_GET_NODES, "ACK thread sleep 1")
                        }
                        if (currentAckFromWearForAppOpenCheck == wearableAppCheckPayloadReturnACK) {
                            resBool[1] = true
                            return resBool
                        }
                        //Wait 2
                        if (currentAckFromWearForAppOpenCheck != wearableAppCheckPayloadReturnACK) {
                            Thread.sleep(250)
                            Log.d(TAG_GET_NODES, "ACK thread sleep 2")
                        }
                        if (currentAckFromWearForAppOpenCheck == wearableAppCheckPayloadReturnACK) {
                            resBool[1] = true
                            return resBool
                        }
                        //Wait 3
                        if (currentAckFromWearForAppOpenCheck != wearableAppCheckPayloadReturnACK) {
                            Thread.sleep(350)
                            Log.d(TAG_GET_NODES, "ACK thread sleep 5")
                        }
                        if (currentAckFromWearForAppOpenCheck == wearableAppCheckPayloadReturnACK) {
                            resBool[1] = true
                            return resBool
                        }
                        resBool[1] = false
                        Log.d(
                            TAG_GET_NODES,
                            "ACK thread timeout, no message received from the wearable "
                        )
                    } catch (exception: Exception) {
                        exception.printStackTrace()
                    }
                } catch (e1: Exception) {
                    Log.d(TAG_GET_NODES, "send message exception")
                    e1.printStackTrace()
                }
            } //end of for loop
        } catch (exception: Exception) {
            Log.e(TAG_GET_NODES, "Task failed: $exception")
            exception.printStackTrace()
        }
        return resBool
    }


    override fun onDataChanged(p0: DataEventBuffer) {
    }

    @SuppressLint("SetTextI18n")
    override fun onMessageReceived(p0: MessageEvent) {
        println("message received")
        try {
            val s = String(p0.data, StandardCharsets.UTF_8)
            val messageEventPath: String = p0.path
            Log.d(TAG_MESSAGE_RECEIVED, "onMessageReceived() Received a message from watch:"
                    + p0.requestId
                    + " "
                    + messageEventPath
                    + " "
                    + s
            )
            if (messageEventPath == APP_OPEN_WEARABLE_PAYLOAD_PATH) {
                currentAckFromWearForAppOpenCheck = s
                Log.d(TAG_MESSAGE_RECEIVED, "Received acknowledgement message that app is open in wear")

                val sbTemp = StringBuilder()
                Log.d("receive1", " $sbTemp")
                messageEvent = p0
                wearableNodeUri = p0.sourceNodeId
            } else if (messageEventPath.isNotEmpty() && messageEventPath == MESSAGE_ITEM_RECEIVED_PATH) {

                try {

                    val sensorDataParts = s.split("HeartRate:", "Velocity:", "Rotation:")
                    val heartRate = sensorDataParts[1].trim().removeSuffix(",")
                    val velocity = sensorDataParts[2].trim().split(",").map { it.trim() }
                    val rotation = sensorDataParts[3].trim().split(",").map { it.trim() }


                    println(sensorDataParts)
                    println(heartRate)
                    println(velocity)
                    println(rotation)
                    val velocityX = String.format("%.2f",velocity[0].toDouble())
                    val velocityY = String.format("%.2f",velocity[1].toDouble())
                    val velocityZ = String.format("%.2f",velocity[2].toDouble())
                    val rotationX = String.format("%.2f",rotation[0].toDouble())
                    val rotationY = String.format("%.2f",rotation[1].toDouble())
                    val rotationZ = String.format("%.2f",rotation[2].toDouble())

                    Log.d("receive1", "HeartRate: $heartRate, Velocity: X=$velocityX, Y=$velocityY, Z=$velocityZ, Rotation: X=$rotationX, Y=$rotationY, Z=$rotationZ")

                    binding.hrText.text = "Heart Rate: $heartRate"
                    binding.velocityText.text = "Velocity: X=$velocityX, Y=$velocityY, Z=$velocityZ"
                    binding.rotationText.text = "Rotation: X=$rotationX, Y=$rotationY, Z=$rotationZ"

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("receive1", "Handled")
        }
    }


    override fun onCapabilityChanged(p0: CapabilityInfo) {
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

    private fun calculateLoad(RPE: Int): Int{
        val targetRPE = 7
        val RPEDifference = targetRPE - RPE
        val suggestedLoad = (((RPEDifference*0.04)+1)*currentLoad).toInt()

        return suggestedLoad
    }
    private fun calculateReps(RPE: Int): Int{
        val targetRPE = 7
        val RPEDifference = (targetRPE - RPE).coerceAtMost(4)
        if (currentReps > 15){
            if(RPEDifference < 0 ){
                val suggestedReps = currentReps + RPEDifference
                return suggestedReps
            }else{
                return currentReps
            }
        }else{
            val suggestedReps = currentReps + RPEDifference
            return suggestedReps
        }
    }
}


