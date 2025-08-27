package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.api.ApiClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException

class ClubForumBoardListActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var boardAdapter: BoardAdapter
    private val boardList = mutableListOf<BoardItem>()
    
    companion object {
        private const val EXTRA_CLUB_PK = "club_pk"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_forum_board_list)
        
        // 뒤로가기 버튼 설정
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        
        // Floating Action Button 설정
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add_post).setOnClickListener {
            val currentClubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val intent = Intent(this, ClubForumBoardCreateActivity::class.java)
            intent.putExtra(EXTRA_CLUB_PK, currentClubPk)
            startActivity(intent)
        }
        
        // 게시판 버튼 설정
        setupBoardButtons()
        
        // RecyclerView 설정
        recyclerView = findViewById(R.id.rv_board_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        boardAdapter = BoardAdapter(boardList) { boardItem ->
            // 아이템 클릭 시 상세 페이지로 이동
            val intent = Intent(this, ClubForumBoardDetailActivity::class.java)
            intent.putExtra("board_item", boardItem)
            val currentClubPk = this.intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            intent.putExtra("club_pk", currentClubPk)
            startActivity(intent)
        }
        
        recyclerView.adapter = boardAdapter
        
        // API 호출
        val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
        fetchClubDetail(clubPk)
        fetchBoardList(clubPk)
    }

    override fun onResume() {
        super.onResume()
        val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
        fetchBoardList(clubPk)
    }
    
    private fun setupBoardButtons() {
        // 공지사항 버튼
        findViewById<TextView>(R.id.btn_notice).setOnClickListener {
            val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val intent = Intent(this, ClubAnnouncementBoardListActivity::class.java)
            intent.putExtra(EXTRA_CLUB_PK, clubPk)
            startActivity(intent)
            finish()
        }
        
        // 자유게시판 버튼 (현재 화면이므로 아무것도 하지 않음)
        findViewById<TextView>(R.id.btn_free_board).setOnClickListener {
            // 이미 자유게시판 화면이므로 아무것도 하지 않음
        }
        
        // AI 리포트 버튼
        findViewById<TextView>(R.id.btn_ai_report).setOnClickListener {
            val currentClubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val intent = Intent(this, LedgerReportActivity::class.java)
            intent.putExtra("club_id", currentClubPk)
            startActivity(intent)
            finish()
        }
    }
    
    private fun fetchBoardList(clubPk: Int) {
        val resolvedClubPk = if (clubPk > 0) clubPk else 1
        val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
        val primaryUrl = "$baseUrl/club/$resolvedClubPk/boards"
        val fallbackUrl = "$baseUrl/club/$resolvedClubPk/boards/"
        android.util.Log.d("API_REQUEST", "요청 URL(우선): $primaryUrl")
        
        // HTTPS(자가서명 등) 환경에서도 동작하도록 개발용 클라이언트 사용
        val client = ApiClient.createUnsafeOkHttpClient()
        fun buildRequest(targetUrl: String): Request =
            Request.Builder()
                .url(targetUrl)
                .get()
                .addHeader("Accept", "application/json")
                .build()

        fun handleResponse(response: Response, usedUrl: String) {
            val responseBody = response.body?.string()
            android.util.Log.d("API_RESPONSE", "응답 코드: ${response.code} (URL: $usedUrl)")
            android.util.Log.d("API_RESPONSE", "응답 본문: $responseBody")
            runOnUiThread {
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val gson = Gson()
                        val type = object : TypeToken<List<BoardItem>>() {}.type
                        val boards = gson.fromJson<List<BoardItem>>(responseBody, type)
                        android.util.Log.d("API_SUCCESS", "파싱된 게시글 수: ${boards.size}")
                        val freeBoardBoards = boards.filter { it.type == "forum" && it.club == clubPk }
                        val sorted = freeBoardBoards.sortedByDescending { parseBoardDate(it.updated_at ?: it.created_at) }
                        android.util.Log.d("API_SUCCESS", "자유게시판 게시글 수: ${sorted.size}")
                        boardList.clear()
                        boardList.addAll(sorted)
                        boardAdapter.notifyDataSetChanged()
                    } catch (e: Exception) {
                        android.util.Log.e("API_ERROR", "데이터 파싱 오류: ${e.message}")
                        Toast.makeText(this@ClubForumBoardListActivity, 
                            "데이터 파싱 오류: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    android.util.Log.e("API_ERROR", "서버 오류: ${response.code} - $responseBody")
                    Toast.makeText(this@ClubForumBoardListActivity, 
                        "서버 오류: ${response.code} - ${responseBody ?: "응답 없음"}", Toast.LENGTH_LONG).show()
                }
            }
        }

        client.newCall(buildRequest(primaryUrl)).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("API_ERROR", "네트워크 오류: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@ClubForumBoardListActivity, 
                        "네트워크 오류: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful && response.code == 400) {
                    android.util.Log.w("API_RETRY", "400 발생. 대체 URL로 재시도: $fallbackUrl")
                    client.newCall(buildRequest(fallbackUrl)).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            android.util.Log.e("API_ERROR", "재시도 네트워크 오류: ${e.message}")
                            runOnUiThread {
                                Toast.makeText(this@ClubForumBoardListActivity,
                                    "재시도 네트워크 오류: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        override fun onResponse(call: Call, retryResponse: Response) {
                            handleResponse(retryResponse, fallbackUrl)
                        }
                    })
                } else {
                    handleResponse(response, primaryUrl)
                }
            }
        })
    }

    private fun parseBoardDate(dateString: String?): Long {
        if (dateString.isNullOrBlank()) return 0L
        return try {
            // 우선 ISO_OFFSET_DATE_TIME 시도 (예: 2025-08-20T10:25:00.000000+09:00)
            val instant = try {
                java.time.OffsetDateTime.parse(
                    dateString,
                    java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
                ).toInstant()
            } catch (_: Exception) {
                // 백업: 마이크로초 포함 패턴
                val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
                java.time.LocalDateTime.parse(dateString.substring(0, 26), fmt)
                    .atZone(java.time.ZoneOffset.UTC)
                    .toInstant()
            }
            instant.toEpochMilli()
        } catch (_: Exception) {
            try {
                val input = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", java.util.Locale.getDefault())
                input.parse(dateString)?.time ?: 0L
            } catch (_: Exception) { 0L }
        }
    }

    private fun fetchClubDetail(clubPk: Int) {
        if (clubPk <= 0) return
        val api = com.example.myapplication.api.ApiClient.getApiService()
        api.getClubDetail(clubPk).enqueue(object : retrofit2.Callback<com.example.myapplication.ClubItem> {
            override fun onResponse(
                call: retrofit2.Call<com.example.myapplication.ClubItem>,
                response: retrofit2.Response<com.example.myapplication.ClubItem>
            ) {
                val club = response.body()
                if (response.isSuccessful && club != null) {
                    findViewById<android.widget.TextView>(R.id.tv_club_title)?.text = club.name
                }
            }
            override fun onFailure(
                call: retrofit2.Call<com.example.myapplication.ClubItem>,
                t: Throwable
            ) { }
        })
    }
}

