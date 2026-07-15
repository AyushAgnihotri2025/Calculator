package com.mrayush.calculator

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import java.util.Calendar

class AboutUs : AppCompatActivity() {

    private val email = "mrayushcontact@gmail.com"
    private val website = "https://calculator.mrayush.me"
    private val youtubeChannel = "UCxFLhp74KkKk-l9gw1_ggcQ"
    private val instagramUser = "AyushAgnihotri2025"

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_us)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        findViewById<LinearLayout>(R.id.rowEmail).setOnClickListener { sendEmail() }
        findViewById<LinearLayout>(R.id.rowWebsite).setOnClickListener { openUrl(website) }
        findViewById<LinearLayout>(R.id.rowYoutube).setOnClickListener {
            openUrl("https://www.youtube.com/channel/$youtubeChannel")
        }
        findViewById<LinearLayout>(R.id.rowPlayStore).setOnClickListener { openPlayStore() }
        findViewById<LinearLayout>(R.id.rowInstagram).setOnClickListener {
            openUrl("https://www.instagram.com/$instagramUser")
        }

        findViewById<TextView>(R.id.copyrightText).text = getString(
            R.string.about_made_with,
            Calendar.getInstance()[Calendar.YEAR]
        )
    }

    // Mirror MainActivity's day/night preference so the About page matches the
    // theme the user picked (localNightMode is per-activity, so it isn't inherited).
    private fun applySavedTheme() {
        val dayNight = getSharedPreferences("DayNight", MODE_PRIVATE)
            .getString("My_DayNight", "")
        delegate.localNightMode = if (dayNight == "yes") {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
    }

    private fun sendEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.appName))
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            openUrl("mailto:$email")
        }
    }

    private fun openPlayStore() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
        } catch (e: ActivityNotFoundException) {
            openUrl("https://play.google.com/store/apps/details?id=$packageName")
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (e: ActivityNotFoundException) {
            // No handler available; silently ignore.
        }
    }
}
