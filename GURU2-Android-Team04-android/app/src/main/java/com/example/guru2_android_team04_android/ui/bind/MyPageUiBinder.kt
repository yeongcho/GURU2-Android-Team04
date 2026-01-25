package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
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

// MyPageUiBinder : activity_mypage.xml ↔ AppService 연동 전담 클래스
class MyPageUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {

    fun bind() {
        val ivProfile = activity.findViewById<ImageView>(R.id.iv_profile_image)
        val tvNickname = activity.findViewById<TextView>(R.id.tv_nickname)
        val tvEmail = activity.findViewById<TextView>(R.id.tv_email)
        val tvBadgeName = activity.findViewById<TextView>(R.id.tv_badge_name)
        val tvDday = activity.findViewById<TextView>(R.id.tv_d_day)
        val ivBadgeFrame = activity.findViewById<ImageView>(R.id.iv_badge_frame_mypage)

        val layoutMemberInfo = activity.findViewById<LinearLayout>(R.id.layout_member_info)
        val tvGuestInfo = activity.findViewById<TextView>(R.id.tv_guest_info)

        val btnAchievements = activity.findViewById<View>(R.id.btn_achievements)
        val btnDiaryList = activity.findViewById<View>(R.id.btn_diary_list)
        val btnMindCard = activity.findViewById<View>(R.id.btn_mind_card)
        val btnEditProfile = activity.findViewById<View>(R.id.btn_edit_profile)
        val btnLogout = activity.findViewById<View>(R.id.btn_logout)
        val btnDeleteAccount = activity.findViewById<View>(R.id.btn_delete_account)

        activity.lifecycleScope.launch(Dispatchers.IO) {
            val profile = appService.getUserProfile()
            val isMember = profile.ownerId.startsWith("USER_")

            withContext(Dispatchers.Main) {
                // 로그인/비로그인 UI 전환
                layoutMemberInfo.visibility = if (isMember) View.VISIBLE else View.VISIBLE
                tvGuestInfo.visibility = if (isMember) View.GONE else View.VISIBLE

                // 게스트면 로그아웃/탈퇴 버튼 숨김 처리(원하면 visible 유지해도 됨)
                btnLogout.visibility = if (isMember) View.VISIBLE else View.GONE
                btnDeleteAccount.visibility = if (isMember) View.VISIBLE else View.GONE

                // 텍스트 바인딩
                tvNickname.text = profile.nickname
                tvEmail.text = profile.emailOrAnon
                tvDday.text = "나와 마주한 지 D+${profile.serviceDays}일 째"

                // 배지 표시(없으면 숨김)
                val badge = profile.selectedBadge
                if (badge == null) {
                    tvBadgeName.visibility = View.GONE
                    ivBadgeFrame.visibility = View.GONE
                } else {
                    tvBadgeName.visibility = View.VISIBLE
                    ivBadgeFrame.visibility = View.VISIBLE
                    tvBadgeName.text = badge.name
                    ivBadgeFrame.setImageResource(badgeIconResOf(badge.badgeId, badge.name))
                }

                // 프로필 이미지 표시(기본/URI)
                bindProfileImage(ivProfile, profile.profileImageUri)

                // 클릭 동작들
                btnAchievements.setOnClickListener {
                    if (!isMember) {
                        Toast.makeText(activity, "로그인 후 이용할 수 있어요.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    activity.startActivity(Intent(activity, ProfileBadgeEditActivity::class.java))
                }

                btnDiaryList.setOnClickListener {
                    activity.startActivity(Intent(activity, DiaryCalendarActivity::class.java))
                }

                btnMindCard.setOnClickListener {
                    activity.startActivity(Intent(activity, MindCardArchiveActivity::class.java))
                }

                btnEditProfile.setOnClickListener {
                    if (!isMember) {
                        Toast.makeText(activity, "로그인 후 이용할 수 있어요.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    activity.startActivity(Intent(activity, ProfileEditActivity::class.java))
                }

                btnLogout.setOnClickListener {
                    appService.logout()
                    activity.startActivity(Intent(activity, LoginActivity::class.java))
                    activity.finish()
                }

                btnDeleteAccount.setOnClickListener {
                    AlertDialog.Builder(activity)
                        .setTitle("회원 탈퇴")
                        .setMessage("정말 탈퇴할까요? 모든 데이터가 삭제돼요.")
                        .setPositiveButton("탈퇴") { _, _ ->
                            val ok = appService.withdrawCurrentUser()
                            if (!ok) {
                                Toast.makeText(activity, "탈퇴에 실패했어요.", Toast.LENGTH_SHORT).show()
                                return@setPositiveButton
                            }
                            activity.startActivity(Intent(activity, LoginActivity::class.java))
                            activity.finish()
                        }
                        .setNegativeButton("취소", null)
                        .show()
                }
            }
        }
    }

    private fun bindProfileImage(iv: ImageView, uriString: String?) {
        val s = uriString.orEmpty().trim()
        if (s.isBlank() || s == "default.png") {
            iv.setImageResource(R.drawable.ic_icon_profile)
            return
        }
        runCatching {
            iv.setImageURI(Uri.parse(s))
        }.getOrElse {
            iv.setImageResource(R.drawable.ic_icon_profile)
        }
    }

    private fun badgeIconResOf(badgeId: Int, badgeName: String): Int {
        // DB seed(1~5) + XML에 있는 6개 디자인 간 불일치가 있어서 “id 우선 + name 보조”로 안전 처리
        return when (badgeId) {
            1 -> R.drawable.ic_badge_start
            2 -> R.drawable.ic_badge_month      // "한 달의 조각들"
            3 -> R.drawable.ic_badge_emotion_log // "감정 로그 수집가"
            4 -> R.drawable.ic_badge_three_days  // "작심삼일 마스터"
            5 -> R.drawable.ic_badge_wine        // "감정 소믈리에"
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
