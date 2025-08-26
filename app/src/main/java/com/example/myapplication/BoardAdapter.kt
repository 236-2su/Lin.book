package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class BoardAdapter(
    private val boardList: List<BoardItem>,
    private val onItemClick: (BoardItem) -> Unit
) : RecyclerView.Adapter<BoardAdapter.BoardViewHolder>() {

    class BoardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.tv_date)
        val titleText: TextView = itemView.findViewById(R.id.tv_title)
        val contentText: TextView = itemView.findViewById(R.id.tv_content)
        val viewsText: TextView = itemView.findViewById(R.id.tv_views)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_board, parent, false)
        return BoardViewHolder(view)
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {
        val board = boardList[position]
        
        holder.dateText.text = formatDate(board.created_at)
        holder.titleText.text = board.title
        holder.contentText.text = board.content
        holder.viewsText.text = "조회수 ${board.views}"
        
        holder.itemView.setOnClickListener {
            onItemClick(board)
        }
    }

    override fun getItemCount(): Int = boardList.size
    
    private fun formatDate(dateString: String): String {
        return try {
            // 1) 표준 ISO-8601(+09:00 또는 Z) 우선 처리
            val instant = try {
                java.time.OffsetDateTime.parse(dateString, java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .toInstant()
            } catch (_: Exception) {
                // 2) 타임존 표기가 없는 경우(마이크로초 포함) → UTC로 간주
                val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
                java.time.LocalDateTime.parse(dateString, fmt).atZone(java.time.ZoneOffset.UTC).toInstant()
            }
            val kst = instant.atZone(java.time.ZoneId.of("Asia/Seoul"))
            kst.format(
                java.time.format.DateTimeFormatter
                    .ofPattern("yyyy년 MM월 dd일(E) HH:mm")
                    .withLocale(Locale.KOREA)
            )
        } catch (e: Exception) {
            // 최후 수단: 기존 포맷터 시도
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                val outputFormat = SimpleDateFormat("yyyy년 MM월 dd일(E) HH:mm", Locale.KOREA)
                outputFormat.format(date ?: Date())
            } catch (_: Exception) { dateString }
        }
    }
}
