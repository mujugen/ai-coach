package com.mujugen.mypersonaltrainer

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mujugen.mypersonaltrainer.databinding.ActivityWorkoutBinding
import com.mujugen.mypersonaltrainer.ml.Model1
import com.robinhood.spark.SparkAdapter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random


class WorkoutActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var binding: ActivityWorkoutBinding
    private var currentSet = 1
    private var exerciseStarted = false
    private val heartRateArray = MaxSizeArrayLarge<String>()
    private val velocityXArray = MaxSizeArrayLarge<String>()
    private val velocityYArray = MaxSizeArrayLarge<String>()
    private val velocityZArray = MaxSizeArrayLarge<String>()
    private val rotationXArray = MaxSizeArrayLarge<String>()
    private val rotationYArray = MaxSizeArrayLarge<String>()
    private val rotationZArray = MaxSizeArrayLarge<String>()
    private val timeIndiceArray = MaxSizeArrayLarge<String>()
    private var sensorData: String = ""
    private var lastNonZeroHeartRate = "0.0"
    private lateinit var buttonClickAnimation: Animation
    private val heartRateArrayGraph = MaxSizeArray<String>()
    private val movementArrayGraph = MaxSizeArray<String>()
    private var duration = 0L
    private var connectedNode: Node? = null
    private lateinit var messageClient: MessageClient
    private var connectionStatus = false
    private var inputReps = "0"
    private var inputLoad = "0"
    private var predictedRPE = "0"
    private var recommendedLoad = "0"
    private lateinit var fadeInAnimation: Animation

    private var highestLoadOfSelectedExercise = 0
    private var todayVolume = 0
    private var setsLeftOfSelectedExercise = 0
    private var exerciseType = ""
    private var sex = "Male"
    private var yearsTrained = ""
    private var age = ""
    private var highestAllTime = 0
    private var highestAllTimeExercise = ""

    val hrScaler = Scaler(181F)
    val velocityXScaler = Scaler(62.49824F)
    val velocityYScaler = Scaler(59.13199F)
    val velocityZScaler = Scaler(53.77856F)
    val rotationXScaler = Scaler(33.74542F)
    val rotationYScaler = Scaler(14.00836F)
    val rotationZScaler = Scaler(16.70961F)
    val durationScaler = Scaler(1F)
    val reps_scaler = Scaler(1F)
    val years_trained_scaler = Scaler(1F)
    val load_scaler = Scaler(1F)
    val age_scaler = Scaler(1F)


    fun runModel() {
        val dataArray = Array(1) { Array(7193) { FloatArray(15) } }

        val arraySize = heartRateArray.toList().size

        for (i in 0 until 7193) {
            if(i<arraySize){
                // Exercise Selected (replace 5.0f with actual data if dynamic)

                val encodedExercise = when (exerciseType) {
                    "back rows" -> 0.0F
                    "bench press" -> 1.0F
                    "bicep curl" -> 2.0F
                    "chest fly" -> 3.0F
                    "hammer curl" -> 4.0F
                    "lat pulldown" -> 5.0F
                    "shoulder press" -> 6.0F
                    "tricep pushdown" -> 7.0F
                    else -> 0.0F
                }

                dataArray[0][i][0] = encodedExercise

                // Duration (scaled)
                dataArray[0][i][1] = durationScaler.scale(duration.toFloat())

                // Reps (scaled)
                dataArray[0][i][2] = reps_scaler.scale(inputReps.toFloat())

                // Years Trained (scaled)
                val numericYearsTrained = when (yearsTrained) {
                    "Less than 1 year" -> 0
                    "1 year" -> 1
                    "2 years" -> 2
                    "3 or more years" -> 3
                    else -> 0
                }
                dataArray[0][i][3] = years_trained_scaler.scale(numericYearsTrained.toFloat())

                // Sex (encoded)
                dataArray[0][i][4] = if (sex == "Male") 2.0f else 1.0f

                // Load (scaled)
                dataArray[0][i][5] = load_scaler.scale(inputLoad.toFloat())

                // Age (scaled)
                dataArray[0][i][6] = age_scaler.scale(age.toFloat())

                // Set (currentSet)
                val adjustedSet = currentSet.coerceIn(1, 6) - 1
                dataArray[0][i][7] = adjustedSet.toFloat()

                // HeartRate (scaled)
                dataArray[0][i][8] = hrScaler.scale(heartRateArray.toList()[i].toFloat())

                // VelocityX, VelocityY, VelocityZ (scaled)
                dataArray[0][i][9] = velocityXScaler.scale(velocityXArray.toList()[i].toFloat())
                dataArray[0][i][10] = velocityYScaler.scale(velocityYArray.toList()[i].toFloat())
                dataArray[0][i][11] = velocityZScaler.scale(velocityZArray.toList()[i].toFloat())

                // RotationX, RotationY, RotationZ (scaled)
                dataArray[0][i][12] = rotationXScaler.scale(rotationXArray.toList()[i].toFloat())
                dataArray[0][i][13] = rotationYScaler.scale(rotationYArray.toList()[i].toFloat())
                dataArray[0][i][14] = rotationZScaler.scale(rotationZArray.toList()[i].toFloat())
            }
            else{
                val encodedExercise = when (exerciseType) {
                    "back rows" -> 0.0F
                    "bench press" -> 1.0F
                    "bicep curl" -> 2.0F
                    "chest fly" -> 3.0F
                    "hammer curl" -> 4.0F
                    "lat pulldown" -> 5.0F
                    "shoulder press" -> 6.0F
                    "tricep pushdown" -> 7.0F
                    else -> 0.0F
                }

                dataArray[0][i][0] = encodedExercise

                // Duration (scaled)
                dataArray[0][i][1] = durationScaler.scale(duration.toFloat())

                // Reps (scaled)
                dataArray[0][i][2] = reps_scaler.scale(inputReps.toFloat())

                // Years Trained (scaled)
                val numericYearsTrained = when (yearsTrained) {
                    "Less than 1 year" -> 1
                    "1 year" -> 2
                    "2 years" -> 3
                    "3 or more years" -> 4
                    else -> 0
                }
                dataArray[0][i][3] = years_trained_scaler.scale(numericYearsTrained.toFloat())

                // Sex (encoded)
                dataArray[0][i][4] = if (sex == "Male") 2.0f else 1.0f

                // Load (scaled)
                dataArray[0][i][5] = load_scaler.scale(inputLoad.toFloat())

                // Age (scaled)
                dataArray[0][i][6] = age_scaler.scale(age.toFloat())

                // Set (currentSet)
                val adjustedSet = currentSet.coerceIn(1, 6) - 1
                dataArray[0][i][7] = adjustedSet.toFloat()

                // HeartRate (scaled)
                dataArray[0][i][8] = hrScaler.scale(0f)

                // VelocityX, VelocityY, VelocityZ (scaled)
                dataArray[0][i][9] = velocityXScaler.scale(0f)
                dataArray[0][i][10] = velocityYScaler.scale(0f)
                dataArray[0][i][11] = velocityZScaler.scale(0f)

                // RotationX, RotationY, RotationZ (scaled)
                dataArray[0][i][12] = rotationXScaler.scale(0f)
                dataArray[0][i][13] = rotationYScaler.scale(0f)
                dataArray[0][i][14] = rotationZScaler.scale(0f)
            }

        }
        println("Exercise Selected ${dataArray[0][0][0]}")
        println("Duration ${dataArray[0][0][1]}")
        println("Reps ${dataArray[0][0][2]}")
        println("Years Trained ${dataArray[0][0][3]}")
        println("Sex ${dataArray[0][0][4]}")
        println("Load ${dataArray[0][0][5]}")
        println("Age ${dataArray[0][0][6]}")
        println("Set ${dataArray[0][0][7]}")
        println("HeartRate ${dataArray[0][0][8]}")
        println("VelocityX ${dataArray[0][0][9]}")
        println("VelocityY ${dataArray[0][0][10]}")
        println("VelocityZ ${dataArray[0][0][11]}")
        println("RotationX ${dataArray[0][0][12]}")
        println("RotationY ${dataArray[0][0][13]}")
        println("RotationZ ${dataArray[0][0][14]}")
        // Load model
        val assetManager = assets
        val modelStream = assetManager.open("model1.tflite")
        val modelByteBuffer = ByteBuffer.allocateDirect(modelStream.available()).order(ByteOrder.nativeOrder())
        modelByteBuffer.put(modelStream.readBytes())
        modelStream.close()


        // Initialize interpreter with GPU delegate
        val options = Interpreter.Options()
        //val gpuDelegate = GpuDelegate()
        //options.addDelegate(gpuDelegate)
        val interpreter = Interpreter(modelByteBuffer, options)

        // Set up input and output buffers
        val inputShape = interpreter.getInputTensor(0).shape()
        println("inputShape = ")
        println(inputShape[0]) // returns 1 (the x value of the sequences used in my model)
        println(inputShape[1]) // returns 7193 (the number of rows of my pandas dataframe)
        println(inputShape[2]) // returns 15 (which is the number of columns of my pandas dataframe)
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputShape[1] * inputShape[2])

        for (i in 0 until inputShape[1]) {
            for (j in 0 until inputShape[2]) {
                byteBuffer.putFloat(dataArray[0][i][j])
            }
        }

        val outputShape = interpreter.getOutputTensor(0).shape()
        // Assuming outputShape[1] is 1, as per the error message
        val output = Array(1) { FloatArray(outputShape[1]) }

        // Adjust the interpreter.run call accordingly
        interpreter.run(dataArray, output)

        // The predicted value can be accessed as follows, based on your model's output
        val predictedValue = output[0][0]
        println("Predicted value is: $predictedValue")


        // Close interpreter and delegate when done
        interpreter.close()
        //gpuDelegate.close()
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutBinding.inflate(layoutInflater)  // Initialize binding
        setContentView(binding.root)

        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_2)
        messageClient = Wearable.getMessageClient(this)
        messageClient.addListener(this)
        connectToSmartwatch()

        buttonClickAnimation = AnimationUtils.loadAnimation(this, R.anim.button_click_animation)
        exerciseType = intent.getStringExtra("exerciseType").toString()
        runBlocking { loadData() }

        binding.exerciseTypeText.text = exerciseType
        binding.setText.text = "Set $currentSet"
        binding.hrGraph.adapter = SparkGraphAdapter(heartRateArrayGraph.toList())
        binding.movementGraph.adapter = SparkGraphAdapter(movementArrayGraph.toList())

        var lastPressedTime = 0L
        fun goFunction(){
            binding.goBtn.startAnimation(buttonClickAnimation)
            val currentTime = System.currentTimeMillis()
            duration = 0L
            if (lastPressedTime != 0L) {
                duration = currentTime - lastPressedTime
                println("Duration between presses: $duration ms")
            }

            lastPressedTime = currentTime

            if (!exerciseStarted) {
                timeIndiceArray.clear()
                heartRateArray.clear()
                velocityXArray.clear()
                velocityYArray.clear()
                velocityZArray.clear()
                rotationXArray.clear()
                rotationYArray.clear()
                rotationZArray.clear()
                movementArrayGraph.clear()
                heartRateArrayGraph.clear()
                binding.goBtn.setImageResource(R.drawable.stop_btn)
                exerciseStarted = true
                sendMessageToSmartwatch("Go")
            } else {
                sendMessageToSmartwatch("Stop")
                binding.goBtn.setImageResource(R.drawable.go_btn)

                binding.popup.visibility = View.VISIBLE
                binding.popup.startAnimation(fadeInAnimation)
                binding.setInputPage.visibility = View.VISIBLE
                binding.mainLayout.isClickable = false;
                binding.goBtn.isClickable = false;

                exerciseStarted = false



            }

        }

        binding.goBtn.setOnClickListener {
            goFunction()
        }

        binding.confirmInputBtn.setOnClickListener {
            inputReps = binding.inputReps.text.toString().trim()
            inputLoad = binding.inputLoad.text.toString().trim()

            if (inputReps.isEmpty() || inputLoad.isEmpty()) {
                // Display a toast when there's no input
                Toast.makeText(this@WorkoutActivity, "Please input workout details", Toast.LENGTH_SHORT).show()
            } else {
                    binding.setInputPage.visibility = View.GONE
                    binding.loadingPage.startAnimation(fadeInAnimation)
                    binding.loadingPage.visibility = View.VISIBLE




                runBlocking {  lifecycleScope.launch {
                    try {
                        val preferences = dataStore.data.first()
                        if (inputLoad.toInt() > highestLoadOfSelectedExercise) {
                            when (exerciseType) {
                                "Bench Press" -> dataStore.edit { preferences -> preferences[stringPreferencesKey("highest_bench_press")] = inputLoad}
                                "Back Rows" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("highest_back_rows")] = inputLoad}
                                "Tricep Pushdown" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("highest_tricep_pushdown")] = inputLoad}
                                "Bicep Curl" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("highest_bicep_curl")] = inputLoad}
                                "Lat Pulldown" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("highest_lat_pulldown")] = inputLoad}
                                "Hammer Curl" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("highest_hammer_curl")] = inputLoad}
                                "Shoulder Press" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("highest_shoulder_press")] = inputLoad}
                                "Chest Fly" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("highest_chest_fly")] = inputLoad}}
                        }
                        todayVolume += inputReps.toInt()
                        val currentDate = Date()
                        val currentDayOfWeek = SimpleDateFormat("EEEE", Locale.US).format(currentDate)
                        when (currentDayOfWeek) {
                            "Sunday" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("sunday_volume")] = todayVolume.toString();preferences[stringPreferencesKey("monday_volume")] = "0";preferences[stringPreferencesKey("tuesday_volume")] = "0";preferences[stringPreferencesKey("wednesday_volume")] = "0";preferences[stringPreferencesKey("thursday_volume")] = "0";preferences[stringPreferencesKey("friday_volume")] = "0";preferences[stringPreferencesKey("saturday_volume")] = "0";}
                            "Monday" -> dataStore.edit { preferences -> preferences[stringPreferencesKey("monday_volume")] = todayVolume.toString();preferences[stringPreferencesKey("tuesday_volume")] = "0";preferences[stringPreferencesKey("wednesday_volume")] = "0";preferences[stringPreferencesKey("thursday_volume")] = "0";preferences[stringPreferencesKey("friday_volume")] = "0";preferences[stringPreferencesKey("saturday_volume")] = "0";}
                            "Tuesday" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("tuesday_volume")] = todayVolume.toString();preferences[stringPreferencesKey("wednesday_volume")] = "0";preferences[stringPreferencesKey("thursday_volume")] = "0";preferences[stringPreferencesKey("friday_volume")] = "0";preferences[stringPreferencesKey("saturday_volume")] = "0";}
                            "Wednesday" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("wednesday_volume")] = todayVolume.toString();preferences[stringPreferencesKey("thursday_volume")] = "0";preferences[stringPreferencesKey("friday_volume")] = "0";preferences[stringPreferencesKey("saturday_volume")] = "0";}
                            "Thursday" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("thursday_volume")] = todayVolume.toString();preferences[stringPreferencesKey("friday_volume")] = "0";preferences[stringPreferencesKey("saturday_volume")] = "0";}
                            "Friday" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("friday_volume")] = todayVolume.toString();preferences[stringPreferencesKey("saturday_volume")] = "0";}
                            "Saturday" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("saturday_volume")] = todayVolume.toString()}

                        }
                        if (setsLeftOfSelectedExercise>0) {
                            setsLeftOfSelectedExercise -= 1
                            when (exerciseType) {
                                "Bench Press" -> dataStore.edit { preferences -> preferences[stringPreferencesKey("daily_bench_press_sets_left")] = setsLeftOfSelectedExercise.toString() }
                                "Back Rows" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("daily_back_rows_sets_left")] = setsLeftOfSelectedExercise.toString() }
                                "Tricep Pushdown" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("daily_tricep_pushdown_sets_left")] = setsLeftOfSelectedExercise.toString() }
                                "Bicep Curl" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("daily_bicep_curl_sets_left")] = setsLeftOfSelectedExercise.toString() }
                                "Lat Pulldown" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("daily_lat_pulldown_sets_left")] = setsLeftOfSelectedExercise.toString() }
                                "Hammer Curl" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("daily_hammer_curl_sets_left")] = setsLeftOfSelectedExercise.toString() }
                                "Shoulder Press" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("daily_shoulder_press_sets_left")] = setsLeftOfSelectedExercise.toString() }
                                "Chest Fly" ->  dataStore.edit { preferences -> preferences[stringPreferencesKey("daily_chest_fly_sets_left")] = setsLeftOfSelectedExercise.toString() }}
                        }

                        if(inputLoad.toInt()>highestAllTime){
                            highestAllTime = inputLoad.toInt()
                            highestAllTimeExercise = exerciseType
                            if(highestAllTime != 0 && highestAllTimeExercise != ""){
                                dataStore.edit { preferences -> preferences[stringPreferencesKey("highest_all_time")] = highestAllTime.toString(); preferences[stringPreferencesKey("highest_all_time_exercise")] = highestAllTimeExercise}

                            }
                        }

                        }catch (e: Exception) {
                        println("Error loading data: ${e.message}")
                    }
                    } }
                    hideKeyboard()

                Handler(Looper.getMainLooper()).postDelayed({
                    calculateResults()
                }, 1000)

                }

        }


        binding.continueBtn.setOnClickListener {
            binding.resultPage.visibility = View.GONE
            binding.popup.visibility = View.GONE
            currentSet += 1
            binding.setText.text = "Set $currentSet"
            binding.mainLayout.isClickable = true;
            binding.goBtn.isClickable = true;
        }


    }




    fun calculateResults(){
        println("Reps: $inputReps, Load: $inputLoad, Exercised Selected: $exerciseType, Duration: $duration, Set: $currentSet, Sex: $sex, Age: $age, Experience: $yearsTrained, HeartRate: $heartRateArray, VelocityX: $velocityXArray, VelocityY: $velocityYArray, VelocityZ: $velocityZArray, RotationX: $rotationXArray, RotationY: $rotationYArray, RotationZ: $rotationZArray")
        val targetRPE = Random.nextInt(1, 11)
        val RPEDifference = targetRPE - predictedRPE.toInt()
        recommendedLoad = (((RPEDifference * 0.04) + 1) * inputLoad.toInt()).toInt().toString()
        binding.recommendedLoadText.text = recommendedLoad
        binding.rpeText.text = targetRPE.toString()

        binding.inputLoad.setText(recommendedLoad)
        println("running model")
        runModel()
        println("finished running model")

        Handler(Looper.getMainLooper()).postDelayed({
            binding.loadingPage.visibility = View.GONE
            binding.resultPage.startAnimation(fadeInAnimation)
            binding.resultPage.visibility = View.VISIBLE
        }, 2000)
    }

    fun hideKeyboard() {
        val view = currentFocus ?: binding.root
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }


    var messageCount = 0
    override fun onMessageReceived(messageEvent: MessageEvent) {
        val message = String(messageEvent.data)
        println("Received message: $message")
        if (message == "Go") {
            println("Go")
            if(!exerciseStarted){
                binding.goBtn.performClick()
            }
        }
        if (message == "Stop") {
            println("Stop")
            if(exerciseStarted){
                binding.goBtn.performClick()
            }
        }
        if (message.startsWith("DateTime:")) {
            val sensorDataParts = message.split("DateTime:", "HeartRate:", "Velocity:", "Rotation:")
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
                    binding.hrText.text = "$lastNonZeroHeartRate"
                    heartRateArray.add(lastNonZeroHeartRate)
                    heartRateArrayGraph.add(lastNonZeroHeartRate)
                } else {
                    binding.hrText.text = "$heartRate"
                    heartRateArray.add(heartRate)
                    heartRateArrayGraph.add(heartRate)
                    lastNonZeroHeartRate = heartRate
                }
                velocityXArray.add(toStandardNotation(velocityX.toFloat()))
                velocityYArray.add(toStandardNotation(velocityY.toFloat()))
                velocityZArray.add(toStandardNotation(velocityZ.toFloat()))
                rotationXArray.add(toStandardNotation(rotationX.toFloat()))
                rotationYArray.add(toStandardNotation(rotationY.toFloat()))
                rotationZArray.add(toStandardNotation(rotationZ.toFloat()))
                when (exerciseType) {
                    "Bench Press" -> movementArrayGraph.add(rotationZ)
                    "Back Rows" ->  movementArrayGraph.add(rotationY)
                    "Tricep Pushdown" ->  movementArrayGraph.add(rotationY)
                    "Bicep Curl" ->  movementArrayGraph.add(rotationY)
                    "Lat Pulldown" ->  movementArrayGraph.add(rotationZ)
                    "Hammer Curl" ->  movementArrayGraph.add(rotationZ)
                    "Shoulder Press" ->  movementArrayGraph.add(velocityX)
                    "Chest Fly" ->  movementArrayGraph.add(rotationY)
                    }

                messageCount++
                if (messageCount % 20 == 0) {
                    messageCount = 0

                    binding.hrGraph.adapter = SparkGraphAdapter(heartRateArrayGraph.toList())
                    binding.movementGraph.adapter = SparkGraphAdapter(movementArrayGraph.toList())
                }
            }
        }
    }

    fun toStandardNotation(value: Float): String {
        val formatter = DecimalFormat("0.#####") // Up to 5 decimal places, modify as needed
        return formatter.format(value)
    }

    fun connectToSmartwatch() {
        // Check if there are already connected nodes (smartwatches)
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isNotEmpty()) {
                // Use the first connected node for simplicity, or choose the desired one
                connectedNode = nodes[0]
                sendMessageToSmartwatch("Connect")
                connectionStatus = true
            } else {
                // No connected nodes found, return to the previous activity
                goToPreviousActivity()
            }
        }.addOnFailureListener {
            // Failed to retrieve connected nodes, return to the previous activity
            goToPreviousActivity()
        }
    }

    fun goToPreviousActivity() {
        Toast.makeText(this, "Failed to connect to smartwatch", Toast.LENGTH_SHORT).show()
        finish()  // End the current activity and return to the previous one
    }


    private fun sendMessageToSmartwatch(message: String) {
        connectedNode?.let { node ->
            // Build the message
            val byteMessage = message.toByteArray()
            // Send the message to the connected node (smartwatch)
            messageClient.sendMessage(node.id, "/message_path", byteMessage)
            println("sent to $node.id")
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val preferences = dataStore.data.first()


                val currentDate = Date()
                val currentDayOfWeek = SimpleDateFormat("EEEE", Locale.US).format(currentDate)
                todayVolume = when (currentDayOfWeek) {
                    "Sunday" ->  preferences[stringPreferencesKey("sunday_volume")]?.toInt() ?: 0
                    "Monday" ->   preferences[stringPreferencesKey("monday_volume")]?.toInt() ?: 0
                    "Tuesday" ->   preferences[stringPreferencesKey("tuesday_volume")]?.toInt() ?: 0
                    "Wednesday" ->   preferences[stringPreferencesKey("wednesday_volume")]?.toInt() ?: 0
                    "Thursday" ->   preferences[stringPreferencesKey("thursday_volume")]?.toInt() ?: 0
                    "Friday" ->   preferences[stringPreferencesKey("friday_volume")]?.toInt() ?: 0
                    "Saturday" ->   preferences[stringPreferencesKey("saturday_volume")]?.toInt() ?: 0
                    else -> 0
                }

                highestLoadOfSelectedExercise = when (exerciseType) {
                    "Bench Press" ->  preferences[stringPreferencesKey("highest_bench_press")]?.toInt() ?: 0
                    "Back Rows" ->  preferences[stringPreferencesKey("highest_back_rows")]?.toInt() ?: 0
                    "Tricep Pushdown" ->  preferences[stringPreferencesKey("highest_tricep_pushdown")]?.toInt() ?: 0
                    "Bicep Curl" ->  preferences[stringPreferencesKey("highest_bicep_curl")]?.toInt() ?: 0
                    "Lat Pulldown" ->  preferences[stringPreferencesKey("highest_lat_pulldown")]?.toInt() ?: 0
                    "Hammer Curl" ->  preferences[stringPreferencesKey("highest_hammer_curl")]?.toInt() ?: 0
                    "Shoulder Press" ->  preferences[stringPreferencesKey("highest_shoulder_press")]?.toInt() ?: 0
                    "Chest Fly" ->  preferences[stringPreferencesKey("highest_chest_fly")]?.toInt() ?: 0
                    else -> 0
                }

                setsLeftOfSelectedExercise = when (exerciseType) {
                    "Bench Press" ->  preferences[stringPreferencesKey("daily_bench_press_sets_left")]?.toInt() ?: 1
                    "Back Rows" ->  preferences[stringPreferencesKey("daily_back_rows_sets_left")]?.toInt() ?: 2
                    "Tricep Pushdown" ->  preferences[stringPreferencesKey("daily_tricep_pushdown_sets_left")]?.toInt() ?: 3
                    "Bicep Curl" ->  preferences[stringPreferencesKey("daily_bicep_curl_sets_left")]?.toInt() ?: 4
                    "Lat Pulldown" ->  preferences[stringPreferencesKey("daily_lat_pulldown_sets_left")]?.toInt() ?: 5
                    "Hammer Curl" ->  preferences[stringPreferencesKey("daily_hammer_curl_sets_left")]?.toInt() ?: 6
                    "Shoulder Press" ->  preferences[stringPreferencesKey("daily_shoulder_press_sets_left")]?.toInt() ?: 7
                    "Chest Fly" ->  preferences[stringPreferencesKey("daily_chest_fly_sets_left")]?.toInt() ?: 8
                    else -> 0
                }
                println("setsLeftOfSelectedExercise = $setsLeftOfSelectedExercise")
                println("exerciseType = $exerciseType")
                highestAllTime = preferences[stringPreferencesKey("highest_all_time")]?.toInt() ?: 0
                highestAllTimeExercise = preferences[stringPreferencesKey("highest_all_time_exercise")] ?: ""

                sex = preferences[stringPreferencesKey("sex")] ?: "Male"
                yearsTrained = preferences[stringPreferencesKey("experience")] ?: "1 year"
                val birthday = preferences[stringPreferencesKey("birthday")] ?: "2000 1 13"
                age = calculateAge(birthday).toString()
            } catch (e: Exception) {
                println("Error loading data: ${e.message}")
            }
        }
    }

}


fun calculateAge(birthday: String): Int {
    // Define a date format for parsing the birthday string
    val dateFormat = SimpleDateFormat("yyyy MM dd", Locale.US)

    try {
        // Parse the birthday string into a Date object
        val birthDate = dateFormat.parse(birthday)

        // Get the current date
        val currentDate = Date()

        // Calculate the age
        val calendar = Calendar.getInstance()
        calendar.time = birthDate
        val birthYear = calendar.get(Calendar.YEAR)

        calendar.time = currentDate
        val currentYear = calendar.get(Calendar.YEAR)

        return currentYear - birthYear
    } catch (e: Exception) {
        e.printStackTrace()
        // Return a default age or handle the error as needed
        return -1
    }
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


private class SparkGraphAdapter(private val data: List<String>) : SparkAdapter() {
    override fun getCount() = data.size

    override fun getItem(index: Int) = data[index].toFloat()

    override fun getY(index: Int) = getItem(index)
}

data class Scaler(val maxAbs: Float) {
    fun scale(value: Float): Float {
        return value / maxAbs
    }
}


fun encodeExerciseSelected(exercise: String): Float {
    return when(exercise) {
        "back rows" -> 0.0F
        "bench press" -> 1.0F
        "bicep curl" -> 2.0F
        "chest fly" -> 3.0F
        "hammer curl" -> 4.0F
        "lat pulldown" -> 5.0F
        "shoulder press" -> 6.0F
        "tricep pushdown" -> 7.0F
        else -> throw IllegalArgumentException("Invalid Exercise Selected: $exercise")
    }
}

fun encodeSex(sex: String): Float {
    return when(sex) {
        "female" -> 0.0F
        "male" -> 1.0F
        else -> throw IllegalArgumentException("Invalid Sex: $sex")
    }
}

fun encodeSet(set: Int): Float {
    return when(set) {
        1 -> 0.0F
        2 -> 1.0F
        3 -> 2.0F
        4 -> 3.0F
        5 -> 4.0F
        else -> 5.0F
    }
}

