package com.mujugen.mypersonaltrainer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mujugen.mypersonaltrainer.databinding.ActivityMainBinding
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.math.roundToInt
import kotlin.random.Random
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import android.app.AlertDialog
import java.time.LocalDateTime
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
class MainActivity : AppCompatActivity(), CoroutineScope by MainScope(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {
    var activityContext: Context? = null
    private val wearableAppCheckPayload = "AppOpenWearable"
    private val wearableAppCheckPayloadReturnACK = "AppOpenWearableACK"
    private var wearableDeviceConnected: Boolean = false
    private var setsList: ArrayList<Sets> = ArrayList()



    private var currentAckFromWearForAppOpenCheck: String? = null
    private val APP_OPEN_WEARABLE_PAYLOAD_PATH = "/APP_OPEN_WEARABLE_PAYLOAD"

    private val MESSAGE_ITEM_RECEIVED_PATH: String = "/message-item-received"

    private val TAG_GET_NODES: String = "getnodes1"
    private val TAG_MESSAGE_RECEIVED: String = "receive1"

    private var messageEvent: MessageEvent? = null
    private var wearableNodeUri: String? = null

    private var exerciseSelected: String = "None"

    private lateinit var fadeInAnimation: Animation
    private lateinit var fadeOutAnimation: Animation
    private lateinit var binding: ActivityMainBinding
    private var exerciseStarted: Boolean = false
    private var currentSet: Int = 1
    private var uniqueIdentifier = 0
    private val heartRateArray = mutableListOf<String>()
    private val velocityXArray = mutableListOf<String>()
    private val velocityYArray = mutableListOf<String>()
    private val velocityZArray = mutableListOf<String>()
    private val rotationXArray = mutableListOf<String>()
    private val rotationYArray = mutableListOf<String>()
    private val rotationZArray = mutableListOf<String>()
    private var sensorData: String = ""
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
            } else {
                binding.connectPage.startAnimation(fadeOutAnimation)
                binding.connectPage.visibility = View.GONE
                binding.exerciseSelectionPage.startAnimation(fadeInAnimation)
                binding.exerciseSelectionPage.visibility = View.VISIBLE

            }
        }


        setButtonListeners(binding.exerciseLayout)

        binding.proceedBtn.setOnClickListener {
            binding.exerciseSettingsPage.startAnimation(fadeOutAnimation)
            binding.exerciseSettingsPage.visibility = View.GONE
            binding.exercisePage.startAnimation(fadeInAnimation)
            binding.exercisePage.visibility = View.VISIBLE
            binding.exerciseTypeText.text = "$exerciseSelected"
            val currentDateTime: Calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val currentDateTimeString: String = dateFormat.format(currentDateTime.time)
            // sensorData = "${currentSet-1}, $heartRateCSV, $velocityXCSV, $velocityYCSV, $velocityZCSV, $rotationXCSV, $rotationYCSV, $rotationZCSV"
            val currentLoad = binding.loadInput.text
            val currentReps = binding.repsInput.text
            val name = binding.nameInput.text
            val sex = binding.sexInput.text
            val yearsTrained = binding.yearsTrainedInput.text
            val age = binding.ageInput.text
            val dataToSave = "$sensorData, $currentDateTimeString, $exerciseSelected, $currentLoad, $currentReps, $name, $sex, $yearsTrained, $age"
            println("dataToSave = $dataToSave")
            saveToFile(dataToSave)


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
            if (exerciseStarted == false) {
                uniqueIdentifier += 1
                binding.goBtn.setBackgroundResource(R.drawable.red_circle_button)
                exerciseStarted = true
                binding.goBtn.text = "STOP"
                binding.ExerciseSettingsSetNumber.text = "Set $currentSet"
            } else {
                binding.goBtn.setBackgroundResource(R.drawable.circle_button)
                exerciseStarted = false
                currentSet += 1
                binding.setNumber.text = "Set $currentSet"
                val heartRateCSV = heartRateArray.joinToString(" - ") { it.toString() }
                val velocityXCSV = velocityXArray.joinToString(" - ") { it.toString() }
                val velocityYCSV = velocityYArray.joinToString(" - ") { it.toString() }
                val velocityZCSV = velocityZArray.joinToString(" - ") { it.toString() }
                val rotationXCSV = rotationXArray.joinToString(" - ") { it.toString() }
                val rotationYCSV = rotationYArray.joinToString(" - ") { it.toString() }
                val rotationZCSV = rotationZArray.joinToString(" - ") { it.toString() }
                sensorData = "${currentSet-1}, $heartRateCSV, $velocityXCSV, $velocityYCSV, $velocityZCSV, $rotationXCSV, $rotationYCSV, $rotationZCSV"

                // Clearing the arrays for the next set
                heartRateArray.clear()
                velocityXArray.clear()
                velocityYArray.clear()
                velocityZArray.clear()
                rotationXArray.clear()
                rotationYArray.clear()
                rotationZArray.clear()
                binding.goBtn.text = "START"
                exerciseStarted = false
                binding.goBtn.setBackgroundResource(R.drawable.circle_button)
                binding.exercisePage.startAnimation(fadeOutAnimation)
                binding.exercisePage.visibility = View.GONE
                binding.exerciseSettingsPage.startAnimation(fadeInAnimation)
                binding.exerciseSettingsPage.visibility = View.VISIBLE



            }


        }


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
                    binding.exerciseSelectionPage.startAnimation(fadeOutAnimation)
                    binding.exerciseSelectionPage.visibility = View.GONE
                    binding.exercisePage.startAnimation(fadeInAnimation)
                    binding.exercisePage.visibility = View.VISIBLE
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
            //Log.e(TAG_GET_NODES, "Task fetched nodes")
            for (node in nodes) {
                //Log.e(TAG_GET_NODES, "inside loop")
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
                        //Log.d(TAG_GET_NODES, "send message result : $result")
                        resBool[0] = true

                        //Wait for 700 ms/0.7 sec for the acknowledgement message
                        //Wait 1
                        if (currentAckFromWearForAppOpenCheck != wearableAppCheckPayloadReturnACK) {
                            Thread.sleep(100)
                            //Log.d(TAG_GET_NODES, "ACK thread sleep 1")
                        }
                        if (currentAckFromWearForAppOpenCheck == wearableAppCheckPayloadReturnACK) {
                            resBool[1] = true
                            return resBool
                        }
                        //Wait 2
                        if (currentAckFromWearForAppOpenCheck != wearableAppCheckPayloadReturnACK) {
                            Thread.sleep(250)
                            //Log.d(TAG_GET_NODES, "ACK thread sleep 2")
                        }
                        if (currentAckFromWearForAppOpenCheck == wearableAppCheckPayloadReturnACK) {
                            resBool[1] = true
                            return resBool
                        }
                        //Wait 3
                        if (currentAckFromWearForAppOpenCheck != wearableAppCheckPayloadReturnACK) {
                            Thread.sleep(350)
                            //Log.d(TAG_GET_NODES, "ACK thread sleep 5")
                        }
                        if (currentAckFromWearForAppOpenCheck == wearableAppCheckPayloadReturnACK) {
                            resBool[1] = true
                            return resBool
                        }
                        resBool[1] = false
                        //Log.d(
                        //    TAG_GET_NODES,
                        //    "ACK thread timeout, no message received from the wearable "
                        //)
                    } catch (exception: Exception) {
                        exception.printStackTrace()
                    }
                } catch (e1: Exception) {
                    //Log.d(TAG_GET_NODES, "send message exception")
                    e1.printStackTrace()
                }
            } //end of for loop
        } catch (exception: Exception) {
            //Log.e(TAG_GET_NODES, "Task failed: $exception")
            exception.printStackTrace()
        }
        return resBool
    }


    override fun onDataChanged(p0: DataEventBuffer) {
    }

    @SuppressLint("SetTextI18n")
    override fun onMessageReceived(p0: MessageEvent) {
        //println("message received")
        try {
            val s = String(p0.data, StandardCharsets.UTF_8)
            val messageEventPath: String = p0.path
            if (messageEventPath == APP_OPEN_WEARABLE_PAYLOAD_PATH) {
                currentAckFromWearForAppOpenCheck = s
                //Log.d(TAG_MESSAGE_RECEIVED, "Received acknowledgement message that app is open in wear")

                val sbTemp = StringBuilder()
                //Log.d("receive1", " $sbTemp")
                messageEvent = p0
                wearableNodeUri = p0.sourceNodeId
            } else if (messageEventPath.isNotEmpty() && messageEventPath == MESSAGE_ITEM_RECEIVED_PATH) {

                try {

                    val sensorDataParts = s.split("HeartRate:", "Velocity:", "Rotation:")
                    val heartRate = sensorDataParts[1].trim().removeSuffix(",")
                    val velocity = sensorDataParts[2].trim().split(",").map { it.trim() }
                    val rotation = sensorDataParts[3].trim().split(",").map { it.trim() }


                    val velocityX = String.format("%.2f", velocity[0].toDouble())
                    val velocityY = String.format("%.2f", velocity[1].toDouble())
                    val velocityZ = String.format("%.2f", velocity[2].toDouble())
                    val rotationX = String.format("%.2f", rotation[0].toDouble())
                    val rotationY = String.format("%.2f", rotation[1].toDouble())
                    val rotationZ = String.format("%.2f", rotation[2].toDouble())


                    if(exerciseStarted == true) {
                        heartRateArray.add(heartRate)
                        velocityXArray.add(velocityX)
                        velocityYArray.add(velocityY)
                        velocityZArray.add(velocityZ)
                        rotationXArray.add(rotationX)
                        rotationYArray.add(rotationY)
                        rotationZArray.add(rotationZ)
                    }


                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            //Log.d("receive1", "Handled")
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


    private data class Sets(
        var exercise: String = "",
        var set: Int = 0,
        var load: Int = 0,
        var reps: Int = 0,
        var rpe: Int = 0,
        var sLoad: Int = 0,
        var sReps: Int = 0
    )

    //private fun addSetsToLayout() {
    //    val parentLayout = binding.setsContainer
    //
    //    sets.forEachIndexed { index, set ->
    //        // Create new LinearLayout
    //        val newLayout = LinearLayout(this).apply {
    //            orientation = LinearLayout.VERTICAL
    //            setBackgroundResource(R.drawable.square_button)
    //            layoutParams = LinearLayout.LayoutParams(
    //                LinearLayout.LayoutParams.MATCH_PARENT,
    //                LinearLayout.LayoutParams.WRAP_CONTENT
    //            )
    //        }
        //
    //        // Create new TextViews
    //        val setNumberTextView = createTextView("Set ${index + 1}", 24f, 20, Typeface.BOLD)
    //        val loadTextView = createTextView("Load: ${set.load}", 18f)
    //        val repsTextView = createTextView("Reps: ${set.reps}", 18f)
    //        val rpeTextView = createTextView("RPE: ${set.rpe}", 18f)
    //        val suggestedLoadTextView = createTextView("Suggested Load: ${set.sLoad}", 18f)
    //        val suggestedRepsTextView =
    //            createTextView("Suggested Reps: ${set.sReps}", 18f, 0, Typeface.NORMAL, 20)
        //
    //        // Add TextViews to the new LinearLayout
    //        newLayout.apply {
    //            addView(setNumberTextView)
    //            addView(loadTextView)
    //            addView(repsTextView)
    //            addView(rpeTextView)
    //            addView(suggestedLoadTextView)
    //            addView(suggestedRepsTextView)
    //        }
    //
    //        // Add new LinearLayout to parent LinearLayout
    //        parentLayout.addView(newLayout)
    //    }
    //}

    private fun createTextView(
        text: String,
        textSize: Float,
        marginTop: Int = 0,
        textStyle: Int = Typeface.NORMAL,
        marginBottom: Int = 0
    ): TextView {
        val marginLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(dpToPx(30), dpToPx(marginTop), 0, dpToPx(marginBottom))
        }

        return TextView(this).apply {
            this.text = text
            this.textSize = textSize
            this.typeface = Typeface.create("@font/miriam_libre", textStyle)
            setTextColor(Color.BLACK)
            ellipsize = TextUtils.TruncateAt.END
            maxLines = 1
            layoutParams = marginLayoutParams
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).roundToInt()
    }

    private fun saveToFile(data: String) {
        val externalFileDirs = getExternalFilesDirs(null)
        val sdCardPath = externalFileDirs.find { file ->
            Environment.isExternalStorageRemovable(file)
        }?.absolutePath

        if (sdCardPath != null) {
            val path = "$sdCardPath/data5.csv"
            val file = File(path)

            try {
                if (!file.exists()) {
                    file.createNewFile()
                    FileWriter(file, true).use { writer ->
                        BufferedWriter(writer).use { bufferedWriter ->
                            bufferedWriter.write("Set, HeartRate, VelocityX, VelocityY, VelocityZ, RotationX, RotationY, RotationZ, Id, Exercise Selected, Load, Reps, Name, Sex, Years Trained, Age\n")
                        }
                    }
                }
                println("Saving file")
                FileWriter(file, true).use { writer ->
                    BufferedWriter(writer).use { bufferedWriter ->
                        bufferedWriter.write(data)
                    }
                }
            } catch (e: IOException) {
                println("Error encountered")
                e.printStackTrace()
            }
        } else {
            println("SD card not found")
        }
    }




}



