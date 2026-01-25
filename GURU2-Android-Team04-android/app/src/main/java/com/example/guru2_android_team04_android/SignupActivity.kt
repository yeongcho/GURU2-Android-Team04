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

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etNick = findViewById<TextInputEditText>(R.id.et_signup_username)
        val etEmail = findViewById<TextInputEditText>(R.id.et_signup_id)
        val etPw = findViewById<TextInputEditText>(R.id.et_signup_password)
        val etPw2 = findViewById<TextInputEditText>(R.id.et_signup_check_password)

        findViewById<Button>(R.id.btn_signup).setOnClickListener {
            val nick = etNick.text?.toString().orEmpty()
            val email = etEmail.text?.toString().orEmpty()
            val pw = etPw.text?.toString().orEmpty().toCharArray()
            val pw2 = etPw2.text?.toString().orEmpty().toCharArray()

            lifecycleScope.launch(Dispatchers.IO) {
                val r = appService.signUp(email, pw, pw2, nick)

                withContext(Dispatchers.Main) {
                    when (r) {
                        is AppResult.Success -> {
                            // signUp 내부에서 세션 ownerId를 USER_xxx로 전환해줌
                            startActivity(Intent(this@SignupActivity, StartActivity::class.java))
                            finish()
                        }
                        is AppResult.Failure -> {
                            Toast.makeText(
                                this@SignupActivity,
                                r.error.userMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
}
