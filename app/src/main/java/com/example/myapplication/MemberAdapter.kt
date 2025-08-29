package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Member(
    val id: Int,
    val userId: Int,
    val name: String,
    val role: String,  // "leader" or "member" from API
    val status: String, // "active" or "waiting"
    val department: String,
    val studentNumber: String,
    val phone: String,
    val joinDate: String,
    val isMe: Boolean = false
)

// API Response 모델들
data class MemberResponse(
    val id: Int,
    val status: String,
    val role: String,
    val joined_at: String,
    val amount_fee: Int,
    val paid_fee: Int,
    val club: Int,
    val user: Int
)

data class UserResponse(
    val id: Int,
    val name: String,
    val email: String,
    val student_number: String,
    val major: String,
    val admission_year: Int,
    val phone_number: String,
    val status: String,
    val profile_url_image: String,
    val user_key: String
)

class MemberAdapter(
    private val members: MutableList<Member>,
    private val onDeleted: (Member) -> Unit,
    private val onRoleChange: (Member) -> Unit
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {
    
    private var isPresidentMode = false // 현재 사용자가 회장인지 여부
    
    fun setPresidentMode(isPresident: Boolean) {
        isPresidentMode = isPresident
        notifyDataSetChanged()
    }

    inner class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val meBadge: TextView = itemView.findViewById(R.id.meBadge)
        private val roleBadge: TextView = itemView.findViewById(R.id.roleBadge)
        private val memberName: TextView = itemView.findViewById(R.id.memberName)
        private val memberInfo: TextView = itemView.findViewById(R.id.memberInfo)
        private val memberPhone: TextView = itemView.findViewById(R.id.memberPhone)
        private val joinDate: TextView = itemView.findViewById(R.id.joinDate)
        private val roleChangeButton: TextView = itemView.findViewById(R.id.roleChangeButton)
        private val actionButtons: View = itemView.findViewById(R.id.actionButtons)

        fun bind(member: Member) {
            memberName.text = member.name
            memberInfo.text = "${member.department} / ${member.studentNumber}"
            memberPhone.text = member.phone
            joinDate.text = "가입일: ${member.joinDate}"

            // Me 배지 표시 여부 및 테두리 설정
            if (member.isMe) {
                meBadge.visibility = View.VISIBLE
                // Me 배지와 같은 색상의 테두리 적용
                itemView.setBackgroundResource(R.drawable.border_me)
            } else {
                meBadge.visibility = View.GONE
                // 테두리 제거
                itemView.setBackgroundResource(android.R.color.transparent)
            }

            // 역할에 따른 배지 텍스트 설정
            when (member.role) {
                "leader", "회장" -> {
                    roleBadge.text = "회장"
                }
                "officer", "간부" -> {
                    roleBadge.text = "간부"
                }
                "member", "일반", "부원" -> {
                    roleBadge.text = "일반"
                }
                else -> {
                    roleBadge.text = "일반"
                }
            }

            // 권한 변경 버튼 표시 및 클릭 리스너 설정
            if (member.isMe || !isPresidentMode) {
                // 본인이거나 회장이 아닌 경우 권한 변경 버튼 숨기기
                roleChangeButton.visibility = View.GONE
            } else {
                roleChangeButton.visibility = View.VISIBLE
                roleChangeButton.setOnClickListener {
                    onRoleChange(member)
                }
            }
            
            // Me인 경우 액션 버튼 숨기기
            if (member.isMe) {
                actionButtons.visibility = View.GONE
            } else {
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
            onDeleted(deletedMember)
        }
    }

    fun restoreItem(position: Int, member: Member) {
        members.add(position, member)
        notifyItemInserted(position)
    }
}