package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class ClubForumBoardCreateActivity : AppCompatActivity() {
    companion object {
        private const val EXTRA_CLUB_PK = "club_pk"
    }
    
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

        val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
        if (clubPk <= 0) {
            Toast.makeText(this, "동아리 정보가 없습니다.", Toast.LENGTH_LONG).show()
            return
        }

        val userId = UserManager.getUserPk(this) ?: -1
        if (userId <= 0) {
            Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_LONG).show()
            return
        }

        val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
        val url = "$baseUrl/club/$clubPk/boards/"
        val client = com.example.myapplication.api.ApiClient.createUnsafeOkHttpClient()

        fun buildRequest(authorValue: Int, includeClub: Boolean = false): okhttp3.Request {
            val payload = mapOf(
                "author" to authorValue,
                "type" to "forum",
                "title" to title,
                "content" to content
            )
            val finalPayload = if (includeClub) {
                val m = java.util.LinkedHashMap(payload)
                m["club"] = clubPk
                m
            } else payload
            val json = com.google.gson.Gson().toJson(finalPayload)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body: okhttp3.RequestBody = json.toRequestBody(mediaType)
            return okhttp3.Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()
        }

        Thread {
            try {
                android.util.Log.d("CreateForum", "POST /club/$clubPk/boards author(user_pk)=$userId")
                var response = client.newCall(buildRequest(userId)).execute()
                var responseBody = response.body?.string()

                if (response.isSuccessful) {
                    val bodyForUi = responseBody
                    runOnUiThread {
                        try {
                            val created = com.google.gson.Gson().fromJson(bodyForUi, BoardItem::class.java)
                            Toast.makeText(this, "게시글이 작성되었습니다.", Toast.LENGTH_SHORT).show()
                            val intent = android.content.Intent(this, ClubForumBoardDetailActivity::class.java)
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

                val firstCode = response.code
                val firstError = responseBody
                val requireClubField = try {
                    val parser = com.google.gson.JsonParser()
                    val elem = parser.parse(firstError ?: "{}")
                    elem != null && elem.isJsonObject && elem.asJsonObject.has("club")
                } catch (_: Exception) { false }

                // 백업: ClubMember.pk 조회 후 재시도
                val memberPk = fetchMyClubMemberPk(client, baseUrl, clubPk, userId)
                if (memberPk != null) {
                    android.util.Log.d("CreateForum", "Retry with author(member_pk)=$memberPk includeClub=$requireClubField")
                    response.close()
                    response = client.newCall(buildRequest(memberPk, includeClub = requireClubField)).execute()
                    responseBody = response.body?.string()

                    if (response.isSuccessful) {
                        val bodyForUi = responseBody
                        runOnUiThread {
                            try {
                                val created = com.google.gson.Gson().fromJson(bodyForUi, BoardItem::class.java)
                                Toast.makeText(this, "게시글이 작성되었습니다.", Toast.LENGTH_SHORT).show()
                                val intent = android.content.Intent(this, ClubForumBoardDetailActivity::class.java)
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

                val errCode = response.code
                val errBody = responseBody
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "등록 실패: ${firstCode} - ${firstError ?: ""} / 재시도: ${errCode} - ${errBody ?: ""}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: java.io.IOException) {
                runOnUiThread {
                    Toast.makeText(this, "네트워크 오류: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun fetchMyClubMemberPk(client: okhttp3.OkHttpClient, baseUrl: String, clubPk: Int, userId: Int): Int? {
        val listUrl = "$baseUrl/club/$clubPk/members/"
        val req = okhttp3.Request.Builder().url(listUrl).get().build()
        return try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val type = object : com.google.gson.reflect.TypeToken<List<ClubAnnouncementBoardCreateActivity.ClubMemberBrief>>() {}.type
                val items: List<ClubAnnouncementBoardCreateActivity.ClubMemberBrief> = com.google.gson.Gson().fromJson(body, type)
                items.firstOrNull { it.user == userId }?.id
            }
        } catch (_: Exception) { null }
    }
}

