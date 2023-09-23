package com.mujugen.mypersonaltrainer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class WorkoutActivity : AppCompatActivity() {

    private var setNumber = 1
    private var exerciseStarted = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)
        val exerciseType = intent.getStringExtra("exerciseType")

        val exerciseTypeText = findViewById<TextView>(R.id.exerciseTypeText)
        val goBtn = findViewById<ImageView>(R.id.goBtn)
        val setText = findViewById<TextView>(R.id.setText)
        exerciseTypeText.text = exerciseType
        setText.text = "Set $setNumber"


        val loadProgressBar = findViewById<ProgressBar>(R.id.loadProgressBar)
        loadProgressBar.progress = 70

        val repsProgressBar = findViewById<ProgressBar>(R.id.repsProgressBar)
        repsProgressBar.progress = 20

        goBtn.setOnClickListener {
            if(!exerciseStarted){
                goBtn.setImageResource(R.drawable.stop_btn)
                exerciseStarted = true
            }
            else{
                goBtn.setImageResource(R.drawable.go_btn)
                exerciseStarted = false
            }

        }

    }
}