package com.example.myapplication

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AccountCreatedActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_created)
        
        // Intent에서 계좌번호 받기
        val accountNo = intent.getStringExtra("accountNo") ?: ""
        
        // 계좌번호 표시
        val tvAccountNo = findViewById<TextView>(R.id.tv_account_no)
        tvAccountNo.text = accountNo
        
        // 확인 버튼 클릭 시 이전 페이지로 돌아가기
        findViewById<android.widget.Button>(R.id.btn_confirm).setOnClickListener {
            finish()
        }
    }
}
