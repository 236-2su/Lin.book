package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etId: EditText
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
            // TODO: 실제 로그인 검증 로직 구현 (현재는 검증 없이 바로 이동)

            // 로그인 후 ClubListFragment가 보이도록 플래그 전달
            val intent = android.content.Intent(this, MainActivity::class.java)
            intent.putExtra("show_club_list", true)
            startActivity(intent)
            finish() // LoginActivity 종료
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