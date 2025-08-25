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
    
    private fun updateUI() {
        // 기본 표시
        findViewById<TextView>(R.id.tv_title).text = boardItem.title
        findViewById<TextView>(R.id.tv_content).text = boardItem.content
        findViewById<TextView>(R.id.tv_created_date).text = formatDate(boardItem.created_at)
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
                    // 좋아요/댓글 카운트는 메타 영역/댓글 헤더에서 별도 표시
                    // 댓글 수는 loadComments()에서 헤더로 표시됨
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
        api.toggleBoardLike(clubPk, boardId).enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {
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
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", java.util.Locale.getDefault())
            val date = inputFormat.parse(dateString)
            val outputFormat = java.text.SimpleDateFormat("yyyy. MM. dd(E) HH:mm", java.util.Locale.KOREA)
            outputFormat.format(date ?: java.util.Date())
        } catch (e: Exception) {
            dateString
        }
    }
}
