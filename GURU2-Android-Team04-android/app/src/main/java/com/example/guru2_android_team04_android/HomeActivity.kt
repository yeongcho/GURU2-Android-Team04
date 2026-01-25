package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.core.AppResult
import com.example.guru2_android_team04_android.util.DateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val tvDiaryDayText = findViewById<TextView>(R.id.tvDiaryDayText)
        val tvBannerTitle = findViewById<TextView>(R.id.tv_banner_title)
        val tvNicknameMsg = findViewById<TextView>(R.id.tvNicknameMsg)
        val tvMissionText = findViewById<TextView>(R.id.tvMissionText)

        // 날짜 표시
        tvDiaryDayText.text = DateUtil.todayPrettyKo()

        lifecycleScope.launch(Dispatchers.IO) {
            val profile = appService.getUserProfile()
            val ownerId = profile.ownerId

            // 지난달 정보(배너 문구)
            val lastYm = DateUtil.previousMonthYm() // "yyyy-MM"
            val topTag = appService.getLastMonthTopTag(ownerId)

            // 오늘 일기 여부(오늘 entry 찾아서 마음카드 프리뷰 반영)
            val todayYmd = DateUtil.todayYmd()
            val ym = todayYmd.take(7)
            val todayEntry = appService.getEntriesByMonth(ownerId, ym).firstOrNull { it.dateYmd == todayYmd }

            val preview = if (todayEntry != null) {
                when (val r = appService.getMindCardPreviewByEntryIdSafe(todayEntry.entryId)) {
                    is AppResult.Success -> r.data
                    is AppResult.Failure -> null
                }
            } else null

            withContext(Dispatchers.Main) {
                val monthNum = lastYm.substring(5, 7).toIntOrNull() ?: 0
                tvBannerTitle.text = "${profile.nickname}님의 ${monthNum}월 감정 요약"

                // (두 번째 줄 TextView는 id가 없어서 여기선 그대로 두고,
                //  필요하면 XML에 id 하나만 추가하면 topTag도 꽂아줄 수 있어)

                if (preview == null) {
                    tvNicknameMsg.text = "아직 일기를 작성하지 않았어요."
                    tvMissionText.text = "오늘의 미션: '이야기 들려주기' 눌러보기"
                } else {
                    tvNicknameMsg.text = preview.comfortPreview
                    tvMissionText.text = "오늘의 미션: ${preview.mission}"
                }

                // 이야기 들려주기 버튼 분기
                val btn = findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btn_today_write)
                btn.setOnClickListener {
                    if (todayEntry == null) {
                        startActivity(Intent(this@HomeActivity, DiaryEditorActivity::class.java))
                    } else {
                        startActivity(Intent(this@HomeActivity, TodayDiaryDetailActivity::class.java).apply {
                            putExtra("entryId", todayEntry.entryId)
                        })
                    }
                }
            }
        }
    }
}
