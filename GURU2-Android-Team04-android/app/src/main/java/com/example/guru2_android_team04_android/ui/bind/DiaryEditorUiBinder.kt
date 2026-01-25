package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.AppService
import com.example.guru2_android_team04_android.R
import com.example.guru2_android_team04_android.TodayDiaryDetailActivity
import com.example.guru2_android_team04_android.core.AppResult
import com.example.guru2_android_team04_android.data.DiaryEntryReader
import com.example.guru2_android_team04_android.data.model.DiaryEntry
import com.example.guru2_android_team04_android.data.model.MindCardPreview
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.util.DateUtil
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// DiaryEditorUiBinder : activity_diary_editor.xml ↔ AppService 연동 전담 클래스
class DiaryEditorUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {
    private val reader = DiaryEntryReader(activity)

    fun bind(editEntryId: Long?) {
        val tvDate = activity.findViewById<TextView>(R.id.tvDiaryDayText)
        val rgMood = activity.findViewById<RadioGroup>(R.id.rg_mood)
        val ivPreview = activity.findViewById<ImageView>(R.id.iv_emotion_preview)
        val etTitle = activity.findViewById<EditText>(R.id.et_title)
        val etContent = activity.findViewById<EditText>(R.id.et_content)
        val btnSave = activity.findViewById<MaterialButton>(R.id.btn_save_card)

        // 날짜 표시
        tvDate.text = DateUtil.todayPrettyKo()

        // 수정 모드면 기존 값 채우기
        val existing = editEntryId?.let { reader.getByIdOrNull(it) }
        if (existing != null) {
            etTitle.setText(existing.title)
            etContent.setText(existing.content)
            setMoodUi(existing.mood, rgMood, ivPreview)
        }

        rgMood.setOnCheckedChangeListener { _, _ ->
            val mood = moodFromUi(rgMood.checkedRadioButtonId)
            ivPreview.setImageResource(iconResOf(mood))
        }

        btnSave.setOnClickListener {
            val title = etTitle.text?.toString().orEmpty()
            val content = etContent.text?.toString().orEmpty()
            val mood = moodFromUi(rgMood.checkedRadioButtonId)

            activity.lifecycleScope.launch(Dispatchers.IO) {
                val owner = appService.currentOwnerIdOrNull() ?: appService.startAnonymousSession()
                val now = System.currentTimeMillis()

                val entry = if (existing != null) {
                    existing.copy(
                        title = title,
                        content = content,
                        mood = mood,
                        updatedAt = now
                    )
                } else {
                    DiaryEntry(
                        entryId = 0L,
                        ownerId = owner,
                        dateYmd = DateUtil.todayYmd(),
                        title = title,
                        content = content,
                        mood = mood,
                        tags = emptyList(),
                        isFavorite = false,
                        isTemporary = owner.startsWith("ANON_"),
                        createdAt = now,
                        updatedAt = now
                    )
                }

                val r = if (existing == null) {
                    appService.saveEntryAndPrepareMindCardSafe(entry)
                } else {
                    // 수정은 분석까지 강제하지 않고 저장만(원하면 여기서 runAnalysisSafe 호출 가능)
                    val id = appService.upsertEntry(entry)
                    AppResult.Success(
                        MindCardPreview(
                            entryId = id,
                            dateYmd = entry.dateYmd,
                            title = entry.title,
                            mood = entry.mood,
                            tags = entry.tags,
                            comfortPreview = "수정이 완료됐어요.",
                            mission = "천천히 숨 고르기"
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    when (r) {
                        is AppResult.Success -> {
                            activity.startActivity(
                                Intent(activity, TodayDiaryDetailActivity::class.java).apply {
                                    putExtra("entryId", r.data.entryId)
                                }
                            )
                            activity.finish()
                        }

                        is AppResult.Failure -> {
                            Toast.makeText(
                                activity,
                                r.error.userMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun setMoodUi(mood: Mood, rg: RadioGroup, iv: ImageView) {
        val id = when (mood) {
            Mood.JOY -> R.id.rb_joy
            Mood.CONFIDENCE -> R.id.rb_confidence
            Mood.CALM -> R.id.rb_calm
            Mood.NORMAL -> R.id.rb_normal
            Mood.DEPRESSED -> R.id.rb_depression
            Mood.ANGRY -> R.id.rb_anger
            Mood.TIRED -> R.id.rb_fatigue
        }
        rg.check(id)
        iv.setImageResource(iconResOf(mood))
    }

    private fun moodFromUi(checkedId: Int): Mood = when (checkedId) {
        R.id.rb_joy -> Mood.JOY
        R.id.rb_confidence -> Mood.CONFIDENCE
        R.id.rb_calm -> Mood.CALM
        R.id.rb_normal -> Mood.NORMAL
        R.id.rb_depression -> Mood.DEPRESSED
        R.id.rb_anger -> Mood.ANGRY
        R.id.rb_fatigue -> Mood.TIRED
        else -> Mood.NORMAL
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
