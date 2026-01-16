package com.example.guru2_android_team04_android.data.model

// MindCardPreview : "마음 카드" UI에 공통으로 사용하는 카드 모델 (UI 카드 미리보기용 DTO)
// 용도:
// - 홈 화면의 "오늘의 마음 카드"에 사용된다.
// - 마음 카드 보관함(즐겨찾기 목록)에서도 동일한 카드 UI를 그릴 때 사용된다.
// 설계:
// - DiaryEntry / AiAnalysis 같은 DB 엔티티를 그대로 쓰지 않고, UI에 필요한 최소 정보만 가진 DTO로 분리한다.
// - entryId를 기준으로 상세 분석 화면(MindCardDetail)로 자연스럽게 이어진다.
// - Preview 단계에서는 "요약 + 미션 1개"까지만 보여주고, 상세 정보는 MindCardDetail에서 책임진다.
data class MindCardPreview(
    // 일기 고유 ID
    val entryId: Long,

    // 일기 작성 날짜 ("YYYY-MM-DD")
    val dateYmd: String,

    // 일기 제목
    val title: String,

    // 감정 값 (enum Mood)
    val mood: Mood,

    // 태그 목록
    // - 일기 작성 시 사용자가 선택한 태그들
    val tags: List<String>,

    // 카드 본문에 표시할 짧은 문장
    // - AI 분석 요약 또는 위로 메시지
    val comfortPreview: String,

    // 카드에 표시할 미션 1개
    // - AI 행동 제안 중 첫 번째 항목
    // - 홈/보관함 카드에서는 1개만 보여준다.
    val mission: String
)

// MindCardDetail : 마음 카드 "상세 분석 화면" 전용 모델 (UI 상세 화면용 DTO)
// 용도:
// - AI 분석 결과를 사용자에게 충분히 설명하기 위한 데이터 집합이다.
// 설계:
// - Preview 모델과 역할을 명확히 분리한다.
// - UI 구조에 맞춰 summary / trigger / missions / fullText를 분리한다.
// - missions는 UI 영역을 위해 항상 3개를 유지하도록 설계한다.
data class MindCardDetail(
    // 일기 고유 ID
    // - Preview와 Detail을 연결하는 공통 키
    val entryId: Long,

    // AI 분석 요약 문장
    // - 전체 분석을 한두 문장으로 요약한 텍스트
    val summary: String,

    // 감정 트리거 패턴
    // - AI가 분석한 감정 유발 요인 설명
    val triggerPattern: String,

    // 해시태그 목록
    // - 분석 결과에서 추출된 감정/상황 키워드
    val hashtags: List<String>,

    // 행동 미션 목록
    // - 최대 3개를 보여주기 위한 리스트
    val missions: List<String>,

    // 미션 요약 문장
    // - 여러 미션을 한 줄로 정리한 설명
    val missionSummary: String,

    // 전체 분석 원문 텍스트
    val fullText: String
)
