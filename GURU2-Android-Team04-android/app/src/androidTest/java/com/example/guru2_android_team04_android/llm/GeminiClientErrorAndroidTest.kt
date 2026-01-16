package com.example.guru2_android_team04_android.llm

import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// GeminiClientErrorAndroidTest : GeminiClient가 서버 오류(HTTP 500) 상황에서 조용히 넘어가지 않고 예외를 던져 호출자가 즉시 실패를 감지할 수 있는지 검증하는 테스트
// 용도:
// - GeminiClient는 HTTP status가 성공이 아니면 즉시 예외를 발생시키고, 상위(AppService.runAnalysisSafe)가 이를 잡아 AppError.ApiError 등으로 변환하는 구조를 테스트로 보장
@RunWith(AndroidJUnit4::class)
class GeminiClientErrorAndroidTest {

    // MockWebServer: 실제 Gemini 서버 대신 사용하는 로컬 테스트 서버
    // - 원하는 HTTP 코드/바디를 마음대로 반환하게 해서 에러 케이스를 재현한다.
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        // 각 테스트 시작 전 서버를 띄워 테스트가 외부 네트워크에 의존하지 않도록 한다.
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        // 서버를 종료하지 않으면 포트 점유로 인해 테스트에서 실패할 수 있다.
        server.shutdown()
    }

    @Test
    fun analyzeDiary_when_http_500_should_fail_fast() {
        // MockWebServer가 다음 요청에 대해 HTTP 500을 반환하도록 설정한다.
        // - 본문은 의미가 없지만, 실제 서버 오류 메시지처럼 JSON 형태를 흉내낸다.
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"error":"server error"}""")
                .addHeader("Content-Type", "application/json")
        )

        // GeminiClient의 endpointUrl을 MockWebServer 주소로 지정한다.
        // - 실제 Gemini로 요청이 나가지 않고, 위에서 enqueue한 500 응답을 받게 된다.
        val endpoint = server.url("/v1beta/models/gemini-1.5-flash:generateContent").toString()
        val client = GeminiClient(apiKey = "FAKE", endpointUrl = endpoint)

        try {
            // analyzeDiary는 내부에서 resp.isSuccessful이 false이면 예외를 던지도록 구현되어 있다.
            client.analyzeDiary(
                moodLabel = "JOY",
                tags = listOf("평온"),
                diaryText = "오늘은 괜찮았어"
            )

            // 여기까지 왔다는 것 : "예외가 안 났다"는 의미 -> 테스트 실패
            fail("Expected analyzeDiary to throw on HTTP 500")
        } catch (e: Exception) {
            assertTrue(true)
        }
    }
}
