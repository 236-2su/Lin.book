package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.util.Log
import android.content.Intent
import android.app.AlertDialog
import android.widget.Button
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.ApiService.AccountResponse
import com.example.myapplication.api.ApiService.CreateAccountRequest
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Call
import com.example.myapplication.UserDetail
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone

class MeetingAccountFragment : Fragment() {

    private var clubPk: Int = -1

    companion object {
        private const val ARG_CLUB_PK = "club_pk"
        
        fun newInstance(clubPk: Int = -1): MeetingAccountFragment {
            val fragment = MeetingAccountFragment()
            val args = Bundle()
            args.putInt(ARG_CLUB_PK, clubPk)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            clubPk = it.getInt(ARG_CLUB_PK, -1)
            Log.d("MeetingAccountFragment", "onCreate - arguments에서 받은 clubPk: $clubPk")
        } ?: run {
            Log.e("MeetingAccountFragment", "onCreate - arguments가 null입니다!")
        }
    }
    
    // clubPk를 반환하는 메서드
    fun getClubPk(): Int = clubPk

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_meeting_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 디버깅: clubPk 값 확인
        Log.d("MeetingAccountFragment", "onViewCreated - clubPk: $clubPk")
        Log.d("MeetingAccountFragment", "onViewCreated - arguments 확인: ${arguments}")
        Log.d("MeetingAccountFragment", "onViewCreated - ARG_CLUB_PK: ${arguments?.getInt(ARG_CLUB_PK, -1)}")
        
        // 등록하기 버튼 숨기기
        hideRegisterButton()
        
        // 모임통장 버튼을 선택된 상태로 설정
        setMeetingAccountButtonSelected()
        
        // 연동하기 버튼 클릭 이벤트
        val btnLink = view.findViewById<LinearLayout>(R.id.btn_link)
        btnLink?.setOnClickListener {
            // TODO: 연동하기 기능 구현
        }
        
        // 개설하기 버튼 클릭 이벤트
        val btnCreate = view.findViewById<LinearLayout>(R.id.btn_create)
        btnCreate?.setOnClickListener {
            showAccountInfoDialog()
        }
    }
    
    // 등록하기 버튼 숨기기
    private fun hideRegisterButton() {
        // BaseActivity의 hideRegisterButton 메서드 호출
        try {
            val baseActivity = activity as? com.example.myapplication.BaseActivity
            baseActivity?.hideRegisterButton()
        } catch (e: Exception) {
            Log.e("MeetingAccountFragment", "BaseActivity 참조 오류", e)
        }
    }
    
    // 모임통장 버튼을 선택된 상태로 설정
    private fun setMeetingAccountButtonSelected() {
        try {
            val baseActivity = activity as? com.example.myapplication.BaseActivity
            val btnMeetingAccount = baseActivity?.findViewById<android.widget.TextView>(R.id.btn_meeting_account)
            if (btnMeetingAccount != null && baseActivity != null) {
                baseActivity.selectBoardButton(btnMeetingAccount)
                Log.d("MeetingAccountFragment", "모임통장 버튼 선택됨")
            } else {
                Log.e("MeetingAccountFragment", "모임통장 버튼을 찾을 수 없음")
            }
        } catch (e: Exception) {
            Log.e("MeetingAccountFragment", "모임통장 버튼 선택 오류", e)
        }
    }
    
    // 계좌 정보를 동아리별로 SharedPreferences에 저장
    private fun saveAccountInfo(accountNo: String, userName: String) {
        val sharedPrefs = requireContext().getSharedPreferences("club_accounts", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putString("account_no_$clubPk", accountNo)
            .putString("user_name_$clubPk", userName)
            .apply()
        Log.d("MeetingAccountFragment", "동아리 $clubPk 계좌 정보 저장됨: $accountNo, $userName")
    }
    
    // 통장 정보 팝업 다이얼로그 표시
    private fun showAccountInfoDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .create()
        
        // 다이얼로그 레이아웃 설정
        val dialogView = layoutInflater.inflate(R.layout.dialog_account_info, null)
        dialog.setView(dialogView)
        
        // 계좌 개설하기 버튼 클릭 이벤트
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm)
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            // 계좌 생성 API 호출
            createAccount()
        }
        
        // 다이얼로그 스타일 설정
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0.5f)
        
        // 다이얼로그 표시
        dialog.show()
        
        // 다이얼로그 크기 조정
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    
    // 계좌 생성 API 호출
    private fun createAccount() {
        // clubPk가 유효한지 확인
        if (clubPk <= 0) {
            Log.e("MeetingAccountFragment", "유효하지 않은 clubPk: $clubPk")
            showErrorDialog("오류", "동아리 정보를 찾을 수 없습니다.")
            return
        }
        
        // 사용자 ID 가져오기
        val userPk = UserManager.getUserPk(requireContext())
        if (userPk == null) {
            Log.e("MeetingAccountFragment", "사용자 정보를 찾을 수 없습니다")
            showErrorDialog("오류", "사용자 정보를 찾을 수 없습니다.")
            return
        }
        
        // API 호출
        callCreateAccountAPI(clubPk, userPk)
    }
    
    // 계좌 생성 API 호출
    private fun callCreateAccountAPI(clubPk: Int, userPk: Int) {
        Log.d("MeetingAccountFragment", "계좌 생성 API 호출 시작 - clubPk: $clubPk, userPk: $userPk")
        
        // API 요청 객체 생성
        val request = CreateAccountRequest(user_id = userPk)
        
        // API 호출 (club_pk는 Path 파라미터, user_id는 Body에 포함)
        ApiClient.getApiService().createClubAccount(clubPk, request).enqueue(object : Callback<AccountResponse> {
            override fun onResponse(
                call: Call<AccountResponse>,
                response: Response<AccountResponse>
            ) {
                if (response.isSuccessful) {
                    val accountResponse = response.body()
                    if (accountResponse != null && accountResponse.code != null) {
                        Log.d("MeetingAccountFragment", "계좌 생성 성공 - 계좌번호: ${accountResponse.code}")
                        
                        // 계좌번호(code)를 사용하여 다음 페이지로 이동
                        navigateToAccountCreatedPage(accountResponse.code)
                    } else {
                        Log.e("MeetingAccountFragment", "계좌번호를 받지 못했습니다")
                        showErrorDialog("계좌 생성 실패", "계좌번호를 받지 못했습니다.")
                    }
                } else {
                    Log.e("MeetingAccountFragment", "API 응답 오류: ${response.code()}")
                    showErrorDialog("계좌 생성 실패", "서버 오류가 발생했습니다. (코드: ${response.code()})")
                }
            }
            
            override fun onFailure(call: Call<AccountResponse>, t: Throwable) {
                Log.e("MeetingAccountFragment", "API 호출 실패", t)
                showErrorDialog("계좌 생성 실패", "네트워크 오류가 발생했습니다: ${t.message}")
            }
        })
    }
    
    // 계좌 생성 완료 페이지로 이동
    private fun navigateToAccountCreatedPage(accountNo: String) {
        Log.d("MeetingAccountFragment", "계좌 생성 완료, 계좌번호: $accountNo")
        
        // 현재 로그인한 사용자의 정보 가져오기
        val userPk = UserManager.getUserPk(requireContext())
        if (userPk != null) {
            ApiClient.getApiService().getUserDetail(userPk).enqueue(object : Callback<UserDetail> {
                override fun onResponse(
                    call: Call<UserDetail>,
                    response: Response<UserDetail>
                ) {
                    if (response.isSuccessful) {
                        val userDetail = response.body()
                        if (userDetail != null) {
                            // 계좌 정보를 SharedPreferences에 저장
                            saveAccountInfo(accountNo, userDetail.name ?: "사용자")
                            
                            // 사용자 이름과 계좌번호를 함께 전달
                            val intent = Intent(requireContext(), AccountCreatedActivity::class.java)
                            intent.putExtra("accountNo", accountNo)
                            intent.putExtra("userName", userDetail.name ?: "사용자")
                            startActivity(intent)
                        } else {
                            // 사용자 정보를 가져올 수 없는 경우 기본값으로 전달
                            saveAccountInfo(accountNo, "사용자")
                            val intent = Intent(requireContext(), AccountCreatedActivity::class.java)
                            intent.putExtra("accountNo", accountNo)
                            intent.putExtra("userName", "사용자")
                            startActivity(intent)
                        }
                    } else {
                        // API 호출 실패 시 기본값으로 전달
                        saveAccountInfo(accountNo, "사용자")
                        val intent = Intent(requireContext(), AccountCreatedActivity::class.java)
                        intent.putExtra("accountNo", accountNo)
                        intent.putExtra("userName", "사용자")
                        startActivity(intent)
                    }
                }
                
                override fun onFailure(
                    call: Call<UserDetail>,
                    t: Throwable
                ) {
                    Log.e("MeetingAccountFragment", "사용자 정보 가져오기 실패", t)
                    // 네트워크 오류 시 기본값으로 전달
                    saveAccountInfo(accountNo, "사용자")
                    val intent = Intent(requireContext(), AccountCreatedActivity::class.java)
                    intent.putExtra("accountNo", accountNo)
                    intent.putExtra("userName", "사용자")
                    startActivity(intent)
                }
            })
        } else {
            // userPk가 없는 경우 기본값으로 전달
            saveAccountInfo(accountNo, "사용자")
            val intent = Intent(requireContext(), AccountCreatedActivity::class.java)
            intent.putExtra("accountNo", accountNo)
            intent.putExtra("userName", "사용자")
            startActivity(intent)
        }
    }
    
    // 오류 다이얼로그 표시
    private fun showErrorDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
