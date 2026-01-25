package com.example.guru2_android_team04_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.TodayDiaryDetailUiBinder

class TodayDiaryDetailActivity : AppCompatActivity() {

    private val appService by lazy { (application as MyApp).appService }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_today_diary_detail)

        val entryId = intent.getLongExtra("entryId", -1L)
        TodayDiaryDetailUiBinder(this, appService).bind(entryId)
    }
}
