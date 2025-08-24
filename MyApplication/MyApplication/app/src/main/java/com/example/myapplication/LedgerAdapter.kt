package com.example.myapplication

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class LedgerAdapter(
    private val context: Context,
    private val ledgerItems: MutableList<LedgerItem>
) : RecyclerView.Adapter<LedgerAdapter.LedgerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LedgerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ledger_transaction, parent, false)
        return LedgerViewHolder(view)
    }

    override fun onBindViewHolder(holder: LedgerViewHolder, position: Int) {
        val item = ledgerItems[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = ledgerItems.size

    fun removeItem(position: Int) {
        ledgerItems.removeAt(position)
        notifyItemRemoved(position)
    }

    inner class LedgerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tagsContainer: LinearLayout = itemView.findViewById(R.id.tags_container)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tv_author)
        private val tvMemo: TextView = itemView.findViewById(R.id.tv_memo)
        private val ivReceipt: ImageView = itemView.findViewById(R.id.iv_receipt)

        fun bind(item: LedgerItem) {
            tvDate.text = item.date
            tvAmount.text = item.amount
            tvAuthor.text = item.author
            tvMemo.text = item.memo

            // 수입/지출에 따른 금액 색상 변경
            if (item.type == "수입") {
                tvAmount.setTextColor(Color.parseColor("#2457C5"))
            } else {
                tvAmount.setTextColor(Color.parseColor("#C50000"))
            }

            // 영수증 이미지 표시 여부
            ivReceipt.visibility = if (item.hasReceipt) View.VISIBLE else View.GONE

            // 태그 동적 추가
            tagsContainer.removeAllViews()
            item.tags.forEachIndexed { index, tag ->
                val tagView = LayoutInflater.from(context).inflate(R.layout.item_tag, tagsContainer, false) as TextView
                tagView.text = tag

                // 첫 번째 태그는 수입/지출 스타일에 따라
                if (index == 0) {
                    if (item.type == "수입") {
                        tagView.background = ContextCompat.getDrawable(context, R.drawable.btn_income_background)
                        tagView.setTextColor(Color.parseColor("#2457C5"))
                    } else {
                        tagView.background = ContextCompat.getDrawable(context, R.drawable.btn_expense_background)
                        tagView.setTextColor(Color.parseColor("#C50000"))
                    }
                } else { // 두 번째 태그부터는 회색 스타일
                    tagView.background = ContextCompat.getDrawable(context, R.drawable.btn_general_background)
                     tagView.setTextColor(Color.parseColor("#666666"))
                }
                tagsContainer.addView(tagView)
            }
        }
    }
}
