package com.example.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class ClubAnnouncementBoardDetailActivity : AppCompatActivity() {
    
    private lateinit var boardItem: BoardItem
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_announcement_board_detail)
        
        // Intent에서 BoardItem 데이터 받기
        boardItem = intent.getParcelableExtra("board_item") ?: BoardItem(
            1, "announcement", "샘플 게시글", "샘플 내용", 13, "2025-08-20T10:25:00.000000+09:00", 1
        )
        
        // 뒤로가기 버튼 설정: 시스템 백스택으로 이전 페이지 이동
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        
        // 햄버거 메뉴 버튼 설정
        findViewById<ImageView>(R.id.btn_more_options).setOnClickListener { view ->
            showPopupMenu(view)
        }
        
        // UI 업데이트
        updateUI()

        // 댓글 로드
        loadComments()

        // 댓글 전송
        findViewById<android.widget.ImageButton>(R.id.btn_send_comment)?.setOnClickListener {
            sendComment()
        }

        // 좋아요 토글
        findViewById<android.widget.ImageView>(R.id.iv_like)?.setOnClickListener {
            toggleLike()
        }
    }

    private fun fetchAuthorFromUserApi(idFromBoard: Int) {
        val api = com.example.myapplication.api.ApiClient.getApiService()
        // 1차: author를 user_id로 간주하고 조회
        api.getUserDetail(idFromBoard).enqueue(object : retrofit2.Callback<com.example.myapplication.UserDetail> {
            override fun onResponse(
                call: retrofit2.Call<com.example.myapplication.UserDetail>,
                response: retrofit2.Response<com.example.myapplication.UserDetail>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    bindAuthor(response.body()!!)
                    return
                }
                // 2차: ClubMember.id 가능성 → members에서 매핑 후 재조회
                mapClubMemberToUserAndFetch(idFromBoard)
            }
            override fun onFailure(
                call: retrofit2.Call<com.example.myapplication.UserDetail>,
                t: Throwable
            ) { mapClubMemberToUserAndFetch(idFromBoard) }
        })
    }

    private fun mapClubMemberToUserAndFetch(memberIdOrUserId: Int) {
        val clubPk = intent.getIntExtra("club_pk", -1)
        if (clubPk <= 0) return
        val client = com.example.myapplication.api.ApiClient.createUnsafeOkHttpClient()
        val url = com.example.myapplication.BuildConfig.BASE_URL.trimEnd('/') + "/club/" + clubPk + "/members/"
        val req = okhttp3.Request.Builder().url(url).get().build()
        Thread {
            try {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body?.string() ?: return@use
                    val type = object : com.google.gson.reflect.TypeToken<List<ClubAnnouncementBoardCreateActivity.ClubMemberBrief>>() {}.type
                    val items: List<ClubAnnouncementBoardCreateActivity.ClubMemberBrief> = com.google.gson.Gson().fromJson(body, type)
                    val match = items.firstOrNull { it.id == memberIdOrUserId } ?: return@use
                    // 재조회: user/{id}
                    runOnUiThread { fetchAuthorFromUserApiStrict(match.user) }
                }
            } catch (_: Exception) { }
        }.start()
    }

    private fun fetchAuthorFromUserApiStrict(userId: Int) {
        com.example.myapplication.api.ApiClient.getApiService().getUserDetail(userId)
            .enqueue(object : retrofit2.Callback<com.example.myapplication.UserDetail> {
                override fun onResponse(
                    call: retrofit2.Call<com.example.myapplication.UserDetail>,
                    response: retrofit2.Response<com.example.myapplication.UserDetail>
                ) { response.body()?.let { bindAuthor(it) } }
                override fun onFailure(
                    call: retrofit2.Call<com.example.myapplication.UserDetail>,
                    t: Throwable
                ) { }
            })
    }

    private fun bindAuthor(user: com.example.myapplication.UserDetail) {
        val twoDigit = user.student_number?.take(2)
        val yearText = twoDigit?.let { "${it}학번" }
        val major = user.major ?: ""
        findViewById<TextView>(R.id.tv_author_name)?.text = user.name ?: "작성자"
        findViewById<TextView>(R.id.tv_author_info)?.text = listOfNotNull(yearText, major.takeIf { it.isNotBlank() })
            .joinToString(" ")
    }
    
    private fun updateUI() {
        // 기본 표시
        findViewById<TextView>(R.id.tv_title).text = boardItem.title
        findViewById<TextView>(R.id.tv_content).text = boardItem.content
        findViewById<TextView>(R.id.tv_created_date).text = formatDate(boardItem.created_at)
        // 헤더에 클럽명 표시
        val clubPk = intent.getIntExtra("club_pk", -1)
        if (clubPk > 0) {
            com.example.myapplication.api.ApiClient.getApiService().getClubDetail(clubPk)
                .enqueue(object : retrofit2.Callback<com.example.myapplication.ClubItem> {
                    override fun onResponse(
                        call: retrofit2.Call<com.example.myapplication.ClubItem>,
                        response: retrofit2.Response<com.example.myapplication.ClubItem>
                    ) {
                        response.body()?.let { club ->
                            findViewById<TextView>(R.id.tv_club_title)?.text = club.name
                        }
                    }
                    override fun onFailure(
                        call: retrofit2.Call<com.example.myapplication.ClubItem>,
                        t: Throwable
                    ) { }
                })
        }
        // 서버 최신값으로 메타정보 갱신
        refreshMeta()
    }

    private fun refreshMeta() {
        val clubPk = intent.getIntExtra("club_pk", -1)
        val boardId = boardItem.id
        if (clubPk <= 0) return
        com.example.myapplication.api.ApiClient.getApiService().getBoardDetail(clubPk, boardId)
            .enqueue(object : retrofit2.Callback<com.example.myapplication.BoardItem> {
                override fun onResponse(
                    call: retrofit2.Call<com.example.myapplication.BoardItem>,
                    response: retrofit2.Response<com.example.myapplication.BoardItem>
                ) {
                    val latest = response.body() ?: return
                    findViewById<TextView>(R.id.tv_views)?.text = "조회수 ${latest.views}"
                    findViewById<TextView>(R.id.tv_likes_count)?.text = (latest.likes ?: "0").toString()
                    findViewById<TextView>(R.id.tv_comments_count)?.text = (latest.comments ?: "0").toString()
                    // 작성자 이름/학번/학과
                    if (latest.author_name != null || latest.author_student_short != null || latest.author_major != null) {
                        val name = latest.author_name ?: "작성자"
                        val year = latest.author_student_short ?: ""
                        val major = latest.author_major ?: ""
                        findViewById<TextView>(R.id.tv_author_name)?.text = name
                        findViewById<TextView>(R.id.tv_author_info)?.text = listOfNotNull(year, major.takeIf { it.isNotBlank() })
                            .joinToString(" ")
                    } else {
                        // 백업: 사용자 상세 호출로 채우기
                        val userId = latest.author ?: 0
                        if (userId > 0) fetchAuthorFromUserApi(userId)
                    }
                }
                override fun onFailure(
                    call: retrofit2.Call<com.example.myapplication.BoardItem>,
                    t: Throwable
                ) { }
            })
    }

    private fun loadComments() {
        val clubPk = intent.getIntExtra("club_pk", -1)
        val boardId = boardItem.id
        if (clubPk <= 0) return
        com.example.myapplication.api.ApiClient.getApiService()
            .getComments(clubPk, boardId)
            .enqueue(object : retrofit2.Callback<List<com.example.myapplication.CommentItem>> {
                override fun onResponse(
                    call: retrofit2.Call<List<com.example.myapplication.CommentItem>>,
                    response: retrofit2.Response<List<com.example.myapplication.CommentItem>>
                ) {
                    val comments = response.body() ?: emptyList()
                    // 헤더 카운트 갱신
                    findViewById<android.widget.TextView>(R.id.tv_comment_header)?.text = "댓글(${comments.size})"
                    // 목록 렌더링
                    val container = findViewById<android.widget.LinearLayout>(R.id.comments_container)
                    container?.removeAllViews()
                    comments.forEach { comment ->
                        val row = android.widget.LinearLayout(this@ClubAnnouncementBoardDetailActivity).apply {
                            orientation = android.widget.LinearLayout.HORIZONTAL
                            layoutParams = android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 0, 0, 16) }
                            gravity = android.view.Gravity.TOP
                        }

                        val avatar = android.widget.ImageView(this@ClubAnnouncementBoardDetailActivity).apply {
                            layoutParams = android.widget.LinearLayout.LayoutParams(44, 44)
                            setImageResource(R.drawable.account)
                        }
                        row.addView(avatar)

                        val right = android.widget.LinearLayout(this@ClubAnnouncementBoardDetailActivity).apply {
                            orientation = android.widget.LinearLayout.VERTICAL
                            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                                setMargins(12, 0, 0, 0)
                            }
                        }

                        val metaRow = android.widget.LinearLayout(this@ClubAnnouncementBoardDetailActivity).apply {
                            orientation = android.widget.LinearLayout.HORIZONTAL
                        }
                        val authorName = android.widget.TextView(this@ClubAnnouncementBoardDetailActivity).apply {
                            text = "작성자 #${comment.author}"
                            setTextColor(android.graphics.Color.BLACK)
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            textSize = 16f
                        }
                        val authorInfo = android.widget.TextView(this@ClubAnnouncementBoardDetailActivity).apply {
                            text = ""
                            textSize = 12f
                            setPadding(8, 0, 0, 0)
                        }
                        metaRow.addView(authorName)
                        metaRow.addView(authorInfo)

                        val createdAtView = android.widget.TextView(this@ClubAnnouncementBoardDetailActivity).apply {
                            text = formatDate(comment.created_at)
                            textSize = 14f
                        }
                        val contentView = android.widget.TextView(this@ClubAnnouncementBoardDetailActivity).apply {
                            text = comment.content
                            textSize = 16f
                            setPadding(0, 4, 0, 0)
                        }

                        right.addView(metaRow)
                        right.addView(createdAtView)
                        right.addView(contentView)

                        val likeRow = android.widget.LinearLayout(this@ClubAnnouncementBoardDetailActivity).apply {
                            orientation = android.widget.LinearLayout.HORIZONTAL
                            gravity = android.view.Gravity.CENTER_VERTICAL
                        }
                        val likeIcon = android.widget.ImageView(this@ClubAnnouncementBoardDetailActivity).apply {
                            layoutParams = android.widget.LinearLayout.LayoutParams(20, 20)
                            setImageResource(R.drawable.like_img)
                        }
                        val likeCount = android.widget.TextView(this@ClubAnnouncementBoardDetailActivity).apply {
                            text = (comment.likes ?: "0").toString()
                            textSize = 14f
                            setPadding(6, 0, 0, 0)
                        }
                        likeRow.addView(likeIcon)
                        likeRow.addView(likeCount)

                        right.addView(likeRow)
                        row.addView(right)
                        container?.addView(row)
                    }
                }

                override fun onFailure(
                    call: retrofit2.Call<List<com.example.myapplication.CommentItem>>,
                    t: Throwable
                ) { }
            })
    }

    private fun sendComment() {
        val clubPk = intent.getIntExtra("club_pk", -1)
        val boardId = boardItem.id
        if (clubPk <= 0) return
        val input = findViewById<android.widget.EditText>(R.id.et_comment)
        val content = input.text?.toString()?.trim().orEmpty()
        if (content.isEmpty()) {
            Toast.makeText(this, "댓글 내용을 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }
        val api = com.example.myapplication.api.ApiClient.getApiService()
        api.postComment(clubPk, boardId, com.example.myapplication.CommentCreateRequest(content))
            .enqueue(object : retrofit2.Callback<com.example.myapplication.CommentItem> {
                override fun onResponse(
                    call: retrofit2.Call<com.example.myapplication.CommentItem>,
                    response: retrofit2.Response<com.example.myapplication.CommentItem>
                ) {
                    if (response.isSuccessful) {
                        input.setText("")
                        loadComments()
                    } else {
                        Toast.makeText(this@ClubAnnouncementBoardDetailActivity, "댓글 등록 실패", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(
                    call: retrofit2.Call<com.example.myapplication.CommentItem>,
                    t: Throwable
                ) {
                    Toast.makeText(this@ClubAnnouncementBoardDetailActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private var isLiked: Boolean = false

    private fun toggleLike() {
        val clubPk = intent.getIntExtra("club_pk", -1)
        val boardId = boardItem.id
        if (clubPk <= 0) return
        val api = com.example.myapplication.api.ApiClient.getApiService()
        val userId = UserManager.getUserPk(this) ?: -1
        val body = com.example.myapplication.api.ApiService.LikeRequest(userId)
        api.toggleBoardLike(clubPk, boardId, body).enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {
            override fun onResponse(
                call: retrofit2.Call<okhttp3.ResponseBody>,
                response: retrofit2.Response<okhttp3.ResponseBody>
            ) {
                if (response.isSuccessful) {
                    isLiked = !isLiked
                    updateLikeUi()
                    refreshMeta()
                } else {
                    Toast.makeText(this@ClubAnnouncementBoardDetailActivity, "좋아요 처리 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                Toast.makeText(this@ClubAnnouncementBoardDetailActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateLikeUi() {
        val likeView = findViewById<android.widget.ImageView>(R.id.iv_like)
        if (isLiked) {
            likeView?.setImageResource(R.drawable.ic_heart_filled)
        } else {
            likeView?.setImageResource(R.drawable.ic_heart_outline)
        }
    }
    
    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.board_detail_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    // 수정하기 화면으로 이동
                    val intent = Intent(this, ClubAnnouncementBoardUpdateActivity::class.java)
                    intent.putExtra("board_item", boardItem)
                    startActivity(intent)
                    true
                }
                R.id.action_delete -> {
                    // 삭제 확인 다이얼로그 표시
                    showDeleteConfirmDialog()
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
    
    private fun showDeleteConfirmDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_confirm, null)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // 배경을 반투명하게 설정
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 다이얼로그 위치를 화면 아래쪽으로 설정
        dialog.window?.setGravity(android.view.Gravity.BOTTOM)
        
        // 애니메이션 설정 (아래에서 위로 올라오는 효과)
        dialog.window?.attributes?.windowAnimations = R.style.Animation_Dialog
        
        // 취소 버튼
        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        
        // 삭제 버튼
        dialogView.findViewById<Button>(R.id.btn_delete).setOnClickListener {
            // 실제 삭제 로직 구현
            deleteBoard()
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun deleteBoard() {
        // TODO: API 호출하여 게시글 삭제
        Toast.makeText(this, "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun formatDate(dateString: String): String {
        return try {
            val instant = try {
                java.time.OffsetDateTime.parse(dateString, java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .toInstant()
            } catch (_: Exception) {
                val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
                java.time.LocalDateTime.parse(dateString, fmt).atZone(java.time.ZoneOffset.UTC).toInstant()
            }
            val kst = instant.atZone(java.time.ZoneId.of("Asia/Seoul"))
            kst.format(
                java.time.format.DateTimeFormatter
                    .ofPattern("yyyy. MM. dd(E) HH:mm")
                    .withLocale(java.util.Locale.KOREA)
            )
        } catch (e: Exception) {
            try {
                val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", java.util.Locale.getDefault())
                val date = inputFormat.parse(dateString)
                val outputFormat = java.text.SimpleDateFormat("yyyy. MM. dd(E) HH:mm", java.util.Locale.KOREA)
                outputFormat.format(date ?: java.util.Date())
            } catch (_: Exception) { dateString }
        }
    }
}
