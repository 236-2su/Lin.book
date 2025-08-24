package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

class MemberManagementActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var memberAdapter: MemberAdapter
    private val members = mutableListOf<Member>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.member_management)

        setupRecyclerView()
        setupSwipeToDelete()
        loadSampleData()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewMembers)
        memberAdapter = MemberAdapter(members) { deletedMember ->
            // 삭제된 멤버 처리 로직
            showDeletedSnackbar(deletedMember)
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MemberManagementActivity)
            adapter = memberAdapter
        }
    }

    private fun setupSwipeToDelete() {
        val swipeToDeleteCallback = SwipeToDeleteCallback(this) { position ->
            val deletedMember = members[position]
            memberAdapter.removeItem(position)
            
            // Snackbar로 실행 취소 옵션 제공
            Snackbar.make(recyclerView, "${deletedMember.name}님이 삭제되었습니다.", Snackbar.LENGTH_LONG)
                .setAction("실행 취소") {
                    memberAdapter.restoreItem(position, deletedMember)
                }
                .show()
        }
        
        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun loadSampleData() {
        members.addAll(
            listOf(
                Member(1, "김쌔피", "회장", "컴퓨터공학과", "2313545", "010-0000-0000", "2023년 03월 02일", isMe = true),
                Member(2, "김철수", "부회장", "소프트웨어공학과", "2023124", "010-0000-0000", "2024년 04월 27일"),
                Member(3, "윤예진", "일반", "인공지능융합학과", "2023256", "010-0000-0000", "2025년 03월 27일")
            )
        )
        memberAdapter.notifyDataSetChanged()
    }

    private fun showDeletedSnackbar(member: Member) {
        // 추가적인 삭제 후 처리 로직
    }
}