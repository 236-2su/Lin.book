package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ClubAnnouncementBoardCreateActivity : AppCompatActivity() {
    
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var btnBack: Button
    private lateinit var btnSubmit: Button
    
    companion object {
        private const val BASE_URL = "http://10.0.2.2:8000"
        private const val CLUB_PK = 1
        private const val AUTHOR_ID = 1 // TODO: 로그인 연동 시 실제 사용자 id로 대체
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_announcement_board_create)
        
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

        val url = "$BASE_URL/club/$CLUB_PK/boards/"
        val client = OkHttpClient()

        // 요청 본문 생성 (type은 announcement 고정)
        val payload = mapOf(
            "author" to AUTHOR_ID,
            "type" to "announcement",
            "title" to title,
            "content" to content
        )

        val json = Gson().toJson(payload)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body: RequestBody = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this, "게시글이 작성되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            "등록 실패: ${response.code} - ${responseBody ?: "응답 없음"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "네트워크 오류: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
