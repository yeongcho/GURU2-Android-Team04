package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// MainActivity : 앱 실행 시 처음 뜨는 기본 화면(Activity)

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MainActivity는 화면을 직접 보여주기보다 "진입 라우터" 역할만 수행
        val owner = appService.currentOwnerIdOrNull()

        if (owner.isNullOrBlank()) {
            // 세션이 없으면: 스플래시 → 온보딩 흐름으로
            startActivity(Intent(this, SplashActivity::class.java))
        } else {
            // 세션이 있으면: 시작 화면 → 홈으로
            startActivity(Intent(this, StartActivity::class.java))
        }

        finish()
    }
}
