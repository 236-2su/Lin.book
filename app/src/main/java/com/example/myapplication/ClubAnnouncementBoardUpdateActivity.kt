package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class ClubAnnouncementBoardUpdateActivity : AppCompatActivity() {
    
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var btnBack: Button
    private lateinit var btnSubmit: Button
    private lateinit var boardItem: BoardItem
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_announcement_board_update)
        
        // Intent에서 BoardItem 데이터 받기
        boardItem = intent.getParcelableExtra("board_item") ?: BoardItem(
            1, "announcement", "샘플 게시글", "샘플 내용", 13, "2025-08-20T10:25:00.000000+09:00", 1
        )
        
        initViews()
        setupListeners()
        loadBoardData()
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
                updateBoard()
            }
        }
    }
    
    private fun loadBoardData() {
        etTitle.setText(boardItem.title)
        etContent.setText(boardItem.content)
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
    
    private fun updateBoard() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()
        val clubPk = boardItem.club
        val boardId = boardItem.id
        if (clubPk <= 0 || boardId <= 0) {
            Toast.makeText(this, "동아리/게시글 정보가 없습니다.", Toast.LENGTH_LONG).show()
            return
        }

        val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
        val url = "$baseUrl/club/$clubPk/boards/$boardId/"
        val client = com.example.myapplication.api.ApiClient.createUnsafeOkHttpClient()

        val payload = mapOf(
            "title" to title,
            "content" to content
        )
        val json = com.google.gson.Gson().toJson(payload)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body: okhttp3.RequestBody = json.toRequestBody(mediaType)
        val req = okhttp3.Request.Builder()
            .url(url)
            .patch(body)
            .addHeader("Content-Type", "application/json")
            .build()

        Log.d("BoardUpdate", "PATCH /club/$clubPk/boards/$boardId title=${title.length} content=${content.length}")
        Thread {
            try {
                client.newCall(req).execute().use { resp ->
                    val respBody = resp.body?.string()
                    if (resp.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this, "게시글이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                    } else {
                        Log.e("BoardUpdate", "fail code=${resp.code} body=${respBody}")
                        runOnUiThread {
                            Toast.makeText(this, "수정 실패: ${resp.code}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BoardUpdate", "network error: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this, "네트워크 오류: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
