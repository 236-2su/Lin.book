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
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_meeting_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 등록하기 버튼 숨기기
        hideRegisterButton()
        
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
        // 현재 날짜와 시간 가져오기 (한국 시간대 명시)
        val koreaTimeZone = TimeZone.getTimeZone("Asia/Seoul")
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).apply { timeZone = koreaTimeZone }
        val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault()).apply { timeZone = koreaTimeZone }
        
        val currentDate = dateFormat.format(Date())
        val currentTime = timeFormat.format(Date())
        
        // 시스템 시간과 비교를 위한 로그
        val systemDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val systemTime = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
        
        // 랜덤 난수 생성 (20자리)
        val randomNumber = generateUnique20DigitNumber()
        
        // 디버깅을 위한 로그 출력
        Log.d("MeetingAccountFragment", "생성된 데이터:")
        Log.d("MeetingAccountFragment", "한국시간 날짜: $currentDate")
        Log.d("MeetingAccountFragment", "한국시간 시간: $currentTime")
        Log.d("MeetingAccountFragment", "시스템시간 날짜: $systemDate")
        Log.d("MeetingAccountFragment", "시스템시간 시간: $systemTime")
        Log.d("MeetingAccountFragment", "랜덤번호: $randomNumber (길이: ${randomNumber.length})")
        
        // API 요청 데이터 구성
        val requestData = mapOf(
            "Header" to mapOf(
                "apiName" to "createDemandDepositAccount",
                "transmissionDate" to currentDate,
                "transmissionTime" to currentTime,
                "institutionCode" to "00100",
                "fintechAppNo" to "001",
                "apiServiceCode" to "createDemandDepositAccount",
                "institutionTransactionUniqueNo" to randomNumber,
                "apiKey" to "7f9fc447584741399a5dfab7dd3ea443",
                "UserKey" to "1607a094-72cc-4d4f-9ed3-e7cd1d264e2d"
            ),
            "Body" to mapOf(
                "accountTypeUniqueNo" to "088-1-64e152b919e94d"
            )
        )
        
        // 전체 요청 데이터 로그 출력
        Log.d("MeetingAccountFragment", "API 요청 데이터: ${Gson().toJson(requestData)}")
        
        // API 호출
        callCreateAccountAPI(requestData)
    }
    
    // 계좌 생성 API 호출
    private fun callCreateAccountAPI(requestData: Map<String, Any>) {
        Log.d("MeetingAccountFragment", "계좌 생성 API 호출 시작")
        
        // 백그라운드 스레드에서 API 호출
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/createDemandDepositAccount"
                val connection = URL(url).openConnection() as HttpURLConnection
                
                // POST 요청 설정
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                
                // JSON 데이터 전송
                val jsonData = Gson().toJson(requestData)
                connection.outputStream.use { os ->
                    os.write(jsonData.toByteArray())
                }
                
                // 응답 받기
                val responseCode = connection.responseCode
                Log.d("MeetingAccountFragment", "API 응답 코드: $responseCode")
                
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("MeetingAccountFragment", "API 응답: $response")
                    
                                         // JSON 응답에서 accountNo 추출
                     val responseJson = Gson().fromJson(response, Map::class.java)
                     val accountNo = responseJson["accountNo"]?.toString()
                    
                    if (accountNo != null) {
                                                 // 메인 스레드에서 다음 페이지로 이동
                         withContext(Dispatchers.Main) {
                             navigateToAccountCreatedPage(accountNo.toString())
                         }
                    } else {
                        Log.e("MeetingAccountFragment", "accountNo를 찾을 수 없음")
                        showErrorDialog("계좌 생성 실패", "계좌 번호를 받지 못했습니다.")
                    }
                } else {
                    Log.e("MeetingAccountFragment", "API 호출 실패: $responseCode")
                    withContext(Dispatchers.Main) {
                        showErrorDialog("계좌 생성 실패", "서버 오류가 발생했습니다. (코드: $responseCode)")
                    }
                }
                
            } catch (e: Exception) {
                Log.e("MeetingAccountFragment", "API 호출 중 오류 발생", e)
                withContext(Dispatchers.Main) {
                    showErrorDialog("계좌 생성 실패", "네트워크 오류가 발생했습니다: ${e.message}")
                }
            }
        }
    }
    
    // 계좌 생성 완료 페이지로 이동
    private fun navigateToAccountCreatedPage(accountNo: String) {
        Log.d("MeetingAccountFragment", "계좌 생성 완료, 계좌번호: $accountNo")
        
        // AccountCreatedActivity로 이동
        val intent = Intent(requireContext(), AccountCreatedActivity::class.java)
        intent.putExtra("accountNo", accountNo)
        startActivity(intent)
    }
    
    // 20자리 고유 난수 생성
    private fun generateUnique20DigitNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1..999999L).random()
        val combined = "$timestamp$random"
        
        // 20자리로 맞추기
        val result = if (combined.length >= 20) {
            combined.takeLast(20)
        } else {
            combined.padStart(20, '0')
        }
        
        Log.d("MeetingAccountFragment", "난수 생성 상세:")
        Log.d("MeetingAccountFragment", "타임스탬프: $timestamp (${timestamp.toString().length}자리)")
        Log.d("MeetingAccountFragment", "랜덤: $random (${random.toString().length}자리)")
        Log.d("MeetingAccountFragment", "결합: $combined (${combined.length}자리)")
        Log.d("MeetingAccountFragment", "최종결과: $result (${result.length}자리)")
        
        return result
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
