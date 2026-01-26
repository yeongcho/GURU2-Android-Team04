package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// MainActivity : 앱의 "진입점(Launcher)" 역할을 하는 Activity
// 용도:
// - 앱 아이콘을 눌렀을 때 가장 먼저 실행되는 Activity로 사용한다.
// - 실제 첫 화면을 MainActivity에서 직접 보여주지 않고, SplashActivity로 즉시 이동시킨다.
// 동작 흐름:
// 1) onCreate에서 SplashActivity를 startActivity로 실행한다.
// 2) MainActivity는 finish()로 즉시 종료하여 백스택에 남지 않게 한다.
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SplashActivity로 이동
        // - 스플래시 화면에서 초기화/분기(로그인 여부 등)를 처리하고 다음 화면으로 안내한다.
        startActivity(Intent(this, SplashActivity::class.java))

        // MainActivity는 역할이 중계뿐이므로 종료한다.
        // - 뒤로가기 눌렀을 때 다시 MainActivity로 돌아오는 것을 방지한다.
        finish()
    }
}
