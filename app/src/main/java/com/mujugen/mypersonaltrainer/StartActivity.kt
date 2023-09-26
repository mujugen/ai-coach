package com.mujugen.mypersonaltrainer

import android.content.Intent
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.RadioButton
import com.mujugen.mypersonaltrainer.databinding.ActivityStartBinding

class StartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartBinding
    private var hasName = false
    private var hasSex = false
    private var hasExperience = false
    private var hasAge = false
    private var currentPage = "Start"
    private lateinit var fadeInAnimation: Animation
    private lateinit var fadeOutAnimation: Animation
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        val view = binding.root
        binding.initializationProgress.progress = 0
        setContentView(view)
        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)

        binding.startBtn.setOnClickListener{
            if(!hasName || !hasSex || !hasExperience || !hasAge ){
                currentPage = "Name"
                binding.startPage.visibility = View.GONE
                binding.initializationPage.visibility = View.VISIBLE
                binding.initializationPage.startAnimation(fadeInAnimation)
                binding.nameLayout.visibility = View.VISIBLE
            }
        }

        binding.nextBtn.setOnClickListener {
            if(currentPage == "Name"){
                switchPage(binding.nameLayout,binding.sexLayout)
                binding.initializationProgress.progress = 25
                currentPage = "Sex"
            }else if(currentPage == "Sex"){
                switchPage(binding.sexLayout,binding.experienceLayout)
                binding.initializationProgress.progress = 50
                currentPage = "Experience"
            }else if(currentPage == "Experience"){
                switchPage(binding.experienceLayout,binding.ageLayout)
                binding.initializationProgress.progress = 75
                currentPage = "Age"
            }else if(currentPage == "Age"){
                binding.experienceLayout.visibility = View.GONE
                binding.initializationPage.visibility = View.GONE
                binding.initializationProgress.progress = 100
                currentPage = "Start"
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }

        binding.sexRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedRadio = findViewById<RadioButton>(checkedId)
            if (selectedRadio.text == "Male") {
                binding.maleRadioButton.setBackgroundResource(R.drawable.rounded_blue)
                binding.femaleRadioButton.setBackgroundResource(R.drawable.rounded_white)
            }else{
                binding.femaleRadioButton.setBackgroundResource(R.drawable.rounded_blue)
                binding.maleRadioButton.setBackgroundResource(R.drawable.rounded_white)

            }
        }

    }

    fun switchPage(currentPage: View, nextPage: View) {
        //currentPage.startAnimation(fadeOutAnimation)
        currentPage.visibility = View.GONE
        nextPage.visibility = View.VISIBLE
        nextPage.startAnimation(fadeInAnimation)
    }

    override fun onBackPressed() {
        if (currentPage != "Start") {
            if (currentPage == "Age") {
                // If you're on the "Age" page, go back to the "Experience" page.
                switchPage(binding.ageLayout, binding.experienceLayout)
                binding.initializationProgress.progress = 50
                currentPage = "Experience"
            } else if (currentPage == "Experience") {
                // If you're on the "Experience" page, go back to the "Sex" page.
                switchPage(binding.experienceLayout, binding.sexLayout)
                binding.initializationProgress.progress = 25
                currentPage = "Sex"
            } else if (currentPage == "Sex") {
                // If you're on the "Sex" page, go back to the "Name" page.
                switchPage(binding.sexLayout, binding.nameLayout)
                binding.initializationProgress.progress = 0
                currentPage = "Name"
            } else if (currentPage == "Name") {
                binding.nameLayout.visibility = View.GONE
                switchPage(binding.initializationPage, binding.startPage)
                binding.initializationProgress.progress = 0
                currentPage = "Start"
            }
        } else {
            super.onBackPressed()
        }
    }
}