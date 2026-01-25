package com.example.guru2_android_team04_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.MindCardArchiveUiBinder

class MindCardArchiveActivity : AppCompatActivity() {

    private val appService by lazy { (application as MyApp).appService }
    private lateinit var binder: MindCardArchiveUiBinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mindcard_archive)

        binder = MindCardArchiveUiBinder(this, appService)
        binder.bind()
    }

    override fun onResume() {
        super.onResume()
        // 즐겨찾기 해제/상세 보고 돌아왔을 때 갱신
        binder.bind()
    }
}
