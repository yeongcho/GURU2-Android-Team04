package com.example.guru2_android_team04_android.data.db

// AppDb : SQLite DB의 공통 상수 모음 객체
// - DB 파일명, 버전, 테이블명을 한 곳에서 관리해서 문자열 오타로 인한 버그를 줄임
// 사용 예:
// - AppDb.DB_NAME, AppDb.DB_VERSION
// - AppDb.T.ENTRIES 같은 테이블명 상수로 SQL을 작성
object AppDb {

    // 앱 내부 저장소에 생성되는 SQLite 파일 이름
    const val DB_NAME = "app.db"

    // DB 스키마 버전
    // - 테이블 구조(컬럼 추가/변경 등)가 바뀌면 반드시 버전을 올려야 한다.
    // - 버전이 바뀌면 기존 사용자 DB에 대해 onUpgrade가 수행된다.
    const val DB_VERSION = 5

    // T(Table)
    // 테이블명 상수들을 모아두는 내부 객체.
    // - SQL에서 "문자열 직접 입력"을 최소화하기 위해 사용한다.
    // - 테이블명 변경이 필요할 때 여기만 수정하면 된다.
    object T {

        // 회원 정보 테이블
        // - 이메일 / 비밀번호 해시 / 닉네임 등 저장
        const val USERS = "users"

        // 일기 테이블
        // - owner_id(USER_xxx / ANON_xxx) 기준으로 사용자별 데이터 구분
        // - date_ymd(YYYY-MM-DD)로 날짜 단위 저장
        const val ENTRIES = "diary_entries"

        // AI 분석 결과 테이블
        // - 일기(entry_id) 1개당 분석 1개를 저장하는 캐시 용도
        const val ANALYSIS = "ai_analysis"

        // 월간 요약 테이블
        // - owner_id + year_month(YYYY-MM) 단위로 지난달 요약 저장
        const val MONTHLY = "monthly_summaries"

        // 배지 마스터 테이블
        // - 배지 이름 / 설명 / 획득 조건(rule) 저장
        const val BADGES = "badges"

        // 사용자-배지 매핑 테이블
        // - 어떤 owner_id가 어떤 배지를 획득했는지
        // - 현재 선택 배지는 무엇인지 저장
        const val USER_BADGES = "user_badges"

        // 설정 테이블
        // - 프로필 이미지 URI 등 사용자별 설정 저장
        const val SETTINGS = "settings"
    }
}
