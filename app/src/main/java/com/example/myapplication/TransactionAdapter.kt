package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    companion object {
        // 단일 뷰 타입만 사용
    }
    
    private val items = mutableListOf<Any>()
    
    fun updateTransactions(transactions: List<AccountHistoryActivity.Transaction>) {
        items.clear()
        
        // 모든 거래 내역을 순서대로 추가 (날짜 헤더 없이)
        items.addAll(transactions)
        
        notifyDataSetChanged()
    }
    
    override fun getItemViewType(position: Int): Int {
        return 0 // 단일 뷰 타입만 사용하므로 0 반환
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as TransactionViewHolder).bind(items[position] as AccountHistoryActivity.Transaction)
    }
    
    override fun getItemCount(): Int = items.size
    

    
    // 거래 내역 ViewHolder
    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDateTime: TextView = itemView.findViewById(R.id.tv_transaction_type)
        private val tvSummary: TextView = itemView.findViewById(R.id.tv_transaction_summary)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_transaction_amount)
        private val tvBalance: TextView = itemView.findViewById(R.id.tv_transaction_balance)
        private val tvMemo: TextView = itemView.findViewById(R.id.tv_transaction_memo)
        
        fun bind(transaction: AccountHistoryActivity.Transaction) {
            // 날짜와 시간 표시
            val dateTimeText = formatDateTime(transaction.transactionDate, transaction.transactionTime)
            tvDateTime.text = dateTimeText
            
            // 거래 요약 표시 (요약이 없으면 거래 타입명 사용)
            val summary = transaction.transactionSummary ?: transaction.transactionTypeName
            tvSummary.text = summary
            
            // 거래 금액 표시 (+ 또는 -) - 3자리씩 끊어서 표시
            val amountText = when (transaction.transactionType) {
                "1" -> "+ ${formatAmount(transaction.transactionBalance)}원" // 입금(수입)
                "2" -> "- ${formatAmount(transaction.transactionBalance)}원" // 출금(지출)
                else -> "${formatAmount(transaction.transactionBalance)}원"
            }
            tvAmount.text = amountText
            
            // 금액 색상 설정
            val amountColor = when (transaction.transactionType) {
                "1" -> 0xFF2457C5.toInt() // 파란색 (수입)
                "2" -> 0xFFD32F2F.toInt() // 빨간색 (지출)
                else -> 0xFF333333.toInt() // 기본색
            }
            tvAmount.setTextColor(amountColor)
            
            // 거래후잔액 표시 - 3자리씩 끊어서 표시
            tvBalance.text = "${formatAmount(transaction.transactionAfterBalance)}원"
            
            // 거래 메모가 있는 경우 표시
            if (!transaction.transactionMemo.isNullOrEmpty()) {
                tvMemo.text = transaction.transactionMemo
                tvMemo.visibility = View.VISIBLE
            } else {
                tvMemo.visibility = View.GONE
            }
        }
        
        // 날짜와 시간을 포맷팅하는 함수
        private fun formatDateTime(date: String, time: String): String {
            try {
                if (date.length == 8 && time.length == 6) {
                    val year = date.substring(0, 4)
                    val month = date.substring(4, 6)
                    val day = date.substring(6, 8)
                    
                    val hour = time.substring(0, 2)
                    val minute = time.substring(2, 4)
                    
                    // 요일 계산
                    val calendar = Calendar.getInstance()
                    calendar.set(year.toInt(), month.toInt() - 1, day.toInt())
                    val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.SUNDAY -> "일요일"
                        Calendar.MONDAY -> "월요일"
                        Calendar.TUESDAY -> "화요일"
                        Calendar.WEDNESDAY -> "수요일"
                        Calendar.THURSDAY -> "목요일"
                        Calendar.FRIDAY -> "금요일"
                        Calendar.SATURDAY -> "토요일"
                        else -> ""
                    }
                    
                    return "${year}년 ${month}월 ${day}일 $dayOfWeek $hour:$minute"
                }
            } catch (e: Exception) {
                // 파싱 실패 시 원본 반환
            }
            return "$date $time"
        }
        
        // 금액을 3자리씩 끊어서 표시하는 함수
        private fun formatAmount(amount: Long): String {
            return String.format("%,d", amount)
        }
    }
}
