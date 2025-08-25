package com.example.myapplication

import android.os.Bundle

class LedgerReportCreateActivity : BaseActivity() {

    override fun setupContent(savedInstanceState: Bundle?) {
        // LedgerReportCreateActivity 내용을 content_container에 추가
        val contentContainer = findViewById<android.widget.FrameLayout>(R.id.content_container)
        val contentView = layoutInflater.inflate(R.layout.ledger_report_create, null)
        contentContainer.addView(contentView)

        // 뒤로가기 버튼 표시
        showBackButton()
    }
}