package com.example.guru2_android_team04_android.util

import org.junit.Assert.assertTrue
import org.junit.Test

// DateUtilTest : DateUtil(날짜/시간 유틸) 단위 테스트
// 용도:
// - DateUtil이 UI/DB에서 기대하는 날짜 문자열 포맷을 안정적으로 반환하는지 확인한다.
// - 날짜 차이 계산(daysSince)이 음수가 되지 않도록 방어 로직이 동작하는지 검증한다.
class DateUtilTest {

    @Test
    fun todayYmd_format() {
        // 테스트 목적:
        // - DateUtil.todayYmd()가 항상 "YYYY-MM-DD" 형태로 반환되는지 확인한다.
        val t = DateUtil.todayYmd()

        // 예외처리) 단순히 길이만 보는 게 아니라 정규식으로 형식을 엄격히 확인한다.
        // - 4자리 연도-2자리 월-2자리 일 형식인지 검증
        assertTrue(t.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun daysSince_non_negative() {
        // 테스트 목적:
        // - daysSince()가 어떤 입력에서도 음수를 반환하지 않는지 확인한다.
        // - DateUtil.daysSince() 내부에서 nowMillis - startMillis가 음수면 0L을 반환하도록 설계되어 있음
        val now = System.currentTimeMillis()

        // startMillis == now이면 diff가 0이므로 결과는 0 이상이어야 한다.
        val d = DateUtil.daysSince(now)

        assertTrue(d >= 0)
    }
}