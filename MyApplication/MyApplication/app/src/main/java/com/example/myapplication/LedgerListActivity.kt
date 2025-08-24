package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LedgerListActivity : BaseActivity() {

    private lateinit var tvYear: TextView
    private lateinit var tvMonth: TextView
    private lateinit var btnPrevMonth: View
    private lateinit var btnNextMonth: View
    private val calendar: Calendar = Calendar.getInstance()

    private lateinit var recyclerView: RecyclerView
    private lateinit var ledgerAdapter: LedgerAdapter
    private val ledgerItems = mutableListOf<LedgerItem>()

    override fun setupContent() {
        // 앱 제목을 "짱구네 코딩"으로 설정
        setAppTitle("짱구네 코딩")

        // LedgerListActivity의 내용을 content_container에 추가
        val contentContainer = findViewById<android.widget.FrameLayout>(R.id.content_container)
        val contentView = layoutInflater.inflate(R.layout.ledger_list, null)
        contentContainer.addView(contentView)

        // "공개장부" 버튼을 선택된 상태로 업데이트
        val btnPublicAccount = findViewById<TextView>(R.id.btn_public_account)
        val btnNotice = findViewById<TextView>(R.id.btn_notice)
        val btnFreeBoard = findViewById<TextView>(R.id.btn_free_board)
        val btnMeetingAccount = findViewById<TextView>(R.id.btn_meeting_account)
        val btnAiReport = findViewById<TextView>(R.id.btn_ai_report)
        updateBoardButton(btnPublicAccount, btnNotice, btnFreeBoard, btnMeetingAccount, btnAiReport)

        // 날짜 관련 뷰 초기화
        tvYear = contentView.findViewById(R.id.tv_year)
        tvMonth = contentView.findViewById(R.id.tv_month)
        btnPrevMonth = contentView.findViewById(R.id.btn_prev_month)
        btnNextMonth = contentView.findViewById(R.id.btn_next_month)

        updateDate()

        btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateDate()
        }

        btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateDate()
        }

        // RecyclerView 설정
        recyclerView = contentView.findViewById(R.id.recycler_view_ledger)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // 샘플 데이터 추가
        setupSampleData()

        ledgerAdapter = LedgerAdapter(this, ledgerItems)
        recyclerView.adapter = ledgerAdapter
        
        // 스와이프 기능 추가
        val swipeToDeleteCallback = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                ledgerAdapter.removeItem(position)
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun updateDate() {
        val yearFormat = SimpleDateFormat("yyyy년", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MM월", Locale.getDefault())
        tvYear.text = yearFormat.format(calendar.time)
        tvMonth.text = monthFormat.format(calendar.time)
    }

    private fun setupSampleData() {
        ledgerItems.add(
            LedgerItem(
                type = "수입",
                tags = listOf("수입", "회비"),
                date = "2025년 08월 20일 수요일 10:25",
                amount = "+ 20,000원",
                author = "· 작성자 : 김싸피",
                memo = "· 메모 : 신입 동아리원 박싸피 연간 동아리 회비",
                hasReceipt = false
            )
        )
        ledgerItems.add(
            LedgerItem(
                type = "지출",
                tags = listOf("지출", "소모품비"),
                date = "2025년 08월 04일 월요일 10:25",
                amount = "- 15,000원",
                author = "· 작성자 : 김싸피",
                memo = "· 메모 : 신입 동아리원 교육을 위한 운",
                hasReceipt = true
            )
        )
        ledgerItems.add(
            LedgerItem(
                type = "수입",
                tags = listOf("수입", "회비"),
                date = "2025년 08월 20일 수요일 10:25",
                amount = "+ 20,000원",
                author = "· 작성자 : 김싸피",
                memo = "· 메모 : 신입 동아리원 박싸피 연간 동아리 회비",
                hasReceipt = false
            )
        )
    }
}
