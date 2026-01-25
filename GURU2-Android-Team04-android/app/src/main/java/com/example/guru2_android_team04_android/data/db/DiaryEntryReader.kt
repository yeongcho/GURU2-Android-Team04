package com.example.guru2_android_team04_android.data

import android.content.Context
import com.example.guru2_android_team04_android.data.db.AppDb
import com.example.guru2_android_team04_android.data.db.AppDbHelper
import com.example.guru2_android_team04_android.data.model.DiaryEntry
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.util.JsonMini

// DiaryEntryReader : 화면(오늘일기/분석)에서 entryId로 일기 원문이 필요할 때 사용하는 "읽기 전용" 헬퍼
// 설계 이유:
// - AppService는 entryId로 DiaryEntry를 로드하는 함수가 private이어서(UI에서 직접 접근 불가)
// - Service 수정 없이 화면 연동을 위해 DB에서 최소 조회만 수행한다.
class DiaryEntryReader(context: Context) {

    private val appContext = context.applicationContext
    private val helper = AppDbHelper(appContext)

    fun getByIdOrNull(entryId: Long): DiaryEntry? {
        val db = helper.readableDatabase
        return db.rawQuery(
            """
            SELECT entry_id, owner_id, date_ymd, title, content, mood, tags_json, is_favorite, is_temporary, created_at, updated_at
            FROM ${AppDb.T.ENTRIES}
            WHERE entry_id=?
            LIMIT 1
            """.trimIndent(),
            arrayOf(entryId.toString())
        ).use { c ->
            if (!c.moveToFirst()) return null
            DiaryEntry(
                entryId = c.getLong(0),
                ownerId = c.getString(1),
                dateYmd = c.getString(2),
                title = c.getString(3),
                content = c.getString(4),
                mood = Mood.fromDb(c.getInt(5)),
                tags = JsonMini.jsonToList(c.getString(6)),
                isFavorite = c.getInt(7) == 1,
                isTemporary = c.getInt(8) == 1,
                createdAt = c.getLong(9),
                updatedAt = c.getLong(10)
            )
        }
    }
}
