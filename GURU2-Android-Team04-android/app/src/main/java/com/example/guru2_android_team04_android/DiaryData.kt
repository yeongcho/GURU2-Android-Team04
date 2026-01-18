package com.example.guru2_android_team04_android

// 어디서든 접근할 수 있는 일기 데이터 저장소 (싱글톤)
object DiaryData {
    var isWritten: Boolean = false

    // ✅ SharedPreferences에서 찾기 위한 날짜 키 (예: 2026-2-6)
    var dateKey: String = ""

    // ✅ 화면에 보여줄 날짜 (예: 2026년 2월 6일)
    var date: String = ""

    var emotionText: String = "평온"
    var emotionIcon: Int = R.drawable.emotion_normal
    var title: String = ""
    var content: String = ""
}
