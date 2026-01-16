package com.example.guru2_android_team04_android.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// Pbkdf2AndroidTest : 비밀번호 해시 유틸(Pbkdf2)의 핵심 보안 성질을 검증하는 테스트
// 용도:
// - 회원가입/로그인 기능에서 평문 비밀번호를 저장하지 않고 안전하게 검증하기 위해 Pbkdf2.hash / Pbkdf2.verify가 정상 동작하는지 확인한다.
@RunWith(AndroidJUnit4::class)
class Pbkdf2AndroidTest {

    @Test
    fun hash_and_verify_success() {
        // 테스트 목적:
        // - hash()가 만든 문자열을 verify()로 검증했을 때 올바른 비밀번호는 통과(true), 잘못된 비밀번호는 실패(false)하는지 확인한다.
        val pw = "hello1234".toCharArray()

        // hash(): salt + 반복 횟수 + 파생키를 포함한 문자열을 만든다.
        // - 실제 DB에는 이 문자열만 저장하고, 로그인 시 verify로 비교한다.
        val hash = Pbkdf2.hash(pw)

        // 올바른 비밀번호로는 인증이 성공해야 한다.
        assertTrue(Pbkdf2.verify("hello1234".toCharArray(), hash))

        // 잘못된 비밀번호로는 인증이 실패해야 한다.
        assertFalse(Pbkdf2.verify("wrong".toCharArray(), hash))
    }

    @Test
    fun hashes_are_different_due_to_salt() {
        // 테스트 목적:
        // - 같은 비밀번호를 2번 hash()해도 결과가 같으면 안 된다.
        // - 이유: 매번 랜덤 salt를 사용해야 레인보우 테이블/사전 공격에 강해진다.
        val pw = "samepw".toCharArray()

        // 같은 입력이지만 salt가 다르기 때문에 결과 문자열은 달라야 한다.
        val h1 = Pbkdf2.hash(pw)
        val h2 = Pbkdf2.hash(pw)

        // salt가 랜덤이면 해시 문자열 전체가 달라지는 것이 정상이다.
        // 예외처리) 만약 이 값이 같다면 salt가 고정이거나 RNG에 문제가 있는 심각한 버그 가능성이 있다.
        assertNotEquals(h1, h2)

        // 서로 다른 해시라도 verify는 "각 해시에 맞는 비밀번호"를 통과시켜야 한다.
        assertTrue(Pbkdf2.verify("samepw".toCharArray(), h1))
        assertTrue(Pbkdf2.verify("samepw".toCharArray(), h2))
    }
}
