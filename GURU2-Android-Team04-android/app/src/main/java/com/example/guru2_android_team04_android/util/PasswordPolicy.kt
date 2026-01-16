package com.example.guru2_android_team04_android.util

// PasswordPolicy : 사용자 비밀번호 강도 규칙을 정의한 유틸리티
// 용도:
// - 회원가입 및 비밀번호 설정 시 최소한의 보안 수준을 만족하는지 검증하기 위해 사용된다.
// - AppService.signUp()에서 비밀번호 정책 검사에 활용된다.
// 설계:
// - "검증만 담당"하는 순수 유틸리티로, 저장/암호화 로직과 분리
// - 비밀번호를 String이 아닌 CharArray로 받아 사용 후 메모리에서 지우기 쉬운 구조를 유지한다.
object PasswordPolicy {

    // 비밀번호 강도 검사
    // 정책:
    // - 길이: 10자 이상 15자 이하
    // - 구성: 영문자 최소 1개 + 숫자 최소 1개
    // 파라미터:
    // - pw: 사용자 입력 비밀번호(CharArray)
    // 반환:
    // - true  : 정책을 만족하는 비밀번호
    // - false : 길이/구성 중 하나라도 조건을 만족하지 않음
    fun isStrong(pw: CharArray): Boolean {

        // 길이 제한 검사
        // 예외처리) 너무 짧거나 긴 비밀번호는 즉시 거절
        if (pw.size !in 10..15) return false

        var hasLetter = false
        var hasDigit = false

        // 문자 구성 검사
        // - 영문자/숫자를 하나씩이라도 포함하는지 확인
        for (ch in pw) {
            if (ch.isLetter()) hasLetter = true
            if (ch.isDigit()) hasDigit = true

            // 두 조건을 모두 만족하면 더 볼 필요 없이 통과
            if (hasLetter && hasDigit) return true
        }
        // 영문자 또는 숫자 중 하나라도 없으면 실패
        return false
    }
}
