package com.example.guru2_android_team04_android.ui.calendar

import java.time.LocalDate

data class DiaryListItem(
    val date: LocalDate,
    val emotion: EmotionType,
    val title: String
)