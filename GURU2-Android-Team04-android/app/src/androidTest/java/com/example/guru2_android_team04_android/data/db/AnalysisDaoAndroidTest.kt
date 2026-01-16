package com.example.guru2_android_team04_android.data.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.guru2_android_team04_android.TestDbUtil
import com.example.guru2_android_team04_android.data.model.AiAnalysis
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

// AnalysisDaoAndroidTest : AnalysisDao(DB 접근 계층)의 핵심 동작을 검증하는 테스트
// 용도:
// - ai_analysis 테이블에 대해 저장(upsert) -> 조회(getByEntryId) 흐름이 정상 동작하는지 확인한다.
// - DiaryEntry(일기)와 AiAnalysis(분석 결과)가 entry_id로 연결되는 구조이므로, 실제로 일기 1건을 먼저 만들고, 그 entryId로 분석 결과를 저장/조회하는 시나리오를 테스트한다.
@RunWith(AndroidJUnit4::class)
class AnalysisDaoAndroidTest {

    @Test
    fun upsert_and_get() {
        // 테스트용 Application Context 획득
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // 테스트 독립성 보장
        TestDbUtil.wipeDb(context)

        // 실제 SQLite DB 인스턴스 획득
        // - writableDatabase를 사용해 INSERT/UPDATE가 가능하게 한다.
        val db = AppDbHelper(context).writableDatabase

        // 테스트 대상 DAO 준비
        // - diary_entries(일기) 테이블: DiaryDao
        // - ai_analysis(분석) 테이블: AnalysisDao
        val diaryDao = DiaryDao(db)
        val analysisDao = AnalysisDao(db)

        // 1) 선행조건: 분석은 특정 entry_id(일기)에 종속되므로, 먼저 일기 1건을 upsert로 생성한다.
        // - ownerId는 비회원 테스트용 값으로 고정
        // - dateYmd는 UNIQUE(owner_id, date_ymd) 구조를 가정한 테스트 데이터
        val entryId = diaryDao.upsert(
            com.example.guru2_android_team04_android.data.model.DiaryEntry(
                ownerId = "ANON_TEST",
                dateYmd = "2026-01-10",
                title = "t",
                content = "c",
                mood = com.example.guru2_android_team04_android.data.model.Mood.NORMAL,
                tags = listOf("태그")
            )
        )

        // diaryDao.upsert가 실패하면 -1 또는 0이 나올 수 있으므로, 이 테스트에서는 정상 흐름 검증을 위해 entryId가 유효하다는 전제를 둔다.

        // 2) 분석 결과를 upsert로 저장한다.
        // - 설계상 entryId는 UNIQUE로 일기 1건당 분석 1건을 유지(있으면 UPDATE, 없으면 INSERT)한다.
        val aid = analysisDao.upsert(
            AiAnalysis(
                entryId = entryId,
                summary = "요약",
                triggerPattern = "트리거",
                actions = listOf("물 마시기"),
                hashtags = listOf("#키워드"),
                missionSummary = "하루에 작은 돌봄을 실천해요.",
                fullText = "코칭"
            )
        )

        // upsert 결과로 반환된 rowId(또는 analysis_id)가 0보다 크면 저장 성공으로 본다.
        assertTrue(aid > 0)

        // 3) entryId로 분석 결과를 다시 조회하여, 방금 저장한 값이 그대로 나오는지 확인한다.
        val loaded = analysisDao.getByEntryId(entryId)

        // 조회 결과가 null이면 저장 실패 또는 조회 로직 문제이므로 테스트 실패
        assertNotNull(loaded)

        // 핵심 필드들이 기대값과 일치하는지 검증
        assertEquals("요약", loaded!!.summary)
        assertEquals(listOf("#키워드"), loaded.hashtags)
        assertEquals("하루에 작은 돌봄을 실천해요.", loaded.missionSummary)
    }
}