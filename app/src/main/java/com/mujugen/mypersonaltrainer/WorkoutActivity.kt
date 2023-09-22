package com.mujugen.mypersonaltrainer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

class WorkoutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)
        val exerciseType = intent.getStringExtra("exerciseType")
        println("welcome to workout activity")
        println(exerciseType)

        val myLinearLayout = findViewById<TextView>(R.id.textView5)
        myLinearLayout.text = exerciseType

    }
}