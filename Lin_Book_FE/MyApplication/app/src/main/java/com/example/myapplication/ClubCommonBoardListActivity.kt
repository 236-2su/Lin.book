package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException

class ClubCommonBoardListActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var boardAdapter: BoardAdapter
    private val boardList = mutableListOf<BoardItem>()
    
    companion object {
        private const val BASE_URL = "http://10.0.2.2:8000" // Android 에뮬레이터에서 로컬 서버 접근
        private const val CLUB_PK = 1 // 임의로 설정한 club_pk를 1로 변경
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_common_board_list)
        
        // 뒤로가기 버튼 설정
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            val intent = android.content.Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        // Floating Action Button 설정
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add_post).setOnClickListener {
            Toast.makeText(this, "새 게시글 작성", Toast.LENGTH_SHORT).show()
            // TODO: 게시글 작성 페이지로 이동
        }
        
        // RecyclerView 설정
        recyclerView = findViewById(R.id.rv_board_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        boardAdapter = BoardAdapter(boardList) { boardItem ->
            // 아이템 클릭 시 처리 (상세 페이지로 이동 등)
            Toast.makeText(this, "선택된 게시글: ${boardItem.title}", Toast.LENGTH_SHORT).show()
        }
        
        recyclerView.adapter = boardAdapter
        
        // API 호출
        fetchBoardList()
    }
    
    private fun fetchBoardList() {
        val url = "$BASE_URL/club/$CLUB_PK/boards/"
        android.util.Log.d("API_REQUEST", "요청 URL: $url")
        
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Content-Type", "application/json")
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("API_ERROR", "네트워크 오류: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@ClubCommonBoardListActivity, 
                        "네트워크 오류: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                android.util.Log.d("API_RESPONSE", "응답 코드: ${response.code}")
                android.util.Log.d("API_RESPONSE", "응답 본문: $responseBody")
                
                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val gson = Gson()
                            val type = object : TypeToken<List<BoardItem>>() {}.type
                            val boards = gson.fromJson<List<BoardItem>>(responseBody, type)
                            
                            android.util.Log.d("API_SUCCESS", "파싱된 게시글 수: ${boards.size}")
                            
                            // announcement 타입만 필터링
                            val announcementBoards = boards.filter { it.type == "announcement" }
                            
                            android.util.Log.d("API_SUCCESS", "공지사항 게시글 수: ${announcementBoards.size}")
                            
                            boardList.clear()
                            boardList.addAll(announcementBoards)
                            boardAdapter.notifyDataSetChanged()
                            
                        } catch (e: Exception) {
                            android.util.Log.e("API_ERROR", "데이터 파싱 오류: ${e.message}")
                            Toast.makeText(this@ClubCommonBoardListActivity, 
                                "데이터 파싱 오류: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        android.util.Log.e("API_ERROR", "서버 오류: ${response.code} - $responseBody")
                        Toast.makeText(this@ClubCommonBoardListActivity, 
                            "서버 오류: ${response.code} - ${responseBody ?: "응답 없음"}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
