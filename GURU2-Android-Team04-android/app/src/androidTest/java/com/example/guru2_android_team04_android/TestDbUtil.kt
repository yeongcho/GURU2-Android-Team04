package com.example.guru2_android_team04_android

import android.content.Context
import com.example.guru2_android_team04_android.data.db.AppDb

// TestDbUtil : AndroidTest에서 DB를 초기화하기 위한 테스트 전용 유틸리티
// 용도:
// - 각 테스트가 서로 영향을 주지 않도록(테스트 독립성) 실행 전 DB 파일을 삭제해 빈 상태로 만든다.
// - AppDbHelper를 새로 열 때 onCreate/onUpgrade가 다시 수행되며, 스키마/시드 데이터가 다시 생성된다.
object TestDbUtil {

    // 앱의 SQLite DB 파일을 삭제하여 DB를 완전히 초기화한다.
    // 동작 방식:
    // - Context.deleteDatabase(DB_NAME)를 호출하면, 앱 내부 저장소에 있는 해당 DB 파일이 제거된다.
    // - 이후 AppDbHelper로 DB를 열면 "새 DB"로 인식되어 테이블이 다시 생성된다.
    fun wipeDb(context: Context) {
        context.deleteDatabase(AppDb.DB_NAME)
    }
}
