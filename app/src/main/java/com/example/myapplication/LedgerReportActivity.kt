package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class LedgerReportActivity : BaseActivity() {
    
    companion object {
        private const val REQUEST_CREATE_REPORT = 1001
    }
    
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var reportsAdapter: AIReportsAdapter

    override fun setupContent(savedInstanceState: Bundle?) {
        Log.d("LedgerReportActivity", "=== LedgerReportActivity 시작 ===")
        
        try {
            // Intent에서 클럽 ID 받기 (선택적)
            val clubId = intent.getIntExtra("club_id", 0)
            if (clubId > 0) {
                Log.d("LedgerReportActivity", "클럽 ID 수신: $clubId")
            }
            
            // 앱 제목을 "AI 리포트"로 설정
            setAppTitle("AI 리포트")
            Log.d("LedgerReportActivity", "앱 제목 설정 완료")
            
            // AI 리포트 버튼을 선택된 상태로 설정 (안전하게 처리)
            try {
                selectBoardButton(btnAiReport)
                Log.d("LedgerReportActivity", "AI 리포트 버튼 선택 상태 설정 완료")
            } catch (e: Exception) {
                Log.w("LedgerReportActivity", "버튼 선택 상태 설정 실패", e)
            }
            
            // 뒤로가기 버튼 표시
            showBackButton()
            
            // 레이아웃 인플레이션
            val contentContainer = findViewById<android.widget.FrameLayout>(R.id.content_container)
            Log.d("LedgerReportActivity", "content_container 찾기 결과: ${contentContainer != null}")
            
            if (contentContainer == null) {
                Log.e("LedgerReportActivity", "content_container를 찾을 수 없음")
                Toast.makeText(this, "화면 로딩 오류", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            
            Log.d("LedgerReportActivity", "기존 contentContainer 자식 수: ${contentContainer.childCount}")
            contentContainer.removeAllViews() // 기존 뷰 제거
            
            val contentView = layoutInflater.inflate(R.layout.ledger_report, null)
            Log.d("LedgerReportActivity", "ledger_report 레이아웃 인플레이션 완료: ${contentView != null}")
            
            contentContainer.addView(contentView)
            Log.d("LedgerReportActivity", "contentContainer에 뷰 추가 완료")
            Log.d("LedgerReportActivity", "추가 후 contentContainer 자식 수: ${contentContainer.childCount}")
            
            // 뷰가 실제로 보이는지 확인
            Log.d("LedgerReportActivity", "contentView.visibility: ${contentView.visibility}")
            Log.d("LedgerReportActivity", "contentContainer.visibility: ${contentContainer.visibility}")

            // UI 요소 참조
            initializeViews(contentView)
            
            // 버튼 클릭 이벤트 설정
            setupButtonClickEvents(contentView)
            
            // 저장된 리포트 로드
            loadAIReports()
            
            Log.d("LedgerReportActivity", "=== LedgerReportActivity 초기화 완료 ===")
            
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "초기화 중 오류 발생", e)
            Toast.makeText(this, "AI 리포트 화면 로딩 실패: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun initializeViews(contentView: View) {
        try {
            recyclerView = contentView.findViewById(R.id.rv_reports_list)
            emptyState = contentView.findViewById(R.id.empty_state)
            
            Log.d("LedgerReportActivity", "recyclerView: ${recyclerView != null}")
            Log.d("LedgerReportActivity", "emptyState: ${emptyState != null}")
            
            if (recyclerView == null || emptyState == null) {
                Log.e("LedgerReportActivity", "UI 요소 찾기 실패 - recyclerView: $recyclerView, emptyState: $emptyState")
                throw Exception("필수 UI 요소를 찾을 수 없습니다")
            }
            
            // RecyclerView 설정
            recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
            reportsAdapter = AIReportsAdapter({ reportJson ->
                // 리포트 클릭 시 상세 페이지로 이동
                Log.d("LedgerReportActivity", "🎯 리포트 클릭됨!")
                Log.d("LedgerReportActivity", "전달할 데이터: $reportJson")
                
                try {
                    val intent = Intent(this, AIReportDetailActivity::class.java)
                    intent.putExtra("report_data", reportJson)
                    Log.d("LedgerReportActivity", "Intent 생성 완료, Detail Activity 시작")
                    startActivity(intent)
                    Log.d("LedgerReportActivity", "startActivity 호출 완료")
                } catch (e: Exception) {
                    Log.e("LedgerReportActivity", "Detail 페이지 이동 실패", e)
                    Toast.makeText(this, "상세 페이지를 열 수 없습니다: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }) { reportJson, position ->
                // 삭제 처리
                deleteReport(reportJson, position)
            }
            recyclerView.adapter = reportsAdapter
            
            Log.d("LedgerReportActivity", "UI 요소 참조 완료")
            
            // 초기 상태 설정 (리포트 컨테이너는 보이고, 빈 상태는 숨김)
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            Log.d("LedgerReportActivity", "초기 visibility 설정 완료")
            
            // 통계 업데이트
            updateStatistics()
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "UI 요소 참조 실패", e)
            throw e
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("LedgerReportActivity", "onResume 호출됨")
        // 화면 재진입 시 리포트 목록 새로고침
        try {
            loadAIReports()
            setupBoardButtonsForAIReport()
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "onResume에서 로드 실패", e)
        }
    }

    private fun setupButtonClickEvents(contentView: View) {
        try {
            // 리포트 생성 버튼 클릭 이벤트 (화면에 고정된 FloatingActionButton)
            val btnCreateReport = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btn_create_report)
            Log.d("LedgerReportActivity", "화면 고정 FloatingActionButton 찾기 시도: ${btnCreateReport != null}")
            
            if (btnCreateReport != null) {
                Log.d("LedgerReportActivity", "버튼 정보: visibility=${btnCreateReport.visibility}, clickable=${btnCreateReport.isClickable}")
                
                btnCreateReport.setOnClickListener {
                    Log.d("LedgerReportActivity", "🎯 리포트 생성 버튼 클릭됨!")
                    Toast.makeText(this, "AI 리포트 생성 페이지로 이동합니다", Toast.LENGTH_SHORT).show()
                    
                    try {
                        val intent = Intent(this, LedgerReportCreateActivity::class.java)
                        // 현재 club_id를 전달
                        val clubId = getCurrentClubId()
                        intent.putExtra("club_id", clubId)
                        Log.d("LedgerReportActivity", "Intent 생성 완료, club_id: $clubId")
                        startActivityForResult(intent, REQUEST_CREATE_REPORT)
                        Log.d("LedgerReportActivity", "startActivityForResult 호출 완료")
                    } catch (e: Exception) {
                        Log.e("LedgerReportActivity", "리포트 생성 화면 이동 실패", e)
                        Toast.makeText(this, "리포트 생성 화면으로 이동할 수 없습니다: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                Log.d("LedgerReportActivity", "✅ 리포트 생성 버튼 리스너 설정 완료")
            } else {
                Log.e("LedgerReportActivity", "❌ 리포트 생성 버튼을 찾을 수 없음 (R.id.btn_create_report)")
                
                // XML에서 모든 버튼을 찾아보기
                val allButtons = mutableListOf<View>()
                findAllButtonsInView(contentView, allButtons)
                Log.d("LedgerReportActivity", "전체 버튼 수: ${allButtons.size}")
                allButtons.forEachIndexed { index, button ->
                    Log.d("LedgerReportActivity", "버튼 $index: ${button.javaClass.simpleName}, id=${button.id}")
                }
            }
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "버튼 이벤트 설정 실패", e)
        }
    }
    
    private fun findAllButtonsInView(view: View, buttons: MutableList<View>) {
        if (view is Button) {
            buttons.add(view)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findAllButtonsInView(view.getChildAt(i), buttons)
            }
        }
    }
    
    private fun loadAIReports() {
        Log.d("LedgerReportActivity", "🚀 AI 리포트 목록 로드 시작")
        
        showLoadingState(true)
        
        val clubId = getCurrentClubId()
        Log.d("LedgerReportActivity", "🏠 현재 클럽 ID: $clubId")
        
        if (clubId <= 0) {
            Log.e("LedgerReportActivity", "❌ 유효하지 않은 클럽 ID: $clubId")
            showLoadingState(false)
            loadLocalReports()
            return
        }
        
        // 먼저 로컬 리포트를 로드하여 즉시 표시
        Log.d("LedgerReportActivity", "📱 로컬 리포트 우선 로드")
        loadLocalReports()
        
        // 백엔드 데이터 동기화 (재시도 메커니즘 포함)
        loadBackendReportsWithRetry(clubId, maxRetries = 2)
    }
    
    private fun loadBackendReportsWithRetry(clubId: Int, maxRetries: Int, currentAttempt: Int = 0) {
        Log.d("LedgerReportActivity", "📋 장부 목록 조회 중... (시도 ${currentAttempt + 1}/${maxRetries + 1})")
        
        com.example.myapplication.api.ApiClient.getApiService().getLedgerList(clubId).enqueue(object : retrofit2.Callback<List<LedgerApiItem>> {
            override fun onResponse(
                call: retrofit2.Call<List<LedgerApiItem>>,
                response: retrofit2.Response<List<LedgerApiItem>>
            ) {
                showLoadingState(false)
                
                if (response.isSuccessful) {
                    val ledgers = response.body()
                    if (!ledgers.isNullOrEmpty()) {
                        Log.d("LedgerReportActivity", "📋 장부 목록 조회 성공! 총 ${ledgers.size}개 장부")
                        
                        if (ledgers.size == 1) {
                            val onlyLedgerId = ledgers[0].id
                            Log.d("LedgerReportActivity", "장부 1개 자동 선택: ${ledgers[0].name}")
                            loadAndMergeBackendReports(clubId, onlyLedgerId)
                        } else {
                            showLedgerSelectionDialog(clubId, ledgers)
                        }
                    } else {
                        Log.d("LedgerReportActivity", "장부가 없음")
                        // 로컬 데이터가 이미 표시되어 있으므로 추가 작업 불필요
                    }
                } else {
                    Log.e("LedgerReportActivity", "장부 목록 조회 실패: ${response.code()}")
                    handleNetworkError("장부 정보 조회 실패", currentAttempt, maxRetries) {
                        loadBackendReportsWithRetry(clubId, maxRetries, currentAttempt + 1)
                    }
                }
            }
            
            override fun onFailure(call: retrofit2.Call<List<LedgerApiItem>>, t: Throwable) {
                showLoadingState(false)
                Log.e("LedgerReportActivity", "장부 목록 조회 네트워크 오류", t)
                handleNetworkError("네트워크 연결 실패", currentAttempt, maxRetries) {
                    loadBackendReportsWithRetry(clubId, maxRetries, currentAttempt + 1)
                }
            }
        })
    }
    
    private fun showLoadingState(isLoading: Boolean) {
        runOnUiThread {
            // 로딩 상태 표시 (ProgressBar나 기타 로딩 인디케이터 활용)
            Log.d("LedgerReportActivity", "로딩 상태: $isLoading")
            // 실제 로딩 UI 구현 시 여기에 추가
        }
    }
    
    private fun handleNetworkError(message: String, currentAttempt: Int, maxRetries: Int, retryAction: () -> Unit) {
        if (currentAttempt < maxRetries) {
            Log.d("LedgerReportActivity", "$message - 재시도 예정 (${currentAttempt + 1}/$maxRetries)")
            // 1초 후 재시도
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                retryAction()
            }, 1000)
        } else {
            Log.e("LedgerReportActivity", "$message - 최대 재시도 횟수 초과")
            Toast.makeText(this, "$message (로컬 데이터 표시)", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadReportsFromBackend(clubId: Int, ledgerId: Int) {
        Log.d("LedgerReportActivity", "백엔드에서 리포트 목록 조회 시작 - 연간 + 비교 리포트만")
        
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        
        // 연간 리포트 + 유사한 동아리 비교 리포트만 조회
        val allReports = mutableSetOf<String>()
        var completedRequests = 0
        val totalRequests = 3 // 현재 년도 + 작년 연간 리포트 + 저장된 기존 리포트 = 3개 요청
        
        Log.d("LedgerReportActivity", "연간 리포트 + 비교 리포트 조회: ${currentYear}년")
        
        // 1. 현재 년도의 연간 리포트 조회
        Log.d("LedgerReportActivity", "현재 연간 리포트 조회: ${currentYear}년")
        com.example.myapplication.api.ApiClient.getApiService().getYearlyReports(clubId, ledgerId, currentYear).enqueue(object : retrofit2.Callback<List<com.example.myapplication.api.ApiService.BackendReportItem>> {
            override fun onResponse(
                call: retrofit2.Call<List<com.example.myapplication.api.ApiService.BackendReportItem>>,
                response: retrofit2.Response<List<com.example.myapplication.api.ApiService.BackendReportItem>>
            ) {
                if (response.isSuccessful) {
                    val yearlyReports = response.body() ?: emptyList()
                    Log.d("LedgerReportActivity", "${currentYear}년 연간 리포트 수: ${yearlyReports.size}")
                    
                    yearlyReports.forEach { backendReport ->
                        // 자동 생성된 버전 리포트는 제외 (ver_ 포함)
                        if (backendReport.title.contains("_ver_") || backendReport.title.contains("ver_")) {
                            Log.d("LedgerReportActivity", "❌ 자동 버전 리포트 제외: ${backendReport.title}")
                            return@forEach
                        }
                        
                        Log.d("LedgerReportActivity", "🔍 백엔드 리포트 분석 중...")
                        Log.d("LedgerReportActivity", "   제목: ${backendReport.title}")
                        Log.d("LedgerReportActivity", "   ID: ${backendReport.id}")
                        Log.d("LedgerReportActivity", "   장부: ${backendReport.ledger}")
                        
                        // 백엔드 제목에서 실제 타입 판별 (더 정확한 매칭)
                        val actualType = when {
                            backendReport.title.contains("연간종합") || backendReport.title.contains("yearly") -> {
                                Log.d("LedgerReportActivity", "   📊 '연간종합/yearly' 키워드 발견 → yearly")
                                "yearly"
                            }
                            backendReport.title.contains("비교") || backendReport.title.contains("similar") || backendReport.title.contains("comparison") -> {
                                Log.d("LedgerReportActivity", "   🏆 '비교/similar/comparison' 키워드 발견 → comparison")
                                "comparison"
                            }
                            backendReport.title.contains("이벤트비교") || backendReport.title.contains("event_comparison") || backendReport.title.contains("이벤트") -> {
                                Log.d("LedgerReportActivity", "   📅 '이벤트비교/event_comparison/이벤트' 키워드 발견 → event_comparison")
                                "event_comparison"
                            }
                            backendReport.title.contains("년_보고서") || backendReport.title.contains("_년") -> {
                                Log.d("LedgerReportActivity", "   📊 '년_보고서/_년' 키워드 발견 → yearly (연간 종합분석)")
                                "yearly"
                            }
                            backendReport.title.contains("종합") || backendReport.title.contains("comprehensive") -> {
                                Log.d("LedgerReportActivity", "   📊 '종합/comprehensive' 키워드 발견 → yearly")
                                "yearly"
                            }
                            else -> {
                                Log.d("LedgerReportActivity", "   📊 키워드 없음 → yearly (기본값: 연간종합분석)")
                                "yearly"  // 기본값을 yearly로 변경
                            }
                        }
                        val reportJson = convertBackendReportToJson(backendReport, actualType)
                        allReports.add(reportJson)
                        Log.d("LedgerReportActivity", "연간 리포트 추가: ${backendReport.title} (타입: $actualType)")
                        
                        // 생성된 JSON 확인
                        try {
                            val checkJson = org.json.JSONObject(reportJson)
                            Log.d("LedgerReportActivity", "생성된 JSON - 제목: ${checkJson.optString("title")}, 타입: ${checkJson.optString("type")}")
                        } catch (e: Exception) {
                            Log.e("LedgerReportActivity", "JSON 검증 실패", e)
                        }
                    }
                } else {
                    Log.w("LedgerReportActivity", "${currentYear}년 연간 리포트 조회 실패: ${response.code()}")
                }
                
                completedRequests++
                if (completedRequests == totalRequests) {
                    displayAllReports(allReports)
                }
            }
            
            override fun onFailure(call: retrofit2.Call<List<com.example.myapplication.api.ApiService.BackendReportItem>>, t: Throwable) {
                Log.e("LedgerReportActivity", "${currentYear}년 연간 리포트 조회 네트워크 실패", t)
                completedRequests++
                if (completedRequests == totalRequests) {
                    displayAllReports(allReports)
                }
            }
        })
    }
    
    private fun convertBackendReportToJson(backendReport: com.example.myapplication.api.ApiService.BackendReportItem, type: String): String {
        return try {
            // 효율적인 데이터 변환기 사용
            val converter = BackendReportConverter()
            val convertedReport = converter.convert(backendReport, type)
            
            // 로컬 저장은 ReportsDataManager에 위임
            val clubId = getCurrentClubId()
            val reportsManager = ReportsDataManager(this, clubId)
            reportsManager.saveBackendReport(convertedReport)
            
            convertedReport
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "백엔드 리포트 변환 실패", e)
            createFallbackReport(backendReport)
        }
    }
    
    private fun createFallbackReport(backendReport: com.example.myapplication.api.ApiService.BackendReportItem): String {
        return JSONObject().apply {
            put("id", backendReport.id)
            put("title", backendReport.title)
            put("content", "데이터 변환 중 오류가 발생했습니다. 원본 데이터를 확인해주세요.")
            put("type", "error")
            put("created_at", System.currentTimeMillis())
            put("creator", "AI 시스템 (오류)")
            put("backend_id", backendReport.id)
        }.toString()
    }
    
    private fun saveBackendReportToLocal(reportJson: String) {
        try {
            val clubId = getCurrentClubId()
            val sharedPref = getSharedPreferences("ai_reports_club_$clubId", Context.MODE_PRIVATE)
            
            val existingReportsJson = sharedPref.getString("reports_json", "[]")
            val existingReportsArray = org.json.JSONArray(existingReportsJson)
            
            // 중복 방지: 같은 backend_id가 이미 있는지 확인
            val newReport = JSONObject(reportJson)
            val backendId = newReport.optInt("backend_id", -1)
            
            var isDuplicate = false
            for (i in 0 until existingReportsArray.length()) {
                val existingReport = existingReportsArray.getJSONObject(i)
                if (existingReport.optInt("backend_id", -1) == backendId && backendId != -1) {
                    isDuplicate = true
                    break
                }
            }
            
            if (!isDuplicate) {
                existingReportsArray.put(newReport)
                sharedPref.edit()
                    .putString("reports_json", existingReportsArray.toString())
                    .apply()
                Log.d("LedgerReportActivity", "백엔드 리포트를 로컬에 저장: ${newReport.optString("title")}")
            } else {
                Log.d("LedgerReportActivity", "중복된 백엔드 리포트 - 저장 건너뛰기: ${newReport.optString("title")}")
            }
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "백엔드 리포트 로컬 저장 실패", e)
        }
    }
    
    private fun formatBackendContentToText(contentMap: Map<String, Any>, type: String): String {
        try {
            Log.d("LedgerReportActivity", "백엔드 content 파싱 시작: $contentMap")
            
            // 기본 정보 추출
            val clubId = contentMap["club_id"] as? Int ?: 0
            val year = contentMap["year"] as? Int ?: 2024
            val month = contentMap["month"] as? Int?
            
            // 요약 정보 추출
            val summaryMap = contentMap["summary"] as? Map<String, Any> ?: emptyMap()
            val income = (summaryMap["income"] as? Number)?.toInt() ?: 0
            val expense = (summaryMap["expense"] as? Number)?.toInt() ?: 0
            val net = (summaryMap["net"] as? Number)?.toInt() ?: 0
            
            // 기간 텍스트 생성
            val periodText = if (month != null) {
                "${year}년 ${month}월"
            } else {
                "${year}년"
            }
            
            return buildString {
                appendLine("📊 $periodText AI 재정 분석 리포트")
                appendLine()
                appendLine("💰 재정 현황 요약")
                appendLine("• 총 수입: ${String.format("%,d", income)}원")
                appendLine("• 총 지출: ${String.format("%,d", expense)}원")
                appendLine("• 순수익: ${String.format("%,d", net)}원")
                appendLine()
                
                // 거래 유형별 분석
                val byTypeList = contentMap["by_type"] as? List<Map<String, Any>> ?: emptyList()
                if (byTypeList.isNotEmpty()) {
                    appendLine("📋 거래 유형별 분석")
                    byTypeList.forEach { typeData ->
                        val typeName = typeData["type"] as? String ?: "기타"
                        val typeIncome = (typeData["income"] as? Number)?.toInt() ?: 0
                        val typeExpense = (typeData["expense"] as? Number)?.toInt() ?: 0
                        appendLine("• $typeName: 수입 ${formatLedgerAmount(typeIncome)}, 지출 ${formatLedgerAmount(typeExpense)}")
                    }
                    appendLine()
                }
                
                // 결제 수단별 분석
                val byPaymentList = contentMap["by_payment_method"] as? List<Map<String, Any>> ?: emptyList()
                if (byPaymentList.isNotEmpty()) {
                    appendLine("💳 결제 수단별 분석")
                    byPaymentList.forEach { paymentData ->
                        val method = paymentData["payment_method"] as? String ?: "기타"
                        val methodIncome = (paymentData["income"] as? Number)?.toInt() ?: 0
                        val methodExpense = (paymentData["expense"] as? Number)?.toInt() ?: 0
                        appendLine("• $method: 수입 ${formatLedgerAmount(methodIncome)}, 지출 ${formatLedgerAmount(methodExpense)}")
                    }
                    appendLine()
                }
                
                // 행사별 분석
                val byEventList = contentMap["by_event"] as? List<Map<String, Any>> ?: emptyList()
                if (byEventList.isNotEmpty()) {
                    appendLine("🎯 행사별 분석")
                    byEventList.forEach { eventData ->
                        val eventName = eventData["event_name"] as? String ?: "일반 활동"
                        val eventIncome = (eventData["income"] as? Number)?.toInt() ?: 0
                        val eventExpense = (eventData["expense"] as? Number)?.toInt() ?: 0
                        val eventNet = eventIncome - eventExpense
                        appendLine("• $eventName: 수입 ${formatLedgerAmount(eventIncome)}, 지출 ${formatLedgerAmount(eventExpense)}, 순수익 ${formatLedgerAmount(eventNet)}")
                    }
                    appendLine()
                }
                
                appendLine("✨ AI 분석이 완료되었습니다.")
                appendLine("이 리포트는 실제 동아리 데이터를 기반으로 생성되었습니다.")
            }
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "백엔드 content 파싱 오류", e)
            // 파싱 실패 시 기본 메시지 반환
            return "📊 AI 재정 분석 리포트\n\n백엔드에서 생성된 실제 동아리 재정 데이터입니다.\n데이터 파싱 중 오류가 발생했습니다.\n\n원본 데이터: $contentMap"
        }
    }
    
    private fun displayAllReports(backendReports: Set<String>) {
        Log.d("LedgerReportActivity", "백엔드 리포트 수: ${backendReports.size}")
        
        // 로컬 리포트도 함께 로드하여 통합
        val allReports = mutableSetOf<String>()
        allReports.addAll(backendReports)
        
        // 로컬 리포트 추가 (중복 제거)
        try {
            val clubId = getCurrentClubId()
            val sharedPref = getSharedPreferences("ai_reports_club_$clubId", Context.MODE_PRIVATE)
            val reportsJson = sharedPref.getString("reports_json", "[]")
            val reportsArray = org.json.JSONArray(reportsJson)
            
            Log.d("LedgerReportActivity", "로컬 리포트 수: ${reportsArray.length()}")
            
            for (i in 0 until reportsArray.length()) {
                val localReport = reportsArray.getJSONObject(i)
                val backendId = localReport.optInt("backend_id", -1)
                
                // 백엔드 ID가 -1인 것만 추가 (로컬 생성 리포트)
                // 백엔드에서 온 것은 이미 saveBackendReportToLocal에서 저장됨
                if (backendId == -1) {
                    allReports.add(localReport.toString())
                    Log.d("LedgerReportActivity", "로컬 전용 리포트 추가: ${localReport.optString("title")}")
                }
            }
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "로컬 리포트 로드 실패", e)
        }
        
        Log.d("LedgerReportActivity", "전체 리포트 목록 표시 시작 - 총 ${allReports.size}개")
        
        if (allReports.isEmpty()) {
            showEmptyState()
        } else {
            showReportsList(allReports)
        }
    }
    
    private fun loadLocalReports() {
        Log.d("LedgerReportActivity", "=== 로컬 리포트 데이터 로드 시작 ===")
        
        try {
            val clubId = getCurrentClubId()
            val reportsManager = ReportsDataManager(this, clubId)
            
            Log.d("LedgerReportActivity", "🏠 현재 클럽 ID: $clubId")
            
            val reports = reportsManager.getLocalReports()
            Log.d("LedgerReportActivity", "📈 로컬 리포트 수: ${reports.size}")
            
            if (reports.isEmpty()) {
                Log.d("LedgerReportActivity", "❌ 저장된 리포트 없음 - 빈 상태 표시")
                showEmptyState()
            } else {
                Log.d("LedgerReportActivity", "✅ 로컬 리포트 목록 표시 시작")
                showReportsList(reports)
            }
            
            // 통계 업데이트
            updateStatistics()
            
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "로컬 리포트 로드 실패", e)
            handleLoadError("로컬 리포트를 불러올 수 없습니다", e)
        }
    }
    
    private fun showEmptyState() {
        try {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            Log.d("LedgerReportActivity", "빈 상태 표시")
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "빈 상태 표시 실패", e)
        }
    }
    
    // 테스트 리포트 함수들 제거됨 - 사용자 요청

    private fun showReportsList(reports: Set<String>) {
        try {
            Log.d("LedgerReportActivity", "=== showReportsList 시작 ===")
            Log.d("LedgerReportActivity", "📊 입력된 리포트 수: ${reports.size}")
            Log.d("LedgerReportActivity", "🔧 recyclerView 상태: ${recyclerView != null}")
            Log.d("LedgerReportActivity", "🔧 emptyState 상태: ${emptyState != null}")
            Log.d("LedgerReportActivity", "🔧 reportsAdapter 상태: ${::reportsAdapter.isInitialized}")
            
            // 필터링 없이 모든 리포트 표시
            val filteredReports = reports
            
            Log.d("LedgerReportActivity", "🔍 필터링 후 리포트 수: ${filteredReports.size} (필터링 전: ${reports.size})")
            
            // 각 리포트 내용 로깅
            filteredReports.forEachIndexed { index, report ->
                try {
                    val reportObj = JSONObject(report)
                    Log.d("LedgerReportActivity", "📋 리포트 $index: ${reportObj.optString("title")} (타입: ${reportObj.optString("type")})")
                } catch (e: Exception) {
                    Log.e("LedgerReportActivity", "❌ 리포트 $index 파싱 실패: $e")
                }
            }
            
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            
            Log.d("LedgerReportActivity", "✅ Visibility 설정 완료")
            
            // 생성일시 기준으로 정렬 (최신순)
            val sortedReports = filteredReports.sortedByDescending { 
                try {
                    JSONObject(it).getLong("created_at")
                } catch (e: Exception) { 
                    Log.w("LedgerReportActivity", "⚠️ 정렬 중 created_at 파싱 실패: $e")
                    0L 
                }
            }
            
            Log.d("LedgerReportActivity", "📈 정렬된 리포트 수: ${sortedReports.size}")
            
            // RecyclerView 상태 상세 체크
            Log.d("LedgerReportActivity", "🔍 RecyclerView 상세 정보:")
            Log.d("LedgerReportActivity", "  - layoutManager: ${recyclerView.layoutManager}")
            Log.d("LedgerReportActivity", "  - adapter: ${recyclerView.adapter}")
            Log.d("LedgerReportActivity", "  - visibility: ${recyclerView.visibility}")
            Log.d("LedgerReportActivity", "  - width: ${recyclerView.width}, height: ${recyclerView.height}")
            
            // RecyclerView에 데이터 설정
            if (::reportsAdapter.isInitialized) {
                reportsAdapter.updateReports(sortedReports)
                Log.d("LedgerReportActivity", "✅ RecyclerView 어댑터 업데이트 완료")
                
                // 어댑터 아이템 수 확인
                Log.d("LedgerReportActivity", "📦 어댑터 아이템 수: ${reportsAdapter.itemCount}")
                
                // RecyclerView 상태를 다시 한번 강제로 업데이트
                recyclerView.post {
                    Log.d("LedgerReportActivity", "🔄 RecyclerView UI 스레드에서 업데이트")
                    Log.d("LedgerReportActivity", "  - 최종 어댑터 아이템 수: ${reportsAdapter.itemCount}")
                    Log.d("LedgerReportActivity", "  - RecyclerView 자식 수: ${recyclerView.childCount}")
                    
                    // 어댑터에 변경사항 알림
                    recyclerView.adapter?.notifyDataSetChanged()
                    
                    // 레이아웃 매니저에 스크롤 위치 초기화
                    recyclerView.layoutManager?.scrollToPosition(0)
                }
                
            } else {
                Log.e("LedgerReportActivity", "❌ reportsAdapter가 초기화되지 않음!")
                
                // 어댑터가 초기화되지 않았다면 다시 초기화 시도
                try {
                    Log.d("LedgerReportActivity", "🔧 어댑터 긴급 재초기화 시도")
                    initializeRecyclerViewAdapter()
                    
                    // 재초기화 후 다시 데이터 설정
                    if (::reportsAdapter.isInitialized) {
                        reportsAdapter.updateReports(sortedReports)
                        Log.d("LedgerReportActivity", "✅ 긴급 재초기화 후 데이터 설정 완료")
                    }
                } catch (e: Exception) {
                    Log.e("LedgerReportActivity", "❌ 어댑터 긴급 재초기화 실패", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "❌ 리포트 목록 표시 실패", e)
            e.printStackTrace()
            showEmptyState()
        }
    }
    
    private fun initializeRecyclerViewAdapter() {
        Log.d("LedgerReportActivity", "🔧 RecyclerView 어댑터 초기화")
        
        try {
            reportsAdapter = AIReportsAdapter({ reportJson ->
                Log.d("LedgerReportActivity", "🎯 리포트 클릭됨!")
                Log.d("LedgerReportActivity", "전달할 데이터: $reportJson")
                
                try {
                    val intent = Intent(this, AIReportDetailActivity::class.java)
                    intent.putExtra("report_data", reportJson)
                    Log.d("LedgerReportActivity", "AIReportDetailActivity 시작")
                    startActivity(intent)
                    Log.d("LedgerReportActivity", "✅ AIReportDetailActivity 시작 완료")
                } catch (e: Exception) {
                    Log.e("LedgerReportActivity", "상세 페이지를 열 수 없습니다", e)
                    Toast.makeText(this, "상세 페이지를 열 수 없습니다: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }) { reportJson, position ->
                // 삭제 처리
                deleteReport(reportJson, position)
            }
            
            recyclerView.adapter = reportsAdapter
            Log.d("LedgerReportActivity", "✅ 어댑터 초기화 완료")
            
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "❌ 어댑터 초기화 실패", e)
            throw e
        }
    }
    
    private fun testBackendConnection() {
        Log.d("LedgerReportActivity", "🌐 백엔드 연결 테스트 시작")
        Log.d("LedgerReportActivity", "📡 API URL: ${com.example.myapplication.BuildConfig.BASE_URL}")
        
        // 클럽 목록 API로 연결 테스트
        com.example.myapplication.api.ApiClient.getApiService().getClubList().enqueue(object : retrofit2.Callback<List<ClubItem>> {
            override fun onResponse(call: retrofit2.Call<List<ClubItem>>, response: retrofit2.Response<List<ClubItem>>) {
                if (response.isSuccessful) {
                    val clubs = response.body()
                    Log.d("LedgerReportActivity", "✅ 백엔드 연결 성공! 클럽 수: ${clubs?.size ?: 0}")
                    clubs?.take(3)?.forEach { club ->
                        Log.d("LedgerReportActivity", "  - 클럽: ${club.name} (ID: ${club.id})")
                    }
                } else {
                    Log.e("LedgerReportActivity", "❌ 백엔드 연결 실패: ${response.code()} ${response.message()}")
                    Log.e("LedgerReportActivity", "❌ 응답 바디: ${response.errorBody()?.string()}")
                }
            }
            
            override fun onFailure(call: retrofit2.Call<List<ClubItem>>, t: Throwable) {
                Log.e("LedgerReportActivity", "❌ 백엔드 연결 네트워크 오류", t)
            }
        })
    }
    
    private fun testBackendReportData(clubId: Int) {
        Log.d("LedgerReportActivity", "📊 백엔드 리포트 데이터 테스트 - 클럽 ID: $clubId")
        
        // 장부 목록 먼저 확인
        com.example.myapplication.api.ApiClient.getApiService().getLedgerList(clubId).enqueue(object : retrofit2.Callback<List<LedgerApiItem>> {
            override fun onResponse(call: retrofit2.Call<List<LedgerApiItem>>, response: retrofit2.Response<List<LedgerApiItem>>) {
                if (response.isSuccessful) {
                    val ledgers = response.body()
                    Log.d("LedgerReportActivity", "📋 장부 목록 조회 성공! 장부 수: ${ledgers?.size ?: 0}")
                    
                    ledgers?.forEach { ledger ->
                        Log.d("LedgerReportActivity", "  - 장부: ${ledger.name} (ID: ${ledger.id})")
                    }
                    
                    // 첫 번째 장부로 리포트 데이터 확인
                    if (!ledgers.isNullOrEmpty()) {
                        val firstLedgerId = ledgers[0].id
                        testReportAPIs(clubId, firstLedgerId)
                    }
                } else {
                    Log.e("LedgerReportActivity", "❌ 장부 목록 조회 실패: ${response.code()} ${response.message()}")
                }
            }
            
            override fun onFailure(call: retrofit2.Call<List<LedgerApiItem>>, t: Throwable) {
                Log.e("LedgerReportActivity", "❌ 장부 목록 조회 네트워크 오류", t)
            }
        })
    }
    
    private fun testReportAPIs(clubId: Int, ledgerId: Int) {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
        
        Log.d("LedgerReportActivity", "🔍 리포트 API 테스트 - 클럽: $clubId, 장부: $ledgerId, 기간: ${currentYear}년 ${currentMonth}월")
        
        // 1. 연간 리포트 조회 테스트
        com.example.myapplication.api.ApiClient.getApiService().getYearlyReports(clubId, ledgerId, currentYear).enqueue(object : retrofit2.Callback<List<com.example.myapplication.api.ApiService.BackendReportItem>> {
            override fun onResponse(call: retrofit2.Call<List<com.example.myapplication.api.ApiService.BackendReportItem>>, response: retrofit2.Response<List<com.example.myapplication.api.ApiService.BackendReportItem>>) {
                if (response.isSuccessful) {
                    val reports = response.body()
                    Log.d("LedgerReportActivity", "✅ 연간 리포트 조회 성공! 리포트 수: ${reports?.size ?: 0}")
                    reports?.forEach { report ->
                        Log.d("LedgerReportActivity", "  - 리포트: ${report.title} (ID: ${report.id})")
                        Log.d("LedgerReportActivity", "  - 내용 키들: ${report.content.keys}")
                    }
                } else {
                    Log.e("LedgerReportActivity", "❌ 연간 리포트 조회 실패: ${response.code()} ${response.message()}")
                    Log.e("LedgerReportActivity", "❌ URL: report/clubs/$clubId/ledgers/$ledgerId/reports/yearly/?year=$currentYear")
                }
            }
            
            override fun onFailure(call: retrofit2.Call<List<com.example.myapplication.api.ApiService.BackendReportItem>>, t: Throwable) {
                Log.e("LedgerReportActivity", "❌ 연간 리포트 조회 네트워크 오류", t)
            }
        })
    }
    
    private fun loadAndMergeBackendReports(clubId: Int, ledgerId: Int) {
        Log.d("LedgerReportActivity", "🔄 백엔드 리포트 데이터를 기존 리스트에 합치기 시작 (개선된 오류 처리)")
        
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val errorHandler = BackendErrorHandler(this)
        
        // 연간 리포트 조회 (2024년, 2025년) - 백엔드 성능 이슈 고려
        val years = listOf(currentYear - 1, currentYear) // 2024, 2025
        val backendReports = mutableSetOf<String>()
        var completedRequests = 0
        val totalRequests = years.size
        
        years.forEach { year ->
            Log.d("LedgerReportActivity", "📊 ${year}년 연간 리포트 조회 중...")
            
            com.example.myapplication.api.ApiClient.getApiService().getYearlyReports(clubId, ledgerId, year).enqueue(object : retrofit2.Callback<List<com.example.myapplication.api.ApiService.BackendReportItem>> {
                override fun onResponse(
                    call: retrofit2.Call<List<com.example.myapplication.api.ApiService.BackendReportItem>>,
                    response: retrofit2.Response<List<com.example.myapplication.api.ApiService.BackendReportItem>>
                ) {
                    if (response.isSuccessful) {
                        val reports = response.body() ?: emptyList()
                        Log.d("LedgerReportActivity", "✅ ${year}년 백엔드 리포트 수: ${reports.size}")
                        
                        reports.forEach { backendReport ->
                            // 자동 생성된 버전 리포트는 제외 (ver_ 포함)
                            if (backendReport.title.contains("_ver_") || backendReport.title.contains("ver_")) {
                                Log.d("LedgerReportActivity", "❌ 자동 버전 리포트 제외: ${backendReport.title}")
                                return@forEach
                            }
                            
                            Log.d("LedgerReportActivity", "📝 백엔드 리포트: ${backendReport.title}")
                            
                            // 백엔드 리포트를 프론트엔드 형식으로 변환
                            val frontendReport = convertBackendToFrontendFormat(backendReport)
                            backendReports.add(frontendReport)
                        }
                    } else {
                        Log.e("LedgerReportActivity", "❌ ${year}년 리포트 조회 실패: ${response.code()}")
                    }
                    
                    completedRequests++
                    if (completedRequests == totalRequests) {
                        // 모든 요청 완료시 기존 리스트에 합치기
                        mergeBackendReportsToList(backendReports)
                    }
                }
                
                override fun onFailure(call: retrofit2.Call<List<com.example.myapplication.api.ApiService.BackendReportItem>>, t: Throwable) {
                    Log.e("LedgerReportActivity", "❌ ${year}년 리포트 조회 네트워크 실패", t)
                    completedRequests++
                    if (completedRequests == totalRequests) {
                        mergeBackendReportsToList(backendReports)
                    }
                }
            })
        }
    }
    
    private fun convertBackendToFrontendFormat(backendReport: com.example.myapplication.api.ApiService.BackendReportItem): String {
        try {
            // 백엔드 content(Map)에서 데이터 추출
            val content = backendReport.content
            val summary = content["summary"] as? Map<String, Any> ?: emptyMap()
            val income = (summary["income"] as? Number)?.toInt() ?: 0
            val expense = (summary["expense"] as? Number)?.toInt() ?: 0
            val net = (summary["net"] as? Number)?.toInt() ?: 0
            val year = content["year"] as? Int ?: 2025
            
            // 리포트 타입 결정
            val reportType = when {
                backendReport.title.contains("비교") -> "comparison"
                else -> "comprehensive"
            }
            
            // 사용자 친화적 내용 생성
            val formattedContent = """
                📊 ${year}년 동아리 재정 분석 (실제 데이터)
                
                💰 재정 현황:
                • 총 수입: ${String.format(Locale.US, "%,d", income)}원
                • 총 지출: ${String.format(Locale.US, "%,d", expense)}원
                • 순이익: ${String.format(Locale.US, "%,d", net)}원
                
                📈 분석 결과:
                • 재정 건전성: ${if (net > 0) "양호 ✅" else "주의 ⚠️"}
                • 수익률: ${if (income > 0) String.format("%.1f", (net.toDouble() / income) * 100) else "0.0"}%
                
                💡 AI 인사이트:
                • 백엔드 실제 데이터 기반 분석
                • 정확한 재정 현황 반영
                • 실시간 업데이트 가능
            """.trimIndent()
            
            // 프론트엔드 JSON 형식으로 변환
            val frontendReport = JSONObject().apply {
                put("id", backendReport.id)
                put("title", backendReport.title)
                put("content", formattedContent)
                put("type", reportType)
                put("created_at", System.currentTimeMillis())
                put("creator", "AI 시스템 (백엔드)")
                put("backend_id", backendReport.id) // 백엔드 ID 보존
            }
            
            return frontendReport.toString()
            
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "❌ 백엔드 리포트 변환 실패: ${backendReport.title}", e)
            
            // 실패시 기본 형식
            val fallbackReport = JSONObject().apply {
                put("id", backendReport.id)
                put("title", backendReport.title)
                put("content", "백엔드 데이터 변환 중 오류 발생")
                put("type", "comprehensive")
                put("created_at", System.currentTimeMillis())
                put("creator", "AI 시스템")
                put("backend_id", backendReport.id)
            }
            
            return fallbackReport.toString()
        }
    }
    
    private fun mergeBackendReportsToList(backendReports: Set<String>) {
        Log.d("LedgerReportActivity", "🔗 리포트 데이터 병합 시작 - 백엔드 리포트 수: ${backendReports.size}")
        
        try {
            val clubId = getCurrentClubId()
            val reportsManager = ReportsDataManager(this, clubId)
            
            // 단순화된 병합 로직: 백엔드 우선, 중복 제거
            val mergedReports = reportsManager.mergeReports(backendReports)
            
            Log.d("LedgerReportActivity", "📈 병합 완료 - 최종 리포트 수: ${mergedReports.size}")
            
            if (mergedReports.isNotEmpty()) {
                showReportsList(mergedReports)
                reportsManager.saveReports(mergedReports)
            }
            
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "❌ 리포트 병합 실패", e)
            // 실패 시 로컬 데이터라도 표시
            loadLocalReports()
        }
    }

    private fun deleteReport(reportData: JSONObject) {
        try {
            val clubId = getCurrentClubId()
            val sharedPref = getSharedPreferences("ai_reports_club_$clubId", Context.MODE_PRIVATE)
            
            // JSON Array 방식으로 읽기 (일관성 유지)
            val reportsJson = sharedPref.getString("reports_json", "[]")
            val reportsArray = org.json.JSONArray(reportsJson)
            
            val reportToDelete = reportData.toString()
            val updatedArray = org.json.JSONArray()
            
            // 삭제할 리포트를 제외하고 새 배열에 추가
            for (i in 0 until reportsArray.length()) {
                val existingReport = reportsArray.getJSONObject(i)
                if (existingReport.toString() != reportToDelete) {
                    updatedArray.put(existingReport)
                }
            }
            
            // 업데이트된 배열 저장
            val success = sharedPref.edit()
                .putString("reports_json", updatedArray.toString())
                .commit()
                
            if (success) {
                Log.d("LedgerReportActivity", "✅ 리포트 삭제 성공: ${updatedArray.length()}개 남음")
                loadAIReports()
                Toast.makeText(this, "리포트가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                throw Exception("SharedPreferences 저장 실패")
            }
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "리포트 삭제 실패", e)
            Toast.makeText(this, "삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateStatistics() {
        try {
            val clubId = getCurrentClubId()
            val reportsManager = ReportsDataManager(this, clubId)
            val stats = reportsManager.getReportStats()
            
            val contentView = findViewById<android.widget.FrameLayout>(R.id.content_container)?.getChildAt(0)
            contentView?.let { view ->
                // AI 분석 상태 업데이트
                val tvAnalysisStatus = view.findViewById<TextView>(R.id.tv_analysis_status)
                tvAnalysisStatus?.apply {
                    text = if (stats.totalCount > 0) "활성화" else "대기중"
                    setTextColor(
                        ContextCompat.getColor(
                            this@LedgerReportActivity,
                            if (stats.totalCount > 0) android.R.color.holo_green_dark 
                            else android.R.color.darker_gray
                        )
                    )
                }
                
                // 최근 생성일 업데이트
                val tvRecentDate = view.findViewById<TextView>(R.id.tv_recent_date)
                tvRecentDate?.text = if (stats.latestTimestamp > 0) {
                    SimpleDateFormat("MM/dd", Locale.KOREA).format(Date(stats.latestTimestamp))
                } else "없음"
            }
            
            Log.d("LedgerReportActivity", "통계 업데이트 완료: ${stats.totalCount}개 리포트")
            
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "통계 업데이트 실패", e)
            handleStatisticsError()
        }
    }
    
    private fun handleLoadError(message: String, error: Throwable? = null) {
        runOnUiThread {
            showEmptyState()
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            
            // 재시도 버튼 제공 (선택사항)
            Log.e("LedgerReportActivity", "로딩 오류: $message", error)
        }
    }
    
    private fun handleStatisticsError() {
        val contentView = findViewById<android.widget.FrameLayout>(R.id.content_container)?.getChildAt(0)
        contentView?.let { view ->
            view.findViewById<TextView>(R.id.tv_analysis_status)?.apply {
                text = "오류"
                setTextColor(ContextCompat.getColor(this@LedgerReportActivity, android.R.color.holo_red_dark))
            }
            view.findViewById<TextView>(R.id.tv_recent_date)?.text = "없음"
        }
    }

    
    private fun setupBoardButtonsForAIReport() {
        val currentClubId = getCurrentClubId()
        Log.d("LedgerReportActivity", "AI 리포트에서 탭 버튼 설정, Club ID: $currentClubId")
        
        if (currentClubId > 0) {
            // 공지사항 버튼
            btnNotice?.setOnClickListener {
                Log.d("LedgerReportActivity", "공지사항 버튼 클릭 - Club ID: $currentClubId")
                val intent = Intent(this, ClubAnnouncementBoardListActivity::class.java)
                intent.putExtra("club_pk", currentClubId)
                startActivity(intent)
                finish()
            }
            
            // 자유게시판 버튼
            btnFreeBoard?.setOnClickListener {
                Log.d("LedgerReportActivity", "자유게시판 버튼 클릭 - Club ID: $currentClubId")
                val intent = Intent(this, ClubForumBoardListActivity::class.java)
                intent.putExtra("club_pk", currentClubId)
                startActivity(intent)
                finish()
            }
            
            // 행사장부 버튼
            btnEventAccount?.setOnClickListener {
                Log.d("LedgerReportActivity", "행사장부 버튼 클릭 - Club ID: $currentClubId")
                val intent = Intent(this, ClubEventLedgerListActivity::class.java)
                intent.putExtra("club_pk", currentClubId)
                startActivity(intent)
                finish()
            }
        }
    }
    
    private fun showLedgerSelectionDialog(clubId: Int, ledgers: List<LedgerApiItem>) {
        Log.d("LedgerReportActivity", "🔧 장부 선택 다이얼로그 표시")
        
        // 장부 이름 목록 생성
        val ledgerNames = ledgers.map { ledger ->
            "${ledger.name} (ID: ${ledger.id})"
        }.toTypedArray()
        
        // 장부 정보 로깅
        ledgers.forEachIndexed { index, ledger ->
            Log.d("LedgerReportActivity", "  $index. ${ledger.name} (ID: ${ledger.id})")
        }
        
        android.app.AlertDialog.Builder(this)
            .setTitle("📋 장부 선택")
            .setMessage("AI 리포트를 생성할 장부를 선택해주세요")
            .setItems(ledgerNames) { _, which ->
                val selectedLedger = ledgers[which]
                Log.d("LedgerReportActivity", "✅ 장부 선택됨: ${selectedLedger.name} (ID: ${selectedLedger.id})")
                
                // 선택된 장부로 리포트 로드
                loadAndMergeBackendReports(clubId, selectedLedger.id)
                
                // 사용자에게 알림
                android.widget.Toast.makeText(
                    this, 
                    "📋 선택된 장부: ${selectedLedger.name}", 
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("취소") { _, _ ->
                Log.d("LedgerReportActivity", "❌ 장부 선택 취소 - 로컬 데이터만 표시")
                // 취소 시 로컬 데이터만 표시
                loadLocalReports()
            }
            .setCancelable(false) // 선택을 강제함
            .show()
    }
    
    private fun formatLedgerAmount(amount: Int): String {
        return "${String.format(Locale.US, "%,d", amount)}원"
    }
    
    override fun getCurrentClubId(): Int {
        val clubId = intent.getIntExtra("club_id", 4) // Intent에서 가져오거나 기본값 4
        Log.d("LedgerReportActivity", "🔑 getCurrentClubId 호출됨 - Intent에서 받은 값: ${intent.getIntExtra("club_id", -1)}, 최종 반환값: $clubId")
        return clubId
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        Log.d("LedgerReportActivity", "🔄 onActivityResult 호출됨")
        Log.d("LedgerReportActivity", "   📋 requestCode: $requestCode (예상: $REQUEST_CREATE_REPORT)")
        Log.d("LedgerReportActivity", "   ✅ resultCode: $resultCode (예상: ${android.app.Activity.RESULT_OK})")
        Log.d("LedgerReportActivity", "   📦 data: $data")
        
        if (requestCode == REQUEST_CREATE_REPORT) {
            Log.d("LedgerReportActivity", "✅ 올바른 request code 확인됨")
            
            if (resultCode == android.app.Activity.RESULT_OK) {
                Log.d("LedgerReportActivity", "✅ RESULT_OK 확인됨")
                
                val reportCreated = data?.getBooleanExtra("report_created", false) ?: false
                val reportContent = data?.getStringExtra("report_content") ?: data?.getStringExtra("report_data") ?: ""
                val reportType = data?.getStringExtra("report_type") ?: ""
                
                // 리포트 내용에서 제목 추출
                var reportTitle = "새 리포트"
                try {
                    if (reportContent.isNotEmpty()) {
                        val reportObj = org.json.JSONObject(reportContent)
                        reportTitle = reportObj.optString("title", "새 리포트")
                    }
                } catch (e: Exception) {
                    Log.e("LedgerReportActivity", "리포트 제목 추출 실패", e)
                }
                
                Log.d("LedgerReportActivity", "📋 받은 데이터:")
                Log.d("LedgerReportActivity", "   - report_created: $reportCreated")
                Log.d("LedgerReportActivity", "   - report_content 길이: ${reportContent.length}")
                Log.d("LedgerReportActivity", "   - report_title: '$reportTitle'")
                Log.d("LedgerReportActivity", "   - report_type: '$reportType'")
                
                if (reportCreated) {
                    Log.d("LedgerReportActivity", "🎉 새 리포트 생성 완료!")
                    Log.d("LedgerReportActivity", "📋 리포트 제목: $reportTitle")
                    
                    // 새 리포트를 로컬 저장소에 저장
                    if (reportContent.isNotEmpty()) {
                        val clubId = getCurrentClubId()
                        val reportsManager = ReportsDataManager(this, clubId)
                        val saveSuccess = reportsManager.saveBackendReport(reportContent)
                        Log.d("LedgerReportActivity", "💾 새 리포트 로컬 저장: ${if (saveSuccess) "성공" else "실패"}")
                    } else {
                        Log.w("LedgerReportActivity", "⚠️ reportContent가 비어있음 - 데이터 전달 누락 가능성")
                        // 빈 내용이라도 리포트 목록 새로고침은 수행
                    }
                
                // 사용자에게 성공 알림
                Toast.makeText(this, "🤖 AI 리포트가 생성되었습니다!\n$reportTitle", Toast.LENGTH_LONG).show()
                
                // 즉시 목록 새로고침 (강제)
                Log.d("LedgerReportActivity", "🔄 리포트 생성 후 강제 새로고침 시작")
                
                // 즉시 새로고침 (로컬 저장된 데이터 표시)
                loadAIReports()
                
                // 통계 업데이트
                updateStatistics()
                
                // 1초 후에 한번 더 새로고침 (백엔드 동기화 확인)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    Log.d("LedgerReportActivity", "⏰ 1초 후 추가 새로고침 실행")
                    loadAIReports()
                    
                    // 통계 재업데이트
                    updateStatistics()
                }, 1000)
                } else {
                    Log.w("LedgerReportActivity", "⚠️ report_created가 false이거나 없음")
                    Toast.makeText(this, "리포트 생성 완료되었지만 데이터가 누락되었습니다", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w("LedgerReportActivity", "⚠️ resultCode가 RESULT_OK가 아님: $resultCode")
                Toast.makeText(this, "리포트 생성이 취소되거나 실패했습니다", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.w("LedgerReportActivity", "⚠️ requestCode가 일치하지 않음: $requestCode")
        }
    }

    private fun setupLedgerSelectionForReport(contentView: View) {
        Log.d("LedgerReportActivity", "장부 선택 UI 설정 시작")
        
        val clubId = getCurrentClubId()
        if (clubId <= 0) return
        
        // 장부 목록 조회
        ApiClient.getApiService().getLedgerList(clubId).enqueue(object : retrofit2.Callback<List<LedgerApiItem>> {
            override fun onResponse(
                call: retrofit2.Call<List<LedgerApiItem>>,
                response: retrofit2.Response<List<LedgerApiItem>>
            ) {
                if (response.isSuccessful) {
                    val ledgers = response.body() ?: emptyList()
                    Log.d("LedgerReportActivity", "조회된 장부 수: ${ledgers.size}")
                    
                    if (ledgers.size > 1) {
                        // 여러 장부가 있을 때만 선택 UI 표시
                        showLedgerSelectionUI(contentView, ledgers)
                    } else {
                        // 장부가 1개이거나 없으면 선택 UI 숨김
                        hideLedgerSelectionUI(contentView)
                    }
                } else {
                    Log.e("LedgerReportActivity", "장부 목록 조회 실패: ${response.code()}")
                    hideLedgerSelectionUI(contentView)
                }
            }
            
            override fun onFailure(call: retrofit2.Call<List<LedgerApiItem>>, t: Throwable) {
                Log.e("LedgerReportActivity", "장부 목록 조회 네트워크 오류", t)
                hideLedgerSelectionUI(contentView)
            }
        })
    }
    
    private fun showLedgerSelectionUI(contentView: View, ledgers: List<LedgerApiItem>) {
        val selectionContainer = contentView.findViewById<LinearLayout>(R.id.ledger_selection_container)
        val dropdown = contentView.findViewById<LinearLayout>(R.id.dropdown_ledger_selection_report)
        val selectedText = contentView.findViewById<TextView>(R.id.tv_selected_ledger_report)
        
        selectionContainer?.visibility = View.VISIBLE
        
        // 첫 번째 장부를 기본 선택
        if (ledgers.isNotEmpty()) {
            selectedText?.text = ledgers[0].name
            selectedText?.setTextColor(android.graphics.Color.parseColor("#333333"))
        }
        
        dropdown?.setOnClickListener {
            showLedgerSelectionDialog(ledgers, selectedText)
        }
        
        Log.d("LedgerReportActivity", "장부 선택 UI 표시 완료")
    }
    
    private fun hideLedgerSelectionUI(contentView: View) {
        val selectionContainer = contentView.findViewById<LinearLayout>(R.id.ledger_selection_container)
        selectionContainer?.visibility = View.GONE
        Log.d("LedgerReportActivity", "장부 선택 UI 숨김 완료")
    }
    
    private fun showLedgerSelectionDialog(ledgers: List<LedgerApiItem>, selectedText: TextView?) {
        val ledgerNames = ledgers.map { "${it.name} (ID: ${it.id})" }.toTypedArray()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("장부 선택")
            .setMessage("보고서를 보여줄 장부를 선택하세요")
            .setItems(ledgerNames) { _, which ->
                val selectedLedger = ledgers[which]
                selectedText?.text = selectedLedger.name
                selectedText?.setTextColor(android.graphics.Color.parseColor("#333333"))
                
                // 선택된 장부에 따라 리포트 필터링/새로고침
                onLedgerSelected(selectedLedger.id)
            }
            .show()
    }
    
    private fun onLedgerSelected(ledgerId: Int) {
        Log.d("LedgerReportActivity", "장부 선택됨: $ledgerId")
        // 선택된 장부에 맞는 리포트 목록 새로고침
        loadAIReports()
    }
    
    private fun deleteReport(reportJson: String, position: Int) {
        try {
            val reportData = JSONObject(reportJson)
            val backendId = reportData.optInt("backend_id", -1)
            
            if (backendId == -1) {
                // 로컬 저장된 리포트 삭제
                Log.d("LedgerReportActivity", "로컬 생성 리포트 삭제: ${reportData.optString("title")}")
                deleteLocalReport(reportJson, position)
                return
            }
            
            // 백엔드에서 리포트 삭제
            Log.d("LedgerReportActivity", "백엔드 리포트 삭제 시도: ID=$backendId, 제목=${reportData.optString("title")}")
            val apiService = ApiClient.getApiService()
            apiService.deleteReport(backendId).enqueue(object : Callback<okhttp3.ResponseBody> {
                override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@LedgerReportActivity, "리포트가 삭제되었습니다", Toast.LENGTH_SHORT).show()
                            // 로컬에서도 삭제
                            deleteLocalReport(reportJson, position)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@LedgerReportActivity, "리포트 삭제에 실패했습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                
                override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                    runOnUiThread {
                        Toast.makeText(this@LedgerReportActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                        // 네트워크 실패시에도 로컬에서는 삭제
                        deleteLocalReport(reportJson, position)
                    }
                }
            })
            
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "리포트 삭제 실패", e)
            Toast.makeText(this, "리포트 삭제에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteLocalReport(reportJson: String, position: Int) {
        try {
            val clubId = getCurrentClubId()
            val reportsManager = ReportsDataManager(this, clubId)
            
            val success = reportsManager.deleteReport(reportJson)
            
            if (success) {
                // UI 업데이트
                reportsAdapter.removeReport(position)
                updateStatistics()
                
                Log.d("LedgerReportActivity", "로컬 리포트 삭제 완료")
                Toast.makeText(this, "리포트가 삭제되었습니다", Toast.LENGTH_SHORT).show()
            } else {
                throw Exception("리포트 삭제 실패")
            }
            
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "로컬 리포트 삭제 실패", e)
            Toast.makeText(this, "리포트 삭제에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}