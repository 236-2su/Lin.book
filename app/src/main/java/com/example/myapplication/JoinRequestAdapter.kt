package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class JoinRequest(
    val id: Int,
    val userId: Int,
    val name: String,
    val joinedAt: String,
    val status: String = "waiting",
    val department: String = "",
    val studentNumber: String = ""
)

class JoinRequestAdapter(
    private val joinRequests: MutableList<JoinRequest>,
    private val onApprove: (JoinRequest) -> Unit,
    private val onReject: (JoinRequest) -> Unit
) : RecyclerView.Adapter<JoinRequestAdapter.JoinRequestViewHolder>() {

    class JoinRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRequestDate: TextView = itemView.findViewById(R.id.tv_request_date)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val tvMemberInfo: TextView = itemView.findViewById(R.id.tv_member_info)
        private val btnApprove: TextView = itemView.findViewById(R.id.btn_approve)
        private val btnReject: TextView = itemView.findViewById(R.id.btn_reject)

        fun bind(joinRequest: JoinRequest, onApprove: (JoinRequest) -> Unit, onReject: (JoinRequest) -> Unit) {
            tvRequestDate.text = "가입 요청일: ${joinRequest.joinedAt}"
            tvName.text = joinRequest.name
            tvMemberInfo.text = "${joinRequest.department} / ${joinRequest.studentNumber}"

            btnApprove.setOnClickListener {
                onApprove(joinRequest)
            }

            btnReject.setOnClickListener {
                onReject(joinRequest)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JoinRequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_join_request, parent, false)
        return JoinRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: JoinRequestViewHolder, position: Int) {
        holder.bind(joinRequests[position], onApprove, onReject)
    }

    override fun getItemCount(): Int = joinRequests.size

    fun updateData(newRequests: List<JoinRequest>) {
        joinRequests.clear()
        joinRequests.addAll(newRequests)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        if (position >= 0 && position < joinRequests.size) {
            joinRequests.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}