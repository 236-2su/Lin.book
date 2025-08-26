package com.example.myapplication

import android.os.Bundle
import android.content.Intent
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
        private const val EXTRA_CLUB_PK = "club_pk"
        private const val PREFS_AUTH = "auth"
        private const val KEY_USER_ID = "user_id"
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

        val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
        if (clubPk <= 0) {
            runOnUiThread {
                Toast.makeText(this, "동아리 정보가 없습니다.", Toast.LENGTH_LONG).show()
            }
            return
        }
        // 로그인 시 저장된 사용자 ID 확인
        val userId = getCurrentUserId()
        if (userId <= 0) {
            runOnUiThread {
                Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_LONG).show()
            }
            return
        }
        val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
        val url = "$baseUrl/club/$clubPk/boards/"
        val client = com.example.myapplication.api.ApiClient.createUnsafeOkHttpClient()

        fun buildRequest(authorValue: Int): Request {
            val payload = mapOf(
                "author" to authorValue,
                "type" to "announcement",
                "title" to title,
                "content" to content
            )
            val json = Gson().toJson(payload)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body: RequestBody = json.toRequestBody(mediaType)
            return Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()
        }

        Thread {
            try {
                var response = client.newCall(buildRequest(userId)).execute()
                var responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful) {
                        try {
                            val created = Gson().fromJson(responseBody, BoardItem::class.java)
                            Toast.makeText(this, "게시글이 작성되었습니다.", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, ClubAnnouncementBoardDetailActivity::class.java)
                            intent.putExtra("board_item", created)
                            intent.putExtra("club_pk", clubPk)
                            startActivity(intent)
                            finish()
                        } catch (e: Exception) {
                            Toast.makeText(this, "작성 성공했지만 응답 파싱에 실패했습니다.", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    } else {
                        // BE가 여전히 ClubMember pk를 요구하는 경우를 대비해 재시도
                        val memberPk = fetchMyClubMemberPk(client, baseUrl, clubPk, userId)
                        if (memberPk != null) {
                            response.close()
                            response = client.newCall(buildRequest(memberPk)).execute()
                            responseBody = response.body?.string()
                            if (response.isSuccessful) {
                                try {
                                    val created = Gson().fromJson(responseBody, BoardItem::class.java)
                                    Toast.makeText(this, "게시글이 작성되었습니다.", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, ClubAnnouncementBoardDetailActivity::class.java)
                                    intent.putExtra("board_item", created)
                                    intent.putExtra("club_pk", clubPk)
                                    startActivity(intent)
                                    finish()
                                } catch (e: Exception) {
                                    Toast.makeText(this, "작성 성공했지만 응답 파싱에 실패했습니다.", Toast.LENGTH_LONG).show()
                                    finish()
                                }
                            } else {
                                Toast.makeText(
                                    this,
                                    "등록 실패: ${response.code} - ${responseBody ?: "응답 없음"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this,
                                "등록 실패: ${response.code} - ${responseBody ?: "응답 없음"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "네트워크 오류: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun fetchMyClubMemberPk(client: OkHttpClient, baseUrl: String, clubPk: Int, userId: Int): Int? {
        val listUrl = "$baseUrl/club/$clubPk/members/"
        val req = Request.Builder().url(listUrl).get().build()
        return try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val type = object : com.google.gson.reflect.TypeToken<List<ClubMemberBrief>>() {}.type
                val items: List<ClubMemberBrief> = Gson().fromJson(body, type)
                items.firstOrNull { it.user == userId }?.id
            }
        } catch (_: Exception) {
            null
        }
    }

    data class ClubMemberBrief(
        val id: Int,
        val user: Int
    )

    private fun getCurrentUserId(): Int {
        val prefs = getSharedPreferences(PREFS_AUTH, MODE_PRIVATE)
        return prefs.getInt(KEY_USER_ID, -1)
    }
}
