package com.example.guru2_android_team04_android.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.guru2_android_team04_android.DiaryDataManager
import com.example.guru2_android_team04_android.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var tvCurrentMonth: TextView
    private lateinit var ivMonthTitle: ImageView // 달별 이미지를 보여줄 뷰 추가
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton

    // 프로젝트 기준 연도 및 초기 월 설정
    private var currentYear = 2026
    private var currentMonth = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // 현재 날짜 기반 초기값 설정
        val calendar = Calendar.getInstance()
        currentYear = calendar.get(Calendar.YEAR)
        currentMonth = calendar.get(Calendar.MONTH) + 1

        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.view_pager)
        tvCurrentMonth = view.findViewById(R.id.tv_current_month)

        // layout_calendar_top.xml에 정의한 ImageView 연결
        ivMonthTitle = view.findViewById(R.id.iv_month_title)

        btnPrevMonth = view.findViewById(R.id.btn_prev_month)
        btnNextMonth = view.findViewById(R.id.btn_next_month)

        // 샘플 데이터 생성
        if (DiaryDataManager.getAllDiaries().isEmpty()) {
            DiaryDataManager.generateSampleData()
        }

        setupViewPager()
        updateMonthDisplay()

        // 이전 달 이동 리스너
        btnPrevMonth.setOnClickListener {
            currentMonth--
            if (currentMonth < 1) {
                currentMonth = 12
                currentYear--
            }
            updateMonthAndRefresh()
        }

        // 다음 달 이동 리스너
        btnNextMonth.setOnClickListener {
            currentMonth++
            if (currentMonth > 12) {
                currentMonth = 1
                currentYear++
            }
            updateMonthAndRefresh()
        }

        return view
    }

    private fun setupViewPager() {
        val adapter = CalendarPagerAdapter(requireActivity())
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "캘린더"
                1 -> "리스트"
                else -> ""
            }
        }.attach()
    }

    private fun updateMonthDisplay() {
        // 1. 제작하신 영문 파일명 이미지 배열
        val monthImages = arrayOf(
            R.drawable.month_1, R.drawable.month_2, R.drawable.month_3,
            R.drawable.month_4, R.drawable.month_5, R.drawable.month_6,
            R.drawable.month_7, R.drawable.month_8, R.drawable.month_9,
            R.drawable.month_10, R.drawable.month_11, R.drawable.month_12
        )

        // 2. 현재 월에 맞는 이미지 교체
        ivMonthTitle.setImageResource(monthImages[currentMonth - 1])

        // 3. 연도 텍스트 업데이트
        tvCurrentMonth.text = "$currentYear"
    }

    private fun updateMonthAndRefresh() {
        updateMonthDisplay()
        // 프래그먼트 재생성을 통해 데이터 새로고침
        setupViewPager()
    }

    private inner class CalendarPagerAdapter(fragmentActivity: FragmentActivity)
        : FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> CalendarGridFragment.newInstance(currentYear, currentMonth)
                1 -> CalendarListFragment.newInstance(currentYear, currentMonth)
                else -> CalendarGridFragment.newInstance(currentYear, currentMonth)
            }
        }
    }
}