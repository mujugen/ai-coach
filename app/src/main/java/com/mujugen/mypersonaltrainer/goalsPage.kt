package com.mujugen.mypersonaltrainer

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView

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



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        val volumeGoalsProgressBar = view.findViewById<ProgressBar>(R.id.volumeGoalsProgressBar)
        val strengthGoalsProgressBar = view.findViewById<ProgressBar>(R.id.strengthGoalsProgressBar)
        val consistencyGoalsProgressBar = view.findViewById<ProgressBar>(R.id.consistencyGoalsProgressBar)
        val weightGoalsProgressBar = view.findViewById<ProgressBar>(R.id.weightGoalsProgressBar)

        val volumeGoalsProgressText = view.findViewById<TextView>(R.id.volumeGoalsProgressText)
        val strengthGoalsProgressText = view.findViewById<TextView>(R.id.strengthGoalsProgressText)
        val consistencyGoalsProgressText = view.findViewById<TextView>(R.id.consistencyGoalsProgressText)
        val weightGoalsProgressText = view.findViewById<TextView>(R.id.weightGoalsProgressText)

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
        setGoalBtn.setOnClickListener { setGoalsPopup.visibility = View.VISIBLE }


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

                hideKeyboard(view)

                setGoalsPopup.visibility = View.INVISIBLE
            } catch (e: NumberFormatException) {
            }
        }

    }

    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment workoutPage.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            GoalsPage().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}