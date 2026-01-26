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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// MyPageUiBinder : 마이페이지 화면(activity_mypage.xml) <-> 데이터(AppService) 연결 전담 클래스
// 용도:
// - 현재 사용자 프로필을 조회해 화면에 표시한다(닉네임/이메일/D-day/대표 뱃지/프로필 이미지).
// - 로그인 회원(USER_)과 게스트 상태를 구분하여 UI 노출을 다르게 처리한다.
// - 마이페이지 메뉴(업적/일기 모아보기/마음 카드/프로필 편집/로그아웃/회원탈퇴) 클릭 이벤트를 연결한다.
// 동작 흐름:
// 1) 화면의 View들을 findViewById로 가져온다.
// 2) 버튼 클릭 이벤트를 먼저 연결하고, 로그인 여부(isMember)에 따라 접근 제한을 둔다.
// 3) IO 스레드에서 사용자 프로필을 조회한다.
// 4) Main 스레드에서 로그인/게스트 상태에 맞게 View visibility와 텍스트/이미지를 업데이트한다.
class MyPageUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {

    companion object {
        // PREFS_NAME/KEY_PROFILE_URI : 로컬에 저장해 둔 프로필 이미지 URI를 관리하기 위한 SharedPreferences 키
        // - 서버 프로필 이미지가 없을 때, 사용자가 선택한 로컬 이미지를 임시로 표시할 수 있다.
        private const val PREFS_NAME = "mypage_prefs"
        private const val KEY_PROFILE_URI = "profile_image_uri"
    }

    // bind : 마이페이지 화면을 현재 상태에 맞게 다시 채운다.
    // - onCreate/onResume에서 호출되어 최신 정보(뱃지/프로필)를 반영한다.
    fun bind() {

        // 프로필 카드 UI 요소
        val ivProfile = activity.findViewById<ImageView>(R.id.iv_profile_image)
        val tvNickname = activity.findViewById<TextView>(R.id.tv_nickname)
        val tvEmail = activity.findViewById<TextView>(R.id.tv_email)
        val tvBadgeName = activity.findViewById<TextView>(R.id.tv_badge_name)
        val tvDday = activity.findViewById<TextView>(R.id.tv_d_day)
        val ivBadgeFrame = activity.findViewById<ImageView>(R.id.iv_badge_frame_mypage)

        // 회원 정보 영역(로그인 상태에서 노출) / 게스트 안내 문구(게스트 상태에서 노출)
        val layoutMemberInfo = activity.findViewById<LinearLayout>(R.id.layout_member_info)
        val tvGuestInfo = activity.findViewById<TextView>(R.id.tv_guest_info)

        // 메뉴 버튼들
        val btnAchievements = activity.findViewById<View>(R.id.btn_achievements)
        val btnDiaryList = activity.findViewById<View>(R.id.btn_diary_list)
        val btnMindCard = activity.findViewById<View>(R.id.btn_mind_card)
        val btnEditProfile = activity.findViewById<View>(R.id.btn_edit_profile)
        val btnLogout = activity.findViewById<View>(R.id.btn_logout)
        val btnDeleteAccount = activity.findViewById<View>(R.id.btn_delete_account)

        // 섹션 타이틀(로그인 상태에서만 노출)
        val tvSectionMyRecords = activity.findViewById<View>(R.id.tv_section_my_records)
        val tvSectionAccountInfo = activity.findViewById<View>(R.id.tv_section_account_info)

        // 게스트 안내 문구 클릭 시 로그인 화면으로 이동
        // - "로그인하고 업적을 달성하세요" 문구를 클릭했을 때 로그인 유도
        tvGuestInfo.setOnClickListener {
            activity.startActivity(Intent(activity, LoginActivity::class.java))
        }

        // isMember : 현재 사용자가 "로그인한 회원(USER_로 시작)"인지 여부
        // - 비회원(게스트/익명)은 접근 제한 및 UI 숨김 처리에 사용한다.
        var isMember = false

        // 마이페이지에서 프로필 이미지 변경/선택 기능 제거
        ivProfile.isClickable = false
        ivProfile.isFocusable = false

        // 일기 모아보기(캘린더/리스트) 진입
        btnDiaryList.setOnClickListener {
            // 예외처리) 게스트 상태면 기능을 막고 안내 토스트를 띄운다.
            if (!isMember) {
                Toast.makeText(activity, "로그인 후 이용할 수 있어요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            activity.startActivity(Intent(activity, DiaryCalendarActivity::class.java))
        }

        // 마음 카드 보관함 진입
        btnMindCard.setOnClickListener {
            // 예외처리) 게스트 상태면 기능을 막고 안내 토스트를 띄운다.
            if (!isMember) {
                Toast.makeText(activity, "로그인 후 이용할 수 있어요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            activity.startActivity(Intent(activity, MindCardArchiveActivity::class.java))
        }

        // 업적(뱃지) 확인/대표 뱃지 변경 화면 진입
        btnAchievements.setOnClickListener {
            // 예외처리) 게스트 상태면 업적을 조회할 수 없으므로 안내 토스트를 띄운다.
            if (!isMember) {
                Toast.makeText(activity, "로그인 후 이용할 수 있어요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            activity.startActivity(Intent(activity, ProfileBadgeEditActivity::class.java))
        }

        // 프로필 편집 화면 진입
        btnEditProfile.setOnClickListener {
            // 예외처리) 게스트 상태면 프로필 편집 불가하므로 안내 토스트를 띄운다.
            if (!isMember) {
                Toast.makeText(activity, "로그인 후 이용할 수 있어요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            activity.startActivity(Intent(activity, ProfileEditActivity::class.java))
        }

        // 로그아웃 처리
        // - 서비스 로그아웃 + 로컬에 저장된 프로필 이미지 URI 제거 + 로그인 화면으로 이동
        btnLogout.setOnClickListener {
            appService.logout()
            clearSavedProfileUri()
            activity.startActivity(Intent(activity, LoginActivity::class.java))
            activity.finish()
        }

        // 회원 탈퇴 처리
        // - AlertDialog로 재확인 후 탈퇴 실행
        // - 성공 시 로컬 이미지 URI 제거 후 로그인 화면으로 이동
        btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(activity)
                .setTitle("회원 탈퇴")
                .setMessage("정말 탈퇴할까요? 모든 데이터가 삭제돼요.")
                .setPositiveButton("탈퇴") { _, _ ->

                    // withdrawCurrentUser : 현재 로그인 유저의 계정을 탈퇴 처리한다.
                    val ok = appService.withdrawCurrentUser()

                    // 예외처리) 서버/로컬 처리 실패 시 화면 이동 없이 안내만 한다.
                    if (!ok) {
                        Toast.makeText(activity, "탈퇴에 실패했어요.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    clearSavedProfileUri()
                    activity.startActivity(Intent(activity, LoginActivity::class.java))
                    activity.finish()
                }
                .setNegativeButton("취소", null)
                .show()
        }

        // 프로필 조회 및 화면 갱신
        // - IO에서 getUserProfile 호출 후 Main에서 UI 반영
        activity.lifecycleScope.launch(Dispatchers.IO) {

            // profile : 현재 사용자 프로필
            // - 게스트/미로그인 상태에서는 예외가 발생하거나 null로 처리될 수 있어 runCatching으로 안전하게 감싼다.
            val profile = runCatching { appService.getUserProfile() }.getOrNull()

            withContext(Dispatchers.Main) {

                // 예외처리) 프로필을 가져오지 못하면(미로그인/오류) 게스트 UI로 전환한다.
                if (profile == null) {
                    isMember = false

                    // 로그인 정보 영역 숨김, 게스트 안내 표시
                    layoutMemberInfo.visibility = View.GONE
                    tvGuestInfo.visibility = View.VISIBLE

                    // "나의 기록" 섹션 및 버튼들은 로그인 상태에서만 노출
                    tvSectionMyRecords.visibility = View.GONE
                    btnDiaryList.visibility = View.GONE
                    btnMindCard.visibility = View.GONE

                    // "계정 정보" 섹션 및 버튼들은 로그인 상태에서만 노출
                    tvSectionAccountInfo.visibility = View.GONE
                    btnEditProfile.visibility = View.GONE
                    btnLogout.visibility = View.GONE
                    btnDeleteAccount.visibility = View.GONE

                    // 뱃지 프레임/뱃지명도 로그인 상태에서만 의미가 있으므로 숨김
                    ivBadgeFrame.visibility = View.GONE
                    tvBadgeName.visibility = View.GONE

                    // 게스트 상태에서는 로컬 이미지도 보여주지 않기 위해 null로 바인딩한다.
                    bindProfileImage(ivProfile, null)
                    return@withContext
                }

                // isMember 판정 기준
                // - ownerId가 "USER_"로 시작하면 로그인 회원으로 간주한다.
                isMember = profile.ownerId.startsWith("USER_")

                // 회원/게스트에 따라 상단 프로필 영역 표시를 전환한다.
                layoutMemberInfo.visibility = if (isMember) View.VISIBLE else View.GONE
                tvGuestInfo.visibility = if (isMember) View.GONE else View.VISIBLE

                // 회원일 때만 "나의 기록" 섹션 노출
                tvSectionMyRecords.visibility = if (isMember) View.VISIBLE else View.GONE
                btnDiaryList.visibility = if (isMember) View.VISIBLE else View.GONE
                btnMindCard.visibility = if (isMember) View.VISIBLE else View.GONE

                // 회원일 때만 "계정 정보" 섹션 노출
                tvSectionAccountInfo.visibility = if (isMember) View.VISIBLE else View.GONE
                btnEditProfile.visibility = if (isMember) View.VISIBLE else View.GONE
                btnLogout.visibility = if (isMember) View.VISIBLE else View.GONE
                btnDeleteAccount.visibility = if (isMember) View.VISIBLE else View.GONE

                // 게스트면 로컬 사진/뱃지 등 개인화 정보는 절대 보여주지 않는다.
                // 예: 이전 로그인 사용자의 로컬 이미지가 남아있는 경우를 방지한다.
                if (!isMember) {
                    ivBadgeFrame.visibility = View.GONE
                    tvBadgeName.visibility = View.GONE
                    bindProfileImage(ivProfile, null)
                    return@withContext
                }

                // 회원 프로필 정보 표시
                tvNickname.text = profile.nickname
                tvEmail.text = profile.emailOrAnon
                tvDday.text = "나와 마주한 지 D+${profile.serviceDays}일 째"

                // 대표 뱃지 표시
                // - 선택된 뱃지가 없으면 뱃지 영역을 숨긴다.
                val badge = profile.selectedBadge
                if (badge == null) {
                    tvBadgeName.visibility = View.GONE
                    ivBadgeFrame.visibility = View.GONE
                } else {
                    tvBadgeName.visibility = View.VISIBLE
                    ivBadgeFrame.visibility = View.VISIBLE
                    tvBadgeName.text = badge.name

                    // badgeId 또는 이름으로 아이콘 리소스를 결정해 프레임에 표시한다.
                    ivBadgeFrame.setImageResource(badgeIconResOf(badge.badgeId, badge.name))
                }

                // 프로필 이미지 URI 선택 우선순위
                // 1) 서버 URI가 있고 default.png가 아니면 서버 이미지를 우선 사용
                // 2) 서버 이미지가 없으면 로컬 저장 URI를 사용(사용자가 이전에 설정한 사진)
                // 3) 둘 다 없으면 기본 아이콘을 사용
                val serverUri = profile.profileImageUri
                val localUri = getSavedProfileUri()
                val displayUri = when {
                    !serverUri.isNullOrBlank() && serverUri.trim() != "default.png" -> serverUri
                    !localUri.isNullOrBlank() -> localUri
                    else -> null
                }

                bindProfileImage(ivProfile, displayUri)
            }
        }
    }

    // bindProfileImage : ImageView에 프로필 이미지를 적용한다.
    // 동작:
    // - uriString이 비어있거나 "default.png"면 기본 아이콘으로 표시 + 회색 tint 적용
    // - 유효한 uriString이면 tint 제거 후 setImageURI로 로컬 이미지를 표시
    // 예외처리) URI가 깨졌거나 접근 불가한 경우 기본 아이콘으로 fallback 한다.
    private fun bindProfileImage(iv: ImageView, uriString: String?) {
        val s = uriString.orEmpty().trim()

        // 기본 아이콘 조건
        // - 서버 기본값("default.png") 또는 null/blank면 기본 프로필 아이콘을 사용한다.
        if (s.isBlank() || s == "default.png") {
            iv.setImageResource(R.drawable.ic_icon_profile)
            iv.imageTintList = ColorStateList.valueOf(Color.parseColor("#DEDEDE"))
            iv.clearColorFilter()
            return
        }

        // 사진일 때는 tint가 있으면 원본 색이 망가져서 제거한다.
        iv.clearColorFilter()
        iv.imageTintList = null

        // ok : ContentResolver로 실제 InputStream을 열어볼 수 있는지 검사한다.
        val ok = runCatching {
            activity.contentResolver.openInputStream(Uri.parse(s))?.use { }
            true
        }.getOrElse { false }

        // 예외처리) URI가 유효하지 않으면 기본 아이콘으로 fallback
        if (!ok) {
            iv.setImageResource(R.drawable.ic_icon_profile)
            iv.imageTintList = ColorStateList.valueOf(Color.parseColor("#DEDEDE"))
            iv.clearColorFilter()
            return
        }

        // 실제 이미지 적용
        // 예외처리) setImageURI 자체가 실패하는 케이스가 있으므로 onFailure로 fallback 처리한다.
        runCatching { iv.setImageURI(Uri.parse(s)) }
            .onFailure {
                iv.setImageResource(R.drawable.ic_icon_profile)
                iv.imageTintList = ColorStateList.valueOf(Color.parseColor("#DEDEDE"))
                iv.clearColorFilter()
            }
    }

    // getSavedProfileUri : SharedPreferences에 저장된 "로컬 프로필 이미지 URI"를 읽는다.
    // 용도:
    // - 서버 프로필 이미지가 없을 때, 사용자가 지정했던 로컬 이미지를 대신 보여주기 위함이다.
    private fun getSavedProfileUri(): String? {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PROFILE_URI, null)
    }

    // clearSavedProfileUri : 로컬에 저장된 프로필 이미지 URI를 삭제한다.
    // 용도:
    // - 로그아웃/회원탈퇴 시 이전 사용자의 로컬 이미지가 다음 사용자(또는 게스트)에게 노출되는 것을 방지한다.
    private fun clearSavedProfileUri() {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_PROFILE_URI).apply()
    }

    // badgeIconResOf : 대표 뱃지 아이콘 리소스를 반환한다.
    // 동작:
    // - badgeId가 알려진 값(1~5)이면 ID 기준으로 고정 매핑
    // - 그렇지 않으면 badgeName에 포함된 키워드로 추정 매핑(데이터 변경/호환 대응)
    // 예외처리) 어떤 규칙에도 안 맞으면 기본 뱃지 아이콘으로 fallback 한다.
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
                else -> R.drawable.ic_badge_emotion_log
            }
        }
    }
}
