package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.*
import com.example.guru2_android_team04_android.core.AppResult
import com.example.guru2_android_team04_android.data.db.DiaryEntryReader
import com.example.guru2_android_team04_android.data.model.Mood
import com.example.guru2_android_team04_android.util.MindCardTextUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// ArchiveDiaryDetailUiBinder : 일기 상세 화면(activity_archive_diary_detail.xml) <-> 데이터(AppService/DB) 연결 전담 클래스
// 용도:
// - entryId로 일기 내용을 조회하고, 화면에 표시한다.
// - 마음 카드 프리뷰/상세를 조회해(서비스/API) 화면에 표시한다.
// - 즐겨찾기 토글, 이미지 카드 저장 버튼 이벤트를 연결한다.
// - 같은 달의 일기 목록을 기반으로 "이전/다음 일기" 이동을 구현한다.
// 동작 흐름:
// 1) entryId 유효성 검증
// 2) IO 스레드에서 일기/마음카드 데이터 조회
// 3) Main 스레드에서 UI 반영 + 클릭 이벤트 연결
class ArchiveDiaryDetailUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {
    // reader : entryId로 일기 데이터를 읽는 로컬 DB Reader
    // - 상세 화면에서 제목/본문/감정/즐겨찾기 상태를 빠르게 불러오는 용도
    private val reader = DiaryEntryReader(activity)

    // bind : entryId에 해당하는 일기를 화면에 바인딩한다.
    fun bind(entryId: Long) {

        // 예외처리) entryId가 없거나 0 이하이면 조회 대상이 없으므로 토스트 후 화면 종료
        if (entryId <= 0L) {
            Toast.makeText(activity, "entryId가 올바르지 않아요.", Toast.LENGTH_SHORT).show()
            activity.finish()
            return
        }

        // tvDate : 카드 내부 날짜(TextView) "8일 목요일" 형태로 표시
        val tvDate = activity.findViewById<TextView>(R.id.tvDiaryDayText)

        // ivMood : 감정 이모지(ImageView)
        val ivMood = activity.findViewById<ImageView>(R.id.ivDetailEmotion)

        // tvMood : 감정 태그(TextView) "태그: 평온" 형태로 표시
        val tvMood = activity.findViewById<TextView>(R.id.tvDetailEmotionTag)

        // tvTitle/tvContent : 일기 제목/본문 표시
        val tvTitle = activity.findViewById<TextView>(R.id.tvDiaryTitle)
        val tvContent = activity.findViewById<TextView>(R.id.tvDiaryContent)

        // ivFav : 즐겨찾기 하트 아이콘(토글 UI)
        val ivFav = activity.findViewById<ImageView>(R.id.ivDetailFavorites)

        // ivMonthIcon : 월 스티커(카드 위에 겹치는 장식 이미지)
        // - entry.dateYmd에서 월을 추출해 month_1 ~ month_12로 매핑한다.
        val ivMonthIcon = activity.findViewById<ImageView>(R.id.ivMonthIcon)

        // tvComfort/tvConsole/tvMission : 마음 카드(위로 2줄 + 미션 1줄)
        val tvComfort = activity.findViewById<TextView>(R.id.tvNicknameMsg)
        val tvConsole = activity.findViewById<TextView>(R.id.tvConsoleText)
        val tvMission = activity.findViewById<TextView>(R.id.tvMissionText)

        // tvAnalysis/tvTags : 마음지기의 분석 내용 + 해시태그 표시
        val tvAnalysis = activity.findViewById<TextView>(R.id.tv_analysis_content)
        val tvTags = activity.findViewById<TextView>(R.id.tv_analysis_tags)

        // tvA1~tvA3/tvFooter : 실천안 3개 + 마무리 문장
        val tvA1 = activity.findViewById<TextView>(R.id.tv_action_1)
        val tvA2 = activity.findViewById<TextView>(R.id.tv_action_2)
        val tvA3 = activity.findViewById<TextView>(R.id.tv_action_3)
        val tvFooter = activity.findViewById<TextView>(R.id.tv_action_footer)

        // btnSaveCard : 마음 카드 결과를 이미지로 렌더링해서 갤러리에 저장하는 버튼
        val btnSaveCard =
            activity.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save_card)

        // btnPrev/btnNext : 헤더의 화살표 버튼
        // - 이 화면에서는 "이전 달/다음 달"이 아니라 "이전 일기/다음 일기" 이동으로 사용한다.
        val btnPrev = activity.findViewById<ImageView>(R.id.btnPrevMonth)
        val btnNext = activity.findViewById<ImageView>(R.id.btnNextMonth)

        // tvHeader : 헤더 날짜(TextView) "2026년 1월 8일 목요일" 형태로 표시
        val tvHeader = activity.findViewById<TextView>(R.id.tvMonth)

        // 데이터 조회는 IO에서 수행(로컬 DB + 서비스 호출)
        activity.lifecycleScope.launch(Dispatchers.IO) {

            // entry : entryId로 일기 1건 조회
            val entry = reader.getByIdOrNull(entryId)

            // 예외처리) entryId는 유효했지만 DB에서 일기를 못 찾으면(삭제/오류) 안내 후 종료
            if (entry == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "일기를 찾을 수 없어요.", Toast.LENGTH_SHORT).show()
                    activity.finish()
                }
                return@launch
            }

            // nickname : 마음 카드 문구 생성에 사용할 사용자 닉네임
            val nickname = appService.getUserProfile().nickname

            // preview : 마음 카드 프리뷰(짧은 위로/미션)
            // - 실패 시 null로 처리하고, UI에서는 기본 문구로 fallback 한다.
            val preview = when (val r = appService.getMindCardPreviewByEntryIdSafe(entryId)) {
                is AppResult.Success -> r.data
                is AppResult.Failure -> null
            }

            // detail : 마음 카드 상세(분석/해시태그/실천안)
            // - 실패 시 null로 처리하고, UI에서는 기본 텍스트/실천안으로 fallback 한다.
            val detail = when (val r = appService.getMindCardDetailByEntryIdSafe(entryId)) {
                is AppResult.Success -> r.data
                is AppResult.Failure -> null
            }

            // monthEntries : 같은 달의 일기 목록(이전/다음 이동을 위해 필요)
            // ym : entry.dateYmd("yyyy-MM-dd")에서 yyyy-MM만 추출
            val ym = entry.dateYmd.take(7)
            val ownerId = entry.ownerId
            val monthEntries = appService.getEntriesByMonth(ownerId, ym).sortedBy { it.dateYmd }

            // idx : 현재 entry가 같은 달 목록에서 몇 번째인지 찾는다.
            // - idx를 기준으로 idx-1(prev), idx+1(next)를 계산한다.
            val idx = monthEntries.indexOfFirst { it.entryId == entryId }

            // UI 반영은 Main에서 수행
            withContext(Dispatchers.Main) {

                // 헤더 날짜는 전체 날짜 + 요일 표시(가독성 좋은 포맷)
                tvHeader.text = prettyDateHeader(entry.dateYmd)

                // 카드 내부 날짜는 "일 + 요일"로 표시(디자인과 일치)
                tvDate.text = prettyDayOnly(entry.dateYmd)

                // 제목/본문 표시
                tvTitle.text = entry.title
                tvContent.text = entry.content

                // 감정 아이콘/태그 표시
                ivMood.setImageResource(iconResOf(entry.mood))
                tvMood.text = "태그: ${moodKo(entry.mood)}"

                // 월 스티커 표시
                // 예외처리) 월 파싱 실패 시 1월로 처리한다.
                val month = entry.dateYmd.drop(5).take(2).toIntOrNull() ?: 1
                ivMonthIcon.setImageResource(stickerResOf(month))

                // 마음 카드 위로 문구는 2줄로 나눠 보여준다.
                val (line1, line2) =
                    MindCardTextUtil.makeComfortLines(nickname, preview?.comfortPreview)
                tvComfort.text = line1
                tvConsole.text = line2

                // 오늘의 미션은 프리뷰가 없으면 기본 미션으로 표시한다.
                tvMission.text = "오늘의 미션: ${preview?.mission ?: "천천히 숨 고르기"}"

                // 분석/해시태그/실천안 표시
                // 예외처리) 상세(detail)를 못 불러오면 기본 문구/기본 실천안을 넣어 화면이 비지 않도록 한다.
                if (detail == null) {
                    tvAnalysis.text = "분석을 불러오지 못했어요."
                    tvTags.text = ""
                    tvA1.text = "1. 천천히 숨 고르기"
                    tvA2.text = "2. 물 한 잔 마시기"
                    tvA3.text = "3. 가볍게 스트레칭"
                    tvFooter.text = "오늘은 여기까지도 충분해요."
                } else {
                    tvAnalysis.text = detail.fullText

                    // hashtags는 #이 없을 수도 있어 UI 표기 시 #을 보장한다.
                    tvTags.text = detail.hashtags.joinToString(" ") {
                        if (it.startsWith("#")) it else "#$it"
                    }

                    // missions는 최대 3개를 표시한다.
                    // 예외처리) missions가 3개 미만이어도 getOrNull로 안전하게 처리한다.
                    tvA1.text = "1. ${detail.missions.getOrNull(0).orEmpty()}"
                    tvA2.text = "2. ${detail.missions.getOrNull(1).orEmpty()}"
                    tvA3.text = "3. ${detail.missions.getOrNull(2).orEmpty()}"
                    tvFooter.text = detail.missionSummary
                }

                // 즐겨찾기 토글
                // fav : 현재 즐겨찾기 상태를 UI와 동기화하기 위해 로컬 변수로 관리한다.
                var fav = entry.isFavorite
                ivFav.setImageResource(if (fav) R.drawable.ic_favorites_o else R.drawable.ic_favorites_x)

                // 하트 클릭 시 즐겨찾기 상태를 서버/DB에 반영하고 성공하면 UI도 변경한다.
                ivFav.setOnClickListener {
                    val next = !fav

                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        val ok = appService.setEntryFavorite(entry.ownerId, entry.entryId, next)

                        withContext(Dispatchers.Main) {
                            if (ok) {
                                fav = next
                                ivFav.setImageResource(
                                    if (fav) R.drawable.ic_favorites_o else R.drawable.ic_favorites_x
                                )
                            } else {
                                // 예외처리) 즐겨찾기 변경 실패 시 상태를 바꾸지 않고 안내만 한다.
                                Toast.makeText(activity, "즐겨찾기 변경에 실패했어요.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                // 이미지 카드로 소장하기
                // - 분석 카드 결과를 이미지로 만들어 갤러리에 저장한다.
                btnSaveCard.setOnClickListener {
                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        val r = appService.exportMindCardToGallerySafe(activity, entryId)

                        withContext(Dispatchers.Main) {
                            when (r) {
                                is AppResult.Success ->
                                    Toast.makeText(activity, "갤러리에 저장했어요!", Toast.LENGTH_SHORT).show()
                                is AppResult.Failure ->
                                    Toast.makeText(activity, r.error.userMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                // 이전 일기 이동
                // 예외처리) 현재가 첫 번째면 prev가 없으므로 아무 동작도 하지 않는다(getOrNull).
                btnPrev.setOnClickListener {
                    val prev = monthEntries.getOrNull(idx - 1) ?: return@setOnClickListener
                    activity.startActivity(Intent(activity, ArchiveDiaryDetailActivity::class.java).apply {
                        putExtra("entryId", prev.entryId)
                    })
                    activity.finish()
                }

                // 다음 일기 이동
                // 예외처리) 현재가 마지막이면 next가 없으므로 아무 동작도 하지 않는다(getOrNull).
                btnNext.setOnClickListener {
                    val next = monthEntries.getOrNull(idx + 1) ?: return@setOnClickListener
                    activity.startActivity(Intent(activity, ArchiveDiaryDetailActivity::class.java).apply {
                        putExtra("entryId", next.entryId)
                    })
                    activity.finish()
                }
            }
        }
    }

    // prettyDateHeader : "yyyy-MM-dd"를 "yyyy년 M월 d일 요일" 형태로 변환한다(헤더용).
    // 예외처리) 파싱 실패 시 원문을 그대로 반환해 앱 크래시를 방지한다.
    private fun prettyDateHeader(ymd: String): String {
        val y = ymd.take(4).toIntOrNull() ?: return ymd
        val m = ymd.drop(5).take(2).toIntOrNull() ?: return ymd
        val d = ymd.takeLast(2).toIntOrNull() ?: return ymd

        val c = Calendar.getInstance()
        c.set(y, m - 1, d)

        val wd = when (c.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "일요일"
            Calendar.MONDAY -> "월요일"
            Calendar.TUESDAY -> "화요일"
            Calendar.WEDNESDAY -> "수요일"
            Calendar.THURSDAY -> "목요일"
            Calendar.FRIDAY -> "금요일"
            Calendar.SATURDAY -> "토요일"
            else -> ""
        }
        return "${y}년 ${m}월 ${d}일 $wd"
    }

    // prettyDayOnly : "yyyy-MM-dd"를 "d일 요일" 형태로 변환한다(카드 내부 날짜용).
    // 예외처리) 파싱 실패 시 원문을 그대로 반환해 앱 크래시를 방지한다.
    private fun prettyDayOnly(ymd: String): String {
        val y = ymd.take(4).toIntOrNull() ?: return ymd
        val m = ymd.drop(5).take(2).toIntOrNull() ?: return ymd
        val d = ymd.takeLast(2).toIntOrNull() ?: return ymd

        val c = Calendar.getInstance()
        c.set(y, m - 1, d)

        val wd = when (c.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "일요일"
            Calendar.MONDAY -> "월요일"
            Calendar.TUESDAY -> "화요일"
            Calendar.WEDNESDAY -> "수요일"
            Calendar.THURSDAY -> "목요일"
            Calendar.FRIDAY -> "금요일"
            Calendar.SATURDAY -> "토요일"
            else -> ""
        }
        return "${d}일 $wd"
    }

    // moodKo : Mood enum을 UI에 표시할 한글 문자열로 변환한다.
    private fun moodKo(m: Mood): String = when (m) {
        Mood.JOY -> "기쁨"
        Mood.CONFIDENCE -> "자신감"
        Mood.CALM -> "평온"
        Mood.NORMAL -> "평범"
        Mood.DEPRESSED -> "우울"
        Mood.ANGRY -> "분노"
        Mood.TIRED -> "피곤함"
    }

    // iconResOf : Mood enum에 해당하는 감정 아이콘 리소스를 반환한다.
    private fun iconResOf(mood: Mood): Int = when (mood) {
        Mood.JOY -> R.drawable.emotion_joy
        Mood.CONFIDENCE -> R.drawable.emotion_confidence
        Mood.CALM -> R.drawable.emotion_calm
        Mood.NORMAL -> R.drawable.emotion_normal
        Mood.DEPRESSED -> R.drawable.emotion_sad
        Mood.ANGRY -> R.drawable.emotion_angry
        Mood.TIRED -> R.drawable.emotion_tired
    }

    // stickerResOf : 월(1~12)을 월 스티커 리소스(month_1~month_12)로 매핑한다.
    // 예외처리) 범위를 벗어나면 1월 스티커로 처리한다.
    private fun stickerResOf(month: Int): Int = when (month) {
        1 -> R.drawable.month_1
        2 -> R.drawable.month_2
        3 -> R.drawable.month_3
        4 -> R.drawable.month_4
        5 -> R.drawable.month_5
        6 -> R.drawable.month_6
        7 -> R.drawable.month_7
        8 -> R.drawable.month_8
        9 -> R.drawable.month_9
        10 -> R.drawable.month_10
        11 -> R.drawable.month_11
        12 -> R.drawable.month_12
        else -> R.drawable.month_1
    }
}