package com.example.guru2_android_team04_android.data.model

import com.example.guru2_android_team04_android.core.AppError

// MindCardPreviewResult : 마음 카드 화면에 필요한 결과 데이터를 담는 모델
// 용도:
// - UI에 표시할 데이터(preview)는 항상 제공하되,
// - AI 분석 결과가 없거나 분석 조회/생성 과정에서 문제가 있었으면 그 이유를 analysisError로 함께 전달한다.
data class MindCardPreviewResult(

    // preview : 마음 카드 UI를 그리기 위한 핵심 데이터
    val preview: MindCardPreview,

    // analysisError : AI 분석 관련 오류(있으면 실패 원인), 없으면 null
    // - null: 분석이 정상적으로 존재하거나(또는 분석이 필요 없는 흐름이거나) 오류가 없는 상태
    // - non-null: 분석 조회/생성/파싱 실패 등으로 인해 사용자에게 안내가 필요한 상태
    // 예외처리) 오류는 String 대신 AppError로 표준화해서 UI에서 error.userMessage만 사용해도 일관된 메시지를 보여줄 수 있다.
    val analysisError: AppError? = null
)
