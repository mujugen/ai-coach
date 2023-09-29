package com.mujugen.mypersonaltrainer

import android.content.Context
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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
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
    private lateinit var buttonClickAnimation: Animation
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        val view = binding.root
        binding.initializationProgress.progress = 0
        setContentView(view)
        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        buttonClickAnimation = AnimationUtils.loadAnimation(this, R.anim.button_click_animation)

        loadSavedData()

        binding.startBtn.setOnClickListener{
            binding.startBtn.startAnimation(buttonClickAnimation)
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
            binding.nextBtn.startAnimation(buttonClickAnimation)

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
                runBlocking {
                    saveDataToDataStore()
                }
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

    private fun loadSavedData() {
        GlobalScope.launch {
            val preferences = dataStore.data.first()
            name = preferences[stringPreferencesKey("name")] ?: ""
            sex = preferences[stringPreferencesKey("sex")] ?: ""
            experience = preferences[stringPreferencesKey("experience")] ?: ""
            birthday = preferences[stringPreferencesKey("birthday")] ?: ""
        }
    }

    // Function to save data to DataStore
    private suspend fun saveDataToDataStore() {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("name")] = name
            preferences[stringPreferencesKey("sex")] = sex
            preferences[stringPreferencesKey("experience")] = experience
            preferences[stringPreferencesKey("birthday")] = birthday
            preferences[stringPreferencesKey("highest_bench_press")] = "50"
            preferences[stringPreferencesKey("highest_back_rows")] = "60"
            preferences[stringPreferencesKey("highest_bicep_curl")] = "70"
            preferences[stringPreferencesKey("highest_tricep_pushdown")] = "80"
            preferences[stringPreferencesKey("highest_lat_pulldown")] = "90"
            preferences[stringPreferencesKey("highest_chest_fly")] = "100"
            preferences[stringPreferencesKey("highest_shoulder_press")] = "110"
            preferences[stringPreferencesKey("highest_hammer_curl")] = "120"
            preferences[stringPreferencesKey("highest_all_time")] = "130"
            preferences[stringPreferencesKey("highest_all_time_exercise")] = "Bicep Curl"
            preferences[stringPreferencesKey("workout_streak")] = "150"
            preferences[stringPreferencesKey("current_weight")] = "75"
            preferences[stringPreferencesKey("starting_weight")] = "170"
            preferences[stringPreferencesKey("workout_duration")] = "180"
            preferences[stringPreferencesKey("volume_goal")] = "190"
            preferences[stringPreferencesKey("strength_goal")] = "200"
            preferences[stringPreferencesKey("consistency_goal")] = "52"
            preferences[stringPreferencesKey("weight_goal")] = "13"
            preferences[stringPreferencesKey("monday_volume")] = "50"
            preferences[stringPreferencesKey("tuesdsay_volume")] = "60"
            preferences[stringPreferencesKey("wednesday_volume")] = "70"
            preferences[stringPreferencesKey("thursday_volume")] = "80"
            preferences[stringPreferencesKey("friday_volume")] = "90"
            preferences[stringPreferencesKey("saturday_volume")] = "50"
            preferences[stringPreferencesKey("sunday_volume")] = "30"
            preferences[stringPreferencesKey("daily_bench_press_sets_left")] = "3"
            preferences[stringPreferencesKey("daily_back_rows_sets_left")] = "3"
            preferences[stringPreferencesKey("daily_bicep_curl_sets_left")] = "2"
            preferences[stringPreferencesKey("daily_tricep_pushdown_sets_left")] = "1"
            preferences[stringPreferencesKey("daily_lat_pulldown_sets_left")] = "0"
            preferences[stringPreferencesKey("daily_chest_fly_sets_left")] = "5"
            preferences[stringPreferencesKey("daily_shoulder_press_sets_left")] = "4"
            preferences[stringPreferencesKey("daily_hammer_curl_sets_left")] = "3"
        }
    }
}