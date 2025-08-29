package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AccountCreatedActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_created)
        
        // Intent에서 계좌번호와 사용자 이름 받기
        val accountNo = intent.getStringExtra("accountNo") ?: ""
        val userName = intent.getStringExtra("userName") ?: "사용자"
        
        // 계좌번호 표시
        val tvAccountNo = findViewById<TextView>(R.id.tv_account_no)
        tvAccountNo.text = accountNo
        
        // 확인 버튼 클릭 시 AccountHistoryActivity로 이동
        findViewById<android.widget.Button>(R.id.btn_confirm).setOnClickListener {
            val intent = Intent(this, AccountHistoryActivity::class.java)
            intent.putExtra("accountNo", accountNo)
            intent.putExtra("userName", userName)
            startActivity(intent)
            finish() // 현재 페이지 종료
        }
    }
}
