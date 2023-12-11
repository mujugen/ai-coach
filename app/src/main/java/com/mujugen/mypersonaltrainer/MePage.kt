package com.mujugen.mypersonaltrainer

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import com.mujugen.mypersonaltrainer.databinding.FragmentMePageBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MePage : Fragment() {
    private var _binding: FragmentMePageBinding? = null
    private val binding get() = _binding!!
    private var param1: String? = null
    private var param2: String? = null
    private var highest_bench_press = "0"
    private var highest_back_rows = "0"
    private var highest_bicep_curl = "0"
    private var highest_tricep_pushdown = "0"
    private var highest_lat_pulldown = "0"
    private var highest_chest_fly = "0"
    private var highest_shoulder_press = "0"
    private var highest_hammer_curl = "0"
    private var highest_all_time = "0"
    private var highest_all_time_exercise = "Undefined"
    private var workout_streak = "0"
    private var current_weight = "0"
    private var name = "Ernest Khalimov"
    private var sex = "Male"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val date = view.findViewById<TextView>(R.id.date)
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.US)
        date.text = dateFormat.format(currentDate)
        runBlocking { loadData()}
        binding.highestBenchPressText.text = "$highest_bench_press kg"
        binding.highestBackRowsText.text = "$highest_back_rows kg"
        binding.highestBicepCurlText.text = "$highest_bicep_curl kg"
        binding.highestTricepPushdownText.text = "$highest_tricep_pushdown kg"
        binding.highestLatPulldownText.text = "$highest_lat_pulldown kg"
        binding.highestChestFlyText.text = "$highest_chest_fly kg"
        binding.highestShoulderPressText.text = "$highest_shoulder_press kg"
        binding.highestHammerCurlText.text = "$highest_hammer_curl kg"
        binding.highestAllTimeText.text = "$highest_all_time kg"
        binding.highestAllTimeExerciseText.text = highest_all_time_exercise
        binding.workoutStreakText.text = "$workout_streak Weeks"
        binding.currentWeightText.text = "$current_weight kg"
        binding.nameText.text = name
        if(sex == "Female"){
            binding.profilePicture.setImageResource(R.drawable.female_profile)
        }else{
            binding.profilePicture.setImageResource(R.drawable.male_profile)
        }

        val infoButton = view.findViewById<TextView>(R.id.infoButton)
        val popup = view.findViewById<FrameLayout>(R.id.popup)
        val popup1 = view.findViewById<LinearLayout>(R.id.popup1)
        val infoNextBtn1 = view.findViewById<Button>(R.id.infoNextBtn1)

        infoButton.setOnClickListener {
            popup.visibility = View.VISIBLE
            popup1.visibility = View.VISIBLE
        }

        infoNextBtn1.setOnClickListener {
            popup.visibility = View.GONE
            popup1.visibility = View.GONE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMePageBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onDestroyView() {
        super.onDestroyView()
        // Avoid memory leaks
        _binding = null
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val preferences = requireContext().dataStore.data.first()
                highest_bench_press = preferences[stringPreferencesKey("highest_bench_press")] ?: "0"
                highest_back_rows = preferences[stringPreferencesKey("highest_back_rows")] ?: "0"
                highest_bicep_curl = preferences[stringPreferencesKey("highest_bicep_curl")] ?: "0"
                highest_tricep_pushdown = preferences[stringPreferencesKey("highest_tricep_pushdown")] ?: "0"
                highest_lat_pulldown = preferences[stringPreferencesKey("highest_lat_pulldown")] ?: "0"
                highest_chest_fly = preferences[stringPreferencesKey("highest_chest_fly")] ?: "0"
                highest_shoulder_press = preferences[stringPreferencesKey("highest_shoulder_press")] ?: "0"
                highest_hammer_curl = preferences[stringPreferencesKey("highest_hammer_curl")] ?: "0"
                highest_all_time = preferences[stringPreferencesKey("highest_all_time")] ?: "0"
                highest_all_time_exercise = preferences[stringPreferencesKey("highest_all_time_exercise")] ?: "Undefined"
                workout_streak = preferences[stringPreferencesKey("workout_streak")] ?: "0"
                current_weight = preferences[stringPreferencesKey("current_weight")] ?: "0"
                name = preferences[stringPreferencesKey("name")] ?: "Ernest Khalimov"
                sex = preferences[stringPreferencesKey("sex")] ?: "Male"
            } catch (e: Exception) {
                println("Error loading data: ${e.message}")
            }
        }
    }



}