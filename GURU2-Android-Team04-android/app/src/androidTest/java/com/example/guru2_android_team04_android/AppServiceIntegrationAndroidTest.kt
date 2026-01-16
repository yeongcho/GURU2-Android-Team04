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

// AppServiceIntegrationAndroidTest : 앱의 핵심 플로우(저장 -> AI분석 -> 카드 프리뷰 -> 상세 -> 즐겨찾기 -> 보관함)를 한 번에 검증하는 통합 테스트
// 용도:
// - AppService가 DB/세션/AI 클라이언트를 엮어서 유저 기능 흐름을 정상 수행하는지 확인한다.
// - 단위 테스트가 아니라, 실제 앱 사용 시나리오에 가까운 end-to-end(통합) 형태로 검증한다.
@RunWith(AndroidJUnit4::class)
class AppServiceIntegrationAndroidTest {

    // MockWebServer : OkHttp 기반 GeminiClient가 호출할 가짜 서버
    // - 실제 네트워크 요청 대신, 우리가 준비한 JSON 응답을 반환한다.
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // 테스트 독립성 보장
        TestDbUtil.wipeDb(context)

        // 가짜 서버 시작(포트 자동 할당)
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        // 테스트 종료 시 가짜 서버 종료(자원/포트 정리)
        server.shutdown()
    }

    @Test
    fun save_then_prepare_mindcard_then_detail_then_favorite_archive() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // 1) Gemini API 가상 응답 데이터 설정
        // - GeminiClient는 candidates[0].content.parts[0].text 에서 텍스트를 추출한다.
        // - AppService.runAnalysis()는 이 텍스트가 JSON만 존재한다고 가정하고 parseStrictJson을 수행한다.
        val jsonOnly = """
            {
              "summary":"오늘 감정 요약",
              "trigger_pattern":"트리거 패턴",
              "hashtags":["#피곤","#휴식"],
              "actions":["물 한 잔 마시기","5분 산책하기","숨 3번 길게 내쉬기"],
              "mission_summary":"작게라도 몸과 마음을 돌보는 하루로 만들어봐요.",
              "full_text":"오늘은 여기서 멈춰도 괜찮아요. 충분히 애썼어요. 무거운 마음은 여기에 두고 가요."
            }
        """.trimIndent()

        // Gemini 실응답 형태를 흉내 낸 JSON
        // - parts[0].text는 문자열이므로 jsonOnly를 문자열 리터럴로 감싸서 넣어야 한다(quote() 사용).
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

        // 가짜 서버에 요청 1개에 대한 응답을 등록
        // - saveEntryAndPrepareMindCardSafe() 내부에서 runAnalysisSafe()가 호출되며 결국 GeminiClient.analyzeDiary()가 이 응답을 받게 된다.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(geminiLikeResponse)
                .addHeader("Content-Type", "application/json")
        )

        // 2) 서비스 인스턴스 준비
        // - endpointUrl을 MockWebServer 주소로 설정해 외부 통신 없이 테스트한다.
        val endpoint = server.url("/v1beta/models/gemini-1.5-flash:generateContent").toString()
        val geminiClient = GeminiClient(apiKey = "FAKE", endpointUrl = endpoint)
        val appService = AppService(context, geminiClient)

        // 3) 비회원 세션 시작
        // - SessionManager가 기기 로컬에 anon ownerId를 저장/재사용하도록 설계되어 있다.
        val ownerId = appService.startAnonymousSession()
        assertTrue(ownerId.startsWith("ANON_"))

        // 4) 일기 저장 및 마음 카드 프리뷰(홈 카드) 준비
        val r = appService.saveEntryAndPrepareMindCardSafe(
            DiaryEntry(
                ownerId = ownerId,
                dateYmd = "2026-01-10",
                title = "오늘",
                content = "오늘은 조금 지쳤어",
                mood = Mood.TIRED,
                tags = listOf("피곤")
            )
        )

        // 예외처리) saveEntryAndPrepareMindCardSafe()가 실패하면 이후 흐름 자체가 성립하지 않으므로 바로 중단한다.
        assertTrue("결과가 Success여야 합니다", r is AppResult.Success)

        // saveEntryAndPrepareMindCardSafe()는 MindCardPreview를 돌려준다.
        // - comfortPreview: 위로/요약 프리뷰 문구
        // - mission: 오늘의 미션 1개
        val payload = (r as AppResult.Success).data
        assertTrue("위로 문구가 비어있지 않아야 합니다", payload.comfortPreview.isNotBlank())
        assertTrue("미션이 비어있지 않아야 합니다", payload.mission.isNotBlank())

        // 5) 상세 분석 결과 조회
        // - AppService.getMindCardDetailByEntryIdSafe()는 분석 결과(AiAnalysis)를 기반으로 MindCardDetail을 만든다.
        // - missions는 UI에서 3개 고정이므로 부족하면 기본값으로 채워준다.
        val detail = appService.getMindCardDetailByEntryIdSafe(payload.entryId)
        assertTrue("상세 조회가 성공해야 합니다", detail is AppResult.Success)

        val d = (detail as AppResult.Success).data
        assertEquals("미션은 항상 3개여야 합니다", 3, d.missions.size)
        assertTrue("미션 요약이 존재해야 합니다", d.missionSummary.isNotBlank())

        // 6) 즐겨찾기(하트) 설정 및 보관함 확인
        // - setEntryFavorite은 entries.is_favorite 값을 업데이트한다.
        // - getMindCardArchive는 is_favorite=1인 항목만 목록으로 반환한다.
        val okFav = appService.setEntryFavorite(ownerId, payload.entryId, true)
        assertTrue("즐겨찾기 설정이 성공해야 합니다", okFav)

        val archive = appService.getMindCardArchive(ownerId)
        assertEquals("보관함에 1개의 카드가 있어야 합니다", 1, archive.size)
        assertEquals(payload.entryId, archive[0].entryId)

        // - MindCardPreview.comfortPreview는 makeShortComfortMessage()로 만들어진다.
        // - makeShortComfortMessage()는 full_text에서 1~2문장 프리뷰를 우선 사용하고, 프리뷰가 비어있을 때만 summary를 사용한다.
        // - 따라서 comfortPreview가 "오늘 감정 요약"과 정확히 일치한다고 단정하면 테스트가 깨질 수 있다.
        assertEquals("보관함 카드의 요약 문구가 일치해야 합니다", "오늘 감정 요약", archive[0].comfortPreview)
    }

    // JSON 문자열을 JSON 내부의 문자열로 안전하게 넣기 위한 헬퍼
    // - Gemini 응답에서는 parts[0].text가 문자열이므로, JSON 본문(jsonOnly)을 다시 문자열로 감싸고 따옴표/줄바꿈을 이스케이프해야 한다.
    private fun String.quote(): String =
        "\"" + this.replace("\"", "\\\"").replace("\n", "\\n") + "\""
}
