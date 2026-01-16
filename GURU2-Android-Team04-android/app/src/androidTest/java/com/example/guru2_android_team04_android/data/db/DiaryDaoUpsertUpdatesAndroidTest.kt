package com.example.guru2_android_team04_android.data.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.guru2_android_team04_android.TestDbUtil
import com.example.guru2_android_team04_android.data.model.DiaryEntry
import com.example.guru2_android_team04_android.data.model.Mood
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

// DiaryDaoUpsertUpdatesAndroidTest : DiaryDao.upsert()가 같은 사용자(ownerId) + 같은 날짜(dateYmd)에 대해 INSERT가 아니라 UPDATE처럼 동작하는지 검증하는 테스트
// 용도:
// - diary_entries 테이블에 UNIQUE(owner_id, date_ymd)가 설정되어 있다는 전제에서, 동일 owner/date로 upsert를 2번 호출하면 새 row 생성이 아니라 기존 row 갱신이 되어야 한다.
// - 사용자가 같은 날짜 일기를 다시 저장하면 수정으로 처리되는 설계를 테스트로 증명한다.
@RunWith(AndroidJUnit4::class)
class DiaryDaoUpsertUpdatesAndroidTest {

    @Test
    fun upsert_same_owner_and_date_updates_fields() {
        // 테스트는 Android 환경에서 실제 SQLite DB를 사용하므로 Application Context가 필요하다.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // 테스트 독립성 보장
        TestDbUtil.wipeDb(context)

        // writableDatabase 호출 시 DB가 없으면 생성되고(AppDbHelper.onCreate), 이후 DiaryDao가 테이블에 접근할 수 있는 상태가 된다.
        val db = AppDbHelper(context).writableDatabase
        val dao = DiaryDao(db)

        val owner = "ANON_TEST"
        val ymd = "2026-01-12"

        // 1) 최초 저장: 같은 owner/date에 해당하는 row가 없으므로 INSERT 경로로 저장된다.
        dao.upsert(
            DiaryEntry(
                ownerId = owner,
                dateYmd = ymd,
                title = "초기 제목",
                content = "초기 내용",
                mood = Mood.CALM,
                tags = listOf("태그1")
            )
        )

        // 2) 같은 owner/date로 다시 저장:
        // - UNIQUE(owner_id, date_ymd) 조건 때문에 동일 날짜 일기 1개만 유지 정책이 적용된다.
        // - upsert 구현은 update를 먼저 시도하므로 여기서는 UPDATE 경로로 동작해야 한다.
        dao.upsert(
            DiaryEntry(
                ownerId = owner,
                dateYmd = ymd,
                title = "수정 제목",
                content = "수정 내용",
                mood = Mood.JOY,
                tags = listOf("태그2", "태그3")
            )
        )

        // 3) 검증: getByDate로 조회했을 때 값이 수정된 상태여야 한다.
        val loaded = dao.getByDate(owner, ymd)

        // 예외처리) 조회 결과가 없으면(저장이 실패했거나 where 조건이 잘못된 경우) null일 수 있으므로 먼저 null 체크
        assertNotNull(loaded)

        // title/content/mood가 두 번째 upsert 값으로 업데이트되었는지 확인한다.
        assertEquals("수정 제목", loaded!!.title)
        assertEquals("수정 내용", loaded.content)
        assertEquals(Mood.JOY, loaded.mood)
    }
}
