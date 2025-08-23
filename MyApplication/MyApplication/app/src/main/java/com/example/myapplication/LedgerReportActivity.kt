package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button

class LedgerReportActivity : BaseActivity() {

    override fun setupContent() {
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