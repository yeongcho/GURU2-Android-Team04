package com.example.guru2_android_team04_android.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

// NetworkUtil : 앱에서 현재 네트워크 연결 가능 여부를 간단히 확인하는 유틸리티
// 용도:
// - AI 분석 요청(Gemini API)처럼 네트워크가 필요한 기능을 실행하기 전에, 인터넷 연결 상태를 빠르게 체크해서 사용자에게 적절한 안내를 하기 위해 사용한다.
// 체크 기준:
// - 단순히 Wi-Fi/모바일 데이터에 연결되었는지 뿐만 아니라, 실제로 인터넷 사용이 가능한 상태인지(VALIDATED)까지 확인한다.
object NetworkUtil {

    // isNetworkAvailable : 현재 인터넷 사용 가능 여부를 반환한다.
    // 파라미터:
    // - context: 시스템 서비스(ConnectivityManager)를 가져오기 위한 Context
    // 반환:
    // - true  : 인터넷 사용 가능(또는 권한 문제로 정확한 판별이 불가한 경우)
    // - false : 인터넷 사용 불가(네트워크 없음/검증 불가)
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            // ConnectivityManager : 현재 네트워크 상태(활성 네트워크, 기능)를 조회하는 시스템 서비스
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // activeNetwork : 현재 앱에서 사용할 "활성 네트워크" (없으면 인터넷 사용 불가)
            val network = cm.activeNetwork ?: return false

            // networkCapabilities : 해당 네트워크가 제공하는 기능들(인터넷 가능 여부 등)
            val caps = cm.getNetworkCapabilities(network) ?: return false

            // NET_CAPABILITY_INTERNET : 인터넷 기능을 제공하는 네트워크인지
            // NET_CAPABILITY_VALIDATED : 실제로 외부 인터넷 연결이 검증된 상태인지(포털/제한망이면 false 가능)
            // - 둘 다 true여야 "실제 인터넷 사용 가능"으로 판단한다.
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (_: SecurityException) {
            // 예외처리) 네트워크 상태 조회에 필요한 권한/보안 정책 문제로 SecurityException이 날 수 있다.
            // - 예: 특정 기기/정책에서 네트워크 정보 접근이 제한되는 경우
            // - 이 경우 앱이 죽지 않게 하고, 네트워크가 있다고 가정하여(=true) 다음 로직에서 실패를 처리하게 한다.
            true
        }
    }
}
