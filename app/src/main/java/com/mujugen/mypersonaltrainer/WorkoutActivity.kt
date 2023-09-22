package com.mujugen.mypersonaltrainer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class WorkoutActivity : AppCompatActivity() {

    private val setNumber = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)
        val exerciseType = intent.getStringExtra("exerciseType")

        val exerciseTypeText = findViewById<TextView>(R.id.exerciseTypeText)
        val setText = findViewById<TextView>(R.id.setText)
        exerciseTypeText.text = exerciseType
        setText.text = "Set $setNumber"


        val loadProgressBar = findViewById<ProgressBar>(R.id.loadProgressBar)
        loadProgressBar.progress = 70

        val repsProgressBar = findViewById<ProgressBar>(R.id.repsProgressBar)
        repsProgressBar.progress = 20
    }
}