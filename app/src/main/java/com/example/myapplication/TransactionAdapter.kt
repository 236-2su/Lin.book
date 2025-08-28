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

class TransactionAdapter(
    private var transactions: List<TransactionItem>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private var onItemClickListener: ((Int) -> Unit)? = null

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int = transactions.size

    fun updateData(newTransactions: List<TransactionItem>) {
        this.transactions = newTransactions
        notifyDataSetChanged()
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val type: TextView = itemView.findViewById(R.id.tv_transaction_type)
        private val category: TextView = itemView.findViewById(R.id.tv_transaction_category)
        private val date: TextView = itemView.findViewById(R.id.tv_transaction_date)
        private val amount: TextView = itemView.findViewById(R.id.tv_transaction_amount)
        private val vendor: TextView = itemView.findViewById(R.id.tv_transaction_vendor)
        private val description: TextView = itemView.findViewById(R.id.tv_transaction_description)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(position)
                }
            }
        }

        fun bind(transaction: TransactionItem) {
            // 타입 및 카테고리
            type.text = transaction.type
            if (transaction.category.isNullOrEmpty()) {
                category.visibility = View.GONE
            } else {
                category.visibility = View.VISIBLE
                category.text = transaction.category
            }

            // 날짜 포맷 변경
            try {
                if (transaction.dateTime != null) {
                    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val formatter = SimpleDateFormat("yyyy년 MM월 dd일 E요일 HH:mm", Locale.KOREAN)
                    val parsedDate = parser.parse(transaction.dateTime)
                    if (parsedDate != null) {
                        date.text = formatter.format(parsedDate)
                    } else {
                        date.text = "날짜 정보 없음"
                    }
                } else {
                    date.text = "날짜 정보 없음"
                }
            } catch (e: Exception) {
                date.text = transaction.dateTime ?: "날짜 정보 없음" // 파싱 실패 시 원본 표시
            }

            // 금액 포맷 변경
            val formattedAmount = NumberFormat.getNumberInstance(Locale.KOREA).format(transaction.amount)
            if (transaction.type == "수입") {
                amount.text = "+ ${formattedAmount}원"
                amount.setTextColor(Color.parseColor("#1976D2")) // 파란색
            } else {
                amount.text = "- ${formattedAmount}원"
                amount.setTextColor(Color.parseColor("#D32F2F")) // 빨간색
            }
            
            // 작성자 및 메모
            vendor.text = "· 작성자 : ${transaction.vendor}"
            description.text = "· 메모 : ${transaction.description}"
        }
    }
}
