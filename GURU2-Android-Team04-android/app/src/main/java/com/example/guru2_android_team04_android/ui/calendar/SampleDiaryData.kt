package com.example.guru2_android_team04_android.ui.calendar

import java.time.YearMonth

fun getSampleDiaryList(month: YearMonth): List<DiaryListItem> {
    return listOf(
        DiaryListItem(month.atDay(18), EmotionType.JOY, "좋아하는 영화를 본 날"),
        DiaryListItem(month.atDay(19), EmotionType.CALM, "봉사활동을 하고 온 날"),
        DiaryListItem(month.atDay(21), EmotionType.TIRED, "할 일이 너무 많아서 지친다.."),
        DiaryListItem(month.atDay(23), EmotionType.SAD, "피곤하지만 이제 주말이니까")
    )
}