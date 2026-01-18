// DashboardFragment.kt
package com.example.guru2_android_team04_android.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.guru2_android_team04_android.MyApp
import com.example.guru2_android_team04_android.R

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
        val tvSelectedDate = view.findViewById<TextView>(R.id.tv_selected_date)
        val tvDiaryPreview = view.findViewById<TextView>(R.id.tv_diary_preview)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val y = year
            val m = month + 1
            val d = dayOfMonth

            // ✅ DB의 date_ymd 형식과 맞추기 위해 0-padding
            val clickedYmd = String.format("%04d-%02d-%02d", y, m, d)
            val clickedYm = String.format("%04d-%02d", y, m)

            tvSelectedDate.text = "${y}년 ${m}월 ${d}일"

            val appService = (requireActivity().application as MyApp).appService
            val ownerId = appService.currentOwnerIdOrNull() ?: appService.startAnonymousSession()

            // 월 단위로 가져와서 해당 날짜 entry 찾기
            val entries = appService.getEntriesByMonth(ownerId, clickedYm)
            val entry = entries.firstOrNull { it.dateYmd == clickedYmd }

            if (entry != null) {
                tvDiaryPreview.text = "[제목] ${entry.title}\n\n${entry.content}"
                tvDiaryPreview.setTextColor(resources.getColor(R.color.black, null))
            } else {
                tvDiaryPreview.text = "작성된 일기가 없습니다."
                tvDiaryPreview.setTextColor(resources.getColor(R.color.text_gray, null))
            }
        }

        return view
    }
}
