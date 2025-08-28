package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventLedgerAdapter(
    private val events: List<EventItem>
) : RecyclerView.Adapter<EventLedgerAdapter.EventLedgerViewHolder>() {

    // 5가지 배경 drawable 리소스 배열
    private val backgroundDrawables = intArrayOf(
        R.drawable.card_box_light_blue, // 연한 파랑
        R.drawable.card_box_pink,       // 연한 빨강
        R.drawable.card_box_yellow,     // 연한 노랑
        R.drawable.card_box_green,      // 연한 초록
        R.drawable.card_box_purple      // 연한 보라
    )

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
        
        // 이벤트 ID를 기반으로 일관성 있는 배경 drawable 적용
        val drawableIndex = event.id % backgroundDrawables.size
        val backgroundDrawable = backgroundDrawables[drawableIndex]
        holder.itemView.setBackgroundResource(backgroundDrawable)
        
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            android.util.Log.d("EventLedgerAdapter", "아이템 클릭됨 - 이벤트: ${event.name}")
            
            val intent = Intent(context, ClubEventLedgerDetailActivity::class.java).apply {
                putExtra("club_pk", (context as ClubEventLedgerListActivity).intent.getIntExtra(ClubEventLedgerListActivity.EXTRA_CLUB_PK, -1))
                putExtra("event_name", event.name)
                putExtra("event_start_date", event.start_date)
                putExtra("event_end_date", event.end_date)
                putExtra("event_pk", event.id)
            }
            
            android.util.Log.d("EventLedgerAdapter", "Intent: $intent")
            android.util.Log.d("EventLedgerAdapter", "ClubEventLedgerDetailActivity로 이동 시작")
            
            try {
                context.startActivity(intent)
                android.util.Log.d("EventLedgerAdapter", "Activity 시작 성공")
            } catch (e: Exception) {
                android.util.Log.e("EventLedgerAdapter", "Activity 시작 실패: ${e.message}")
            }
        }
    }

    override fun getItemCount(): Int = events.size
}