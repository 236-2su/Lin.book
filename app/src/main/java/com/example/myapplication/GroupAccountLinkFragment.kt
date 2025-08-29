package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.LinearLayout
import android.widget.TextView
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

class GroupAccountLinkFragment : Fragment() {

    companion object {
        fun newInstance(): GroupAccountLinkFragment {
            return GroupAccountLinkFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.group_account_link_or_create_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 등록하기 버튼 숨기기
        hideRegisterButton(view)
        
        // 추가로 모든 등록하기 버튼 찾아서 숨기기
        hideAllRegisterButtons(view)
        
        // 안내사항 텍스트에 굵은 글씨 적용
        applyBoldTextToGuidelines(view)
        
        // 연동하기 버튼 클릭 이벤트
        val btnLink = view.findViewById<LinearLayout>(R.id.btn_link)
        btnLink?.setOnClickListener {
            // TODO: 연동하기 기능 구현
            Toast.makeText(requireContext(), "연동하기 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }
        
        // 개설하기 버튼 클릭 이벤트
        val btnCreate = view.findViewById<LinearLayout>(R.id.btn_create)
        btnCreate?.setOnClickListener {
            showAccountInfoDialog()
        }
    }
    
    // 등록하기 버튼 숨기기
    private fun hideRegisterButton(view: View) {
        try {
            // BaseActivity의 등록하기 버튼 숨기기
            val baseActivity = activity as? BaseActivity
            baseActivity?.hideRegisterButton()
            
            // Fragment 내부의 등록하기 버튼도 숨기기 (있다면)
            val registerButton = view.findViewById<View>(R.id.btn_register)
            registerButton?.visibility = View.GONE
            
        } catch (e: Exception) {
            Log.e("GroupAccountLinkFragment", "등록하기 버튼 숨기기 오류", e)
        }
    }
    
    // 모든 등록하기 버튼을 찾아서 숨기기
    private fun hideAllRegisterButtons(view: View) {
        try {
            // 1. Fragment 내부의 모든 등록하기 버튼 찾기
            val registerButton = view.findViewById<View>(R.id.btn_register)
            registerButton?.visibility = View.GONE
            
            // 2. Activity 전체에서 등록하기 버튼 찾기
            activity?.findViewById<View>(R.id.btn_register)?.visibility = View.GONE
            
            // 3. content_container 내부의 모든 등록하기 버튼 찾기
            val contentContainer = activity?.findViewById<View>(R.id.content_container)
            contentContainer?.findViewById<View>(R.id.btn_register)?.visibility = View.GONE
            
            // 4. root_page.xml의 모든 등록하기 버튼 찾기
            val rootView = activity?.findViewById<View>(android.R.id.content)
            rootView?.findViewById<View>(R.id.btn_register)?.visibility = View.GONE
            
            // 5. 모든 하위 View에서 등록하기 버튼 찾기 (재귀적 검색)
            hideRegisterButtonRecursively(view)
            
            Log.d("GroupAccountLinkFragment", "모든 등록하기 버튼 숨김 완료")
            
        } catch (e: Exception) {
            Log.e("GroupAccountLinkFragment", "모든 등록하기 버튼 숨기기 오류", e)
        }
    }
    
    // 재귀적으로 모든 하위 View에서 등록하기 버튼 찾아서 숨기기
    private fun hideRegisterButtonRecursively(view: View) {
        try {
            // 현재 View가 등록하기 버튼인지 확인
            if (view.id == R.id.btn_register) {
                view.visibility = View.GONE
                Log.d("GroupAccountLinkFragment", "등록하기 버튼 발견 및 숨김: ${view.javaClass.simpleName}")
            }
            
            // ViewGroup인 경우 모든 자식 View 검사
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    val childView = view.getChildAt(i)
                    hideRegisterButtonRecursively(childView)
                }
            }
        } catch (e: Exception) {
            Log.e("GroupAccountLinkFragment", "재귀적 등록하기 버튼 숨기기 오류", e)
        }
    }
    
    // 안내사항 텍스트에 굵은 글씨 적용
    private fun applyBoldTextToGuidelines(view: View) {
        try {
            // 첫 번째 안내사항: "신한은행" 굵게
            val firstGuideline = view.findViewById<TextView>(R.id.tv_first_guideline)
            firstGuideline?.let { textView ->
                val text = "신한은행 모임통장만 연동가능합니다."
                val spannableString = android.text.SpannableString(text)
                val boldSpan = android.text.style.StyleSpan(android.graphics.Typeface.BOLD)
                val startIndex = text.indexOf("신한은행")
                val endIndex = startIndex + "신한은행".length
                spannableString.setSpan(boldSpan, startIndex, endIndex, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                textView.text = spannableString
            }
            
            // 두 번째 안내사항: "연동하기" 굵게
            val secondGuideline = view.findViewById<TextView>(R.id.tv_second_guideline)
            secondGuideline?.let { textView ->
                val text = "기존 신한은행 모임통장이 있으시면 '연동하기'를 이용해주세요."
                val spannableString = android.text.SpannableString(text)
                val boldSpan = android.text.style.StyleSpan(android.graphics.Typeface.BOLD)
                val startIndex = text.indexOf("연동하기")
                val endIndex = startIndex + "연동하기".length
                spannableString.setSpan(boldSpan, startIndex, endIndex, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                textView.text = spannableString
            }
            
            // 세 번째 안내사항: "신분증" 굵게
//            val thirdGuideline = view.findViewById<TextView>(R.id.tv_third_guideline)
//            thirdGuideline?.let { textView ->
//                val text = "새 모임통장 개설 시 신분증이 필요합니다."
//                val spannableString = android.text.SpannableString(text)
//                val boldSpan = android.text.style.StyleSpan(android.graphics.Typeface.BOLD)
//                val startIndex = text.indexOf("신분증")
//                val endIndex = startIndex + "신분증".length
//                spannableString.setSpan(boldSpan, startIndex, endIndex, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//                textView.text = spannableString
//            }
            
        } catch (e: Exception) {
            Log.e("GroupAccountLinkFragment", "안내사항 텍스트 굵게 적용 중 오류 발생", e)
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
        
        // 랜덤 난수 생성 (20자리)
        val randomNumber = generateUnique20DigitNumber()
        
        // 디버깅을 위한 로그 출력
        Log.d("GroupAccountLinkFragment", "생성된 데이터:")
        Log.d("GroupAccountLinkFragment", "한국시간 날짜: $currentDate")
        Log.d("GroupAccountLinkFragment", "한국시간 시간: $currentTime")
        Log.d("GroupAccountLinkFragment", "랜덤번호: $randomNumber (길이: ${randomNumber.length})")
        
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
                "userKey" to "1607a094-72cc-4d4f-9ed3-e7cd1d264e2d"
            ),
            "accountTypeUniqueNo" to "088-1-64e152b919e94d"  // "Body" 제거하고 직접 추가
        )
        
        // 전체 요청 데이터 로그 출력
        Log.d("GroupAccountLinkFragment", "API 요청 데이터: ${Gson().toJson(requestData)}")
        
        // API 호출
        callCreateAccountAPI(requestData)
    }
    
    // 계좌 생성 API 호출
    private fun callCreateAccountAPI(requestData: Map<String, Any>) {
        Log.d("GroupAccountLinkFragment", "=== 계좌 생성 API 호출 시작 ===")
        
        // 백그라운드 스레드에서 API 호출
        CoroutineScope(Dispatchers.IO).launch {
            var connection: HttpURLConnection? = null
            try {
                val url = "https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/createDemandDepositAccount"
                Log.d("GroupAccountLinkFragment", "요청 URL: $url")
                
                connection = URL(url).openConnection() as HttpURLConnection
                
                // POST 요청 설정
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", "Android")
                connection.doOutput = true
                connection.doInput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                
                // JSON 데이터 전송
                val jsonData = Gson().toJson(requestData)
                val bytes = jsonData.toByteArray(Charsets.UTF_8)
                connection.setRequestProperty("Content-Length", bytes.size.toString())
                
                // 요청 정보 로깅
                Log.d("GroupAccountLinkFragment", "=== 요청 정보 ===")
                Log.d("GroupAccountLinkFragment", "요청 메서드: ${connection.requestMethod}")
                Log.d("GroupAccountLinkFragment", "요청 헤더:")
                connection.requestProperties.forEach { (key, value) ->
                    Log.d("GroupAccountLinkFragment", "  $key: $value")
                }
                Log.d("GroupAccountLinkFragment", "요청 데이터: $jsonData")
                Log.d("GroupAccountLinkFragment", "데이터 크기: ${bytes.size} bytes")
                Log.d("GroupAccountLinkFragment", "데이터 인코딩: UTF-8")
                
                // 데이터 전송
                Log.d("GroupAccountLinkFragment", "데이터 전송 시작...")
                connection.outputStream.use { os ->
                    os.write(bytes)
                }
                Log.d("GroupAccountLinkFragment", "데이터 전송 완료")
                
                // 응답 받기
                Log.d("GroupAccountLinkFragment", "=== 응답 정보 ===")
                val responseCode = connection.responseCode
                Log.d("GroupAccountLinkFragment", "응답 코드: $responseCode")
                Log.d("GroupAccountLinkFragment", "응답 메시지: ${connection.responseMessage}")
                
                if (responseCode == 200 || responseCode == 201) {  // 200 또는 201 모두 성공으로 처리
                    Log.d("GroupAccountLinkFragment", "성공 응답 처리 시작... (코드: $responseCode)")
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("GroupAccountLinkFragment", "성공 응답 데이터: $response")
                    
                    // JSON 응답에서 accountNo 추출
                    val responseJson = Gson().fromJson(response, Map::class.java)
                    Log.d("GroupAccountLinkFragment", "응답 JSON 파싱 결과: $responseJson")
                    
                    // REC 객체에서 accountNo 추출
                    val recObject = responseJson["REC"] as? Map<*, *>
                    val accountNo = recObject?.get("accountNo")?.toString()
                    Log.d("GroupAccountLinkFragment", "REC 객체: $recObject")
                    Log.d("GroupAccountLinkFragment", "추출된 accountNo: $accountNo")
                    
                    if (accountNo != null) {
                        Log.d("GroupAccountLinkFragment", "계좌 생성 성공! 계좌번호: $accountNo")
                        // 메인 스레드에서 다음 페이지로 이동
                        withContext(Dispatchers.Main) {
                            navigateToAccountCreatedPage(accountNo.toString())
                        }
                    } else {
                        Log.e("GroupAccountLinkFragment", "accountNo를 찾을 수 없음")
                        Log.e("GroupAccountLinkFragment", "응답 JSON 키들: ${responseJson.keys}")
                        if (recObject != null) {
                            Log.e("GroupAccountLinkFragment", "REC 객체 키들: ${recObject.keys}")
                        }
                        withContext(Dispatchers.Main) {
                            showErrorDialog("계좌 생성 실패", "계좌 번호를 받지 못했습니다.")
                        }
                    }
                } else {
                    Log.e("GroupAccountLinkFragment", "=== API 호출 실패 ===")
                    Log.e("GroupAccountLinkFragment", "응답 코드: $responseCode")
                    Log.e("GroupAccountLinkFragment", "응답 메시지: ${connection.responseMessage}")
                    
                    // 에러 응답도 읽어보기
                    try {
                        val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("GroupAccountLinkFragment", "에러 응답 데이터: $errorResponse")
                        
                        withContext(Dispatchers.Main) {
                            showErrorDialog("계좌 생성 실패", "서버 오류가 발생했습니다.\n\n코드: $responseCode\n메시지: ${connection.responseMessage}\n\n상세: $errorResponse")
                        }
                    } catch (e: Exception) {
                        Log.e("GroupAccountLinkFragment", "에러 응답 읽기 실패", e)
                        withContext(Dispatchers.Main) {
                            showErrorDialog("계좌 생성 실패", "서버 오류가 발생했습니다. (코드: $responseCode)")
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e("GroupAccountLinkFragment", "=== API 호출 중 예외 발생 ===")
                Log.e("GroupAccountLinkFragment", "예외 타입: ${e.javaClass.simpleName}")
                Log.e("GroupAccountLinkFragment", "예외 메시지: ${e.message}")
                Log.e("GroupAccountLinkFragment", "예외 스택트레이스:", e)
                
                withContext(Dispatchers.Main) {
                    showErrorDialog("계좌 생성 실패", "네트워크 오류가 발생했습니다:\n\n${e.javaClass.simpleName}: ${e.message}")
                }
            } finally {
                // 연결 해제
                try {
                    connection?.disconnect()
                    Log.d("GroupAccountLinkFragment", "HTTP 연결 해제 완료")
                } catch (e: Exception) {
                    Log.e("GroupAccountLinkFragment", "연결 해제 중 오류", e)
                }
            }
        }
    }
    
    // 계좌 생성 완료 페이지로 이동
    private fun navigateToAccountCreatedPage(accountNo: String) {
        Log.d("GroupAccountLinkFragment", "계좌 생성 완료, 계좌번호: $accountNo")
        
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
