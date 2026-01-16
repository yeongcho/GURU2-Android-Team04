package com.example.guru2_android_team04_android.util

import android.content.Context
import android.net.ConnectivityManager

// NetworkUtil : 네트워크 연결 상태를 간단히 확인하기 위한 유틸리티
// 용도:
// - AI 분석 요청, 서버 통신 등 네트워크가 필수인 기능을 실행하기 전에 현재 기기가 인터넷에 연결되어 있는지 판단하는 데 사용된다.
// - AppService.runAnalysisSafe() 등에서 네트워크 미연결 상태를 사전에 감지해 사용자에게 적절한 오류를 반환하기 위해 활용된다.
// 설계 포인트:
// - Android 시스템 서비스에 의존 -> 연결 여부만 판단하는 가벼운 헬퍼로, 네트워크 품질이나 속도까지는 판단하지 않는다.
object NetworkUtil {

    // 현재 네트워크 연결 가능 여부를 반환한다.
    // 파라미터:
    // - context: 시스템 서비스 접근을 위한 Context
    // 반환:
    // - true  : 네트워크가 연결되어 있다고 판단
    // - false : 네트워크가 없거나 연결되지 않음
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            // ConnectivityManager를 통해 현재 활성 네트워크 정보 조회
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val info = cm.activeNetworkInfo

            // activeNetworkInfo가 존재하고, 연결 상태(isConnected)면 true
            info != null && info.isConnected
        } catch (_: SecurityException) {
            // 예외처리) ACCESS_NETWORK_STATE 권한이 없거나 테스트/제한된 실행 환경에서 시스템 서비스 접근이 막힌 경우
            // - false를 반환하면 AI 분석 등 핵심 기능이 전부 차단될 수 있으므로 연결된 것으로 간주하여 앱 흐름이 완전히 멈추지 않도록 방어
            true
        }
    }
}