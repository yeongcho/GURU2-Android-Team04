package com.example.guru2_android_team04_android.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// DateUtil : 날짜/시간 관련 공통 로직을 모아둔 유틸리티 객체
// 용도:
// - 앱 전반에서 사용하는 날짜 포맷과 계산 로직을 한 곳에 모아 중복을 제거한다.
// - 일기 작성 날짜, 월간 요약 기준 월, 서비스 이용 일수 계산 등에 사용된다.
// 설계 이유:
// - 날짜 포맷 문자열을 코드 곳곳에 직접 쓰면 오타/불일치 위험이 크다.
// - Calendar/SimpleDateFormat 사용 로직을 공통화해 가독성과 유지보수성을 높인다.
object DateUtil {

    // 일기 날짜용 포맷 ("YYYY-MM-DD")
    // - diary_entries.date_ymd 컬럼과 동일한 형식
    private val ymdFmt = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)

    // 월 단위 포맷 ("YYYY-MM")
    // - monthly_summaries.year_month 컬럼과 동일한 형식
    private val ymFmt = SimpleDateFormat("yyyy-MM", Locale.KOREA)

    // 오늘 날짜를 "YYYY-MM-DD" 형식 문자열로 반환한다.
    // 사용 예:
    // - 일기 저장 시 date_ymd 자동 채움
    fun todayYmd(): String =
        ymdFmt.format(System.currentTimeMillis())

    // 현재 월을 "YYYY-MM" 형식 문자열로 반환한다.
    // 사용 예:
    // - 캘린더 기본 월
    // - 이번 달 통계 조회
    fun thisMonthYm(): String =
        ymFmt.format(System.currentTimeMillis())

    // 특정 월("YYYY-MM")의 첫째 날을 "YYYY-MM-01" 형태로 반환한다.
    // 사용 예:
    // - 월간 조회 범위 시작일 계산
    fun firstDayOfMonthYmd(yearMonth: String): String =
        "$yearMonth-01"

    // 오늘이 해당 월의 첫째 날인지 여부를 반환한다.
    // 사용 예:
    // - 앱 실행 시 "지난달 요약 생성" 같은 월 단위 트리거 조건 판단
    fun isFirstDayToday(): Boolean {
        val c = Calendar.getInstance()
        return c.get(Calendar.DAY_OF_MONTH) == 1
    }

    // 이전 달을 "YYYY-MM" 형식으로 반환한다.
    // 사용 예:
    // - 지난달 월간 요약 조회/생성
    fun previousMonthYm(): String {
        val c = Calendar.getInstance()
        c.add(Calendar.MONTH, -1)
        return ymFmt.format(c.time)
    }

    // 특정 시점(startMillis)부터 현재(nowMillis)까지 경과한 "일 수"를 계산한다.
    // 입력:
    // - startMillis: 기준 시각 (Unix ms)
    // - nowMillis: 비교 시각 (기본값: 현재 시각)
    // 반환:
    // - 경과 일 수 (Long)
    // 사용 예:
    // - 서비스 이용 일수 계산
    // - 프로필 화면에서 "N일째 사용 중" 표시
    fun daysSince(startMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long {

        val diff = nowMillis - startMillis
        // 예외처리) 기준 시각이 미래인 경우 음수가 나오므로 0일로 처리
        if (diff < 0) return 0L
        // 밀리초 → 일 단위 변환
        return diff / (24L * 60L * 60L * 1000L)
    }

    // 오늘 요일을 한국어(월/화/수/목/금/토/일)로 반환한다.
    // 사용 예:
    // - 시작 화면의 "좋은 -요일이에요!" 문구 생성
    fun todayWeekdayKo(): String {
        val c = Calendar.getInstance()
        return when (c.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "일요일"
            Calendar.MONDAY -> "월요일"
            Calendar.TUESDAY -> "화요일"
            Calendar.WEDNESDAY -> "수요일"
            Calendar.THURSDAY -> "목요일"
            Calendar.FRIDAY -> "금요일"
            Calendar.SATURDAY -> "토요일"
            else -> "오늘"
        }
    }

    // 오늘 날짜를 홈 화면 표시용 포맷("YYYY년 M월 D일 요일")으로 반환한다.
    // 사용 예:
    // - 홈 화면 상단 "2026년 2월 6일 금요일" 같은 텍스트 표시
    fun todayPrettyKo(): String {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH) + 1
        val day = c.get(Calendar.DAY_OF_MONTH)
        val weekday = todayWeekdayKo()
        return "${year}년 ${month}월 ${day}일 ${weekday}"
    }

    // "yyyy-MM-dd" 형태의 날짜를 "YYYY년 M월 D일 요일" 형태로 변환한다.
    // 사용 예:
    // - 상세 화면에서 entry.dateYmd를 예쁜 포맷으로 표시
    fun ymdToPrettyKo(ymd: String): String {
        return try {
            val d = ymdFmt.parse(ymd) ?: return todayPrettyKo()
            val c = Calendar.getInstance().apply { time = d }

            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH) + 1
            val day = c.get(Calendar.DAY_OF_MONTH)

            val weekday = when (c.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "일요일"
                Calendar.MONDAY -> "월요일"
                Calendar.TUESDAY -> "화요일"
                Calendar.WEDNESDAY -> "수요일"
                Calendar.THURSDAY -> "목요일"
                Calendar.FRIDAY -> "금요일"
                Calendar.SATURDAY -> "토요일"
                else -> "오늘"
            }

            "${year}년 ${month}월 ${day}일 ${weekday}"
        } catch (_: Exception) {
            // 예외처리) 파싱 실패 시 오늘 날짜로 대체
            todayPrettyKo()
        }
    }

}
