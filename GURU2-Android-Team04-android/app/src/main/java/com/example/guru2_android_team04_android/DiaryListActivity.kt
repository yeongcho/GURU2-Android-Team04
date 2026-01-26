package com.example.guru2_android_team04_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.BottomNavBinder
import com.example.guru2_android_team04_android.ui.bind.DiaryListUiBinder
import com.example.guru2_android_team04_android.util.DateUtil

// DiaryListActivity : 주차별 일기 리스트 화면 Activity
// 용도:
// - 특정 월의 일기를 주차별(7일 단위) 리스트로 보여준다.
// - 월 이동(이전/다음) + 주 이동(이전/다음) + 탭 전환(요약/캘린더/리스트)을 제공한다.
// 설계:
// - 실제 화면 구성/이벤트/데이터 바인딩은 DiaryListUiBinder가 담당한다.
// - Activity는 yearMonth 파라미터 준비 후 binder에 위임한다.
class DiaryListActivity : AppCompatActivity() {
    // appService : 월/주차별 일기 조회 등을 수행하는 서비스 레이어
    private val appService by lazy { (application as MyApp).appService }

    // binder : activity_diary_list.xml의 UI를 세팅하고 데이터를 붙여주는 전담 클래스
    private lateinit var binder: DiaryListUiBinder

    // yearMonth : 현재 보고 있는 월(형식: "yyyy-MM")
    private var yearMonth: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_list)

        // 하단 네비게이션 바를 캘린더 섹션(캘린더/리스트가 같은 메뉴)으로 표시한다.
        BottomNavBinder.bind(this, R.id.navigation_calendar)

        // 진입 시 표시할 월 결정
        // - 다른 화면에서 넘어오면 yearMonth를 전달받아 동일한 달을 유지한다.
        // - 없으면 현재 월로 시작한다.
        yearMonth = intent.getStringExtra("yearMonth") ?: DateUtil.thisMonthYm()

        // binder가 월 헤더/주차 선택/RecyclerView 세팅 및 데이터 바인딩을 수행한다.
        binder = DiaryListUiBinder(this, appService)
        binder.bind(yearMonth)
    }
}
