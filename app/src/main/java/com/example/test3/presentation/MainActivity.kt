package com.example.test3.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.test3.R
import com.example.test3.presentation.theme.Test3Theme
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp



class MainActivity : ComponentActivity() {
    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null
    private var wakeLock: WakeLock? = null
    private var heartRateValue = mutableStateOf("Awaiting sensor data...")
    private var velocityValue = mutableStateOf("Awaiting sensor data...")
    private var rotationValue = mutableStateOf("Awaiting sensor data...")

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                when (it.sensor.type) {
                    Sensor.TYPE_HEART_RATE -> {
                        heartRateValue.value = it.values[0].toString()
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        velocityValue.value = "X: ${"%.1f".format(it.values[0])}, Y: ${"%.1f".format(it.values[1])}, Z: ${"%.1f".format(it.values[2])}"
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        rotationValue.value = "X: ${"%.1f".format(it.values[0])}, Y: ${"%.1f".format(it.values[1])}, Z: ${"%.1f".format(it.values[2])}"
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "MyApp::MyWakelockTag")
        wakeLock?.acquire()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                sensorManager.registerListener(sensorEventListener, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
                sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
                sensorManager.registerListener(sensorEventListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL)
            } else {
                heartRateValue.value = "Permission not granted"
                velocityValue.value = "Permission not granted"
                rotationValue.value = "Permission not granted"
            }
        }

        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) -> {
                sensorManager.registerListener(sensorEventListener, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
                sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
                sensorManager.registerListener(sensorEventListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
            else -> {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BODY_SENSORS)) {
                    // Show an explanation to the user.
                } else {
                    // Request permission.
                    requestPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
                }
            }
        }

        setContent {
            WearApp(
                heartRate = heartRateValue.value,
                velocity = velocityValue.value,
                rotation = rotationValue.value
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.release()
        sensorManager.unregisterListener(sensorEventListener)
    }
}

@Composable
fun WearApp(heartRate: String, velocity: String, rotation: String) {
    Test3Theme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Center
        ) {
            Greeting(heartRate = heartRate, velocity = velocity, rotation = rotation)
        }
    }
}

@Composable
fun Greeting(heartRate: String, velocity: String, rotation: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        textAlign = TextAlign.Left,
        color = MaterialTheme.colors.primary,
        fontSize = 12.sp,  // Set the font size here
        text = "Heart rate: $heartRate bpm\nVelocity: $velocity\nRotation: $rotation"
    )
}




@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Preview heart rate", "Preview velocity", "Preview rotation")
}
