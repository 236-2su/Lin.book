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
            reportsAdapter = AIReportsAdapter { reportJson ->
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
            }
            recyclerView.adapter = reportsAdapter
            
            Log.d("LedgerReportActivity", "UI 요소 참조 완료")
            
            // 초기 상태 설정 (리포트 컨테이너는 보이고, 빈 상태는 숨김)
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            Log.d("LedgerReportActivity", "초기 visibility 설정 완료")
            
            // 통계 업데이트
            updateStatistics(contentView)
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
            // 리포트 생성 버튼 클릭 이벤트 (화면에 고정된 Button)
            val btnCreateReport = findViewById<Button>(R.id.btn_create_report)
            Log.d("LedgerReportActivity", "화면 고정 Button 찾기 시도: ${btnCreateReport != null}")
            
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
        
        // 1. 백엔드 연결 상태 테스트
        testBackendConnection()
        
        val clubId = getCurrentClubId()
        Log.d("LedgerReportActivity", "🏠 현재 클럽 ID: $clubId")
        
        if (clubId <= 0) {
            Log.e("LedgerReportActivity", "❌ 유효하지 않은 클럽 ID: $clubId")
            // 클럽 ID가 없어도 로컬 데이터는 확인
            loadLocalReports()
            return
        }
        
        // 2. 실제 백엔드 데이터 확인
        testBackendReportData(clubId)
        
        // 3. 장부 목록을 가져와서 첫 번째 장부 ID를 얻음
        Log.d("LedgerReportActivity", "📋 장부 목록 조회 중...")
        com.example.myapplication.api.ApiClient.getApiService().getLedgerList(clubId).enqueue(object : retrofit2.Callback<List<LedgerApiItem>> {
            override fun onResponse(
                call: retrofit2.Call<List<LedgerApiItem>>,
                response: retrofit2.Response<List<LedgerApiItem>>
            ) {
                if (response.isSuccessful) {
                    val ledgers = response.body()
                    if (!ledgers.isNullOrEmpty()) {
                        Log.d("LedgerReportActivity", "📋 장부 목록 조회 성공! 총 ${ledgers.size}개 장부")
                        
                        // 장부가 1개면 자동 선택, 여러개면 사용자 선택
                        if (ledgers.size == 1) {
                            val onlyLedgerId = ledgers[0].id
                            Log.d("LedgerReportActivity", "장부 1개 자동 선택: ${ledgers[0].name} (ID: $onlyLedgerId)")
                            loadAndMergeBackendReports(clubId, onlyLedgerId)
                        } else {
                            Log.d("LedgerReportActivity", "장부 ${ledgers.size}개 발견 - 사용자 선택 필요")
                            showLedgerSelectionDialog(clubId, ledgers)
                        }
                    } else {
                        Log.d("LedgerReportActivity", "장부가 없어서 빈 상태 표시")
                        showEmptyState()
                    }
                } else {
                    Log.e("LedgerReportActivity", "장부 목록 조회 실패: ${response.code()}")
                    // 실패 시 로컬 데이터 사용
                    loadLocalReports()
                }
            }
            
            override fun onFailure(call: retrofit2.Call<List<LedgerApiItem>>, t: Throwable) {
                Log.e("LedgerReportActivity", "장부 목록 조회 네트워크 오류", t)
                // 실패 시 로컬 데이터 사용
                loadLocalReports()
            }
        })
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
        // 백엔드 content(Map)를 사용자 친화적 텍스트로 변환
        val formattedContent = formatBackendContentToText(backendReport.content, type)
        
        val reportData = org.json.JSONObject().apply {
            put("id", backendReport.id)
            put("title", backendReport.title)
            put("content", formattedContent) // 실제 통계 데이터를 포맷된 텍스트로 변환
            put("type", type)
            put("created_at", System.currentTimeMillis()) 
            put("creator", "AI 시스템")
            put("backend_id", backendReport.id) // 백엔드 ID 추가
        }
        
        // 백엔드 리포트를 로컬에도 저장
        saveBackendReportToLocal(reportData.toString())
        
        return reportData.toString()
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
                        appendLine("• $typeName: 수입 ${String.format("%,d", typeIncome)}원, 지출 ${String.format("%,d", typeExpense)}원")
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
                        appendLine("• $method: 수입 ${String.format("%,d", methodIncome)}원, 지출 ${String.format("%,d", methodExpense)}원")
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
                        appendLine("• $eventName: 수입 ${String.format("%,d", eventIncome)}원, 지출 ${String.format("%,d", eventExpense)}원, 순수익 ${String.format("%,d", eventNet)}원")
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
            Log.d("LedgerReportActivity", "🏠 현재 클럽 ID: $clubId")
            
            val sharedPref = getSharedPreferences("ai_reports_club_$clubId", Context.MODE_PRIVATE)
            
            // 모든 저장된 키 확인
            val allKeys = sharedPref.all
            Log.d("LedgerReportActivity", "📦 SharedPreferences에 저장된 모든 키: ${allKeys.keys}")
            
            // JSON Array 방식으로 읽기
            val reportsJson = sharedPref.getString("reports_json", "[]")
            Log.d("LedgerReportActivity", "📄 저장된 JSON 원본: $reportsJson")
            
            val reportsArray = org.json.JSONArray(reportsJson)
            Log.d("LedgerReportActivity", "📊 JSON Array 길이: ${reportsArray.length()}")
            
            // JSONArray를 Set<String>으로 변환
            val reports = mutableSetOf<String>()
            for (i in 0 until reportsArray.length()) {
                val reportObj = reportsArray.getJSONObject(i)
                Log.d("LedgerReportActivity", "📋 리포트 $i: ${reportObj.optString("title", "제목없음")}")
                reports.add(reportObj.toString())
            }
            
            Log.d("LedgerReportActivity", "📈 최종 로컬 리포트 수: ${reports.size}")
            
            // 로컬 데이터가 없으면 테스트 데이터 추가
            if (reports.isEmpty()) {
                Log.d("LedgerReportActivity", "❌ 로컬 데이터 없음 - 테스트 데이터 생성")
                val testReports = createTestReports()
                
                // 테스트 데이터를 SharedPreferences에도 저장
                saveTestReportsToLocal(testReports)
                
                showReportsList(testReports)
            } else {
                Log.d("LedgerReportActivity", "✅ 로컬 리포트 목록 표시 시작 (${reports.size}개)")
                showReportsList(reports)
            }
            
            // 통계 업데이트 (매번 호출)
            val contentView = findViewById<android.widget.FrameLayout>(R.id.content_container)?.getChildAt(0)
            contentView?.let { updateStatistics(it) }
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "리포트 로드 실패", e)
            showEmptyState()
            Toast.makeText(this, "리포트 목록을 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
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
    
    private fun createTestReports(): Set<String> {
        Log.d("LedgerReportActivity", "테스트 리포트 데이터 생성 중...")
        
        val currentTime = System.currentTimeMillis()
        val testReports = mutableSetOf<String>()
        
        // 1. 종합 운영 평가 리포트
        val comprehensiveReport = JSONObject().apply {
            put("id", 1)
            put("title", "2025년 종합 운영 평가 리포트")
            put("type", "comprehensive")
            put("created_at", currentTime - 86400000) // 1일 전
            put("creator", "AI 시스템")
            put("content", """
                📊 2025년 동아리 종합 운영 평가
                
                💰 재정 현황:
                • 총 수입: 1,850,000원
                • 총 지출: 1,420,000원
                • 순이익: 430,000원
                • 예산 대비 달성률: 92%
                
                📈 활동 분석:
                • 정기 모임: 24회 (목표 대비 100%)
                • 대외 활동: 8회
                • 회원 참여율: 87%
                
                ⭐ 개선 제안:
                • 홍보 예산 10% 증액 권장
                • 신입 회원 모집 강화 필요
                • 온라인 활동 비중 확대
            """.trimIndent())
        }
        testReports.add(comprehensiveReport.toString())
        
        // 2. 타 동아리 비교 분석 리포트
        val comparisonReport = JSONObject().apply {
            put("id", 2)
            put("title", "유사 동아리 비교 분석 리포트")
            put("type", "comparison")
            put("created_at", currentTime - 172800000) // 2일 전
            put("creator", "AI 시스템")
            put("content", """
                🔄 유사 동아리 비교 분석
                
                📊 재정 비교 (월평균):
                • 우리 동아리: 154,000원
                • A 동아리: 187,000원 (+21%)
                • B 동아리: 142,000원 (-8%)
                
                🎯 활동 비교:
                • 정기 모임: 우리(24) vs 평균(22) ✅
                • 대외 활동: 우리(8) vs 평균(12) ⚠️
                • 회원 수: 우리(28) vs 평균(32)
                
                💡 인사이트:
                • 대외 활동 참여도 증대 필요
                • 재정 관리는 우수한 수준
                • 회원 모집에 더 집중 권장
            """.trimIndent())
        }
        testReports.add(comparisonReport.toString())
        
        // 3. 이전 분기 종합 리포트
        val quarterReport = JSONObject().apply {
            put("id", 3)
            put("title", "2024년 4분기 종합 리포트")
            put("type", "comprehensive")
            put("created_at", currentTime - 604800000) // 1주일 전
            put("creator", "AI 시스템")
            put("content", """
                📅 2024년 4분기 종합 리포트
                
                💰 분기별 성과:
                • 수입: 520,000원
                • 지출: 380,000원
                • 순이익: 140,000원
                
                📊 주요 성과:
                • 신입 회원 8명 유치
                • 대외 행사 3회 참여
                • SNS 팔로워 25% 증가
                
                🎯 다음 분기 목표:
                • 예산 20% 증액
                • 정기 행사 확대
                • 졸업생 네트워크 구축
            """.trimIndent())
        }
        testReports.add(quarterReport.toString())
        
        Log.d("LedgerReportActivity", "테스트 리포트 ${testReports.size}개 생성 완료")
        return testReports
    }

    private fun saveTestReportsToLocal(testReports: Set<String>) {
        try {
            val clubId = getCurrentClubId()
            val sharedPref = getSharedPreferences("ai_reports_club_$clubId", Context.MODE_PRIVATE)
            
            Log.d("LedgerReportActivity", "🔄 테스트 데이터를 SharedPreferences에 저장 중...")
            
            val reportsArray = org.json.JSONArray()
            testReports.forEach { reportJson ->
                val reportObj = JSONObject(reportJson)
                reportsArray.put(reportObj)
                Log.d("LedgerReportActivity", "💾 저장: ${reportObj.optString("title")}")
            }
            
            val saved = sharedPref.edit()
                .putString("reports_json", reportsArray.toString())
                .commit()
            
            Log.d("LedgerReportActivity", "✅ SharedPreferences 저장 결과: $saved")
            Log.d("LedgerReportActivity", "📦 저장된 데이터 크기: ${reportsArray.length()}개")
            
            // 저장 확인
            val savedData = sharedPref.getString("reports_json", "[]")
            Log.d("LedgerReportActivity", "🔍 저장 확인: ${savedData?.length ?: 0} 문자")
            
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "❌ 테스트 데이터 저장 실패", e)
        }
    }

    private fun showReportsList(reports: Set<String>) {
        try {
            Log.d("LedgerReportActivity", "=== showReportsList 시작 ===")
            Log.d("LedgerReportActivity", "📊 입력된 리포트 수: ${reports.size}")
            Log.d("LedgerReportActivity", "🔧 recyclerView 상태: ${recyclerView != null}")
            Log.d("LedgerReportActivity", "🔧 emptyState 상태: ${emptyState != null}")
            Log.d("LedgerReportActivity", "🔧 reportsAdapter 상태: ${::reportsAdapter.isInitialized}")
            
            // 각 리포트 내용 로깅
            reports.forEachIndexed { index, report ->
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
            val sortedReports = reports.sortedByDescending { 
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
            } else {
                Log.e("LedgerReportActivity", "❌ reportsAdapter가 초기화되지 않음!")
            }
            
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "❌ 리포트 목록 표시 실패", e)
            e.printStackTrace()
            showEmptyState()
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
        Log.d("LedgerReportActivity", "🔄 백엔드 리포트 데이터를 기존 리스트에 합치기 시작")
        
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        
        // 연간 리포트 조회 (2024년, 2025년)
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
                • 총 수입: ${String.format("%,d", income)}원
                • 총 지출: ${String.format("%,d", expense)}원
                • 순이익: ${String.format("%,d", net)}원
                
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
        Log.d("LedgerReportActivity", "🔗 백엔드 리포트를 기존 리스트에 합치기 - 백엔드 리포트 수: ${backendReports.size}")
        
        try {
            // 기존 로컬 리포트 가져오기
            val clubId = getCurrentClubId()
            val sharedPref = getSharedPreferences("ai_reports_club_$clubId", Context.MODE_PRIVATE)
            val existingReportsJson = sharedPref.getString("reports_json", "[]")
            val existingReportsArray = org.json.JSONArray(existingReportsJson)
            
            Log.d("LedgerReportActivity", "📦 기존 로컬 리포트 수: ${existingReportsArray.length()}")
            
            // 기존 리포트들을 Set으로 변환
            val allReports = mutableSetOf<String>()
            
            // 1. 기존 로컬 리포트 추가
            for (i in 0 until existingReportsArray.length()) {
                allReports.add(existingReportsArray.getJSONObject(i).toString())
            }
            
            // 2. 백엔드 리포트 추가 (중복 체크)
            backendReports.forEach { backendReport ->
                try {
                    val backendReportObj = JSONObject(backendReport)
                    val backendId = backendReportObj.optInt("backend_id", -1)
                    
                    // 중복 체크: 같은 backend_id가 이미 있는지 확인
                    var isDuplicate = false
                    for (existingReport in allReports) {
                        val existingReportObj = JSONObject(existingReport)
                        if (existingReportObj.optInt("backend_id", -1) == backendId && backendId != -1) {
                            isDuplicate = true
                            break
                        }
                    }
                    
                    if (!isDuplicate) {
                        allReports.add(backendReport)
                        Log.d("LedgerReportActivity", "➕ 백엔드 리포트 추가: ${backendReportObj.optString("title")}")
                    } else {
                        Log.d("LedgerReportActivity", "⏭️ 중복 백엔드 리포트 건너뛰기: ${backendReportObj.optString("title")}")
                    }
                } catch (e: Exception) {
                    Log.e("LedgerReportActivity", "❌ 백엔드 리포트 처리 실패", e)
                }
            }
            
            Log.d("LedgerReportActivity", "📈 최종 합쳐진 리포트 수: ${allReports.size}")
            
            // 3. 화면에 합쳐진 리스트 표시
            if (allReports.isNotEmpty()) {
                showReportsList(allReports)
                
                // 4. SharedPreferences에도 저장 (백엔드 데이터 포함)
                val mergedArray = org.json.JSONArray()
                allReports.forEach { report ->
                    mergedArray.put(JSONObject(report))
                }
                
                sharedPref.edit()
                    .putString("reports_json", mergedArray.toString())
                    .apply()
                
                Log.d("LedgerReportActivity", "✅ 합쳐진 데이터 SharedPreferences 저장 완료")
            }
            
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "❌ 백엔드 리포트 합치기 실패", e)
        }
    }

    private fun deleteReport(reportData: JSONObject) {
        try {
            val sharedPref = getSharedPreferences("ai_reports", Context.MODE_PRIVATE)
            val reports = sharedPref.getStringSet("reports", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            
            val reportJson = reportData.toString()
            reports.remove(reportJson)
            
            sharedPref.edit()
                .putStringSet("reports", reports)
                .apply()
            
            loadAIReports()
            Toast.makeText(this, "리포트가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "리포트 삭제 실패", e)
            Toast.makeText(this, "삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateStatistics(contentView: View) {
        try {
            val sharedPref = getSharedPreferences("ai_reports", Context.MODE_PRIVATE)
            val reports = sharedPref.getStringSet("reports", emptySet()) ?: emptySet()
            
            // 총 리포트 수 업데이트
            val tvTotalReports = contentView.findViewById<TextView>(R.id.tv_total_reports)
            tvTotalReports?.text = reports.size.toString()
            
            // 최근 생성일 업데이트
            val tvRecentDate = contentView.findViewById<TextView>(R.id.tv_recent_date)
            if (reports.isNotEmpty()) {
                val latestReport = reports.maxByOrNull { 
                    try {
                        JSONObject(it).getLong("created_at")
                    } catch (e: Exception) { 
                        0L 
                    }
                }
                
                latestReport?.let { reportJson ->
                    try {
                        val reportData = JSONObject(reportJson)
                        val createdAt = reportData.getLong("created_at")
                        val dateFormat = SimpleDateFormat("MM/dd", Locale.KOREA)
                        tvRecentDate?.text = dateFormat.format(Date(createdAt))
                    } catch (e: Exception) {
                        tvRecentDate?.text = "오늘"
                    }
                }
            } else {
                tvRecentDate?.text = "없음"
            }
            
            Log.d("LedgerReportActivity", "통계 업데이트 완료: ${reports.size}개 리포트")
        } catch (e: Exception) {
            Log.e("LedgerReportActivity", "통계 업데이트 실패", e)
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
        return String.format("%,d", amount)
    }
    
    override fun getCurrentClubId(): Int {
        val clubId = intent.getIntExtra("club_id", 4) // Intent에서 가져오거나 기본값 4
        Log.d("LedgerReportActivity", "🔑 getCurrentClubId 호출됨 - Intent에서 받은 값: ${intent.getIntExtra("club_id", -1)}, 최종 반환값: $clubId")
        return clubId
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CREATE_REPORT && resultCode == android.app.Activity.RESULT_OK) {
            val reportCreated = data?.getBooleanExtra("report_created", false) ?: false
            val reportTitle = data?.getStringExtra("report_title") ?: ""
            
            if (reportCreated) {
                Log.d("LedgerReportActivity", "🎉 새 리포트 생성 완료!")
                Log.d("LedgerReportActivity", "📋 리포트 제목: $reportTitle")
                
                // 사용자에게 성공 알림
                Toast.makeText(this, "🤖 AI 리포트가 생성되었습니다!\n$reportTitle", Toast.LENGTH_LONG).show()
                
                // 즉시 목록 새로고침 (강제)
                Log.d("LedgerReportActivity", "🔄 리포트 생성 후 강제 새로고침 시작")
                
                // 1초 후에 새로고침 (백엔드 저장 완료 대기)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    Log.d("LedgerReportActivity", "⏰ 1초 후 새로고침 실행")
                    loadAIReports()
                    
                    // 통계 업데이트
                    val contentView = findViewById<android.widget.FrameLayout>(R.id.content_container)?.getChildAt(0)
                    contentView?.let { updateStatistics(it) }
                }, 1000)
            }
        }
    }
}