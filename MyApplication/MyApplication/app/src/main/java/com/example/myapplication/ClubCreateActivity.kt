package com.example.myapplication

import android.os.Bundle

class ClubCreateActivity : BaseActivity() {

    override fun setupContent() {
        // ClubCreateActivity의 내용을 content_container에 추가
        val contentContainer = findViewById<android.widget.FrameLayout>(R.id.content_container)
        val contentView = layoutInflater.inflate(R.layout.activity_club_create, null)
        contentContainer.addView(contentView)

        // root_page.xml의 제목을 "동아리"로 설정
        setAppTitle("동아리")

        // 게시판 버튼 숨기기
        hideBoardButtons()
    }
}
