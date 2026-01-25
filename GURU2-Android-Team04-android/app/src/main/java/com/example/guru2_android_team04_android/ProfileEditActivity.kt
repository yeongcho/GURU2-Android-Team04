package com.example.guru2_android_team04_android

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.ProfileEditUiBinder

class ProfileEditActivity : AppCompatActivity() {

    private val appService by lazy { (application as MyApp).appService }
    private lateinit var binder: ProfileEditUiBinder

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            // 선택한 이미지를 Binder에 전달(미리보기 + 저장용)
            binder.setSelectedProfileImageUri(uri?.toString())
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        binder = ProfileEditUiBinder(this, appService)
        binder.bind()

        // 프로필 이미지 클릭 -> 갤러리 열기
        findViewById<android.widget.ImageView>(R.id.iv_profile_image).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    override fun onResume() {
        super.onResume()
        binder.bind()
    }
}
