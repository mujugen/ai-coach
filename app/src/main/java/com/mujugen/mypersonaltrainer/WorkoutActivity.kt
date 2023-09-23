package com.mujugen.mypersonaltrainer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.robinhood.spark.SparkAdapter
import com.robinhood.spark.SparkView
import java.util.ArrayDeque
import java.util.ArrayList
import kotlin.random.Random

class WorkoutActivity : AppCompatActivity() {

    private var currentSet = 1
    private var exerciseStarted = false
    private val heartRateArray = MainActivity.MaxSizeArrayLarge<String>()
    private val velocityXArray = MainActivity.MaxSizeArrayLarge<String>()
    private val velocityYArray = MainActivity.MaxSizeArrayLarge<String>()
    private val velocityZArray = MainActivity.MaxSizeArrayLarge<String>()
    private val rotationXArray = MainActivity.MaxSizeArrayLarge<String>()
    private val rotationYArray = MainActivity.MaxSizeArrayLarge<String>()
    private val rotationZArray = MainActivity.MaxSizeArrayLarge<String>()
    private val timeIndiceArray = MainActivity.MaxSizeArrayLarge<String>()
    private var sensorData: String = ""
    private var lastNonZeroHeartRate = "0.0"

    private val heartRateArrayGraph = MaxSizeArray<String>()
    private lateinit var hrGraph: SparkView
    private var duration = 0L
    private var isGoBtnClickable = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)
        val exerciseType = intent.getStringExtra("exerciseType")

        val exerciseTypeText = findViewById<TextView>(R.id.exerciseTypeText)
        val goBtn = findViewById<ImageView>(R.id.goBtn)
        val setNumber = findViewById<TextView>(R.id.setText)
        val hrText = findViewById<TextView>(R.id.hrText)
        exerciseTypeText.text = exerciseType
        setNumber.text = "Set $currentSet"
        val loadProgressBar = findViewById<ProgressBar>(R.id.loadProgressBar)
        loadProgressBar.progress = 70

        val repsProgressBar = findViewById<ProgressBar>(R.id.repsProgressBar)
        repsProgressBar.progress = 20

        hrGraph = findViewById(R.id.hrGraph)
        heartRateArrayGraph.add(Random.nextInt(0, 101).toString())
        heartRateArrayGraph.add(Random.nextInt(0, 101).toString())
        heartRateArrayGraph.add(Random.nextInt(0, 101).toString())
        heartRateArrayGraph.add(Random.nextInt(0, 101).toString())
        heartRateArrayGraph.add(Random.nextInt(0, 101).toString())
        heartRateArrayGraph.add(Random.nextInt(0, 101).toString())
        heartRateArrayGraph.add(Random.nextInt(0, 101).toString())
        heartRateArrayGraph.add(Random.nextInt(0, 101).toString())
        heartRateArrayGraph.add(Random.nextInt(0, 101).toString())
        heartRateArrayGraph.add(Random.nextInt(0, 101).toString())
        heartRateArrayGraph.add(Random.nextInt(0, 101).toString())
        heartRateArrayGraph.add(Random.nextInt(0, 101).toString())
        heartRateArrayGraph.add(Random.nextInt(0, 101).toString())
        hrGraph.adapter = SparkGraphAdapter(heartRateArrayGraph.toList())


        var lastPressedTime = 0L
        goBtn.setOnClickListener {
            if(isGoBtnClickable){
                val currentTime = System.currentTimeMillis()
                duration = 0L
                if (lastPressedTime != 0L) {
                    duration = currentTime - lastPressedTime
                    println("Duration between presses: $duration ms")
                }

                lastPressedTime = currentTime

                isGoBtnClickable = false
                if (!exerciseStarted) {
                    goBtn.setImageResource(R.drawable.stop_btn)
                    exerciseStarted = true
                    val randomInt = Random.nextInt(0, 101)
                    heartRateArrayGraph.add(randomInt.toString())
                    hrGraph.adapter = SparkGraphAdapter(heartRateArrayGraph.toList())
                    hrText.text = "$randomInt"
                } else {
                    goBtn.setImageResource(R.drawable.go_btn)
                    setNumber.text = "Set $currentSet"
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

                    timeIndiceArray.clear()
                    heartRateArray.clear()
                    velocityXArray.clear()
                    velocityYArray.clear()
                    velocityZArray.clear()
                    rotationXArray.clear()
                    rotationYArray.clear()
                    rotationZArray.clear()


                    exerciseStarted = false

                }
                val cooldownTimer = object : CountDownTimer(2000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                    }

                    override fun onFinish() {
                        isGoBtnClickable = true
                    }
                }
                cooldownTimer.start()
            }
        }

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