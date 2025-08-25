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
        // 좋아요/댓글 수
        findViewById<TextView>(R.id.tv_likes_count)?.text = (boardItem.likes ?: "0").toString()
        findViewById<TextView>(R.id.tv_comments_count)?.text = (boardItem.comments ?: "0").toString()
        
        // 작성자 정보 설정 (실제로는 API에서 받아와야 함)
        findViewById<TextView>(R.id.tv_author_name).text = "김자유"
        findViewById<TextView>(R.id.tv_author_info).text = "16학번 컴퓨터공학과"
        findViewById<TextView>(R.id.tv_created_date).text = formatDate(boardItem.created_at)
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

