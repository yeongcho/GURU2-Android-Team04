package com.example.guru2_android_team04_android.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.guru2_android_team04_android.TestDbUtil
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

// AppDbHelperAndroidTest : AppDbHelper(DB 스키마/생성 로직)가 올바르게 동작하는지 검증하는 테스트
// 용도:
// - 앱 실행 시 AppDbHelper가 SQLite DB를 생성/업그레이드하면서 필요한 테이블들이 실제로 만들어지는지 확인한다.
// - 특정 컬럼(is_favorite, is_temporary 등)이 포함되어 있는지도 확인하여,
//   "코드가 기대하는 스키마"와 "실제 생성된 DB 스키마"가 어긋나지 않도록 방지한다.
@RunWith(AndroidJUnit4::class)
class AppDbHelperAndroidTest {

    @Test
    fun creates_tables() {
        // 테스트용 Application Context 획득
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 테스트 독립성 보장
        TestDbUtil.wipeDb(context)

        // AppDbHelper를 통해 DB 인스턴스를 연다.
        // - readableDatabase를 호출하는 순간 내부적으로 DB 파일이 없으면 생성되며, onCreate()에서 스키마 생성 SQL이 실행된다.
        val helper = AppDbHelper(context)
        val db = helper.readableDatabase

        // 1) sqlite_master 테이블을 조회하여 현재 DB에 존재하는 모든 테이블 이름을 수집한다.
        // - sqlite_master는 SQLite가 관리하는 메타 테이블로, 스키마 정의(테이블/인덱스 등)를 가지고 있다.
        val tables = mutableSetOf<String>()
        db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
            while (c.moveToNext()) tables.add(c.getString(0))
        }

        // 2) 앱에서 사용하는 주요 테이블들이 실제로 생성되었는지 확인한다.
        // - AppDb.T.*는 테이블명 상수로, 문자열 오타/불일치 방지 목적
        Assert.assertTrue(tables.contains(AppDb.T.USERS))
        Assert.assertTrue(tables.contains(AppDb.T.ENTRIES))
        Assert.assertTrue(tables.contains(AppDb.T.ANALYSIS))
        Assert.assertTrue(tables.contains(AppDb.T.BADGES))
        Assert.assertTrue(tables.contains(AppDb.T.USER_BADGES))
        Assert.assertTrue(tables.contains(AppDb.T.MONTHLY))
        Assert.assertTrue(tables.contains(AppDb.T.SETTINGS))

        // 3) ENTRIES 테이블 컬럼 스키마 확인
        // - PRAGMA table_info(tableName)는 특정 테이블의 컬럼 목록을 반환한다.
        // - DAO/서비스 코드에서 is_favorite / is_temporary 컬럼을 사용하므로, DB 스키마에도 반드시 존재해야 한다.
        val cols = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info(${AppDb.T.ENTRIES})", null).use { c ->
            while (c.moveToNext()) {
                cols.add(c.getString(c.getColumnIndexOrThrow("name")))
            }
        }
        Assert.assertTrue(cols.contains("is_favorite"))
        Assert.assertTrue(cols.contains("is_temporary"))

        // 4) ANALYSIS 테이블 컬럼 스키마 확인
        // - AI 분석 결과에서 hashtags/actions 같은 리스트 데이터는 JSON 문자열로 저장하므로 hashtags_json이 필요하다.
        // - mission_summary는 카드 UI에서 미션 요약 표시를 위해 사용된다.
        val aCols = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info(${AppDb.T.ANALYSIS})", null).use { c ->
            while (c.moveToNext()) {
                aCols.add(c.getString(c.getColumnIndexOrThrow("name")))
            }
        }
        Assert.assertTrue(aCols.contains("hashtags_json"))
        Assert.assertTrue(aCols.contains("mission_summary"))
    }
}
