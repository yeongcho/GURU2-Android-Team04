package com.example.guru2_android_team04_android.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.guru2_android_team04_android.R
import java.util.Locale

class DiaryListAdapter(
    private var diaryList: List<DiaryListItem>
) : RecyclerView.Adapter<DiaryListAdapter.DiaryViewHolder>() {

    inner class DiaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivEmotion: ImageView = itemView.findViewById(R.id.iv_emotion)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_diary_title)
        val tvDay: TextView = itemView.findViewById(R.id.tv_day)
        val tvDayOfWeek: TextView = itemView.findViewById(R.id.tv_day_of_week)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diary_card, parent, false)
        return DiaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        val item = diaryList[position]

        holder.ivEmotion.setImageResource(getEmotionDrawableId(item.emotion))
        holder.tvTitle.text = item.title
        holder.tvDay.text = item.date.dayOfMonth.toString()

        val dayOfWeek = item.date.dayOfWeek.getDisplayName(
            java.time.format.TextStyle.SHORT,
            Locale.ENGLISH
        ).uppercase()
        holder.tvDayOfWeek.text = dayOfWeek

        // ★ 클릭 리스너 추가: 항목 클릭 시 상세 화면으로 이동
        holder.itemView.setOnClickListener {
            val activity = it.context as AppCompatActivity
            val detailFragment = DiaryDetailFragment()

            // Fragment를 교체하고, 뒤로가기 버튼을 위해 백스택에 추가합니다.
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, detailFragment) // MainActivity의 컨테이너 ID 확인 필요
                .addToBackStack(null)
                .commit()
        }
    }

    override fun getItemCount(): Int = diaryList.size

    fun updateData(newList: List<DiaryListItem>) {
        diaryList = newList
        notifyDataSetChanged()
    }

    private fun getEmotionDrawableId(emotion: EmotionType): Int {
        return when (emotion) {
            EmotionType.ANGRY -> R.drawable.emotion_angry
            EmotionType.CALM -> R.drawable.emotion_calm
            EmotionType.CONFIDENCE -> R.drawable.emotion_confidence
            EmotionType.JOY -> R.drawable.emotion_joy
            EmotionType.NORMAL -> R.drawable.emotion_normal
            EmotionType.SAD -> R.drawable.emotion_sad
            EmotionType.TIRED -> R.drawable.emotion_tired
            else -> R.drawable.emotion_normal
        }
    }
}