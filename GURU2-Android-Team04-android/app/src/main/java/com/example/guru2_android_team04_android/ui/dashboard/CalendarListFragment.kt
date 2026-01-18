package com.example.guru2_android_team04_android.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager // 추가됨
import androidx.recyclerview.widget.RecyclerView // 추가됨
import com.example.guru2_android_team04_android.DiaryDataManager
import com.example.guru2_android_team04_android.DiaryEntry
import com.example.guru2_android_team04_android.R

class CalendarListFragment : Fragment() {

    private var currentYear: Int = 2026
    private var currentMonth: Int = 1

    companion object {
        fun newInstance(year: Int, month: Int): CalendarListFragment {
            val fragment = CalendarListFragment()
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
        // 레이아웃 파일 인플레이트
        val view = inflater.inflate(R.layout.fragment_calendar_list, container, false)

        // XML의 ID와 일치시키기 (rv_diary_list 또는 recycler_diary_list 중 하나로 통일 필요)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_diary_list)

        // RecyclerView 설정 (LayoutManager 추가 필수)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // 데이터 로드 및 어댑터 연결
        val diaries = DiaryDataManager.getDiariesByMonth(currentYear, currentMonth)

        // DiaryListAdapter가 같은 파일 하단이나 패키지 내에 정의되어 있어야 함
        recyclerView.adapter = DiaryListAdapter(diaries) { diary ->
            // 클릭 시 상세 화면 이동 로직 작성 공간
        }

        return view
    }
}

// CalendarListFragment.kt 파일의 가장 아랫부분에 추가하세요.

class DiaryListAdapter(
    private val diaries: List<DiaryEntry>,
    private val onItemClick: (DiaryEntry) -> Unit
) : RecyclerView.Adapter<DiaryListAdapter.DiaryViewHolder>() {

    // 디자인 목업의 각 일기 항목(카드)에 들어갈 뷰들을 연결합니다.
    class DiaryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivEmotion: android.widget.ImageView = view.findViewById(R.id.iv_emotion)
        val tvTitle: android.widget.TextView = view.findViewById(R.id.tv_diary_title)
        val tvDay: android.widget.TextView = view.findViewById(R.id.tv_day)
        val tvDayOfWeek: android.widget.TextView = view.findViewById(R.id.tv_day_of_week)
        val layoutCard: View = view.findViewById(R.id.layout_card_content)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        // 리스트의 한 칸을 구성할 레이아웃 파일을 인플레이트합니다.
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diary_card, parent, false)
        return DiaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        val diary = diaries[position]

        // 1. 감정 아이콘 설정 (유정 님이 바꾼 영문 리소스 이름과 매칭됩니다)
        holder.ivEmotion.setImageResource(diary.emotionIcon)

        // 2. 일기 제목 및 날짜 설정
        holder.tvTitle.text = diary.title
        holder.tvDay.text = diary.day.toString()
        holder.tvDayOfWeek.text = diary.dayOfWeek

        // 3. 카드 클릭 시 상세 화면으로 이동하는 리스너
        holder.layoutCard.setOnClickListener {
            onItemClick(diary)
        }
    }

    override fun getItemCount() = diaries.size
}