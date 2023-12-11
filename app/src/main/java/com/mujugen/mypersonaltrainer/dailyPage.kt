package com.mujugen.mypersonaltrainer

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Color
import android.graphics.Typeface
import android.widget.FrameLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.NumberPicker
import androidx.core.content.res.ResourcesCompat
import androidx.datastore.preferences.core.edit

class DailyPage : Fragment() {
    private var monday_volume = 0
    private var tuesday_volume = 0
    private var wednesday_volume = 0
    private var thursday_volume = 0
    private var friday_volume = 0
    private var saturday_volume = 0
    private var sunday_volume = 0
    private var today_volume = 0
    private var current_weight = 0
    private var daily_bench_press_sets_left = 0
    private var daily_back_rows_sets_left = 0
    private var daily_bicep_curl_sets_left = 0
    private var daily_tricep_pushdown_sets_left = 0
    private var daily_lat_pulldown_sets_left = 0
    private var daily_chest_fly_sets_left = 0
    private var daily_shoulder_press_sets_left = 0
    private var daily_hammer_curl_sets_left = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_daily_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val date = view.findViewById<TextView>(R.id.date)
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.US)
        date.text = dateFormat.format(currentDate)

        runBlocking { loadData() }
        val dailyWorkoutContainer = view.findViewById<LinearLayout>(R.id.dailyWorkoutContainer)
        if (daily_bench_press_sets_left > 1) {
            dailyWorkoutContainer.addView(createExerciseLayout("Bench Press", "$daily_bench_press_sets_left sets"))
        }
        if (daily_back_rows_sets_left > 1) {
            dailyWorkoutContainer.addView(createExerciseLayout("Back Rows", "$daily_back_rows_sets_left sets"))
        }
        if (daily_bicep_curl_sets_left > 1) {
            dailyWorkoutContainer.addView(createExerciseLayout("Bicep Curls", "$daily_bicep_curl_sets_left sets"))
        }
        if (daily_tricep_pushdown_sets_left > 1) {
            dailyWorkoutContainer.addView(createExerciseLayout("Tricep Pushdown", "$daily_tricep_pushdown_sets_left sets"))
        }
        if (daily_lat_pulldown_sets_left > 1) {
            dailyWorkoutContainer.addView(createExerciseLayout("Lat Pulldown", "$daily_lat_pulldown_sets_left sets"))
        }
        if (daily_chest_fly_sets_left > 1) {
            dailyWorkoutContainer.addView(createExerciseLayout("Chest Fly", "$daily_chest_fly_sets_left sets"))
        }
        if (daily_shoulder_press_sets_left > 1) {
            dailyWorkoutContainer.addView(createExerciseLayout("Shoulder Press", "$daily_shoulder_press_sets_left sets"))
        }
        if (daily_hammer_curl_sets_left > 1) {
            dailyWorkoutContainer.addView(createExerciseLayout("Hammer Curl", "$daily_hammer_curl_sets_left sets"))
        }

        val currentWeight = view.findViewById<TextView>(R.id.currentWeight)
        val todayVolumeText = view.findViewById<TextView>(R.id.todayVolume)
        currentWeight.text = "$current_weight kg"
        todayVolumeText.text = "$today_volume reps"

        val density = resources.displayMetrics.density
        val maxHeight = (100 * density).toInt()  // 100dp in pixels
        val minHeight = (10 * density).toInt()   // 10dp in pixels
        val maxVolume = listOf(monday_volume, tuesday_volume, wednesday_volume, thursday_volume, friday_volume, saturday_volume, sunday_volume).maxOrNull() ?: 1
        val ratio = maxHeight.toDouble() / maxVolume.toDouble()
        val dailyPageVolumeBar1 = view.findViewById<View>(R.id.dailyPageVolumeBar1)
        val dailyPageVolumeBar2 = view.findViewById<View>(R.id.dailyPageVolumeBar2)
        val dailyPageVolumeBar3 = view.findViewById<View>(R.id.dailyPageVolumeBar3)
        val dailyPageVolumeBar4 = view.findViewById<View>(R.id.dailyPageVolumeBar4)
        val dailyPageVolumeBar5 = view.findViewById<View>(R.id.dailyPageVolumeBar5)
        val dailyPageVolumeBar6 = view.findViewById<View>(R.id.dailyPageVolumeBar6)
        val dailyPageVolumeBar7 = view.findViewById<View>(R.id.dailyPageVolumeBar7)
        val infoButton = view.findViewById<TextView>(R.id.infoButton)
        val popup = view.findViewById<FrameLayout>(R.id.popup)
        val popup1 = view.findViewById<LinearLayout>(R.id.popup1)
        val popup2 = view.findViewById<LinearLayout>(R.id.popup2)
        val weightBoxDemo = view.findViewById<LinearLayout>(R.id.weightBoxDemo)
        val infoNextBtn1 = view.findViewById<Button>(R.id.infoNextBtn1)
        val infoNextBtn2 = view.findViewById<Button>(R.id.infoNextBtn2)

        val currentDayOfWeek = SimpleDateFormat("EEEE", Locale.US).format(currentDate)

        when (currentDayOfWeek) {
            "Sunday" -> {
                monday_volume = 0
                tuesday_volume = 0
                wednesday_volume = 0
                thursday_volume = 0
                friday_volume = 0
                saturday_volume = 0
            }
            "Monday" -> {
                tuesday_volume = 0
                wednesday_volume = 0
                thursday_volume = 0
                friday_volume = 0
                saturday_volume = 0
            }
            "Tuesday" -> {
                wednesday_volume = 0
                thursday_volume = 0
                friday_volume = 0
                saturday_volume = 0
            }
            "Wednesday" -> {
                thursday_volume = 0
                friday_volume = 0
                saturday_volume = 0
            }
            "Thursday" -> {
                friday_volume = 0
                saturday_volume = 0
            }
            "Friday" -> {
                saturday_volume = 0
            }
            "Saturday" -> {}
        }

        dailyPageVolumeBar1.layoutParams.height = computeBarHeight(sunday_volume, ratio, minHeight)
        dailyPageVolumeBar2.layoutParams.height = computeBarHeight(monday_volume, ratio, minHeight)
        dailyPageVolumeBar3.layoutParams.height = computeBarHeight(tuesday_volume, ratio, minHeight)
        dailyPageVolumeBar4.layoutParams.height = computeBarHeight(wednesday_volume, ratio, minHeight)
        dailyPageVolumeBar5.layoutParams.height = computeBarHeight(thursday_volume, ratio, minHeight)
        dailyPageVolumeBar6.layoutParams.height = computeBarHeight(friday_volume, ratio, minHeight)
        dailyPageVolumeBar7.layoutParams.height = computeBarHeight(saturday_volume, ratio, minHeight)

        setVolumeBarBackgroundTint(dailyPageVolumeBar1, "Sunday", currentDayOfWeek)
        setVolumeBarBackgroundTint(dailyPageVolumeBar2, "Monday", currentDayOfWeek)
        setVolumeBarBackgroundTint(dailyPageVolumeBar3, "Tuesday", currentDayOfWeek)
        setVolumeBarBackgroundTint(dailyPageVolumeBar4, "Wednesday", currentDayOfWeek)
        setVolumeBarBackgroundTint(dailyPageVolumeBar5, "Thursday", currentDayOfWeek)
        setVolumeBarBackgroundTint(dailyPageVolumeBar6, "Friday", currentDayOfWeek)
        setVolumeBarBackgroundTint(dailyPageVolumeBar7, "Saturday", currentDayOfWeek)

        val weightBox = view.findViewById<LinearLayout>(R.id.weightBox)
        weightBox.setOnLongClickListener {
            showWeightPicker()
            true
        }

        infoButton.setOnClickListener {
            popup.visibility = View.VISIBLE
            popup1.visibility = View.VISIBLE
        }

        infoNextBtn1.setOnClickListener {
            popup1.visibility = View.GONE
            popup2.visibility = View.VISIBLE
            weightBoxDemo.visibility = View.VISIBLE
        }

        infoNextBtn2.setOnClickListener {
            popup2.visibility = View.GONE
            weightBoxDemo.visibility = View.GONE
            popup.visibility = View.GONE
        }

    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val preferences = requireContext().dataStore.data.first()
                daily_bench_press_sets_left = preferences[stringPreferencesKey("daily_bench_press_sets_left")]?.toInt() ?: 0
                daily_back_rows_sets_left = preferences[stringPreferencesKey("daily_back_rows_sets_left")]?.toInt() ?: 0
                daily_bicep_curl_sets_left = preferences[stringPreferencesKey("daily_bicep_curl_sets_left")]?.toInt() ?: 0
                daily_tricep_pushdown_sets_left = preferences[stringPreferencesKey("daily_tricep_pushdown_sets_left")]?.toInt() ?: 0
                daily_lat_pulldown_sets_left = preferences[stringPreferencesKey("daily_lat_pulldown_sets_left")]?.toInt() ?: 0
                daily_chest_fly_sets_left = preferences[stringPreferencesKey("daily_chest_fly_sets_left")]?.toInt() ?: 0
                daily_shoulder_press_sets_left = preferences[stringPreferencesKey("daily_shoulder_press_sets_left")]?.toInt() ?: 0
                daily_hammer_curl_sets_left = preferences[stringPreferencesKey("daily_hammer_curl_sets_left")]?.toInt() ?: 0
                current_weight = preferences[stringPreferencesKey("current_weight")]?.toInt() ?: 0
                monday_volume = preferences[stringPreferencesKey("monday_volume")]?.toInt() ?: 0
                tuesday_volume = preferences[stringPreferencesKey("tuesday_volume")]?.toInt() ?: 0
                wednesday_volume = preferences[stringPreferencesKey("wednesday_volume")]?.toInt() ?: 0
                thursday_volume = preferences[stringPreferencesKey("thursday_volume")]?.toInt() ?: 0
                friday_volume = preferences[stringPreferencesKey("friday_volume")]?.toInt() ?: 0
                saturday_volume = preferences[stringPreferencesKey("saturday_volume")]?.toInt() ?: 0
                sunday_volume = preferences[stringPreferencesKey("sunday_volume")]?.toInt() ?: 0

                val currentDate = Date()
                val currentDayOfWeek = SimpleDateFormat("EEEE", Locale.US).format(currentDate)
                today_volume = when (currentDayOfWeek) {
                    "Sunday" ->  preferences[stringPreferencesKey("sunday_volume")]?.toInt() ?: 0
                    "Monday" ->   preferences[stringPreferencesKey("monday_volume")]?.toInt() ?: 0
                    "Tuesday" ->   preferences[stringPreferencesKey("tuesday_volume")]?.toInt() ?: 0
                    "Wednesday" ->   preferences[stringPreferencesKey("wednesday_volume")]?.toInt() ?: 0
                    "Thursday" ->   preferences[stringPreferencesKey("thursday_volume")]?.toInt() ?: 0
                    "Friday" ->   preferences[stringPreferencesKey("friday_volume")]?.toInt() ?: 0
                    "Saturday" ->   preferences[stringPreferencesKey("saturday_volume")]?.toInt() ?: 0
                    else -> 0
                }

            } catch (e: Exception) {
                println("Error loading data: ${e.message}")
            }
        }
    }

    // Function to show the NumberPicker for selecting a new weight.
    private fun showWeightPicker() {
        val numberPicker = NumberPicker(requireContext())
        numberPicker.minValue = 30  // Set your desired min weight
        numberPicker.maxValue = 200  // Set your desired max weight
        numberPicker.value = current_weight

        val weightPickerDialog = AlertDialog.Builder(requireContext())
            .setTitle("Select New Weight")
            .setView(numberPicker)
            .setPositiveButton("OK") { _, _ ->
                val selectedWeight = numberPicker.value
                updateCurrentWeight(selectedWeight)
                saveWeightToDatastore(selectedWeight)
            }
            .setNegativeButton("Cancel", null)
            .create()

        weightPickerDialog.show()
    }

    // Function to update the displayed currentWeight based on the selected value.
    private fun updateCurrentWeight(weight: Int) {
        current_weight = weight
        val currentWeight = view?.findViewById<TextView>(R.id.currentWeight)
        currentWeight?.text = "$current_weight kg"
    }

    // Function to save the updated weight to the datastore.
    private fun saveWeightToDatastore(weight: Int) {
        val weightKey = stringPreferencesKey("current_weight")
        lifecycleScope.launch {
            requireContext().dataStore.edit { preferences ->
                preferences[weightKey] = weight.toString()
            }
        }
    }
    fun computeBarHeight(volume: Int, ratio: Double, minHeight: Int): Int {
        val computedHeight = (volume * ratio).toInt()
        return Math.max(computedHeight, minHeight)  // Ensure it's not below minHeight
    }

    private fun setVolumeBarBackgroundTint(bar: View, barDay: String, currentDay: String) {
        if (barDay == currentDay) {
            bar.backgroundTintList = null  // Removes the background tint
        } else {
            bar.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#757575"))
        }
    }

    private fun createExerciseLayout(exerciseName: String, sets: String): LinearLayout {
        val context = requireContext()
        val typeface = ResourcesCompat.getFont(context, R.font.pt_sans)
        val exerciseLayout = LinearLayout(context)
        exerciseLayout.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        ).apply {
            setMargins(50, 0, 30, 0)
        }
        exerciseLayout.orientation = LinearLayout.HORIZONTAL

        val exerciseNameTextView = TextView(context)
        exerciseNameTextView.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(10, 10, 0, 0)
        }
        exerciseNameTextView.typeface = typeface
        exerciseNameTextView.text = exerciseName
        exerciseNameTextView.setTextColor((Color.parseColor("#262626")))
        exerciseNameTextView.textSize = 20f
        exerciseNameTextView.setTypeface(null, Typeface.BOLD)
        exerciseLayout.addView(exerciseNameTextView)

        val setsTextView = TextView(context)
        setsTextView.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            1f
        ).apply {
            setMargins(10, 10, 40, 0)
        }
        setsTextView.typeface = typeface
        setsTextView.text = sets
        setsTextView.textAlignment = TextView.TEXT_ALIGNMENT_VIEW_END
        setsTextView.setTextColor(Color.parseColor("#FFFFFF"))  // Use parseColor for consistency
        setsTextView.textSize = 17f
        setsTextView.setTypeface(null, Typeface.BOLD)
        exerciseLayout.addView(setsTextView)

        return exerciseLayout
    }

}