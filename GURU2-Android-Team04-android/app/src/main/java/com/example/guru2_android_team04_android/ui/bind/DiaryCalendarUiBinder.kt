package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.*
import com.example.guru2_android_team04_android.data.model.Mood
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// DiaryCalendarUiBinder : 감정 캘린더 화면(activity_diary_calendar.xml) <-> AppService 연결 전담 클래스
// 용도(XML 기준):
// - 상단 월 헤더(이전/다음)와 탭(요약/캘린더/리스트) 클릭 이벤트를 연결한다.
// - 월 스티커 이미지를 현재 월에 맞게 변경한다.
// - 캘린더 그리드(7열×5행=35칸)에 날짜 + 감정 이모티콘을 배치한다.
// - 월간 감정 분포를 파이차트(MPAndroidChart)로 시각화한다.
// 설계:
// - 데이터 조회는 IO 스레드, UI 반영은 Main 스레드에서 수행한다.
// - 그리드의 각 칸은 XML에 tvDay1..tvDay35 / ivEmotion1..ivEmotion35 형태로 id가 정의되어 있다고 가정한다.
class DiaryCalendarUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {

    // bind : 특정 월(yearMonth="yyyy-MM")을 기준으로 화면 전체를 세팅하고 데이터를 바인딩한다.
    fun bind(yearMonth: String) {
        // 월 이동 헤더 영역(View 참조)
        val tvMonth = activity.findViewById<TextView>(R.id.tvMonth)
        val btnPrev = activity.findViewById<ImageView>(R.id.btnPrevMonth)
        val btnNext = activity.findViewById<ImageView>(R.id.btnNextMonth)

        // 상단 탭(요약/캘린더/리스트) 버튼
        val btnTabSummary = activity.findViewById<TextView>(R.id.btnTabSummary)
        val btnTabCalendar = activity.findViewById<TextView>(R.id.btnTabCalendar)
        val btnTabList = activity.findViewById<TextView>(R.id.btnTabList)

        // 월 스티커 / 캘린더 그리드 / 파이차트
        val ivSticker = activity.findViewById<ImageView>(R.id.ivMonthSticker)
        val grid = activity.findViewById<GridLayout>(R.id.calendarGrid)
        val pie = activity.findViewById<com.github.mikephil.charting.charts.PieChart>(R.id.pieChart)

        // yearMonth("yyyy-MM")를 year, month로 파싱하여 상단 표시 텍스트/스티커에 반영
        val (yy, mm) = parseYm(yearMonth)
        tvMonth.text = "${mm.toString().padStart(2, '0')}월 $yy"
        ivSticker.setImageResource(stickerResOf(mm))

        // 탭 이동 이벤트
        // - 요약/리스트 탭은 해당 Activity로 이동하면서 yearMonth를 전달해 동일한 달을 유지한다.
        btnTabSummary.setOnClickListener {
            activity.startActivity(Intent(activity, MonthlySummaryActivity::class.java).apply {
                putExtra("yearMonth", yearMonth)
            })
        }
        // 현재 화면이 캘린더 탭이므로 동작 없음
        btnTabCalendar.setOnClickListener { }
        btnTabList.setOnClickListener {
            activity.startActivity(Intent(activity, DiaryListActivity::class.java).apply {
                putExtra("yearMonth", yearMonth)
            })
        }

        // 월 이동 이벤트
        // - 이동 시 새 Activity를 띄우고 현재 Activity는 finish()로 정리한다.
        btnPrev.setOnClickListener {
            val prev = shiftYm(yearMonth, -1)
            activity.startActivity(Intent(activity, DiaryCalendarActivity::class.java).apply {
                putExtra("yearMonth", prev)
            })
            activity.finish()
        }
        btnNext.setOnClickListener {
            val next = shiftYm(yearMonth, +1)
            activity.startActivity(Intent(activity, DiaryCalendarActivity::class.java).apply {
                putExtra("yearMonth", next)
            })
            activity.finish()
        }

        // 해당 월의 일기 데이터 로드 -> 캘린더 그리드 + 파이차트에 반영
        activity.lifecycleScope.launch(Dispatchers.IO) {
            // 현재 로그인/세션 사용자(ownerId) 기준으로 해당 월의 일기를 가져온다.
            val ownerId = appService.getUserProfile().ownerId

            // 월별 일기 리스트 조회 후 날짜순 정렬
            val entries = appService.getEntriesByMonth(ownerId, yearMonth).sortedBy { it.dateYmd }

            // 날짜("yyyy-MM-dd") → DiaryEntry 매핑 (캘린더 칸에서 빠르게 찾기 위함)
            val byYmd = entries.associateBy { it.dateYmd }

            // Mood별 개수 집계 (파이차트 조각 데이터)
            val moodCount = entries.groupingBy { it.mood }.eachCount()

            withContext(Dispatchers.Main) {
                // 캘린더 그리드(35칸)를 날짜 배치 규칙에 맞게 채운다.
                bindCalendarGrid(grid, yy, mm, byYmd)

                // 파이차트 스타일(디자인 반영)
                val typeface: Typeface? = ResourcesCompat.getFont(activity, R.font.nanumsquare_eb)
                val textColor = Color.parseColor("#45694B")

                // 파이차트 입력값 생성: (count, "기쁨/우울" 등 라벨)
                val pieEntries = buildList {
                    for ((m, c) in moodCount) add(PieEntry(c.toFloat(), moodKo(m)))
                }

                if (pieEntries.isEmpty()) {
                    // 예외처리) 해당 월에 일기가 하나도 없으면 차트 데이터를 만들 수 없으므로
                    // "데이터 없음" 안내 텍스트를 표시한다.
                    pie.clear()
                    pie.setNoDataText("이번 달 데이터가 없어요.")
                    pie.setNoDataTextColor(textColor)
                    if (typeface != null) pie.setNoDataTextTypeface(typeface)
                } else {
                    val ds = PieDataSet(pieEntries, "")

                    // 각 감정 라벨(한글)에 대해 고정된 색상을 매핑해 디자인을 통일한다.
                    ds.colors = pieEntries.map { e -> labelColorOf(e.label) }

                    // 숫자(value) 표시는 제거(라벨만 보여주기)
                    ds.setDrawValues(false)

                    // PieChart 기본 설정
                    pie.data = PieData(ds)
                    pie.description.isEnabled = false
                    pie.legend.isEnabled = false

                    // 조각 라벨(기쁨/우울 등)
                    pie.setDrawEntryLabels(true)
                    pie.setEntryLabelColor(textColor)
                    pie.setEntryLabelTextSize(12f)
                    if (typeface != null) pie.setEntryLabelTypeface(typeface)

                    // 원 형태로 보기 설정
                    pie.setUsePercentValues(false)
                    pie.setDrawHoleEnabled(true)
                    pie.holeRadius = 0f
                    pie.transparentCircleRadius = 60f

                    // 변경 사항 반영
                    pie.invalidate()
                }
            }
        }
    }

    // bindCalendarGrid : 7열×5행(총 35칸) 그리드에 날짜와 감정 아이콘을 배치한다.
    // 동작 규칙:
    // - 첫째 날의 요일에 따라 offset(앞 공백 칸)을 계산한다.
    // - dayNum이 1..말일 범위이면 날짜를 표시하고, 해당 날짜에 일기가 있으면 감정 아이콘을 표시한다.
    // - 일기가 없으면 아이콘을 숨기고 "일기가 없어요" 토스트를 보여준다.
    // - 날짜 범위 밖(앞 공백/뒤 공백)은 빈칸 처리하고 클릭 비활성화한다.
    private fun bindCalendarGrid(
        grid: GridLayout,
        year: Int,
        month: Int, // 1..12
        byYmd: Map<String, com.example.guru2_android_team04_android.data.model.DiaryEntry>
    ) {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // firstDow : 해당 월 1일의 요일 (1=일요일..7=토요일)
        val firstDow = cal.get(Calendar.DAY_OF_WEEK)

        // offset : 그리드에서 1일이 시작되기 전 공백 칸 개수 (일요일 시작 기준)
        val offset = (firstDow - Calendar.SUNDAY) // 0..6

        // 해당 월의 마지막 날짜(28~31)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 1..35) {
            // 예외처리) 그리드 child 개수가 부족한 경우 null이 될 수 있으므로 continue 처리한다.
            val cell = grid.getChildAt(i - 1) ?: continue

            // 각 셀 내부에는 tvDay{i}, ivEmotion{i} 형태의 id가 있다고 가정한다.
            // 예: i=1이면 tvDay1 / ivEmotion1
            val tvId = activity.resources.getIdentifier("tvDay$i", "id", activity.packageName)
            val ivId = activity.resources.getIdentifier("ivEmotion$i", "id", activity.packageName)

            val tvDay = cell.findViewById<TextView>(tvId)
            val ivEmotion = cell.findViewById<ImageView>(ivId)

            // dayNum : 해당 셀이 실제 며칠을 의미하는지 계산
            // - offset만큼 앞에서부터 비워두고, 그 다음 칸부터 1일이 들어간다.
            val dayNum = i - offset

            if (dayNum in 1..daysInMonth) {
                // 유효한 날짜(이번 달 범위)인 경우
                tvDay.text = dayNum.toString()

                // 날짜 문자열 생성: "yyyy-MM-dd"
                val ymd = "%04d-%02d-%02d".format(year, month, dayNum)

                val entry = byYmd[ymd]
                if (entry == null) {
                    // 해당 날짜에 일기가 없으면 감정 아이콘을 숨김
                    ivEmotion.visibility = View.INVISIBLE

                    // 빈 날짜를 눌렀을 때는 "일기가 없어요" 안내만 제공
                    cell.setOnClickListener {
                        Toast.makeText(activity, "이 날짜의 일기가 없어요.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 해당 날짜에 일기가 있으면 감정 아이콘 표시 + 클릭 시 상세 화면 이동
                    ivEmotion.visibility = View.VISIBLE
                    ivEmotion.setImageResource(iconResOf(entry.mood))

                    cell.setOnClickListener {
                        activity.startActivity(
                            Intent(activity, ArchiveDiaryDetailActivity::class.java).apply {
                                putExtra("entryId", entry.entryId)
                            }
                        )
                    }
                }
            } else {
                // 이번 달 범위 밖(앞 공백/뒤 공백)
                tvDay.text = ""
                ivEmotion.visibility = View.INVISIBLE

                // 범위 밖 칸은 클릭 이벤트 제거(오동작 방지)
                cell.setOnClickListener(null)
            }
        }
    }

    // parseYm : "yyyy-MM" 문자열을 (year, month)로 파싱한다.
    // 예외처리) 형식이 깨졌다면 기본값(1970-01)을 사용한다.
    private fun parseYm(ym: String): Pair<Int, Int> {
        val y = ym.take(4).toIntOrNull() ?: 1970
        val m = ym.drop(5).take(2).toIntOrNull() ?: 1
        return y to m
    }

    // shiftYm : yearMonth("yyyy-MM")를 기준으로 deltaMonth만큼 월을 이동한 "yyyy-MM"을 반환한다.
    // - Calendar.add(Calendar.MONTH, deltaMonth)를 이용해 연도 변경(12월 -> 1월 등)도 자동 처리한다.
    private fun shiftYm(ym: String, deltaMonth: Int): String {
        val (y, m) = parseYm(ym)
        val c = Calendar.getInstance()
        c.set(y, m - 1, 1)
        c.add(Calendar.MONTH, deltaMonth)
        val ny = c.get(Calendar.YEAR)
        val nm = c.get(Calendar.MONTH) + 1
        return "%04d-%02d".format(ny, nm)
    }

    // stickerResOf : 월(1~12)에 맞는 스티커 drawable 리소스를 반환한다.
    // - 디자인 요소: 달력이 바뀔 때 월별 스티커가 변경된다.
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

    // moodKo : 감정 enum을 파이차트 라벨(한글)로 변환한다.
    private fun moodKo(m: Mood): String = when (m) {
        Mood.JOY -> "기쁨"
        Mood.CONFIDENCE -> "자신감"
        Mood.CALM -> "평온"
        Mood.NORMAL -> "평범"
        Mood.DEPRESSED -> "우울"
        Mood.ANGRY -> "분노"
        Mood.TIRED -> "피곤함"
    }

    // iconResOf : 감정 enum -> 감정 이모티콘 drawable 매핑
    // - 캘린더 그리드에서 날짜별 감정 아이콘을 표시할 때 사용한다.
    private fun iconResOf(mood: Mood): Int = when (mood) {
        Mood.JOY -> R.drawable.emotion_joy
        Mood.CONFIDENCE -> R.drawable.emotion_confidence
        Mood.CALM -> R.drawable.emotion_calm
        Mood.NORMAL -> R.drawable.emotion_normal
        Mood.DEPRESSED -> R.drawable.emotion_sad
        Mood.ANGRY -> R.drawable.emotion_angry
        Mood.TIRED -> R.drawable.emotion_tired
    }

    // labelColorOf : 파이차트 조각 라벨(한글) 기준으로 색상을 고정 매핑한다.
    // - 감정별 색을 일정하게 유지해서 사용자 인지(기쁨=초록 계열 등)를 돕는다.
    private fun labelColorOf(label: String): Int = when (label) {
        "분노" -> Color.parseColor("#ED7173")
        "평온" -> Color.parseColor("#FBE4BC")
        "자신감" -> Color.parseColor("#F6CFE1")
        "기쁨" -> Color.parseColor("#D6E9C7")
        "평범" -> Color.parseColor("#D1CFD2")
        "우울" -> Color.parseColor("#BDE3E7")
        "피곤함" -> Color.parseColor("#E3BCD8")
        else -> Color.parseColor("#D1CFD2")
    }
}