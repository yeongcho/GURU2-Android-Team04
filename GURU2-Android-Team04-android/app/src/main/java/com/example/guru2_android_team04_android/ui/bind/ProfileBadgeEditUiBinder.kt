package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.graphics.Color
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

class ProfileBadgeEditUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {
    private var selectedBadgeId: Int? = null

    fun bind() {
        val tvNickname = activity.findViewById<TextView>(R.id.tv_nickname)
        val tvEmail = activity.findViewById<TextView>(R.id.tv_email)
        val tvBadgeName = activity.findViewById<TextView>(R.id.tv_badge_name)
        val tvDday = activity.findViewById<TextView>(R.id.tv_d_day)
        val ivBadgeFrame = activity.findViewById<ImageView>(R.id.iv_badge_frame_mypage)

        val btnSave = activity.findViewById<android.view.View>(R.id.btn_save_badge)

        // 6개 슬롯(레이아웃 + 배지아이콘 + 체크아이콘 + badgeId)
        // 주의: traveler(배지4)는 서버/모델에 없을 수 있어서 예시로 9994 유지
        val slots = listOf(
            BadgeSlot(
                root = activity.findViewById(R.id.layout_badge_1),
                icon = activity.findViewById(R.id.iv_badge_1),
                check = activity.findViewById(R.id.iv_check_1),
                badgeId = 1
            ),
            BadgeSlot(
                root = activity.findViewById(R.id.layout_badge_2),
                icon = activity.findViewById(R.id.iv_badge_2),
                check = activity.findViewById(R.id.iv_check_2),
                badgeId = 4
            ),
            BadgeSlot(
                root = activity.findViewById(R.id.layout_badge_3),
                icon = activity.findViewById(R.id.iv_badge_3),
                check = activity.findViewById(R.id.iv_check_3),
                badgeId = 2
            ),
            BadgeSlot(
                root = activity.findViewById(R.id.layout_badge_4),
                icon = activity.findViewById(R.id.iv_badge_4),
                check = activity.findViewById(R.id.iv_check_4),
                badgeId = 9994 // 서버에 없으면 statuses에 안 잡힘 → 자동으로 미획득 처리됨
            ),
            BadgeSlot(
                root = activity.findViewById(R.id.layout_badge_5),
                icon = activity.findViewById(R.id.iv_badge_5),
                check = activity.findViewById(R.id.iv_check_5),
                badgeId = 3
            ),
            BadgeSlot(
                root = activity.findViewById(R.id.layout_badge_6),
                icon = activity.findViewById(R.id.iv_badge_6),
                check = activity.findViewById(R.id.iv_check_6),
                badgeId = 5
            ),
        )

        activity.lifecycleScope.launch(Dispatchers.IO) {
            val profile = appService.getUserProfile()
            val ownerId = profile.ownerId

            // 게스트 방어
            if (!ownerId.startsWith("USER_")) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "로그인 후 이용할 수 있어요.", Toast.LENGTH_SHORT).show()
                    activity.finish()
                }
                return@launch
            }

            val badgeStatuses = appService.getBadgeStatuses(ownerId)

            withContext(Dispatchers.Main) {
                // 상단 프로필 표시
                tvNickname.text = profile.nickname
                tvEmail.text = profile.emailOrAnon
                tvDday.text = "나와 마주한 지 D+${profile.serviceDays}일 째"

                // 현재 대표배지 반영
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

                // 그리드 반영
                applyBadgeGrid(
                    slots = slots,
                    statuses = badgeStatuses,
                    tvBadgeName = tvBadgeName,
                    ivBadgeFrame = ivBadgeFrame
                )

                // 저장 버튼
                btnSave.setOnClickListener {
                    val chosen = selectedBadgeId
                    if (chosen == null) {
                        Toast.makeText(activity, "선택된 배지가 없어요.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // 메인스레드 막기 + 중복클릭 방지
                    btnSave.isEnabled = false

                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        runCatching {
                            appService.selectBadge(ownerId, chosen)
                        }.onSuccess {
                            withContext(Dispatchers.Main) {
                                activity.startActivity(Intent(activity, MyPageActivity::class.java))
                                activity.finish()
                            }
                        }.onFailure {
                            withContext(Dispatchers.Main) {
                                btnSave.isEnabled = true
                                Toast.makeText(activity, "저장에 실패했어요. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun applyBadgeGrid(
        slots: List<BadgeSlot>,
        statuses: List<BadgeStatus>,
        tvBadgeName: TextView,
        ivBadgeFrame: ImageView
    ) {
        val earnedSet = statuses.filter { it.isEarned }.map { it.badge.badgeId }.toSet()
        val selectedFromServer = statuses.firstOrNull { it.isSelected }?.badge?.badgeId
        selectedBadgeId = selectedBadgeId ?: selectedFromServer

        // 체크 초기화
        slots.forEach { it.check.visibility = android.view.View.GONE }

        for (slot in slots) {
            val isEarned = earnedSet.contains(slot.badgeId)
            val canSelect = isEarned

            // “미획득” → 회색 tint + 흐리게
            if (isEarned) {
                slot.root.alpha = 1.0f
                slot.icon.clearColorFilter()
            } else {
                slot.root.alpha = 0.35f
                slot.icon.setColorFilter(Color.parseColor("#9A9A9A"))
            }

            // 현재 선택 체크
            if (slot.badgeId == selectedBadgeId) {
                slot.check.visibility = android.view.View.VISIBLE
            }

            slot.root.setOnClickListener {
                if (!canSelect) {
                    Toast.makeText(activity, "아직 획득하지 못한 배지예요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                selectedBadgeId = slot.badgeId
                slots.forEach { it.check.visibility = android.view.View.GONE }
                slot.check.visibility = android.view.View.VISIBLE

                // 상단 미리보기 업데이트
                val chosenBadge = statuses.firstOrNull { it.badge.badgeId == slot.badgeId }?.badge
                tvBadgeName.text = chosenBadge?.name ?: tvBadgeName.text
                ivBadgeFrame.setImageResource(
                    badgeIconResOf(slot.badgeId, chosenBadge?.name.orEmpty())
                )
            }
        }
    }

    private data class BadgeSlot(
        val root: LinearLayout,
        val icon: ImageView,
        val check: ImageView,
        val badgeId: Int
    )

    private fun badgeIconResOf(badgeId: Int, badgeName: String): Int {
        return when (badgeId) {
            1 -> R.drawable.ic_badge_start
            2 -> R.drawable.ic_badge_month
            3 -> R.drawable.ic_badge_emotion_log
            4 -> R.drawable.ic_badge_three_days
            5 -> R.drawable.ic_badge_wine
            9994 -> R.drawable.ic_badge_traveler
            else -> when {
                badgeName.contains("꾸준") -> R.drawable.ic_badge_start
                badgeName.contains("삼일") -> R.drawable.ic_badge_three_days
                badgeName.contains("한 달") -> R.drawable.ic_badge_month
                badgeName.contains("로그") -> R.drawable.ic_badge_emotion_log
                badgeName.contains("소믈리에") -> R.drawable.ic_badge_wine
                badgeName.contains("여행") -> R.drawable.ic_badge_traveler
                else -> R.drawable.ic_badge_emotion_log
            }
        }
    }
}
