package com.example.guru2_android_team04_android.ui.bind

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.AppService
import com.example.guru2_android_team04_android.MyPageActivity
import com.example.guru2_android_team04_android.R
import com.example.guru2_android_team04_android.data.model.BadgeStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ProfileBadgeEditUiBinder : 배지 선택/적용 화면(activity_profile_badge_edit.xml) <-> AppService 연동 전담 클래스
// 용도:
// - 사용자가 획득한 배지 목록을 불러와 그리드 형태로 표시한다.
// - 배지를 클릭하면 선택 표시(체크 아이콘)와 프로필 카드 프레임 미리보기를 변경한다.
// - 저장 버튼 클릭 시 선택한 배지를 대표 배지로 저장한다.
// - 미획득 배지는 선택 불가 처리(회색 처리 + 토스트 안내)한다.
// 동작 흐름:
// 1) bind()에서 프로필 조회 후 로그인 사용자(USER_)인지 확인한다.
// 2) 배지 상태 목록(appService.getBadgeStatuses)을 가져온다.
// 3) 획득/미획득에 따라 UI(alpha/컬러필터)를 적용하고, 클릭 시 선택 상태를 갱신한다.
// 4) 저장 버튼 클릭 시 selectBadge를 호출하고 성공하면 MyPage로 이동한다.
class ProfileBadgeEditUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {
    // selectedBadgeId : 현재 화면에서 선택된 배지 ID(저장할 대상)
    // - 처음에는 서버에서 선택된 대표 배지 값으로 초기화될 수 있다.
    private var selectedBadgeId: Int? = null

    companion object {
        // 프로필 이미지 로컬 URI fallback을 위한 SharedPreferences 키
        private const val PREFS_NAME = "mypage_prefs"
        private const val KEY_PROFILE_URI = "profile_image_uri"
    }

    fun bind() {
        val ivProfile = activity.findViewById<ImageView>(R.id.iv_profile_image)
        val tvNickname = activity.findViewById<TextView>(R.id.tv_nickname)
        val tvEmail = activity.findViewById<TextView>(R.id.tv_email)
        val tvBadgeName = activity.findViewById<TextView>(R.id.tv_badge_name)
        val tvDday = activity.findViewById<TextView>(R.id.tv_d_day)
        val ivBadgeFrame = activity.findViewById<ImageView>(R.id.iv_badge_frame_mypage)

        val btnSave = activity.findViewById<View>(R.id.btn_save_badge)

        // slots : 화면에 고정 배치된 배지 6개(레이아웃/아이콘/체크뷰/배지ID)를 묶어 관리한다.
        // - badgeId는 서버 badgeId와 매핑되며, 여기서는 traveler 배지를 9994로 확장했다.
        val slots = listOf(
            BadgeSlot(activity.findViewById(R.id.layout_badge_1), activity.findViewById(R.id.iv_badge_1), activity.findViewById(R.id.iv_check_1), 1),
            BadgeSlot(activity.findViewById(R.id.layout_badge_2), activity.findViewById(R.id.iv_badge_2), activity.findViewById(R.id.iv_check_2), 4),
            BadgeSlot(activity.findViewById(R.id.layout_badge_3), activity.findViewById(R.id.iv_badge_3), activity.findViewById(R.id.iv_check_3), 2),
            BadgeSlot(activity.findViewById(R.id.layout_badge_4), activity.findViewById(R.id.iv_badge_4), activity.findViewById(R.id.iv_check_4), 5),
            BadgeSlot(activity.findViewById(R.id.layout_badge_5), activity.findViewById(R.id.iv_badge_5), activity.findViewById(R.id.iv_check_5), 3),
        )

        activity.lifecycleScope.launch(Dispatchers.IO) {

            // profile : 현재 사용자 프로필
            val profile = appService.getUserProfile()
            val ownerId = profile.ownerId

            // 예외처리) 로그인 사용자만 대표 배지를 저장할 수 있다.
            // - 게스트/익명은 USER_로 시작하지 않으므로 차단한다.
            if (!ownerId.startsWith("USER_")) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "로그인 후 이용할 수 있어요.", Toast.LENGTH_SHORT).show()
                    activity.finish()
                }
                return@launch
            }

            // badgeStatuses : 서버/로컬에서 조회한 배지 상태 목록
            // - isEarned : 획득 여부
            // - isSelected : 현재 대표 배지로 선택되어 있는지 여부
            val badgeStatuses = appService.getBadgeStatuses(ownerId)

            withContext(Dispatchers.Main) {

                // 프로필 카드(상단 미리보기) 기본 정보 표시
                tvNickname.text = profile.nickname
                tvEmail.text = profile.emailOrAnon
                tvDday.text = "나와 마주한 지 D+${profile.serviceDays}일 째"

                // 프로필 이미지 표시
                // - 서버 URI가 기본값(default.png)이 아니면 서버값 우선
                // - 서버값이 없으면 SharedPreferences에 저장된 로컬 URI 사용
                val serverUri = profile.profileImageUri
                val localUri = getSavedProfileUri()
                val displayUri = when {
                    !serverUri.isNullOrBlank() && serverUri.trim() != "default.png" -> serverUri
                    !localUri.isNullOrBlank() -> localUri
                    else -> null
                }
                bindProfileImage(ivProfile, displayUri)

                // 현재 대표 배지 표시(서버 기준)
                // - selectedBadgeId도 여기서 초기화해, 그리드에 체크 표시가 나오게 한다.
                val current = profile.selectedBadge
                if (current != null) {
                    tvBadgeName.text = current.name
                    ivBadgeFrame.setImageResource(badgeIconResOf(current.badgeId, current.name))
                    selectedBadgeId = current.badgeId
                } else {
                    tvBadgeName.text = "대표 배지 없음"
                    ivBadgeFrame.setImageResource(R.drawable.ic_badge_emotion_log)
                    selectedBadgeId = null
                }

                // 배지 그리드 구성
                // - 획득/미획득 색상 처리
                // - 클릭 시 선택/체크/미리보기 갱신
                applyBadgeGrid(
                    slots = slots,
                    statuses = badgeStatuses,
                    tvBadgeName = tvBadgeName,
                    ivBadgeFrame = ivBadgeFrame
                )

                // 저장 버튼 클릭 시 대표 배지 저장
                btnSave.setOnClickListener {
                    val chosen = selectedBadgeId

                    // 예외처리) 선택된 배지가 없으면 저장할 수 없으므로 안내한다.
                    if (chosen == null) {
                        Toast.makeText(activity, "선택된 배지가 없어요.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // 중복 클릭 방지(저장 중 비활성화)
                    btnSave.isEnabled = false

                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        runCatching { appService.selectBadge(ownerId, chosen) }
                            .onSuccess {
                                withContext(Dispatchers.Main) {
                                    // 저장 성공 시 마이페이지로 이동
                                    // - MyPage에서 onResume으로 최신 대표 배지 반영 가능
                                    activity.startActivity(Intent(activity, MyPageActivity::class.java))
                                    activity.finish()
                                }
                            }
                            .onFailure {
                                withContext(Dispatchers.Main) {
                                    // 예외처리) 저장 실패 시 버튼을 다시 활성화하고 안내한다.
                                    btnSave.isEnabled = true
                                    Toast.makeText(activity, "저장에 실패했어요. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }
            }
        }
    }

    // applyBadgeGrid : 배지 그리드 UI를 상태에 맞춰 구성한다.
    // 처리 내용:
    // - earnedSet으로 획득 배지 목록을 만든다.
    // - 미획득 배지는 회색 처리하고 선택을 막는다.
    // - 현재 선택된 배지는 체크 아이콘을 visible로 한다.
    // - 클릭 시 selectedBadgeId 갱신 + 체크 이동 + 상단 미리보기(tvBadgeName/ivBadgeFrame) 갱신
    private fun applyBadgeGrid(
        slots: List<BadgeSlot>,
        statuses: List<BadgeStatus>,
        tvBadgeName: TextView,
        ivBadgeFrame: ImageView
    ) {
        val earnedSet = statuses.filter { it.isEarned }.map { it.badge.badgeId }.toSet()
        val selectedFromServer = statuses.firstOrNull { it.isSelected }?.badge?.badgeId

        // selectedBadgeId가 아직 없다면 서버에서 선택된 값으로 초기화한다.
        selectedBadgeId = selectedBadgeId ?: selectedFromServer

        // 먼저 모든 체크를 숨기고, 선택된 것만 다시 보여준다.
        slots.forEach { it.check.visibility = View.GONE }

        for (slot in slots) {
            val isEarned = earnedSet.contains(slot.badgeId)
            val canSelect = isEarned

            // 획득 여부에 따라 UI 처리
            if (isEarned) {
                slot.root.alpha = 1.0f
                slot.icon.clearColorFilter()
            } else {
                slot.root.alpha = 0.35f
                slot.icon.setColorFilter(Color.parseColor("#9A9A9A"))
            }

            // 현재 선택 배지는 체크 표시
            if (slot.badgeId == selectedBadgeId) {
                slot.check.visibility = View.VISIBLE
            }

            slot.root.setOnClickListener {
                // 예외처리) 미획득 배지는 선택 불가
                if (!canSelect) {
                    Toast.makeText(activity, "아직 획득하지 못한 배지예요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // 선택 상태 갱신
                selectedBadgeId = slot.badgeId

                // 체크 이동
                slots.forEach { it.check.visibility = View.GONE }
                slot.check.visibility = View.VISIBLE

                // 선택한 배지 이름/프레임 미리보기 갱신
                val chosenBadge = statuses.firstOrNull { it.badge.badgeId == slot.badgeId }?.badge
                tvBadgeName.text = chosenBadge?.name ?: tvBadgeName.text
                ivBadgeFrame.setImageResource(badgeIconResOf(slot.badgeId, chosenBadge?.name.orEmpty()))
            }
        }
    }

    // bindProfileImage : 프로필 이미지 표시(URI 기반)
    // 동작:
    // - uriString이 null/blank/default.png이면 기본 아이콘 + tint 적용
    // - 유효하면 tint 제거 후 setImageURI로 표시
    // 예외처리) URI 접근 실패/표시 실패 시 기본 아이콘으로 fallback 한다.
    private fun bindProfileImage(iv: ImageView, uriString: String?) {
        val s = uriString.orEmpty().trim()

        // 기본 아이콘일 때 tint 적용
        if (s.isBlank() || s == "default.png") {
            iv.setImageResource(R.drawable.ic_icon_profile)
            iv.imageTintList = ColorStateList.valueOf(Color.parseColor("#DEDEDE"))
            iv.clearColorFilter()
            return
        }

        // 사진일 때 tint 제거
        iv.clearColorFilter()
        iv.imageTintList = null

        val ok = runCatching {
            activity.contentResolver.openInputStream(Uri.parse(s))?.use { }
            true
        }.getOrElse { false }

        // 예외처리) 스트림을 열 수 없으면 기본 아이콘으로 fallback
        if (!ok) {
            iv.setImageResource(R.drawable.ic_icon_profile)
            iv.imageTintList = ColorStateList.valueOf(Color.parseColor("#DEDEDE"))
            iv.clearColorFilter()
            return
        }

        // 예외처리) setImageURI 실패 시 기본 아이콘으로 fallback
        runCatching { iv.setImageURI(Uri.parse(s)) }
            .onFailure {
                iv.setImageResource(R.drawable.ic_icon_profile)
                iv.imageTintList = ColorStateList.valueOf(Color.parseColor("#DEDEDE"))
                iv.clearColorFilter()
            }
    }

    // getSavedProfileUri : SharedPreferences에 저장된 로컬 프로필 이미지 URI를 조회한다.
    // 용도:
    // - 서버 이미지가 없을 때 로컬 저장 값을 fallback으로 사용한다.
    private fun getSavedProfileUri(): String? {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PROFILE_URI, null)
    }

    // BadgeSlot : 배지 그리드의 한 칸을 구성하는 View 묶음
    // - root : 클릭 영역
    // - icon : 배지 이미지
    // - check : 선택 체크 오버레이
    // - badgeId : 서버/로컬 배지 ID
    private data class BadgeSlot(
        val root: LinearLayout,
        val icon: ImageView,
        val check: ImageView,
        val badgeId: Int
    )

    // badgeIconResOf : 배지 ID/이름을 기반으로 아이콘 리소스를 결정한다.
    // 동작:
    // - badgeId가 알려진 값이면 ID 기반 고정 매핑
    // 예외처리) 어떤 조건에도 해당하지 않으면 기본 아이콘으로 fallback 한다.
    private fun badgeIconResOf(badgeId: Int, badgeName: String): Int {
        return when (badgeId) {
            1 -> R.drawable.ic_badge_start
            2 -> R.drawable.ic_badge_month
            3 -> R.drawable.ic_badge_emotion_log
            4 -> R.drawable.ic_badge_three_days
            5 -> R.drawable.ic_badge_wine
            else -> when {
                badgeName.contains("꾸준") -> R.drawable.ic_badge_start
                badgeName.contains("삼일") -> R.drawable.ic_badge_three_days
                badgeName.contains("한 달") -> R.drawable.ic_badge_month
                badgeName.contains("로그") -> R.drawable.ic_badge_emotion_log
                badgeName.contains("소믈리에") -> R.drawable.ic_badge_wine
                else -> R.drawable.ic_badge_traveler
            }
        }
    }
}