/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.example.test3.presentation

import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material.Text
import com.example.test3.R
import com.example.test3.presentation.theme.Test3Theme
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
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
import com.example.test3.presentation.theme.Test3Theme
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button

class MainActivity : ComponentActivity() {
    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null
    private var heartRateValue = mutableStateOf("Awaiting sensor data...")
    private var velocityValue = mutableStateOf("Awaiting sensor data...")
    private var rotationValue = mutableStateOf("Awaiting sensor data...")
    fun onButtonClick() {
        println("button clicked")
    }
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
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
                rotation = rotationValue.value,
                onClick = {onButtonClick()}
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(sensorEventListener)
    }
}

@Composable
fun WearApp(
    heartRate: String,
    velocity: String,
    rotation: String,
    onClick: () -> Unit  // Add onClick parameter
) {
    Test3Theme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Center
        ) {
            Greeting(heartRate = heartRate, velocity = velocity, rotation = rotation)
            Button(
                onClick = onClick,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp)
            ) {
                Text(text = "Click Me")
            }
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


interface Clickable {
    fun onButtonClick()
}

