package com.example.guru2_android_team04_android.security

import com.example.guru2_android_team04_android.util.PasswordPolicy
import org.junit.Assert.*
import org.junit.Test

// PasswordPolicyTest : PasswordPolicy(비밀번호 강도 규칙) 단위 테스트
// 용도:
// - 회원가입/비밀번호 설정 시 사용되는 PasswordPolicy.isStrong() 규칙이 요구사항(길이 + 문자 구성)을 정확히 만족하는지 검증한다.
// 검증 대상 규칙 :
// - 길이: 10~15자(포함)
// - 구성: 영문(letter) 1개 이상 + 숫자(digit) 1개 이상 포함
// - 위 조건을 만족하면 true, 아니면 false
class PasswordPolicyTest {

    @Test
    fun valid_when_10_to_15_chars_and_contains_letter_and_digit() {
        // 테스트 목적:
        // - 길이 조건(10~15) + 문자 조건(영문+숫자)을 만족하는 경우 true인지 확인한다.

        // 10자이며 영문/숫자 혼합
        assertTrue(PasswordPolicy.isStrong("a1b2c3d4e5".toCharArray()))

        // 10자이며 영문/숫자 혼합
        assertTrue(PasswordPolicy.isStrong("abcde12345".toCharArray()))

        // 13자이며 영문/숫자 혼합
        assertTrue(PasswordPolicy.isStrong("abcd1234efgh5".toCharArray()))

        // 15자이며 영문/숫자 혼합(상한값 케이스 포함)
        assertTrue(PasswordPolicy.isStrong("abcde12345fghij".toCharArray()))
    }

    @Test
    fun invalid_when_too_short_or_too_long() {
        // 테스트 목적:
        // - 길이 조건을 벗어나면(9자 이하 또는 16자 이상) 무조건 false인지 확인한다.
        // - 영문/숫자 조건을 만족하더라도 길이가 범위를 벗어나면 실패해야 한다.

        // 9자(최소 길이 10 미만)
        assertFalse(PasswordPolicy.isStrong("a1b2c3d4e".toCharArray()))

        // 16자(최대 길이 15 초과)
        assertFalse(PasswordPolicy.isStrong("abcde12345fghijk".toCharArray()))
    }

    @Test
    fun invalid_when_missing_letter_or_digit() {
        // 테스트 목적:
        // - 길이가 맞더라도 영문 또는 숫자만이면 false인지 확인한다.
        // - PasswordPolicy는 isLetter / isDigit 둘 다 최소 1개씩을 요구한다.

        // 예외처리) 숫자만 존재(영문 없음)
        assertFalse(PasswordPolicy.isStrong("1234567890".toCharArray()))

        // 예외처리) 영문만 존재(숫자 없음)
        assertFalse(PasswordPolicy.isStrong("abcdefghij".toCharArray()))

        // 예외처리) 대문자 영문만 존재(숫자 없음)
        assertFalse(PasswordPolicy.isStrong("ABCDEFGHIJKL".toCharArray()))
    }

    @Test
    fun edge_lengths_10_and_15() {
        // 테스트 목적:
        // - 경계값(최소 10자, 최대 15자)이 포함으로 처리되는지 확인한다.
        // - 경계값에서도 영문/숫자 조건을 만족하면 true여야 한다.

        // 10자 경계값: 영문 2개(A, b) + 숫자 포함
        assertTrue(PasswordPolicy.isStrong("A23456789b".toCharArray()))     // 10

        // 15자 경계값: 영문 포함 + 숫자 포함
        assertTrue(PasswordPolicy.isStrong("A23456789bC3456".toCharArray()))// 15
    }
}