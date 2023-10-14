package com.mujugen.mypersonaltrainer

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
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

/**
 * A simple [Fragment] subclass.
 * Use the [workoutPage.newInstance] factory method to
 * create an instance of this fragment.
 */
class GoalsPage : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var volumeProgress = 400
    private var strengthProgress = 320
    private var consistencyProgress = 8
    private var currentWeight = 95

    private var startingWeight = 100
    private var volumeGoal = 800
    private var strengthGoal = 400
    private var consistencyGoal = 12
    private var weightGoal = 85
    private var weightChangeGoal = kotlin.math.abs(startingWeight - weightGoal)
    private var weightChangeProgress = kotlin.math.abs(currentWeight - startingWeight)


    private lateinit var buttonClickAnimation: Animation



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buttonClickAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.button_click_animation)

        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_goals_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val date = view.findViewById<TextView>(R.id.date)
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.US)
        date.text = dateFormat.format(currentDate)

        val volumeGoalsProgressBar = view.findViewById<ProgressBar>(R.id.volumeGoalsProgressBar)
        val strengthGoalsProgressBar = view.findViewById<ProgressBar>(R.id.strengthGoalsProgressBar)
        val consistencyGoalsProgressBar = view.findViewById<ProgressBar>(R.id.consistencyGoalsProgressBar)
        val weightGoalsProgressBar = view.findViewById<ProgressBar>(R.id.weightGoalsProgressBar)
        val volumeGoalsProgressText = view.findViewById<TextView>(R.id.volumeGoalsProgressText)
        val strengthGoalsProgressText = view.findViewById<TextView>(R.id.strengthGoalsProgressText)
        val consistencyGoalsProgressText = view.findViewById<TextView>(R.id.consistencyGoalsProgressText)
        val weightGoalsProgressText = view.findViewById<TextView>(R.id.weightGoalsProgressText)

        runBlocking {  loadData()}


        weightChangeGoal = kotlin.math.abs(startingWeight - weightGoal)
        weightChangeProgress = kotlin.math.abs(currentWeight - startingWeight)
        volumeGoalsProgressBar.progress = ((volumeProgress.toFloat() / volumeGoal) * 100).toInt()
        strengthGoalsProgressBar.progress = ((strengthProgress.toFloat() / strengthGoal) * 100).toInt()
        consistencyGoalsProgressBar.progress = ((consistencyProgress.toFloat() / consistencyGoal) * 100).toInt()
        weightGoalsProgressBar.progress = ((weightChangeProgress.toFloat() / weightChangeGoal) * 100).toInt()
        volumeGoalsProgressText.text = "$volumeProgress reps/$volumeGoal reps"
        strengthGoalsProgressText.text = "$strengthProgress kg/$strengthGoal kg"
        consistencyGoalsProgressText.text = "$consistencyProgress weeks/$consistencyGoal weeks"
        weightGoalsProgressText.text = "$currentWeight kg/$weightGoal kg"


        val setGoalBtn = view.findViewById<Button>(R.id.setGoalBtn)
        val confirmGoalBtn = view.findViewById<Button>(R.id.confirmGoalBtn)


        val setGoalsPopup = view.findViewById<FrameLayout>(R.id.setGoalsPopup)
        setGoalBtn.setOnClickListener {
            setGoalBtn.startAnimation(buttonClickAnimation)
            setGoalsPopup.visibility = View.VISIBLE
        }


        val setGoalsVolumeText = view.findViewById<EditText>(R.id.setGoalsVolumeText)
        val setGoalsStrengthText = view.findViewById<EditText>(R.id.setGoalsStrengthText)
        val setGoalsConsistencyText = view.findViewById<EditText>(R.id.setGoalsConsistencyText)
        val setGoalsWeightText = view.findViewById<EditText>(R.id.setGoalsWeightText)

        // Set the initial text values
        setGoalsVolumeText.setText(volumeGoal.toString())
        setGoalsStrengthText.setText(strengthGoal.toString())
        setGoalsConsistencyText.setText(consistencyGoal.toString())
        setGoalsWeightText.setText(weightGoal.toString())

        confirmGoalBtn.setOnClickListener {
            val volumeText = setGoalsVolumeText.text.toString()
            val strengthText = setGoalsStrengthText.text.toString()
            val consistencyText = setGoalsConsistencyText.text.toString()
            val weightText = setGoalsWeightText.text.toString()

            try {
                volumeGoal = volumeText.toInt()
                strengthGoal = strengthText.toInt()
                consistencyGoal = consistencyText.toInt()
                weightGoal = weightText.toInt()
                startingWeight = currentWeight
                weightChangeGoal = kotlin.math.abs(startingWeight - weightGoal)
                weightChangeProgress = kotlin.math.abs(currentWeight - startingWeight)

                lifecycleScope.launch {
                    requireContext().dataStore.edit { preferences ->
                        preferences[stringPreferencesKey("volume_goal")] = volumeGoal.toString()
                        preferences[stringPreferencesKey("strength_goal")] = strengthGoal.toString()
                        preferences[stringPreferencesKey("consistency_goal")] = consistencyGoal.toString()
                        preferences[stringPreferencesKey("weight_goal")] = weightGoal.toString()
                        preferences[stringPreferencesKey("starting_weight")] = startingWeight.toString()
                    }
                }

                volumeGoalsProgressBar.progress = ((volumeProgress.toFloat() / volumeGoal) * 100).toInt()
                strengthGoalsProgressBar.progress = ((strengthProgress.toFloat() / strengthGoal) * 100).toInt()
                consistencyGoalsProgressBar.progress = ((consistencyProgress.toFloat() / consistencyGoal) * 100).toInt()
                weightGoalsProgressBar.progress = ((weightChangeProgress.toFloat() / weightChangeGoal) * 100).toInt()

                volumeGoalsProgressText.text = "$volumeProgress reps/$volumeGoal reps"
                strengthGoalsProgressText.text = "$strengthProgress kg/$strengthGoal kg"
                consistencyGoalsProgressText.text = "$consistencyProgress weeks/$consistencyGoal weeks"
                weightGoalsProgressText.text = "$currentWeight kg/$weightGoal kg"

                hideKeyboard(view)

                setGoalsPopup.visibility = View.INVISIBLE
            } catch (e: NumberFormatException) {
            }
        }

    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val preferences = requireContext().dataStore.data.first()
                volumeGoal = preferences[stringPreferencesKey("volume_goal")]?.toInt() ?: 0
                strengthGoal = preferences[stringPreferencesKey("strength_goal")]?.toInt() ?: 0
                consistencyGoal = preferences[stringPreferencesKey("consistency_goal")]?.toInt() ?: 0
                weightGoal = preferences[stringPreferencesKey("weight_goal")]?.toInt() ?: 0
                startingWeight = preferences[stringPreferencesKey("starting_weight")]?.toInt() ?: 0
                currentWeight = preferences[stringPreferencesKey("current_weight")]?.toInt() ?: 0
                consistencyProgress = preferences[stringPreferencesKey("workout_streak")]?.toInt() ?: 0
                val monday_volume = preferences[stringPreferencesKey("monday_volume")]?.toInt() ?: 0
                val tuesday_volume = preferences[stringPreferencesKey("tuesday_volume")]?.toInt() ?: 0
                val wednesday_volume = preferences[stringPreferencesKey("wednesday_volume")]?.toInt() ?: 0
                val thursday_volume = preferences[stringPreferencesKey("thursday_volume")]?.toInt() ?: 0
                val friday_volume = preferences[stringPreferencesKey("friday_volume")]?.toInt() ?: 0
                val saturday_volume = preferences[stringPreferencesKey("saturday_volume")]?.toInt() ?: 0
                val sunday_volume = preferences[stringPreferencesKey("sunday_volume")]?.toInt() ?: 0
                volumeProgress = monday_volume + tuesday_volume + wednesday_volume + thursday_volume + friday_volume + saturday_volume + sunday_volume
                val highest_bench_press = preferences[stringPreferencesKey("highest_bench_press")] ?.toInt()?: 0
                val highest_back_rows = preferences[stringPreferencesKey("highest_back_rows")]?.toInt() ?: 0
                val highest_bicep_curl = preferences[stringPreferencesKey("highest_bicep_curl")]?.toInt() ?: 0
                val highest_tricep_pushdown = preferences[stringPreferencesKey("highest_tricep_pushdown")]?.toInt() ?: 0
                val highest_lat_pulldown = preferences[stringPreferencesKey("highest_lat_pulldown")]?.toInt() ?: 0
                val highest_chest_fly = preferences[stringPreferencesKey("highest_chest_fly")]?.toInt() ?: 0
                val highest_shoulder_press = preferences[stringPreferencesKey("highest_shoulder_press")]?.toInt() ?: 0
                val highest_hammer_curl = preferences[stringPreferencesKey("highest_hammer_curl")]?.toInt() ?: 0
                strengthProgress = highest_bench_press + highest_back_rows + highest_bicep_curl + highest_tricep_pushdown + highest_lat_pulldown + highest_chest_fly + highest_shoulder_press + highest_hammer_curl


            } catch (e: Exception) {
                println("Error loading data: ${e.message}")
            }
        }
    }

    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}