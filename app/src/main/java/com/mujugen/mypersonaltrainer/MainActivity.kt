package com.mujugen.mypersonaltrainer

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import com.mujugen.mypersonaltrainer.databinding.ActivityMainBinding
import com.robinhood.spark.SparkAdapter
import com.robinhood.spark.SparkView
import kotlinx.coroutines.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import java.util.ArrayDeque
import java.text.DecimalFormat

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {
    var activityContext: Context? = null
    private val wearableAppCheckPayload = "AppOpenWearable"
    private val wearableAppCheckPayloadReturnACK = "AppOpenWearableACK"
    private var wearableDeviceConnected: Boolean = false
    private var setsList: ArrayList<Sets> = ArrayList()

    private var isGoBtnClickable = true


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
    private val heartRateArray = MaxSizeArrayLarge<String>()
    private val velocityXArray = MaxSizeArrayLarge<String>()
    private val velocityYArray = MaxSizeArrayLarge<String>()
    private val velocityZArray = MaxSizeArrayLarge<String>()
    private val rotationXArray = MaxSizeArrayLarge<String>()
    private val rotationYArray = MaxSizeArrayLarge<String>()
    private val rotationZArray = MaxSizeArrayLarge<String>()
    private val timeIndiceArray = MaxSizeArrayLarge<String>()
    private val heartRateArrayGraph = MaxSizeArray<String>()
    private val velocityXArrayGraph = MaxSizeArray<String>()
    private val velocityYArrayGraph = MaxSizeArray<String>()
    private val velocityZArrayGraph = MaxSizeArray<String>()
    private val rotationXArrayGraph = MaxSizeArray<String>()
    private val rotationYArrayGraph = MaxSizeArray<String>()
    private val rotationZArrayGraph = MaxSizeArray<String>()
    private var sensorData: String = ""
    private var lastNonZeroHeartRate = "0.0"

    private lateinit var hrGraph: SparkView
    private lateinit var velocityXGraph: SparkView
    private lateinit var velocityYGraph: SparkView
    private lateinit var velocityZGraph: SparkView
    private lateinit var rotationXGraph: SparkView
    private lateinit var rotationYGraph: SparkView
    private lateinit var rotationZGraph: SparkView
    private var duration = 0L

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        hrGraph = findViewById(R.id.hrGraph)
        velocityXGraph = findViewById(R.id.velocityXGraph)
        velocityYGraph = findViewById(R.id.velocityYGraph)
        velocityZGraph = findViewById(R.id.velocityZGraph)
        rotationXGraph = findViewById(R.id.rotationXGraph)
        rotationYGraph = findViewById(R.id.rotationYGraph)
        rotationZGraph = findViewById(R.id.rotationZGraph)


        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        activityContext = this
        wearableDeviceConnected = false
        val sexInputOptions = resources.getStringArray(R.array.sexInputOptions)
        val sexInputAdapter = ArrayAdapter(this,
            R.layout.spinner_dropdown_item, sexInputOptions)
        binding.sexInput.adapter = sexInputAdapter


        val RPEInputOptions = resources.getStringArray(R.array.RPEInputOptions)
        val RPEInputAdapter = ArrayAdapter(this,
            R.layout.spinner_dropdown_item, RPEInputOptions)
        binding.RPEInput.adapter = RPEInputAdapter

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

        binding.ExerciseSettingsSetNumber.setOnLongClickListener {
            println("press")

            // Create an input field for the AlertDialog
            val input = EditText(it.context)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            input.layoutParams = lp

            // Set the current value of the global variable 'current set' to the input field
            input.setText(currentSet.toString())

            // Create the AlertDialog
            AlertDialog.Builder(it.context)
                .setTitle("Change Current Set")
                .setView(input)
                .setPositiveButton("Update") { dialog, which ->
                    try {
                        currentSet = input.text.toString().toInt()
                        binding.ExerciseSettingsSetNumber.text = "Set $currentSet"
                        Toast.makeText(it.context, "Updated to $currentSet", Toast.LENGTH_SHORT).show()
                    } catch (e: NumberFormatException) {
                        Toast.makeText(it.context, "Invalid Number", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()


            true
        }

        binding.exerciseType.setOnLongClickListener {
            println("press")

            // Create an input field for the AlertDialog
            val input = EditText(it.context)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            input.layoutParams = lp

            // Set the current value of the global variable 'current set' to the input field
            input.setText(exerciseSelected)

            // Create the AlertDialog
            AlertDialog.Builder(it.context)
                .setTitle("Change Current Exercise")
                .setView(input)
                .setPositiveButton("Update") { dialog, which ->
                    try {
                        exerciseSelected = input.text.toString()
                        binding.exerciseType.text = "$exerciseSelected"
                        Toast.makeText(it.context, "Updated to $exerciseSelected", Toast.LENGTH_SHORT).show()
                    } catch (e: NumberFormatException) {
                        Toast.makeText(it.context, "Invalid", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()


            true
        }


        binding.discardBtn.setOnLongClickListener {
            println("discard")

            exerciseStarted = false
            binding.goBtn.setBackgroundResource(R.drawable.circle_button)
            binding.exerciseSettingsPage.startAnimation(fadeOutAnimation)
            binding.exerciseSettingsPage.visibility = View.GONE
            binding.exerciseSelectionPage.startAnimation(fadeInAnimation)
            binding.exerciseSelectionPage.visibility = View.VISIBLE
            currentSet = 1
            binding.loadInput.setText("")
            binding.repsInput.setText("")
            binding.RPEInput.setSelection(0)
            binding.nameInput.setText("")
            binding.sexInput.setSelection(0)
            binding.ageInput.setText("")
            binding.yearsTrainedInput.setText("")
            binding.ExerciseSettingsSetNumber.text = "Set $currentSet"
            true
        }




        binding.proceedBtn.setOnClickListener {
            hideKeyboard(it)
            val currentLoad = binding.loadInput.text
            val currentReps = binding.repsInput.text
            val currentRPE = binding.RPEInput.selectedItem.toString()
            if(currentLoad.isEmpty() || currentReps.isEmpty() || currentRPE == "-"){
                println("Missing fields")
            }else{
                binding.exerciseSettingsPage.startAnimation(fadeOutAnimation)
                binding.exerciseSettingsPage.visibility = View.GONE
                binding.exercisePage.startAnimation(fadeInAnimation)
                binding.exercisePage.visibility = View.VISIBLE
                binding.exerciseTypeText.text = "$exerciseSelected"
                val currentDateTime: Calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val currentDateTimeString: String = dateFormat.format(currentDateTime.time)
                // sensorData = "${currentSet-1}, $heartRateCSV, $velocityXCSV, $velocityYCSV, $velocityZCSV, $rotationXCSV, $rotationYCSV, $rotationZCSV"

                val name = binding.nameInput.text
                val sex = binding.sexInput.selectedItem.toString()
                val yearsTrained = binding.yearsTrainedInput.text
                val age = binding.ageInput.text
                val dataToSave = "${currentSet},$sensorData,$currentDateTimeString,$exerciseSelected,$currentLoad,$currentReps,$name,$sex,$yearsTrained,$age,$currentRPE,$duration\n"
                println("dataToSave = $dataToSave")
                saveToFile(dataToSave)

                binding.loadInput.setText("")
                binding.repsInput.setText("")
                binding.RPEInput.setSelection(0)
                currentSet += 1
                binding.setNumber.text = "Set $currentSet"
            }
        }


        binding.finishBtn2.setOnClickListener {
            binding.summaryPage.startAnimation(fadeOutAnimation)
            binding.summaryPage.visibility = View.GONE
            binding.exerciseSelectionPage.startAnimation(fadeInAnimation)
            binding.exerciseSelectionPage.visibility = View.VISIBLE
        }
        binding.finishBtn3.setOnClickListener {
            exerciseStarted = false
            binding.goBtn.setBackgroundResource(R.drawable.circle_button)
            binding.exercisePage.startAnimation(fadeOutAnimation)
            binding.exercisePage.visibility = View.GONE
            binding.exerciseSelectionPage.startAnimation(fadeInAnimation)
            binding.exerciseSelectionPage.visibility = View.VISIBLE
            currentSet = 1
            binding.loadInput.setText("")
            binding.repsInput.setText("")
            binding.RPEInput.setSelection(0)
            binding.nameInput.setText("")
            binding.sexInput.setSelection(0)
            binding.ageInput.setText("")
            binding.yearsTrainedInput.setText("")
            binding.ExerciseSettingsSetNumber.text = "Set $currentSet"
        }
        binding.backBtn2.setOnClickListener {
            binding.summaryPage.startAnimation(fadeOutAnimation)
            binding.summaryPage.visibility = View.GONE
            binding.exercisePage.startAnimation(fadeInAnimation)
            binding.exercisePage.visibility = View.VISIBLE
        }
        var lastPressedTime = 0L
        binding.goBtn.setOnClickListener {
            if(isGoBtnClickable){
                val currentTime = System.currentTimeMillis()
                duration = 0L
                if (lastPressedTime != 0L) {
                    duration = currentTime - lastPressedTime
                    println("Duration between presses: $duration ms")
                }

                lastPressedTime = currentTime

                isGoBtnClickable = false
                if (exerciseStarted == false) {
                    // duration variable starts counting here the time it takes until next goBtn press
                    binding.goBtn.setBackgroundResource(R.drawable.red_circle_button)
                    exerciseStarted = true
                    binding.goBtn.text = "STOP"
                    binding.ExerciseSettingsSetNumber.text = "Set $currentSet"
                } else {
                    binding.goBtn.setBackgroundResource(R.drawable.circle_button)
                    exerciseStarted = false
                    binding.setNumber.text = "Set $currentSet"
                    println("heartRateArray = $heartRateArray")
                    println("velocityXArray = $velocityXArray")
                    println("velocityYArray = $velocityYArray")
                    println("velocityZArray = $velocityZArray")
                    println("rotationXArray = $rotationXArray")
                    println("rotationYArray = $rotationYArray")
                    println("rotationZArray = $rotationZArray")
                    val timeIndiceCSV = timeIndiceArray.joinToString()
                    val heartRateCSV = heartRateArray.joinToString()
                    val velocityXCSV = velocityXArray.joinToString()
                    val velocityYCSV = velocityYArray.joinToString()
                    val velocityZCSV = velocityZArray.joinToString()
                    val rotationXCSV = rotationXArray.joinToString()
                    val rotationYCSV = rotationYArray.joinToString()
                    val rotationZCSV = rotationZArray.joinToString()
                    println("heartRateCSV = $heartRateCSV")
                    println("velocityXCSV = $velocityXCSV")
                    println("velocityYCSV = $velocityYCSV")
                    println("velocityZCSV = $velocityZCSV")
                    println("rotationXCSV = $rotationXCSV")
                    println("rotationYCSV = $rotationYCSV")
                    println("rotationZCSV = $rotationZCSV")
                    sensorData = "$timeIndiceCSV,$heartRateCSV,$velocityXCSV,$velocityYCSV,$velocityZCSV,$rotationXCSV,$rotationYCSV,$rotationZCSV"

                    // Clearing the arrays for the next set
                    timeIndiceArray.clear()
                    heartRateArray.clear()
                    velocityXArray.clear()
                    velocityYArray.clear()
                    velocityZArray.clear()
                    rotationXArray.clear()
                    rotationYArray.clear()
                    rotationZArray.clear()

                    heartRateArrayGraph.clear()
                    velocityXArrayGraph.clear()
                    velocityYArrayGraph.clear()
                    velocityZArrayGraph.clear()
                    rotationXArrayGraph.clear()
                    rotationYArrayGraph.clear()
                    rotationZArrayGraph.clear()
                    binding.goBtn.text = "START"
                    exerciseStarted = false
                    binding.goBtn.setBackgroundResource(R.drawable.circle_button)
                    binding.exercisePage.startAnimation(fadeOutAnimation)
                    binding.exercisePage.visibility = View.GONE
                    binding.exerciseSettingsPage.startAnimation(fadeInAnimation)
                    binding.exerciseSettingsPage.visibility = View.VISIBLE

                    // duration variable stops counting when pressed again

                }
                val cooldownTimer = object : CountDownTimer(2000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        // No action needed here for now
                    }

                    override fun onFinish() {
                        isGoBtnClickable = true
                    }
                }
                cooldownTimer.start()
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
                    currentSet = 1
                    binding.setNumber.text = "Set $currentSet"
                }
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun initialiseDevicePairing(tempAct: Activity) {
        // Coroutine
        launch(Dispatchers.Default) {
            var getNodesResBool: BooleanArray? = null

            try {
                getNodesResBool = getNodes(tempAct.applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }

                // UI Thread
                withContext(Dispatchers.Main) {
                    if (getNodesResBool!![0]) {
                        // if message Acknowledgement Received
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

                    println("received: $s")

                    val sensorDataParts = s.split("DateTime:", "HeartRate:", "Velocity:", "Rotation:")
                    val timeIndice = sensorDataParts[1].trim().removeSuffix(",")
                    val heartRate = sensorDataParts[2].trim().removeSuffix(",")
                    val velocity = sensorDataParts[3].trim().split(",").map { it.trim() }
                    val rotation = sensorDataParts[4].trim().split(",").map { it.trim() }


                    val velocityX = velocity[0].toString()
                    val velocityY = velocity[1].toString()
                    val velocityZ = velocity[2].toString()
                    val rotationX = rotation[0].toString()
                    val rotationY = rotation[1].toString()
                    val rotationZ = rotation[2].toString()





                    if(exerciseStarted == true) {
                        timeIndiceArray.add(timeIndice)
                        if(heartRate.toFloat() == 0.0f && lastNonZeroHeartRate != "0.0") {
                            heartRateArray.add(lastNonZeroHeartRate)
                        } else {
                            heartRateArray.add(heartRate)
                            lastNonZeroHeartRate = heartRate
                        }
                        velocityXArray.add(toStandardNotation(velocityX.toFloat()))
                        velocityYArray.add(toStandardNotation(velocityY.toFloat()))
                        velocityZArray.add(toStandardNotation(velocityZ.toFloat()))
                        rotationXArray.add(toStandardNotation(rotationX.toFloat()))
                        rotationYArray.add(toStandardNotation(rotationY.toFloat()))
                        rotationZArray.add(toStandardNotation(rotationZ.toFloat()))


                    }
                    heartRateArrayGraph.add(if (Math.abs(heartRate.toFloat()) < 1) "0.0" else heartRate)
                    velocityXArrayGraph.add(if (Math.abs(velocityX.toFloat()) < 0.2) "0.0" else velocityX)
                    velocityYArrayGraph.add(if (Math.abs(velocityY.toFloat()) < 0.2) "0.0" else velocityY)
                    velocityZArrayGraph.add(if (Math.abs(velocityZ.toFloat()) < 0.2) "0.0" else velocityZ)
                    rotationXArrayGraph.add(if (Math.abs(rotationX.toFloat()) < 0.2) "0.0" else rotationX)
                    rotationYArrayGraph.add(if (Math.abs(rotationY.toFloat()) < 0.2) "0.0" else rotationY)
                    rotationZArrayGraph.add(if (Math.abs(rotationZ.toFloat()) < 0.2) "0.0" else rotationZ)

                    hrGraph.adapter = SparkGraphAdapter(heartRateArrayGraph.toList())
                    velocityXGraph.adapter = SparkGraphAdapter(velocityXArrayGraph.toList())
                    velocityYGraph.adapter = SparkGraphAdapter(velocityYArrayGraph.toList())
                    velocityZGraph.adapter = SparkGraphAdapter(velocityZArrayGraph.toList())
                    rotationXGraph.adapter = SparkGraphAdapter(rotationXArrayGraph.toList())
                    rotationYGraph.adapter = SparkGraphAdapter(rotationYArrayGraph.toList())
                    rotationZGraph.adapter = SparkGraphAdapter(rotationZArrayGraph.toList())

                    hrGraph.cornerRadius = 20f
                    velocityXGraph.cornerRadius = 20f
                    velocityYGraph.cornerRadius = 20f
                    velocityZGraph.cornerRadius = 20f
                    rotationXGraph.cornerRadius = 20f
                    rotationYGraph.cornerRadius = 20f
                    rotationZGraph.cornerRadius = 20f



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
            val path = "$sdCardPath/data15.csv"
            val file = File(path)

            try {
                if (!file.exists()) {
                    file.createNewFile()
                    FileWriter(file, true).use { writer ->
                        BufferedWriter(writer).use { bufferedWriter ->
                            bufferedWriter.write("Set,TimeIndice,HeartRate,VelocityX,VelocityY,VelocityZ,RotationX,RotationY,RotationZ,Id,Exercise Selected,Load,Reps,Name,Sex,Years Trained,Age,RPE,Duration\n")
                        }
                    }
                }
                println("Saving file internally")
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
            saveToFileInternal(data)
        }
    }

    private fun saveToFileInternal(data: String) {
        val internalFilePath = getFilesDir().absolutePath + "/data15.csv"
        val file = File(internalFilePath)

        try {
            if (!file.exists()) {
                file.createNewFile()
                FileWriter(file, true).use { writer ->
                    BufferedWriter(writer).use { bufferedWriter ->
                        bufferedWriter.write("Set,TimeIndice,HeartRate,VelocityX,VelocityY,VelocityZ,RotationX,RotationY,RotationZ,Id,Exercise Selected,Load,Reps,Name,Sex,Years Trained,Age,RPE,Duration\n")
                    }
                }
            }
            println("Saving file internally")
            FileWriter(file, true).use { writer ->
                BufferedWriter(writer).use { bufferedWriter ->
                    bufferedWriter.write(data)
                }
            }
        } catch (e: IOException) {
            println("Error encountered")
            e.printStackTrace()
        }
    }
    private class SparkGraphAdapter(private val data: List<String>) : SparkAdapter() {
        override fun getCount() = data.size

        override fun getItem(index: Int) = data[index].toFloat()

        override fun getY(index: Int) = getItem(index)
    }

    class MaxSizeArray<T>() {
        private val deque: ArrayDeque<T> = ArrayDeque(300)

        fun add(element: T) {
            if (deque.size == 300) {
                deque.pollFirst()  // Remove the oldest element
            }
            deque.addLast(element)
        }
        fun clear() {
            deque.clear()
        }

        fun toList(): List<T> {
            return deque.toList()
        }
        fun toArray(): Array<Any?> = deque.toArray()

        override fun toString(): String = deque.toString()
    }
    class MaxSizeArrayLarge<T>() {
        private val list: ArrayList<T> = ArrayList()

        fun add(element: T) {
            list.add(element)
        }

        fun clear() {
            list.clear()
        }

        fun toList(): List<T> {
            return list.toList()
        }

        fun toArray(): Array<Any?> = list.toArray()

        fun joinToString(separator: String = ";"): String {
            return list
                .filter { it.toString().isNotBlank() }
                .joinToString(separator) { it.toString().replace(",", "").replace("\n", "").trim() }
        }

        override fun toString(): String = list.toString()
    }

    fun toStandardNotation(value: Float): String {
        val formatter = DecimalFormat("0.#####") // Up to 5 decimal places, modify as needed
        return formatter.format(value)
    }

    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }



}




