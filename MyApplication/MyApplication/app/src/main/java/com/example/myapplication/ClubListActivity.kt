package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.FrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ClubListActivity : BaseActivity() {

    override fun setupContent() {
        // 앱 제목을 "동아리"로 설정
        setAppTitle("동아리")

        // 게시판 버튼들 숨김
        hideBoardButtons()

        // ClubListActivity 내용을 content_container에 추가
        val contentContainer = findViewById<FrameLayout>(R.id.content_container)
        val contentView = layoutInflater.inflate(R.layout.activity_club_list, null)
        contentContainer.addView(contentView)

        // FAB 버튼 찾아서 클릭 리스너 설정
        val fab = findViewById<FloatingActionButton>(R.id.fab_edit)
        fab.setOnClickListener {
            val intent = Intent(this, ClubCreateActivity::class.java)
            startActivity(intent)
        }
    }
}
