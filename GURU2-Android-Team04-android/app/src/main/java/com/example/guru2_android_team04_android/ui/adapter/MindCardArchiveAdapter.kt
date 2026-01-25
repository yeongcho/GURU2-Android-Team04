package com.example.guru2_android_team04_android.ui.bind

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.guru2_android_team04_android.R
import com.example.guru2_android_team04_android.data.model.MindCardPreview

class MindCardArchiveAdapter(
    private val nicknameProvider: () -> String,
    private val onUnfavorite: (MindCardPreview) -> Unit,
    private val onOpenDetail: (MindCardPreview) -> Unit
) : RecyclerView.Adapter<MindCardArchiveAdapter.VH>() {

    private val items = mutableListOf<MindCardPreview>()

    fun submitList(newItems: List<MindCardPreview>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_mindcard, parent, false)
        return VH(v as ViewGroup)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val nickname = nicknameProvider()

        holder.tvDay.text = "${formatKoreanDate(item.dateYmd)} 마음 카드🌙"

        val (l1, l2) = splitTwoLines(item.comfortPreview)
        holder.tvNick.text = "${nickname}님, ${l1.ifBlank { "오늘도 기록해줘서 고마워요." }}"
        holder.tvConsole.text = l2.ifBlank { "지금은 충분히 잘하고 있어요." }

        holder.tvMission.text = "오늘의 미션: ${item.mission}"

        holder.ivFav.setOnClickListener { onUnfavorite(item) }
        holder.tvLook.setOnClickListener { onOpenDetail(item) }
    }

    override fun getItemCount(): Int = items.size

    class VH(root: ViewGroup) : RecyclerView.ViewHolder(root) {
        val tvDay: TextView = root.findViewById(R.id.tvDayMsg)
        val tvNick: TextView = root.findViewById(R.id.tvNicknameMsg)
        val tvConsole: TextView = root.findViewById(R.id.tvConsoleText)
        val tvMission: TextView = root.findViewById(R.id.tvMissionText)
        val ivFav: ImageView = root.findViewById(R.id.ivFavorite)
        val tvLook: TextView = root.findViewById(R.id.tvLookAnalysis)
    }

    private fun splitTwoLines(text: String): Pair<String, String> {
        val t = text.trim()
        if (t.isBlank()) return "" to ""
        val parts = t.split("\n", ". ", "。", "!", "?", "…")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val first = parts.getOrNull(0).orEmpty()
        val second = parts.getOrNull(1).orEmpty()
        return first to second
    }

    private fun formatKoreanDate(ymd: String): String {
        val y = ymd.take(4)
        val m = ymd.drop(5).take(2).toIntOrNull() ?: 1
        val d = ymd.takeLast(2).toIntOrNull() ?: 1
        return "${y}년 ${m}월 ${d}일"
    }
}
