package com.example.myapplication

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class EventTransactionAdapter(
    private var transactions: List<EventTransactionItem>
) : RecyclerView.Adapter<EventTransactionAdapter.EventTransactionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventTransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_transaction, parent, false)
        return EventTransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventTransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int = transactions.size

    fun updateData(newTransactions: List<EventTransactionItem>) {
        this.transactions = newTransactions
        notifyDataSetChanged()
    }

    inner class EventTransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val transactionType: TextView = itemView.findViewById(R.id.tv_transaction_type)
        private val category: TextView = itemView.findViewById(R.id.tv_transaction_category)
        private val date: TextView = itemView.findViewById(R.id.tv_transaction_date)
        private val amount: TextView = itemView.findViewById(R.id.tv_transaction_amount)
        private val vendor: TextView = itemView.findViewById(R.id.tv_transaction_vendor)
        private val description: TextView = itemView.findViewById(R.id.tv_transaction_description)

        fun bind(transaction: EventTransactionItem) {
            // amount 값으로 수입/지출 판단
            val isIncome = transaction.amount >= 0
            if (isIncome) {
                transactionType.text = "수입"
                transactionType.setBackgroundResource(R.drawable.btn_income_background)
                transactionType.setTextColor(Color.parseColor("#2457C5"))
            } else {
                transactionType.text = "지출"
                transactionType.setBackgroundResource(R.drawable.btn_expense_background)
                transactionType.setTextColor(Color.parseColor("#D32F2F"))
            }
            
            // type을 카테고리로 표시
            if (transaction.type.isNullOrEmpty()) {
                category.visibility = View.GONE
            } else {
                category.visibility = View.VISIBLE
                category.text = transaction.type
            }

            // 날짜 포맷 변경
            try {
                if (transaction.dateTime != null) {
                    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val formatter = SimpleDateFormat("yyyy년 MM월 dd일 E요일 HH:mm", Locale.KOREAN)
                    
                    // dateTime이 Z로 끝나는 경우 처리
                    val dateTimeStr = if (transaction.dateTime.endsWith("Z")) {
                        transaction.dateTime.replace("Z", "")
                    } else {
                        transaction.dateTime.substringBefore(".")
                    }
                    
                    val parsedDate = parser.parse(dateTimeStr)
                    if (parsedDate != null) {
                        date.text = formatter.format(parsedDate)
                    } else {
                        date.text = "날짜 정보 없음"
                    }
                } else {
                    date.text = "날짜 정보 없음"
                }
            } catch (e: Exception) {
                date.text = transaction.dateTime ?: "날짜 정보 없음"
            }

            // 금액 포맷 변경 (절댓값으로 표시)
            val absAmount = Math.abs(transaction.amount)
            val formattedAmount = NumberFormat.getNumberInstance(Locale.KOREA).format(absAmount)
            if (isIncome) {
                amount.text = "+ ${formattedAmount}원"
                amount.setTextColor(Color.parseColor("#2457C5"))
            } else {
                amount.text = "- ${formattedAmount}원"
                amount.setTextColor(Color.parseColor("#D32F2F"))
            }
            
            // 거래처 및 메모
            vendor.text = "· 거래처 : ${transaction.vendor}"
            description.text = "· 상세 : ${transaction.description}"
        }
    }
}