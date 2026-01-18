package com.example.guru2_android_team04_android.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.guru2_android_team04_android.DiaryDataManager
import com.example.guru2_android_team04_android.R

class CalendarGridFragment : Fragment() {

    private var currentYear: Int = 2026
    private var currentMonth: Int = 1

    companion object {
        fun newInstance(year: Int, month: Int): CalendarGridFragment {
            val fragment = CalendarGridFragment()
            val args = Bundle()
            args.putInt("year", year)
            args.putInt("month", month)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentYear = it.getInt("year")
            currentMonth = it.getInt("month")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar_grid, container, false)

        val gridView = view.findViewById<GridView>(R.id.grid_calendar)
        val tvStatsInfo = view.findViewById<TextView>(R.id.tv_stats_info)

        val diaries = DiaryDataManager.getDiariesByMonth(currentYear, currentMonth)
        val adapter = CalendarAdapter(requireContext(), currentYear, currentMonth, diaries)
        gridView.adapter = adapter

        // 통계 업데이트
        val stats = DiaryDataManager.getMonthlyStats(currentYear, currentMonth)
        tvStatsInfo.text = "이번 달은 ${stats.topEmotion}을(를) 가장 많이 느꼈어요!"

        return view
    }
}