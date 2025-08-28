//package com.example.myapplication
//
//import android.content.Intent
//import android.os.Bundle
//import android.widget.Button
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.appcompat.widget.AppCompatImageButton
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.example.myapplication.api.ApiClient
//import com.google.gson.Gson
//import com.google.gson.reflect.TypeToken
//import okhttp3.*
//import java.io.IOException
//
//class ClubAnnouncementBoardListActivity : AppCompatActivity() {
//
//    private lateinit var recyclerView: RecyclerView
//    private lateinit var boardAdapter: BoardAdapter
//    private val boardList = mutableListOf<BoardItem>()
//    private var clubPk: Int = -1
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_club_announcement_board_list)
//
//        // club_pk 받기
//        clubPk = intent.getIntExtra("club_pk", -1)
//
//        // root_page의 뒤로가기 버튼 표시 및 설정
//        val rootBackButton = findViewById<Button>(R.id.btn_back_root)
//        rootBackButton?.visibility = android.view.View.VISIBLE
//        rootBackButton?.setOnClickListener {
//            finish()
//        }
//
//        // 기존 뒤로가기 버튼은 숨기기
//        findViewById<Button>(R.id.btn_back)?.visibility = android.view.View.GONE
//
//        // 멤버 관리 버튼 설정
//        findViewById<AppCompatImageButton>(R.id.btn_member)?.setOnClickListener {
//            if (clubPk > 0) {
//                val intent = Intent(this, ClubMemberManagementActivity::class.java)
//                intent.putExtra(ClubMemberManagementActivity.EXTRA_CLUB_PK, clubPk)
//                intent.putExtra(ClubMemberManagementActivity.EXTRA_USER_PK, UserManager.getUserPk(this) ?: -1)
//                startActivity(intent)
//            }
//        }
//
//        // 게시판 버튼들 설정
//        setupBoardButtons()
//
//        // RecyclerView 설정
//        recyclerView = findViewById(R.id.rv_board_list)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//
//        boardAdapter = BoardAdapter(boardList) { boardItem ->
//            // 아이템 클릭 시 상세 페이지로 이동
//            val intent = Intent(this, ClubAnnouncementBoardDetailActivity::class.java)
//            intent.putExtra("board_item", boardItem)
//            intent.putExtra("club_pk", clubPk)
//            startActivity(intent)
//        }
//
//        recyclerView.adapter = boardAdapter
//
//        // API 호출
//        if (clubPk > 0) {
//            fetchClubDetail(clubPk)
//            fetchBoardList(clubPk)
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        if (clubPk > 0) {
//            fetchBoardList(clubPk)
//        }
//    }
//
//    private fun setupBoardButtons() {
//        // 공지사항 버튼 (현재 활성화된 상태)
//        findViewById<TextView>(R.id.tv_announcement).setOnClickListener {
//            // 이미 공지사항 페이지에 있으므로 아무것도 하지 않음
//        }
//
//        // 자유게시판 버튼
//        findViewById<TextView>(R.id.tv_free_board).setOnClickListener {
//            val intent = Intent(this, MainActivity::class.java)
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//            intent.putExtra("show_free_board", true)
//            intent.putExtra("club_pk", clubPk)
//            startActivity(intent)
//        }
//
//        // 공개장부 버튼
//        findViewById<TextView>(R.id.tv_public_ledger).setOnClickListener {
//            val intent = Intent(this, MainActivity::class.java)
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//            intent.putExtra("show_public_ledger", true)
//            intent.putExtra("club_pk", clubPk)
//            startActivity(intent)
//        }
//
//        // 행사장부 버튼
//        findViewById<TextView>(R.id.tv_event_ledger).setOnClickListener {
//            val intent = Intent(this, MainActivity::class.java)
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//            intent.putExtra("show_event_account", true)
//            intent.putExtra("club_pk", clubPk)
//            startActivity(intent)
//        }
//
//        // 모임통장 버튼
//        findViewById<TextView>(R.id.tv_meeting_account).setOnClickListener {
//            val intent = Intent(this, MainActivity::class.java)
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//            intent.putExtra("show_meeting_account", true)
//            intent.putExtra("club_pk", clubPk)
//            startActivity(intent)
//        }
//
//        // AI 리포트 버튼
//        findViewById<TextView>(R.id.tv_ai_report).setOnClickListener {
//            val intent = Intent(this, MainActivity::class.java)
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//            intent.putExtra("show_ai_report", true)
//            intent.putExtra("club_pk", clubPk)
//            startActivity(intent)
//        }
//    }
//
//    private fun fetchBoardList(clubPk: Int) {
//        if (clubPk <= 0) {
//            android.util.Log.e("API_ERROR", "유효하지 않은 club_pk: $clubPk")
//            Toast.makeText(this, "동아리 정보를 확인할 수 없습니다.", Toast.LENGTH_LONG).show()
//            return
//        }
//        val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
//        val primaryUrl = "$baseUrl/club/$clubPk/boards"
//        val fallbackUrl = "$baseUrl/club/$clubPk/boards/"
//        android.util.Log.d("API_REQUEST", "요청 URL(우선): $primaryUrl")
//
//        // HTTPS(자가서명 등) 환경에서도 동작하도록 개발용 클라이언트 사용
//        val client = ApiClient.createUnsafeOkHttpClient()
//        fun buildRequest(targetUrl: String): Request =
//            Request.Builder()
//                .url(targetUrl)
//                .get()
//                .addHeader("Accept", "application/json")
//                .build()
//
//        fun handleResponse(response: Response, usedUrl: String) {
//            val responseBody = response.body?.string()
//            android.util.Log.d("API_RESPONSE", "응답 코드: ${response.code} (URL: $usedUrl)")
//            android.util.Log.d("API_RESPONSE", "응답 본문: $responseBody")
//            runOnUiThread {
//                if (response.isSuccessful && responseBody != null) {
//                    try {
//                        val gson = Gson()
//                        val type = object : TypeToken<List<BoardItem>>() {}.type
//                        val boards = gson.fromJson<List<BoardItem>>(responseBody, type)
//                        android.util.Log.d("API_SUCCESS", "파싱된 게시글 수: ${boards.size}")
//                        val announcementBoards = boards.filter { it.type == "announcement" && it.club == clubPk }
//                        val sorted = announcementBoards.sortedByDescending { parseBoardDate(it.updated_at ?: it.created_at) }
//                        android.util.Log.d("API_SUCCESS", "공지사항 게시글 수: ${sorted.size}")
//                        boardList.clear()
//                        boardList.addAll(sorted)
//                        boardAdapter.notifyDataSetChanged()
//                    } catch (e: Exception) {
//                        android.util.Log.e("API_ERROR", "데이터 파싱 오류: ${e.message}")
//                        Toast.makeText(this,
//                            "데이터 파싱 오류: ${e.message}", Toast.LENGTH_LONG).show()
//                    }
//                } else {
//                    android.util.Log.e("API_ERROR", "서버 오류: ${response.code} - $responseBody")
//                    Toast.makeText(this,
//                        "서버 오류: ${response.code} - ${responseBody ?: "응답 없음"}", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//
//        client.newCall(buildRequest(primaryUrl)).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                android.util.Log.e("API_ERROR", "네트워크 오류: ${e.message}")
//                runOnUiThread {
//                    Toast.makeText(this@ClubAnnouncementBoardListActivity,
//                        "네트워크 오류: ${e.message}", Toast.LENGTH_LONG).show()
//                }
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                if (!response.isSuccessful && response.code == 400) {
//                    android.util.Log.w("API_RETRY", "400 발생. 대체 URL로 재시도: $fallbackUrl")
//                    client.newCall(buildRequest(fallbackUrl)).enqueue(object : Callback {
//                        override fun onFailure(call: Call, e: IOException) {
//                            android.util.Log.e("API_ERROR", "재시도 네트워크 오류: ${e.message}")
//                            runOnUiThread {
//                                Toast.makeText(this@ClubAnnouncementBoardListActivity,
//                                    "재시도 네트워크 오류: ${e.message}", Toast.LENGTH_LONG).show()
//                            }
//                        }
//                        override fun onResponse(call: Call, retryResponse: Response) {
//                            handleResponse(retryResponse, fallbackUrl)
//                        }
//                    })
//                } else {
//                    handleResponse(response, primaryUrl)
//                }
//            }
//        })
//    }
//
//    private fun parseBoardDate(dateString: String?): Long {
//        if (dateString.isNullOrBlank()) return 0L
//        return try {
//            val instant = try {
//                java.time.OffsetDateTime.parse(
//                    dateString,
//                    java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
//                ).toInstant()
//            } catch (_: Exception) {
//                val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
//                java.time.LocalDateTime.parse(dateString.substring(0, 26), fmt)
//                    .atZone(java.time.ZoneOffset.UTC)
//                    .toInstant()
//            }
//            instant.toEpochMilli()
//        } catch (_: Exception) {
//            try {
//                val input = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", java.util.Locale.getDefault())
//                input.parse(dateString)?.time ?: 0L
//            } catch (_: Exception) { 0L }
//        }
//    }
//
//    private fun fetchClubDetail(clubPk: Int) {
//        if (clubPk <= 0) {
//            return
//        }
//        val api = ApiClient.getApiService()
//        api.getClubDetail(clubPk).enqueue(object : retrofit2.Callback<ClubItem> {
//            override fun onResponse(
//                call: retrofit2.Call<ClubItem>,
//                response: retrofit2.Response<ClubItem>
//            ) {
//                val club = response.body()
//                if (response.isSuccessful && club != null) {
//                    bindClubHeader(club)
//                }
//            }
//            override fun onFailure(
//                call: retrofit2.Call<ClubItem>,
//                t: Throwable
//            ) { /* 무시: 헤더만 미표시 */ }
//        })
//    }
//
//    private fun bindClubHeader(club: ClubItem) {
//        findViewById<TextView>(R.id.tv_club_title)?.text = club.name
//        findViewById<TextView>(R.id.tv_welcome)?.text = "🎇 Welcome"
//        // Welcome 아래 설명은 short_description으로 표시
//        findViewById<TextView>(R.id.tv_club_description)?.text = club.shortDescription
//
//        // 커버 이미지 설정 (API에서 받아온 이미지가 있으면 사용, 없으면 기본 이미지)
//        val coverImageView = findViewById<android.widget.ImageView>(R.id.iv_club_cover)
//        coverImageView?.let { imageView ->
//            if (club.coverImage != null && club.coverImage.isNotEmpty()) {
//                // Glide나 Picasso를 사용하여 이미지 로드
//                // Glide.with(this).load(club.coverImage).into(imageView)
//                // 또는 기본 이미지 유지
//            } else {
//                // 기본 이미지 설정 (더 적절한 이미지로 변경)
//                imageView.setImageResource(R.drawable.club_cover_img)
//            }
//        }
//    }
//}
