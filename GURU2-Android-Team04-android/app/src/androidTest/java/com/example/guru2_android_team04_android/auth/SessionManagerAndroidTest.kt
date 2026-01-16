package com.example.guru2_android_team04_android.auth

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

// SessionManagerAndroidTest : SessionManager의 안드로이드 환경 동작을 검증하는 테스트
// 용도:
// - SessionManager가 SharedPreferences(또는 내부 저장소)에 값을 제대로 저장/로드하는지 확인한다.
// - 특히 비회원(익명) 세션에서 사용하는 ownerId(ANON_*)와 생성 시각(createdAt)이 앱 실행/인스턴스가 바뀌어도 "한 번 생성되면 계속 유지"되는지(=persist) 검증한다.
@RunWith(AndroidJUnit4::class)
class SessionManagerAndroidTest {

    @Test
    fun anon_id_persists() {
        // 테스트용 Context 획득
        // - instrumentation 환경에서 Application Context를 가져온다.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // 테스트 대상 생성
        val sm = SessionManager(context)

        // 테스트 독립성 보장: 이전 테스트 실행 결과(저장된 값)가 남아있으면 persist 검증이 왜곡될 수 있으므로 초기화한다.
        sm.clear()

        // 1) 최초 호출: 익명 ownerId가 없으면 새로 생성되어야 한다.
        val a1 = sm.getOrCreateAnonOwnerId()

        // 2) 두 번째 호출: 이미 생성된 값이 있으므로 같은 값이 반환되어야 한다.
        val a2 = sm.getOrCreateAnonOwnerId()

        // 두 값이 동일해야 한다.
        assertEquals(a1, a2)

        // 익명 ownerId 규칙 검증: ANON_ 접두어로 시작해야 한다.
        assertTrue(a1.startsWith("ANON_"))
    }

    @Test
    fun anon_created_at_persists() {
        // 테스트용 Context 획득
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // 테스트 대상 생성
        val sm = SessionManager(context)

        // createdAt은 "익명 세션 최초 생성 시각"을 저장하는 값이다.
        // 1) 최초 호출: 값이 없으면 현재 시각 기반으로 생성되어 저장된다.
        val t1 = sm.getOrCreateAnonCreatedAt()

        // 2) 두 번째 호출: 저장된 값이 그대로 반환되어야 한다.
        val t2 = sm.getOrCreateAnonCreatedAt()

        // 두 값이 동일해야 한다.
        assertEquals(t1, t2)

        // 생성 시각은 Unix time(ms) 형태이므로 0보다 커야 정상이다.
        assertTrue(t1 > 0)
    }
}