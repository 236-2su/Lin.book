package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Member(
    val id: Int,
    val name: String,
    val role: String,
    val department: String,
    val studentId: String,
    val phone: String,
    val joinDate: String,
    val isMe: Boolean = false
)

class MemberAdapter(
    private val members: MutableList<Member>,
    private val onMemberDeleted: (Member) -> Unit
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val roleBadge: TextView = itemView.findViewById(R.id.roleBadge)
        private val memberName: TextView = itemView.findViewById(R.id.memberName)
        private val memberInfo: TextView = itemView.findViewById(R.id.memberInfo)
        private val memberPhone: TextView = itemView.findViewById(R.id.memberPhone)
        private val joinDate: TextView = itemView.findViewById(R.id.joinDate)
        private val roleChangeButton: TextView = itemView.findViewById(R.id.roleChangeButton)
        private val actionButtons: View = itemView.findViewById(R.id.actionButtons)

        fun bind(member: Member) {
            memberName.text = member.name
            memberInfo.text = "${member.department} / ${member.studentId}"
            memberPhone.text = member.phone
            joinDate.text = "가입일: ${member.joinDate}"

            // 역할에 따른 배지 스타일 설정
            when (member.role) {
                "회장" -> {
                    if (member.isMe) {
                        roleBadge.text = "Me"
                        roleBadge.setBackgroundResource(R.drawable.btn_me_selected)
                        roleBadge.setTextColor(android.graphics.Color.WHITE)
                    } else {
                        roleBadge.text = "회장"
                        roleBadge.setBackgroundResource(R.drawable.btn_vice_president)
                        roleBadge.setTextColor(android.graphics.Color.parseColor("#333333"))
                    }
                }
                "부회장" -> {
                    roleBadge.text = "부회장"
                    roleBadge.setBackgroundResource(R.drawable.btn_vice_president)
                    roleBadge.setTextColor(android.graphics.Color.parseColor("#333333"))
                }
                "일반" -> {
                    roleBadge.text = "일반"
                    roleBadge.setBackgroundResource(R.drawable.btn_general)
                    roleBadge.setTextColor(android.graphics.Color.parseColor("#333333"))
                }
            }

            // Me인 경우 권한 변경과 액션 버튼 숨기기
            if (member.isMe) {
                roleChangeButton.visibility = View.GONE
                actionButtons.visibility = View.GONE
            } else {
                roleChangeButton.visibility = View.VISIBLE
                actionButtons.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_single_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(members[position])
    }

    override fun getItemCount(): Int = members.size

    fun removeItem(position: Int) {
        if (position >= 0 && position < members.size) {
            val deletedMember = members[position]
            members.removeAt(position)
            notifyItemRemoved(position)
            onMemberDeleted(deletedMember)
        }
    }

    fun restoreItem(position: Int, member: Member) {
        members.add(position, member)
        notifyItemInserted(position)
    }
}