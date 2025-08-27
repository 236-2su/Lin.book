package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.api.ApiClient
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class MemberManagementActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var memberAdapter: MemberAdapter
    private val members = mutableListOf<Member>()
    private var clubPk: Int = -1
    private var currentUserPk: Int = -1

    companion object {
        const val EXTRA_CLUB_PK = "club_pk"
        const val EXTRA_USER_PK = "user_pk"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.member_management)

        // Intent에서 club_pk와 user_pk 받기
        clubPk = intent.getIntExtra(EXTRA_CLUB_PK, -1)
        currentUserPk = intent.getIntExtra(EXTRA_USER_PK, -1)

        if (clubPk == -1) {
            Toast.makeText(this, "동아리 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupRecyclerView()
        setupSwipeToDelete()
        loadMembersFromApi()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewMembers)
        memberAdapter = MemberAdapter(members) { deletedMember ->
            // 삭제된 멤버 처리 로직
            showDeletedSnackbar(deletedMember)
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MemberManagementActivity)
            adapter = memberAdapter
        }
    }

    private fun setupSwipeToDelete() {
        val swipeToDeleteCallback = SwipeToDeleteCallback(this) { position ->
            val deletedMember = members[position]
            memberAdapter.removeItem(position)
            
            // Snackbar로 실행 취소 옵션 제공
            Snackbar.make(recyclerView, "${deletedMember.name}님이 삭제되었습니다.", Snackbar.LENGTH_LONG)
                .setAction("실행 취소") {
                    memberAdapter.restoreItem(position, deletedMember)
                }
                .show()
        }
        
        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun loadMembersFromApi() {
        val apiService = ApiClient.getApiService()
        
        // 1단계: 멤버 목록 조회
        apiService.getClubMembers(clubPk).enqueue(object : Callback<List<MemberResponse>> {
            override fun onResponse(
                call: Call<List<MemberResponse>>,
                response: Response<List<MemberResponse>>
            ) {
                if (response.isSuccessful) {
                    val memberList = response.body() ?: emptyList()
                    
                    // active 멤버만 필터링
                    val activeMembers = memberList.filter { it.status == "active" }
                    
                    if (activeMembers.isEmpty()) {
                        Toast.makeText(this@MemberManagementActivity, "멤버가 없습니다.", Toast.LENGTH_SHORT).show()
                        return
                    }
                    
                    // 2단계: 사용자 정보 조회
                    loadUserDetails(activeMembers)
                } else {
                    Log.e("API_ERROR", "멤버 목록 조회 실패: ${response.code()}")
                    Toast.makeText(this@MemberManagementActivity, "멤버 목록을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<MemberResponse>>, t: Throwable) {
                Log.e("API_ERROR", "네트워크 오류: ${t.message}")
                Toast.makeText(this@MemberManagementActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserDetails(memberResponses: List<MemberResponse>) {
        val apiService = ApiClient.getApiService()
        
        apiService.getUserList().enqueue(object : Callback<List<UserResponse>> {
            override fun onResponse(
                call: Call<List<UserResponse>>,
                response: Response<List<UserResponse>>
            ) {
                if (response.isSuccessful) {
                    val userList = response.body() ?: emptyList()
                    
                    // 멤버와 사용자 정보 매칭
                    members.clear()
                    
                    for (memberResponse in memberResponses) {
                        val user = userList.find { it.id == memberResponse.user }
                        if (user != null) {
                            val member = Member(
                                id = memberResponse.id,
                                userId = memberResponse.user,
                                name = user.name,
                                role = memberResponse.role,
                                status = memberResponse.status,
                                department = user.major,
                                studentNumber = user.student_number,
                                phone = user.phone_number,
                                joinDate = formatDate(memberResponse.joined_at),
                                isMe = (memberResponse.user == currentUserPk)
                            )
                            members.add(member)
                        }
                    }
                    
                    // role 순서대로 정렬 (leader 먼저, 그 다음 member)
                    members.sortBy { 
                        when(it.role) {
                            "leader" -> 0
                            else -> 1
                        }
                    }
                    
                    memberAdapter.notifyDataSetChanged()
                } else {
                    Log.e("API_ERROR", "사용자 목록 조회 실패: ${response.code()}")
                    Toast.makeText(this@MemberManagementActivity, "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                Log.e("API_ERROR", "네트워크 오류: ${t.message}")
                Toast.makeText(this@MemberManagementActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: return dateString)
        } catch (e: Exception) {
            dateString
        }
    }

    private fun showDeletedSnackbar(member: Member) {
        // 추가적인 삭제 후 처리 로직
    }
}