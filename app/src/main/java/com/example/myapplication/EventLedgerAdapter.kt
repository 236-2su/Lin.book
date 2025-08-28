package com.example.myapplication

import android.content.Intent
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
        
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ClubEventLedgerDetailActivity::class.java).apply {
                putExtra("club_pk", (context as ClubEventLedgerListActivity).intent.getIntExtra(ClubEventLedgerListActivity.EXTRA_CLUB_PK, -1))
                putExtra("event_name", event.name)
                putExtra("event_start_date", event.start_date)
                putExtra("event_end_date", event.end_date)
                putExtra("event_pk", event.id)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = events.size
}