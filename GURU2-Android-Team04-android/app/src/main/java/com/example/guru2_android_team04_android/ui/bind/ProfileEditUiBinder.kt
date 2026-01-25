package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.AppService
import com.example.guru2_android_team04_android.MyPageActivity
import com.example.guru2_android_team04_android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ProfileEditUiBinder : activity_profile_edit.xml ↔ AppService 연동 전담 클래스
class ProfileEditUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {
    private var selectedImageUri: String? = null

    fun setSelectedProfileImageUri(uri: String?) {
        selectedImageUri = uri
        val iv = activity.findViewById<ImageView>(R.id.iv_profile_image)
        bindProfileImage(iv, uri)
    }

    fun bind() {
        val ivProfile = activity.findViewById<ImageView>(R.id.iv_profile_image)
        val etNickname = activity.findViewById<EditText>(R.id.et_nickname) // XML 변경 반영
        val tvEmail = activity.findViewById<TextView>(R.id.tv_email)
        val tvBadgeName = activity.findViewById<TextView>(R.id.tv_badge_name)
        val tvDday = activity.findViewById<TextView>(R.id.tv_d_day)
        val ivBadgeFrame = activity.findViewById<ImageView>(R.id.iv_badge_frame_mypage)
        val btnSave = activity.findViewById<View>(R.id.btn_save_profile)

        activity.lifecycleScope.launch(Dispatchers.IO) {
            val profile = appService.getUserProfile()
            val ownerId = profile.ownerId

            if (!ownerId.startsWith("USER_")) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "로그인 후 이용할 수 있어요.", Toast.LENGTH_SHORT).show()
                    activity.finish()
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                // 초기 값 세팅
                etNickname.setText(profile.nickname)
                tvEmail.text = profile.emailOrAnon
                tvDday.text = "나와 마주한 지 D+${profile.serviceDays}일 째"

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

                selectedImageUri = profile.profileImageUri
                bindProfileImage(ivProfile, selectedImageUri)

                // 저장
                btnSave.setOnClickListener {
                    val nick = etNickname.text?.toString().orEmpty().trim()
                    if (nick.isBlank()) {
                        Toast.makeText(activity, "닉네임을 입력해줘.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // 중복 클릭 방지
                    btnSave.isEnabled = false

                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        val okNick = runCatching { appService.updateNickname(nick) }.getOrDefault(false)
                        val okImg = runCatching { appService.updateProfileImageUri(selectedImageUri) }.getOrDefault(false)

                        withContext(Dispatchers.Main) {
                            if (!okNick || !okImg) {
                                btnSave.isEnabled = true
                                Toast.makeText(activity, "저장에 실패했어요.", Toast.LENGTH_SHORT).show()
                                return@withContext
                            }

                            activity.startActivity(Intent(activity, MyPageActivity::class.java))
                            activity.finish()
                        }
                    }
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