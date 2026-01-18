package com.example.guru2_android_team04_android.ui.dashboard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.guru2_android_team04_android.DiaryEntry
import com.example.guru2_android_team04_android.R
import java.util.*

class CalendarAdapter(
    private val context: Context,
    private val year: Int,
    private val month: Int,
    private val diaries: List<DiaryEntry>
) : BaseAdapter() {

    private val days = mutableListOf<Int>()

    init {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..lastDay) days.add(i)
    }

    override fun getCount(): Int = days.size
    override fun getItem(position: Int): Any = days[position]
    override fun getItemId(position: Int): Long = position.toLong()

    fun getDayAtPosition(position: Int): Int = days[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false)
        val day = days[position]

        val tvDay = view.findViewById<TextView>(R.id.tv_day)
        val ivEmotion = view.findViewById<ImageView>(R.id.iv_calendar_emotion)

        tvDay.text = day.toString()

        // 해당 날짜에 일기가 있는지 확인하여 아이콘 표시
        val dateStr = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        val diary = diaries.find { it.date == dateStr }

        if (diary != null) {
            ivEmotion.visibility = View.VISIBLE
            ivEmotion.setImageResource(diary.emotionIcon)
        } else {
            ivEmotion.visibility = View.GONE
        }

        return view
    }
}