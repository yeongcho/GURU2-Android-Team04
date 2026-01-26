package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.*
import com.example.guru2_android_team04_android.util.DateUtil
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object BottomNavBinder {

    fun bind(activity: AppCompatActivity, selectedItemId: Int) {
        val nav = activity.findViewById<BottomNavigationView>(R.id.nav_view) ?: return
        nav.selectedItemId = selectedItemId

        nav.setOnItemSelectedListener { item ->
            val appService = (activity.application as MyApp).appService

            when (item.itemId) {
                R.id.navigation_home -> {
                    if (selectedItemId != R.id.navigation_home) {
                        activity.startActivity(Intent(activity, HomeActivity::class.java))
                        activity.finish()
                    }
                    true
                }

                R.id.navigation_diary -> {
                    if (selectedItemId == R.id.navigation_diary) return@setOnItemSelectedListener true

                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        val profile = appService.getUserProfile()
                        val ownerId = profile.ownerId

                        val todayYmd = DateUtil.todayYmd()
                        val ym = todayYmd.take(7)
                        val todayEntry = appService.getEntriesByMonth(ownerId, ym)
                            .firstOrNull { it.dateYmd == todayYmd }

                        withContext(Dispatchers.Main) {
                            if (todayEntry == null) {
                                activity.startActivity(Intent(activity, DiaryEditorActivity::class.java))
                            } else {
                                activity.startActivity(
                                    Intent(activity, TodayDiaryDetailActivity::class.java).apply {
                                        putExtra("entryId", todayEntry.entryId)
                                    }
                                )
                            }
                            activity.finish()
                        }
                    }
                    true
                }

                R.id.navigation_calendar -> {
                    // 게스트면 캘린더 진입 막고 토스트
                    val ownerId = appService.currentOwnerIdOrNull()
                    val isMember = ownerId?.startsWith("USER_") == true
                    if (!isMember) {
                        Toast.makeText(activity, "로그인 후 이용할 수 있어요.", Toast.LENGTH_SHORT).show()
                        return@setOnItemSelectedListener false
                    }

                    if (selectedItemId != R.id.navigation_calendar) {
                        activity.startActivity(Intent(activity, DiaryCalendarActivity::class.java))
                        activity.finish()
                    }
                    true
                }

                R.id.navigation_mypage -> {
                    if (selectedItemId != R.id.navigation_mypage) {
                        activity.startActivity(Intent(activity, MyPageActivity::class.java))
                        activity.finish()
                    }
                    true
                }

                else -> false
            }
        }
    }
}
