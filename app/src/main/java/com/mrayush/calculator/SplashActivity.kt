package com.mrayush.calculator

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.os.postDelayed

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Perform initialization tasks in the background
        // For example, loading data or setting up UI components
        initializeMainScreen {
            // Transition to the main screen when the content is ready
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun initializeMainScreen(onInitializationComplete: () -> Unit) {
        // Simulate initialization tasks (you should replace this with your actual tasks)
        Handler(Looper.getMainLooper()).postDelayed({
            // Initialization tasks complete
            onInitializationComplete()
        }, 1800) // Simulated delay of 2 seconds
    }
}