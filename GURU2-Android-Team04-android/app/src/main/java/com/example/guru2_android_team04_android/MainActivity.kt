package com.example.guru2_android_team04_android

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

// MainActivity : 앱 실행 시 처음 뜨는 기본 화면(Activity)
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)

        val tv = TextView(this).apply {
            text = "App is running"
            textSize = 20f
            setPadding(40, 80, 40, 40)
        }

        root.addView(tv)
        setContentView(root)
    }
}
