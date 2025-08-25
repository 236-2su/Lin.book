package com.example.myapplication

import android.os.Bundle
import java.text.SimpleDateFormat
import java.util.*

class LedgerReportCreateActivity : BaseActivity() {

    override fun setupContent(savedInstanceState: Bundle?) {
        // 앱 제목을 "리포트 생성"으로 설정
        setAppTitle("리포트 생성")
        // LedgerReportCreateActivity 내용을 content_container에 추가
        val contentContainer = findViewById<android.widget.FrameLayout>(R.id.content_container)
        val contentView = layoutInflater.inflate(R.layout.ledger_report_create, null)
        contentContainer.addView(contentView)

        // 뒤로가기 버튼 표시
        showBackButton()
    }
}