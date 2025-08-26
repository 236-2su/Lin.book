package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventLedgerAdapter(
    private val events: List<EventItem>
) : RecyclerView.Adapter<EventLedgerAdapter.EventLedgerViewHolder>() {

    class EventLedgerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.tv_event_name)
        val periodTextView: TextView = view.findViewById(R.id.tv_event_period)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventLedgerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_ledger, parent, false)
        return EventLedgerViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventLedgerViewHolder, position: Int) {
        val event = events[position]
        holder.nameTextView.text = event.name
        holder.periodTextView.text = "행사 예정 기간: ${event.start_date} ~ ${event.end_date}"
    }

    override fun getItemCount(): Int = events.size
}