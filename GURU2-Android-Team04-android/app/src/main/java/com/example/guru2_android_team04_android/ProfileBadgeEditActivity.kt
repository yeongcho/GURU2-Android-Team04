package com.example.guru2_android_team04_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guru2_android_team04_android.ui.bind.ProfileBadgeEditUiBinder

class ProfileBadgeEditActivity : AppCompatActivity() {

    private val appService by lazy { (application as MyApp).appService }
    private lateinit var binder: ProfileBadgeEditUiBinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_badge_edit)

        binder = ProfileBadgeEditUiBinder(this, appService)
        binder.bind()
    }

    override fun onResume() {
        super.onResume()
        binder.bind()
    }
}
