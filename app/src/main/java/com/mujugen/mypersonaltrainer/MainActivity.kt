package com.mujugen.mypersonaltrainer

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.Fragment
import com.google.android.gms.wearable.*
import com.mujugen.mypersonaltrainer.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.*

class MainActivity : AppCompatActivity(){
    private lateinit var binding: ActivityMainBinding
    private var highest_bench_press = "0"
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root

        loadSavedData()



        setContentView(view)
        replaceFragment(DailyPage())
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_item_daily -> {
                    println("Daily")
                    replaceFragment(DailyPage())
                }
                R.id.menu_item_workout -> {
                    println("Workout")
                    replaceFragment(WorkoutPage())
                }
                R.id.menu_item_goals -> {
                    println("Goals")
                    replaceFragment(GoalsPage())
                }
                R.id.menu_item_me -> {
                    println("Me")
                    replaceFragment(MePage())
                }
            }
            true
        }
    }

    override fun onPause() {
        super.onPause()
    }


    override fun onResume() {
        super.onResume()
    }


    private data class Sets(
        var exercise: String = "",
        var set: Int = 0,
        var load: Int = 0,
        var reps: Int = 0,
        var rpe: Int = 0,
        var sLoad: Int = 0,
        var sReps: Int = 0
    )


    private fun loadSavedData() {
        GlobalScope.launch {
            try {
                val preferences = dataStore.data.first()
                highest_bench_press = preferences[stringPreferencesKey("highest_bench_press")] ?: ""
                println("1234highestbp = $highest_bench_press")
            } catch (e: Exception) {
                println("Error loading data: ${e.message}")
            }

        }
    }
    private fun replaceFragment(fragment: Fragment) {
        println("replaceFragment")
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, fragment)
        fragmentTransaction.commit()
    }

}





