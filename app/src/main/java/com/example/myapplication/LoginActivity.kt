package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
                Toast.makeText(this, "아이디(이메일)를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 로그인 API 호출
            val api = ApiClient.getApiService()
            api.login(ApiService.LoginRequest(email)).enqueue(object : Callback<ApiService.LoginResponse> {
                override fun onResponse(
                    call: Call<ApiService.LoginResponse>,
                    response: Response<ApiService.LoginResponse>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            // user_pk 저장
                            UserManager.saveUserPk(this@LoginActivity, body.pk)
                            // club_pks 저장 (내 동아리 섹션에서 사용)
                            val pks = body.club_pks ?: emptyList()
                            getSharedPreferences("auth", MODE_PRIVATE)
                                .edit()
                                .putString("club_pks", pks.joinToString(","))
                                .apply()

                            // ClubListFragment 표시
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.putExtra("show_club_list", true)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "로그인 응답이 비어 있습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "로그인 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiService.LoginResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "로그인 오류: ${t.message}", Toast.LENGTH_SHORT).show()
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
    }
}