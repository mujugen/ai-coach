package com.mujugen.mypersonaltrainer

import android.content.Intent
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.mujugen.mypersonaltrainer.databinding.ActivityStartBinding

class StartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartBinding
    private var hasName = false
    private var hasSex = false
    private var hasExperience = false
    private var hasAge = false
    private var currentPage = "Start"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        val view = binding.root
        binding.initializationProgress.progress = 0
        setContentView(view)

        binding.startBtn.setOnClickListener{
            if(!hasName || !hasSex || !hasExperience || !hasAge ){
                currentPage = "Name"
                binding.startPage.visibility = View.GONE
                binding.initializationPage.visibility = View.VISIBLE
                binding.nameLayout.visibility = View.VISIBLE
            }
        }

        binding.nextBtn.setOnClickListener {
            if(currentPage == "Name"){
                binding.nameLayout.visibility = View.GONE
                binding.sexLayout.visibility = View.VISIBLE
                binding.initializationProgress.progress = 25
                currentPage = "Sex"
            }else if(currentPage == "Sex"){
                binding.sexLayout.visibility = View.GONE
                binding.experienceLayout.visibility = View.VISIBLE
                binding.initializationProgress.progress = 50
                currentPage = "Experience"
            }else if(currentPage == "Experience"){
                binding.experienceLayout.visibility = View.GONE
                binding.ageLayout.visibility = View.VISIBLE
                binding.initializationProgress.progress = 75
                currentPage = "Age"
            }else if(currentPage == "Age"){
                binding.experienceLayout.visibility = View.GONE
                binding.ageLayout.visibility = View.GONE
                binding.initializationPage.visibility = View.GONE
                binding.initializationProgress.progress = 100
                currentPage = "Start"
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }

    }
}