package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.api.Comment
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(
    private var comments: List<Comment>,
    private val currentUserMemberPk: Int?,
    private val onDeleteClick: (Comment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAuthor: TextView = itemView.findViewById(R.id.tv_comment_author)
        val tvMajor: TextView = itemView.findViewById(R.id.tv_comment_major)
        val tvTime: TextView = itemView.findViewById(R.id.tv_comment_time)
        val tvContent: TextView = itemView.findViewById(R.id.tv_comment_content)
        val btnDelete: TextView = itemView.findViewById(R.id.btn_delete_comment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        
        holder.tvAuthor.text = comment.author_name
        holder.tvMajor.text = comment.author_major
        holder.tvContent.text = comment.content
        
        val displayTimeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.KOREA)
        val date = parseDateWithMultipleFormats(comment.created_at)
        holder.tvTime.text = if (date != null) {
            displayTimeFormat.format(date)
        } else {
            "시간 오류"
        }

        if (currentUserMemberPk != null && comment.author == currentUserMemberPk) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener {
                onDeleteClick(comment)
            }
        } else {
            holder.btnDelete.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = comments.size

    fun updateComments(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }

    private fun parseDateWithMultipleFormats(dateString: String): Date? {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        )

        for (format in formats) {
            try {
                return format.parse(dateString)
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }
}