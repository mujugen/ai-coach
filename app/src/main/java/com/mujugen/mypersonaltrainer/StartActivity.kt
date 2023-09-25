package com.mujugen.mypersonaltrainer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mujugen.mypersonaltrainer.databinding.ActivityStartBinding

class StartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)

        binding.startBtn.setOnClickListener{
            println("Hello world")
        }
    }
}