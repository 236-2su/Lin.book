package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ClubForumBoardCreateActivity : AppCompatActivity() {
    
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var btnBack: Button
    private lateinit var btnSubmit: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_forum_board_create)
        
        initViews()
        setupListeners()
    }
    
    private fun initViews() {
        etTitle = findViewById(R.id.et_title)
        etContent = findViewById(R.id.et_content)
        btnBack = findViewById(R.id.btn_back)
        btnSubmit = findViewById(R.id.btn_submit)
    }
    
    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        
        btnSubmit.setOnClickListener {
            if (validateInput()) {
                createBoard()
            }
        }
    }
    
    private fun validateInput(): Boolean {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()
        
        if (title.isEmpty()) {
            Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            etTitle.requestFocus()
            return false
        }
        
        if (content.isEmpty()) {
            Toast.makeText(this, "내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
            etContent.requestFocus()
            return false
        }
        
        return true
    }
    
    private fun createBoard() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()
        
        // TODO: API 호출하여 게시글 생성
        Toast.makeText(this, "게시글이 작성되었습니다.", Toast.LENGTH_SHORT).show()
        finish()
    }
}

