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
import com.example.myapplication.api.ApiService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException

class ClubAnnouncementBoardListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var boardAdapter: BoardAdapter
    private val boardList = mutableListOf<BoardItem>()

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

        // 멤버 버튼: 멤버 리스트 화면으로 이동
        findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_member)?.setOnClickListener {
            val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val userPk = UserManager.getUserPk(this) ?: -1
            val intent = Intent(this, ClubMemberManagementActivity::class.java)
            intent.putExtra(ClubMemberManagementActivity.EXTRA_CLUB_PK, clubPk)
            intent.putExtra(ClubMemberManagementActivity.EXTRA_USER_PK, userPk)
            startActivity(intent)
        }

        // 설정 버튼: 동아리 정보 수정 화면으로 이동
        findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_settings)?.setOnClickListener {
            val updateIntent = Intent(this, ClubUpdateActivity::class.java)
            // 현재 액티비티의 intent에서 club_pk를 가져와 전달
            val currentClubPk = this.intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            updateIntent.putExtra("club_pk", currentClubPk)
            startActivity(updateIntent)
        }

        // RecyclerView 설정
        recyclerView = findViewById(R.id.rv_board_list)
        recyclerView.layoutManager = LinearLayoutManager(this)

        boardAdapter = BoardAdapter(boardList) { boardItem ->
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

        // 공개장부 버튼
        findViewById<TextView>(R.id.btn_public_account).setOnClickListener {
            val currentClubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            // MainActivity로 이동하여 LedgerContentFragment 표시 (root_page와 동일한 과정)
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("show_public_ledger", true)
            intent.putExtra("club_pk", currentClubPk)
            // ledger_pk는 MainActivity에서 동아리 ID를 기반으로 조회하도록 수정
            startActivity(intent)
            finish()
        }

        // 모임통장 버튼
        findViewById<TextView>(R.id.btn_meeting_account).setOnClickListener {
            val currentClubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            // MainActivity로 이동하여 MeetingAccountFragment 표시 (root_page와 동일한 과정)
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("show_meeting_account", true)
            intent.putExtra("club_pk", currentClubPk)
            startActivity(intent)
            finish()
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

        client.newCall(buildRequest(primaryUrl)).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("API_ERROR", "네트워크 오류: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@ClubAnnouncementBoardListActivity,
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
                                Toast.makeText(this@ClubAnnouncementBoardListActivity,
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

    // helper to request similar via OkHttp fallback (클래스 내부 메서드)
    private fun requestSimilarFallback(selectedId: Int, listContainer: android.widget.LinearLayout, showLoading: (Boolean) -> Unit, api: ApiService) {
        val client = ApiClient.createUnsafeOkHttpClient()
        val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
        val url = "$baseUrl/club/$selectedId/similar/"
        android.util.Log.d("AI_PERSONAL", "폴백 요청 URL: $url")
        val req = okhttp3.Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .build()
        client.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this@ClubAnnouncementBoardListActivity, "추천 요청 실패: ${e.message ?: "네트워크 오류"}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use { resp ->
                    val body = resp.body?.string()
                    android.util.Log.d("AI_PERSONAL", "폴백 응답 코드=${resp.code} body=${body?.take(300)}")
                    fun parseIdsFromJson(json: String): kotlin.collections.Set<Int> {
                        return try {
                            val element = com.google.gson.JsonParser().parse(json)
                            fun extractFromArray(arr: com.google.gson.JsonArray): kotlin.collections.MutableSet<Int> {
                                val out = mutableSetOf<Int>()
                                var i = 0
                                while (i < arr.size()) {
                                    val el = arr.get(i)
                                    try {
                                        if (el.isJsonPrimitive) {
                                            val prim = el.asJsonPrimitive
                                            if (prim.isNumber) out.add(prim.asInt)
                                            else if (prim.isString) prim.asString.toIntOrNull()?.let { out.add(it) }
                                        } else if (el.isJsonObject) {
                                            val obj = el.asJsonObject
                                            val idVal = when {
                                                obj.has("id") -> obj.get("id")
                                                obj.has("club_id") -> obj.get("club_id")
                                                obj.has("pk") -> obj.get("pk")
                                                else -> null
                                            }
                                            if (idVal != null) {
                                                if (idVal.isJsonPrimitive && idVal.asJsonPrimitive.isNumber) out.add(idVal.asInt)
                                                else if (idVal.isJsonPrimitive && idVal.asJsonPrimitive.isString) idVal.asString.toIntOrNull()?.let { out.add(it) }
                                            }
                                        }
                                    } catch (_: Exception) {}
                                    i++
                                }
                                return out
                            }
                            if (element.isJsonArray) {
                                extractFromArray(element.asJsonArray)
                            } else if (element.isJsonObject) {
                                val obj = element.asJsonObject
                                val key = listOf("results", "items", "data").firstOrNull { k -> obj.has(k) && obj.get(k).isJsonArray }
                                if (key != null) extractFromArray(obj.getAsJsonArray(key)) else {
                                    val idVal = when {
                                        obj.has("id") -> obj.get("id")
                                        obj.has("club_id") -> obj.get("club_id")
                                        obj.has("pk") -> obj.get("pk")
                                        else -> null
                                    }
                                    val set = mutableSetOf<Int>()
                                    if (idVal != null) {
                                        if (idVal.isJsonPrimitive && idVal.asJsonPrimitive.isNumber) set.add(idVal.asInt)
                                        else if (idVal.isJsonPrimitive && idVal.asJsonPrimitive.isString) idVal.asString.toIntOrNull()?.let { set.add(it) }
                                    }
                                    set
                                }
                            } else emptySet()
                        } catch (_: Exception) { emptySet() }
                    }
                    val ids: kotlin.collections.Set<Int> = try {
                        if (!resp.isSuccessful || body == null) emptySet() else parseIdsFromJson(body)
                    } catch (_: Exception) { emptySet() }
                    runOnUiThread {
                        if (ids.isEmpty()) {
                            showLoading(false)
                            listContainer.removeAllViews()
                            val empty = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                text = "추천 결과가 없습니다."
                                setTextColor(android.graphics.Color.parseColor("#666666"))
                                textSize = 16f
                                gravity = android.view.Gravity.CENTER
                                setPadding(0, 100, 0, 100)
                            }
                            listContainer.addView(empty)
                        } else {
                            api.getClubList().enqueue(object : retrofit2.Callback<kotlin.collections.List<ClubItem>> {
                                override fun onResponse(
                                    call: retrofit2.Call<kotlin.collections.List<ClubItem>>,
                                    response2: retrofit2.Response<kotlin.collections.List<ClubItem>>
                                ) {
                                    val all2 = response2.body() ?: emptyList()
                                    val matched = all2.filter { ids.contains(it.id) }
                                    listContainer.removeAllViews()
                                    matched.forEach { club ->
                                        val card = android.widget.LinearLayout(this@ClubAnnouncementBoardListActivity).apply {
                                            orientation = android.widget.LinearLayout.VERTICAL
                                            setBackgroundResource(R.drawable.card_box_fafa)
                                            setPadding(40, 40, 40, 40)
                                            val tvName = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                                text = club.name
                                                setTextColor(android.graphics.Color.BLACK)
                                                textSize = 18f
                                                setTypeface(null, android.graphics.Typeface.BOLD)
                                            }
                                            val tvDept = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                                text = "${club.department} / ${club.location}"
                                                setTextColor(android.graphics.Color.parseColor("#666666"))
                                                textSize = 12f
                                            }
                                            val tvDesc = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                                text = club.shortDescription
                                                setTextColor(android.graphics.Color.parseColor("#333333"))
                                                textSize = 12f
                                            }
                                            addView(tvName)
                                            addView(tvDept)
                                            addView(tvDesc)
                                            setOnClickListener {
                                                val intent = Intent(this@ClubAnnouncementBoardListActivity, ClubAnnouncementBoardListActivity::class.java)
                                                intent.putExtra("club_pk", club.id)
                                                startActivity(intent)
                                            }
                                        }
                                        val lp = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                                        lp.setMargins(4, 4, 4, 16)
                                        card.layoutParams = lp
                                        listContainer.addView(card)
                                    }
                                    showLoading(false)
                                }
                                override fun onFailure(call: retrofit2.Call<kotlin.collections.List<ClubItem>>, t: Throwable) {
                                    showLoading(false)
                                    Toast.makeText(this@ClubAnnouncementBoardListActivity, "클럽 목록 요청 실패", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    }
                }
            }
        })
    }
}
