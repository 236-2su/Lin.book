package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.ApiService
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class ClubMemberManagementActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var memberAdapter: MemberAdapter
    private lateinit var joinRequestAdapter: JoinRequestAdapter
    private val members = mutableListOf<Member>()
    private val joinRequests = mutableListOf<JoinRequest>()
    private var clubPk: Int = -1
    private var currentUserPk: Int = -1
    private var isShowingAll = true
    private var isCurrentUserPresident = false // 현재 사용자가 회장인지 여부

    companion object {
        const val EXTRA_CLUB_PK = "club_pk"
        const val EXTRA_USER_PK = "user_pk"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_member_management)

        // Intent에서 club_pk와 user_pk 받기
        clubPk = intent.getIntExtra(EXTRA_CLUB_PK, -1)
        currentUserPk = intent.getIntExtra(EXTRA_USER_PK, -1)
        
        Log.d("MEMBER_DEBUG", "받은 clubPk: $clubPk, currentUserPk: $currentUserPk")

        if (clubPk == -1) {
            Toast.makeText(this, "동아리 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        setupRecyclerView()
        setupSwipeToDelete()
        loadClubInfo()
        loadMembersFromApi()
    }

    private fun setupViews() {
        // 뒤로가기 버튼 설정
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 전체 탭은 기본 선택 상태 (현재 active 멤버들 표시)
        findViewById<TextView>(R.id.btnAll)?.setOnClickListener {
            if (!isShowingAll) {
                isShowingAll = true
                updateTabSelection()
                showAllMembers()
            }
        }

        // 가입요청 탭
        findViewById<TextView>(R.id.btnJoinRequest)?.setOnClickListener {
            if (isShowingAll) {
                isShowingAll = false
                updateTabSelection()
                loadWaitingMembers()
            }
        }
        
        // 기본 탭 선택 상태 설정
        updateTabSelection()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewMembers)
        
        // 멤버 어댑터 초기화
        memberAdapter = MemberAdapter(
            members,
            onDeleted = { deletedMember ->
                showDeletedSnackbar(deletedMember)
            },
            onRoleChange = { member ->
                // 권한 변경 다이얼로그 표시
                if (isCurrentUserPresident) {
                    showRoleChangeDialog(member)
                }
            }
        )
        
        // 가입 요청 어댑터 초기화
        joinRequestAdapter = JoinRequestAdapter(
            joinRequests,
            onApprove = { joinRequest ->
                approveMember(joinRequest)
            },
            onReject = { joinRequest ->
                rejectMember(joinRequest)
            }
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ClubMemberManagementActivity)
            adapter = memberAdapter // 기본은 멤버 어댑터
        }
    }

    private fun setupSwipeToDelete() {
        val swipeToDeleteCallback = SwipeToDeleteCallback(this) { position ->
            val deletedMember = members[position]
            
            // API 호출로 서버에서 멤버 삭제
            deleteMemberFromServer(deletedMember, position)
        }
        
        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun deleteMemberFromServer(member: Member, position: Int) {
        val apiService = ApiClient.getApiService()
        
        apiService.deleteMember(clubPk, member.id).enqueue(object : Callback<okhttp3.ResponseBody> {
            override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                if (response.isSuccessful) {
                    // 서버 삭제 성공 시 UI에서도 제거
                    memberAdapter.removeItem(position)
                    
                    Snackbar.make(recyclerView, "${member.name}님이 삭제되었습니다.", Snackbar.LENGTH_LONG)
                        .setAction("실행 취소") {
                            // 실행 취소 시에는 다시 서버에 복원 요청이 필요하지만,
                            // 현재는 UI만 복원 (서버 복원 API가 없는 경우)
                            memberAdapter.restoreItem(position, member)
                        }
                        .show()
                } else {
                    // 서버 삭제 실패 시 오류 메시지
                    Toast.makeText(this@ClubMemberManagementActivity, "멤버 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                // 네트워크 오류 시 오류 메시지
                Toast.makeText(this@ClubMemberManagementActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                Log.e("API_ERROR", "멤버 삭제 실패: ${t.message}")
            }
        })
    }

    private fun loadClubInfo() {
        val apiService = ApiClient.getApiService()
        
        apiService.getClubDetail(clubPk).enqueue(object : Callback<ClubItem> {
            override fun onResponse(call: Call<ClubItem>, response: Response<ClubItem>) {
                if (response.isSuccessful) {
                    val club = response.body()
                    if (club != null) {
                        findViewById<TextView>(R.id.tv_club_title).text = club.name
                    }
                }
            }

            override fun onFailure(call: Call<ClubItem>, t: Throwable) {
                Log.e("API_ERROR", "동아리 정보 조회 실패: ${t.message}")
            }
        })
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
                        Toast.makeText(this@ClubMemberManagementActivity, "멤버가 없습니다.", Toast.LENGTH_SHORT).show()
                        return
                    }
                    
                    // 2단계: 사용자 정보 조회
                    loadUserDetails(activeMembers)
                } else {
                    Log.e("API_ERROR", "멤버 목록 조회 실패: ${response.code()}")
                    Toast.makeText(this@ClubMemberManagementActivity, "멤버 목록을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<MemberResponse>>, t: Throwable) {
                Log.e("API_ERROR", "네트워크 오류: ${t.message}")
                Toast.makeText(this@ClubMemberManagementActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
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
                            val isCurrentUser = (memberResponse.user == currentUserPk)
                            Log.d("MEMBER_DEBUG", "멤버: ${user.name}, userId: ${memberResponse.user}, currentUserPk: $currentUserPk, isMe: $isCurrentUser")
                            
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
                                isMe = isCurrentUser
                            )
                            members.add(member)
                            
                            // 현재 사용자가 회장인지 확인
                            if (isCurrentUser && memberResponse.role == "leader") {
                                isCurrentUserPresident = true
                                // MemberAdapter에 회장 권한 전달
                                memberAdapter.setPresidentMode(true)
                            }
                        }
                    }
                    
                    // 정렬 순서: 본인(isMe) -> 회장(leader) -> 간부(officer) -> 일반(member)
                    members.sortBy { 
                        when {
                            it.isMe -> 0                    // 본인이 최우선 (최상단)
                            it.role == "leader" -> 1        // 회장이 두 번째
                            it.role == "officer" -> 2       // 간부가 세 번째
                            else -> 3                        // 일반 멤버가 네 번째
                        }
                    }
                    
                    // 권한에 따라 제목 텍스트 변경
                    updateTitleBasedOnRole()
                    
                    // 권한에 따라 가입 요청 탭 표시/숨김
                    updateJoinRequestTabVisibility()
                    
                    memberAdapter.notifyDataSetChanged()
                } else {
                    Log.e("API_ERROR", "사용자 목록 조회 실패: ${response.code()}")
                    Toast.makeText(this@ClubMemberManagementActivity, "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                Log.e("API_ERROR", "네트워크 오류: ${t.message}")
                Toast.makeText(this@ClubMemberManagementActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
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
    
    private fun updateTabSelection() {
        val btnAll = findViewById<TextView>(R.id.btnAll)
        val btnJoinRequest = findViewById<TextView>(R.id.btnJoinRequest)
        
        if (isShowingAll) {
            // 전체 탭 선택 - Me 스타일 적용
            btnAll?.apply {
                setTextAppearance(R.style.ReferenceSmallButton_Me)
                setBackgroundResource(R.drawable.btn_me_selected)
                setTextColor(resources.getColor(android.R.color.white, null))
            }
            // 가입 요청 탭 미선택 - 파란색 테두리
            btnJoinRequest?.apply {
                setTextAppearance(R.style.ReferenceSmallButton_All)
                setBackgroundResource(R.drawable.tab_unselected_border)
                setTextColor(resources.getColor(android.R.color.black, null))
            }
        } else {
            // 전체 탭 미선택 - 파란색 테두리
            btnAll?.apply {
                setTextAppearance(R.style.ReferenceSmallButton_All)
                setBackgroundResource(R.drawable.tab_unselected_border)
                setTextColor(resources.getColor(android.R.color.black, null))
            }
            // 가입 요청 탭 선택 - Me 스타일 적용
            btnJoinRequest?.apply {
                setTextAppearance(R.style.ReferenceSmallButton_Me)
                setBackgroundResource(R.drawable.btn_me_selected)
                setTextColor(resources.getColor(android.R.color.white, null))
            }
        }
    }
    
    private fun showAllMembers() {
        recyclerView.adapter = memberAdapter
        memberAdapter.notifyDataSetChanged()
    }
    
    private fun loadWaitingMembers() {
        val apiService = ApiClient.getApiService()
        
        // 가입 대기 멤버 목록 조회
        apiService.getWaitingMembers(clubPk).enqueue(object : Callback<List<MemberResponse>> {
            override fun onResponse(
                call: Call<List<MemberResponse>>,
                response: Response<List<MemberResponse>>
            ) {
                if (response.isSuccessful) {
                    val waitingList = response.body() ?: emptyList()
                    
                    if (waitingList.isEmpty()) {
                        joinRequests.clear()
                        recyclerView.adapter = joinRequestAdapter
                        joinRequestAdapter.notifyDataSetChanged()
                        Toast.makeText(this@ClubMemberManagementActivity, "가입 요청이 없습니다.", Toast.LENGTH_SHORT).show()
                        return
                    }
                    
                    // 사용자 정보 조회하여 이름 가져오기
                    loadUserDetailsForWaiting(waitingList)
                } else {
                    Log.e("API_ERROR", "가입 대기 목록 조회 실패: ${response.code()}")
                    Toast.makeText(this@ClubMemberManagementActivity, "가입 요청을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<MemberResponse>>, t: Throwable) {
                Log.e("API_ERROR", "네트워크 오류: ${t.message}")
                Toast.makeText(this@ClubMemberManagementActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun loadUserDetailsForWaiting(waitingMembers: List<MemberResponse>) {
        val apiService = ApiClient.getApiService()
        
        apiService.getUserList().enqueue(object : Callback<List<UserResponse>> {
            override fun onResponse(
                call: Call<List<UserResponse>>,
                response: Response<List<UserResponse>>
            ) {
                if (response.isSuccessful) {
                    val userList = response.body() ?: emptyList()
                    
                    // 가입 요청 리스트 생성
                    joinRequests.clear()
                    
                    for (memberResponse in waitingMembers) {
                        val user = userList.find { it.id == memberResponse.user }
                        if (user != null) {
                            val joinRequest = JoinRequest(
                                id = memberResponse.id,
                                userId = memberResponse.user,
                                name = user.name,
                                joinedAt = formatDate(memberResponse.joined_at),
                                status = memberResponse.status
                            )
                            joinRequests.add(joinRequest)
                        }
                    }
                    
                    // 가입 요청 날짜순으로 정렬 (최신순)
                    joinRequests.sortByDescending { it.id }
                    
                    // RecyclerView 어댑터 변경
                    recyclerView.adapter = joinRequestAdapter
                    joinRequestAdapter.notifyDataSetChanged()
                    
                    // 알림 배너 업데이트
                    val notificationBanner = findViewById<View>(R.id.notificationBanner)
                    notificationBanner?.visibility = View.VISIBLE
                } else {
                    Log.e("API_ERROR", "사용자 목록 조회 실패: ${response.code()}")
                    Toast.makeText(this@ClubMemberManagementActivity, "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                Log.e("API_ERROR", "네트워크 오류: ${t.message}")
                Toast.makeText(this@ClubMemberManagementActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 권한 변경 다이얼로그 표시
    private fun showRoleChangeDialog(member: Member) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_role_change, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroup_roles)
        val memberNameText = dialogView.findViewById<TextView>(R.id.tv_member_name)
        val warningContainer = dialogView.findViewById<View>(R.id.warning_container)
        
        // 멤버 이름 설정
        memberNameText.text = "${member.name}님"
        
        // 현재 권한에 따라 라디오 버튼 선택
        when (member.role) {
            "leader", "회장" -> radioGroup.check(R.id.radio_leader)
            "officer", "간부" -> radioGroup.check(R.id.radio_officer)
            else -> radioGroup.check(R.id.radio_member)
        }
        
        // 라디오 버튼 변경 리스너 - 회장 선택 시 경고 메시지 표시
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            warningContainer.visibility = if (checkedId == R.id.radio_leader && member.role != "leader" && member.role != "회장") {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        
        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setView(dialogView)
            .setPositiveButton("변경하기") { _, _ ->
                val selectedRadioId = radioGroup.checkedRadioButtonId
                val newRole = when (selectedRadioId) {
                    R.id.radio_leader -> "leader"
                    R.id.radio_officer -> "officer"
                    else -> "member"
                }
                
                // 현재 권한과 같으면 변경하지 않음
                val currentRole = when (member.role) {
                    "leader", "회장" -> "leader"
                    "officer", "간부" -> "officer"
                    else -> "member"
                }
                
                if (currentRole != newRole) {
                    updateMemberRole(member, newRole)
                }
            }
            .setNegativeButton("취소", null)
            .create()
        
        // 다이얼로그 배경 투명하게 설정
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
    
    // 권한 변경 API 호출
    private fun updateMemberRole(member: Member, newRole: String) {
        val apiService = ApiClient.getApiService()
        val request = ApiService.MemberRoleUpdateRequest(role = newRole)
        
        apiService.updateMemberRole(clubPk, member.id, request).enqueue(object : Callback<ApiService.MemberRoleUpdateResponse> {
            override fun onResponse(
                call: Call<ApiService.MemberRoleUpdateResponse>,
                response: Response<ApiService.MemberRoleUpdateResponse>
            ) {
                if (response.isSuccessful) {
                    // 성공 시 UI 업데이트
                    val roleText = when (newRole) {
                        "leader" -> "회장"
                        "officer" -> "간부"
                        else -> "부원"
                    }
                    
                    // 멤버 리스트에서 해당 멤버의 권한 업데이트
                    val index = members.indexOf(member)
                    if (index != -1) {
                        members[index] = member.copy(role = roleText)
                        memberAdapter.notifyItemChanged(index)
                    }
                    
                    Toast.makeText(this@ClubMemberManagementActivity, 
                        "${member.name}님의 권한이 ${roleText}(으)로 변경되었습니다.", 
                        Toast.LENGTH_SHORT).show()
                        
                    // 만약 회장으로 변경했다면, 현재 사용자는 일반 멤버가 됨
                    if (newRole == "leader") {
                        isCurrentUserPresident = false
                        // 현재 사용자 찾아서 부원으로 변경
                        val currentUserIndex = members.indexOfFirst { it.id == currentUserPk }
                        if (currentUserIndex != -1) {
                            members[currentUserIndex] = members[currentUserIndex].copy(role = "부원")
                            memberAdapter.notifyItemChanged(currentUserIndex)
                        }
                    }
                } else {
                    Log.e("API_ERROR", "권한 변경 실패: ${response.code()}")
                    Toast.makeText(this@ClubMemberManagementActivity, 
                        "권한 변경에 실패했습니다.", 
                        Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiService.MemberRoleUpdateResponse>, t: Throwable) {
                Log.e("API_ERROR", "네트워크 오류: ${t.message}")
                Toast.makeText(this@ClubMemberManagementActivity, 
                    "네트워크 오류가 발생했습니다.", 
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 멤버 승인 API 호출
    private fun approveMember(joinRequest: JoinRequest) {
        val apiService = ApiClient.getApiService()
        val request = ApiService.MemberApproveRequest(status = "active")
        
        apiService.approveMember(clubPk, joinRequest.id, request).enqueue(object : Callback<ApiService.MemberRoleUpdateResponse> {
            override fun onResponse(
                call: Call<ApiService.MemberRoleUpdateResponse>,
                response: Response<ApiService.MemberRoleUpdateResponse>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ClubMemberManagementActivity, 
                        "${joinRequest.name}님의 가입이 승인되었습니다.", 
                        Toast.LENGTH_SHORT).show()
                    
                    // 가입 요청 목록에서 제거
                    val index = joinRequests.indexOf(joinRequest)
                    if (index != -1) {
                        joinRequests.removeAt(index)
                        joinRequestAdapter.notifyItemRemoved(index)
                    }
                    
                    // 멤버 목록 새로고침 (새 멤버가 추가되었으므로)
                    loadMembersFromApi()
                } else {
                    Log.e("API_ERROR", "멤버 승인 실패: ${response.code()}")
                    Toast.makeText(this@ClubMemberManagementActivity, 
                        "멤버 승인에 실패했습니다.", 
                        Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiService.MemberRoleUpdateResponse>, t: Throwable) {
                Log.e("API_ERROR", "네트워크 오류: ${t.message}")
                Toast.makeText(this@ClubMemberManagementActivity, 
                    "네트워크 오류가 발생했습니다.", 
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 멤버 거절 API 호출
    private fun rejectMember(joinRequest: JoinRequest) {
        val apiService = ApiClient.getApiService()
        
        apiService.deleteMember(clubPk, joinRequest.id).enqueue(object : Callback<okhttp3.ResponseBody> {
            override fun onResponse(
                call: Call<okhttp3.ResponseBody>,
                response: Response<okhttp3.ResponseBody>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ClubMemberManagementActivity, 
                        "${joinRequest.name}님의 가입이 거절되었습니다.", 
                        Toast.LENGTH_SHORT).show()
                    
                    // 가입 요청 목록에서 제거
                    val index = joinRequests.indexOf(joinRequest)
                    if (index != -1) {
                        joinRequests.removeAt(index)
                        joinRequestAdapter.notifyItemRemoved(index)
                    }
                } else {
                    Log.e("API_ERROR", "멤버 거절 실패: ${response.code()}")
                    Toast.makeText(this@ClubMemberManagementActivity, 
                        "멤버 거절에 실패했습니다.", 
                        Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                Log.e("API_ERROR", "네트워크 오류: ${t.message}")
                Toast.makeText(this@ClubMemberManagementActivity, 
                    "네트워크 오류가 발생했습니다.", 
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 권한에 따라 제목 텍스트 업데이트
    private fun updateTitleBasedOnRole() {
        val titleTextView = findViewById<TextView>(R.id.tv_member_title)
        titleTextView.text = if (isCurrentUserPresident) "구성원 관리" else "구성원 목록"
    }
    
    // 권한에 따라 가입 요청 탭 표시/숨김
    private fun updateJoinRequestTabVisibility() {
        val btnJoinRequest = findViewById<TextView>(R.id.btnJoinRequest)
        btnJoinRequest.visibility = if (isCurrentUserPresident) View.VISIBLE else View.GONE
    }
}