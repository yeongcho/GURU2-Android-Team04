package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.core.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnalysisComfortActivity : AppCompatActivity() {

    private val appService by lazy { (application as MyApp).appService }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_comfort)

        val entryId = intent.getLongExtra("entryId", -1L)

        val tvContent = findViewById<TextView>(R.id.tv_analysis_content)
        val tvTags = findViewById<TextView>(R.id.tv_tags)

        lifecycleScope.launch(Dispatchers.IO) {
            val r = appService.getMindCardDetailByEntryIdSafe(entryId)
            withContext(Dispatchers.Main) {
                when (r) {
                    is AppResult.Success -> {
                        tvContent.text = r.data.fullText
                        tvTags.text = r.data.hashtags.joinToString(separator = " ") { "#$it" }
                    }
                    is AppResult.Failure -> {
                        Toast.makeText(
                            this@AnalysisComfortActivity,
                            r.error.userMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        }

        findViewById<com.google.android.material.card.MaterialCardView>(R.id.btnTapContinue).setOnClickListener {
            startActivity(Intent(this, AnalysisActionsActivity::class.java).apply {
                putExtra("entryId", entryId)
            })
        }
    }
}
