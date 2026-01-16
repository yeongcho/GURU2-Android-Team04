package com.example.guru2_android_team04_android

import android.app.Application
import com.example.guru2_android_team04_android.llm.GeminiClient

// MyApp : 앱 전체에서 1번만 생성되는 Application 클래스
// 용도:
// - 앱 시작 시점에 "전역에서 공유할 객체"를 초기화한다.
// - 여기서는 AppService를 싱글톤처럼 만들어, Activity/Fragment 어디서든 동일한 서비스 인스턴스를 사용하게 한다.
// - 서비스(AppService)는 내부에서 DB(AppDbHelper), 세션(SessionManager), 네트워크(GeminiClient) 등을 사용하므로 앱 시작 시 한 번만 생성해두면 객체 생성/의존성 연결이 단순해진다.
//
// 동작 시점:
// - Android 시스템이 앱 프로세스를 만들고, 가장 먼저 Application.onCreate()를 호출한다.
class MyApp : Application() {

    // AppService : 앱 기능(회원/일기/AI/보관함/배지 등)을 묶어 제공하는 서비스 계층
    // - lateinit: onCreate()에서 초기화하기 때문에 선언 시점엔 값을 넣지 않는다.
    // - private set: 외부에서 appService를 임의로 바꾸지 못하게 하여, 전역 상태의 일관성을 유지한다.
    lateinit var appService: AppService
        private set

    override fun onCreate() {
        super.onCreate()
        // GeminiClient : 일기 내용을 AI로 분석하기 위한 네트워크 클라이언트
        // - apiKey는 BuildConfig에 넣어(gradle/build 설정) 코드에 하드코딩하지 않는다.
        // - endpointUrl은 Gemini API의 generateContent 엔드포인트를 사용한다.
        // 예외처리) apiKey가 비어있거나 잘못되면 실제 호출 시점에 HTTP 실패(401/403 등)가 날 수 있다.
        // 이 앱에서는 호출부(AppService.runAnalysisSafe)가 실패를 AppError로 변환해 UI가 처리한다.
        val geminiClient = GeminiClient(
            apiKey = BuildConfig.GEMINI_API_KEY,
            endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
        )

        // AppService 생성
        appService = AppService(applicationContext, geminiClient)
    }
}
