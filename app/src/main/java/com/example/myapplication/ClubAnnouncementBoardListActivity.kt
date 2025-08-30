package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.ApiService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException

class ClubAnnouncementBoardListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var boardAdapter: BoardAdapter
    private val boardList = mutableListOf<BoardItem>()
    private var isMember: Boolean = false
    private var isOfficerOrLeader: Boolean = false

    companion object {
        private const val EXTRA_CLUB_PK = "club_pk"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_announcement_board_list)

        // 뒤로가기 버튼 설정: 시스템 백스택으로 이전 페이지 이동
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 게시판 버튼 설정
        setupBoardButtons()

        // Floating Action Button 설정
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add_post).setOnClickListener {
            val currentClubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val intent = Intent(this, ClubAnnouncementBoardCreateActivity::class.java)
            intent.putExtra(EXTRA_CLUB_PK, currentClubPk)
            startActivity(intent)
        }

        // 공유하기 버튼: 동아리 URL을 클립보드에 복사하고 토스트 안내
        findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_share)?.setOnClickListener {
            val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            if (clubPk > 0) {
                val url = BuildConfig.BASE_URL.trimEnd('/') + "/club/" + clubPk + "/"
                val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("club_url", url)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(this, "동아리 URL이 저장되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        // 비회원 포함 항상 공유 버튼은 보이도록 강제
        findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_share)?.visibility = android.view.View.VISIBLE

        // 헤더 메뉴(햄버거): 구성원 관리 / 회비 관리
        findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_menu)?.setOnClickListener { v ->
            val popup = android.widget.PopupMenu(this, v)
            val membersLabel = if (isOfficerOrLeader) "구성원 관리" else "구성원 조회"
            val feeLabel = if (isOfficerOrLeader) "회비 관리" else "회비 조회"
            popup.menu.add(0, 1, 0, membersLabel)
            popup.menu.add(0, 2, 1, feeLabel)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
                        val userPk = UserManager.getUserPk(this) ?: -1
                        val intent = Intent(this, ClubMemberManagementActivity::class.java)
                        intent.putExtra(ClubMemberManagementActivity.EXTRA_CLUB_PK, clubPk)
                        intent.putExtra(ClubMemberManagementActivity.EXTRA_USER_PK, userPk)
                        startActivity(intent)
                        true
                    }
                    2 -> {
                        val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
                        val intent = Intent(this, ClubFeeManagementActivity::class.java)
                        intent.putExtra(ClubFeeManagementActivity.EXTRA_CLUB_PK, clubPk)
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        // 설정 버튼: 동아리 정보 수정 화면으로 이동
        findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_settings)?.setOnClickListener {
            val updateIntent = Intent(this, ClubUpdateActivity::class.java)
            // 현재 액티비티의 intent에서 club_pk를 가져와 전달
            val currentClubPk = this.intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            updateIntent.putExtra("club_pk", currentClubPk)
            startActivity(updateIntent)
        }
        // 설정 버튼은 기본 숨김(운영진만 노출)
        findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_settings)?.visibility = android.view.View.GONE

        // RecyclerView 설정
        recyclerView = findViewById(R.id.rv_board_list)
        recyclerView.layoutManager = LinearLayoutManager(this)

        boardAdapter = BoardAdapter(boardList) { boardItem ->
            // 비회원은 상세 진입 차단 및 토스트 안내
            if (!isMember) {
                Toast.makeText(this, "동아리원만 조회 가능합니다", Toast.LENGTH_SHORT).show()
                return@BoardAdapter
            }
            // 아이템 클릭 시 상세 페이지로 이동
            val intent = Intent(this, ClubAnnouncementBoardDetailActivity::class.java)
            intent.putExtra("board_item", boardItem)
            val currentClubPk = this.intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            intent.putExtra("club_pk", currentClubPk)
            startActivity(intent)
        }

        recyclerView.adapter = boardAdapter

        // API 호출
        val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
        // 클럽 기본 정보 로드
        fetchClubDetail(clubPk)
        // 멤버/운영진 여부 확인 후 헤더/버튼/하단 고정 가입 버튼 가시성 적용
        applyMembershipUi(clubPk)
        fetchBoardList(clubPk)
    }

    override fun onResume() {
        super.onResume()
        val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
        fetchBoardList(clubPk)
    }

    private fun setupBoardButtons() {
        // 공지사항 버튼 (현재 화면이므로 아무것도 하지 않음)
        findViewById<TextView>(R.id.btn_notice).setOnClickListener {
            // 이미 공지사항 화면이므로 아무것도 하지 않음
        }

        // 자유게시판 버튼
        findViewById<TextView>(R.id.btn_free_board).setOnClickListener {
            val currentClubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val intent = Intent(this, ClubForumBoardListActivity::class.java)
            intent.putExtra(EXTRA_CLUB_PK, currentClubPk)
            startActivity(intent)
            finish()
        }

        // 행사장부 버튼
        findViewById<TextView>(R.id.btn_event_account).setOnClickListener {
            val currentClubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val intent = Intent(this, ClubEventLedgerListActivity::class.java)
            intent.putExtra(ClubEventLedgerListActivity.EXTRA_CLUB_PK, currentClubPk)
            startActivity(intent)
            finish()
        }

        // AI 리포트 버튼
        findViewById<TextView>(R.id.btn_ai_report).setOnClickListener {
            val currentClubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val intent = Intent(this, LedgerReportActivity::class.java)
            intent.putExtra("club_id", currentClubPk)
            startActivity(intent)
            finish()
        }

        // 모임통장 버튼
        findViewById<TextView>(R.id.btn_meeting_account).setOnClickListener {
            val currentClubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val intent = Intent(this, AccountHistoryActivity::class.java)
            intent.putExtra("club_pk", currentClubPk)
            startActivity(intent)
        }
    }

    private fun fetchBoardList(clubPk: Int) {
        if (clubPk <= 0) {
            android.util.Log.e("API_ERROR", "유효하지 않은 club_pk: $clubPk")
            Toast.makeText(this, "동아리 정보를 확인할 수 없습니다.", Toast.LENGTH_LONG).show()
            return
        }
        val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
        val primaryUrl = "$baseUrl/club/$clubPk/boards"
        val fallbackUrl = "$baseUrl/club/$clubPk/boards/"
        android.util.Log.d("API_REQUEST", "요청 URL(우선): $primaryUrl")

        // HTTPS(자가서명 등) 환경에서도 동작하도록 개발용 클라이언트 사용
        val client = ApiClient.createUnsafeOkHttpClient()
        fun buildRequest(targetUrl: String): Request =
            Request.Builder()
                .url(targetUrl)
                .get()
                .addHeader("Accept", "application/json")
                .build()

        fun handleResponse(response: okhttp3.Response, usedUrl: String) {
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
                        val announcementBoards = boards.filter { it.type == "announcement" && it.club == clubPk }
                        val sorted = announcementBoards.sortedByDescending { parseBoardDate(it.updated_at ?: it.created_at) }
                        android.util.Log.d("API_SUCCESS", "공지사항 게시글 수: ${sorted.size}")
                        boardList.clear()
                        boardList.addAll(sorted)
                        boardAdapter.notifyDataSetChanged()
                    } catch (e: Exception) {
                        android.util.Log.e("API_ERROR", "데이터 파싱 오류: ${e.message}")
                        Toast.makeText(this@ClubAnnouncementBoardListActivity,
                            "데이터 파싱 오류: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    android.util.Log.e("API_ERROR", "서버 오류: ${response.code} - $responseBody")
                    Toast.makeText(this@ClubAnnouncementBoardListActivity,
                        "서버 오류: ${response.code} - ${responseBody ?: "응답 없음"}", Toast.LENGTH_LONG).show()
                }
            }
        }

        client.newCall(buildRequest(primaryUrl)).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                android.util.Log.e("API_ERROR", "네트워크 오류: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@ClubAnnouncementBoardListActivity,
                        "네트워크 오류: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!response.isSuccessful && response.code == 400) {
                    android.util.Log.w("API_RETRY", "400 발생. 대체 URL로 재시도: $fallbackUrl")
                    client.newCall(buildRequest(fallbackUrl)).enqueue(object : okhttp3.Callback {
                        override fun onFailure(call: okhttp3.Call, e: IOException) {
                            android.util.Log.e("API_ERROR", "재시도 네트워크 오류: ${e.message}")
                            runOnUiThread {
                                Toast.makeText(this@ClubAnnouncementBoardListActivity,
                                    "재시도 네트워크 오류: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        override fun onResponse(call: okhttp3.Call, retryResponse: okhttp3.Response) {
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
            val instant = try {
                java.time.OffsetDateTime.parse(
                    dateString,
                    java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
                ).toInstant()
            } catch (_: Exception) {
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
        if (clubPk <= 0) {
            return
        }
        val api = ApiClient.getApiService()
        api.getClubDetail(clubPk).enqueue(object : retrofit2.Callback<com.example.myapplication.ClubItem> {
            override fun onResponse(
                call: retrofit2.Call<com.example.myapplication.ClubItem>,
                response: retrofit2.Response<com.example.myapplication.ClubItem>
            ) {
                if (!this@ClubAnnouncementBoardListActivity.isFinishing && !this@ClubAnnouncementBoardListActivity.isDestroyed) {
                    val club = response.body()
                    if (response.isSuccessful && club != null) {
                        bindClubHeader(club)
                    }
                }
            }
            override fun onFailure(
                call: retrofit2.Call<com.example.myapplication.ClubItem>,
                t: Throwable
            ) { /* 무시: 헤더만 미표시 */ }
        })
    }

    private fun bindClubHeader(club: com.example.myapplication.ClubItem) {
        findViewById<TextView>(R.id.tv_club_title)?.text = club.name
        findViewById<TextView>(R.id.tv_welcome)?.text = "🖐🏻 Welcome"
        // Welcome 아래 설명은 short_description으로 표시
        findViewById<TextView>(R.id.tv_club_description)?.text = club.shortDescription
        // 커버 이미지가 API에 없다면 기본 이미지를 유지
    }

    private fun applyMembershipUi(clubPk: Int) {
        if (clubPk <= 0) return
        val userPk = UserManager.getUserPk(this)
        if (userPk == null) {
            // 비회원 UI 적용
            isMember = false
            findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_menu)?.visibility = android.view.View.GONE
            findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_settings)?.visibility = android.view.View.GONE
            findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add_post)?.visibility = android.view.View.GONE
            findViewById<Button>(R.id.btn_bottom_join)?.visibility = android.view.View.VISIBLE
            // 탭: 공지/자유 외 숨김
            findViewById<TextView>(R.id.btn_public_account)?.visibility = android.view.View.GONE
            findViewById<TextView>(R.id.btn_event_account)?.visibility = android.view.View.GONE
            findViewById<TextView>(R.id.btn_meeting_account)?.visibility = android.view.View.GONE
            findViewById<TextView>(R.id.btn_ai_report)?.visibility = android.view.View.GONE
            return
        }
        val api = ApiClient.getApiService()
        api.getClubMembers(clubPk).enqueue(object : retrofit2.Callback<List<com.example.myapplication.MemberResponse>> {
            override fun onResponse(
                call: retrofit2.Call<List<com.example.myapplication.MemberResponse>>,
                response: retrofit2.Response<List<com.example.myapplication.MemberResponse>>
            ) {
                val members = response.body().orEmpty()
                val mine = members.firstOrNull { it.user == userPk && it.status == "active" }
                val isOfficer = mine?.role == "leader" || mine?.role == "officer"
                isMember = mine != null
                isOfficerOrLeader = isOfficer
                // 햄버거: 회원만 표시
                findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_menu)?.visibility = if (isMember) android.view.View.VISIBLE else android.view.View.GONE
                // 설정: 운영진만 표시
                findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_settings)?.visibility = if (isOfficer) android.view.View.VISIBLE else android.view.View.GONE
                // FAB: 운영진만 표시
                findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add_post)?.visibility = if (isOfficer) android.view.View.VISIBLE else android.view.View.GONE
                // 하단 고정 가입 버튼: 비회원만 표시
                findViewById<Button>(R.id.btn_bottom_join)?.visibility = if (!isMember) android.view.View.VISIBLE else android.view.View.GONE
                // 탭: 회원 아니면 공지/자유 외 숨김
                val extraVisibility = if (isMember) android.view.View.VISIBLE else android.view.View.GONE
                findViewById<TextView>(R.id.btn_public_account)?.visibility = extraVisibility
                findViewById<TextView>(R.id.btn_event_account)?.visibility = extraVisibility
                findViewById<TextView>(R.id.btn_meeting_account)?.visibility = extraVisibility
                findViewById<TextView>(R.id.btn_ai_report)?.visibility = extraVisibility
            }
            override fun onFailure(
                call: retrofit2.Call<List<com.example.myapplication.MemberResponse>>,
                t: Throwable
            ) {
                // 실패 시 비회원 UI 적용
                isMember = false
                isOfficerOrLeader = false
                findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_menu)?.visibility = android.view.View.GONE
                findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_settings)?.visibility = android.view.View.GONE
                findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add_post)?.visibility = android.view.View.GONE
                findViewById<Button>(R.id.btn_bottom_join)?.visibility = android.view.View.VISIBLE
                findViewById<TextView>(R.id.btn_public_account)?.visibility = android.view.View.GONE
                findViewById<TextView>(R.id.btn_event_account)?.visibility = android.view.View.GONE
                findViewById<TextView>(R.id.btn_meeting_account)?.visibility = android.view.View.GONE
                findViewById<TextView>(R.id.btn_ai_report)?.visibility = android.view.View.GONE
            }
        })
    }

}
