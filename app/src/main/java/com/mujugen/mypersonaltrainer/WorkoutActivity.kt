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
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import java.text.DecimalFormat

class WorkoutActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener {

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

    private var connectedNode: Node? = null
    private lateinit var messageClient: MessageClient
    private var connectionStatus = false

    private lateinit var goBtn: ImageView
    private lateinit var hrText: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        messageClient = Wearable.getMessageClient(this)
        messageClient.addListener(this)
        connectToSmartwatch()



        val exerciseType = intent.getStringExtra("exerciseType")
        val exerciseTypeText = findViewById<TextView>(R.id.exerciseTypeText)
        goBtn = findViewById<ImageView>(R.id.goBtn)
        val setNumber = findViewById<TextView>(R.id.setText)
        hrText = findViewById<TextView>(R.id.hrText)
        exerciseTypeText.text = exerciseType
        setNumber.text = "Set $currentSet"
        val loadProgressBar = findViewById<ProgressBar>(R.id.loadProgressBar)
        loadProgressBar.progress = 70

        val repsProgressBar = findViewById<ProgressBar>(R.id.repsProgressBar)
        repsProgressBar.progress = 20

        hrGraph = findViewById(R.id.hrGraph)
        hrGraph.adapter = SparkGraphAdapter(heartRateArrayGraph.toList())

        fun goFunction(){

            if (!exerciseStarted) {
                goBtn.setImageResource(R.drawable.stop_btn)
                exerciseStarted = true
                sendMessageToSmartwatch("Go")
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

                sendMessageToSmartwatch("Stop")
            }

        }

        goBtn.setOnClickListener {
            goFunction()
        }

    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val message = String(messageEvent.data)
        if (message == "Go") {
            println("Go")
            if(!exerciseStarted){
                goBtn.performClick()
            }
        }
        if (message == "Stop") {
            println("Stop")
            if(exerciseStarted){
                goBtn.performClick()
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
                    hrText.text = "$lastNonZeroHeartRate"
                    heartRateArray.add(lastNonZeroHeartRate)
                    heartRateArrayGraph.add(lastNonZeroHeartRate)
                } else {
                    hrText.text = "$heartRate"
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

                hrGraph.adapter = SparkGraphAdapter(heartRateArrayGraph.toList())
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

