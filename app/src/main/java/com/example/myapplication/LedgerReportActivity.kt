package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import java.text.SimpleDateFormat
import java.util.*

class LedgerReportActivity : BaseActivity() {

    override fun setupContent(savedInstanceState: Bundle?) {
        // 앱 제목을 "AI 리포트"로 설정
        setAppTitle("AI 리포트")
        
        // AI 리포트 버튼을 선택된 상태로 설정
        selectBoardButton(btnAiReport)
        
        // Intent로 전달받은 스크롤 위치 복원
        val scrollPosition = intent.getIntExtra("scroll_position", 0)
        restoreBoardButtonScrollPositionFromIntent(scrollPosition)
        
        // LedgerReportActivity 내용을 content_container에 추가
        val contentContainer = findViewById<android.widget.FrameLayout>(R.id.content_container)
        val contentView = layoutInflater.inflate(R.layout.ledger_report, null)
        contentContainer.addView(contentView)

        // 버튼 클릭 이벤트 설정
        setupButtonClickEvents(contentView)
    }

    private fun setupButtonClickEvents(contentView: android.view.View) {
        // 리포트 생성 버튼 클릭 이벤트
        val btnCreateReport = contentView.findViewById<Button>(R.id.btn_create_report)
        btnCreateReport.setOnClickListener {
            val intent = Intent(this, LedgerReportCreateActivity::class.java)
            startActivity(intent)
        }
    }
}