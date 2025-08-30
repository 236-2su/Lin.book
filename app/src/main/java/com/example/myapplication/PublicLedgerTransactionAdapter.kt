package com.example.myapplication

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PublicLedgerTransactionAdapter(
    private var transactions: List<TransactionItem>
) : RecyclerView.Adapter<PublicLedgerTransactionAdapter.TransactionViewHolder>() {

    private var onItemClickListener: ((Int) -> Unit)? = null

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction_public_ledger, parent, false)
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
     
     fun getCurrentList(): List<TransactionItem> {
         return transactions
     }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val type: TextView = itemView.findViewById(R.id.tv_transaction_type)
        private val category: TextView = itemView.findViewById(R.id.tv_transaction_category)
        private val date: TextView = itemView.findViewById(R.id.tv_date)
        private val amount: TextView = itemView.findViewById(R.id.tv_amount)
        private val author: TextView = itemView.findViewById(R.id.tv_author)
        private val memo: TextView = itemView.findViewById(R.id.tv_memo)
        private val receipt: ImageView = itemView.findViewById(R.id.iv_receipt)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(position)
                }
            }
        }

        fun bind(transaction: TransactionItem) {
            // 거래 타입 설정 (수입/지출) - amount 값에 따라 결정
            if (transaction.amount > 0) {
                type.text = "수입"
                type.setBackgroundResource(R.drawable.btn_income_background)
                type.setTextColor(Color.parseColor("#2457C5"))
                amount.text = "+ ${NumberFormat.getNumberInstance(Locale.getDefault()).format(transaction.amount)}원"
                amount.setTextColor(Color.parseColor("#2457C5"))
            } else {
                type.text = "지출"
                type.setBackgroundResource(R.drawable.btn_expense_background)
                type.setTextColor(Color.parseColor("#B70000"))
                amount.text = "- ${NumberFormat.getNumberInstance(Locale.getDefault()).format(-transaction.amount)}원"
                amount.setTextColor(Color.parseColor("#C50000"))
            }

            // 카테고리 설정 - category 필드 사용, 없으면 type 필드 사용 (type이 null이면 표시하지 않음)
            val categoryText = transaction.category ?: transaction.type
            if (!categoryText.isNullOrEmpty()) {
                category.visibility = View.VISIBLE
                category.text = categoryText
                category.setBackgroundResource(R.drawable.btn_vice_president)
                category.setTextColor(Color.parseColor("#4F4F4F"))
            } else {
                category.visibility = View.GONE
            }

            // 날짜 포맷팅 - dateTime 필드 사용
            try {
                if (transaction.dateTime != null) {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val dateObj = inputFormat.parse(transaction.dateTime)
                    val outputFormat = SimpleDateFormat("yyyy년 MM월 dd일 EEEE HH:mm", Locale.KOREAN)
                    date.text = outputFormat.format(dateObj!!)
                } else {
                    date.text = "날짜 정보 없음"
                }
            } catch (e: Exception) {
                // 날짜 파싱 실패 시 원본 날짜 표시
                date.text = transaction.dateTime ?: "날짜 정보 없음"
            }

            // 거래처명 설정 - vendor 필드 사용
            author.text = "ㆍ거래처명 : ${transaction.vendor}"

            // 메모 설정 - description 필드 사용 (한 줄로 제한)
            val memoText = transaction.description
            if (memoText.length > 20) {
                memo.text = "ㆍ메모: ${memoText.substring(0, 20)}..."
            } else {
                memo.text = "ㆍ메모: $memoText"
            }

            // 영수증 이미지 표시 (있는 경우)
            if (!transaction.receipt.isNullOrEmpty()) {
                receipt.visibility = View.VISIBLE
                // TODO: Glide나 Picasso를 사용하여 이미지 로드
                // Glide.with(itemView.context).load(transaction.receipt).into(receipt)
            } else {
                receipt.visibility = View.GONE
            }
        }
    }
}
