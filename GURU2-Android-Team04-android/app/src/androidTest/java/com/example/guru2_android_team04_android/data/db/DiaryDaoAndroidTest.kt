package com.example.guru2_android_team04_android.data.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.guru2_android_team04_android.TestDbUtil
import com.example.guru2_android_team04_android.data.model.DiaryEntry
import com.example.guru2_android_team04_android.data.model.Mood
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

// DiaryDaoAndroidTest : DiaryDao(일기 CRUD/조회)와 FavoriteDao(즐겨찾기 변경)가 DB에 정상 반영되는지 검증하는 테스트
// 용도:
// - DiaryDao.upsert()가 INSERT/UPDATE(Upsert)로 동작하는지 확인한다.
// - DiaryDao.getByDate()가 특정 사용자(ownerId) + 날짜(dateYmd) 기준으로 1건을 정확히 조회하는지 확인한다.
// - FavoriteDao.set()로 변경한 즐겨찾기(하트) 상태가 diary_entries.is_favorite 컬럼에 저장되어,
//   다시 DiaryDao로 조회했을 때 그대로 반영되는지 확인한다.
// - DiaryDao.getMoodMapByMonth()가 월 범위 조회 결과를 날짜 -> 감정 Map으로 올바르게 반환하는지 확인한다.
@RunWith(AndroidJUnit4::class)
class DiaryDaoAndroidTest {

    @Test
    fun upsert_and_getByDate() {
        // Android 런타임에서 동작하는 테스트이므로 Application Context를 가져온다.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // 테스트 독립성 보장:
        TestDbUtil.wipeDb(context)

        // writableDatabase를 열면 DB가 없을 경우 생성되며(AppDbHelper.onCreate), 테이블 스키마가 준비된 상태에서 DAO 테스트를 수행할 수 있다.
        val db = AppDbHelper(context).writableDatabase
        val dao = DiaryDao(db)

        val owner = "ANON_TEST"
        val ymd = "2026-01-10"

        // upsert는 (owner_id, date_ymd) UNIQUE를 기반으로
        // - 동일한 날짜가 없으면 INSERT
        // - 동일한 날짜가 있으면 UPDATE
        // 처럼 동작한다.
        val id = dao.upsert(
            DiaryEntry(
                ownerId = owner,
                dateYmd = ymd,
                title = "제목",
                content = "내용",
                mood = Mood.JOY,
                tags = listOf("기쁨")
            )
        )

        // 예외처리) insert가 실패하면 SQLite 규약상 -1이 나올 수 있으므로, 정상 저장을 의미하는 "양수 id"인지 확인한다.
        assertTrue(id > 0)

        // getByDate는 ownerId + dateYmd로 1건을 조회한다.
        val loaded = dao.getByDate(owner, ymd)

        // 예외처리) 해당 날짜에 데이터가 없으면 null이므로 null 여부를 먼저 확인한다.
        assertNotNull(loaded)

        // 저장한 값이 그대로 들어갔는지 확인한다.
        assertEquals("제목", loaded!!.title)
        assertEquals(Mood.JOY, loaded.mood)

        // 즐겨찾기 기본값은 false(0)로 저장되는 구조이므로 false가 맞는지 확인한다.
        assertFalse(loaded.isFavorite)
    }

    @Test
    fun setFavorite_persists() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        TestDbUtil.wipeDb(context)

        val db = AppDbHelper(context).writableDatabase
        val diaryDao = DiaryDao(db)

        // 즐겨찾기 기능은 FavoriteDao가 담당한다.
        // - 소유자(ownerId) 검증을 먼저 하고, 통과하면 diary_entries.is_favorite 값을 0/1로 UPDATE 한다.
        val favoriteDao = FavoriteDao(db)

        val owner = "ANON_TEST"

        // 먼저 일기 1건을 저장해 entryId를 확보한다.
        val id = diaryDao.upsert(
            DiaryEntry(
                ownerId = owner,
                dateYmd = "2026-01-11",
                title = "t",
                content = "c",
                mood = Mood.CALM,
                tags = listOf()
            )
        )

        // favoriteDao.set은 성공/실패를 Boolean으로 반환한다.
        // - true: update가 반영됨(소유자 검증 통과 + row 업데이트 성공)
        // - false: 소유자가 아니거나 row가 없거나 업데이트 실패
        val updated = favoriteDao.set(owner, id, true)
        assertTrue(updated)

        // 다시 DiaryDao로 조회했을 때 isFavorite가 true로 반영되어야 한다.
        val loaded = diaryDao.getByDate(owner, "2026-01-11")
        assertNotNull(loaded)
        assertTrue(loaded!!.isFavorite)
    }

    @Test
    fun moodMapByMonth() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        TestDbUtil.wipeDb(context)

        val db = AppDbHelper(context).writableDatabase
        val dao = DiaryDao(db)

        val owner = "ANON_TEST"

        // 같은 달(2026-01)에 서로 다른 날짜/감정으로 2건을 저장한다.
        dao.upsert(
            DiaryEntry(
                ownerId = owner,
                dateYmd = "2026-01-01",
                title = "t",
                content = "c",
                mood = Mood.CALM,
                tags = listOf()
            )
        )
        dao.upsert(
            DiaryEntry(
                ownerId = owner,
                dateYmd = "2026-01-02",
                title = "t",
                content = "c",
                mood = Mood.ANGRY,
                tags = listOf()
            )
        )

        // getMoodMapByMonth는 "YYYY-MM"을 받아서 그 달의 date_ymd -> mood 형태 Map을 만든다.
        // - 캘린더 UI에서 날짜별 감정 아이콘을 표시할 때 사용한다.
        val map = dao.getMoodMapByMonth(owner, "2026-01")

        // 날짜 키로 조회했을 때 저장한 감정 enum이 정확히 매핑되는지 확인한다.
        assertEquals(Mood.CALM, map["2026-01-01"])
        assertEquals(Mood.ANGRY, map["2026-01-02"])
    }
}
