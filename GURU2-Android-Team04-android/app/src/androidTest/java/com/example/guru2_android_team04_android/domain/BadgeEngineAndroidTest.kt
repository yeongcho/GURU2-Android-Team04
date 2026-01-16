package com.example.guru2_android_team04_android.domain

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.guru2_android_team04_android.TestDbUtil
import com.example.guru2_android_team04_android.data.db.AppDbHelper
import com.example.guru2_android_team04_android.data.db.DiaryDao
import com.example.guru2_android_team04_android.data.model.DiaryEntry
import com.example.guru2_android_team04_android.data.model.Mood
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// BadgeEngineAndroidTest : BadgeEngine(배지 지급 로직)이 DB 상태에 따라 배지를 정상 지급하는지 검증하는 테스트
// 용도:
// - BadgeEngine은 일기 개수/연속 작성/감정 다양성 같은 규칙을 만족하면 배지를 지급한다.
// - 배지 지급 결과는 user_badges 테이블에 기록된다.
// - 이 테스트는 첫 일기 작성 상황에서 배지(badge_id=1)가 지급되는지 확인한다.
@RunWith(AndroidJUnit4::class)
class BadgeEngineAndroidTest {

    @Test
    fun grants_first_entry_badge() {
        // Android Instrumentation 테스트이므로 Application Context가 필요하다.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // 테스트 독립성 보장
        TestDbUtil.wipeDb(context)

        // writableDatabase를 여는 순간, DB가 없으면 생성(onCreate)되고 seed 데이터(배지 목록 등)가 들어갈 수 있다.
        val db = AppDbHelper(context).writableDatabase
        val diaryDao = DiaryDao(db)
        val engine = BadgeEngine(db)

        // ownerId는 사용자 구분 키(회원/비회원 공통)로 동작한다.
        val owner = "ANON_TEST"

        // 1) "첫 일기"를 1개 저장한다.
        // - BadgeEngine은 DiaryDao를 통해 일기 개수 등을 집계하므로, 테스트는 배지 지급 조건을 만들기 위해 최소 1개의 entry를 넣는다.
        diaryDao.upsert(
            DiaryEntry(
                ownerId = owner,
                dateYmd = "2026-01-10",
                title = "t",
                content = "c",
                mood = Mood.JOY,
                tags = listOf()
            )
        )

        // 2) 배지 지급 로직 실행
        // - checkAndGrant는 현재 DB 상태를 기준으로 조건을 만족하는 배지를 user_badges에 INSERT한다.
        engine.checkAndGrant(owner)

        // 3) user_badges 테이블에서 badge_id=1이 지급(INSERT)되었는지 확인한다.
        // - SELECT 1 + LIMIT 1 패턴으로 "존재 여부"만 최소 비용으로 검사한다.
        val earned = db.rawQuery(
            "SELECT 1 FROM user_badges WHERE owner_id=? AND badge_id=1 LIMIT 1",
            arrayOf(owner)
        ).use { c ->
            // 예외처리) 결과 row가 없으면 moveToFirst()가 false → earned=false
            c.moveToFirst()
        }

        // 4) badge_id=1이 지급되었어야 한다.
        assertTrue(earned)
    }
}
