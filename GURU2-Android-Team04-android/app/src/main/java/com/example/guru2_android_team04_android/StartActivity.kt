package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.util.DateUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        // XML에 첫 TextView를 찾아 문구를 교체
        val tv = findFirstTextView(window.decorView)
        val weekday = DateUtil.todayWeekdayKo()
        tv?.text = "좋은 ${weekday}이에요! \n오늘 당신의 하루는 어떤 색이었나요?"

        lifecycleScope.launch {
            delay(1200)
            startActivity(Intent(this@StartActivity, HomeActivity::class.java))
            finish()
        }
    }

    private fun findFirstTextView(v: View?): TextView? {
        if (v == null) return null
        if (v is TextView) return v
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                val found = findFirstTextView(v.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }
}
