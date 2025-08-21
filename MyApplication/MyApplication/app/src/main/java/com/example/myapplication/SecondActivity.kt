package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.view.View

class SecondActivity : BaseActivity() {

    private lateinit var contentView: View

    override fun setupContent() {
        // SecondActivity 내용을 content_container에 추가
        val contentContainer = findViewById<android.widget.FrameLayout>(R.id.content_container)
        contentView = layoutInflater.inflate(R.layout.activity_second_content, null)
        contentContainer.addView(contentView)
        
        // SecondActivity 로직 실행
        setupSecondActivityLogic()
    }
    
    private fun setupSecondActivityLogic() {
        // 뒤로가기 버튼 이벤트
        val btnBack = contentView.findViewById<Button>(R.id.btn_back)
        btnBack.setOnClickListener {
            finish() // 현재 액티비티 종료하고 이전 페이지로 돌아가기
        }
    }
}
