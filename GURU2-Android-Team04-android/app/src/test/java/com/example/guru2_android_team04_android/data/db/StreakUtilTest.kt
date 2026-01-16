package com.example.guru2_android_team04_android.data.db

import org.junit.Assert.assertEquals
import org.junit.Test
import com.example.guru2_android_team04_android.util.StreakUtil

// StreakUtilTest : StreakUtil(연속 작성 일수 계산 유틸)의 단위 테스트
// 용도:
// - 최신 -> 과거로 정렬된 날짜 리스트를 넣었을 때, 연속 작성(streak) 일수가 기대값과 동일하게 계산되는지 검증한다.
// - streak 로직이 날짜 경계(연속/끊김/빈 리스트)에 대해 올바르게 동작하는지 확인한다.
class StreakUtilTest {

    @Test
    fun streak_basic() {
        // 테스트 목적:
        // - 연속 구간과 끊기는 지점이 섞여 있을 때,
        //   최신 날짜부터 연속인 구간만 카운트하고 끊기면 즉시 종료하는지 확인한다.

        // 최신순 정렬된 날짜
        // - 10일 -> 09일 -> 08일은 연속 3일
        // - 그 다음 05일은 08일의 전날이 아니므로 streak가 끊겨야 한다.
        val dates = listOf("2026-01-10", "2026-01-09", "2026-01-08", "2026-01-05")

        // 기대 결과:
        // - 최신부터 연속 3개만 인정되므로 3
        assertEquals(3, StreakUtil.computeConsecutiveDays(dates))
    }

    @Test
    fun streak_single() {
        // 테스트 목적:
        // - 날짜가 1개만 있을 때, 최소 streak는 1로 계산되는지 확인한다. (StreakUtil은 비어있지 않으면 기본 streak=1에서 시작)
        val dates = listOf("2026-01-10")

        // 기대 결과: 1일 연속
        assertEquals(1, StreakUtil.computeConsecutiveDays(dates))
    }

    @Test
    fun streak_empty() {
        // 테스트 목적:
        // - 입력 리스트가 비어 있을 때 0을 반환하는지 확인한다.
        // 예외처리) StreakUtil 내부에서 emptyList면 0을 바로 반환하도록 방어 로직이 있다.
        val dates = emptyList<String>()

        // 기대 결과: 일기 기록이 없으므로 0일 연속
        assertEquals(0, StreakUtil.computeConsecutiveDays(dates))
    }
}