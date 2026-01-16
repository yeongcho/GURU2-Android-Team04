package com.example.guru2_android_team04_android

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.guru2_android_team04_android.core.AppResult
import com.example.guru2_android_team04_android.data.model.DiaryEntry
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.llm.GeminiClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// AppServiceArchiveToggleAndroidTest : AppService의 즐겨찾기(하트)와 보관함 조회 연동을 검증하는 통합 테스트
// 용도:
// - 사용자가 일기를 저장한 뒤 하트를 켰을 때 보관함 목록에 들어가는지 확인한다.
// - 하트를 다시 껐을 때 보관함에서 빠지는지 확인한다.
@RunWith(AndroidJUnit4::class)
class AppServiceArchiveToggleAndroidTest {

    // MockWebServer : OkHttp가 호출할 가짜 서버
    // - GeminiClient가 endpointUrl로 호출하면 여기서 준비한 응답을 내려준다.
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        // Android 계측 테스트 환경에서 사용할 Context 확보
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // 테스트 독립성 보장
        TestDbUtil.wipeDb(context)

        // 가짜 HTTP 서버 실행
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        // 테스트 종료 후 가짜 서버 종료(포트 점유 방지)
        server.shutdown()
    }

    @Test
    fun favorite_toggle_on_then_off_reflects_in_archive() {
        // 테스트에서 쓸 Context 확보
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Gemini가 JSON만 반환한다고 가정한 바디(분석 결과 스키마)
        // - AppService.saveEntryAndPrepareMindCardSafe() 내부에서 runAnalysisSafe -> GeminiClient 호출을 하므로 이 테스트는 그 경로가 정상 동작하도록 유효한 JSON을 준비
        val jsonOnly = """
            {
              "summary":"요약",
              "trigger_pattern":"트리거",
              "hashtags":["#tag1"],
              "actions":["a1","a2","a3"],
              "mission_summary":"ms",
              "full_text":"ft"
            }
        """.trimIndent()

        // Gemini API 응답 형태를 흉내낸 JSON
        // - GeminiClient.extractTextFromGeminiResponse()가 candidates[0].content.parts[0].text 를 읽도록 구현되어 있으므로 그 구조 그대로 만들어준다.
        val geminiLikeResponse = """
            {
              "candidates":[
                {
                  "content":{
                    "parts":[{"text":${jsonOnly.quote()}}]
                  }
                }
              ]
            }
        """.trimIndent()

        // 가짜 서버에 다음 요청 1개에 대한 응답을 큐에 등록
        // - analyzeDiary가 호출되면 여기서 200 + JSON 바디를 내려준다.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(geminiLikeResponse)
                .addHeader("Content-Type", "application/json")
        )

        // MockWebServer가 제공하는 로컬 URL을 GeminiClient의 endpoint로 사용
        val endpoint = server.url("/v1beta/models/gemini-1.5-flash:generateContent").toString()
        val geminiClient = GeminiClient(apiKey = "FAKE", endpointUrl = endpoint)

        // AppService는 내부에서 DB(AppDbHelper)와 세션(SessionManager)을 사용한다.
        // - 여기서는 실제 앱과 같은 형태로 AppService를 생성해 통합 흐름을 검증한다.
        val appService = AppService(context, geminiClient)

        // 익명 세션 시작: ownerId는 "ANON_xxx" 형태로 만들어지고 SessionManager에 저장된다.
        val ownerId = appService.startAnonymousSession()

        // 일기 저장 + 마음카드(프리뷰) 생성
        // - 내부에서 DB upsert + 배지 체크 + (가능하면) Gemini 분석까지 수행한다.
        val r = appService.saveEntryAndPrepareMindCardSafe(
            DiaryEntry(
                ownerId = ownerId,
                dateYmd = "2026-01-13",
                title = "t",
                content = "c",
                mood = Mood.TIRED,
                tags = listOf("피곤")
            )
        )

        // 저장/카드 생성 결과는 Success여야 한다.
        assertTrue(r is AppResult.Success)

        // Success인 경우 MindCardPreview(또는 Payload 역할의 데이터)에서 entryId를 꺼내 토글에 사용한다.
        val payload = (r as AppResult.Success).data

        // ON: 즐겨찾기(하트) 켜기
        // - FavoriteDao.set()이 entries.is_favorite을 1로 업데이트한다.
        assertTrue(appService.setEntryFavorite(ownerId, payload.entryId, true))

        // 보관함 조회: 즐겨찾기된 것만 나와야 하므로 size=1 기대
        var archive = appService.getMindCardArchive(ownerId)
        assertEquals(1, archive.size)
        assertEquals(payload.entryId, archive[0].entryId)

        // OFF: 즐겨찾기(하트) 끄기
        // - entries.is_favorite을 0으로 업데이트한다.
        assertTrue(appService.setEntryFavorite(ownerId, payload.entryId, false))

        // 보관함 재조회: 이제 즐겨찾기 해제됐으니 size=0 기대
        archive = appService.getMindCardArchive(ownerId)
        assertEquals(0, archive.size)
    }

    // JSON 문자열을 "JSON 안에 넣을 수 있는 문자열 리터럴"로 감싸기 위한 헬퍼
    // - Gemini 응답 JSON에서 parts[0].text는 문자열이므로, 실제 JSON 문자열을 다시 문자열로 감싸야 한다.
    private fun String.quote(): String =
        "\"" + this.replace("\"", "\\\"").replace("\n", "\\n") + "\""
}
