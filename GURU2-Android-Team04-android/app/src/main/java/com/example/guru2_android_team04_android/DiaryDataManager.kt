package com.example.guru2_android_team04_android

import java.util.*

// 기존 코드와의 호환성을 위한 객체 (Unresolved reference: DiaryData 해결)
object DiaryData {
    var isWritten: Boolean = false
    var date: String = ""
    var emotionText: String = ""
    var emotionIcon: Int = 0
    var title: String = ""
    var content: String = ""
}

// 일기 데이터 모델
data class DiaryEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val dayOfWeek: String,
    val emotionType: String,
    val emotionIcon: Int,
    val title: String,
    val content: String,
    val analysis: String = "",
    val mission: String = "",
    val hashtags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getFormattedDate(): String = "${year}년 ${month}월 ${day}일"
    fun getWeekOfMonth(): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day)
        return calendar.get(Calendar.WEEK_OF_MONTH)
    }
}

// 통계 데이터 모델
data class MonthlyStats(
    val year: Int,
    val month: Int,
    val emotionCounts: Map<String, Int>,
    val totalEntries: Int,
    val topEmotion: String
)

object DiaryDataManager {
    var currentDiary: DiaryEntry? = null
    private val diaryMap = mutableMapOf<String, DiaryEntry>()

    fun saveDiary(entry: DiaryEntry) {
        diaryMap[entry.date] = entry
        currentDiary = entry

        // DiaryData 동기화
        DiaryData.isWritten = true
        DiaryData.date = entry.getFormattedDate()
        DiaryData.emotionText = entry.emotionType
        DiaryData.emotionIcon = entry.emotionIcon
        DiaryData.title = entry.title
        DiaryData.content = entry.content
    }

    fun getDiary(date: String): DiaryEntry? = diaryMap[date]

    fun getDiariesByMonth(year: Int, month: Int): List<DiaryEntry> {
        return diaryMap.values.filter { it.year == year && it.month == month }
            .sortedByDescending { it.day }
    }

    fun getMonthlyStats(year: Int, month: Int): MonthlyStats {
        val diaries = getDiariesByMonth(year, month)
        val emotionCounts = diaries.groupingBy { it.emotionType }.eachCount()
        val topEmotion = emotionCounts.maxByOrNull { it.value }?.key ?: "평온"
        return MonthlyStats(year, month, emotionCounts, diaries.size, topEmotion)
    }

    fun getAllDiaries(): List<DiaryEntry> = diaryMap.values.toList()

    // 샘플 데이터 (리소스 이름은 반드시 영문이어야 함)
    fun generateSampleData() {
        val emotions = listOf(
            Triple("기쁨", R.drawable.emotion_joy, "좋아하는 영화를 본 날"),
            Triple("평온", R.drawable.emotion_calm, "산책을 하고 온 날"),
            Triple("슬픔", R.drawable.emotion_sad, "지치는 하루였다"),
            Triple("자신감", R.drawable.emotion_confidence, "발표를 잘 마쳤다")
        )
        val year = 2026
        val month = 1
        for (i in 1..5) {
            val emotion = emotions[i % emotions.size]
            saveDiary(DiaryEntry(
                date = "2026-01-${13 + i}",
                year = year, month = month, day = 13 + i,
                dayOfWeek = "요일", emotionType = emotion.first,
                emotionIcon = emotion.second, title = emotion.third,
                content = "상세 내용입니다.", analysis = "AI 분석 결과입니다.",
                mission = "따뜻한 차 마시기", hashtags = listOf("#감정", "#일기")
            ))
        }
    }
}