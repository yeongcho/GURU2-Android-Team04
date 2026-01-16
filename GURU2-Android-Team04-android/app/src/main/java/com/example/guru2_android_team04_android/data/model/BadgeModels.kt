package com.example.guru2_android_team04_android.data.model

// Badge : 앱에서 정의한 배지(업적)의 기본 정보 모델
// 용도:
// - 사용자가 특정 조건을 달성했을 때 획득할 수 있는 배지를 표현한다.
// - 배지 자체의 정적인 정보(이름, 설명, 조건 정의)를 담는다.
// 설계:
// - 배지의 정의 정보와 사용자 상태 정보를 분리한다.
// - 실제로 사용자가 배지를 얻었는지 여부는 BadgeStatus에서 관리한다.
data class Badge(
    // 배지 고유 ID (PK)
    // - DB 및 코드 전반에서 배지를 식별하는 기준 값
    val badgeId: Int,

    // 배지 이름
    // - UI에 표시되는 배지 제목
    val name: String,

    // 배지 설명
    // - 배지를 획득하기 위한 조건 설명 또는 축하 문구
    val description: String,

    // 배지 조건 타입
    // - 어떤 기준으로 배지를 판단하는지 나타낸다.
    // - BadgeEngine에서 ruleType에 따라 로직 분기 처리
    val ruleType: String,

    // 배지 조건 값
    // - ruleType과 함께 사용되는 기준 숫자
    // - 예: ENTRY_COUNT + 10 → 일기 10개 작성 시 획득
    val ruleValue: Int
)

// BadgeStatus : 특정 사용자 기준의 배지 상태를 나타내는 모델
// 용도:
// - "이 배지를 사용자가 획득했는지", "현재 프로필에 선택했는지"를 표현한다.
// - Badge + 사용자 상태 정보를 합친 ViewModel/DTO 성격의 클래스
// - 배지 목록 화면, 프로필 배지 선택 화면에서 주로 사용된다.
// 설계 의도:
// - Badge(정의 정보)와 사용자 상태를 한 번에 UI로 전달하기 위함
// - DB 구조를 그대로 노출하지 않고, 화면에 필요한 형태로 조합
data class BadgeStatus(
    // 배지 정의 정보
    // - 공통 배지 정보 (이름, 설명, 조건 등)
    val badge: Badge,

    // 해당 사용자가 이 배지를 획득했는지 여부
    // - true: 조건을 만족하여 이미 획득한 배지
    // - false: 아직 획득하지 못한 배지
    val isEarned: Boolean,

    // 해당 배지가 현재 선택된 배지인지 여부
    // - 프로필 화면에서 대표 배지로 표시할 때 사용
    // - 일반적으로 획득한 배지 중 하나만 true가 된다.
    val isSelected: Boolean
)
