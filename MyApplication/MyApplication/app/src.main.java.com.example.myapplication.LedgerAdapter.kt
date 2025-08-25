package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LedgerAdapter(
    private var ledgerItems: List<LedgerApiItem>
) : RecyclerView.Adapter<LedgerAdapter.LedgerViewHolder>() {

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
        ledgerItems = newItems
        notifyDataSetChanged()
    }

    inner class LedgerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ledgerName: TextView = itemView.findViewById(R.id.tv_ledger_name)

        fun bind(item: LedgerApiItem) {
            ledgerName.text = item.name
        }
    }
}
