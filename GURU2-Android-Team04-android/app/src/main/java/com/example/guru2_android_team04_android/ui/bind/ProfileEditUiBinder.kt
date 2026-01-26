package com.example.guru2_android_team04_android.ui.bind

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.AppService
import com.example.guru2_android_team04_android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ProfileEditUiBinder : 프로필 편집 화면(activity_profile_edit.xml) <-> AppService 연동 전담 클래스
// 용도:
// - 현재 사용자 프로필 정보를 불러와 화면에 표시한다(닉네임/이메일/D-day/대표 배지/프로필 이미지).
// - 사용자가 입력한 닉네임과 선택한 프로필 이미지를 저장한다.
// - Activity에서 이미지 선택 결과(URI)를 전달받아 즉시 미리보기에 반영한다.
// 동작 흐름:
// 1) bind()에서 프로필을 IO 스레드로 조회한다.
// 2) Main 스레드에서 EditText/TextView/ImageView 등에 값을 반영한다.
// 3) 저장 버튼 클릭 시 닉네임 업데이트 + (선택한 경우) 프로필 이미지 URI 업데이트를 수행한다.
// 4) 성공 시 finish()로 종료하여 이전 화면(MyPage)에서 onResume으로 최신 반영되게 한다.
class ProfileEditUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {
    // selectedProfileImageUri : 사용자가 새로 선택한 프로필 이미지 URI
    // - null이면 이미지를 새로 선택하지 않음을 의미한다.
    // - 저장 시 null이 아니면 updateProfileImageUri를 호출한다.
    private var selectedProfileImageUri: String? = null

    fun bind() {
        val ivProfile = activity.findViewById<ImageView>(R.id.iv_profile_image)
        val etNickname = activity.findViewById<EditText>(R.id.et_nickname)
        val tvEmail = activity.findViewById<TextView>(R.id.tv_email)
        val tvBadgeName = activity.findViewById<TextView>(R.id.tv_badge_name)
        val tvDday = activity.findViewById<TextView>(R.id.tv_d_day)
        val ivBadgeFrame = activity.findViewById<ImageView>(R.id.iv_badge_frame_mypage)

        val btnSave = activity.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btn_save_profile)

        activity.lifecycleScope.launch(Dispatchers.IO) {
            // profile : 현재 사용자 프로필
            // - 실패 시 null로 처리하여 앱 크래시를 방지한다.
            val profile = runCatching { appService.getUserProfile() }.getOrNull()

            withContext(Dispatchers.Main) {

                // 예외처리) 프로필을 못 불러오면 편집할 대상이 없으므로 토스트 후 화면을 종료한다.
                if (profile == null) {
                    Toast.makeText(activity, "프로필을 불러올 수 없어요.", Toast.LENGTH_SHORT).show()
                    activity.finish()
                    return@withContext
                }

                // 닉네임/이메일/D-day 표시
                // - 닉네임은 편집 가능하므로 EditText에 setText 한다.
                etNickname.setText(profile.nickname)
                tvEmail.text = profile.emailOrAnon
                tvDday.text = "나와 마주한 지 D+${profile.serviceDays}일 째"

                // 대표 배지 표시
                // - 배지가 없으면 배지명/프레임을 숨긴다.
                val badge = profile.selectedBadge
                if (badge == null) {
                    tvBadgeName.visibility = android.view.View.GONE
                    ivBadgeFrame.visibility = android.view.View.GONE
                } else {
                    tvBadgeName.visibility = android.view.View.VISIBLE
                    ivBadgeFrame.visibility = android.view.View.VISIBLE
                    tvBadgeName.text = badge.name

                    // 배지 프레임 아이콘 설정
                    ivBadgeFrame.setImageResource(
                        activity.resources.getIdentifier("ic_badge_emotion_log", "drawable", activity.packageName)
                    )
                }

                // 프로필 이미지 표시 우선순위
                // 1) 사용자가 이번 화면에서 새로 선택한 이미지(selectedProfileImageUri)가 있으면 그것을 우선 표시
                // 2) 없으면 서버에 저장된 profile.profileImageUri를 표시
                val uriToShow = selectedProfileImageUri ?: profile.profileImageUri
                bindProfileImage(ivProfile, uriToShow)

                // 저장 버튼 클릭 로직
                // - 닉네임은 항상 업데이트 시도
                // - 이미지는 새로 선택한 경우에만 업데이트 시도
                btnSave.setOnClickListener {
                    val newNick = etNickname.text?.toString().orEmpty().trim()

                    // 예외처리) 닉네임이 비어있으면 저장할 수 없으므로 토스트로 안내한다.
                    if (newNick.isBlank()) {
                        Toast.makeText(activity, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        val nickOk = appService.updateNickname(newNick)

                        // 이미지는 사용자가 새로 선택했을 때만 업데이트한다.
                        val imgOk = if (selectedProfileImageUri != null) {
                            appService.updateProfileImageUri(selectedProfileImageUri)
                        } else true

                        withContext(Dispatchers.Main) {
                            // 예외처리) 닉네임/이미지 중 하나라도 실패하면 안내하고 종료하지 않는다.
                            if (!nickOk || !imgOk) {
                                Toast.makeText(activity, "저장에 실패했어요.", Toast.LENGTH_SHORT).show()
                                return@withContext
                            }

                            Toast.makeText(activity, "저장했어요.", Toast.LENGTH_SHORT).show()
                            // 저장 성공 시 종료
                            // - 이전 화면(MyPage)은 onResume에서 다시 bind()하여 최신 반영한다.
                            activity.finish()
                        }
                    }
                }
            }
        }
    }

    // setSelectedProfileImageUri : Activity에서 선택된 이미지 URI를 전달받는다.
    // 용도:
    // - 사용자가 사진을 고르면 즉시 미리보기에 반영하고, 저장 시 업데이트 대상으로 사용한다.
    fun setSelectedProfileImageUri(uriString: String?) {
        selectedProfileImageUri = uriString
        val ivProfile = activity.findViewById<ImageView>(R.id.iv_profile_image)
        bindProfileImage(ivProfile, uriString)
    }

    // bindProfileImage : 프로필 ImageView에 URI 기반 이미지를 표시한다.
    // 동작:
    // - uriString이 null/blank/default.png이면 기본 아이콘 표시 + 회색 tint 적용
    // - 유효 URI면 tint 제거 후 setImageURI로 로컬 이미지를 표시
    // 예외처리) URI 접근이 불가하거나 setImageURI 실패 시 기본 아이콘으로 fallback 한다.
    private fun bindProfileImage(iv: ImageView, uriString: String?) {
        val s = uriString.orEmpty().trim()

        // 기본 아이콘 조건
        if (s.isBlank() || s == "default.png") {
            iv.setImageResource(R.drawable.ic_icon_profile)
            iv.imageTintList = ColorStateList.valueOf(Color.parseColor("#DEDEDE"))
            return
        }

        // 사진일 때는 원본 색이 보이도록 tint를 제거한다.
        iv.clearColorFilter()
        iv.imageTintList = null

        // ok : 해당 URI를 실제로 읽을 수 있는지 stream open으로 검증한다.
        val ok = runCatching {
            activity.contentResolver.openInputStream(Uri.parse(s))?.use { }
            true
        }.getOrElse { false }

        // 예외처리) URI가 유효하지 않으면 기본 아이콘으로 되돌린다.
        if (!ok) {
            iv.setImageResource(R.drawable.ic_icon_profile)
            return
        }

        // 예외처리) setImageURI 자체 실패 가능성이 있으므로 runCatching으로 fallback 처리한다.
        runCatching {
            iv.setImageURI(Uri.parse(s))
        }.getOrElse {
            iv.setImageResource(R.drawable.ic_icon_profile)
        }
    }
}