package com.example.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ClubForumBoardDetailActivity : AppCompatActivity() {
    
    private lateinit var boardItem: BoardItem
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_forum_board_detail)
        
        // Intent에서 BoardItem 데이터 받기
        boardItem = intent.getParcelableExtra("board_item") ?: BoardItem(
            1, "free_board", "샘플 게시글", "샘플 내용", 13, "2025-08-20T10:25:00.000000+09:00", 1
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
    }
    
    private fun updateUI() {
        // 제목 설정
        findViewById<TextView>(R.id.tv_title).text = boardItem.title
        
        // 내용 설정
        findViewById<TextView>(R.id.tv_content).text = boardItem.content
        
        // 조회수 설정
        findViewById<TextView>(R.id.tv_views).text = "조회수 ${boardItem.views}"
        // 좋아요/댓글 수 (초기 표시)
        findViewById<TextView>(R.id.tv_likes_count)?.text = (boardItem.likes ?: "0").toString()
        findViewById<TextView>(R.id.tv_comments_count)?.text = (boardItem.comments ?: "0").toString()
        
        // 헤더 클럽명
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
        findViewById<TextView>(R.id.tv_created_date).text = formatDate(boardItem.created_at)
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
                    // 작성자 표시(이름/학번/학과)
                    if (latest.author_name != null || latest.author_student_short != null || latest.author_major != null) {
                        val name = latest.author_name ?: "작성자"
                        val year = latest.author_student_short ?: ""
                        val major = latest.author_major ?: ""
                        findViewById<TextView>(R.id.tv_author_name)?.text = name
                        findViewById<TextView>(R.id.tv_author_info)?.text = listOfNotNull(year, major.takeIf { it.isNotBlank() })
                            .joinToString(" ")
                    } else {
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
    
    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.board_detail_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    // 수정하기 화면으로 이동
                    val intent = Intent(this, ClubForumBoardUpdateActivity::class.java)
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
                    // 재조회로 카운트 업데이트
                    refreshMeta()
                } else {
                    Toast.makeText(this@ClubForumBoardDetailActivity, "좋아요 처리 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                Toast.makeText(this@ClubForumBoardDetailActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchAuthorFromUserApi(idFromBoard: Int) {
        val api = com.example.myapplication.api.ApiClient.getApiService()
        api.getUserDetail(idFromBoard).enqueue(object : retrofit2.Callback<com.example.myapplication.UserDetail> {
            override fun onResponse(
                call: retrofit2.Call<com.example.myapplication.UserDetail>,
                response: retrofit2.Response<com.example.myapplication.UserDetail>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    bindAuthor(response.body()!!)
                    return
                }
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

