package com.example.guru2_android_team04_android.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// StreakUtil : 연속 작성(streak) 일수 계산 전용 유틸리티
// 용도:
// - 사용자가 며칠 연속으로 일기를 작성했는지 계산하기 위해 사용한다.
// - DiaryDao.computeStreakFromLatest()에서 "최신 -> 과거" 순으로 정렬된 날짜 목록을 전달받아 streak 값을 산출
object StreakUtil {

    // "YYYY-MM-DD" 문자열을 Date/Calendar로 파싱하기 위한 포맷터
    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)

    // 최신 날짜 기준으로 이어진 연속 작성 일수를 계산한다.
    // 계산 로직:
    // 1) 첫 번째(최신) 날짜를 기준으로 streak = 1부터 시작
    // 2) 다음 날짜가 "바로 전날"인지 비교
    // 3) 전날이면 streak 증가, 아니면 연속이 끊긴 것으로 판단하고 종료
    // 반환값:
    // - 연속 작성 일수
    // - 일기 기록이 전혀 없으면 0
    fun computeConsecutiveDays(sortedDescDates: List<String>): Int {

        // 예외처리) 날짜 목록이 비어 있으면 streak는 0
        if (sortedDescDates.isEmpty()) return 0

        // 첫 날짜는 최소 1일 연속으로 간주
        var streak = 1

        // 비교 기준이 되는 날짜 (가장 최신 날짜)
        var prev = toCal(sortedDescDates[0])

        // 두 번째 날짜부터 순차적으로 비교
        for (i in 1 until sortedDescDates.size) {
            val cur = toCal(sortedDescDates[i])

            // prev 날짜를 "하루 전"으로 이동
            prev.add(Calendar.DAY_OF_MONTH, -1)

            if (sameDay(prev, cur)) {
                streak += 1
                // 다음 비교를 위해 기준 날짜를 현재 날짜로 갱신
                prev = cur
            } else {
                // 연속이 끊기면 이후 날짜는 볼 필요가 없으므로 종료
                break
            }
        }
        return streak
    }

    // "YYYY-MM-DD" 문자열을 Calendar 객체로 변환한다.
    // 목적:
    // - 날짜 간 연산(전날 비교, 동일 날짜 비교)을 쉽게 처리하기 위함
    private fun toCal(ymd: String): Calendar {

        // 예외처리)
        // - fmt.parse()는 실패 시 null을 반환할 수 있다.
        // - 하지만 이 값은 DB에 저장된 날짜 문자열로, 형식이 보장된다는 전제하에 !!로 처리한다.
        // - 만약 외부 입력을 직접 받는 구조라면 null 체크가 필요
        val d = fmt.parse(ymd)!!

        return Calendar.getInstance().apply { time = d }
    }

    // 두 Calendar 객체가 같은 날짜인지 판별한다.
    // - 연/연중일(DAY_OF_YEAR) 기준으로 비교한다.
    private fun sameDay(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }
}
