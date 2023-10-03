package com.mujugen.mypersonaltrainer

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import com.robinhood.spark.SparkAdapter
import com.robinhood.spark.SparkView
import java.util.ArrayDeque
import java.util.ArrayList
import kotlin.random.Random
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.mujugen.mypersonaltrainer.databinding.ActivityWorkoutBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
        binding.loadProgressBar.progress = 70
        binding.repsProgressBar.progress = 20
        binding.hrGraph.adapter = SparkGraphAdapter(heartRateArrayGraph.toList())

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
                    calculateResults()
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



    override fun onMessageReceived(messageEvent: MessageEvent) {
        val message = String(messageEvent.data)
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

                binding.hrGraph.adapter = SparkGraphAdapter(heartRateArrayGraph.toList())
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
            }
        }
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
