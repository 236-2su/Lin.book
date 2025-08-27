package com.example.myapplication

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.ApiService
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

class LedgerReportCreateActivity : BaseActivity() {
    
    private var selectedReportType = ""
    private var progressDialog: ProgressDialog? = null

    override fun setupContent(savedInstanceState: Bundle?) {
        // 앱 제목을 "리포트 생성"으로 설정
        setAppTitle("리포트 생성")
        

        
        // LedgerReportCreateActivity 내용을 content_container에 추가
        val contentContainer = findViewById<android.widget.FrameLayout>(R.id.content_container)
        val contentView = layoutInflater.inflate(R.layout.ledger_report_create, null)
        contentContainer.addView(contentView)

        // 뒤로가기 버튼 표시
        showBackButton()
        
        // 버튼 이벤트 설정
        setupButtonClickEvents(contentView)
    }
    
    private fun setupButtonClickEvents(contentView: View) {
        // 리포트 종류 선택 드롭다운
        setupReportTypeSelection(contentView)
        
        // 생성하기 버튼
        contentView.findViewById<Button>(R.id.btn_create_report)?.setOnClickListener {
            generateAIReport(contentView)
        }
    }
    
    private fun setupReportTypeSelection(contentView: View) {
        val dropdown = contentView.findViewById<LinearLayout>(R.id.dropdown_report_type)
        val selectedText = contentView.findViewById<TextView>(R.id.tv_selected_report_type)
        
        dropdown?.setOnClickListener {
            showReportTypeDialog(selectedText)
        }
    }
    
    private fun showReportTypeDialog(selectedText: TextView?) {
        val reportTypes = arrayOf(
            "📊 연간 종합 분석",
            "🏆 타 동아리 비교 분석",
            "📅 년도별 이벤트 비교 분석"
        )
        val reportTypeKeys = arrayOf(
            "yearly", 
            "comparison",
            "event_comparison"
        )
        
        AlertDialog.Builder(this)
            .setTitle("리포트 종류 선택")
            .setItems(reportTypes) { _, which ->
                selectedReportType = reportTypeKeys[which]
                selectedText?.text = reportTypes[which]
                selectedText?.setTextColor(Color.parseColor("#333333"))
            }
            .show()
    }
    

    
    private fun generateAIReport(contentView: View) {
        // 입력값 검증
        val reportName = contentView.findViewById<EditText>(R.id.et_report_name)?.text?.toString()
        
        if (reportName.isNullOrBlank()) {
            Toast.makeText(this, "리포트명을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedReportType.isEmpty()) {
            Toast.makeText(this, "리포트 종류를 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 로딩 상태 표시
        showProgressDialog("AI 리포트를 생성하고 있습니다...\n30-60초 정도 소요됩니다.")
        
        lifecycleScope.launch {
            try {
                val clubId = getCurrentClubId()
                
                // 백엔드 API 호출
                callBackendAPI(clubId, selectedReportType, reportName)
                
            } catch (e: Exception) {
                hideProgressDialog()
                Log.e("LedgerReportCreate", "API 호출 실패", e)
                Toast.makeText(this@LedgerReportCreateActivity, 
                    "오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun saveReportLocally(title: String, content: String, type: String) {
        Log.d("LedgerReportCreate", "로컬 저장 시작 - 제목: $title, 타입: $type")
        
        val clubId = getCurrentClubId()
        val sharedPref = getSharedPreferences("ai_reports_club_$clubId", Context.MODE_PRIVATE)
        
        // StringSet 대신 JSON Array로 저장 (더 안정적)
        val existingReportsJson = sharedPref.getString("reports_json", "[]")
        val existingReportsArray = org.json.JSONArray(existingReportsJson)
        
        Log.d("LedgerReportCreate", "기존 저장된 리포트 수: ${existingReportsArray.length()}")
        
        val reportData = JSONObject().apply {
            put("id", System.currentTimeMillis())
            put("title", title)
            put("content", content)
            put("type", type)
            put("created_at", System.currentTimeMillis())
            put("creator", "사용자") // 실제로는 로그인한 사용자 정보 사용
            put("backend_id", -1) // 로컬 생성은 -1로 구분
        }
        
        // 새 리포트를 배열에 추가
        existingReportsArray.put(reportData)
        
        Log.d("LedgerReportCreate", "리포트 JSON: ${reportData}")
        Log.d("LedgerReportCreate", "저장할 리포트 수: ${existingReportsArray.length()}")
        
        val success = sharedPref.edit()
            .putString("reports_json", existingReportsArray.toString())
            .commit() // apply() 대신 commit()으로 동기 저장
            
        Log.d("LedgerReportCreate", "저장 결과: $success")
        
        // 저장 확인
        val verifyReportsJson = sharedPref.getString("reports_json", "[]")
        val verifyReportsArray = org.json.JSONArray(verifyReportsJson)
        Log.d("LedgerReportCreate", "저장 확인 - 실제 저장된 리포트 수: ${verifyReportsArray.length()}")
    }
    
    private fun showSuccessDialog(content: String) {
        AlertDialog.Builder(this)
            .setTitle("✅ 리포트 생성 완료")
            .setMessage("AI 리포트가 성공적으로 생성되었습니다!\n\n📊 분석 결과를 확인하시겠습니까?")
            .setPositiveButton("목록으로 돌아가기") { _, _ ->
                finish() // 이전 화면으로 돌아가기
            }
            .setNeutralButton("미리보기") { _, _ ->
                showReportPreview(content)
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showReportPreview(content: String) {
        // 스크롤 가능한 텍스트 뷰 생성
        val textView = TextView(this).apply {
            text = content
            setPadding(40, 40, 40, 40)
            textSize = 14f
            setTextColor(Color.parseColor("#333333"))
        }
        
        val scrollView = android.widget.ScrollView(this).apply {
            addView(textView)
        }
        
        AlertDialog.Builder(this)
            .setTitle("📊 AI 리포트 미리보기")
            .setView(scrollView)
            .setPositiveButton("닫기", null)
            .setNeutralButton("목록으로") { _, _ ->
                finish()
            }
            .show()
    }
    
    private fun showProgressDialog(message: String) {
        progressDialog = ProgressDialog(this).apply {
            setMessage(message)
            setCancelable(false)
            show()
        }
    }
    
    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
    
    override fun getCurrentClubId(): Int {
        val clubId = intent.getIntExtra("club_id", 4) // Intent에서 가져오거나 기본값 4
        Log.d("LedgerReportCreate", "🔑 getCurrentClubId 호출됨 - Intent에서 받은 값: ${intent.getIntExtra("club_id", -1)}, 최종 반환값: $clubId")
        return clubId
    }
    
    private fun formatBackendReportToText(reportData: ApiService.AIReportResponse): String {
        val periodText = if (reportData.month != null) {
            "${reportData.year}년 ${reportData.month}월"
        } else {
            "${reportData.year}년"
        }
        
        val summary = reportData.summary
        val income = summary["income"] ?: 0
        val expense = summary["expense"] ?: 0
        val net = summary["net"] ?: 0
        
        return buildString {
            // 📊 헤더
            appendLine("🤖 Hey-Bi AI 재정 분석 리포트")
            appendLine("📅 분석 기간: $periodText")
            appendLine("🔍 분석 엔진: Hey-Bi v2.0")
            appendLine("=".repeat(40))
            appendLine()
            
            // 🎯 핵심 인사이트 (AI 분석)
            appendLine("🎯 AI 핵심 인사이트")
            generateAIInsights(income, expense, net)
            appendLine()
            
            // 💰 재정 현황 요약
            appendLine("💰 재정 현황 요약")
            appendLine("• 총 수입: ${String.format("%,d", income)}원 ${getAmountEmoji(income)}")
            appendLine("• 총 지출: ${String.format("%,d", expense)}원 ${getAmountEmoji(expense)}")
            appendLine("• 순수익: ${String.format("%,d", net)}원 ${getNetAmountEmoji(net)}")
            appendLine("• 재정 건전성: ${getFinancialHealth(income, expense, net)}")
            appendLine()
            
            // 📊 AI 재정 분석
            appendLine("📊 AI 재정 상태 분석")
            generateFinancialAnalysis(income, expense, net)
            appendLine()
            
            if (reportData.by_type.isNotEmpty()) {
                appendLine("📋 거래 유형별 AI 분석")
                reportData.by_type.forEach { typeData ->
                    val type = typeData["type"] as? String ?: "기타"
                    val typeIncome = typeData["income"] as? Int ?: 0
                    val typeExpense = typeData["expense"] as? Int ?: 0
                    appendLine("• $type: 수입 ${String.format("%,d", typeIncome)}원, 지출 ${String.format("%,d", typeExpense)}원")
                    appendLine("  → ${getTypeAnalysis(type, typeIncome, typeExpense)}")
                }
                appendLine()
            }
            
            if (reportData.by_payment_method.isNotEmpty()) {
                appendLine("💳 결제 수단별 AI 분석")
                reportData.by_payment_method.forEach { paymentData ->
                    val method = paymentData["payment_method"] as? String ?: "기타"
                    val methodIncome = paymentData["income"] as? Int ?: 0
                    val methodExpense = paymentData["expense"] as? Int ?: 0
                    appendLine("• $method: 수입 ${String.format("%,d", methodIncome)}원, 지출 ${String.format("%,d", methodExpense)}원")
                    appendLine("  → ${getPaymentMethodAnalysis(method, methodIncome, methodExpense)}")
                }
                appendLine()
            }
            
            if (reportData.by_event.isNotEmpty()) {
                appendLine("🎯 행사별 AI 분석")
                reportData.by_event.forEach { eventData ->
                    val eventName = eventData["event_name"] as? String ?: "일반 활동"
                    val eventIncome = eventData["income"] as? Int ?: 0
                    val eventExpense = eventData["expense"] as? Int ?: 0
                    val eventNet = eventIncome - eventExpense
                    appendLine("• $eventName")
                    appendLine("  수입: ${String.format("%,d", eventIncome)}원, 지출: ${String.format("%,d", eventExpense)}원")
                    appendLine("  순수익: ${String.format("%,d", eventNet)}원 ${getNetAmountEmoji(eventNet)}")
                    appendLine("  → ${getEventAnalysis(eventName, eventIncome, eventExpense, eventNet)}")
                    appendLine()
                }
            }
            
            // 💡 AI 제안사항
            appendLine("💡 AI 맞춤 제안사항")
            generateAIRecommendations(income, expense, net, reportData)
            appendLine()
            
            // 📈 향후 전망
            appendLine("📈 AI 예측 및 전망")
            generateFuturePrediction(income, expense, net)
            appendLine()
            
            appendLine("=".repeat(40))
            appendLine("✨ Hey-Bi AI 분석 완료")
            appendLine("📊 이 리포트는 실제 동아리 데이터를 AI가 분석한 결과입니다")
            appendLine("🔄 정기적인 분석으로 더 정확한 인사이트를 제공받으세요")
        }
    }
    
    private fun generateAIInsights(income: Int, expense: Int, net: Int): String {
        return buildString {
            when {
                net > 100000 -> {
                    appendLine("🌟 우수한 재정 관리 상태입니다!")
                    appendLine("• 현재 흑자 운영으로 매우 안정적인 상태를 유지하고 있습니다")
                    appendLine("• 여유 자금을 활용한 투자나 신규 프로젝트를 고려해보세요")
                }
                net > 0 -> {
                    appendLine("😊 안정적인 재정 운영 중입니다")
                    appendLine("• 수입과 지출의 균형이 잘 맞춰져 있습니다")
                    appendLine("• 현재 수준을 유지하며 예비비 확보를 권장합니다")
                }
                net > -50000 -> {
                    appendLine("⚠️ 주의가 필요한 재정 상황입니다")
                    appendLine("• 지출이 수입을 약간 초과하고 있어 개선이 필요합니다")
                    appendLine("• 불필요한 지출 항목 점검과 수입원 다각화를 권장합니다")
                }
                else -> {
                    appendLine("🚨 긴급한 재정 개선이 필요합니다")
                    appendLine("• 즉시 예산 재조정과 절약 계획이 필요한 상황입니다")
                    appendLine("• 필수 지출 외 모든 항목을 재검토하고 추가 수입원을 확보하세요")
                }
            }
            
            // 데이터가 없는 경우 (0원)
            if (income == 0 && expense == 0) {
                clear()
                appendLine("📋 데이터 수집이 필요합니다")
                appendLine("• 현재 분석할 수 있는 거래 데이터가 없습니다")
                appendLine("• 장부에 거래 내역을 입력하시면 더 정확한 AI 분석이 가능합니다")
                appendLine("• 최근 3개월간의 수입/지출 내역을 추가해보세요")
            }
        }
    }
    
    private fun generateFinancialAnalysis(income: Int, expense: Int, net: Int): String {
        return buildString {
            val ratio = if (income > 0) (expense.toDouble() / income * 100).toInt() else 0
            
            appendLine("• 지출 비율: ${ratio}% ${if (ratio < 80) "👍 적정" else if (ratio < 100) "⚠️ 주의" else "🚨 위험"}")
            
            when {
                ratio < 70 -> appendLine("• AI 평가: 매우 효율적인 예산 관리 👏")
                ratio < 85 -> appendLine("• AI 평가: 양호한 재정 관리 상태 ✅")
                ratio < 100 -> appendLine("• AI 평가: 지출 관리 개선 필요 ⚠️")
                else -> appendLine("• AI 평가: 즉시 지출 절약 필요 🚨")
            }
            
            if (income > 0) {
                val savingRate = ((income - expense).toDouble() / income * 100).toInt()
                appendLine("• 저축률: ${savingRate}% ${if (savingRate > 20) "🌟 우수" else if (savingRate > 10) "👍 양호" else "📈 개선필요"}")
            }
        }
    }
    
    private fun generateAIRecommendations(income: Int, expense: Int, net: Int, reportData: ApiService.AIReportResponse): String {
        return buildString {
            // 기본 추천사항
            when {
                net > 50000 -> {
                    appendLine("• 🎯 여유 자금 활용: 동아리 발전을 위한 장기 투자 계획을 수립하세요")
                    appendLine("• 📈 성장 전략: 신규 장비 구입이나 대규모 이벤트를 기획해보세요")
                    appendLine("• 💡 예비비 관리: 현재 순이익의 30%를 비상금으로 적립 권장")
                }
                net > 0 -> {
                    appendLine("• ⚖️ 균형 유지: 현재 수준의 재정 관리를 지속하세요")
                    appendLine("• 🛡️ 리스크 관리: 예상치 못한 지출에 대비한 예비비 확보")
                    appendLine("• 📊 정기 점검: 월 1회 재정 현황 점검으로 안정성 유지")
                }
                else -> {
                    appendLine("• 🔍 지출 분석: 필수 지출과 선택적 지출을 구분하여 관리")
                    appendLine("• 💰 수입 증대: 후원이나 사업 아이템을 통한 추가 수입원 모색")
                    appendLine("• ⏰ 긴급 계획: 다음 달까지 지출 20% 절약 목표 설정")
                }
            }
            
            // 데이터 기반 추천
            if (reportData.by_payment_method.isNotEmpty()) {
                appendLine("• 💳 결제 최적화: 가장 효율적인 결제 수단을 우선 활용하세요")
            }
            
            if (reportData.by_event.isNotEmpty()) {
                appendLine("• 🎪 행사 기획: 수익성 높은 행사 형태를 참고하여 향후 계획 수립")
            }
        }
    }
    
    private fun generateFuturePrediction(income: Int, expense: Int, net: Int): String {
        return buildString {
            val monthlyTrend = when {
                net > 50000 -> "매우 긍정적"
                net > 0 -> "안정적"
                net > -30000 -> "주의 필요"
                else -> "개선 필요"
            }
            
            appendLine("• 📊 향후 3개월 전망: $monthlyTrend")
            
            when {
                net > 0 -> {
                    val projectedSavings = net * 3
                    appendLine("• 💰 예상 누적 잉여금: ${String.format("%,d", projectedSavings)}원")
                    appendLine("• 🎯 달성 가능 목표: 대규모 프로젝트 실행 가능")
                }
                else -> {
                    appendLine("• ⚠️ 예상 부족 금액: ${String.format("%,d", Math.abs(net * 3))}원")
                    appendLine("• 🛠️ 필요 조치: 즉시 재정 개선 계획 실행 필요")
                }
            }
            
            appendLine("• 📈 AI 권장 주기: 월 1회 정기 분석으로 변화 추적")
        }
    }
    
    // Helper 함수들
    private fun getAmountEmoji(amount: Int): String = when {
        amount > 500000 -> "🔥"
        amount > 100000 -> "💪"
        amount > 50000 -> "👍"
        amount > 0 -> "📊"
        else -> "📉"
    }
    
    private fun getNetAmountEmoji(net: Int): String = when {
        net > 100000 -> "🌟"
        net > 50000 -> "💚"
        net > 0 -> "✅"
        net > -50000 -> "⚠️"
        else -> "🚨"
    }
    
    private fun getFinancialHealth(income: Int, expense: Int, net: Int): String = when {
        net > 100000 -> "🌟 매우 우수"
        net > 50000 -> "💚 우수"
        net > 0 -> "✅ 양호"
        net > -50000 -> "⚠️ 주의"
        else -> "🚨 위험"
    }
    
    private fun getTypeAnalysis(type: String, income: Int, expense: Int): String {
        val net = income - expense
        return when {
            net > 0 -> "수익성 항목으로 지속 권장"
            net == 0 -> "수지균형 상태, 효율성 검토 필요"
            else -> "비용 절감 또는 수입 증대 방안 검토"
        }
    }
    
    private fun getPaymentMethodAnalysis(method: String, income: Int, expense: Int): String {
        return when (method.lowercase()) {
            "현금" -> "현금 관리의 투명성 확보가 중요합니다"
            "카드", "신용카드" -> "포인트 적립이나 할인 혜택 활용을 고려하세요"
            "계좌이체" -> "가장 투명하고 추적 가능한 결제 방식입니다"
            else -> "결제 수단별 장단점을 고려한 선택이 필요합니다"
        }
    }
    
    private fun getEventAnalysis(eventName: String, income: Int, expense: Int, net: Int): String {
        return when {
            net > 50000 -> "🌟 수익성이 높은 우수한 행사입니다. 유사한 행사를 더 기획해보세요"
            net > 0 -> "✅ 수익을 창출한 성공적인 행사입니다"
            net > -30000 -> "⚠️ 소폭 적자이지만 참여도나 만족도를 고려하면 의미있는 행사입니다"
            else -> "🚨 비용 대비 효과를 재검토하고 개선 방안을 모색하세요"
        }
    }
    
    private fun callBackendAPI(clubId: Int, reportType: String, reportName: String) {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
        
        // 먼저 장부 목록을 가져와서 첫 번째 장부 ID를 사용
        fetchFirstLedgerIdAndCreateReport(clubId, reportType, reportName, currentYear, currentMonth)
    }
    
    private fun fetchFirstLedgerIdAndCreateReport(clubId: Int, reportType: String, reportName: String, currentYear: Int, currentMonth: Int) {
        Log.d("LedgerReportCreate", "장부 목록 조회 시작 - Club: $clubId")
        
        ApiClient.getApiService().getLedgerList(clubId).enqueue(object : retrofit2.Callback<List<LedgerApiItem>> {
            override fun onResponse(
                call: retrofit2.Call<List<LedgerApiItem>>,
                response: retrofit2.Response<List<LedgerApiItem>>
            ) {
                if (response.isSuccessful) {
                    val ledgers = response.body()
                    if (!ledgers.isNullOrEmpty()) {
                        Log.d("LedgerReportCreate", "📋 장부 목록 조회 성공! 총 ${ledgers.size}개 장부")
                        
                        // 장부가 1개면 자동 선택, 여러개면 사용자 선택
                        if (ledgers.size == 1) {
                            val onlyLedgerId = ledgers[0].id
                            Log.d("LedgerReportCreate", "장부 1개 자동 선택: ${ledgers[0].name} (ID: $onlyLedgerId)")
                            createReportWithLedgerId(clubId, onlyLedgerId, reportType, reportName, currentYear, currentMonth)
                        } else {
                            Log.d("LedgerReportCreate", "장부 ${ledgers.size}개 발견 - 사용자 선택 필요")
                            showLedgerSelectionForCreate(clubId, ledgers, reportType, reportName, currentYear, currentMonth)
                        }
                    } else {
                        hideProgressDialog()
                        showErrorMessage("동아리에 장부가 없습니다. 장부를 먼저 생성해주세요.")
                    }
                } else {
                    hideProgressDialog()
                    Log.e("LedgerReportCreate", "장부 목록 조회 실패: ${response.code()}")
                    showErrorMessage("장부 목록을 가져올 수 없습니다. (${response.code()})")
                }
            }
            
            override fun onFailure(call: retrofit2.Call<List<LedgerApiItem>>, t: Throwable) {
                hideProgressDialog()
                Log.e("LedgerReportCreate", "장부 목록 조회 네트워크 오류", t)
                showErrorMessage("네트워크 오류가 발생했습니다: ${t.message}")
            }
        })
    }
    
    private fun createReportWithLedgerId(clubId: Int, ledgerId: Int, reportType: String, reportName: String, currentYear: Int, currentMonth: Int) {
        Log.d("LedgerReportCreate", "리포트 생성 API 호출 시작 - Club: $clubId, Ledger: $ledgerId, Type: $reportType")
        
        val call = when (reportType) {
            "yearly" -> {
                // 연간 종합 분석
                Log.d("LedgerReportCreateActivity", "📊 연간 종합 분석 리포트 생성 중...")
                ApiClient.getApiService().createYearlyReport(clubId, ledgerId, currentYear)
            }
            "comparison" -> {
                // 유사 동아리 비교 리포트
                Log.d("LedgerReportCreateActivity", "🔄 비교 분석 리포트 생성 중...")
                ApiClient.getApiService().createSimilarClubsReport(clubId, currentYear)
            }
            "event_comparison" -> {
                // 년도별 이벤트 비교 분석 - 연간 데이터를 활용
                Log.d("LedgerReportCreateActivity", "📅 년도별 이벤트 비교 분석 리포트 생성 중...")
                ApiClient.getApiService().createYearlyReport(clubId, ledgerId, currentYear)
            }
            else -> {
                // 기본값: 연간 리포트
                Log.d("LedgerReportCreateActivity", "📊 기본 연간 리포트 생성 중...")
                ApiClient.getApiService().createYearlyReport(clubId, ledgerId, currentYear)
            }
        }
        
        call.enqueue(object : retrofit2.Callback<ApiService.AIReportResponse> {
            override fun onResponse(
                call: retrofit2.Call<ApiService.AIReportResponse>,
                response: retrofit2.Response<ApiService.AIReportResponse>
            ) {
                hideProgressDialog()
                
                if (response.isSuccessful) {
                    val reportData = response.body()
                    if (reportData != null) {
                        Log.d("LedgerReportCreate", "API 응답 성공: 클럽 ${reportData.club_id}, ${reportData.year}년")
                        
                        // 백엔드 응답을 사용자 친화적인 텍스트로 변환 (타입 정보 포함)
                        val typeDisplayName = when (reportType) {
                            "yearly" -> "연간종합분석"
                            "comparison" -> "비교분석"
                            "event_comparison" -> "이벤트비교분석"
                            else -> "AI분석"
                        }
                        
                        val reportTitle = if (reportData.month != null) {
                            "${reportName.ifEmpty { "${reportData.year}년 ${reportData.month}월 ${typeDisplayName} 리포트" }}"
                        } else {
                            "${reportName.ifEmpty { "${reportData.year}년 연간 ${typeDisplayName} 리포트" }}"
                        }
                        
                        val reportContent = if (reportType == "event_comparison") {
                            formatEventComparisonReport(reportData, clubId, ledgerId)
                        } else {
                            formatBackendReportToText(reportData)
                        }
                        
                        // 성공 - 로컬 저장도 하고 백엔드에서도 저장됨
                        saveReportLocally(
                            title = reportTitle,
                            content = reportContent,
                            type = reportType
                        )
                        
                        // 추가 디버깅 - 저장 후 즉시 확인
                        val clubId = getCurrentClubId()
                        val sharedPref = getSharedPreferences("ai_reports_club_$clubId", Context.MODE_PRIVATE)
                        val savedReportsJson = sharedPref.getString("reports_json", "[]")
                        val savedReportsArray = org.json.JSONArray(savedReportsJson)
                        Log.d("LedgerReportCreate", "💾 저장 후 즉시 확인 - 총 저장된 리포트 수: ${savedReportsArray.length()}")
                        for (i in 0 until savedReportsArray.length()) {
                            val report = savedReportsArray.getJSONObject(i)
                            Log.d("LedgerReportCreate", "   - 리포트 ${i+1}: ${report.optString("title")} (타입: ${report.optString("type")})")
                        }
                        
                        Log.d("LedgerReportCreate", "✅ 백엔드 리포트 생성 성공!")
                        Log.d("LedgerReportCreate", "   - 제목: $reportTitle")
                        Log.d("LedgerReportCreate", "   - 타입: $reportType")
                        Log.d("LedgerReportCreate", "   - 클럽 ID: $clubId")
                        
                        // 생성 성공 시 결과 전달하고 화면 종료
                        val resultIntent = android.content.Intent()
                        resultIntent.putExtra("report_created", true)
                        resultIntent.putExtra("report_title", reportTitle)
                        setResult(android.app.Activity.RESULT_OK, resultIntent)
                        
                        showSuccessDialog(reportContent)
                    } else {
                        Log.e("LedgerReportCreate", "응답 데이터가 null")
                        showErrorMessage("서버 응답 데이터가 올바르지 않습니다")
                    }
                } else {
                    Log.e("LedgerReportCreate", "API 호출 실패: ${response.code()} - ${response.message()}")
                    showErrorMessage("리포트 생성 실패: ${response.message()}")
                }
            }
            
            override fun onFailure(call: retrofit2.Call<ApiService.AIReportResponse>, t: Throwable) {
                hideProgressDialog()
                Log.e("LedgerReportCreate", "네트워크 오류", t)
                showErrorMessage("네트워크 오류가 발생했습니다: ${t.message}")
            }
        })
    }
    
    private fun showLedgerSelectionForCreate(
        clubId: Int, 
        ledgers: List<LedgerApiItem>, 
        reportType: String, 
        reportName: String, 
        currentYear: Int, 
        currentMonth: Int
    ) {
        hideProgressDialog() // 선택 중에는 프로그레스 숨김
        
        Log.d("LedgerReportCreate", "🔧 장부 선택 다이얼로그 표시 (리포트 생성용)")
        
        // 장부 이름 목록 생성
        val ledgerNames = ledgers.map { ledger ->
            "${ledger.name} (ID: ${ledger.id})"
        }.toTypedArray()
        
        // 장부 정보 로깅
        ledgers.forEachIndexed { index, ledger ->
            Log.d("LedgerReportCreate", "  $index. ${ledger.name} (ID: ${ledger.id})")
        }
        
        AlertDialog.Builder(this)
            .setTitle("📋 장부 선택")
            .setMessage("AI 리포트를 생성할 장부를 선택해주세요\n\n선택한 장부의 데이터를 바탕으로 리포트가 생성됩니다.")
            .setItems(ledgerNames) { _, which ->
                val selectedLedger = ledgers[which]
                Log.d("LedgerReportCreate", "✅ 장부 선택됨: ${selectedLedger.name} (ID: ${selectedLedger.id})")
                
                // 사용자에게 선택 알림
                Toast.makeText(
                    this, 
                    "📋 선택된 장부: ${selectedLedger.name}\n리포트 생성을 시작합니다...", 
                    Toast.LENGTH_SHORT
                ).show()
                
                // 다시 프로그레스 표시하고 리포트 생성
                showProgressDialog("AI 리포트를 생성하고 있습니다...\n30-60초 정도 소요됩니다.")
                createReportWithLedgerId(clubId, selectedLedger.id, reportType, reportName, currentYear, currentMonth)
            }
            .setNegativeButton("취소") { _, _ ->
                Log.d("LedgerReportCreate", "❌ 장부 선택 취소 - 리포트 생성 중단")
                Toast.makeText(this, "리포트 생성이 취소되었습니다", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false) // 선택을 강제함
            .show()
    }
    
    private fun formatLedgerAmount(amount: Int): String {
        return String.format("%,d", amount)
    }
    
    private fun formatEventComparisonReport(reportData: ApiService.AIReportResponse, clubId: Int, ledgerId: Int): String {
        Log.d("LedgerReportCreate", "📅 이벤트 비교 분석 리포트 포맷팅 시작")
        
        val currentYear = reportData.year
        val previousYear = currentYear - 1
        
        // 기본 연간 정보
        val summary = reportData.summary as? Map<String, Any> ?: mapOf()
        val income = (summary["income"] as? Number)?.toInt() ?: 0
        val expense = (summary["expense"] as? Number)?.toInt() ?: 0
        val net = (summary["net"] as? Number)?.toInt() ?: 0
        
        // 이벤트별 데이터 추출 (현재 API 응답에서 바로 가져옴)
        val byEventList = reportData.by_event
        val eventsByName = mutableMapOf<String, MutableMap<String, Any>>()
        
        // 이벤트 데이터를 수집 (API에서 이미 집계된 데이터)
        for (eventData in byEventList) {
            val eventName = eventData["event_name"] as? String ?: "이벤트 미지정"
            if (eventName == "이벤트 미지정") continue
            
            val eventIncome = (eventData["income"] as? Number)?.toInt() ?: 0
            val eventExpense = (eventData["expense"] as? Number)?.toInt() ?: 0
            val eventNet = (eventData["net"] as? Number)?.toInt() ?: 0
            
            eventsByName[eventName] = mutableMapOf(
                "income" to eventIncome,
                "expense" to eventExpense,
                "net" to eventNet
            )
        }
        
        return buildString {
            appendLine("📅 Hey-Bi AI 년도별 이벤트 비교 분석")
            appendLine("🔍 분석 기간: ${currentYear}년 (전년 대비)")
            appendLine("🤖 분석 엔진: Hey-Bi Event Analytics v2.0")
            appendLine("=".repeat(50))
            appendLine()
            
            appendLine("📊 ${currentYear}년 전체 재정 현황")
            appendLine("• 총 수입: ${String.format("%,d", income)}원 ${getAmountEmoji(income)}")
            appendLine("• 총 지출: ${String.format("%,d", expense)}원 ${getAmountEmoji(expense)}")
            appendLine("• 순수익: ${String.format("%,d", net)}원 ${getNetAmountEmoji(net)}")
            appendLine()
            
            appendLine("🎪 ${currentYear}년 이벤트별 상세 분석")
            appendLine("=".repeat(30))
            
            if (eventsByName.isEmpty()) {
                appendLine("⚠️ 분석할 이벤트 데이터가 없습니다")
                appendLine("• 이벤트를 등록하고 거래 내역을 연결하면 더 정확한 분석이 가능합니다")
                appendLine("• 정기 모임, 행사, 워크샵 등을 이벤트로 등록해보세요")
            } else {
                var eventIndex = 1
                eventsByName.toList().sortedByDescending { it.second["expense"] as Int }.forEach { (eventName, eventData) ->
                    val eventIncome = eventData["income"] as Int
                    val eventExpense = eventData["expense"] as Int
                    val eventNet = eventData["net"] as Int
                    
                    appendLine("${eventIndex}. 🎯 $eventName")
                    appendLine("   💰 수입: ${String.format("%,d", eventIncome)}원")
                    appendLine("   💸 지출: ${String.format("%,d", eventExpense)}원")
                    appendLine("   📈 순손익: ${String.format("%,d", eventNet)}원 ${getNetAmountEmoji(eventNet)}")
                    appendLine("   📊 ${getEventPerformanceAnalysis(eventName, eventIncome, eventExpense, eventNet)}")
                    appendLine()
                    
                    eventIndex++
                }
            }
            
            appendLine("🔮 AI 이벤트 예측 & 제안")
            appendLine("=".repeat(30))
            appendLine(generateEventPredictions(eventsByName, currentYear))
            appendLine()
            
            appendLine("💡 AI 이벤트 최적화 제안")
            appendLine("=".repeat(30))
            appendLine(generateEventOptimizationTips(eventsByName, income, expense))
            appendLine()
            
            appendLine("📈 다음 년도 이벤트 예산 가이드")
            appendLine("=".repeat(30))
            appendLine(generateNextYearEventBudget(eventsByName, currentYear + 1))
            appendLine()
            
            appendLine("=".repeat(50))
            appendLine("✨ Hey-Bi 이벤트 분석 완료")
            appendLine("📅 정기적인 이벤트 분석으로 더 나은 기획을 해보세요")
            appendLine("🎯 각 이벤트의 ROI를 추적하여 효율적인 운영이 가능합니다")
        }
    }
    
    private fun getEventPerformanceAnalysis(eventName: String, income: Int, expense: Int, net: Int): String {
        return when {
            net > expense / 2 -> "🌟 매우 성공적인 이벤트! 높은 수익성을 보였습니다"
            net > 0 -> "😊 수익 창출 이벤트로 긍정적인 결과를 보였습니다"
            net > -expense / 2 -> "⚖️ 적정 수준의 투자 이벤트입니다"
            else -> "⚠️ 비용 대비 효과 검토가 필요한 이벤트입니다"
        }
    }
    
    private fun generateEventPredictions(eventsByName: Map<String, Map<String, Any>>, currentYear: Int): String {
        return buildString {
            if (eventsByName.isEmpty()) {
                appendLine("🔍 이벤트 데이터가 없어 예측이 어렵습니다")
                appendLine("• 올해 이벤트를 진행하시면 내년 예측이 가능해집니다")
                return@buildString
            }
            
            appendLine("📊 ${currentYear + 1}년 예상 이벤트 트렌드:")
            
            val avgExpensePerEvent = eventsByName.values.map { it["expense"] as Int }.average()
            val totalEventsCount = eventsByName.size
            
            appendLine("• 예상 이벤트 수: ${totalEventsCount + 1}~${totalEventsCount + 3}개")
            appendLine("• 이벤트당 평균 예상 비용: ${String.format("%,d", avgExpensePerEvent.toInt())}원")
            appendLine("• 총 이벤트 예산 권장: ${String.format("%,d", (avgExpensePerEvent * (totalEventsCount + 2)).toInt())}원")
            appendLine()
            
            // 가장 비용이 많이 든 이벤트 기준 제안
            val expensiveEvents = eventsByName.toList().sortedByDescending { it.second["expense"] as Int }.take(2)
            if (expensiveEvents.isNotEmpty()) {
                appendLine("🎯 중점 관리 이벤트:")
                expensiveEvents.forEach { (name, data) ->
                    val expense = data["expense"] as Int
                    appendLine("• $name: 예산 ${String.format("%,d", (expense * 1.1).toInt())}원 (10% 인상 권장)")
                }
            }
        }
    }
    
    private fun generateEventOptimizationTips(eventsByName: Map<String, Map<String, Any>>, totalIncome: Int, totalExpense: Int): String {
        return buildString {
            if (eventsByName.isEmpty()) {
                appendLine("💡 이벤트 기획 시작 가이드:")
                appendLine("• 동아리 목적에 맞는 정기 이벤트를 계획해보세요")
                appendLine("• 회원 참여도가 높은 소규모 이벤트부터 시작하세요")
                appendLine("• 각 이벤트마다 예산을 미리 설정하고 관리하세요")
                return@buildString
            }
            
            val profitableEvents = eventsByName.filter { (it.value["net"] as Int) > 0 }
            val lossEvents = eventsByName.filter { (it.value["net"] as Int) <= 0 }
            
            if (profitableEvents.isNotEmpty()) {
                appendLine("✅ 성공 이벤트 분석:")
                profitableEvents.toList().sortedByDescending { it.second["net"] as Int }.take(2).forEach { (name, data) ->
                    appendLine("• $name: 이 이벤트의 성공 요소를 다른 이벤트에도 적용해보세요")
                }
                appendLine()
            }
            
            if (lossEvents.isNotEmpty()) {
                appendLine("🔧 개선 필요 이벤트:")
                lossEvents.toList().sortedBy { it.second["net"] as Int }.take(2).forEach { (name, data) ->
                    val expense = data["expense"] as Int
                    appendLine("• $name: 비용 절감 또는 수익 창출 방안을 검토해보세요")
                    appendLine("  - 스폰서십 확보, 참가비 조정, 비용 효율화 고려")
                }
                appendLine()
            }
            
            appendLine("📈 전체 최적화 제안:")
            val eventExpenseRatio = if (totalExpense > 0) (eventsByName.values.sumOf { it["expense"] as Int }.toDouble() / totalExpense * 100) else 0.0
            appendLine("• 현재 전체 지출 중 이벤트 비중: ${String.format("%.1f", eventExpenseRatio)}%")
            
            when {
                eventExpenseRatio < 30 -> appendLine("• 이벤트 투자를 늘려 회원 만족도를 높여보세요")
                eventExpenseRatio > 70 -> appendLine("• 이벤트 외 일반 운영비도 적절히 배분해보세요")
                else -> appendLine("• 현재 이벤트 투자 비율이 적절합니다")
            }
        }
    }
    
    private fun generateNextYearEventBudget(eventsByName: Map<String, Map<String, Any>>, nextYear: Int): String {
        return buildString {
            appendLine("🎯 ${nextYear}년 이벤트 예산 계획:")
            appendLine()
            
            if (eventsByName.isEmpty()) {
                appendLine("📋 신규 이벤트 예산 가이드:")
                appendLine("• 정기 모임: 월 50,000~100,000원")
                appendLine("• 워크샵/세미나: 회당 100,000~300,000원") 
                appendLine("• 대규모 행사: 회당 500,000~1,000,000원")
                appendLine("• 친목 활동: 회당 30,000~100,000원")
                appendLine()
                appendLine("💰 총 연간 이벤트 예산 권장: 1,000,000~2,000,000원")
                return@buildString
            }
            
            appendLine("📊 기존 이벤트 기반 예산:")
            var totalRecommendedBudget = 0
            
            eventsByName.toList().sortedByDescending { it.second["expense"] as Int }.forEach { (name, data) ->
                val expense = data["expense"] as Int
                val net = data["net"] as Int
                
                val recommendedBudget = when {
                    net > 0 -> (expense * 1.2).toInt() // 수익 이벤트는 20% 증액
                    net > -expense / 2 -> (expense * 1.1).toInt() // 적정 손실은 10% 증액
                    else -> (expense * 0.9).toInt() // 손실 이벤트는 10% 감액
                }
                
                totalRecommendedBudget += recommendedBudget
                
                appendLine("• $name")
                appendLine("  올해 사용: ${String.format("%,d", expense)}원")
                appendLine("  내년 권장: ${String.format("%,d", recommendedBudget)}원")
                appendLine("  ${if (recommendedBudget > expense) "📈 확대 추천" else if (recommendedBudget < expense) "📉 축소 검토" else "➡️ 유지"}")
                appendLine()
            }
            
            appendLine("🎪 신규 이벤트 예산 여유분: ${String.format("%,d", totalRecommendedBudget / 5)}원")
            appendLine("💰 총 권장 이벤트 예산: ${String.format("%,d", totalRecommendedBudget + totalRecommendedBudget / 5)}원")
            appendLine()
            
            appendLine("⏰ 미진행 이벤트 예측:")
            val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
            if (currentMonth < 12) {
                val remainingMonths = 12 - currentMonth
                appendLine("• 남은 기간: ${remainingMonths}개월")
                appendLine("• 추가 이벤트 가능: ${remainingMonths / 2}~${remainingMonths}개")
                
                if (eventsByName.isNotEmpty()) {
                    val avgMonthlyEventCost = eventsByName.values.sumOf { it["expense"] as Int } / Math.max(currentMonth, 1)
                    val predictedRemainCost = avgMonthlyEventCost * remainingMonths / 2
                    appendLine("• 예상 추가 비용: ${String.format("%,d", predictedRemainCost)}원")
                }
            }
        }
    }
    
    private fun showErrorMessage(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}