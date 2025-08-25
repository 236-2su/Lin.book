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
            // ISO 8601 형식의 날짜를 파싱
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            
            // 한국어 형식으로 출력
            val outputFormat = SimpleDateFormat("yyyy년 MM월 dd일(E) HH:mm", Locale.KOREA)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            // 파싱 실패 시 원본 문자열 반환
            dateString
        }
    }
}
