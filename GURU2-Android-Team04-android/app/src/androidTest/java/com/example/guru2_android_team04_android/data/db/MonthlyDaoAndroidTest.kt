package com.example.guru2_android_team04_android.data.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.guru2_android_team04_android.TestDbUtil
import com.example.guru2_android_team04_android.data.model.MonthlySummary
import com.example.guru2_android_team04_android.data.model.Mood
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// MonthlyDaoAndroidTest : MonthlyDao가 월간 요약 데이터를 정상적으로 저장/조회하는지 검증하는 테스트
// 용도:
// - MonthlySummary는 한 달 단위 감정 요약을 저장하는 테이블(monthly)에 대응한다.
// - 월별 요약은 AppService.ensureLastMonthMonthlySummary() 같은 기능에서 생성될 수 있고, 이후 지난 달 요약/월별 기록 화면에서 재조회하여 보여준다.
@RunWith(AndroidJUnit4::class)
class MonthlyDaoAndroidTest {

    @Test
    fun upsert_and_getYear() {
        // Android Instrumentation 테스트이므로 Application Context가 필요하다.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // 테스트 독립성 보장
        TestDbUtil.wipeDb(context)

        // writableDatabase를 여는 시점에 DB가 없으면 생성(onCreate)되고,
        // MonthlyDao가 monthly 테이블에 접근할 수 있는 상태가 된다.
        val db = AppDbHelper(context).writableDatabase
        val dao = MonthlyDao(db)

        // 1) 2026년 1월 요약 저장
        // - ownerId: 사용자 구분 키(회원/비회원 공통 구조)
        // - yearMonth: "YYYY-MM" 형식(월 단위 식별자)
        // - dominantMood/topTag/summaryText: 월간 요약에 필요한 핵심 정보
        dao.upsert(
            MonthlySummary(
                ownerId = "ANON_TEST",
                yearMonth = "2026-01",
                dominantMood = Mood.JOY,
                topTag = "태그",
                summaryText = "1월 요약"
            )
        )

        // 2) 2026년 2월 요약 저장
        // - topTag가 없을 수 있으므로 빈 문자열("")로 저장
        dao.upsert(
            MonthlySummary(
                ownerId = "ANON_TEST",
                yearMonth = "2026-02",
                dominantMood = Mood.CALM,
                topTag = "",
                summaryText = "2월 요약"
            )
        )

        // 3) 조회: 특정 사용자(ownerId)의 특정 연도(2026년) 월간 요약 목록을 가져온다.
        val list = dao.getYear("ANON_TEST", 2026)

        // 4) 검증: 2개가 조회되어야 한다.
        assertEquals(2, list.size)

        // 5) 검증: yearMonth 기준으로 오름차순 정렬되어 반환되는지 확인한다.
        // - getYear가 ORDER BY yearMonth ASC를 사용한다는 전제(DAO 구현 정책)를 검증한다.
        assertEquals("2026-01", list[0].yearMonth)
        assertEquals("2026-02", list[1].yearMonth)
    }
}
