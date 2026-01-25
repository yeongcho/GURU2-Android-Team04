package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            delay(1200)

            val owner = appService.currentOwnerIdOrNull()
            if (owner.isNullOrBlank()) {
                startActivity(Intent(this@SplashActivity, OnboardingActivity::class.java))
            } else {
                startActivity(Intent(this@SplashActivity, StartActivity::class.java))
            }
            finish()
        }
    }
}
