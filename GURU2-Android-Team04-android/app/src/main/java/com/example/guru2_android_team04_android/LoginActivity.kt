package com.example.guru2_android_team04_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.guru2_android_team04_android.core.AppResult
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etId = findViewById<TextInputEditText>(R.id.et_id)
        val etPw = findViewById<TextInputEditText>(R.id.et_password)

        findViewById<Button>(R.id.btn_login).setOnClickListener {
            val email = etId.text?.toString().orEmpty()
            val pw = etPw.text?.toString().orEmpty().toCharArray()

            lifecycleScope.launch(Dispatchers.IO) {
                val r = appService.login(email, pw)

                withContext(Dispatchers.Main) {
                    when (r) {
                        is AppResult.Success -> {
                            startActivity(Intent(this@LoginActivity, StartActivity::class.java))
                            finish()
                        }

                        is AppResult.Failure -> {
                            Toast.makeText(
                                this@LoginActivity,
                                r.error.userMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

        findViewById<Button>(R.id.btn_signup).setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
