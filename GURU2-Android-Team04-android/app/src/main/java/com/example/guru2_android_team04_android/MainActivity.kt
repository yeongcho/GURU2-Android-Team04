package com.example.guru2_android_team04_android

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.guru2_android_team04_android.ui.calendar.CalendarFragment  // 추가!

// MainActivity : 앱 실행 시 처음 뜨는 기본 화면(Activity)

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // 처음엔 홈 화면 보여주기
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // ★ 1. 홈 (navigation_home)
                R.id.navigation_home -> {
                    replaceFragment(HomeFragment())
                    true
                }

                // ★ 2. 일기장 (navigation_diary)
                // (일기 쓰기 화면으로 이동하게 해둘게요. 원하시면 목록 화면으로 바꿔도 됩니다!)
                R.id.navigation_diary -> {
                    val intent = Intent(this, WriteActivity::class.java)
                    startActivity(intent)
                    false
                }

                // ★ 3. 캘린더 (navigation_calendar)
                R.id.navigation_calendar -> {
                    replaceFragment(CalendarFragment())
                    true
                }

                // ★ 4. 마이페이지 (navigation_mypage)
                R.id.navigation_mypage -> {
                    replaceFragment(MyPageFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}