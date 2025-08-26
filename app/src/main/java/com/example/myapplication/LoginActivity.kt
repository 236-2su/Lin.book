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
            val id = etId.text.toString().trim()
            
            if (id.isEmpty()) {
                Toast.makeText(this, "아이디를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 로그인 API 호출
            performLogin(id)
        }
    }

    // 로그인 처리 메서드
    private fun performLogin(userId: String) {
        // 아이디만 입력하면 로그인되도록 간단하게 처리
        // 실제로는 API 호출을 해야 하지만, 여기서는 아이디가 있으면 로그인 성공으로 처리
        
        if (userId.isNotEmpty()) {
            // 로그인 성공 - user_pk를 임시로 1로 설정 (실제로는 API에서 받아와야 함)
            UserManager.saveUserPk(this, 1)
            
            Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
            
            // MainActivity로 이동
            val intent = android.content.Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // LoginActivity 종료
        } else {
            Toast.makeText(this, "아이디를 입력해주세요", Toast.LENGTH_SHORT).show()
        }
    }
}