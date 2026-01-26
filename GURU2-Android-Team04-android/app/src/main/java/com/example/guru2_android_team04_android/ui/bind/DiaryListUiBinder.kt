package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.guru2_android_team04_android.*
import com.example.guru2_android_team04_android.ui.adapter.DiaryEntryAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// DiaryListUiBinder : 주차별 일기 리스트 화면(activity_diary_list.xml) <-> AppService 연결 전담 클래스
// 용도:
// - 상단 월 헤더(이전/다음) + 탭(요약/캘린더/리스트) 클릭 이벤트를 연결한다.
// - 주차 선택(이전/다음 주) 로직을 구성하고 현재 주차 텍스트를 갱신한다.
// - 선택된 주차 구간(startYmd~endYmd)에 해당하는 일기 목록을 RecyclerView에 표시한다.
class DiaryListUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {
    // weekIndex : 현재 선택된 주차 인덱스(0부터 시작)
    private var weekIndex = 0

    // weeks : 해당 월의 주차 구간 리스트
    private lateinit var weeks: List<Pair<String, String>>

    // RecyclerView Adapter
    // - 아이템 클릭 시 보관함/아카이브 상세 화면으로 이동한다.
    private val adapter = DiaryEntryAdapter { entry ->
        activity.startActivity(Intent(activity, ArchiveDiaryDetailActivity::class.java).apply {
            putExtra("entryId", entry.entryId)
        })
    }

    // bind : 특정 월(yearMonth="yyyy-MM")을 기준으로 화면 요소와 이벤트, 데이터 바인딩을 수행한다.
    fun bind(yearMonth: String) {
        // 상단 월 헤더
        val tvMonth = activity.findViewById<TextView>(R.id.tvMonth)
        val btnPrevMonth = activity.findViewById<ImageView>(R.id.btnPrevMonth)
        val btnNextMonth = activity.findViewById<ImageView>(R.id.btnNextMonth)

        // 상단 탭(요약/캘린더/리스트)
        val btnTabSummary = activity.findViewById<TextView>(R.id.btnTabSummary)
        val btnTabCalendar = activity.findViewById<TextView>(R.id.btnTabCalendar)
        val btnTabList = activity.findViewById<TextView>(R.id.btnTabList)

        // 월 스티커
        val ivSticker = activity.findViewById<ImageView>(R.id.ivMonthSticker)

        // 주차 선택(이전/다음 주) + 주차 표시 텍스트
        val btnPrevWeek = activity.findViewById<ImageView>(R.id.btnPrevWeek)
        val btnNextWeek = activity.findViewById<ImageView>(R.id.btnNextWeek)
        val tvWeek = activity.findViewById<TextView>(R.id.tvWeek)

        // RecyclerView 세팅
        // - LinearLayoutManager로 세로 리스트를 구성하고 adapter를 연결한다.
        val rv = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvDiaryList)
        rv.layoutManager = LinearLayoutManager(activity)
        rv.adapter = adapter

        // 월 표시 텍스트/스티커 세팅
        val (yy, mm) = parseYm(yearMonth)
        tvMonth.text = "${mm.toString().padStart(2, '0')}월 $yy"
        ivSticker.setImageResource(stickerResOf(mm))

        // 탭 이동
        // - 이동 시 yearMonth를 전달해 동일한 월 상태를 유지한다.
        btnTabSummary.setOnClickListener {
            activity.startActivity(Intent(activity, MonthlySummaryActivity::class.java).apply {
                putExtra("yearMonth", yearMonth)
            })
            activity.finish()
        }
        btnTabCalendar.setOnClickListener {
            activity.startActivity(Intent(activity, DiaryCalendarActivity::class.java).apply {
                putExtra("yearMonth", yearMonth)
            })
            activity.finish()
        }
        // 현재 화면이 리스트 탭이므로 동작 없음
        btnTabList.setOnClickListener {  }

        // 월 이동
        // - 이전/다음 달로 yearMonth를 이동시키고 같은 화면을 다시 연다.
        btnPrevMonth.setOnClickListener {
            val prev = shiftYm(yearMonth, -1)
            activity.startActivity(Intent(activity, DiaryListActivity::class.java).apply {
                putExtra("yearMonth", prev)
            })
            activity.finish()
        }
        btnNextMonth.setOnClickListener {
            val next = shiftYm(yearMonth, +1)
            activity.startActivity(Intent(activity, DiaryListActivity::class.java).apply {
                putExtra("yearMonth", next)
            })
            activity.finish()
        }

        // 주차 구간 계산
        // - weeks = [(yyyy-MM-01 ~ yyyy-MM-07), (yyyy-MM-08 ~ ...), ...]
        weeks = computeWeeks(yearMonth)

        // 예외처리) weeks를 안전하게 범위 보정
        weekIndex = weekIndex.coerceIn(0, weeks.size - 1)

        // renderWeek : weekIndex에 해당하는 주차를 화면에 반영하고 해당 구간 일기를 로드한다.
        fun renderWeek() {
            tvWeek.text = weekLabel(weekIndex)
            val (startYmd, endYmd) = weeks[weekIndex]
            bindWeekEntries(startYmd, endYmd)
        }

        // 이전 주: 0 밑으로 내려가지 않게 제한
        btnPrevWeek.setOnClickListener {
            weekIndex = (weekIndex - 1).coerceAtLeast(0)
            renderWeek()
        }

        // 다음 주: 마지막 인덱스를 넘지 않게 제한
        btnNextWeek.setOnClickListener {
            weekIndex = (weekIndex + 1).coerceAtMost(weeks.size - 1)
            renderWeek()
        }

        // 최초 진입 시 현재 weekIndex 주차를 표시
        renderWeek()
    }

    // bindWeekEntries : startYmd~endYmd 구간에 해당하는 일기를 가져와 RecyclerView에 표시한다.
    private fun bindWeekEntries(startYmd: String, endYmd: String) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            // 로그인/세션 사용자(ownerId) 기준으로 주차 구간 일기 조회
            val ownerId = appService.getUserProfile().ownerId

            val list = appService.getEntriesByWeek(ownerId, startYmd, endYmd)
                .sortedBy { it.dateYmd }

            // 조회된 리스트를 Adapter에 반영(RecyclerView 갱신)
            withContext(Dispatchers.Main) {
                adapter.submitList(list)
            }
        }
    }

    // computeWeeks : 해당 월을 "7일 단위 구간"으로 분리한다.
    private fun computeWeeks(yearMonth: String): List<Pair<String, String>> {
        val (y, m) = parseYm(yearMonth)
        val c = Calendar.getInstance()
        c.set(y, m - 1, 1, 0, 0, 0)
        c.set(Calendar.MILLISECOND, 0)

        val daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH)

        val ranges = mutableListOf<Pair<String, String>>()
        var start = 1
        while (start <= daysInMonth) {
            val end = (start + 6).coerceAtMost(daysInMonth)
            ranges.add(
                "%04d-%02d-%02d".format(y, m, start) to "%04d-%02d-%02d".format(y, m, end)
            )
            start += 7
        }
        return ranges
    }

    // weekLabel : 주차 인덱스를 "첫째 주/둘째 주/..."로 보여주기 위한 라벨
    private fun weekLabel(i: Int): String = when (i) {
        0 -> "첫째 주"
        1 -> "둘째 주"
        2 -> "셋째 주"
        3 -> "넷째 주"
        else -> "${i + 1}째 주"
    }

    // parseYm : "yyyy-MM" 문자열을 (year, month)로 파싱한다.
    // 예외처리) 파싱 실패 시 기본값을 사용한다.
    private fun parseYm(ym: String): Pair<Int, Int> {
        val y = ym.take(4).toIntOrNull() ?: 1970
        val m = ym.drop(5).take(2).toIntOrNull() ?: 1
        return y to m
    }

    // shiftYm : yearMonth("yyyy-MM")에서 deltaMonth만큼 이동한 "yyyy-MM"을 반환한다.
    private fun shiftYm(ym: String, deltaMonth: Int): String {
        val (y, m) = parseYm(ym)
        val c = Calendar.getInstance()
        c.set(y, m - 1, 1)
        c.add(Calendar.MONTH, deltaMonth)
        val ny = c.get(Calendar.YEAR)
        val nm = c.get(Calendar.MONTH) + 1
        return "%04d-%02d".format(ny, nm)
    }

    // stickerResOf : 월별 스티커 리소스 매핑
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
}