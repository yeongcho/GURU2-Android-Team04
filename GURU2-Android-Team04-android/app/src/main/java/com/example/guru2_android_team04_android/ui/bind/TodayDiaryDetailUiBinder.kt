package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.*
import com.example.guru2_android_team04_android.core.AppResult
import com.example.guru2_android_team04_android.data.DiaryEntryReader
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.util.DateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TodayDiaryDetailUiBinder : activity_today_diary_detail.xml ↔ AppService 연동 전담 클래스
class TodayDiaryDetailUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {
    private val reader = DiaryEntryReader(activity)

    fun bind(entryId: Long) {
        if (entryId <= 0L) {
            Toast.makeText(activity, "entryId가 올바르지 않아요.", Toast.LENGTH_SHORT).show()
            activity.finish()
            return
        }

        val tvDate = activity.findViewById<TextView>(R.id.tvDiaryDayText)
        val ivMood = activity.findViewById<ImageView>(R.id.ivDetailEmotion)
        val tvMood = activity.findViewById<TextView>(R.id.tvDetailEmotionTag)
        val tvTitle = activity.findViewById<TextView>(R.id.tvDiaryTitle)
        val tvContent = activity.findViewById<TextView>(R.id.tvDiaryContent)

        val tvEdit = activity.findViewById<TextView>(R.id.tvDetailEdit)
        val tvDelete = activity.findViewById<TextView>(R.id.tvDetailDelete)
        val ivFav = activity.findViewById<ImageView>(R.id.ivDetailFavorites)

        val tvComfort = activity.findViewById<TextView>(R.id.tvNicknameMsg)
        val tvMission = activity.findViewById<TextView>(R.id.tvMissionText)

        val btnAnalysis =
            activity.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save_card)

        activity.lifecycleScope.launch(Dispatchers.IO) {
            val entry = reader.getByIdOrNull(entryId)
            if (entry == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "일기를 찾을 수 없어요.", Toast.LENGTH_SHORT).show()
                    activity.finish()
                }
                return@launch
            }

            val preview = when (val r = appService.getMindCardPreviewByEntryIdSafe(entryId)) {
                is AppResult.Success -> r.data
                is AppResult.Failure -> null
            }

            withContext(Dispatchers.Main) {
                // 날짜/일기 본문 표시
                tvDate.text = DateUtil.ymdToPrettyKo(entry.dateYmd)
                tvTitle.text = entry.title
                tvContent.text = entry.content

                ivMood.setImageResource(iconResOf(entry.mood))
                tvMood.text = "태그: ${moodKo(entry.mood)}"

                // 마음카드 프리뷰
                if (preview == null) {
                    tvComfort.text = "오늘도 기록해줘서 고마워요. 지금은 충분히 잘하고 있어요."
                    tvMission.text = "오늘의 미션: 천천히 숨 고르기"
                } else {
                    tvComfort.text = preview.comfortPreview
                    tvMission.text = "오늘의 미션: ${preview.mission}"
                }

                // 즐겨찾기 토글
                var fav = entry.isFavorite
                ivFav.setImageResource(if (fav) R.drawable.ic_favorites_o else R.drawable.ic_favorites_x)
                ivFav.setOnClickListener {
                    val owner = entry.ownerId
                    val next = !fav

                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        val ok = appService.setEntryFavorite(owner, entryId, next)
                        withContext(Dispatchers.Main) {
                            if (ok) {
                                fav = next
                                ivFav.setImageResource(if (fav) R.drawable.ic_favorites_o else R.drawable.ic_favorites_x)
                            } else {
                                Toast.makeText(activity, "즐겨찾기 변경에 실패했어요.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                // 수정
                tvEdit.setOnClickListener {
                    activity.startActivity(
                        Intent(activity, DiaryEditorActivity::class.java).apply {
                            putExtra("entryId", entryId)
                        }
                    )
                }

                // 삭제
                tvDelete.setOnClickListener {
                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        appService.deleteEntry(entryId)
                        withContext(Dispatchers.Main) {
                            activity.startActivity(Intent(activity, HomeActivity::class.java))
                            activity.finish()
                        }
                    }
                }

                // 상세 분석 보러가기
                btnAnalysis.setOnClickListener {
                    activity.startActivity(
                        Intent(activity, AnalysisStartActivity::class.java).apply {
                            putExtra("entryId", entryId)
                        }
                    )
                }
            }
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
