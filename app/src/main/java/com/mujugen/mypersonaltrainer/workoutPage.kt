package com.mujugen.mypersonaltrainer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
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
class WorkoutPage : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

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
        return inflater.inflate(R.layout.fragment_workout_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val date = view.findViewById<TextView>(R.id.date)
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.US)
        date.text = dateFormat.format(currentDate)

        val workoutPageWorkoutIds = arrayOf(
            R.id.workoutPageWorkout1,
            R.id.workoutPageWorkout2,
            R.id.workoutPageWorkout3,
            R.id.workoutPageWorkout4,
            R.id.workoutPageWorkout5,
            R.id.workoutPageWorkout6,
            R.id.workoutPageWorkout7,
            R.id.workoutPageWorkout8
        )

        for (i in 0 until workoutPageWorkoutIds.size) {
            val workoutPageWorkout = view.findViewById<LinearLayout>(workoutPageWorkoutIds[i])

            workoutPageWorkout.setOnClickListener {
                val intent = Intent(requireContext(), WorkoutActivity::class.java)

                // You can customize the exercise type based on the index or any other logic you want.
                val exerciseType = when (i) {
                    0 -> "Bench Press"
                    1 -> "Back Rows"
                    2 -> "Tricep Pushdown"
                    3 -> "Bicep Curl"
                    4 -> "Lat Pulldown"
                    5 -> "Hammer Curl"
                    6 -> "Shoulder Press"
                    7 -> "Chest Fly"
                    else -> "Undefined"
                }

                intent.putExtra("exerciseType", exerciseType)
                startActivity(intent)
            }
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
            WorkoutPage().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}