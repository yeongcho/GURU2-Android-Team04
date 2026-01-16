package com.example.guru2_android_team04_android.data.db

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.guru2_android_team04_android.TestDbUtil
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// BadgeDaoAndroidTest : BadgeDao(배지 조회/상태 조회)가 정상 동작하는지 확인하는 테스트
// 용도:
// - 앱이 처음 실행될 때 seedBadges로 기본 배지 데이터가 DB에 삽입되는지 검증한다.
// - BadgeDao.getAllBadgeStatuses(ownerId)가 "전체 배지 목록"을 빠짐없이 반환하는지 검증한다.
@RunWith(AndroidJUnit4::class)
class BadgeDaoAndroidTest {

    @Test
    fun seed_badges_exist_and_status_list_returns_all() {
        // Android 런타임에서 동작하는 테스트이므로 Application Context를 가져온다.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // 테스트 독립성 보장
        TestDbUtil.wipeDb(context)

        // readableDatabase에 접근하는 순간 DB가 없으면 생성된다.
        val db = AppDbHelper(context).readableDatabase
        val dao = BadgeDao(db)

        // 특정 ownerId에 대한 배지 상태 목록을 가져온다.
        // - 구현상 보통 badges(전체 목록) LEFT JOIN user_badges(획득/선택 정보) 형태로 구성된다.
        // - 따라서 user_badges에 기록이 없어도 전체 badges는 모두 반환되어야 한다.
        val statuses = dao.getAllBadgeStatuses("ANON_TEST")

        // seedBadges에서 5개를 넣는 구조라면, 상태 리스트도 반드시 5개가 되어야 한다.
        assertEquals(5, statuses.size)
    }
}
