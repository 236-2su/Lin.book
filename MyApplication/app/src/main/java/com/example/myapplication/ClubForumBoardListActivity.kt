package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException

class ClubForumBoardListActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var boardAdapter: BoardAdapter
    private val boardList = mutableListOf<BoardItem>()
    
    companion object {
        private const val BASE_URL = "http://10.0.2.2:8000" // Android 에뮬레이터에서 로컬 서버 접근
        private const val CLUB_PK = 1 // 임의로 설정한 club_pk를 1로 변경
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_forum_board_list)
        
        // 뒤로가기 버튼 설정
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        // Floating Action Button 설정
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add_post).setOnClickListener {
            val intent = Intent(this, ClubForumBoardCreateActivity::class.java)
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
            startActivity(intent)
        }
        
        recyclerView.adapter = boardAdapter
        
        // API 호출
        fetchBoardList()
    }
    
    private fun setupBoardButtons() {
        // 공지사항 버튼
        findViewById<TextView>(R.id.btn_notice).setOnClickListener {
            val intent = Intent(this, ClubAnnouncementBoardListActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        // 자유게시판 버튼 (현재 화면이므로 아무것도 하지 않음)
        findViewById<TextView>(R.id.btn_free_board).setOnClickListener {
            // 이미 자유게시판 화면이므로 아무것도 하지 않음
        }
    }
    
    private fun fetchBoardList() {
        val primaryUrl = "$BASE_URL/club/$CLUB_PK/boards"
        val fallbackUrl = "$BASE_URL/club/$CLUB_PK/boards/"
        android.util.Log.d("API_REQUEST", "요청 URL(우선): $primaryUrl")
        
        val client = OkHttpClient()
        fun buildRequest(targetUrl: String): Request =
            Request.Builder()
                .url(targetUrl)
                .get()
                .addHeader("Accept", "application/json")
                .build()

        fun handleResponse(response: Response, usedUrl: String) {
            val responseBody = response.body?.string()
            android.util.Log.d("API_RESPONSE", "응답 코드: ${'$'}{response.code} (URL: ${'$'}usedUrl)")
            android.util.Log.d("API_RESPONSE", "응답 본문: ${'$'}responseBody")
            runOnUiThread {
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val gson = Gson()
                        val type = object : TypeToken<List<BoardItem>>() {}.type
                        val boards = gson.fromJson<List<BoardItem>>(responseBody, type)
                        android.util.Log.d("API_SUCCESS", "파싱된 게시글 수: ${'$'}{boards.size}")
                        val freeBoardBoards = boards.filter { it.type == "free_board" }
                        android.util.Log.d("API_SUCCESS", "자유게시판 게시글 수: ${'$'}{freeBoardBoards.size}")
                        boardList.clear()
                        boardList.addAll(freeBoardBoards)
                        boardAdapter.notifyDataSetChanged()
                    } catch (e: Exception) {
                        android.util.Log.e("API_ERROR", "데이터 파싱 오류: ${'$'}{e.message}")
                        Toast.makeText(this@ClubForumBoardListActivity, 
                            "데이터 파싱 오류: ${'$'}{e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    android.util.Log.e("API_ERROR", "서버 오류: ${'$'}{response.code} - ${'$'}responseBody")
                    Toast.makeText(this@ClubForumBoardListActivity, 
                        "서버 오류: ${'$'}{response.code} - ${'$'}{responseBody ?: "응답 없음"}", Toast.LENGTH_LONG).show()
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
                            android.util.Log.e("API_ERROR", "재시도 네트워크 오류: ${'$'}{e.message}")
                            runOnUiThread {
                                Toast.makeText(this@ClubForumBoardListActivity,
                                    "재시도 네트워크 오류: ${'$'}{e.message}", Toast.LENGTH_LONG).show()
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
}
