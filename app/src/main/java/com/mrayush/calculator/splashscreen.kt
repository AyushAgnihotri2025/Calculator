package com.mrayush.calculator

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import androidx.core.os.postDelayed

class splashscreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        getSupportActionBar()?.hide()
        setContentView(R.layout.activity_splashscreen)


        Handler(Looper.getMainLooper()).postDelayed(2000) {
            finish()
            startActivity(Intent(this, MainActivity::class.java))
        }

    }
}