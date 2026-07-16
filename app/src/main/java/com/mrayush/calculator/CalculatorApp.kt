package com.mrayush.calculator

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class CalculatorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(readNightMode(this))
    }

    companion object {
        /** Resolve the day/night preference. */
        fun readNightMode(context: Context): Int {
            val prefs = context.getSharedPreferences("DayNight", MODE_PRIVATE)
            return when (prefs.getString("My_DayNight", "").orEmpty()) {
                "yes" -> AppCompatDelegate.MODE_NIGHT_YES
                "no" -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        }
    }
}
