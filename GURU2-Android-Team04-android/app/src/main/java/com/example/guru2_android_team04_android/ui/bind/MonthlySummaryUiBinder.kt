package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.*
import com.example.guru2_android_team04_android.data.db.AppDbHelper
import com.example.guru2_android_team04_android.data.db.MonthlyDao
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.data.model.ko
import com.example.guru2_android_team04_android.util.DateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// MonthlySummaryUiBinder : 월간 요약 화면(activity_monthly_summary.xml) <-> 데이터(AppService/DB) 연결 전담 클래스
// 용도:
// - yearMonth(yyyy-MM)에 해당하는 월간 요약 데이터를 화면에 표시한다.
// - 로컬 DB 캐시(MonthlyDao)를 먼저 조회해 빠르게 표시한다.
// - 캐시가 없고 "지난 달"이면 AppService로 요약 생성/확보를 시도한다.
// - 탭 이동(요약/캘린더/리스트), 월 이동(이전/다음) 이벤트를 연결한다.
// 동작 흐름:
// 1) 상단 헤더/탭/스티커 UI를 yearMonth 기준으로 세팅한다.
// 2) IO에서 월간 요약 캐시 조회
// 3) 캐시가 없고 지난달이면 ensureLastMonthMonthlySummary로 생성 시도
// 4) Main에서 summary가 있으면 UI 채우고, 없으면 빈 상태 안내를 표시한다.
class MonthlySummaryUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {
    // helper : MonthlyDao가 사용할 SQLite 접근 헬퍼
    // - 월간 요약 캐시를 로컬 DB에서 조회할 때 사용한다.
    private val helper = AppDbHelper(activity.applicationContext)

    fun bind(yearMonth: String) {

        // tvMonth : 현재 월/년도를 보여주는 헤더 텍스트("01월 2026")
        val tvMonth = activity.findViewById<TextView>(R.id.tvMonth)

        // btnPrev/btnNext : 이전 달/다음 달로 이동하는 버튼
        val btnPrev = activity.findViewById<ImageView>(R.id.btnPrevMonth)
        val btnNext = activity.findViewById<ImageView>(R.id.btnNextMonth)

        // 탭 버튼(요약/캘린더/리스트)
        val btnTabSummary = activity.findViewById<TextView>(R.id.btnTabSummary)
        val btnTabCalendar = activity.findViewById<TextView>(R.id.btnTabCalendar)
        val btnTabList = activity.findViewById<TextView>(R.id.btnTabList)

        // ivSticker : 월 스티커 이미지(월별 리소스로 변경)
        val ivSticker = activity.findViewById<ImageView>(R.id.ivMonthSticker)

        // scroll : 요약 데이터가 있을 때 보여줄 콘텐츠 영역
        val scroll = activity.findViewById<ScrollView>(R.id.scrollViewContent)

        // tvEmpty : 요약 데이터가 없을 때 보여줄 빈 상태 안내 문구
        val tvEmpty = activity.findViewById<TextView>(R.id.tv_empty_message)

        // 요약 콘텐츠 뷰들
        val tvTitle = activity.findViewById<TextView>(R.id.tv_summary_title)
        val tvMost = activity.findViewById<TextView>(R.id.tv_most_emotion)
        val ivMost = activity.findViewById<ImageView>(R.id.iv_most_emotion)
        val tvFlow = activity.findViewById<TextView>(R.id.iv_most_flow)
        val tvOneLine = activity.findViewById<TextView>(R.id.tv_one_line_summary)
        val tvDetail = activity.findViewById<TextView>(R.id.tv_detail_summary)
        val tvKw1 = activity.findViewById<TextView>(R.id.tv_keyword_1)
        val tvKw2 = activity.findViewById<TextView>(R.id.tv_keyword_2)
        val tvKw3 = activity.findViewById<TextView>(R.id.tv_keyword_3)

        // yearMonth 파싱(yyyy-MM -> (연,월))
        val (yy, mm) = parseYm(yearMonth)

        // 헤더 텍스트 및 월 스티커 세팅
        tvMonth.text = "${mm.toString().padStart(2, '0')}월 $yy"
        ivSticker.setImageResource(stickerResOf(mm))
        tvTitle.text = "${mm}월 월간 요약"

        // 탭 클릭 이벤트
        // 요약 탭은 현재 화면이므로 이동 없음
        btnTabSummary.setOnClickListener { }

        // 캘린더 화면으로 이동하면서 yearMonth를 그대로 전달한다(같은 월 유지)
        btnTabCalendar.setOnClickListener {
            activity.startActivity(Intent(activity, DiaryCalendarActivity::class.java).apply {
                putExtra("yearMonth", yearMonth)
            })
            activity.finish()
        }

        // 리스트 화면으로 이동하면서 yearMonth를 그대로 전달한다(같은 월 유지)
        btnTabList.setOnClickListener {
            activity.startActivity(Intent(activity, DiaryListActivity::class.java).apply {
                putExtra("yearMonth", yearMonth)
            })
            activity.finish()
        }

        // 월 이동(이전/다음)
        // shiftYm으로 yearMonth를 이동시킨 뒤 MonthlySummaryActivity를 다시 열어 갱신한다.
        btnPrev.setOnClickListener {
            val prev = shiftYm(yearMonth, -1)
            activity.startActivity(Intent(activity, MonthlySummaryActivity::class.java).apply {
                putExtra("yearMonth", prev)
            })
            activity.finish()
        }
        btnNext.setOnClickListener {
            val next = shiftYm(yearMonth, +1)
            activity.startActivity(Intent(activity, MonthlySummaryActivity::class.java).apply {
                putExtra("yearMonth", next)
            })
            activity.finish()
        }

        // 월간 요약 데이터 로딩
        activity.lifecycleScope.launch(Dispatchers.IO) {

            // ownerId : 사용자별 월간 요약 캐시를 구분하기 위한 키
            val ownerId = appService.getUserProfile().ownerId

            // 1) MonthlyDao로 로컬 캐시 먼저 조회
            val cached = MonthlyDao(helper.readableDatabase).get(ownerId, yearMonth)

            // 2) 캐시가 없으면
            // - 지난달인 경우: 월간 요약이 아직 생성되지 않았을 수 있으므로 생성/확보를 시도
            // - 그 외 월: 자동 생성 정책이 아니면 null 처리(빈 상태 UI로 안내).
            val summary = if (cached != null) {
                cached
            } else {
                if (yearMonth == DateUtil.previousMonthYm()) {
                    appService.ensureLastMonthMonthlySummary(ownerId)
                } else null
            }

            withContext(Dispatchers.Main) {

                // 예외처리) summary가 없으면 스크롤 콘텐츠를 숨기고 빈 상태 안내를 보여준다.
                if (summary == null) {
                    scroll.visibility = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "${mm}월 요약이 아직 작성되지 않았어요.\n일기를 더 써보세요!"
                    return@withContext
                }

                // summary가 있으면 콘텐츠를 표시한다.
                scroll.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE

                // 한 줄 요약 / 상세 요약 표시
                tvOneLine.text = summary.oneLineSummary
                tvDetail.text = summary.detailSummary

                // dominantMood : 최다 감정을 한글로 표시 + 아이콘 매핑
                tvMost.text = summary.dominantMood.ko()
                ivMost.setImageResource(iconResOf(summary.dominantMood))

                // emotionFlow : "JOY -> TIRED -> DEPRESSED" 같은 문자열을 한글로 변환해서 표시
                tvFlow.text = "'${emotionFlowKo(summary.emotionFlow)}'"

                // keywords : 최대 3개만 칩 형태로 출력하고, 없는 칩은 숨긴다.
                val kws = summary.keywords.take(3)
                val chips = listOf(tvKw1, tvKw2, tvKw3)

                for (i in chips.indices) {
                    val tv = chips[i]
                    tv.text = kws.getOrNull(i).orEmpty()
                    tv.visibility = if (tv.text.isNullOrBlank()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    // emotionFlowKo : 감정 흐름 문자열을 한글로 변환한다.
    // 예: "JOY -> TIRED -> DEPRESSED -> 회복" => "기쁨 -> 피곤함 -> 우울 -> 회복"
    // 예외처리) Mood enum으로 변환되지 않는 단어(예: "회복")는 그대로 유지한다.
    private fun emotionFlowKo(flowRaw: String): String {
        if (flowRaw.isBlank()) return flowRaw

        // "->" 기준으로 토큰 분리 후 trim으로 공백을 정리한다.
        val tokens = flowRaw.split("->").map { it.trim() }.filter { it.isNotEmpty() }

        return tokens.joinToString(" -> ") { code ->
            // 예외처리) 서버/데이터에서 "Tried" 같은 오타가 들어오면 TIRED로 정규화한다.
            val normalized = if (code.equals("Tried", ignoreCase = true)) "TIRED" else code

            runCatching { Mood.valueOf(normalized.uppercase()) }
                .getOrNull()
                ?.ko()
                ?: code
        }
    }

    // parseYm : "yyyy-MM"을 (연,월)로 파싱한다.
    // 예외처리) 파싱 실패 시 기본값(1970-01)을 사용해 앱 크래시를 방지한다.
    private fun parseYm(ym: String): Pair<Int, Int> {
        val y = ym.take(4).toIntOrNull() ?: 1970
        val m = ym.drop(5).take(2).toIntOrNull() ?: 1
        return y to m
    }

    // shiftYm : 현재 yyyy-MM에서 deltaMonth만큼 이동한 yyyy-MM을 만든다.
    // 예: shiftYm("2026-01", -1) => "2025-12"
    private fun shiftYm(ym: String, deltaMonth: Int): String {
        val (y, m) = parseYm(ym)
        val c = Calendar.getInstance()
        c.set(y, m - 1, 1)
        c.add(Calendar.MONTH, deltaMonth)
        val ny = c.get(Calendar.YEAR)
        val nm = c.get(Calendar.MONTH) + 1
        return "%04d-%02d".format(ny, nm)
    }

    // stickerResOf : 월(1~12)을 월 스티커 리소스로 매핑한다.
    // 예외처리) 범위를 벗어나면 1월 스티커로 처리한다.
    private fun stickerResOf(month: Int): Int = when (month) {
        1 -> R.drawable.month_1
        2 -> R.drawable.month_2
        3 -> R.drawable.month_3
        4 -> R.drawable.month_4
        5 -> R.drawable.month_5
        6 -> R.drawable.month_6
        7 -> R.drawable.month_7
        8 -> R.drawable.month_8
        9 -> R.drawable.month_9
        10 -> R.drawable.month_10
        11 -> R.drawable.month_11
        12 -> R.drawable.month_12
        else -> R.drawable.month_1
    }

    // iconResOf : Mood에 해당하는 감정 아이콘 리소스를 반환한다.
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