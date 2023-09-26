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
import android.widget.Toast
class StartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartBinding
    private var name = ""
    private var sex = ""
    private var experience = ""
    private var birthday  = ""
    private var age  = ""
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
            if(name == "" || sex == "" || experience == "" || birthday == "" ){
                println("2")
                currentPage = "Name"
                binding.startPage.visibility = View.GONE
                binding.initializationPage.visibility = View.VISIBLE
                binding.initializationPage.startAnimation(fadeInAnimation)
                binding.nameLayout.visibility = View.VISIBLE
            }else{
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }

        binding.nextBtn.setOnClickListener {
            if(currentPage == "Name"){
                println("Name = ${binding.nameTextInput.text}")
                if(binding.nameTextInput.text.toString() == ""){
                    showToast("Enter your name")
                    return@setOnClickListener
                }
                name = binding.nameTextInput.text.toString()
                switchPage(binding.nameLayout,binding.sexLayout)
                binding.initializationProgress.progress = 25
                currentPage = "Sex"
            }else if(currentPage == "Sex"){
                val selectedSexRadioButtonId = binding.sexRadioGroup.checkedRadioButtonId

                if (selectedSexRadioButtonId == -1) {
                    showToast("Select your sex")
                    return@setOnClickListener
                }

                val selectedSexRadioButton = findViewById<RadioButton>(selectedSexRadioButtonId)
                sex = selectedSexRadioButton.text.toString()

                switchPage(binding.sexLayout,binding.experienceLayout)
                binding.initializationProgress.progress = 50
                currentPage = "Experience"
            }else if(currentPage == "Experience"){
                val selectedExperienceRadioButtonId = binding.experienceRadioGroup.checkedRadioButtonId

                if (selectedExperienceRadioButtonId == -1) {
                    showToast("Select your experience level")
                    return@setOnClickListener
                }
                val selectedExperienceRadioButton = findViewById<RadioButton>(selectedExperienceRadioButtonId)
                experience = selectedExperienceRadioButton.text.toString()


                switchPage(binding.experienceLayout,binding.ageLayout)
                binding.initializationProgress.progress = 75
                currentPage = "Age"
            }else if(currentPage == "Age"){
                val year = binding.ageDatePicker.year.toInt()
                val month = binding.ageDatePicker.month.toInt()
                val day = binding.ageDatePicker.dayOfMonth.toInt()
                birthday = "$year $month $day"
                age = calculateAge(year, month, day)

                if (age.toInt() < 5) {
                    showToast("Ages 5 and above only")
                    return@setOnClickListener
                }
                binding.ageLayout.visibility = View.GONE
                binding.initializationPage.visibility = View.GONE
                binding.startPage.visibility = View.VISIBLE
                binding.initializationProgress.progress = 0
                currentPage = "Start"
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }

            println("Name: $name Sex: $sex Experience: $experience Age: $age")
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
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    fun switchPage(currentPage: View, nextPage: View) {
        //currentPage.startAnimation(fadeOutAnimation)
        currentPage.visibility = View.GONE
        nextPage.visibility = View.VISIBLE
        nextPage.startAnimation(fadeInAnimation)
    }

    private fun calculateAge(birthYear: Int, birthMonth: Int, birthDay: Int): String {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1 // Month is zero-based
        val currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)

        var age = currentYear - birthYear

        if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
            age--
        }

        return age.toString()
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