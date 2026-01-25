package com.example.guru2_android_team04_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.MyPageUiBinder

class MyPageActivity : AppCompatActivity() {

    private val appService by lazy { (application as MyApp).appService }
    private lateinit var binder: MyPageUiBinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)

        binder = MyPageUiBinder(this, appService)
        binder.bind()
    }

    override fun onResume() {
        super.onResume()
        // 배지 변경/프로필 변경하고 돌아왔을 때 최신 반영
        binder.bind()
    }
}
