package com.example.guru2_android_team04_android.ui.bind

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.guru2_android_team04_android.AppService
import com.example.guru2_android_team04_android.ArchiveDiaryDetailActivity
import com.example.guru2_android_team04_android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// MindCardArchiveUiBinder : activity_mindcard_archive.xml ↔ AppService 연동 전담 클래스
class MindCardArchiveUiBinder(
    private val activity: AppCompatActivity,
    private val appService: AppService
) {
    private var ownerId: String = ""
    private var nickname: String = ""

    private lateinit var rv: RecyclerView
    private lateinit var adapter: MindCardArchiveAdapter

    fun bind() {
        rv = activity.findViewById(R.id.rvMindCards)

        adapter = MindCardArchiveAdapter(
            nicknameProvider = { nickname },
            onUnfavorite = { item ->
                // 즐겨찾기 해제는 IO에서 처리하고, 끝나면 리스트 갱신
                activity.lifecycleScope.launch(Dispatchers.IO) {
                    runCatching { appService.setEntryFavorite(ownerId, item.entryId, false) }
                    refresh()
                }
            },
            onOpenDetail = { item ->
                activity.startActivity(
                    Intent(activity, ArchiveDiaryDetailActivity::class.java).apply {
                        putExtra("entryId", item.entryId)
                    }
                )
            }
        )

        rv.layoutManager = LinearLayoutManager(activity)
        rv.adapter = adapter

        activity.lifecycleScope.launch(Dispatchers.IO) {
            val profile = appService.getUserProfile()
            ownerId = profile.ownerId
            nickname = profile.nickname

            if (!ownerId.startsWith("USER_")) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "로그인 후 이용할 수 있어요.", Toast.LENGTH_SHORT).show()
                    activity.finish()
                }
                return@launch
            }

            refresh()
        }
    }

    private suspend fun refresh() {
        val items = appService.getMindCardArchive(ownerId)
        withContext(Dispatchers.Main) {
            adapter.submitList(items)
        }
    }
}
