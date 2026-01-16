package com.example.guru2_android_team04_android.util

import android.util.Patterns

// EmailPolicy : 이메일 입력값에 대한 정규화 + 검증 규칙을 모아둔 유틸리티 객체
// 용도:
// - 회원가입/로그인 시 사용자가 입력한 이메일을 일관된 규칙으로 처리한다.
// - DB 저장 전 이메일 형식을 검증하여 잘못된 입력을 사전에 차단한다.
object EmailPolicy {

    // 사용자 입력 이메일을 정규화한다.
    // 처리 내용:
    // - 앞뒤 공백 제거(trim)
    // - 소문자로 변환(lowercase)
    //
    // 설계:
    // - "Test@Email.com" 과 "test@email.com"을 동일 계정으로 취급하기 위해 저장/비교 전에 소문자로 통일한다.
    fun normalize(email: String): String =
        email.trim().lowercase()

    // 이메일 형식이 올바른지 검사한다.
    // 구현:
    // - Android에서 제공하는 Patterns.EMAIL_ADDRESS 정규식을 사용
    // 반환:
    // - true  : 이메일 형식이 유효함
    // - false : 빈 문자열이거나 형식이 올바르지 않음
    fun isValid(email: String): Boolean {
        val e = email.trim()
        // 예외처리) 공백 제거 후 빈 값이면 이메일로 인정하지 않는다.
        if (e.isEmpty()) return false
        return Patterns.EMAIL_ADDRESS.matcher(e).matches()
    }
}
