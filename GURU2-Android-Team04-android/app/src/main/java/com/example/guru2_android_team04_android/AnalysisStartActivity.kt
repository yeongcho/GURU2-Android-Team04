package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.data.DiaryEntryReader
import com.example.guru2_android_team04_android.util.DateUtil
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class AnalysisStartActivity : AppCompatActivity() {

    private val reader by lazy { DiaryEntryReader(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_start)

        val entryId = intent.getLongExtra("entryId", -1L)

        // XML에 날짜 TextView id가 없어서 "두 번째 TextView"를 찾아 바꿀 수도 있지만,
        // 지금은 간단히 루트에서 첫/둘 TextView 찾는 유틸을 쓰는 방식으로 처리 가능.
        // 여기서는 안전하게: 화면에 id를 부여하는 게 베스트(하지만 네 XML을 안 바꾸는 조건이라면 아래 방식 권장)
        val tvDate = findViewById<TextView>(R.id.tv_analysis_date)

        if (entryId <= 0L) {
            Toast.makeText(this, "entryId가 올바르지 않아요.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 날짜는 그냥 오늘로 표기(원하면 entry.dateYmd 기반으로 변경 가능)
        // tvDate?.text = DateUtil.todayPrettyKo()
        lifecycleScope.launch(Dispatchers.IO) {
            val entry = reader.getByIdOrNull(entryId)

            val pretty = if (entry != null) {
                // entry.dateYmd = "yyyy-MM-dd" 를 "yyyy년 M월 d일 E요일" 같은 형태로 변환
                ymdToPrettyKo(entry.dateYmd)
            } else {
                // 못 찾으면 오늘 날짜로 대체
                DateUtil.todayPrettyKo()
            }

            withContext(Dispatchers.Main) {
                tvDate.text = pretty
            }
        }

        findViewById<MaterialButton>(R.id.btn_start_analysis).setOnClickListener {
            startActivity(Intent(this, AnalysisDiaryActivity::class.java).apply {
                putExtra("entryId", entryId)
            })
        }
    }

    private fun ymdToPrettyKo(ymd: String): String {
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
            val outFmt = SimpleDateFormat("yyyy년 M월 d일 E요일", Locale.KOREA)
            outFmt.format(inFmt.parse(ymd)!!)
        } catch (_: Exception) {
            // 파싱 실패 시 오늘 날짜로 fallback
            DateUtil.todayPrettyKo()
        }
    }
}
