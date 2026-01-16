package com.example.guru2_android_team04_android.llm

import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// GeminiClientAndroidTest : GeminiClient가 Gemini API 응답 형태에서 JSON 텍스트를 추출하고, 그 JSON을 GeminiResult로 정상 파싱하는지 검증하는 테스트
// 용도:
// - MockWebServer를 사용하면 "가짜 서버"가 요청을 받고 고정된 응답을 돌려주므로, 네트워크/요금/키 없이도 파싱 로직만 검증
@RunWith(AndroidJUnit4::class)
class GeminiClientAndroidTest {

    // MockWebServer: 테스트용 로컬 HTTP 서버
    // - GeminiClient의 endpointUrl을 여기로 바꿔치기하여 실제 호출 대신 테스트 응답을 받는다.
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        // 각 테스트 실행 전, 서버를 새로 띄워서 포트 충돌/상태 누수를 막는다.
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        // 서버가 살아 있으면 다음 테스트나 다른 프로세스에서 포트 점유 문제가 생길 수 있으니 항상 종료한다.
        server.shutdown()
    }

    @Test
    fun analyzeDiary_parses_json_from_gemini_shape() {
        // GeminiClient는 "text에 JSON만 출력"하도록 프롬프트를 구성한다.
        // 이 테스트는 그 전제를 만족하는 JSON 페이로드를 직접 만든다.
        val jsonOnly = """
            {
              "summary":"요약입니다",
              "trigger_pattern":"트리거입니다",
              "hashtags":["#평온"],
              "actions":["물 한 잔 마시기","5분 산책하기","숨 3번 길게 내쉬기"],
              "mission_summary":"작게라도 몸과 마음을 돌보는 하루로 만들어봐요.",
              "full_text":"따뜻한 코칭입니다"
            }
        """.trimIndent()

        // 실제 Gemini 응답 형태:
        // {
        //   "candidates":[{"content":{"parts":[{"text":"..."}]}}]
        // }
        // 여기서 text는 "문자열"이어야 하므로, jsonOnly를 JSON 문자열로 이스케이프해서 넣는다.
        val geminiLikeResponse = """
            {
              "candidates":[
                {
                  "content":{
                    "parts":[{"text":${jsonOnly.asJsonString()}}]
                  }
                }
              ]
            }
        """.trimIndent()

        // 서버가 요청 1건에 대해 돌려줄 응답을 큐에 넣는다.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(geminiLikeResponse)
        )

        // GeminiClient가 실제 Gemini 대신 MockWebServer로 호출하도록 endpointUrl을 구성한다.
        val endpoint = server.url("/v1beta/models/gemini-1.5-flash:generateContent").toString()

        // apiKey는 Mock 서버에서는 검사하지 않으므로 더미 값을 사용한다.
        val client = GeminiClient(apiKey = "FAKE", endpointUrl = endpoint)

        // analyzeDiary 호출 시:
        // 1) HTTP POST 전송
        // 2) 응답 JSON에서 text 추출
        // 3) text(JSON 문자열)를 파싱해서 GeminiResult 생성
        val result = client.analyzeDiary(
            moodLabel = "JOY",
            tags = listOf("평온"),
            diaryText = "오늘은 괜찮았어"
        )

        // 최종 결과가 jsonOnly에 정의한 값 그대로 들어왔는지 확인한다.
        assertEquals("요약입니다", result.summary)
        assertEquals("트리거입니다", result.triggerPattern)
        assertEquals(listOf("물 한 잔 마시기","5분 산책하기","숨 3번 길게 내쉬기"), result.actions)
        assertEquals(listOf("#평온"), result.hashtags)
        assertEquals("작게라도 몸과 마음을 돌보는 하루로 만들어봐요.", result.missionSummary)
        assertEquals("따뜻한 코칭입니다", result.fullText)
    }

    // 문자열을 "JSON 문자열 리터럴"로 만들어준다.
    private fun String.asJsonString(): String {
        val escaped = this
            .replace("\\", "\\\\")   // 백슬래시 이스케이프
            .replace("\"", "\\\"")   // 큰따옴표 이스케이프
            .replace("\n", "\\n")    // 개행 이스케이프
        return "\"$escaped\""
    }
}
