package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LedgerAdapter(
    private var ledgerItems: MutableList<LedgerApiItem> // 수정 가능한 MutableList로 변경
) : RecyclerView.Adapter<LedgerAdapter.LedgerViewHolder>() {

    // 클릭 리스너 인터페이스 정의
    interface OnItemClickListener {
        fun onItemClick(ledger: LedgerApiItem)
    }

    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LedgerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ledger_box, parent, false)
        return LedgerViewHolder(view)
    }

    override fun onBindViewHolder(holder: LedgerViewHolder, position: Int) {
        val item = ledgerItems[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = ledgerItems.size

    fun updateData(newItems: List<LedgerApiItem>) {
        ledgerItems.clear()
        ledgerItems.addAll(newItems)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        if (position in 0 until ledgerItems.size) {
            ledgerItems.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    inner class LedgerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ledgerName: TextView = itemView as TextView

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onItemClick(ledgerItems[position])
                }
            }
        }

        fun bind(item: LedgerApiItem) {
            ledgerName.text = item.name
        }
    }
}
