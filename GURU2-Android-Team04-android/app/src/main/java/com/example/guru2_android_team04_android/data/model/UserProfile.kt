package com.example.guru2_android_team04_android.data.model

// UserProfile : 프로필 화면에 필요한 정보를 한 번에 담아 전달하는 모델
// 용도:
// - AppService.getUserProfile()에서 UI로 내려주는 최종 응답 형태로 사용한다.
// - 회원/비회원(익명) 프로필을 동일한 구조로 표현하기 위해 만든 DTO.
// 설계:
// - ownerId: "USER_xxx"(회원) 또는 "ANON_xxx"(비회원)로 사용자 세션을 식별한다.
// - emailOrAnon: 회원이면 email, 비회원이면 "ANON" 같은 표시용 문자열을 담는다.
// - serviceDays: 가입일/익명 시작일 기준으로 계산한 앱 이용 일수(배지/프로필 표시용).
// - selectedBadge: 사용자가 대표로 선택한 배지(없으면 null).
// - profileImageUri: settings 테이블에 저장된 프로필 이미지 경로/URI(없으면 null).
data class UserProfile(
    // 세션/데이터 소유자 식별자
    // - 회원: "USER_<userId>"
    // - 비회원: "ANON_<random>"
    val ownerId: String,

    // 프로필에 표시할 닉네임
    // - 비회원일 경우 "익명" 같은 기본값을 넣어 사용한다.
    val nickname: String,

    // 프로필에 표시할 이메일 또는 익명 표시 문자열
    val emailOrAnon: String,

    // 서비스 이용 일수(일 단위)
    // - 회원: 가입(createdAt) 기준
    // - 비회원: 익명 세션 시작 시각 기준
    val serviceDays: Long,

    // 선택된 대표 배지(없으면 null)
    // - BadgeDao에서 조회한 결과를 담는다.
    val selectedBadge: Badge?,

    // 프로필 이미지 URI/파일명 (settings 테이블에 저장)
    // - null이면 기본 이미지를 사용하도록 UI에서 처리한다.
    val profileImageUri: String?
)
