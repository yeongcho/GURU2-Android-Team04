package com.example.guru2_android_team04_android.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.guru2_android_team04_android.TestDbUtil
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

// AppDbSchemaAndroidTest : 앱이 기대하는 SQLite 스키마(테이블/컬럼)가 실제로 생성되는지 검증하는 테스트
// 용도:
// - AppDbHelper가 DB를 생성/마이그레이션한 결과가 DAO/서비스 코드가 요구하는 형태인지 확인한다.
// - 테이블명/컬럼명이 바뀌거나 마이그레이션이 누락되면, 앱 실행 중 런타임 크래시가 날 수 있으므로 테스트에서 조기 발견한다.
// - SETTINGS의 setting_key 같은 마이그레이션에서 변경된 컬럼이 반영됐는지 확인하는 안전장치로 사용한다.
@RunWith(AndroidJUnit4::class)
class AppDbSchemaAndroidTest {

    @Test
    fun schema_has_required_tables_and_columns() {
        // Android Instrumentation 환경에서 테스트용 Context를 얻는다.
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 테스트 독립성 보장
        TestDbUtil.wipeDb(context)

        // DB 연결:
        // - readableDatabase에 "최초로" 접근하는 순간 DB 파일이 없으면 생성이 진행된다.
        // - 이때 AppDbHelper.onCreate가 호출되어 테이블들이 만들어진다.
        val db = AppDbHelper(context).readableDatabase

        // 생성된 테이블 목록 수집:
        // - sqlite_master는 SQLite가 관리하는 메타 테이블로, 테이블/인덱스/뷰의 정의가 저장된다.
        // - android_metadata / sqlite_sequence는 시스템 테이블이므로 검증 대상에서 제외한다.
        val tables = mutableSetOf<String>()
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_metadata' AND name NOT LIKE 'sqlite_sequence'",
            null
        ).use { c ->
            while (c.moveToNext()) tables.add(c.getString(0))
        }

        // 핵심 테이블 존재 여부 확인:
        // - 테이블명은 코드 전체에서 AppDb.T 상수로 참조하므로, 테스트도 상수를 그대로 사용해 일관성을 유지한다.
        assertTrue("USERS 테이블 누락", tables.contains(AppDb.T.USERS))
        assertTrue("ENTRIES 테이블 누락", tables.contains(AppDb.T.ENTRIES))
        assertTrue("ANALYSIS 테이블 누락", tables.contains(AppDb.T.ANALYSIS))
        assertTrue("MONTHLY 테이블 누락", tables.contains(AppDb.T.MONTHLY))
        assertTrue("BADGES 테이블 누락", tables.contains(AppDb.T.BADGES))
        assertTrue("USER_BADGES 테이블 누락", tables.contains(AppDb.T.USER_BADGES))
        assertTrue("SETTINGS 테이블 누락", tables.contains(AppDb.T.SETTINGS))

        // ENTRIES 테이블 주요 컬럼 확인:
        // - is_favorite: 즐겨찾기(하트) 여부를 저장하는 플래그
        // - is_temporary: 비회원 임시 저장글을 목록/통계에서 숨기기 위한 플래그
        val entryCols = pragmaCols(db, AppDb.T.ENTRIES)
        assertTrue("ENTRIES: is_favorite 컬럼 누락", entryCols.contains("is_favorite"))
        assertTrue("ENTRIES: is_temporary 컬럼 누락", entryCols.contains("is_temporary"))

        // ANALYSIS 테이블 주요 컬럼 확인:
        // - hashtags_json: 해시태그(List<String>)를 JSON 문자열로 저장하는 컬럼
        // - mission_summary: 행동 제안(미션)을 대표하는 1줄 요약 문장 컬럼
        val analysisCols = pragmaCols(db, AppDb.T.ANALYSIS)
        assertTrue("ANALYSIS: hashtags_json 컬럼 누락", analysisCols.contains("hashtags_json"))
        assertTrue("ANALYSIS: mission_summary 컬럼 누락", analysisCols.contains("mission_summary"))

        // SETTINGS 테이블 컬럼 확인:
        // - setting_key: 설정 키 컬럼(예: profile_image_uri)
        // - 마이그레이션에서 컬럼 rename/재생성이 있었다면, 여기서 누락될 수 있으니 별도 체크한다.
        val settingsCols = pragmaCols(db, AppDb.T.SETTINGS)
        assertTrue("SETTINGS: setting_key 컬럼 누락 (RENAME 확인)", settingsCols.contains("setting_key"))
    }

    // 특정 테이블의 컬럼명을 Set으로 반환하는 헬퍼 함수
    // 용도:
    // - PRAGMA table_info 결과를 공통 로직으로 묶어서, 테이블별 컬럼 검증 코드를 간결하게 유지한다.
    private fun pragmaCols(
        db: android.database.sqlite.SQLiteDatabase,
        table: String
    ): Set<String> {
        val cols = mutableSetOf<String>()

        // PRAGMA table_info(table):
        // - cid, name, type, notnull, dflt_value, pk 등 "컬럼 메타데이터"를 반환한다.
        db.rawQuery("PRAGMA table_info($table)", null).use { c ->
            // 예외처리) "name" 컬럼이 없으면 PRAGMA 결과 자체가 비정상이다.
            // 이 경우 getColumnIndexOrThrow가 예외를 던져 테스트가 즉시 실패하도록 한다.
            val nameIdx = c.getColumnIndexOrThrow("name")
            while (c.moveToNext()) {
                cols.add(c.getString(nameIdx))
            }
        }

        return cols
    }
}
