package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class LedgerDetailActivity : BaseActivity() {

    private var ledgerItems: ArrayList<LedgerItem>? = null
    private var currentPosition: Int = 0

    private lateinit var btnPrev: View
    private lateinit var btnNext: View
    private lateinit var contentView: View
    
    // 댓글 좋아요 상태 관리
    private var isCommentLiked = false
    private var commentLikeCount = 0

    // Activity Result Launcher
    private val editLedgerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { intent ->
                val editedAmount = intent.getStringExtra("EDITED_AMOUNT") ?: ""
                val editedMemo = intent.getStringExtra("EDITED_MEMO") ?: ""
                val editedPartnerName = intent.getStringExtra("EDITED_PARTNER_NAME") ?: ""
                val editedType = intent.getStringExtra("EDITED_TYPE") ?: ""
                val editedPaymentMethod = intent.getStringExtra("EDITED_PAYMENT_METHOD") ?: ""
                val editedDate = intent.getStringExtra("EDITED_DATE") ?: ""

                // 수정된 데이터로 현재 아이템 업데이트
                ledgerItems?.let { items ->
                    val currentItem = items[currentPosition]
                    val newTags = when (editedPaymentMethod) {
                        "현금" -> listOf(editedType, "현금")
                        "카드" -> listOf(editedType, "카드")
                        "계좌이체" -> listOf(editedType, "계좌이체")
                        "기타" -> listOf(editedType, "기타")
                        else -> listOf(editedType, "기타")
                    }
                    
                    // 새로운 LedgerItem 객체 생성하여 교체
                    val updatedItem = currentItem.copy(
                        type = editedType,
                        amount = if (editedType == "수입") "+ ${editedAmount}원" else "- ${editedAmount}원",
                        memo = "· 메모 : $editedMemo",
                        date = editedDate,
                        tags = newTags
                    )
                    
                    // ArrayList에서 해당 위치의 아이템 교체
                    items[currentPosition] = updatedItem
                }

                // 화면 새로고침
                displayLedgerItem(currentPosition)
                
                Toast.makeText(this, "장부가 수정되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ledgerItems = intent.getSerializableExtra("LEDGER_ITEM_LIST") as? ArrayList<LedgerItem>
        currentPosition = intent.getIntExtra("CURRENT_POSITION", 0)

        super.onCreate(savedInstanceState)
    }

    override fun setupContent(savedInstanceState: Bundle?) {
        val contentContainer = findViewById<FrameLayout>(R.id.content_container)
        contentContainer.removeAllViews()
        contentView = LayoutInflater.from(this).inflate(R.layout.activity_ledger_detail, contentContainer, true)

        setAppTitle("장부 상세")
        hideBoardButtons()

        btnPrev = contentView.findViewById(R.id.btn_prev_day)
        btnNext = contentView.findViewById(R.id.btn_next_day)

        btnPrev.setOnClickListener {
            if (currentPosition > 0) {
                currentPosition--
                displayLedgerItem(currentPosition)
            }
        }

        btnNext.setOnClickListener {
            if (ledgerItems != null && currentPosition < ledgerItems!!.size - 1) {
                currentPosition++
                displayLedgerItem(currentPosition)
            }
        }

        // 삭제하기 버튼 설정
        val btnDelete = contentView.findViewById<TextView>(R.id.btn_delete)
        btnDelete?.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        displayLedgerItem(currentPosition)
    }

    private fun showDeleteConfirmationDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("삭제 확인")
            .setMessage("정말로 이 장부 항목을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                deleteLedgerItem()
            }
            .setNegativeButton("취소") { _, _ ->
                // 아무것도 하지 않음
            }
            .show()
    }

    private fun deleteLedgerItem() {
        // 현재 장부 항목의 정보를 가져옴
        val currentItem = ledgerItems?.get(currentPosition)
        if (currentItem != null) {
            // DELETE API 요청
            sendDeleteRequest(currentItem)
        } else {
            Toast.makeText(this, "삭제할 항목을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendDeleteRequest(ledgerItem: LedgerItem) {
        // 기본값 설정 (실제로는 동적으로 가져와야 함)
        val clubPk = "1" // 임시 값
        val ledgerPk = "1" // 임시 값
        val transactionId = "1" // 임시 값

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val baseUrl = com.example.myapplication.BuildConfig.BASE_URL.trimEnd('/')
                val url = URL("$baseUrl/club/$clubPk/ledger/$ledgerPk/transactions/$transactionId/")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "DELETE"
                connection.setRequestProperty("Content-Type", "application/json")
                
                val responseCode = connection.responseCode
                android.util.Log.d("LedgerDetail", "DELETE 요청 응답 코드: $responseCode")
                
                withContext(Dispatchers.Main) {
                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                        Toast.makeText(this@LedgerDetailActivity, "장부 항목이 성공적으로 삭제되었습니다.", Toast.LENGTH_LONG).show()
                        android.util.Log.d("LedgerDetail", "장부 항목 삭제 성공")
                        
                        // 삭제된 항목을 리스트에서 제거
                        ledgerItems?.removeAt(currentPosition)
                        
                        // 리스트가 비어있으면 이전 페이지로 이동
                        if (ledgerItems.isNullOrEmpty()) {
                            finish()
                        } else {
                            // 현재 위치 조정 및 다음 항목 표시
                            if (currentPosition >= ledgerItems!!.size) {
                                currentPosition = ledgerItems!!.size - 1
                            }
                            displayLedgerItem(currentPosition)
                        }
                    } else {
                        Toast.makeText(this@LedgerDetailActivity, "장부 항목 삭제에 실패했습니다. (응답 코드: $responseCode)", Toast.LENGTH_LONG).show()
                        android.util.Log.e("LedgerDetail", "장부 항목 삭제 실패 - 응답 코드: $responseCode")
                    }
                }
                
                connection.disconnect()
                
            } catch (e: Exception) {
                android.util.Log.e("LedgerDetail", "장부 항목 삭제 중 오류 발생", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LedgerDetailActivity, "장부 항목 삭제 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun displayLedgerItem(position: Int) {
        val item = ledgerItems?.get(position) ?: return

        // 날짜 (월, 일)
        val tvMonth = contentView.findViewById<TextView>(R.id.tv_month_detail)
        val tvDay = contentView.findViewById<TextView>(R.id.tv_day_detail)
        item.date.split(".").let {
            if (it.size >= 3) {
                tvMonth.text = "${it[1]}월"
                tvDay.text = it[2]
            }
        }

        // 태그
        val tagsContainer = contentView.findViewById<LinearLayout>(R.id.tags_container_detail)
        tagsContainer.removeAllViews()
        item.tags.forEach { tag ->
            val styleResId = when (tag) {
                "수입" -> R.style.ReferenceSmallButton_Income
                "지출" -> R.style.ReferenceSmallButton_Expense
                "회비" -> R.style.ReferenceSmallButton_Membership
                "소모품비" -> R.style.ReferenceSmallButton_Supplies
                else -> R.style.ReferenceSmallButton_General
            }
            val tagView = TextView(ContextThemeWrapper(this, styleResId), null, 0)
            tagView.text = tag
            tagsContainer.addView(tagView)
        }

        // 상세 정보
        val tvFullDate = contentView.findViewById<TextView>(R.id.tv_full_date)
        val tvAmount = contentView.findViewById<TextView>(R.id.tv_amount_detail)
        val tvAuthor = contentView.findViewById<TextView>(R.id.tv_author_detail)

        tvFullDate.text = item.date
        tvAmount.text = item.amount
        tvAuthor.text = "작성자: ${item.author}"

        // 수정하기 버튼 클릭 리스너 설정
        val btnEdit = contentView.findViewById<TextView>(R.id.btn_edit)
        btnEdit.setOnClickListener {
            val intent = Intent(this@LedgerDetailActivity, LedgerEditActivity::class.java)
            
            // LedgerItem에서 직접 데이터 추출 (기존 구조 그대로 사용)
            val amount = item.amount.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
            val date = item.date   // LedgerItem의 date 필드 직접 사용
            val memo = item.memo   // LedgerItem의 memo 필드 직접 사용
            val vendor = item.author // LedgerItem의 author 필드를 vendor로 사용
            
            // type 필드가 "수입"/"지출"이 아니면 금액 부호로 자동 설정
            val type = when {
                item.type == "수입" || item.type == "지출" -> item.type
                amount > 0 -> "수입"
                amount < 0 -> "지출"
                else -> "기타"
            }
            
            // 태그에서 결제수단 찾기
            val paymentMethod = when {
                item.tags.contains("현금") -> "현금"
                item.tags.contains("카드") -> "카드"
                item.tags.contains("계좌이체") -> "계좌이체"
                else -> "기타"
            }
            
            // 디버깅을 위한 로그
            Log.d("LedgerDetail", "수정하기 버튼 클릭 - 전달할 데이터:")
            Log.d("LedgerDetail", "전체 item 객체: $item")
            Log.d("LedgerDetail", "원본 item.type: '${item.type}'")
            Log.d("LedgerDetail", "계산된 type: '$type'")
            Log.d("LedgerDetail", "date: $date, amount: $amount")
            Log.d("LedgerDetail", "vendor: $vendor, payment_method: $paymentMethod, memo: $memo")
            
            // TRANSACTION_CATEGORY 값 확인
            Log.d("LedgerDetail", "TRANSACTION_CATEGORY로 전달할 값: '${item.type}'")
            
                            // Intent로 데이터 전달
                intent.putExtra("ORIGINAL_TYPE", type)
                intent.putExtra("ORIGINAL_DATE", date)
                intent.putExtra("ORIGINAL_AMOUNT", amount)
                intent.putExtra("VENDOR_NAME", vendor)
                intent.putExtra("PAYMENT_METHOD", paymentMethod)
                intent.putExtra("ORIGINAL_MEMO", memo)
                intent.putExtra("TRANSACTION_CATEGORY", item.type) // 원본 item.type 값을 그대로 사용
            
            editLedgerLauncher.launch(intent)
        }

        // 댓글 하트 기능 설정
        setupCommentHeart()
        
        // --- 네비게이션 버튼 가시성 업데이트 ---
        btnPrev.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
        btnNext.visibility = if (ledgerItems != null && position == ledgerItems!!.size - 1) View.INVISIBLE else View.VISIBLE
    }
    
    // 댓글 하트 기능 설정
    private fun setupCommentHeart() {
        val ivCommentHeart = contentView.findViewById<ImageView>(R.id.iv_comment_heart)
        val tvCommentLikeCount = contentView.findViewById<TextView>(R.id.tv_comment_like_count)
        
        // 초기 상태 설정
        updateCommentHeartUI()
        
        // 하트 클릭 이벤트
        ivCommentHeart?.setOnClickListener {
            toggleCommentLike()
        }
    }
    
    // 댓글 좋아요 토글
    private fun toggleCommentLike() {
        isCommentLiked = !isCommentLiked
        
        if (isCommentLiked) {
            commentLikeCount++
            Toast.makeText(this, "댓글을 좋아합니다", Toast.LENGTH_SHORT).show()
        } else {
            commentLikeCount--
            Toast.makeText(this, "좋아요를 취소했습니다", Toast.LENGTH_SHORT).show()
        }
        
        updateCommentHeartUI()
    }
    
    // 댓글 하트 UI 업데이트
    private fun updateCommentHeartUI() {
        val ivCommentHeart = contentView.findViewById<ImageView>(R.id.iv_comment_heart)
        val tvCommentLikeCount = contentView.findViewById<TextView>(R.id.tv_comment_like_count)
        
        // 하트 아이콘 변경
        if (isCommentLiked) {
            ivCommentHeart?.setImageResource(R.drawable.ic_heart_filled)
        } else {
            ivCommentHeart?.setImageResource(R.drawable.ic_heart_outline)
        }
        
        // 좋아요 수 업데이트
        tvCommentLikeCount?.text = commentLikeCount.toString()
    }
}
