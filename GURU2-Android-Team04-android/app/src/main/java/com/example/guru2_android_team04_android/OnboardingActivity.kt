package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        findViewById<Button>(R.id.btn_login).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        findViewById<Button>(R.id.btn_guest_start).setOnClickListener {
            // 비회원 세션 시작 → ownerId 세팅 완료
            appService.startAnonymousSession()
            startActivity(Intent(this, StartActivity::class.java))
            finish()
        }
    }
}
