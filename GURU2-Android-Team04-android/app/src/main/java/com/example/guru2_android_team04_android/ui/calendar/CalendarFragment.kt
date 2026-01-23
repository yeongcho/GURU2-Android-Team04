package com.example.guru2_android_team04_android.ui.calendar

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.guru2_android_team04_android.R
import com.example.guru2_android_team04_android.databinding.FragmentCalendarBinding
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private var currentMonth = YearMonth.now()
    private var selectedTab = CalendarTab.LIST // 시안에 맞춰 리스트를 기본값으로 설정 가능

    private var listContainer: View? = null
    private var diaryListAdapter: DiaryListAdapter? = null
    private var currentWeek = 4

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        // 리스트용 레이아웃 인플레이트
        listContainer = inflater.inflate(R.layout.fragment_calendar_list, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupListView()
        updateUI()
    }

    private fun setupViews() {
        binding.btnPrevMonth.setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)
            updateUI()
        }
        binding.btnNextMonth.setOnClickListener {
            currentMonth = currentMonth.plusMonths(1)
            updateUI()
        }

        binding.btnTabSummary.setOnClickListener { selectedTab = CalendarTab.SUMMARY; updateTabUI() }
        binding.btnTabCalendar.setOnClickListener { selectedTab = CalendarTab.CALENDAR; updateTabUI() }
        binding.btnTabList.setOnClickListener { selectedTab = CalendarTab.LIST; updateTabUI() }
    }

    private fun setupListView() {
        listContainer?.let { container ->
            val rvDiaryList = container.findViewById<RecyclerView>(R.id.rvDiaryList)
            // 외부 SampleDiaryData.kt의 함수 호출
            diaryListAdapter = DiaryListAdapter(getSampleDiaryList(currentMonth))

            rvDiaryList.apply {
                adapter = diaryListAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }

            val btnPrevWeek = container.findViewById<ImageView>(R.id.btnPrevWeek)
            val btnNextWeek = container.findViewById<ImageView>(R.id.btnNextWeek)
            val tvWeek = container.findViewById<TextView>(R.id.tvWeek)

            btnPrevWeek.setOnClickListener { if (currentWeek > 1) { currentWeek--; updateWeekDisplay(tvWeek) } }
            btnNextWeek.setOnClickListener { if (currentWeek < 5) { currentWeek++; updateWeekDisplay(tvWeek) } }
            updateWeekDisplay(tvWeek)
        }
    }

    private fun updateUI() {
        binding.tvMonth.text = currentMonth.format(DateTimeFormatter.ofPattern("MM월 yyyy"))
        listContainer?.let { container ->
            val ivMonthSticker = container.findViewById<ImageView>(R.id.ivMonthSticker)
            ivMonthSticker.setImageResource(getMonthStickerId(currentMonth.monthValue))
            diaryListAdapter?.updateData(getSampleDiaryList(currentMonth))
        }
        updateTabUI()
    }

    private fun updateTabUI() {
        val inactiveColor = Color.parseColor("#6F8F74")
        val activeColor = Color.parseColor("#000000")

        binding.btnTabSummary.setTextColor(inactiveColor)
        binding.btnTabCalendar.setTextColor(inactiveColor)
        binding.btnTabList.setTextColor(inactiveColor)

        when (selectedTab) {
            CalendarTab.SUMMARY -> {
                binding.btnTabSummary.setTextColor(activeColor)
                showSummaryView()
            }
            CalendarTab.CALENDAR -> {
                binding.btnTabCalendar.setTextColor(activeColor)
                showCalendarView()
            }
            CalendarTab.LIST -> {
                binding.btnTabList.setTextColor(activeColor)
                showListView()
            }
        }
    }

    private fun showSummaryView() {
        binding.calendarCard.visibility = View.GONE
        binding.graphCard.visibility = View.GONE
        listContainer?.visibility = View.GONE
    }

    private fun showCalendarView() {
        binding.calendarCard.visibility = View.VISIBLE
        binding.graphCard.visibility = View.VISIBLE
        listContainer?.let { (binding.root as ViewGroup).removeView(it) }
    }

    private fun showListView() {
        binding.calendarCard.visibility = View.GONE
        binding.graphCard.visibility = View.GONE
        listContainer?.let { container ->
            if (container.parent == null) (binding.root as ViewGroup).addView(container)
            container.visibility = View.VISIBLE
        }
    }

    private fun updateWeekDisplay(tvWeek: TextView) {
        val weekNames = arrayOf("첫째", "둘째", "셋째", "넷째", "다섯째")
        tvWeek.text = "${weekNames[currentWeek - 1]} 주"
    }

    private fun getMonthStickerId(month: Int): Int {
        return resources.getIdentifier("month_$month", "drawable", requireContext().packageName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}