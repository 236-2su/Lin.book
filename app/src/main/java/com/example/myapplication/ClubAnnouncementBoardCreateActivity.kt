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

        fun buildRequest(authorValue: Int, includeClub: Boolean = false): Request {
            val payload = mapOf(
                "author" to authorValue,
                "type" to "announcement",
                "title" to title,
                "content" to content
            )
            // club 필드가 필요한 서버(이전 배포본) 대응
            val finalPayload = if (includeClub) {
                val m = java.util.LinkedHashMap(payload)
                m["club"] = clubPk
                m
            } else payload

            val json = Gson().toJson(finalPayload)
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
                // 1차 시도: author에 user_pk 전송
                android.util.Log.d("CreateBoard", "POST /club/$clubPk/boards author(user_pk)=$userId")
                var response = client.newCall(buildRequest(userId)).execute()
                var responseBody = response.body?.string()

                if (response.isSuccessful) {
                    val bodyForUi = responseBody
                    runOnUiThread {
                        try {
                            val created = Gson().fromJson(bodyForUi, BoardItem::class.java)
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
                    }
                    return@Thread
                }

                // 실패 시 에러 메시지 저장
                val firstCode = response.code
                val firstError = responseBody
                val requireClubField = try {
                    val parser = com.google.gson.JsonParser()
                    val elem = parser.parse(firstError ?: "{}")
                    elem != null && elem.isJsonObject && elem.asJsonObject.has("club")
                } catch (_: Exception) { false }

                // 2차 시도(백업): author에 ClubMember.pk 전송
                val memberPk = fetchMyClubMemberPk(client, baseUrl, clubPk, userId)
                if (memberPk != null) {
                    android.util.Log.d("CreateBoard", "Retry with author(member_pk)=$memberPk includeClub=$requireClubField")
                    response.close()
                    response = client.newCall(buildRequest(memberPk, includeClub = requireClubField)).execute()
                    responseBody = response.body?.string()

                    if (response.isSuccessful) {
                        val bodyForUi = responseBody
                        runOnUiThread {
                            try {
                                val created = Gson().fromJson(bodyForUi, BoardItem::class.java)
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
                        }
                        return@Thread
                    }
                }

                // 모두 실패한 경우 UI에 에러 표시
                val errCode = response.code
                val errBody = responseBody
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "등록 실패: ${firstCode} - ${firstError ?: ""} / 재시도: ${errCode} - ${errBody ?: ""}",
                        Toast.LENGTH_LONG
                    ).show()
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
        return UserManager.getUserPk(this) ?: -1
    }
}
