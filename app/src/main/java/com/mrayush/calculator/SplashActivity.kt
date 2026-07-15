package com.mrayush.calculator

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Neutralise the Android 12+ system splash: showing a blank background
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        animateEntrance()

        // Perform initialization tasks in the background
        // (loading data, setting up UI components, etc.)
        initializeMainScreen {
            // Transition to the main screen when the content is ready
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    /** Staggered "power-on" reveal for the device, name, tagline and loader. */
    private fun animateEntrance() {
        val display = findViewById<View>(R.id.splashDisplay)
        val name = findViewById<View>(R.id.splashName)
        val tagline = findViewById<View>(R.id.splashTagline)
        val footer = findViewById<View>(R.id.splashFooter)

        // LCD panel: fade + subtle scale-in, like the screen lighting up.
        display.alpha = 0f
        display.scaleX = 0.86f
        display.scaleY = 0.86f
        display.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(120).setDuration(560)
            .setInterpolator(OvershootInterpolator(1.4f)).start()

        // Name, tagline and footer rise into place, one after another.
        riseIn(name, delay = 380)
        riseIn(tagline, delay = 480)
        riseIn(footer, delay = 620)
    }

    private fun riseIn(view: View, delay: Long) {
        view.alpha = 0f
        view.translationY = 40f
        view.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(480)
            .setInterpolator(DecelerateInterpolator()).start()
    }

    private fun initializeMainScreen(onInitializationComplete: () -> Unit) {
        // Simulate initialization tasks (replace with actual startup work).
        Handler(Looper.getMainLooper()).postDelayed({
            onInitializationComplete()
        }, 2000)
    }
}
