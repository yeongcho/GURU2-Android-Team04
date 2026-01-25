package com.example.guru2_android_team04_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.DiaryEditorUiBinder

class DiaryEditorActivity : AppCompatActivity() {

    private val appService by lazy { (application as MyApp).appService }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_editor)

        val entryId = intent.getLongExtra("entryId", -1L).takeIf { it > 0L }
        DiaryEditorUiBinder(this, appService).bind(entryId)
    }
}
