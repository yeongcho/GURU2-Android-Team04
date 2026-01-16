package com.example.guru2_android_team04_android.export

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import java.io.OutputStream

// CardExporter : "마음 카드"를 비트맵 이미지로 렌더링하고, 갤러리(MediaStore)에 저장하는 객체
// 용도:
// - 사용자가 만든 마음 카드를 이미지(JPEG)로 내보내기해서 기기 갤러리에 저장할 때 사용한다.
// 구성:
// - renderMindCard(): 카드 이미지를 Bitmap으로 그린다.
// - saveToGallery(): 생성된 Bitmap을 MediaStore에 JPEG로 저장하고 Uri를 반환한다.
object CardExporter {

    // 카드 이미지에 너무 긴 본문이 들어가면 레이아웃이 깨지므로 글자 수를 상한선으로 제한한다.
    private const val MAX_CONTENT_CHARS = 500

    // 본문 영역에 출력할 최대 줄 수(카드 높이 초과 방지)
    private const val MAX_CONTENT_LINES = 14

    // DiaryEntry(일기) + AiAnalysis(분석, 없을 수도 있음)를 받아 "마음 카드" 이미지를 만든다.
    // 반환:
    // - 화면/저장에 사용할 Bitmap(1080x1350)
    // 설계:
    // - 본문은 길이/줄 수 제한을 둬서 카드 레이아웃이 안정적으로 유지되게 한다.
    fun renderMindCard(
        entry: com.example.guru2_android_team04_android.data.model.DiaryEntry,
        analysis: com.example.guru2_android_team04_android.data.model.AiAnalysis?
    ): Bitmap {

        // 출력 이미지 크기(고정)
        // - 공유/저장 시 품질을 일정하게 유지하기 위해 고정 해상도로 렌더링한다.
        val w = 1080
        val h = 1350

        // 결과 이미지(투명 포함 ARGB) 생성 후 Canvas로 그림을 그린다.
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // 배경색(앱 톤)
        canvas.drawColor(Color.parseColor("#0F1B2D"))

        // 카드 배경(라운드 박스)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E2B42")
        }
        val rect = RectF(80f, 120f, (w - 80).toFloat(), (h - 120).toFloat())
        canvas.drawRoundRect(rect, 48f, 48f, cardPaint)

        // 상단 타이틀(날짜)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(entry.dateYmd, 120f, 210f, titlePaint)

        // 태그/기분(서브 텍스트)
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C8D3E6")
            textSize = 34f
        }
        canvas.drawText("태그: ${entry.tags.joinToString(", ")}", 120f, 270f, subPaint)
        canvas.drawText("기분: ${entry.mood.name}", 120f, 320f, subPaint)

        // 본문 텍스트 페인트(제목/내용 공용)
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
        }

        // 제목은 최대 2줄까지만 허용(레이아웃 유지)
        drawMultilineBounded(
            canvas,
            "제목: ${entry.title}",
            120f,
            400f,
            w - 240f,
            bodyPaint,
            maxLines = 2
        )

        // 내용이 너무 길면 카드에서 다 보여줄 수 없으므로 글자 수로 1차 제한
        // 예외처리) MAX_CONTENT_CHARS를 넘으면 뒤를 잘라 "…"로 생략 표시
        val safeContent = if (entry.content.length > MAX_CONTENT_CHARS) {
            entry.content.take(MAX_CONTENT_CHARS) + "…"
        } else entry.content

        // 본문은 최대 MAX_CONTENT_LINES 줄까지만 출력
        val contentTop = 520f
        drawMultilineBounded(
            canvas,
            safeContent,
            120f,
            contentTop,
            w - 240f,
            bodyPaint,
            maxLines = MAX_CONTENT_LINES
        )

        // 하단 미션 문구
        // - 분석이 있으면 actions[0]을 사용
        // - 분석이 없으면 기본 문구로 대체
        val mission = analysis?.actions?.firstOrNull()?.let { "오늘의 미션: $it" }
            ?: "오늘의 미션: 천천히 숨 고르기"

        val missionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A6F0C6")
            textSize = 38f
            typeface = Typeface.DEFAULT_BOLD
        }
        drawMultilineBounded(
            canvas,
            mission,
            120f,
            1120f,
            w - 240f,
            missionPaint,
            maxLines = 2
        )

        // 하단 서명(앱 이름/브랜딩)
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#95A3BD")
            textSize = 26f
        }
        canvas.drawText("EmoDiary", 120f, 1240f, footerPaint)

        return bmp
    }

    // 텍스트를 "최대 폭(maxWidth)" 안에서 자동 줄바꿈하며 그린다.
    // 기능:
    // - 공백 기준으로 토큰(단어)을 나눠서 한 줄에 들어갈 수 있으면 출력
    // - 폭을 초과하면 다음 줄로 이동
    // - maxLines를 초과하면 마지막에 "…"를 찍고 종료
    // 설계:
    // - \n을 강제 줄바꿈 토큰으로 처리하기 위해 replace/split로 가공한다.
    private fun drawMultilineBounded(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint,
        maxLines: Int
    ) {
        // "\n"을 독립 토큰으로 만들어 for-loop에서 줄바꿈으로 처리한다.
        val words = text.replace("\n", " \n ").split(" ")

        var curX = x
        var curY = y

        // 줄 간격은 글자 크기 기반으로 적당히 띄운다.
        val lineHeight = paint.textSize * 1.35f

        var lines = 1
        var ended = false

        // 다음 줄로 이동하는 로컬 함수
        fun newLine() {
            lines += 1
            curX = x
            curY += lineHeight
            if (lines > maxLines) ended = true
        }

        for (w in words) {
            if (ended) break

            // "\n" 토큰은 강제 줄바꿈 처리
            if (w == "\n") {
                newLine()
                continue
            }

            // 토큰 뒤에 공백 1칸을 붙여서 다음 단어와 간격을 만든다.
            val token = "$w "
            val measure = paint.measureText(token)

            // 현재 줄에 더 이상 안 들어가면 줄바꿈
            if (curX + measure > x + maxWidth) {
                newLine()
                if (ended) break
            }

            // 마지막 줄에서 남은 공간이 "…"도 못 찍을 정도면 생략 표시 후 종료
            if (lines == maxLines && curX + measure > x + maxWidth - paint.measureText("…")) {
                canvas.drawText("…", curX, curY, paint)
                ended = true
                break
            }

            // 실제 출력은 단어(w)만 찍고, 커서 이동은 token 폭만큼 한다.
            canvas.drawText(w, curX, curY, paint)
            curX += measure
        }
    }

    // Bitmap을 MediaStore(갤러리)에 JPEG로 저장하고, 저장된 이미지 Uri를 반환한다.
    // 동작:
    // 1) MediaStore에 메타데이터(ContentValues)로 insert하여 Uri를 발급받는다.
    // 2) Uri에 대한 OutputStream을 열어 bitmap.compress로 JPEG 데이터 기록
    // 3) flush/close 후 Uri 반환
    //
    // 예외처리) insert가 실패하면 uri가 null이므로 RuntimeException 발생
    // 예외처리) OutputStream을 못 열면 RuntimeException 발생
    fun saveToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, displayName)
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: throw RuntimeException("MediaStore insert failed")

        var os: OutputStream? = null
        try {
            os = context.contentResolver.openOutputStream(uri)

            // 예외처리) 저장 스트림을 열지 못하면 저장이 불가능하므로 예외 처리
            if (os == null) throw RuntimeException("openOutputStream failed")

            // JPEG 품질 92: 용량과 품질의 균형값
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, os)
            os.flush()
            return uri
        } finally {

            // 예외처리) close 중 예외가 나더라도 앱이 죽지 않도록 무시한다.
            try { os?.close() } catch (_: Exception) {}
        }
    }
}
