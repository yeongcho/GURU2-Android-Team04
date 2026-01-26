package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// SplashActivity : 앱 실행 직후 잠깐 표시되는 스플래시 화면 Activity
// 용도:
// - 앱 로고 화면을 짧게 보여주고, 이후 다음 화면(OnboardingActivity)으로 자동 이동
// - 사용자가 앱을 켰을 때 "로딩/전환"의 흐름을 자연스럽게 만들어준다.
class SplashActivity : AppCompatActivity() {

    // onCreate : Activity가 생성될 때 최초로 호출되는 생명주기 메서드
    // 동작 흐름:
    // 1) 스플래시 레이아웃(activity_splash)을 화면에 표시
    // 2) 1.2초 대기 후(OnboardingActivity로) 화면 전환
    // 3) finish()로 스플래시 화면을 백스택에서 제거해 뒤로가기로 돌아오지 않게 한다.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 스플래시 UI 표시
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            // delay : 스플래시 노출 시간을 확보하기 위한 대기(밀리초 단위)
            // 예외처리) Thread.sleep을 쓰면 메인 스레드를 막아 ANR 위험이 있으므로, non-blocking 방식인 delay를 사용한다.
            delay(1200)

            // 다음 화면으로 이동 (온보딩/로그인/메인 진입점 등)
            startActivity(Intent(this@SplashActivity, OnboardingActivity::class.java))

            // finish : 스플래시를 종료해 백스택에 남지 않도록 한다.
            // - 사용자가 뒤로가기를 눌러도 스플래시로 되돌아오지 않는다.
            finish()
        }
    }
}
