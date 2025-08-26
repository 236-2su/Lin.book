package com.example.myapplication

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etId: EditText
    private lateinit var etPassword: EditText
    private lateinit var tvRegister: TextView
    private lateinit var btnLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etId = findViewById(R.id.et_id)
        etPassword = findViewById(R.id.et_password)
        tvRegister = findViewById(R.id.tv_register)
        btnLogin = findViewById(R.id.btn_login)
    }

    private fun setupListeners() {
        // 회원가입 링크 클릭
        tvRegister.setOnClickListener {
            // TODO: 회원가입 페이지로 이동
            Toast.makeText(this, "회원가입 페이지로 이동", Toast.LENGTH_SHORT).show()
        }
        
        // 로그인하기 버튼 클릭
        btnLogin.setOnClickListener {
            val email = etId.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "이메일을 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val api = com.example.myapplication.api.ApiClient.getApiService()
            api.login(com.example.myapplication.api.ApiService.LoginRequest(email))
                .enqueue(object : retrofit2.Callback<com.example.myapplication.api.ApiService.LoginResponse> {
                    override fun onResponse(
                        call: retrofit2.Call<com.example.myapplication.api.ApiService.LoginResponse>,
                        response: retrofit2.Response<com.example.myapplication.api.ApiService.LoginResponse>
                    ) {
                        if (!this@LoginActivity.isFinishing && !this@LoginActivity.isDestroyed) {
                            val body = response.body()
                            if (response.isSuccessful && body != null) {
                                val prefs = getSharedPreferences("auth", MODE_PRIVATE)
                                prefs.edit().putInt("user_id", body.pk).apply()
                                // club_pks 저장 (쉼표구분 문자열로 보관)
                                body.club_pks?.let { list ->
                                    val joined = list.joinToString(",")
                                    prefs.edit().putString("club_pks", joined).apply()
                                }

                                val intent = android.content.Intent(this@LoginActivity, MainActivity::class.java)
                                intent.putExtra("show_club_list", true)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this@LoginActivity, "로그인 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onFailure(
                        call: retrofit2.Call<com.example.myapplication.api.ApiService.LoginResponse>,
                        t: Throwable
                    ) {
                        if (!this@LoginActivity.isFinishing && !this@LoginActivity.isDestroyed) {
                            Toast.makeText(this@LoginActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
        }
    }

    // 로그인 처리 메서드 (필요시 호출)
    private fun performLogin() {
        val id = etId.text.toString()
        val password = etPassword.text.toString()

        if (id.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "아이디와 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: 실제 로그인 로직 구현
        Toast.makeText(this, "로그인 시도: $id", Toast.LENGTH_SHORT).show()
    }
}
