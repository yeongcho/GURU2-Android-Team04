package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.data.DiaryEntryReader
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.util.DateUtil

class AnalysisDiaryActivity : AppCompatActivity() {

    private val reader by lazy { DiaryEntryReader(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_diary)

        val entryId = intent.getLongExtra("entryId", -1L)
        val entry = reader.getByIdOrNull(entryId)
        if (entry == null) {
            Toast.makeText(this, "일기를 찾을 수 없어요.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.tvDiaryDayText).text = DateUtil.todayPrettyKo()
        findViewById<TextView>(R.id.tvDetailEmotionTag).text = "태그: ${moodKo(entry.mood)}"
        findViewById<ImageView>(R.id.ivDetailEmotion).setImageResource(iconResOf(entry.mood))
        findViewById<TextView>(R.id.tvDiaryTitle).text = entry.title
        findViewById<TextView>(R.id.tvDiaryContent).text = entry.content

        findViewById<com.google.android.material.card.MaterialCardView>(R.id.btnTapContinue).setOnClickListener {
            startActivity(Intent(this, AnalysisComfortActivity::class.java).apply {
                putExtra("entryId", entryId)
            })
        }
    }

    private fun moodKo(m: Mood): String = when (m) {
        Mood.JOY -> "기쁨"
        Mood.CONFIDENCE -> "자신감"
        Mood.CALM -> "평온"
        Mood.NORMAL -> "평범"
        Mood.DEPRESSED -> "우울"
        Mood.ANGRY -> "분노"
        Mood.TIRED -> "피곤함"
    }

    private fun iconResOf(mood: Mood): Int = when (mood) {
        Mood.JOY -> R.drawable.emotion_joy
        Mood.CONFIDENCE -> R.drawable.emotion_confidence
        Mood.CALM -> R.drawable.emotion_calm
        Mood.NORMAL -> R.drawable.emotion_normal
        Mood.DEPRESSED -> R.drawable.emotion_sad
        Mood.ANGRY -> R.drawable.emotion_angry
        Mood.TIRED -> R.drawable.emotion_tired
    }
}
