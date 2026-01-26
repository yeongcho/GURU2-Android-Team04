package com.example.guru2_android_team04_android.data.model

// Mood.ko() : 감정(Mood) enum 값을 "한국어 표시 문자열"로 변환하는 확장 함수
// 용도:
// - DB/로직에서는 Mood(enum)로 감정을 관리하고,
// - UI에는 사람이 읽기 쉬운 한글 라벨이 필요하므로 변환용 함수를 제공한다.
fun Mood.ko(): String = when (this) {

    // JOY 감정은 UI에서 "기쁨"으로 표시
    Mood.JOY -> "기쁨"

    // CALM 감정은 UI에서 "평온"으로 표시
    Mood.CALM -> "평온"

    // CONFIDENCE 감정은 UI에서 "자신감"으로 표시
    Mood.CONFIDENCE -> "자신감"

    // NORMAL 감정은 UI에서 "평범"으로 표시
    Mood.NORMAL -> "평범"

    // DEPRESSED 감정은 UI에서 "우울"로 표시
    Mood.DEPRESSED -> "우울"

    // ANGRY 감정은 UI에서 "분노"로 표시
    Mood.ANGRY -> "분노"

    // TIRED 감정은 UI에서 "피곤함"으로 표시
    Mood.TIRED -> "피곤함"
}

