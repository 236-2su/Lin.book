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
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.ApiService
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import kotlin.math.roundToInt

class LedgerReportCreateActivity : BaseActivity() {
    
    private var selectedReportType = ""
    private var progressDialog: ProgressDialog? = null
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

    override fun setupContent(savedInstanceState: Bundle?) {
        setAppTitle("AI 리포트 생성")
        
        val contentContainer = findViewById<android.widget.FrameLayout>(R.id.content_container)
        val contentView = layoutInflater.inflate(R.layout.ledger_report_create, null)
        contentContainer.addView(contentView)

        showBackButton()
        setupButtonClickEvents(contentView)
        setupDefaultValues(contentView)
        
        Log.d("LedgerReportCreate", "🚀 AI 리포트 생성 액티비티 시작 - ${currentYear}년 ${currentMonth}월")
    }
    
    private fun setupButtonClickEvents(contentView: View) {
        setupReportTypeSelection(contentView)
        
        contentView.findViewById<Button>(R.id.btn_create_report)?.setOnClickListener {
            generatePerfectAIReport(contentView)
        }
        
        // 디버깅용: 긴급 테스트 모드 (길게 누르기)
        contentView.findViewById<Button>(R.id.btn_create_report)?.setOnLongClickListener {
            Log.d("LedgerReportCreate", "🧪 긴급 테스트 모드 활성화!")
            Toast.makeText(this, "🧪 긴급 테스트 모드로 리포트 생성", Toast.LENGTH_LONG).show()
            
            // 강제로 기본값 설정
            selectedReportType = "gemini_ai_analysis"
            val selectedText = contentView.findViewById<TextView>(R.id.tv_selected_report_type)
            selectedText?.text = "🤖 Gemini AI 심화 분석 리포트"
            selectedText?.setTextColor(Color.parseColor("#1976D2"))
            
            val reportName = contentView.findViewById<EditText>(R.id.et_report_name)?.text?.toString()
            val finalName = if (reportName.isNullOrBlank()) "긴급테스트_${System.currentTimeMillis()}" else reportName
            
            // 강제 샘플 리포트 생성 테스트
            Log.d("LedgerReportCreate", "🧪 강제 샘플 리포트 생성 테스트")
            generateFallbackReport(finalName, "gemini_ai_analysis", getCurrentClubId())
            true
        }
    }
    
    private fun setupDefaultValues(contentView: View) {
        // 기본 리포트명 설정
        val reportNameEdit = contentView.findViewById<EditText>(R.id.et_report_name)
        val defaultName = "${currentYear}년 ${currentMonth}월 AI 재정분석"
        reportNameEdit?.setText(defaultName)
        
        // 기본 리포트 타입 설정 (3년간 이벤트 분석)
        val selectedText = contentView.findViewById<TextView>(R.id.tv_selected_report_type)
        selectedReportType = "three_year_event"
        selectedText?.text = "📅 3년간 이벤트 분석 리포트"
        selectedText?.setTextColor(Color.parseColor("#1976D2"))
        
        Log.d("LedgerReportCreate", "✅ 기본값 설정 완료 - 리포트명: '$defaultName', 타입: '$selectedReportType'")
    }
    
    private fun setupReportTypeSelection(contentView: View) {
        val dropdown = contentView.findViewById<LinearLayout>(R.id.dropdown_report_type)
        val selectedText = contentView.findViewById<TextView>(R.id.tv_selected_report_type)
        
        Log.d("LedgerReportCreate", "🔧 드롭다운 설정 - dropdown: ${dropdown != null}, selectedText: ${selectedText != null}")
        
        // XML foreground 속성 제거하고 단순한 클릭 이벤트만 설정
        dropdown?.foreground = null
        
        // 단순한 클릭 이벤트만 설정 (중복 제거)
        dropdown?.setOnClickListener {
            Log.d("LedgerReportCreate", "🖱️ 드롭다운 클릭! 다이얼로그 열기")
            showReportTypeDialog(selectedText)
        }
        
        // 혹시 모를 백업용 - TextView도 클릭 가능하게
        selectedText?.setOnClickListener {
            Log.d("LedgerReportCreate", "🖱️ 텍스트 클릭! 다이얼로그 열기")
            showReportTypeDialog(selectedText)
        }
        
        Log.d("LedgerReportCreate", "✅ 단순화된 드롭다운 이벤트 설정 완료")
    }
    
    private fun showReportTypeDialog(selectedText: TextView?) {
        Log.d("LedgerReportCreate", "🎯 새로운 AI 리포트 선택 다이얼로그 시작")
        
        val reportTypes = arrayOf(
            "📅 3년간 이벤트 분석 리포트",
            "🔍 유사 동아리 비교 분석 리포트", 
            "🤖 Gemini AI 심화 분석 리포트"
        )
        val reportTypeKeys = arrayOf("three_year_event", "similar_clubs_comparison", "gemini_ai_analysis")
        
        try {
            // 더 간단한 다이얼로그로 변경
            AlertDialog.Builder(this)
                .setTitle("AI 리포트 종류 선택")
                .setItems(reportTypes) { dialog, which ->
                    selectedReportType = reportTypeKeys[which]
                    selectedText?.text = reportTypes[which]
                    selectedText?.setTextColor(Color.parseColor("#1976D2"))
                    
                    Log.d("LedgerReportCreate", "✅ 선택: ${reportTypes[which]} → $selectedReportType")
                    Toast.makeText(this, "선택: ${reportTypes[which]}", Toast.LENGTH_SHORT).show()
                    
                    dialog.dismiss()
                }
                .setNegativeButton("취소") { dialog, _ ->
                    Log.d("LedgerReportCreate", "❌ 리포트 선택 취소")
                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
                
            Log.d("LedgerReportCreate", "✅ 다이얼로그 표시됨")
            
        } catch (e: Exception) {
            Log.e("LedgerReportCreate", "❌ 다이얼로그 오류: ${e.message}", e)
            Toast.makeText(this, "선택 창을 열 수 없습니다: ${e.message}", Toast.LENGTH_LONG).show()
            
            // 오류 시 기본값으로 설정
            selectedReportType = "yearly"
            selectedText?.text = reportTypes[0]
            selectedText?.setTextColor(Color.parseColor("#1976D2"))
            Toast.makeText(this, "기본값으로 설정됨: 연간 리포트", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun generatePerfectAIReport(contentView: View) {
        val reportName = contentView.findViewById<EditText>(R.id.et_report_name)?.text?.toString()
        
        Log.d("LedgerReportCreate", "🎯 PERFECT AI 리포트 생성 프로세스 시작!")
        Log.d("LedgerReportCreate", "   📝 리포트명: '$reportName'")
        Log.d("LedgerReportCreate", "   🎯 선택된 리포트 타입: '$selectedReportType'")
        Log.d("LedgerReportCreate", "   📅 분석 기간: ${currentYear}년")
        
        // 디버깅: 현재 상태 확인
        Toast.makeText(this, "리포트 생성 시작: $selectedReportType", Toast.LENGTH_LONG).show()
        
        // 리포트명 기본값 설정 (빈값인 경우)
        val finalReportName = if (reportName.isNullOrBlank()) {
            val defaultName = "AI_리포트_${System.currentTimeMillis()}"
            Log.d("LedgerReportCreate", "📝 기본 리포트명 자동 설정: $defaultName")
            defaultName
        } else {
            reportName
        }
        
        if (selectedReportType.isEmpty()) {
            Log.w("LedgerReportCreate", "❌ 리포트 종류 미선택")
            Toast.makeText(this, "리포트 종류를 먼저 선택해주세요!", Toast.LENGTH_LONG).show()
            showValidationError("리포트 종류를 선택해주세요.", "드롭다운에서 분석 유형을 선택하세요.")
            return
        }
        
        // 완벽한 진행 상태 표시
        showAdvancedProgressDialog("🤖 Hey-Bi AI가 고급 분석을 수행하고 있습니다...", 
            "✨ 데이터 수집 및 패턴 분석\n📊 재정 건전성 평가\n💡 맞춤형 인사이트 생성\n⏰ 약 30-60초 소요")
        
        val clubId = getCurrentClubId()
        Log.d("LedgerReportCreate", "✅ 모든 검증 완료! 클럽 ID: $clubId")
        
        // 완벽한 AI 리포트 생성 시작
        executeAdvancedAIReportGeneration(clubId, selectedReportType, finalReportName)
    }
    
    private fun executeAdvancedAIReportGeneration(clubId: Int, reportType: String, reportName: String) {
        Log.d("LedgerReportCreate", "🚀 고급 AI 분석 엔진 가동 - 클럽: $clubId, 타입: $reportType")
        Toast.makeText(this, "AI 분석 시작: $reportType", Toast.LENGTH_SHORT).show()
        
        when (reportType) {
            "three_year_event" -> {
                Log.d("LedgerReportCreate", "✅ 3년 이벤트 분석 선택됨")
                generateThreeYearEventReport(clubId, reportName)
            }
            "similar_clubs_comparison" -> {
                Log.d("LedgerReportCreate", "✅ 유사 동아리 비교 선택됨") 
                generateNewSimilarClubsReport(clubId, reportName)
            }
            "gemini_ai_analysis" -> {
                Log.d("LedgerReportCreate", "✅ Gemini AI 분석 선택됨")
                generateGeminiAIAnalysisReport(clubId, reportName)
            }
            // 기존 리포트들도 유지 (호환성을 위해)
            "yearly" -> generateAdvancedYearlyReport(clubId, reportName)
            "yearly_3years" -> generateYearly3YearsComparisonReport(clubId, reportName)
            "similar_clubs" -> generateSimilarClubsComparisonReport(clubId, reportName)
            "ai_advice" -> generateAIAdviceReport(clubId, reportName)
            "comparison" -> generateAdvancedComparisonReport(clubId, reportName) 
            "event_comparison" -> generateAdvancedEventComparisonReport(clubId, reportName)
            else -> {
                Log.e("LedgerReportCreate", "❌ 지원하지 않는 리포트 타입: $reportType")
                hideProgressDialog()
                showAdvancedError("시스템 오류", "지원하지 않는 리포트 종류입니다.", "다른 리포트 유형을 선택해주세요.")
            }
        }
    }
    
    // 새로운 3개 AI 리포트 생성 함수들
    private fun generateThreeYearEventReport(clubId: Int, reportName: String) {
        Log.d("LedgerReportCreate", "📅 3년간 이벤트 분석 리포트 생성 시작...")
        
        showAdvancedProgressDialog("3년간 이벤트 데이터 수집 및 분석 중...", "AI가 이벤트 데이터를 분석하고 있습니다")
        
        // AIReportDataCollector를 사용해서 데이터 수집
        collectDataAndGenerateReport(clubId, reportName, "three_year_event")
    }
    
    private fun generateNewSimilarClubsReport(clubId: Int, reportName: String) {
        Log.d("LedgerReportCreate", "🔍 유사 동아리 비교 분석 리포트 생성 시작...")
        
        showAdvancedProgressDialog("유사 동아리 데이터 수집 및 비교 분석 중...", "AI가 유사 동아리들과 비교 분석하고 있습니다")
        
        // AIReportDataCollector를 사용해서 데이터 수집
        collectDataAndGenerateReport(clubId, reportName, "similar_clubs_comparison")
    }
    
    private fun generateGeminiAIAnalysisReport(clubId: Int, reportName: String) {
        Log.d("LedgerReportCreate", "🤖 Gemini AI 심화 분석 리포트 생성 시작...")
        
        showAdvancedProgressDialog("Gemini AI 심화 분석 중...", "AI가 고도화된 인사이트를 생성하고 있습니다")
        
        // AIReportDataCollector를 사용해서 데이터 수집
        collectDataAndGenerateReport(clubId, reportName, "gemini_ai_analysis")
    }
    
    private fun collectDataAndGenerateReport(clubId: Int, reportName: String, reportType: String) {
        Log.d("LedgerReportCreate", "🔄 collectDataAndGenerateReport 호출됨")
        Toast.makeText(this, "데이터 수집 시작: $reportType", Toast.LENGTH_SHORT).show()
        
        try {
            val dataCollector = com.example.myapplication.ai.AIReportDataCollector(this)
            val analysisService = com.example.myapplication.ai.AIAnalysisService(this)
            
            // 데이터 소스 설정 (모든 소스 사용)
            val selectedSources = listOf("ledger", "events")
            
            Log.d("LedgerReportCreate", "📊 데이터 수집 시작 - 클럽: $clubId, 타입: $reportType")
            
            // 코루틴으로 데이터 수집 및 분석 실행
            Thread {
                try {
                    Log.d("LedgerReportCreate", "🔍 데이터 수집 중...")
                    
                    // 데이터 수집
                    val clubData = kotlinx.coroutines.runBlocking {
                        dataCollector.collectClubData(clubId, selectedSources)
                    }
                    
                    Log.d("LedgerReportCreate", "📋 데이터 수집 결과:")
                    Log.d("LedgerReportCreate", "   클럽 정보: ${if (clubData.clubInfo != null) "✅" else "❌"}")
                    Log.d("LedgerReportCreate", "   장부 데이터: ${if (clubData.ledgerData != null) "✅ ${clubData.ledgerData.size}개" else "❌"}")
                    Log.d("LedgerReportCreate", "   거래 내역: ${if (clubData.transactions != null) "✅ ${clubData.transactions.size}건" else "❌"}")
                    Log.d("LedgerReportCreate", "   이벤트: ${if (clubData.events != null) "✅ ${clubData.events.size}개" else "❌"}")
                    Log.d("LedgerReportCreate", "   재정 요약: ${if (clubData.financialSummary != null) "✅" else "❌"}")
                    
                    // 데이터 상태에 따른 처리 결정
                    val hasAnyData = clubData.clubInfo != null || clubData.ledgerData != null || clubData.events != null
                    val hasMinimalData = clubData.clubInfo != null || (clubData.ledgerData != null && clubData.ledgerData.isNotEmpty())
                    
                    Log.d("LedgerReportCreate", "📊 데이터 상태 분석:")
                    Log.d("LedgerReportCreate", "   - 전체 데이터 있음: $hasAnyData")
                    Log.d("LedgerReportCreate", "   - 최소 데이터 있음: $hasMinimalData")
                    
                    if (!hasAnyData) {
                        Log.w("LedgerReportCreate", "⚠️ 모든 데이터가 없어 샘플 리포트 생성")
                        runOnUiThread {
                            Toast.makeText(this@LedgerReportCreateActivity, "API 데이터 없음, 샘플 리포트 생성", Toast.LENGTH_SHORT).show()
                            generateFallbackReport(reportName, reportType, clubId)
                        }
                        return@Thread
                    }
                    
                    if (!hasMinimalData) {
                        Log.w("LedgerReportCreate", "⚠️ 핵심 데이터 부족하지만 기본 리포트 시도")
                        runOnUiThread {
                            Toast.makeText(this@LedgerReportCreateActivity, "제한된 데이터로 리포트 생성", Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    // AI 분석 수행
                    Log.d("LedgerReportCreate", "🤖 AI 분석 시작...")
                    val analysisResult = kotlinx.coroutines.runBlocking {
                        analysisService.generateReport(clubData, reportType, null)
                    }
                    
                    Log.d("LedgerReportCreate", "🎯 AI 분석 결과: ${if (analysisResult.success) "성공" else "실패"}")
                    
                    runOnUiThread {
                        if (analysisResult.success) {
                            Log.d("LedgerReportCreate", "✅ AI 분석 완료!")
                            Log.d("LedgerReportCreate", "🔍 분석 결과 미리보기: ${analysisResult.content.take(200)}...")
                            Toast.makeText(this@LedgerReportCreateActivity, "AI 분석 완료, 리포트 저장 중", Toast.LENGTH_SHORT).show()
                            saveAndShowReport(reportName, analysisResult.content)
                        } else {
                            Log.e("LedgerReportCreate", "❌ AI 분석 실패: ${analysisResult.error}")
                            Toast.makeText(this@LedgerReportCreateActivity, "AI 분석 실패, 샘플 리포트 생성", Toast.LENGTH_SHORT).show()
                            // AI 분석 실패시에도 기본 리포트 생성
                            generateFallbackReport(reportName, reportType, clubId)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LedgerReportCreate", "❌ 리포트 생성 중 예외", e)
                    runOnUiThread {
                        // 예외 발생시에도 기본 리포트 생성
                        generateFallbackReport(reportName, reportType, clubId)
                    }
                }
            }.start()
            
        } catch (e: Exception) {
            Log.e("LedgerReportCreate", "❌ 데이터 수집 시작 실패", e)
            // 시작 실패시에도 기본 리포트 생성
            generateFallbackReport(reportName, reportType, clubId)
        }
    }
    
    private fun generateFallbackReport(reportName: String, reportType: String, clubId: Int) {
        Log.d("LedgerReportCreate", "🛠️ 폴백 리포트 생성: $reportType")
        
        val fallbackContent = when (reportType) {
            "three_year_event" -> generate3YearEventFallbackContent(reportName)
            "similar_clubs_comparison" -> generateClubComparisonFallbackContent(reportName)
            "gemini_ai_analysis" -> generateGeminiFallbackContent(reportName)
            else -> generateGenericFallbackContent(reportName, reportType)
        }
        
        saveAndShowReport(reportName, fallbackContent)
    }
    
    private fun generate3YearEventFallbackContent(reportName: String): String {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return """
📅 3년간 이벤트 예산 비교 분석 리포트

📊 분석 개요
• 분석 기간: ${currentYear - 2}년 ~ ${currentYear}년
• 생성일시: ${java.text.SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", java.util.Locale.KOREA).format(java.util.Date())}

⚠️ 데이터 수집 안내
현재 동아리의 이벤트 데이터를 수집하는 중 일시적인 문제가 발생했습니다.

📈 일반적인 동아리 이벤트 예산 트렌드
• 신규 동아리: 연간 50만원 ~ 100만원
• 중견 동아리: 연간 100만원 ~ 200만원
• 대형 동아리: 연간 200만원 이상

💡 3년간 분석 권장사항
• 정기적인 이벤트 예산 기록 관리
• 연도별 이벤트 성과 평가 및 개선
• 멤버 만족도를 고려한 예산 배분

🔄 데이터 수집 완료 후 재분석
이벤트 데이터가 축적되면 더 정확한 3년간 비교 분석이 가능합니다.

✨ Hey-Bi AI가 생성한 리포트입니다.
        """.trimIndent()
    }
    
    private fun generateClubComparisonFallbackContent(reportName: String): String {
        return """
🏆 유사 동아리 비교 분석 리포트

📊 분석 개요
• 생성일시: ${java.text.SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", java.util.Locale.KOREA).format(java.util.Date())}

⚠️ 데이터 수집 안내
현재 동아리 및 유사 동아리 데이터를 수집하는 중 일시적인 문제가 발생했습니다.

🔍 비교 분석 준비 중
• 유사 동아리 검색 및 매칭
• 멤버 수, 활동 규모, 재정 현황 비교 준비
• 벤치마킹 포인트 식별 작업

💡 동아리 발전을 위한 일반적 제언
• 정기적인 활동 기록 관리
• 타 동아리와의 네트워킹 활동
• 차별화된 특색 프로그램 개발
• 멤버 만족도 향상 방안 수립

🚀 향후 분석 계획
유사 동아리 데이터 수집이 완료되면 상세한 비교 분석을 제공해드립니다.

✨ Hey-Bi AI가 생성한 리포트입니다.
        """.trimIndent()
    }
    
    private fun generateGeminiFallbackContent(reportName: String): String {
        return """
🤖 Gemini AI 스타일 재정 분석 리포트

📊 분석 개요
• 생성일시: ${java.text.SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", java.util.Locale.KOREA).format(java.util.Date())}
• AI 모델: Gemini 2.5-pro 스타일 분석

⚠️ 데이터 연결 상태
재정 데이터 수집 중 일시적인 연결 문제가 발생했습니다.

💡 AI 기반 일반적 재정 관리 제언

1. 📊 데이터 기반 의사결정
   • 정기적인 재정 현황 점검 (월 1회)
   • 수입/지출 패턴 분석 및 기록
   • 예산 대비 실적 모니터링

2. 🎯 효율적 예산 운영
   • 고정비와 변동비 구분 관리
   • 예상치 못한 지출에 대비한 예비비 확보
   • 이벤트별 예산 계획 수립

3. 📈 성장 지향적 재정 전략
   • 수입원 다각화 방안 모색
   • 비용 효율성 개선 포인트 발굴
   • 투명한 재정 공개로 신뢰도 향상

4. 🔮 미래 준비
   • 중장기 재정 계획 수립
   • 리스크 관리 체계 구축
   • 지속 가능한 운영 모델 개발

🔄 실제 데이터 기반 분석 예정
재정 데이터 연결이 복구되면 맞춤형 AI 분석을 제공해드립니다.

⚡ Gemini AI 스타일로 생성된 리포트입니다.
        """.trimIndent()
    }
    
    private fun generateGenericFallbackContent(reportName: String, reportType: String): String {
        return """
📋 AI 분석 리포트

• 리포트명: $reportName
• 분석 유형: ${getReportTypeKorean(reportType)}
• 생성일시: ${java.text.SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", java.util.Locale.KOREA).format(java.util.Date())}

⚠️ 데이터 수집 중
현재 동아리 데이터를 수집하는 중 일시적인 문제가 발생했습니다.

💡 분석 준비 완료 후 재생성
데이터 수집이 완료되면 상세한 AI 분석을 제공해드립니다.

✨ Hey-Bi AI가 생성한 리포트입니다.
        """.trimIndent()
    }
    
    private fun getReportTypeKorean(type: String): String {
        return when (type) {
            "three_year_event" -> "📅 3년간 이벤트 분석"
            "similar_clubs_comparison" -> "🏆 유사 동아리 비교"
            "gemini_ai_analysis" -> "🤖 Gemini AI 분석"
            else -> "📊 일반 분석"
        }
    }
    
    private fun saveAndShowReport(reportName: String, content: String) {
        try {
            Log.d("LedgerReportCreate", "💾 리포트 저장 시작")
            Log.d("LedgerReportCreate", "   📝 리포트명: '$reportName'")
            Log.d("LedgerReportCreate", "   📊 내용 길이: ${content.length} 문자")
            Log.d("LedgerReportCreate", "   🏷️ 타입: '$selectedReportType'")
            
            // SharedPreferences에 리포트 저장
            val sharedPref = getSharedPreferences("ai_reports", Context.MODE_PRIVATE)
            val reportId = "report_${System.currentTimeMillis()}"
            val reportJson = org.json.JSONObject().apply {
                put("id", reportId)
                put("name", reportName)
                put("content", content)
                put("created_at", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()))
                put("type", selectedReportType)
            }
            
            Log.d("LedgerReportCreate", "📋 JSON 객체 생성 완료: ${reportJson.toString().length} 문자")
            
            // 기존 리포트 목록 가져오기
            val existingReports = sharedPref.getString("reports_list", "[]") ?: "[]"
            val reportsArray = org.json.JSONArray(existingReports)
            
            // 새 리포트 추가
            reportsArray.put(reportJson)
            
            // 저장
            with(sharedPref.edit()) {
                putString("reports_list", reportsArray.toString())
                putString(reportId, reportJson.toString())
                apply()
            }
            
            hideProgressDialog()
            
            Log.d("LedgerReportCreate", "✅ 리포트 저장 완료!")
            
            // 성공 메시지 표시
            android.app.AlertDialog.Builder(this)
                .setTitle("🎉 AI 리포트 생성 완료!")
                .setMessage("${reportName}\n\n새로운 AI 분석 리포트가 성공적으로 생성되었습니다.")
                .setPositiveButton("리포트 보기") { _, _ ->
                    // 리포트 상세 화면으로 이동
                    val intent = android.content.Intent(this, AIReportDetailActivity::class.java)
                    intent.putExtra("report_content", reportJson.toString())
                    intent.putExtra("report_name", reportName)
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("목록으로") { _, _ ->
                    // AI 리포트 목록으로 돌아가기
                    finish()
                }
                .setCancelable(false)
                .show()
                
        } catch (e: Exception) {
            Log.e("LedgerReportCreate", "❌ 리포트 저장 실패", e)
            hideProgressDialog()
            showAdvancedError("저장 실패", "리포트를 저장할 수 없습니다.", e.message ?: "알 수 없는 오류")
        }
    }
    
    private fun generateAdvancedYearlyReport(clubId: Int, reportName: String) {
        Log.d("LedgerReportCreate", "📊 고급 연간 분석 리포트 생성 시작...")
        
        // 단계별 진행상태 업데이트
        updateProgressMessage("📋 장부 데이터 수집 중...")
        
        ApiClient.getApiService().getLedgerList(clubId)
            .enqueue(object : retrofit2.Callback<List<LedgerApiItem>> {
                override fun onResponse(call: retrofit2.Call<List<LedgerApiItem>>, response: retrofit2.Response<List<LedgerApiItem>>) {
                    Log.d("LedgerReportCreate", "📋 장부 목록 API 응답: ${response.code()}")
                    Log.d("LedgerReportCreate", "   성공: ${response.isSuccessful}")
                    Log.d("LedgerReportCreate", "   데이터 수: ${response.body()?.size ?: 0}")
                    
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        val ledgerId = response.body()!![0].id
                        Log.d("LedgerReportCreate", "📋 장부 데이터 수집 완료 - ID: $ledgerId")
                        
                        updateProgressMessage("🤖 AI 재정 분석 엔진 실행 중...")
                        
                        // 고급 연간 리포트 API 호출 (YearlyReportResponse 사용)
                        Log.d("LedgerReportCreate", "🔄 연간 리포트 API 호출: clubId=$clubId, ledgerId=$ledgerId, year=$currentYear")
                        ApiClient.getApiService().createYearlyReport(clubId, ledgerId, currentYear)
                            .enqueue(object : retrofit2.Callback<ApiService.YearlyReportResponse> {
                                override fun onResponse(call: retrofit2.Call<ApiService.YearlyReportResponse>, response: retrofit2.Response<ApiService.YearlyReportResponse>) {
                                    Log.d("LedgerReportCreate", "📊 연간 리포트 API 응답: ${response.code()}")
                                    Log.d("LedgerReportCreate", "   성공: ${response.isSuccessful}")
                                    Log.d("LedgerReportCreate", "   응답 본문 존재: ${response.body() != null}")
                                    
                                    if (!response.isSuccessful) {
                                        Log.e("LedgerReportCreate", "   에러 본문: ${response.errorBody()?.string()}")
                                    }
                                    
                                    handleYearlyReportResponse(response, reportName, clubId)
                                }
                                
                                override fun onFailure(call: retrofit2.Call<ApiService.YearlyReportResponse>, t: Throwable) {
                                    Log.e("LedgerReportCreate", "❌ 연간 리포트 API 실패", t)
                                    handleAdvancedApiError("고급 연간 분석", t)
                                }
                            })
                    } else {
                        Log.w("LedgerReportCreate", "❌ 장부 데이터 없음 - 코드: ${response.code()}")
                        if (!response.isSuccessful) {
                            Log.e("LedgerReportCreate", "   에러 본문: ${response.errorBody()?.string()}")
                        }
                        hideProgressDialog()
                        showAdvancedError("데이터 부족", "동아리에 분석할 장부 데이터가 없습니다.", "장부를 먼저 생성하고 거래 내역을 입력해주세요.")
                    }
                }
                
                override fun onFailure(call: retrofit2.Call<List<LedgerApiItem>>, t: Throwable) {
                    handleAdvancedApiError("장부 데이터 수집", t)
                }
            })
    }
    
    private fun generateAdvancedComparisonReport(clubId: Int, reportName: String) {
        Log.d("LedgerReportCreate", "🏆 고급 비교 분석 리포트 생성 시작...")
        
        updateProgressMessage("🔍 유사 동아리 검색 및 데이터 수집 중...")
        
        ApiClient.getApiService().createSimilarClubsReport(clubId, currentYear)
            .enqueue(object : retrofit2.Callback<ApiService.SimilarClubsReportResponse> {
                override fun onResponse(call: retrofit2.Call<ApiService.SimilarClubsReportResponse>, response: retrofit2.Response<ApiService.SimilarClubsReportResponse>) {
                    Log.d("LedgerReportCreate", "🏆 비교 분석 API 응답: ${response.code()}")
                    handleAdvancedComparisonReportResponse(response, reportName, clubId)
                }
                
                override fun onFailure(call: retrofit2.Call<ApiService.SimilarClubsReportResponse>, t: Throwable) {
                    handleAdvancedApiError("고급 비교 분석", t)
                }
            })
    }
    
    private fun generateAdvancedEventComparisonReport(clubId: Int, reportName: String) {
        Log.d("LedgerReportCreate", "📅 고급 이벤트 분석 리포트 생성 시작...")
        
        updateProgressMessage("🎪 이벤트 데이터 분석 중...")
        
        // 이벤트 비교는 연간 리포트를 기반으로 생성
        ApiClient.getApiService().getLedgerList(clubId)
            .enqueue(object : retrofit2.Callback<List<LedgerApiItem>> {
                override fun onResponse(call: retrofit2.Call<List<LedgerApiItem>>, response: retrofit2.Response<List<LedgerApiItem>>) {
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        val ledgerId = response.body()!![0].id
                        Log.d("LedgerReportCreate", "📋 장부 데이터 수집 완료 - ID: $ledgerId")
                        
                        updateProgressMessage("🤖 AI 이벤트 분석 엔진 실행 중...")
                        
                        // 연간 리포트를 가져와서 이벤트 분석에 사용
                        ApiClient.getApiService().createYearlyReport(clubId, ledgerId, currentYear)
                            .enqueue(object : retrofit2.Callback<ApiService.YearlyReportResponse> {
                                override fun onResponse(call: retrofit2.Call<ApiService.YearlyReportResponse>, response: retrofit2.Response<ApiService.YearlyReportResponse>) {
                                    Log.d("LedgerReportCreate", "📅 이벤트 분석 API 응답: ${response.code()}")
                                    handleEventReportResponse(response, reportName, clubId)
                                }
                                
                                override fun onFailure(call: retrofit2.Call<ApiService.YearlyReportResponse>, t: Throwable) {
                                    handleAdvancedApiError("고급 이벤트 분석", t)
                                }
                            })
                    } else {
                        hideProgressDialog()
                        showAdvancedError("데이터 부족", "동아리에 분석할 이벤트 데이터가 없습니다.", "이벤트를 먼저 생성하고 관련 거래를 입력해주세요.")
                    }
                }
                
                override fun onFailure(call: retrofit2.Call<List<LedgerApiItem>>, t: Throwable) {
                    handleAdvancedApiError("이벤트 데이터 수집", t)
                }
            })
        
        ApiClient.getApiService().getLedgerList(clubId)
            .enqueue(object : retrofit2.Callback<List<LedgerApiItem>> {
                override fun onResponse(call: retrofit2.Call<List<LedgerApiItem>>, response: retrofit2.Response<List<LedgerApiItem>>) {
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        val ledgerId = response.body()!![0].id
                        
                        updateProgressMessage("📈 이벤트 성과 분석 및 최적화 제안 생성 중...")
                        
                        ApiClient.getApiService().createYearlyReport(clubId, ledgerId, currentYear)
                            .enqueue(object : retrofit2.Callback<ApiService.YearlyReportResponse> {
                                override fun onResponse(call: retrofit2.Call<ApiService.YearlyReportResponse>, response: retrofit2.Response<ApiService.YearlyReportResponse>) {
                                    Log.d("LedgerReportCreate", "📅 이벤트 분석 API 응답: ${response.code()}")
                                    handleEventReportResponse(response, reportName, clubId)
                                }
                                
                                override fun onFailure(call: retrofit2.Call<ApiService.YearlyReportResponse>, t: Throwable) {
                                    handleAdvancedApiError("고급 이벤트 분석", t)
                                }
                            })
                    } else {
                        hideProgressDialog()
                        showAdvancedError("이벤트 데이터 부족", "분석할 이벤트 데이터가 부족합니다.", "이벤트를 생성하고 관련 거래를 추가해주세요.")
                    }
                }
                
                override fun onFailure(call: retrofit2.Call<List<LedgerApiItem>>, t: Throwable) {
                    handleAdvancedApiError("이벤트 데이터 수집", t)
                }
            })
    }
    
    private fun handleYearlyReportResponse(response: retrofit2.Response<ApiService.YearlyReportResponse>, reportName: String, clubId: Int) {
        hideProgressDialog()
        
        if (response.isSuccessful && response.body() != null) {
            val reportData = response.body()!!
            Log.d("LedgerReportCreate", "✅ 연간 분석 완료!")
            Log.d("LedgerReportCreate", "   📊 요약 데이터: ${reportData.summary}")
            Log.d("LedgerReportCreate", "   🏷️ 항목별 분석: ${reportData.by_type.size}개")
            Log.d("LedgerReportCreate", "   📅 월별 데이터: ${reportData.by_month.size}개월")
            
            val perfectReportContent = createYearlyReportFromBackend(reportData)
            saveReportWithAdvancedMetrics(reportName, perfectReportContent, "yearly", clubId)
            
            Toast.makeText(this, "✅ 연간 AI 리포트 생성 완료!", Toast.LENGTH_LONG).show()
        } else {
            Log.e("LedgerReportCreate", "❌ 연간 분석 실패: ${response.code()}")
            val errorBody = response.errorBody()?.string()
            Log.e("LedgerReportCreate", "   상세 오류: $errorBody")
            showAdvancedError("분석 실패", "연간 재정 분석에 실패했습니다.", "다시 시도하거나 관리자에게 문의하세요. (오류코드: ${response.code()})")
        }
    }
    
    private fun handleAdvancedComparisonReportResponse(response: retrofit2.Response<ApiService.SimilarClubsReportResponse>, reportName: String, clubId: Int) {
        hideProgressDialog()
        
        if (response.isSuccessful && response.body() != null) {
            val comparisonData = response.body()!!
            Log.d("LedgerReportCreate", "✅ 고급 비교 분석 완료!")
            Log.d("LedgerReportCreate", "   🏆 비교 동아리 수: ${comparisonData.similar_club_reports.size}개")
            
            val perfectComparisonContent = createPerfectComparisonReport(comparisonData)
            saveReportWithAdvancedMetrics(reportName, perfectComparisonContent, "comparison", clubId)
        } else {
            Log.e("LedgerReportCreate", "❌ 비교 분석 실패: ${response.code()}")
            
            // 백업: 비교 데이터가 없는 경우 기본 분석 제공
            if (response.code() == 404) {
                Log.d("LedgerReportCreate", "🔄 비교 데이터 부족으로 기본 분석 제공")
                val fallbackContent = createFallbackComparisonReport(reportName, clubId)
                saveReportWithAdvancedMetrics(reportName, fallbackContent, "comparison", clubId)
            } else {
                showAdvancedError("비교 분석 실패", "동아리 비교 분석에 실패했습니다.", "네트워크 상태를 확인하고 다시 시도해주세요. (오류코드: ${response.code()})")
            }
        }
    }
    
    private fun handleEventReportResponse(response: retrofit2.Response<ApiService.YearlyReportResponse>, reportName: String, clubId: Int) {
        hideProgressDialog()
        
        if (response.isSuccessful && response.body() != null) {
            val reportData = response.body()!!
            Log.d("LedgerReportCreate", "✅ 이벤트 분석 완료!")
            
            val eventReportContent = createEventReportFromBackend(reportData)
            saveReportWithAdvancedMetrics(reportName, eventReportContent, "event_comparison", clubId)
            
            Toast.makeText(this, "✅ 이벤트 비교 AI 리포트 생성 완료!", Toast.LENGTH_LONG).show()
        } else {
            Log.e("LedgerReportCreate", "❌ 이벤트 분석 실패: ${response.code()}")
            val errorBody = response.errorBody()?.string()
            Log.e("LedgerReportCreate", "   상세 오류: $errorBody")
            showAdvancedError("이벤트 분석 실패", "이벤트 비교 분석에 실패했습니다.", "이벤트 데이터를 확인하고 다시 시도해주세요. (오류코드: ${response.code()})")
        }
    }
    
    // 3년간 연도별 비교 리포트 생성
    private fun generateYearly3YearsComparisonReport(clubId: Int, reportName: String) {
        Log.d("LedgerReportCreate", "📈 3년간 연도별 비교 분석 시작...")
        updateProgressMessage("📊 3년간 데이터 수집 및 분석 중...")
        
        ApiClient.getApiService().getLedgerList(clubId)
            .enqueue(object : retrofit2.Callback<List<LedgerApiItem>> {
                override fun onResponse(call: retrofit2.Call<List<LedgerApiItem>>, response: retrofit2.Response<List<LedgerApiItem>>) {
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        val ledgerId = response.body()!![0].id
                        generate3YearsData(clubId, ledgerId, reportName)
                    } else {
                        hideProgressDialog()
                        showAdvancedError("데이터 부족", "3년간 비교할 장부 데이터가 없습니다.", "장부를 먼저 생성해주세요.")
                    }
                }
                
                override fun onFailure(call: retrofit2.Call<List<LedgerApiItem>>, t: Throwable) {
                    handleAdvancedApiError("3년간 데이터 수집", t)
                }
            })
    }
    
    private fun generate3YearsData(clubId: Int, ledgerId: Int, reportName: String) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = listOf(currentYear - 2, currentYear - 1, currentYear)
        val yearlyReports = mutableMapOf<Int, ApiService.YearlyReportResponse?>()
        var completedRequests = 0
        
        updateProgressMessage("📊 ${years.size}년간 데이터 분석 중...")
        
        years.forEach { year ->
            ApiClient.getApiService().createYearlyReport(clubId, ledgerId, year)
                .enqueue(object : retrofit2.Callback<ApiService.YearlyReportResponse> {
                    override fun onResponse(call: retrofit2.Call<ApiService.YearlyReportResponse>, response: retrofit2.Response<ApiService.YearlyReportResponse>) {
                        synchronized(yearlyReports) {
                            if (response.isSuccessful) {
                                yearlyReports[year] = response.body()
                            } else {
                                yearlyReports[year] = null
                            }
                            completedRequests++
                            
                            if (completedRequests == years.size) {
                                hideProgressDialog()
                                val reportContent = create3YearsComparisonReport(years, yearlyReports, clubId)
                                saveReportWithAdvancedMetrics(reportName, reportContent, "yearly_3years", clubId)
                                Toast.makeText(this@LedgerReportCreateActivity, "3년간 비교 리포트 완성!", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    
                    override fun onFailure(call: retrofit2.Call<ApiService.YearlyReportResponse>, t: Throwable) {
                        synchronized(yearlyReports) {
                            yearlyReports[year] = null
                            completedRequests++
                            
                            if (completedRequests == years.size) {
                                hideProgressDialog()
                                val reportContent = create3YearsComparisonReport(years, yearlyReports, clubId)
                                saveReportWithAdvancedMetrics(reportName, reportContent, "yearly_3years", clubId)
                                Toast.makeText(this@LedgerReportCreateActivity, "일부 데이터 누락된 3년 비교 리포트 완성", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                })
        }
    }
    
    // 유사 동아리 비교 리포트 생성
    private fun generateSimilarClubsComparisonReport(clubId: Int, reportName: String) {
        Log.d("LedgerReportCreate", "🏆 유사 동아리 비교 분석 시작...")
        updateProgressMessage("🔍 유사 동아리 검색 중...")
        
        // 유사 동아리 비교 리포트 API 호출 (이미 존재하는 API 사용)
        ApiClient.getApiService().createSimilarClubsReport(clubId, currentYear)
            .enqueue(object : retrofit2.Callback<ApiService.SimilarClubsReportResponse> {
                override fun onResponse(call: retrofit2.Call<ApiService.SimilarClubsReportResponse>, response: retrofit2.Response<ApiService.SimilarClubsReportResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        // 추가 정보 수집을 위해 클럽 정보와 멤버 수 가져오기
                        fetchClubDetailsAndCreateReport(response.body()!!, reportName, clubId)
                    } else {
                        hideProgressDialog()
                        showAdvancedError("비교 분석 실패", "유사 동아리 데이터를 찾을 수 없습니다.", "다른 리포트 유형을 시도해보세요.")
                    }
                }
                
                override fun onFailure(call: retrofit2.Call<ApiService.SimilarClubsReportResponse>, t: Throwable) {
                    handleAdvancedApiError("유사 동아리 비교", t)
                }
            })
    }
    
    data class ClubDetailWithMembers(
        val clubDetail: ClubItem,
        val memberCount: Int
    )
    
    private fun fetchClubDetailsAndCreateReport(similarClubsData: ApiService.SimilarClubsReportResponse, reportName: String, clubId: Int) {
        updateProgressMessage("📊 동아리 상세 정보와 멤버 수를 확인하고 있습니다...")
        
        val clubDetailsMap = mutableMapOf<Int, ClubDetailWithMembers>()
        var completedRequests = 0
        val totalClubs = 1 + similarClubsData.similar_club_reports.size // 우리 동아리 + 유사 동아리들
        
        // 우리 동아리 정보 가져오기
        fetchClubDetailWithMembers(clubId) { ourClubDetail ->
            if (ourClubDetail != null) {
                clubDetailsMap[clubId] = ourClubDetail
            }
            completedRequests++
            
            if (completedRequests == totalClubs) {
                val reportContent = createEnhancedSimilarClubsReportWithDetails(similarClubsData, clubDetailsMap)
                saveReportWithAdvancedMetrics(reportName, reportContent, "similar_clubs", clubId)
                hideProgressDialog()
                Toast.makeText(this@LedgerReportCreateActivity, "유사 동아리 비교 리포트 완성!", Toast.LENGTH_LONG).show()
            }
        }
        
        // 유사 동아리들 정보 가져오기
        similarClubsData.similar_club_reports.forEach { similarReport ->
            val similarClubId = similarReport.club_id
            fetchClubDetailWithMembers(similarClubId) { clubDetail ->
                if (clubDetail != null) {
                    clubDetailsMap[similarClubId] = clubDetail
                }
                completedRequests++
                
                if (completedRequests == totalClubs) {
                    val reportContent = createEnhancedSimilarClubsReportWithDetails(similarClubsData, clubDetailsMap)
                    saveReportWithAdvancedMetrics(reportName, reportContent, "similar_clubs", clubId)
                    hideProgressDialog()
                    Toast.makeText(this@LedgerReportCreateActivity, "유사 동아리 비교 리포트 완성!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun fetchClubDetailWithMembers(clubId: Int, callback: (ClubDetailWithMembers?) -> Unit) {
        // 동아리 상세 정보 가져오기
        ApiClient.getApiService().getClubDetail(clubId).enqueue(object : retrofit2.Callback<ClubItem> {
            override fun onResponse(call: retrofit2.Call<ClubItem>, response: retrofit2.Response<ClubItem>) {
                if (response.isSuccessful && response.body() != null) {
                    val clubDetail = response.body()!!
                    
                    // 동아리 멤버 수 가져오기
                    ApiClient.getApiService().getClubMembers(clubId).enqueue(object : retrofit2.Callback<List<MemberResponse>> {
                        override fun onResponse(memberCall: retrofit2.Call<List<MemberResponse>>, memberResponse: retrofit2.Response<List<MemberResponse>>) {
                            val memberCount = if (memberResponse.isSuccessful && memberResponse.body() != null) {
                                memberResponse.body()!!.size
                            } else {
                                0
                            }
                            
                            callback(ClubDetailWithMembers(clubDetail, memberCount))
                        }
                        
                        override fun onFailure(memberCall: retrofit2.Call<List<MemberResponse>>, t: Throwable) {
                            // 멤버 정보 실패해도 동아리 정보만으로라도 처리
                            callback(ClubDetailWithMembers(clubDetail, 0))
                        }
                    })
                } else {
                    callback(null)
                }
            }
            
            override fun onFailure(call: retrofit2.Call<ClubItem>, t: Throwable) {
                callback(null)
            }
        })
    }
    
    // AI 재무 조언 리포트 생성
    private fun generateAIAdviceReport(clubId: Int, reportName: String) {
        Log.d("LedgerReportCreate", "🤖 AI 재무 조언 리포트 생성 시작...")
        updateProgressMessage("🤖 Gemini AI가 재정 데이터를 분석하고 있습니다...")
        
        ApiClient.getApiService().getLedgerList(clubId)
            .enqueue(object : retrofit2.Callback<List<LedgerApiItem>> {
                override fun onResponse(call: retrofit2.Call<List<LedgerApiItem>>, response: retrofit2.Response<List<LedgerApiItem>>) {
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        val ledgerId = response.body()!![0].id
                        
                        updateProgressMessage("🧠 Gemini AI가 맞춤형 조언을 생성하고 있습니다...")
                        
                        ApiClient.getApiService().getLedgerAdvice(clubId, ledgerId, currentYear)
                            .enqueue(object : retrofit2.Callback<ApiService.GeminiAdviceResponse> {
                                override fun onResponse(call: retrofit2.Call<ApiService.GeminiAdviceResponse>, response: retrofit2.Response<ApiService.GeminiAdviceResponse>) {
                                    hideProgressDialog()
                                    if (response.isSuccessful && response.body() != null) {
                                        val adviceData = response.body()!!
                                        val aiReportContent = createAIAdviceReport(adviceData, clubId)
                                        saveReportWithAdvancedMetrics(reportName, aiReportContent, "ai_advice", clubId)
                                        Toast.makeText(this@LedgerReportCreateActivity, "AI 조언 리포트 완성!", Toast.LENGTH_LONG).show()
                                    } else {
                                        val errorBody = response.errorBody()?.string()
                                        Log.e("LedgerReportCreate", "❌ AI 조언 생성 실패: ${response.code()}, $errorBody")
                                        showAdvancedError("AI 분석 실패", "Gemini AI 조언 생성에 실패했습니다.", "잠시 후 다시 시도하거나 다른 리포트 유형을 선택해주세요.")
                                    }
                                }
                                
                                override fun onFailure(call: retrofit2.Call<ApiService.GeminiAdviceResponse>, t: Throwable) {
                                    Log.e("LedgerReportCreate", "❌ AI 조언 API 네트워크 오류", t)
                                    hideProgressDialog()
                                    showAdvancedError("네트워크 오류", "AI 조언 서비스에 연결할 수 없습니다.", "네트워크 연결을 확인하고 다시 시도해주세요.")
                                }
                            })
                    } else {
                        hideProgressDialog()
                        showAdvancedError("데이터 부족", "AI 분석할 장부 데이터가 없습니다.", "장부를 먼저 생성해주세요.")
                    }
                }
                
                override fun onFailure(call: retrofit2.Call<List<LedgerApiItem>>, t: Throwable) {
                    handleAdvancedApiError("장부 데이터 수집", t)
                }
            })
    }
    
    private fun createAIAdviceReport(adviceData: ApiService.GeminiAdviceResponse, clubId: Int): String {
        val reportBuilder = StringBuilder()
        
        reportBuilder.append("🤖 Gemini AI 재무 조언 분석 리포트\n")
        reportBuilder.append("=====================================\n")
        reportBuilder.append("🧠 AI 엔진: Google Gemini 2.5 Pro\n")
        reportBuilder.append("🆔 동아리ID: $clubId\n")
        reportBuilder.append("📅 분석 년도: ${currentYear}년\n")
        reportBuilder.append("⚡ 생성 시간: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n\n")
        
        // 전체 평가
        reportBuilder.append("📊 AI 종합 평가\n")
        reportBuilder.append("─────────────────────────────────────\n")
        reportBuilder.append("${adviceData.overall}\n\n")
        
        // 월별 동향 분석
        reportBuilder.append("📅 월별 동향 AI 분석\n")
        reportBuilder.append("─────────────────────────────────────\n")
        reportBuilder.append("${adviceData.by_month}\n\n")
        
        // 수입원 분석
        reportBuilder.append("💰 수입원 AI 분석\n")
        reportBuilder.append("─────────────────────────────────────\n")
        reportBuilder.append("${adviceData.by_income}\n\n")
        
        // AI 맞춤형 조언
        reportBuilder.append("💡 Gemini AI 맞춤형 조언\n")
        reportBuilder.append("─────────────────────────────────────\n")
        adviceData.advices.forEachIndexed { index, advice ->
            reportBuilder.append("${index + 1}. $advice\n\n")
        }
        
        // 추가 AI 인사이트
        reportBuilder.append("🎯 AI 추가 인사이트\n")
        reportBuilder.append("─────────────────────────────────────\n")
        reportBuilder.append("🔍 분석 신뢰도: ${calculateAdviceReliability(adviceData)}%\n")
        reportBuilder.append("🚀 실행 우선순위: ${getPriorityAdvice(adviceData.advices)}\n")
        reportBuilder.append("📈 예상 개선 효과: ${getExpectedImprovement(adviceData)}\n\n")
        
        reportBuilder.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        reportBuilder.append("🤖 이 조언은 Google Gemini AI가 실제 재정 데이터를 분석하여 생성했습니다\n")
        reportBuilder.append("💡 정기적인 AI 분석으로 더 정확한 인사이트를 받아보세요\n")
        reportBuilder.append("📞 문의: Hey-Bi AI 지원팀\n")
        reportBuilder.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        
        return reportBuilder.toString()
    }
    
    private fun calculateAdviceReliability(adviceData: ApiService.GeminiAdviceResponse): Int {
        // AI 조언의 신뢰도를 계산 (조언의 구체성과 길이를 기반으로 추정)
        val totalLength = adviceData.overall.length + adviceData.by_month.length + adviceData.by_income.length + adviceData.advices.sumOf { it.length }
        val adviceCount = adviceData.advices.size
        
        return when {
            totalLength > 1000 && adviceCount >= 3 -> 95
            totalLength > 500 && adviceCount >= 2 -> 85
            totalLength > 200 -> 75
            else -> 65
        }
    }
    
    private fun getPriorityAdvice(advices: List<String>): String {
        return if (advices.isNotEmpty()) {
            "\"${advices.first()}\""
        } else {
            "구체적인 조언이 제공되지 않았습니다."
        }
    }
    
    private fun getExpectedImprovement(adviceData: ApiService.GeminiAdviceResponse): String {
        val adviceKeywords = listOf("증대", "개선", "절감", "효율", "수익", "성장")
        val hasPositiveKeywords = adviceData.advices.any { advice ->
            adviceKeywords.any { keyword -> advice.contains(keyword, true) }
        }
        
        return when {
            hasPositiveKeywords && adviceData.advices.size >= 3 -> "높음 (15-25% 개선 예상)"
            hasPositiveKeywords -> "보통 (10-15% 개선 예상)"
            adviceData.advices.size >= 2 -> "낮음 (5-10% 개선 예상)"
            else -> "미미함 (개선 방향 모색 필요)"
        }
    }
    
    private fun createYearlyReportFromBackend(reportData: ApiService.YearlyReportResponse): String {
        val summary = reportData.summary
        val income = summary["income"] ?: 0
        val expense = summary["expense"] ?: 0
        val net = summary["net"] ?: (income - expense)
        
        Log.d("LedgerReportCreate", "📊 연간 리포트 생성 시작")
        Log.d("LedgerReportCreate", "   수입: ${formatPerfectAmount(income)}")
        Log.d("LedgerReportCreate", "   지출: ${formatPerfectAmount(expense)}")
        Log.d("LedgerReportCreate", "   순이익: ${formatPerfectAmount(net)}")
        
        val reportBuilder = StringBuilder()
        
        // 헤더
        reportBuilder.append("🤖 AI 연간 재정분석 리포트\n")
        reportBuilder.append("=====================================\n")
        reportBuilder.append("📅 분석기간: ${reportData.year}년 전체\n")
        reportBuilder.append("🏢 장부ID: ${reportData.ledger_id}\n")
        reportBuilder.append("🆔 동아리ID: ${reportData.club_id}\n\n")
        
        // 종합 요약
        reportBuilder.append("💰 연간 재정 요약\n")
        reportBuilder.append("─────────────────────────────────────\n")
        reportBuilder.append("📈 총 수입: ${formatPerfectAmount(income)}\n")
        reportBuilder.append("📉 총 지출: ${formatPerfectAmount(expense)}\n")
        reportBuilder.append("💎 순 이익: ${formatPerfectAmount(net)} ${getAdvancedNetEmoji(net)}\n")
        
        val profitRate = if (income > 0) ((net.toDouble() / income.toDouble()) * 100).roundToInt() else 0
        reportBuilder.append("📊 수익률: ${profitRate}%\n\n")
        
        // 항목별 분석 (백엔드 딕셔너리 형태 처리)
        if (reportData.by_type.isNotEmpty()) {
            reportBuilder.append("🏷️ 항목별 상세분석\n")
            reportBuilder.append("─────────────────────────────────────\n")
            
            val sortedTypes = reportData.by_type.entries.sortedByDescending { entry ->
                val typeData = entry.value
                (typeData["income"] ?: 0) - (typeData["expense"] ?: 0)
            }
            
            sortedTypes.forEach { (typeName, typeData) ->
                val typeIncome = typeData["income"] ?: 0
                val typeExpense = typeData["expense"] ?: 0
                val typeNet = typeIncome - typeExpense
                
                reportBuilder.append("📋 $typeName\n")
                reportBuilder.append("   수입: ${formatPerfectAmount(typeIncome)}\n")
                reportBuilder.append("   지출: ${formatPerfectAmount(typeExpense)}\n")
                reportBuilder.append("   순액: ${formatPerfectAmount(typeNet)} ${getAdvancedNetEmoji(typeNet)}\n")
                reportBuilder.append("   ${getAdvancedTypeAnalysis(typeName, typeIncome, typeExpense, typeNet)}\n\n")
            }
        }
        
        // 월별 추이 분석
        if (reportData.by_month.isNotEmpty()) {
            reportBuilder.append("📅 월별 추이 분석\n")
            reportBuilder.append("─────────────────────────────────────\n")
            
            val monthlyData = mutableListOf<Triple<Int, Int, Int>>() // month, income, expense
            
            for (monthKey in reportData.by_month.keys.sorted()) {
                try {
                    val monthInt = monthKey.toInt()
                    val monthData = reportData.by_month[monthKey]
                    monthData?.let { data ->
                        val monthIncome = data.summary["income"] ?: 0
                        val monthExpense = data.summary["expense"] ?: 0
                        monthlyData.add(Triple(monthInt, monthIncome, monthExpense))
                    }
                } catch (e: NumberFormatException) {
                    Log.w("LedgerReportCreate", "월별 데이터 키 파싱 실패: $monthKey")
                }
            }
            
            monthlyData.forEach { (month, monthIncome, monthExpense) ->
                val monthNet = monthIncome - monthExpense
                reportBuilder.append("📆 ${month}월: 수입 ${formatPerfectAmount(monthIncome)} | ")
                reportBuilder.append("지출 ${formatPerfectAmount(monthExpense)} | ")
                reportBuilder.append("순액 ${formatPerfectAmount(monthNet)} ${getAdvancedNetEmoji(monthNet)}\n")
            }
            reportBuilder.append("\n")
        }
        
        // AI 분석 결론
        reportBuilder.append("🤖 AI 종합 분석\n")
        reportBuilder.append("─────────────────────────────────────\n")
        reportBuilder.append("${getYearlyAnalysisInsight(income, expense, net)}\n\n")
        
        reportBuilder.append("📊 리포트 생성 완료: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
        
        return reportBuilder.toString()
    }
    
    // 3년간 비교 리포트 생성
    private fun create3YearsComparisonReport(years: List<Int>, yearlyReports: Map<Int, ApiService.YearlyReportResponse?>, clubId: Int): String {
        val reportBuilder = StringBuilder()
        
        reportBuilder.append("📈 AI 3년간 연도별 비교 분석 리포트\n")
        reportBuilder.append("=====================================\n")
        reportBuilder.append("📅 분석기간: ${years.first()}년 ~ ${years.last()}년 (3년간)\n")
        reportBuilder.append("🆔 동아리ID: $clubId\n")
        reportBuilder.append("🤖 분석엔진: Hey-Bi Advanced Comparative Analytics\n\n")
        
        // 연도별 요약
        reportBuilder.append("💰 연도별 재정 요약\n")
        reportBuilder.append("─────────────────────────────────────\n")
        
        val yearData = mutableMapOf<Int, Triple<Int, Int, Int>>() // year to (income, expense, net)
        
        years.forEach { year ->
            val report = yearlyReports[year]
            if (report != null) {
                val income = report.summary["income"] ?: 0
                val expense = report.summary["expense"] ?: 0
                val net = income - expense
                yearData[year] = Triple(income, expense, net)
                
                reportBuilder.append("📊 ${year}년:\n")
                reportBuilder.append("   수입: ${formatPerfectAmount(income)}\n")
                reportBuilder.append("   지출: ${formatPerfectAmount(expense)}\n")
                reportBuilder.append("   순이익: ${formatPerfectAmount(net)} ${getAdvancedNetEmoji(net)}\n\n")
            } else {
                reportBuilder.append("📊 ${year}년: ❌ 데이터 없음\n\n")
            }
        }
        
        // 성장 추이 분석
        if (yearData.size >= 2) {
            reportBuilder.append("📈 성장 추이 분석\n")
            reportBuilder.append("─────────────────────────────────────\n")
            
            val sortedYears = yearData.keys.sorted()
            for (i in 1 until sortedYears.size) {
                val prevYear = sortedYears[i-1]
                val currentYear = sortedYears[i]
                val prevData = yearData[prevYear]!!
                val currentData = yearData[currentYear]!!
                
                val incomeGrowth = if (prevData.first > 0) ((currentData.first - prevData.first).toDouble() / prevData.first * 100).roundToInt() else 0
                val expenseGrowth = if (prevData.second > 0) ((currentData.second - prevData.second).toDouble() / prevData.second * 100).roundToInt() else 0
                val netGrowth = currentData.third - prevData.third
                
                reportBuilder.append("📅 ${prevYear}년 → ${currentYear}년 변화:\n")
                reportBuilder.append("   수입 증감: ${if (incomeGrowth > 0) "+" else ""}${incomeGrowth}%\n")
                reportBuilder.append("   지출 증감: ${if (expenseGrowth > 0) "+" else ""}${expenseGrowth}%\n")
                reportBuilder.append("   순이익 변화: ${formatPerfectAmount(netGrowth)} ${if (netGrowth > 0) "📈" else "📉"}\n\n")
            }
        }
        
        // 이벤트 예산 분석
        reportBuilder.append("🎪 이벤트 예산 분석 및 예측\n")
        reportBuilder.append("─────────────────────────────────────\n")
        
        val totalEventBudgets = mutableMapOf<String, MutableList<Int>>()
        val completedEvents = mutableListOf<String>()
        
        yearlyReports.values.filterNotNull().forEach { report ->
            report.by_month.values.forEach { monthData ->
                monthData.by_event?.forEach { event ->
                    val eventName = event["event_name"] as? String ?: "Unknown"
                    val eventIncome = event["income"] as? Int ?: 0
                    val eventExpense = event["expense"] as? Int ?: 0
                    val eventBudget = eventIncome + eventExpense
                    
                    totalEventBudgets.getOrPut(eventName) { mutableListOf() }.add(eventBudget)
                }
            }
        }
        
        if (totalEventBudgets.isNotEmpty()) {
            reportBuilder.append("📊 진행된 이벤트 (과거 실적 기반):\n")
            totalEventBudgets.forEach { (eventName, budgets) ->
                val avgBudget = budgets.average().roundToInt()
                val maxBudget = budgets.maxOrNull() ?: 0
                val minBudget = budgets.minOrNull() ?: 0
                
                reportBuilder.append("   🎯 $eventName\n")
                reportBuilder.append("      평균 예산: ${formatPerfectAmount(avgBudget)}\n")
                reportBuilder.append("      최대/최소: ${formatPerfectAmount(maxBudget)} / ${formatPerfectAmount(minBudget)}\n")
                reportBuilder.append("      실행 횟수: ${budgets.size}회\n\n")
            }
            
            // 예정된 이벤트 예측
            reportBuilder.append("🔮 향후 예정 이벤트 예산 예측:\n")
            val avgEventBudget = totalEventBudgets.values.flatten().average().roundToInt()
            reportBuilder.append("   📈 평균 이벤트 예산: ${formatPerfectAmount(avgEventBudget)}\n")
            reportBuilder.append("   💡 권장 예산 범위: ${formatPerfectAmount((avgEventBudget * 0.8).roundToInt())} ~ ${formatPerfectAmount((avgEventBudget * 1.2).roundToInt())}\n\n")
        }
        
        // AI 종합 분석
        reportBuilder.append("🤖 AI 3년간 종합 분석\n")
        reportBuilder.append("─────────────────────────────────────\n")
        reportBuilder.append("${get3YearsAnalysisInsight(yearData)}\n\n")
        
        reportBuilder.append("📊 리포트 생성 완료: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
        
        return reportBuilder.toString()
    }
    
    // 향상된 유사 동아리 비교 리포트
    private fun createEnhancedSimilarClubsReport(reportData: ApiService.SimilarClubsReportResponse): String {
        val reportBuilder = StringBuilder()
        
        reportBuilder.append("🏆 AI 유사 동아리 비교 분석 리포트\n")
        reportBuilder.append("=====================================\n")
        reportBuilder.append("🔍 분석 대상: ${reportData.similar_club_reports.size}개 유사 동아리\n")
        reportBuilder.append("🤖 분석엔진: Hey-Bi Similarity Matching v4.0\n\n")
        
        val ourReport = reportData.original_club_report
        val ourIncome = ourReport.summary["income"] ?: 0
        val ourExpense = ourReport.summary["expense"] ?: 0
        val ourNet = ourIncome - ourExpense
        
        reportBuilder.append("🏢 우리 동아리 현황\n")
        reportBuilder.append("─────────────────────────────────────\n")
        reportBuilder.append("📈 수입: ${formatPerfectAmount(ourIncome)}\n")
        reportBuilder.append("📉 지출: ${formatPerfectAmount(ourExpense)}\n")
        reportBuilder.append("💎 순이익: ${formatPerfectAmount(ourNet)} ${getAdvancedNetEmoji(ourNet)}\n\n")
        
        reportBuilder.append("🔍 유사 동아리 비교 분석\n")
        reportBuilder.append("─────────────────────────────────────\n")
        
        reportData.similar_club_reports.forEachIndexed { index, similarReport ->
            val similarIncome = similarReport.summary["income"] ?: 0
            val similarExpense = similarReport.summary["expense"] ?: 0
            val similarNet = similarIncome - similarExpense
            
            reportBuilder.append("🏅 유사 동아리 #${index + 1}\n")
            reportBuilder.append("   수입: ${formatPerfectAmount(similarIncome)} ${getComparisonIndicator(ourIncome, similarIncome)}\n")
            reportBuilder.append("   지출: ${formatPerfectAmount(similarExpense)} ${getComparisonIndicator(ourExpense, similarExpense)}\n")
            reportBuilder.append("   순이익: ${formatPerfectAmount(similarNet)} ${getComparisonIndicator(ourNet, similarNet)}\n")
            
            // 이벤트 및 타입 비교
            if (ourReport.by_type.isNotEmpty() && similarReport.by_type.isNotEmpty()) {
                reportBuilder.append("   📊 활동 유형 비교: ${compareActivityTypes(ourReport.by_type, similarReport.by_type)}\n")
            }
            reportBuilder.append("\n")
        }
        
        // 경쟁력 분석
        reportBuilder.append("🎯 경쟁력 분석\n")
        reportBuilder.append("─────────────────────────────────────\n")
        reportBuilder.append("${getCompetitivenessAnalysis(ourReport, reportData.similar_club_reports)}\n\n")
        
        reportBuilder.append("📊 리포트 생성 완료: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
        
        return reportBuilder.toString()
    }
    
    private fun createEnhancedSimilarClubsReportWithDetails(reportData: ApiService.SimilarClubsReportResponse, clubDetailsMap: Map<Int, ClubDetailWithMembers>): String {
        val reportBuilder = StringBuilder()
        
        reportBuilder.append("🔍 유사 동아리 비교 분석 리포트 (상세정보 포함)\n")
        reportBuilder.append("=====================================\n")
        reportBuilder.append("🔍 분석 대상: ${reportData.similar_club_reports.size}개 유사 동아리\n")
        reportBuilder.append("📅 분석 기간: ${currentYear}년\n")
        reportBuilder.append("🤖 분석엔진: Hey-Bi Enhanced Similarity Matching v5.0\n\n")
        
        val ourReport = reportData.original_club_report
        val ourIncome = ourReport.summary["income"] ?: 0
        val ourExpense = ourReport.summary["expense"] ?: 0
        val ourNet = ourIncome - ourExpense
        val ourClubDetail = clubDetailsMap[ourReport.club_id]
        
        reportBuilder.append("🏢 우리 동아리 현황\n")
        reportBuilder.append("─────────────────────────────────────\n")
        if (ourClubDetail != null) {
            reportBuilder.append("📛 동아리명: ${ourClubDetail.clubDetail.name}\n")
            reportBuilder.append("🏫 소속: ${ourClubDetail.clubDetail.department}\n")
            reportBuilder.append("📂 분야: ${ourClubDetail.clubDetail.majorCategory}\n")
            reportBuilder.append("👥 멤버 수: ${ourClubDetail.memberCount}명\n")
            reportBuilder.append("📝 설명: ${ourClubDetail.clubDetail.description}\n")
        }
        reportBuilder.append("📈 수입: ${formatPerfectAmount(ourIncome)}\n")
        reportBuilder.append("📉 지출: ${formatPerfectAmount(ourExpense)}\n")
        reportBuilder.append("💎 순이익: ${formatPerfectAmount(ourNet)} ${getAdvancedNetEmoji(ourNet)}\n")
        if (ourClubDetail != null && ourClubDetail.memberCount > 0) {
            val perPersonIncome = ourIncome / ourClubDetail.memberCount
            val perPersonExpense = ourExpense / ourClubDetail.memberCount
            reportBuilder.append("💰 멤버당 수입: ${formatPerfectAmount(perPersonIncome)}\n")
            reportBuilder.append("💸 멤버당 지출: ${formatPerfectAmount(perPersonExpense)}\n")
        }
        reportBuilder.append("\n")
        
        reportBuilder.append("🔍 유사 동아리 상세 비교\n")
        reportBuilder.append("─────────────────────────────────────\n")
        
        reportData.similar_club_reports.forEachIndexed { index, similarReport ->
            val similarIncome = similarReport.summary["income"] ?: 0
            val similarExpense = similarReport.summary["expense"] ?: 0
            val similarNet = similarIncome - similarExpense
            val similarClubDetail = clubDetailsMap[similarReport.club_id]
            
            reportBuilder.append("🏅 비교 동아리 #${index + 1}\n")
            if (similarClubDetail != null) {
                reportBuilder.append("   📛 동아리명: ${similarClubDetail.clubDetail.name}\n")
                reportBuilder.append("   🏫 소속: ${similarClubDetail.clubDetail.department}\n")
                reportBuilder.append("   📂 분야: ${similarClubDetail.clubDetail.majorCategory}\n")
                reportBuilder.append("   👥 멤버 수: ${similarClubDetail.memberCount}명\n")
                
                // 멤버수 비교
                if (ourClubDetail != null && ourClubDetail.memberCount > 0 && similarClubDetail.memberCount > 0) {
                    val memberDiff = similarClubDetail.memberCount - ourClubDetail.memberCount
                    val memberDiffPercent = ((memberDiff.toDouble() / ourClubDetail.memberCount) * 100).toInt()
                    reportBuilder.append("   👤 멤버수 차이: ${if (memberDiff > 0) "+" else ""}${memberDiff}명 (${if (memberDiffPercent > 0) "+" else ""}${memberDiffPercent}%)\n")
                }
            } else {
                reportBuilder.append("   📛 동아리명: 정보 없음\n")
                reportBuilder.append("   👥 멤버 수: 정보 없음\n")
            }
            
            reportBuilder.append("   📈 수입: ${formatPerfectAmount(similarIncome)} ${getComparisonIndicator(ourIncome, similarIncome)}\n")
            reportBuilder.append("   📉 지출: ${formatPerfectAmount(similarExpense)} ${getComparisonIndicator(ourExpense, similarExpense)}\n")
            reportBuilder.append("   💎 순이익: ${formatPerfectAmount(similarNet)} ${getComparisonIndicator(ourNet, similarNet)}\n")
            
            // 멤버당 효율성 비교
            if (similarClubDetail != null && similarClubDetail.memberCount > 0) {
                val similarPerPersonIncome = similarIncome / similarClubDetail.memberCount
                val similarPerPersonExpense = similarExpense / similarClubDetail.memberCount
                reportBuilder.append("   💰 멤버당 수입: ${formatPerfectAmount(similarPerPersonIncome)}\n")
                reportBuilder.append("   💸 멤버당 지출: ${formatPerfectAmount(similarPerPersonExpense)}\n")
                
                if (ourClubDetail != null && ourClubDetail.memberCount > 0) {
                    val ourPerPersonIncome = ourIncome / ourClubDetail.memberCount
                    val ourPerPersonExpense = ourExpense / ourClubDetail.memberCount
                    val incomeEfficiency = if (ourPerPersonIncome > 0) {
                        ((similarPerPersonIncome - ourPerPersonIncome).toDouble() / ourPerPersonIncome * 100).toInt()
                    } else 0
                    val expenseEfficiency = if (ourPerPersonExpense > 0) {
                        ((similarPerPersonExpense - ourPerPersonExpense).toDouble() / ourPerPersonExpense * 100).toInt()
                    } else 0
                    
                    reportBuilder.append("   📊 멤버당 수입 효율성: ${if (incomeEfficiency > 0) "+" else ""}${incomeEfficiency}% ${if (incomeEfficiency > 0) "📈" else "📉"}\n")
                    reportBuilder.append("   📊 멤버당 지출 효율성: ${if (expenseEfficiency > 0) "+" else ""}${expenseEfficiency}% ${if (expenseEfficiency < 0) "✅" else "❌"}\n")
                }
            }
            
            // 이벤트 및 타입 비교
            if (ourReport.by_type.isNotEmpty() && similarReport.by_type.isNotEmpty()) {
                reportBuilder.append("   📊 활동 유형 비교: ${compareActivityTypes(ourReport.by_type, similarReport.by_type)}\n")
            }
            reportBuilder.append("\n")
        }
        
        // 종합 경쟁력 분석 (멤버수 포함)
        reportBuilder.append("🎯 종합 경쟁력 분석\n")
        reportBuilder.append("─────────────────────────────────────\n")
        reportBuilder.append("${getCompetitivenessAnalysisWithMembers(ourReport, reportData.similar_club_reports, clubDetailsMap)}\n\n")
        
        reportBuilder.append("📊 리포트 생성 완료: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
        
        return reportBuilder.toString()
    }
    
    private fun createEventReportFromBackend(reportData: ApiService.YearlyReportResponse): String {
        val summary = reportData.summary
        val income = summary["income"] ?: 0
        val expense = summary["expense"] ?: 0
        val net = summary["net"] ?: (income - expense)
        
        val reportBuilder = StringBuilder()
        
        // 헤더
        reportBuilder.append("🎪 AI 이벤트 비교 분석 리포트\n")
        reportBuilder.append("=====================================\n")
        reportBuilder.append("📅 분석기간: ${reportData.year}년 전체\n")
        reportBuilder.append("🏢 장부ID: ${reportData.ledger_id}\n\n")
        
        // 전체 요약
        reportBuilder.append("💰 전체 재정 개요\n")
        reportBuilder.append("─────────────────────────────────────\n")
        reportBuilder.append("📈 총 수입: ${formatPerfectAmount(income)}\n")
        reportBuilder.append("📉 총 지출: ${formatPerfectAmount(expense)}\n")
        reportBuilder.append("💎 순 이익: ${formatPerfectAmount(net)} ${getAdvancedNetEmoji(net)}\n\n")
        
        // 월별 이벤트 활동 분석
        if (reportData.by_month.isNotEmpty()) {
            reportBuilder.append("🎪 월별 이벤트 활동 분석\n")
            reportBuilder.append("─────────────────────────────────────\n")
            
            val monthlyEventAnalysis = mutableListOf<String>()
            
            for (monthKey in reportData.by_month.keys.sorted()) {
                try {
                    val monthInt = monthKey.toInt()
                    val monthData = reportData.by_month[monthKey]
                    monthData?.let { data ->
                        val monthIncome = data.summary["income"] ?: 0
                        val monthExpense = data.summary["expense"] ?: 0
                        val monthNet = monthIncome - monthExpense
                        val eventCount = data.by_event?.size ?: 0
                        
                        if (eventCount > 0) {
                            reportBuilder.append("📆 ${monthInt}월 이벤트 활동\n")
                            reportBuilder.append("   🎯 이벤트 수: ${eventCount}개\n")
                            reportBuilder.append("   💰 이벤트 수입: ${formatPerfectAmount(monthIncome)}\n")
                            reportBuilder.append("   💸 이벤트 지출: ${formatPerfectAmount(monthExpense)}\n")
                            reportBuilder.append("   📊 순 효과: ${formatPerfectAmount(monthNet)} ${getAdvancedNetEmoji(monthNet)}\n")
                            
                            // 활성도 평가
                            val activityLevel = when {
                                eventCount >= 3 && monthNet > 0 -> "🔥 매우 활발 (수익성 우수)"
                                eventCount >= 2 -> "✨ 활발 (적절한 활동량)"
                                eventCount == 1 -> "📌 보통 (단일 이벤트)"
                                else -> "💤 저조 (이벤트 부족)"
                            }
                            reportBuilder.append("   🏆 활성도: $activityLevel\n\n")
                        }
                    }
                } catch (e: NumberFormatException) {
                    Log.w("LedgerReportCreate", "월별 이벤트 데이터 키 파싱 실패: $monthKey")
                }
            }
        }
        
        // 이벤트 카테고리별 분석 (항목별 데이터 활용)
        if (reportData.by_type.isNotEmpty()) {
            reportBuilder.append("🏷️ 이벤트 카테고리별 분석\n")
            reportBuilder.append("─────────────────────────────────────\n")
            
            val sortedTypes = reportData.by_type.entries.sortedByDescending { entry ->
                val typeData = entry.value
                (typeData["income"] ?: 0) - (typeData["expense"] ?: 0)
            }
            
            sortedTypes.forEach { (typeName, typeData) ->
                val typeIncome = typeData["income"] ?: 0
                val typeExpense = typeData["expense"] ?: 0
                val typeNet = typeIncome - typeExpense
                
                reportBuilder.append("🎭 $typeName 이벤트\n")
                reportBuilder.append("   💰 총 수입: ${formatPerfectAmount(typeIncome)}\n")
                reportBuilder.append("   💸 총 지출: ${formatPerfectAmount(typeExpense)}\n")
                reportBuilder.append("   📈 순 수익: ${formatPerfectAmount(typeNet)} ${getAdvancedNetEmoji(typeNet)}\n")
                reportBuilder.append("   ${getEventTypeAnalysis(typeName, typeIncome, typeExpense, typeNet)}\n\n")
            }
        }
        
        // AI 이벤트 전략 제안
        reportBuilder.append("🤖 AI 이벤트 전략 분석\n")
        reportBuilder.append("─────────────────────────────────────\n")
        reportBuilder.append("${getEventStrategyInsight(reportData)}\n\n")
        
        reportBuilder.append("📊 리포트 생성 완료: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
        
        return reportBuilder.toString()
    }
    
    private fun createPerfectYearlyReport(reportData: ApiService.AIReportResponse): String {
        val summary = reportData.summary
        val income = (summary["income"] as? Number)?.toInt() ?: 0
        val expense = (summary["expense"] as? Number)?.toInt() ?: 0
        val net = income - expense
        
        // 백엔드 API 응답 구조에 따른 데이터 처리
        val consolidatedByType = mutableListOf<Map<String, Any>>()
        val consolidatedByPayment = mutableListOf<Map<String, Any>>()
        val consolidatedByEvent = mutableListOf<Map<String, Any>>()
        
        Log.d("LedgerReportCreate", "🔍 백엔드 응답 분석 시작...")
        Log.d("LedgerReportCreate", "   by_type: ${reportData.by_type}")
        Log.d("LedgerReportCreate", "   by_payment_method: ${reportData.by_payment_method}")
        Log.d("LedgerReportCreate", "   by_event: ${reportData.by_event}")
        
        // 백엔드에서 by_type이 이미 리스트로 올 경우 그대로 사용
        if (reportData.by_type.isNotEmpty()) {
            consolidatedByType.addAll(reportData.by_type)
            Log.d("LedgerReportCreate", "✅ by_type 직접 사용: ${consolidatedByType.size}개")
        }
        
        if (reportData.by_payment_method.isNotEmpty()) {
            consolidatedByPayment.addAll(reportData.by_payment_method)
            Log.d("LedgerReportCreate", "✅ by_payment_method 직접 사용: ${consolidatedByPayment.size}개")
        }
        
        if (reportData.by_event.isNotEmpty()) {
            consolidatedByEvent.addAll(reportData.by_event)
            Log.d("LedgerReportCreate", "✅ by_event 직접 사용: ${consolidatedByEvent.size}개")
        }
        
        // 만약 위 데이터들이 비어있다면 by_month에서 추출
        if (consolidatedByType.isEmpty() || consolidatedByPayment.isEmpty() || consolidatedByEvent.isEmpty()) {
            Log.d("LedgerReportCreate", "📊 월별 데이터에서 종합 정보 추출...")
            
            reportData.by_month?.let { byMonth ->
                val typeMap = mutableMapOf<String, MutableMap<String, Int>>()
                val paymentMap = mutableMapOf<String, MutableMap<String, Int>>()
                val eventMap = mutableMapOf<String, MutableMap<String, Int>>()
                
                // 각 월의 데이터를 순회하면서 종합
                for (i in 1..12) {
                    val monthKey = i.toString()
                    val monthData = byMonth[monthKey] as? Map<String, Any> ?: continue
                    Log.d("LedgerReportCreate", "   처리 중: ${monthKey}월")
                    
                    // by_type 데이터 종합
                    (monthData["by_type"] as? List<Map<String, Any>>)?.forEach { typeItem ->
                        val typeName = typeItem["type"] as? String ?: "기타"
                        val typeIncome = (typeItem["income"] as? Number)?.toInt() ?: 0
                        val typeExpense = (typeItem["expense"] as? Number)?.toInt() ?: 0
                        
                        if (!typeMap.containsKey(typeName)) {
                            typeMap[typeName] = mutableMapOf("income" to 0, "expense" to 0)
                        }
                        typeMap[typeName]!!["income"] = typeMap[typeName]!!["income"]!! + typeIncome
                        typeMap[typeName]!!["expense"] = typeMap[typeName]!!["expense"]!! + typeExpense
                    }
                    
                    // by_payment_method 데이터 종합
                    (monthData["by_payment_method"] as? List<Map<String, Any>>)?.forEach { paymentItem ->
                        val paymentName = paymentItem["payment_method"] as? String ?: "기타"
                        val paymentIncome = (paymentItem["income"] as? Number)?.toInt() ?: 0
                        val paymentExpense = (paymentItem["expense"] as? Number)?.toInt() ?: 0
                        
                        if (!paymentMap.containsKey(paymentName)) {
                            paymentMap[paymentName] = mutableMapOf("income" to 0, "expense" to 0)
                        }
                        paymentMap[paymentName]!!["income"] = paymentMap[paymentName]!!["income"]!! + paymentIncome
                        paymentMap[paymentName]!!["expense"] = paymentMap[paymentName]!!["expense"]!! + paymentExpense
                    }
                    
                    // by_event 데이터 종합
                    (monthData["by_event"] as? List<Map<String, Any>>)?.forEach { eventItem ->
                        val eventName = eventItem["event_name"] as? String ?: "이벤트 미지정"
                        val eventIncome = (eventItem["income"] as? Number)?.toInt() ?: 0
                        val eventExpense = (eventItem["expense"] as? Number)?.toInt() ?: 0
                        
                        if (!eventMap.containsKey(eventName)) {
                            eventMap[eventName] = mutableMapOf("income" to 0, "expense" to 0)
                        }
                        eventMap[eventName]!!["income"] = eventMap[eventName]!!["income"]!! + eventIncome
                        eventMap[eventName]!!["expense"] = eventMap[eventName]!!["expense"]!! + eventExpense
                    }
                }
                
                // 종합된 데이터를 리스트로 변환 (기존 데이터가 없을 때만)
                if (consolidatedByType.isEmpty()) {
                    typeMap.forEach { (typeName, data) ->
                        consolidatedByType.add(mapOf(
                            "type" to typeName,
                            "income" to data["income"]!!,
                            "expense" to data["expense"]!!,
                            "net" to (data["income"]!! - data["expense"]!!)
                        ))
                    }
                }
                
                if (consolidatedByPayment.isEmpty()) {
                    paymentMap.forEach { (paymentName, data) ->
                        consolidatedByPayment.add(mapOf(
                            "payment_method" to paymentName,
                            "income" to data["income"]!!,
                            "expense" to data["expense"]!!,
                            "net" to (data["income"]!! - data["expense"]!!)
                        ))
                    }
                }
                
                if (consolidatedByEvent.isEmpty()) {
                    eventMap.forEach { (eventName, data) ->
                        consolidatedByEvent.add(mapOf(
                            "event_name" to eventName,
                            "income" to data["income"]!!,
                            "expense" to data["expense"]!!,
                            "net" to (data["income"]!! - data["expense"]!!)
                        ))
                    }
                }
            }
        }
        
        Log.d("LedgerReportCreate", "📊 최종 데이터:")
        Log.d("LedgerReportCreate", "   최종 by_type: ${consolidatedByType.size}개")
        Log.d("LedgerReportCreate", "   최종 by_payment: ${consolidatedByPayment.size}개")
        Log.d("LedgerReportCreate", "   최종 by_event: ${consolidatedByEvent.size}개")
        
        return buildString {
            // 🎯 프리미엄 헤더
            appendLine("🤖 Hey-Bi AI 고급 재정 분석 리포트")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📅 분석 기간: ${reportData.year}년 (12개월 종합)")
            appendLine("🔍 분석 엔진: Hey-Bi Advanced Analytics Engine v3.0")
            appendLine("⚡ 실시간 AI 처리: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            
            // 🎯 AI 핵심 인사이트 (최상단 배치)
            appendLine("🎯 Hey-Bi AI 핵심 인사이트")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            appendLine(generateAdvancedAIInsights(income, expense, net))
            appendLine()
            
            // 💰 재정 현황 대시보드
            appendLine("💰 재정 현황 대시보드")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            appendLine("┌─ 총 수입: ${formatPerfectAmount(income)} ${getAdvancedAmountEmoji(income)}")
            appendLine("├─ 총 지출: ${formatPerfectAmount(expense)} ${getAdvancedAmountEmoji(expense)}")
            appendLine("├─ 순수익: ${formatPerfectAmount(net)} ${getAdvancedNetEmoji(net)}")
            appendLine("├─ 재정 건전도: ${getAdvancedFinancialHealth(income, expense, net)}")
            appendLine("├─ 지출 비율: ${calculateExpenseRatio(income, expense)}% ${getExpenseRatioEmoji(income, expense)}")
            appendLine("└─ 저축률: ${calculateSavingRate(income, expense)}% ${getSavingRateEmoji(income, expense)}")
            appendLine()
            
            // 📊 AI 심화 재정 분석
            appendLine("📊 AI 심화 재정 분석")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            appendLine(generateAdvancedFinancialAnalysis(income, expense, net))
            appendLine()
            
            if (consolidatedByType.isNotEmpty()) {
                appendLine("🏷️ 거래 유형별 AI 분석")
                appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
                val sortedTypes = consolidatedByType.sortedByDescending { (it["expense"] as? Number)?.toInt() ?: 0 }
                sortedTypes.forEachIndexed { index, typeData ->
                    val type = typeData["type"] as? String ?: "기타"
                    val typeIncome = (typeData["income"] as? Number)?.toInt() ?: 0
                    val typeExpense = (typeData["expense"] as? Number)?.toInt() ?: 0
                    val typeNet = typeIncome - typeExpense
                    
                    appendLine("${index + 1}. 📋 $type")
                    appendLine("   ├─ 수입: ${formatPerfectAmount(typeIncome)}")
                    appendLine("   ├─ 지출: ${formatPerfectAmount(typeExpense)}")
                    appendLine("   ├─ 순손익: ${formatPerfectAmount(typeNet)} ${getAdvancedNetEmoji(typeNet)}")
                    appendLine("   └─ AI 평가: ${getAdvancedTypeAnalysis(type, typeIncome, typeExpense, typeNet)}")
                    appendLine()
                }
            }
            
            if (consolidatedByPayment.isNotEmpty()) {
                appendLine("💳 결제 수단별 AI 최적화 분석")
                appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
                val sortedPayments = consolidatedByPayment.sortedByDescending { (it["expense"] as? Number)?.toInt() ?: 0 }
                sortedPayments.forEachIndexed { index, paymentData ->
                    val method = paymentData["payment_method"] as? String ?: "기타"
                    val methodIncome = (paymentData["income"] as? Number)?.toInt() ?: 0
                    val methodExpense = (paymentData["expense"] as? Number)?.toInt() ?: 0
                    val methodNet = methodIncome - methodExpense
                    
                    appendLine("${index + 1}. 💰 $method")
                    appendLine("   ├─ 수입: ${formatPerfectAmount(methodIncome)}")
                    appendLine("   ├─ 지출: ${formatPerfectAmount(methodExpense)}")
                    appendLine("   ├─ 순손익: ${formatPerfectAmount(methodNet)}")
                    appendLine("   └─ AI 제안: ${getAdvancedPaymentAnalysis(method, methodIncome, methodExpense, methodNet)}")
                    appendLine()
                }
            }
            
            if (consolidatedByEvent.isNotEmpty()) {
                appendLine("🎯 이벤트별 AI 성과 분석")
                appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
                val sortedEvents = consolidatedByEvent.sortedByDescending { 
                    val eventIncome = (it["income"] as? Number)?.toInt() ?: 0
                    val eventExpense = (it["expense"] as? Number)?.toInt() ?: 0
                    eventIncome - eventExpense
                }
                
                sortedEvents.forEachIndexed { index, eventData ->
                    val eventName = eventData["event_name"] as? String ?: "일반 활동"
                    val eventIncome = (eventData["income"] as? Number)?.toInt() ?: 0
                    val eventExpense = (eventData["expense"] as? Number)?.toInt() ?: 0
                    val eventNet = eventIncome - eventExpense
                    val roi = calculateROI(eventIncome, eventExpense)
                    
                    appendLine("${index + 1}. 🎪 $eventName")
                    appendLine("   ├─ 수입: ${formatPerfectAmount(eventIncome)}")
                    appendLine("   ├─ 지출: ${formatPerfectAmount(eventExpense)}")
                    appendLine("   ├─ 순손익: ${formatPerfectAmount(eventNet)} ${getAdvancedNetEmoji(eventNet)}")
                    appendLine("   ├─ ROI: ${roi}% ${getROIEmoji(roi)}")
                    appendLine("   └─ AI 평가: ${getAdvancedEventAnalysis(eventName, eventIncome, eventExpense, eventNet)}")
                    appendLine()
                }
                
                appendLine("📈 이벤트 성과 요약")
                val profitableEvents = sortedEvents.count { 
                    val eventIncome = (it["income"] as? Number)?.toInt() ?: 0
                    val eventExpense = (it["expense"] as? Number)?.toInt() ?: 0
                    eventIncome > eventExpense
                }
                appendLine("├─ 수익성 이벤트: ${profitableEvents}개/${sortedEvents.size}개 (${((profitableEvents.toDouble()/sortedEvents.size)*100).roundToInt()}%)")
                
                val avgEventROI = sortedEvents.map { calculateROI((it["income"] as? Number)?.toInt() ?: 0, (it["expense"] as? Number)?.toInt() ?: 0) }.average()
                appendLine("├─ 평균 이벤트 ROI: ${avgEventROI.roundToInt()}% ${getROIEmoji(avgEventROI.roundToInt())}")
                
                val totalEventIncome = sortedEvents.sumOf { (it["income"] as? Number)?.toInt() ?: 0 }
                val totalEventExpense = sortedEvents.sumOf { (it["expense"] as? Number)?.toInt() ?: 0 }
                if (expense > 0) {
                    val eventExpenseRatio = ((totalEventExpense.toDouble() / expense) * 100).roundToInt()
                    appendLine("└─ 전체 지출 중 이벤트 비중: ${eventExpenseRatio}% ${getEventRatioEmoji(100, eventExpenseRatio)}")
                }
                appendLine()
            }
            
            // 🔮 AI 예측 및 전망
            appendLine("🔮 AI 예측 및 전망 분석")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            appendLine(generateAdvancedForecast(reportData))
            appendLine()
            
            // 💡 AI 맞춤형 액션 플랜
            appendLine("💡 AI 맞춤형 액션 플랜")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            appendLine(generateAdvancedActionPlan(income, expense, net, reportData))
            appendLine()
            
            // 📊 성과 지표 스코어카드
            appendLine("📊 Hey-Bi AI 성과 지표 스코어카드")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            appendLine(generateAdvancedScoreCard(income, expense, net, reportData))
            appendLine()
            
            // 🏆 최종 AI 평가 및 등급
            appendLine("🏆 Hey-Bi AI 최종 재정 등급")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            appendLine(generateFinalAIGrade(income, expense, net, reportData))
            appendLine()
            
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("✨ Hey-Bi AI 고급 분석 완료")
            appendLine("📊 이 리포트는 실제 데이터 기반 AI 분석 결과입니다")
            appendLine("🔄 월 1회 정기 분석으로 더 정확한 인사이트를 받아보세요")
            appendLine("🎯 문의사항: Hey-Bi AI 지원팀")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }
    
    private fun createPerfectComparisonReport(comparisonData: ApiService.SimilarClubsReportResponse): String {
        return buildString {
            appendLine("🏆 Hey-Bi AI 고급 동아리 비교 분석 리포트")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🔍 분석 엔진: Hey-Bi Comparative Analytics v3.0")
            appendLine("⚡ 실시간 처리: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("📊 비교 대상: ${comparisonData.similar_club_reports.size}개 유사 동아리")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            
            val ourReport = comparisonData.original_club_report
            val ourIncome = (ourReport.summary["income"] as? Number)?.toInt() ?: 0
            val ourExpense = (ourReport.summary["expense"] as? Number)?.toInt() ?: 0
            val ourNet = ourIncome - ourExpense
            
            appendLine("🏢 우리 동아리 현황 (기준선)")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            appendLine("├─ 총 수입: ${formatPerfectAmount(ourIncome)} ${getAdvancedAmountEmoji(ourIncome)}")
            appendLine("├─ 총 지출: ${formatPerfectAmount(ourExpense)} ${getAdvancedAmountEmoji(ourExpense)}")
            appendLine("├─ 순수익: ${formatPerfectAmount(ourNet)} ${getAdvancedNetEmoji(ourNet)}")
            appendLine("└─ 재정 건전도: ${getAdvancedFinancialHealth(ourIncome, ourExpense, ourNet)}")
            appendLine()
            
            appendLine("🔍 유사 동아리 상세 비교")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            
            val similarReports = comparisonData.similar_club_reports
            val avgIncome = similarReports.map { (it.summary["income"] as? Number)?.toInt() ?: 0 }.average()
            val avgExpense = similarReports.map { (it.summary["expense"] as? Number)?.toInt() ?: 0 }.average()
            val avgNet = avgIncome - avgExpense
            
            similarReports.forEachIndexed { index, similarReport ->
                val similarIncome = (similarReport.summary["income"] as? Number)?.toInt() ?: 0
                val similarExpense = (similarReport.summary["expense"] as? Number)?.toInt() ?: 0
                val similarNet = similarIncome - similarExpense
                
                appendLine("${index + 1}. 📊 비교 동아리 ${('A' + index)}")
                appendLine("   ├─ 수입: ${formatPerfectAmount(similarIncome)} ${getComparisonEmoji(ourIncome, similarIncome)}")
                appendLine("   ├─ 지출: ${formatPerfectAmount(similarExpense)} ${getComparisonEmoji(ourExpense, similarExpense)}")
                appendLine("   ├─ 순수익: ${formatPerfectAmount(similarNet)} ${getComparisonEmoji(ourNet, similarNet)}")
                appendLine("   └─ 상대적 성과: ${getRelativePerformance(ourNet, similarNet)}")
                appendLine()
            }
            
            appendLine("📈 AI 비교 분석 결과")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            
            // 수입 비교
            val incomeComparison = ((ourIncome - avgIncome) / avgIncome * 100).roundToInt()
            appendLine("💰 수입 경쟁력:")
            when {
                incomeComparison > 20 -> appendLine("   🌟 평균보다 ${incomeComparison}% 높음 - 뛰어난 수입 창출력!")
                incomeComparison > 0 -> appendLine("   💪 평균보다 ${incomeComparison}% 높음 - 우수한 수입 관리")
                incomeComparison > -10 -> appendLine("   📊 평균 수준 유지 - 안정적 운영")
                else -> appendLine("   📈 평균보다 ${Math.abs(incomeComparison)}% 낮음 - 수입 증대 필요")
            }
            
            // 지출 비교 (낮을수록 좋음)
            val expenseComparison = ((ourExpense - avgExpense) / avgExpense * 100).roundToInt()
            appendLine("💸 지출 효율성:")
            when {
                expenseComparison < -20 -> appendLine("   🌟 평균보다 ${Math.abs(expenseComparison)}% 낮음 - 뛰어난 비용 관리!")
                expenseComparison < 0 -> appendLine("   ✅ 평균보다 ${Math.abs(expenseComparison)}% 낮음 - 효율적 지출")
                expenseComparison < 10 -> appendLine("   📊 평균 수준 - 적정 지출 관리")
                else -> appendLine("   ⚠️ 평균보다 ${expenseComparison}% 높음 - 지출 절약 검토 필요")
            }
            
            // 순수익 비교
            val netComparison = if (avgNet != 0.0) ((ourNet - avgNet) / avgNet * 100).roundToInt() else 0
            appendLine("📊 종합 재정 성과:")
            when {
                netComparison > 50 -> appendLine("   🏆 평균보다 ${netComparison}% 높음 - 최상급 재정 운영!")
                netComparison > 0 -> appendLine("   🌟 평균보다 ${netComparison}% 높음 - 우수한 재정 성과")
                netComparison > -20 -> appendLine("   📊 평균 수준 - 안정적 재정 관리")
                else -> appendLine("   📈 개선 여지 있음 - 전략적 재정 계획 필요")
            }
            appendLine()
            
            appendLine("🎯 AI 맞춤 벤치마킹 전략")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            appendLine(generateBenchmarkingStrategy(ourReport))
            appendLine()
            
            appendLine("🚀 경쟁력 강화 로드맵")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            appendLine(generateCompetitivenessRoadmap(ourReport))
            appendLine()
            
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("✨ Hey-Bi AI 비교 분석 완료")
            appendLine("🏆 지속적인 벤치마킹으로 경쟁력을 강화하세요")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }
    
    private fun createPerfectEventReport(reportData: ApiService.AIReportResponse): String {
        return buildString {
            appendLine("📅 Hey-Bi AI 고급 이벤트 비교 분석 리포트")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🔍 분석 기간: ${reportData.year}년 (이벤트 중심 분석)")
            appendLine("⚡ AI 엔진: Hey-Bi Event Analytics Pro v3.0")
            appendLine("🎯 분석 대상: ${reportData.by_event.size}개 이벤트")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            
            val summary = reportData.summary
            val totalIncome = (summary["income"] as? Number)?.toInt() ?: 0
            val totalExpense = (summary["expense"] as? Number)?.toInt() ?: 0
            val totalNet = totalIncome - totalExpense
            
            appendLine("📊 전체 재정 현황 대시보드")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            appendLine("├─ 연간 총 수입: ${formatPerfectAmount(totalIncome)}")
            appendLine("├─ 연간 총 지출: ${formatPerfectAmount(totalExpense)}")
            appendLine("├─ 연간 순수익: ${formatPerfectAmount(totalNet)} ${getAdvancedNetEmoji(totalNet)}")
            appendLine("└─ 전체 재정 등급: ${getAdvancedFinancialHealth(totalIncome, totalExpense, totalNet)}")
            appendLine()
            
            if (reportData.by_event.isNotEmpty()) {
                appendLine("🎪 이벤트별 고급 성과 분석")
                appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
                
                val sortedEvents = reportData.by_event.sortedByDescending { 
                    val eventIncome = (it["income"] as? Number)?.toInt() ?: 0
                    val eventExpense = (it["expense"] as? Number)?.toInt() ?: 0
                    eventIncome - eventExpense
                }
                
                sortedEvents.forEachIndexed { index, eventData ->
                    val eventName = eventData["event_name"] as? String ?: "일반 활동 ${index + 1}"
                    val eventIncome = (eventData["income"] as? Number)?.toInt() ?: 0
                    val eventExpense = (eventData["expense"] as? Number)?.toInt() ?: 0
                    val eventNet = eventIncome - eventExpense
                    val roi = calculateROI(eventIncome, eventExpense)
                    val efficiency = calculateEfficiency(eventIncome, eventExpense)
                    
                    val rankEmoji = when (index) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> "${index + 1}."
                    }
                    
                    appendLine("$rankEmoji $eventName ${getEventCategoryEmoji(eventName)}")
                    appendLine("   ├─ 수입: ${formatPerfectAmount(eventIncome)} ${getAdvancedAmountEmoji(eventIncome)}")
                    appendLine("   ├─ 지출: ${formatPerfectAmount(eventExpense)} ${getAdvancedAmountEmoji(eventExpense)}")
                    appendLine("   ├─ 순손익: ${formatPerfectAmount(eventNet)} ${getAdvancedNetEmoji(eventNet)}")
                    appendLine("   ├─ ROI: ${roi}% ${getROIEmoji(roi)}")
                    appendLine("   ├─ 효율성 점수: ${efficiency}점/100 ${getEfficiencyEmoji(efficiency)}")
                    appendLine("   ├─ AI 등급: ${getEventGrade(roi, efficiency)}")
                    appendLine("   └─ 맞춤 제안: ${getAdvancedEventStrategy(eventName, roi)}")
                    appendLine()
                }
                
                appendLine("📈 이벤트 성과 종합 분석")
                appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
                
                val profitableEvents = sortedEvents.count { 
                    val eventIncome = (it["income"] as? Number)?.toInt() ?: 0
                    val eventExpense = (it["expense"] as? Number)?.toInt() ?: 0
                    eventIncome > eventExpense
                }
                
                val totalEventIncome = sortedEvents.sumOf { (it["income"] as? Number)?.toInt() ?: 0 }
                val totalEventExpense = sortedEvents.sumOf { (it["expense"] as? Number)?.toInt() ?: 0 }
                val totalEventNet = totalEventIncome - totalEventExpense
                val avgEventROI = sortedEvents.map { calculateROI((it["income"] as? Number)?.toInt() ?: 0, (it["expense"] as? Number)?.toInt() ?: 0) }.average()
                
                appendLine("🏆 성과 지표 요약:")
                appendLine("   ├─ 수익성 이벤트: ${profitableEvents}/${sortedEvents.size}개 (${((profitableEvents.toDouble()/sortedEvents.size)*100).roundToInt()}%)")
                appendLine("   ├─ 평균 이벤트 ROI: ${avgEventROI.roundToInt()}% ${getROIEmoji(avgEventROI.roundToInt())}")
                appendLine("   ├─ 이벤트 총 기여도: ${formatPerfectAmount(totalEventNet)} ${getAdvancedNetEmoji(totalEventNet)}")
                if (totalExpense > 0) {
                    val eventExpenseRatio = ((totalEventExpense.toDouble() / totalExpense) * 100).roundToInt()
                    appendLine("   ├─ 전체 지출 중 이벤트 비중: ${eventExpenseRatio}% ${getEventRatioEmoji(100, eventExpenseRatio)}")
                }
                appendLine("   └─ 종합 이벤트 등급: ${getOverallEventGrade(sortedEvents.size, avgEventROI.roundToInt())}")
                appendLine()
                
                // 최고 성과 이벤트 분석
                if (sortedEvents.isNotEmpty()) {
                    val bestEvent = sortedEvents[0]
                    val bestEventName = bestEvent["event_name"] as? String ?: "최고 성과 이벤트"
                    val bestEventIncome = (bestEvent["income"] as? Number)?.toInt() ?: 0
                    val bestEventExpense = (bestEvent["expense"] as? Number)?.toInt() ?: 0
                    val bestEventNet = bestEventIncome - bestEventExpense
                    
                    appendLine("🌟 최고 성과 이벤트 심층 분석")
                    appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
                    appendLine("🏆 $bestEventName")
                    appendLine("   ✅ 성공 요인:")
                    appendLine("      ├─ 높은 수익성: ${formatPerfectAmount(bestEventNet)} 달성")
                    appendLine("      ├─ 효율적 운영: ROI ${calculateROI(bestEventIncome, bestEventExpense)}%")
                    appendLine("      └─ 최적화된 예산 활용")
                    appendLine("   🚀 재현 전략:")
                    appendLine("      ├─ 동일한 성공 패턴을 다른 이벤트에 적용")
                    appendLine("      ├─ 참가자 만족도 유지 요소 파악")
                    appendLine("      └─ 규모 확대 가능성 검토")
                    appendLine()
                }
                
            } else {
                appendLine("⚠️ 이벤트 데이터 분석 결과")
                appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
                appendLine("📋 현재 분석 가능한 이벤트 데이터가 제한적입니다.")
                appendLine()
                appendLine("🎯 Hey-Bi AI 제안:")
                appendLine("   ├─ 정기 모임을 이벤트로 등록하여 성과 추적")
                appendLine("   ├─ 워크샵, 세미나 등을 별도 이벤트로 분류")
                appendLine("   ├─ 각 이벤트별 예산 설정 및 결과 기록")
                appendLine("   └─ 3개월 후 재분석으로 트렌드 파악")
                appendLine()
            }
            
            appendLine("💡 AI 이벤트 최적화 마스터플랜")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            appendLine(generateEventOptimizationPlan(reportData.by_event))
            appendLine()
            
            appendLine("🔮 다음 년도 이벤트 전략 로드맵")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            appendLine(generateNextYearEventStrategy(reportData.by_event))
            appendLine()
            
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("✨ Hey-Bi AI 이벤트 분석 완료")
            appendLine("🎯 데이터 기반 이벤트 기획으로 더 나은 성과를 만들어보세요")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }
    
    private fun createFallbackComparisonReport(reportTitle: String, clubId: Int): String {
        return buildString {
            appendLine("🏆 Hey-Bi AI 동아리 비교 분석 리포트")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📊 리포트명: $reportTitle")
            appendLine("🏢 동아리 ID: $clubId")
            appendLine("⚡ 생성 시간: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            
            appendLine("📋 분석 상태 안내")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            appendLine("⏳ 현재 유사 동아리 비교 데이터를 수집하고 있습니다.")
            appendLine("🔄 더 많은 동아리 데이터가 축적되면 정확한 비교 분석이 가능합니다.")
            appendLine()
            
            appendLine("🎯 현재 이용 가능한 분석")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            appendLine("✅ 연간 종합 분석 리포트 - 완전 지원")
            appendLine("✅ 이벤트별 비교 분석 리포트 - 완전 지원")
            appendLine("🔄 동아리 비교 분석 - 데이터 수집 중")
            appendLine()
            
            appendLine("💡 Hey-Bi AI 임시 권장사항")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            appendLine("📈 꾸준한 재정 기록으로 분석 정확도를 높이세요")
            appendLine("🤝 다른 동아리와의 정보 공유로 비교 분석 품질 향상")
            appendLine("📊 월별 재정 현황 기록으로 트렌드 파악")
            appendLine("🎯 이벤트별 세부 기록으로 성과 분석 강화")
            appendLine()
            
            appendLine("🔮 업데이트 예정")
            appendLine("▔▔▔▔▔▔▔▔▔▔▔▔▔▔")
            appendLine("📅 다음 주요 업데이트에서 완전한 비교 분석 제공 예정")
            appendLine("🏆 업계 벤치마킹 데이터 추가")
            appendLine("📊 카테고리별 세분화된 비교 분석")
            appendLine()
            
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("✨ Hey-Bi AI 분석 시스템")
            appendLine("📞 문의: Hey-Bi 지원팀")
            appendLine("🔄 정기적인 업데이트로 더 나은 분석을 제공합니다")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }
    
    private fun saveReportWithAdvancedMetrics(title: String, content: String, type: String, clubId: Int, reportData: ApiService.AIReportResponse? = null) {
        Log.d("LedgerReportCreate", "💾 고급 메트릭과 함께 리포트 저장 - 제목: $title")
        
        val sharedPref = getSharedPreferences("ai_reports_club_$clubId", Context.MODE_PRIVATE)
        val existingReportsJson = sharedPref.getString("reports_json", "[]")
        val existingReportsArray = org.json.JSONArray(existingReportsJson ?: "[]")
        
        val reportData = JSONObject().apply {
            put("id", System.currentTimeMillis())
            put("title", title)
            put("content", content)
            put("type", type)
            put("created_at", System.currentTimeMillis())
            put("created_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            put("club_id", clubId)
            put("version", "3.0") // 고급 버전 표시
            put("ai_engine", "Hey-Bi Advanced Analytics")
            
            // 추가 메트릭
            reportData?.let { data ->
                val summary = data.summary
                val income = (summary["income"] as? Number)?.toInt() ?: 0
                val expense = (summary["expense"] as? Number)?.toInt() ?: 0
                val net = income - expense
                
                put("metrics", JSONObject().apply {
                    put("total_income", income)
                    put("total_expense", expense)
                    put("net_profit", net)
                    put("expense_ratio", calculateExpenseRatio(income, expense))
                    put("saving_rate", calculateSavingRate(income, expense))
                    put("financial_grade", getAdvancedFinancialHealth(income, expense, net))
                    put("event_count", data.by_event.size)
                    put("payment_methods", data.by_payment_method.size)
                    put("transaction_types", data.by_type.size)
                })
            }
        }
        
        existingReportsArray.put(reportData)
        
        val success = sharedPref.edit()
            .putString("reports_json", existingReportsArray.toString())
            .commit()
            
        Log.d("LedgerReportCreate", "💾 고급 저장 완료: $success, 총 리포트: ${existingReportsArray.length()}개")
        
        val resultIntent = android.content.Intent()
        resultIntent.putExtra("report_created", true)
        resultIntent.putExtra("report_title", title)
        resultIntent.putExtra("report_type", type)
        resultIntent.putExtra("report_version", "3.0")
        setResult(android.app.Activity.RESULT_OK, resultIntent)
        
        showPerfectSuccessDialog(content, title, type)
    }
    
    // YearlyReportResponse용 오버로드 (매개변수가 적음)
    private fun saveReportWithAdvancedMetrics(title: String, content: String, type: String, clubId: Int) {
        Log.d("LedgerReportCreate", "💾 연간 리포트 저장 (백엔드 연동) - 제목: $title")
        
        val sharedPref = getSharedPreferences("ai_reports_club_$clubId", Context.MODE_PRIVATE)
        val existingReportsJson = sharedPref.getString("reports_json", "[]")
        val existingReportsArray = org.json.JSONArray(existingReportsJson ?: "[]")
        
        val reportData = JSONObject().apply {
            put("id", System.currentTimeMillis())
            put("title", title)
            put("content", content)
            put("type", type)
            put("created_at", System.currentTimeMillis())
            put("created_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            put("club_id", clubId)
            put("version", "4.0") // 백엔드 연동 버전
            put("ai_engine", "Hey-Bi Backend Analytics")
        }
        
        existingReportsArray.put(reportData)
        
        val success = sharedPref.edit()
            .putString("reports_json", existingReportsArray.toString())
            .commit()
        
        if (success) {
            Log.d("LedgerReportCreate", "✅ 백엔드 연동 리포트 저장 완료")
            Toast.makeText(this, "AI 리포트가 생성되었습니다!", Toast.LENGTH_LONG).show()
            finish() // 리포트 생성 후 자동 종료
        } else {
            Log.e("LedgerReportCreate", "❌ 리포트 저장 실패")
            Toast.makeText(this, "리포트 저장에 실패했습니다", Toast.LENGTH_LONG).show()
        }
    }
    
    // =================
    // HELPER FUNCTIONS
    // =================
    
    private fun formatPerfectAmount(amount: Int): String {
        return "${String.format(Locale.US, "%,d", amount)}원"
    }
    
    private fun calculateExpenseRatio(income: Int, expense: Int): Int {
        return if (income > 0) ((expense.toDouble() / income) * 100).roundToInt() else 0
    }
    
    private fun calculateSavingRate(income: Int, expense: Int): Int {
        return if (income > 0) (((income - expense).toDouble() / income) * 100).roundToInt() else 0
    }
    
    private fun calculateROI(income: Int, expense: Int): Int {
        return if (expense > 0) (((income - expense).toDouble() / expense) * 100).roundToInt() else 0
    }
    
    private fun calculateEfficiency(income: Int, expense: Int): Int {
        val roi = calculateROI(income, expense)
        val base = when {
            roi > 100 -> 90
            roi > 50 -> 80
            roi > 0 -> 70
            roi > -25 -> 60
            else -> 40
        }
        return minOf(100, maxOf(0, base + (income / 10000).coerceAtMost(10)))
    }
    
    private fun getAdvancedAmountEmoji(amount: Int): String = when {
        amount > 1000000 -> "🔥💰"
        amount > 500000 -> "💪💰"
        amount > 100000 -> "👍💰"
        amount > 50000 -> "📊💰"
        amount > 0 -> "💰"
        else -> "📉"
    }
    
    
    private fun getExpenseRatioEmoji(income: Int, expense: Int): String {
        val ratio = calculateExpenseRatio(income, expense)
        return when {
            ratio < 50 -> "🌟"
            ratio < 70 -> "💚"
            ratio < 85 -> "✅"
            ratio < 100 -> "⚠️"
            else -> "🚨"
        }
    }
    
    private fun getSavingRateEmoji(income: Int, expense: Int): String {
        val rate = calculateSavingRate(income, expense)
        return when {
            rate > 30 -> "🌟💎"
            rate > 20 -> "💚🎯"
            rate > 10 -> "✅📈"
            rate > 0 -> "📊"
            else -> "⚠️"
        }
    }
    
    private fun getROIEmoji(roi: Int): String = when {
        roi > 200 -> "🚀🌟"
        roi > 100 -> "🔥💎"
        roi > 50 -> "💪🎯"
        roi > 0 -> "✅📈"
        roi > -25 -> "⚠️"
        else -> "🚨"
    }
    
    private fun getAdvancedFinancialHealth(income: Int, expense: Int, net: Int): String = when {
        net > 500000 -> "🌟🏆 최상급 (S+)"
        net > 200000 -> "🌟💎 매우우수 (S)"
        net > 100000 -> "💚🎯 우수 (A+)"
        net > 50000 -> "💚✅ 양호 (A)"
        net > 0 -> "✅📊 보통 (B)"
        net > -100000 -> "⚠️📈 주의 (C)"
        else -> "🚨📉 위험 (D)"
    }
    
    private fun generateAdvancedAIInsights(income: Int, expense: Int, net: Int): String {
        return buildString {
            when {
                net > 200000 -> {
                    appendLine("🌟 탁월한 재정 관리! 최상급 성과를 달성하고 있습니다.")
                    appendLine("💎 현재 운영 전략을 유지하며 성장 투자를 고려하세요.")
                    appendLine("🚀 여유 자금으로 혁신적인 프로젝트나 장기 투자 검토")
                    appendLine("🎯 업계 표준을 뛰어넘는 벤치마킹 사례가 될 수 있습니다.")
                }
                net > 100000 -> {
                    appendLine("💚 우수한 재정 운영! 안정적인 흑자 기조를 유지 중입니다.")
                    appendLine("📈 현재 수준 유지하며 점진적 성장 전략을 추천합니다.")
                    appendLine("🛡️ 리스크 관리를 통해 현재 성과를 지속하세요.")
                }
                net > 0 -> {
                    appendLine("✅ 안정적인 재정 관리가 이루어지고 있습니다.")
                    appendLine("📊 수입과 지출의 균형이 적절히 유지되고 있습니다.")
                    appendLine("🎯 더 나은 성과를 위한 효율성 개선 여지가 있습니다.")
                }
                net > -100000 -> {
                    appendLine("⚠️ 재정 개선이 필요한 상황입니다.")
                    appendLine("🔍 지출 구조를 면밀히 검토하고 우선순위를 재조정하세요.")
                    appendLine("💡 수입원 다각화와 비용 최적화를 동시에 추진하세요.")
                }
                else -> {
                    appendLine("🚨 즉각적인 재정 구조조정이 필요합니다.")
                    appendLine("⚡ 긴급 비용 절감과 수입 증대 방안을 즉시 실행하세요.")
                    appendLine("🛠️ 전문적인 재정 컨설팅을 고려하시기 바랍니다.")
                }
            }
            
            if (income == 0 && expense == 0) {
                clear()
                appendLine("📊 데이터 기반 분석을 위해 거래 내역 입력이 필요합니다.")
                appendLine("🔧 장부에 최근 3-6개월간의 수입/지출을 기록해주세요.")
                appendLine("📈 충분한 데이터가 축적되면 더 정밀한 AI 분석이 가능합니다.")
            }
        }
    }
    
    private fun generateAdvancedFinancialAnalysis(income: Int, expense: Int, net: Int): String {
        return buildString {
            val expenseRatio = calculateExpenseRatio(income, expense)
            val savingRate = calculateSavingRate(income, expense)
            
            appendLine("📈 재정 효율성 지표:")
            appendLine("   ├─ 지출률: ${expenseRatio}% ${getExpenseRatioEmoji(income, expense)}")
            when {
                expenseRatio < 50 -> appendLine("      └─ 매우 효율적인 지출 관리")
                expenseRatio < 70 -> appendLine("      └─ 양호한 지출 통제력")
                expenseRatio < 85 -> appendLine("      └─ 적정 수준의 지출 관리")
                expenseRatio < 100 -> appendLine("      └─ 지출 최적화 필요")
                else -> appendLine("      └─ 즉시 지출 절약 방안 필요")
            }
            
            appendLine("   ├─ 저축률: ${savingRate}% ${getSavingRateEmoji(income, expense)}")
            when {
                savingRate > 30 -> appendLine("      └─ 탁월한 자금 축적 능력")
                savingRate > 20 -> appendLine("      └─ 우수한 저축 성과")
                savingRate > 10 -> appendLine("      └─ 적정 수준의 저축률")
                savingRate > 0 -> appendLine("      └─ 저축률 개선 여지 있음")
                else -> appendLine("      └─ 저축 계획 수립 필요")
            }
            
            if (income > 0) {
                val cashFlowHealth = when {
                    savingRate > 25 -> "🌟 최상급"
                    savingRate > 15 -> "💚 우수"
                    savingRate > 5 -> "✅ 양호"
                    savingRate > -5 -> "⚠️ 주의"
                    else -> "🚨 위험"
                }
                appendLine("   └─ 현금흐름 건전성: $cashFlowHealth")
            }
        }
    }
    
    // 추가적인 고급 분석 함수들...
    private fun generateAdvancedActionPlan(income: Int, expense: Int, net: Int, reportData: ApiService.AIReportResponse): String {
        return buildString {
            appendLine("🎯 단기 액션 아이템 (1-3개월):")
            when {
                net > 100000 -> {
                    appendLine("   ✅ 성장 투자 기회 탐색 및 평가")
                    appendLine("   ✅ 예비비 목표 달성 (순이익의 20-30%)")
                    appendLine("   ✅ 성공 사례 문서화 및 벤치마킹 자료 작성")
                }
                net > 0 -> {
                    appendLine("   📊 현재 수준 유지를 위한 모니터링 시스템 구축")
                    appendLine("   📈 소규모 효율성 개선 프로젝트 실행")
                    appendLine("   🛡️ 리스크 대응 계획 수립")
                }
                else -> {
                    appendLine("   🚨 긴급 비용 절감 계획 수립 및 실행")
                    appendLine("   💰 추가 수입원 발굴 (후원, 사업 등)")
                    appendLine("   🔍 필수/선택 지출 분류 및 우선순위 조정")
                }
            }
            
            appendLine()
            appendLine("🚀 중장기 전략 과제 (6-12개월):")
            appendLine("   🎯 재정 건전성 목표 등급 설정")
            appendLine("   📊 정기 성과 모니터링 체계 구축")
            appendLine("   💡 혁신 프로젝트 기획 및 예산 배정")
            appendLine("   🤝 전략적 파트너십 및 협력 방안 모색")
        }
    }
    
    private fun generateFinalAIGrade(income: Int, expense: Int, net: Int, reportData: ApiService.AIReportResponse): String {
        val grade = getAdvancedFinancialHealth(income, expense, net)
        val score = when {
            net > 500000 -> 95
            net > 200000 -> 90
            net > 100000 -> 85
            net > 50000 -> 80
            net > 0 -> 70
            net > -100000 -> 60
            else -> 45
        }
        
        return buildString {
            appendLine("🏆 Hey-Bi AI 종합 재정 등급: $grade")
            appendLine("📊 정량적 점수: ${score}점/100점")
            appendLine()
            appendLine("🎯 등급별 의미:")
            when {
                score >= 90 -> appendLine("   🌟 업계 최고 수준의 재정 운영 능력")
                score >= 80 -> appendLine("   💚 매우 안정적이고 효율적인 재정 관리")
                score >= 70 -> appendLine("   ✅ 건전한 재정 상태로 지속 가능한 운영")
                score >= 60 -> appendLine("   ⚠️ 개선 여지가 있으나 기본 안정성 확보")
                else -> appendLine("   🔧 적극적인 재정 구조 개선이 필요한 상태")
            }
            
            appendLine()
            appendLine("📈 다음 등급까지:")
            when {
                score < 60 -> appendLine("   🎯 C등급(60점) 달성을 위해 ${60-score}점 개선 필요")
                score < 70 -> appendLine("   🎯 B등급(70점) 달성을 위해 ${70-score}점 개선 필요") 
                score < 80 -> appendLine("   🎯 A등급(80점) 달성을 위해 ${80-score}점 개선 필요")
                score < 90 -> appendLine("   🎯 S등급(90점) 달성을 위해 ${90-score}점 개선 필요")
                else -> appendLine("   🌟 이미 최고 등급! 현재 수준 유지가 목표입니다")
            }
        }
    }
    
    // 추가적인 헬퍼 함수들 계속...
    private fun generateAdvancedScoreCard(income: Int, expense: Int, net: Int, reportData: ApiService.AIReportResponse): String {
        return buildString {
            val scores = mutableMapOf<String, Int>()
            
            // 수익성 점수
            scores["profitability"] = when {
                net > 200000 -> 100
                net > 100000 -> 90
                net > 50000 -> 80
                net > 0 -> 70
                net > -50000 -> 50
                else -> 30
            }
            
            // 효율성 점수
            val efficiency = calculateExpenseRatio(income, expense)
            scores["efficiency"] = when {
                efficiency < 50 -> 100
                efficiency < 70 -> 85
                efficiency < 85 -> 75
                efficiency < 100 -> 60
                else -> 40
            }
            
            // 다양성 점수 (이벤트, 결제수단 등)
            val diversity = reportData.by_event.size + reportData.by_payment_method.size + reportData.by_type.size
            scores["diversity"] = when {
                diversity > 15 -> 100
                diversity > 10 -> 85
                diversity > 5 -> 70
                diversity > 2 -> 60
                else -> 40
            }
            
            // 안정성 점수
            val savingRate = calculateSavingRate(income, expense)
            scores["stability"] = when {
                savingRate > 25 -> 100
                savingRate > 15 -> 85
                savingRate > 5 -> 70
                savingRate > 0 -> 60
                else -> 30
            }
            
            val totalScore = scores.values.average().roundToInt()
            
            appendLine("📊 세부 성과 지표:")
            appendLine("   ├─ 수익성: ${scores["profitability"]}점 ${getScoreEmoji(scores["profitability"]!!)}")
            appendLine("   ├─ 효율성: ${scores["efficiency"]}점 ${getScoreEmoji(scores["efficiency"]!!)}")  
            appendLine("   ├─ 다양성: ${scores["diversity"]}점 ${getScoreEmoji(scores["diversity"]!!)}")
            appendLine("   ├─ 안정성: ${scores["stability"]}점 ${getScoreEmoji(scores["stability"]!!)}")
            appendLine("   └─ 종합점수: ${totalScore}점/100점 ${getScoreEmoji(totalScore)}")
        }
    }
    
    private fun getScoreEmoji(score: Int): String = when {
        score >= 90 -> "🌟"
        score >= 80 -> "💎"  
        score >= 70 -> "💚"
        score >= 60 -> "✅"
        score >= 50 -> "📊"
        else -> "⚠️"
    }
    
    // UI 및 상호작용 함수들
    private fun showAdvancedProgressDialog(title: String, message: String) {
        progressDialog = ProgressDialog(this).apply {
            setTitle(title)
            setMessage(message)
            setCancelable(false)
            show()
        }
    }
    
    private fun updateProgressMessage(message: String) {
        progressDialog?.setMessage(message)
    }
    
    private fun showValidationError(title: String, suggestion: String) {
        AlertDialog.Builder(this)
            .setTitle("⚠️ $title")
            .setMessage("$suggestion")
            .setPositiveButton("확인", null)
            .show()
    }
    
    private fun showAdvancedError(title: String, message: String, suggestion: String) {
        AlertDialog.Builder(this)
            .setTitle("🚨 $title")
            .setMessage("$message\n\n💡 $suggestion")
            .setPositiveButton("확인", null)
            .setNeutralButton("다시 시도") { _, _ ->
                // 다시 시도 로직
            }
            .show()
    }
    
    private fun showPerfectSuccessDialog(content: String, title: String, type: String) {
        val typeEmoji = when (type) {
            "yearly" -> "📊"
            "comparison" -> "🏆"
            "event_comparison" -> "📅"
            else -> "🤖"
        }
        
        AlertDialog.Builder(this)
            .setTitle("🎉 Perfect AI Report Generated!")
            .setMessage("$typeEmoji Hey-Bi AI 고급 분석이 완료되었습니다!\n\n✨ 전문가 수준의 인사이트가 포함된 리포트를 확인해보세요.")
            .setPositiveButton("리포트 보기") { _, _ ->
                showPerfectReportPreview(content, title)
            }
            .setNeutralButton("목록으로") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showPerfectReportPreview(content: String, title: String) {
        val textView = TextView(this).apply {
            text = content
            setPadding(24, 24, 24, 24)
            textSize = 13f
            setTextColor(Color.parseColor("#333333"))
            typeface = android.graphics.Typeface.MONOSPACE
        }
        
        val scrollView = android.widget.ScrollView(this).apply {
            addView(textView)
            setPadding(16, 16, 16, 16)
        }
        
        AlertDialog.Builder(this)
            .setTitle("🤖 $title")
            .setView(scrollView)
            .setPositiveButton("완료") { _, _ ->
                finish()
            }
            .setNeutralButton("공유") { _, _ ->
                // 공유 기능 (향후 구현)
                Toast.makeText(this, "공유 기능은 준비 중입니다", Toast.LENGTH_SHORT).show()
                finish()
            }
            .show()
    }
    
    private fun handleAdvancedApiError(operation: String, t: Throwable) {
        hideProgressDialog()
        Log.e("LedgerReportCreate", "🚨 $operation 고급 API 오류", t)
        
        val errorMessage = when {
            t.message?.contains("timeout", true) == true -> "서버 응답 시간이 초과되었습니다"
            t.message?.contains("network", true) == true -> "네트워크 연결을 확인해주세요"
            t.message?.contains("404", true) == true -> "요청한 리소스를 찾을 수 없습니다"
            else -> "예상치 못한 오류가 발생했습니다"
        }
        
        showAdvancedError("$operation 실패", errorMessage, "잠시 후 다시 시도하거나 네트워크 상태를 확인해주세요.")
    }
    
    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
    
    private fun testDirectApiCall(clubId: Int, reportName: String) {
        Log.d("LedgerReportCreate", "🧪 직접 API 테스트 시작: clubId=$clubId, reportName='$reportName'")
        
        showAdvancedProgressDialog("🧪 긴급 테스트 모드", "API 연결 상태 확인 중...")
        
        // 장부 목록 API 먼저 테스트
        ApiClient.getApiService().getLedgerList(clubId)
            .enqueue(object : retrofit2.Callback<List<LedgerApiItem>> {
                override fun onResponse(call: retrofit2.Call<List<LedgerApiItem>>, response: retrofit2.Response<List<LedgerApiItem>>) {
                    Log.d("LedgerReportCreate", "🧪 [테스트] 장부 목록 API 결과:")
                    Log.d("LedgerReportCreate", "   - HTTP 코드: ${response.code()}")
                    Log.d("LedgerReportCreate", "   - 성공 여부: ${response.isSuccessful}")
                    Log.d("LedgerReportCreate", "   - 데이터 개수: ${response.body()?.size ?: 0}")
                    
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        val ledgerId = response.body()!![0].id
                        Log.d("LedgerReportCreate", "   ✅ 장부 ID 획득: $ledgerId")
                        
                        updateProgressMessage("🧪 연간 리포트 API 테스트 중...")
                        
                        // 연간 리포트 API 테스트 (YearlyReportResponse 사용)
                        ApiClient.getApiService().createYearlyReport(clubId, ledgerId, currentYear)
                            .enqueue(object : retrofit2.Callback<ApiService.YearlyReportResponse> {
                                override fun onResponse(call: retrofit2.Call<ApiService.YearlyReportResponse>, response: retrofit2.Response<ApiService.YearlyReportResponse>) {
                                    Log.d("LedgerReportCreate", "🧪 [테스트] 연간 리포트 API 결과:")
                                    Log.d("LedgerReportCreate", "   - HTTP 코드: ${response.code()}")
                                    Log.d("LedgerReportCreate", "   - 성공 여부: ${response.isSuccessful}")
                                    
                                    hideProgressDialog()
                                    
                                    if (response.isSuccessful && response.body() != null) {
                                        Log.d("LedgerReportCreate", "   ✅ 리포트 생성 성공!")
                                        val reportData = response.body()!!
                                        val perfectReportContent = createYearlyReportFromBackend(reportData)
                                        saveReportWithAdvancedMetrics(reportName, perfectReportContent, "yearly", clubId)
                                        
                                        Toast.makeText(this@LedgerReportCreateActivity, "🧪 테스트 성공! 리포트가 생성되었습니다.", Toast.LENGTH_LONG).show()
                                    } else {
                                        Log.e("LedgerReportCreate", "   ❌ 리포트 생성 실패: ${response.errorBody()?.string()}")
                                        Toast.makeText(this@LedgerReportCreateActivity, "🧪 테스트 실패: HTTP ${response.code()}", Toast.LENGTH_LONG).show()
                                    }
                                }
                                
                                override fun onFailure(call: retrofit2.Call<ApiService.YearlyReportResponse>, t: Throwable) {
                                    Log.e("LedgerReportCreate", "🧪 [테스트] 연간 리포트 API 네트워크 오류", t)
                                    hideProgressDialog()
                                    Toast.makeText(this@LedgerReportCreateActivity, "🧪 네트워크 오류: ${t.message}", Toast.LENGTH_LONG).show()
                                }
                            })
                    } else {
                        Log.e("LedgerReportCreate", "   ❌ 장부 데이터 없음: ${response.errorBody()?.string()}")
                        hideProgressDialog()
                        Toast.makeText(this@LedgerReportCreateActivity, "🧪 장부 데이터 없음: HTTP ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
                
                override fun onFailure(call: retrofit2.Call<List<LedgerApiItem>>, t: Throwable) {
                    Log.e("LedgerReportCreate", "🧪 [테스트] 장부 목록 API 네트워크 오류", t)
                    hideProgressDialog()
                    Toast.makeText(this@LedgerReportCreateActivity, "🧪 네트워크 오류: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
    
    override fun getCurrentClubId(): Int {
        val clubId = intent.getIntExtra("club_id", 4)
        Log.d("LedgerReportCreate", "🏢 현재 클럽 ID: $clubId")
        
        if (clubId <= 0) {
            Log.w("LedgerReportCreate", "⚠️ 유효하지 않은 클럽 ID, 기본값 사용: 4")
            return 4
        }
        
        return clubId
    }
    
    // 3년간 분석용 헬퍼 함수들
    private fun get3YearsAnalysisInsight(yearData: Map<Int, Triple<Int, Int, Int>>): String {
        if (yearData.size < 2) {
            return "📊 분석할 데이터가 부족합니다. 최소 2년간의 데이터가 필요합니다."
        }
        
        val sortedYears = yearData.keys.sorted()
        val firstYear = yearData[sortedYears.first()]!!
        val lastYear = yearData[sortedYears.last()]!!
        
        val totalGrowth = lastYear.third - firstYear.third
        val avgIncome = yearData.values.map { it.first }.average().roundToInt()
        val avgExpense = yearData.values.map { it.second }.average().roundToInt()
        val consistency = calculateConsistencyScore(yearData.values.map { it.third })
        
        return when {
            totalGrowth > 500000 && consistency > 70 -> 
                "🚀 탁월한 성장세! 3년간 ${formatPerfectAmount(totalGrowth)}의 성장을 달성하며 안정적인 운영을 보여주고 있습니다. 현재 전략을 유지하며 규모를 확장해보세요."
            totalGrowth > 100000 && consistency > 50 -> 
                "📈 긍정적인 성장 추세입니다. 꾸준한 개선이 보이며, 향후 더 큰 도약을 위한 기반이 마련되었습니다."
            totalGrowth > 0 -> 
                "✅ 완만한 성장 중입니다. 성장 속도를 높이기 위해 새로운 수익원 개발을 고려해보세요."
            consistency > 60 -> 
                "⚖️ 안정적인 운영을 하고 있습니다. 성장을 위한 적극적인 투자와 도전이 필요한 시점입니다."
            else -> 
                "⚠️ 재정 상태가 불안정합니다. 비용 구조를 재검토하고 안정적인 수익원 확보에 집중하세요."
        }
    }
    
    private fun calculateConsistencyScore(netProfits: List<Int>): Int {
        if (netProfits.size < 2) return 0
        val avg = netProfits.average()
        val variance = netProfits.map { (it - avg) * (it - avg) }.average()
        val stdDev = kotlin.math.sqrt(variance)
        val coefficient = if (avg != 0.0) stdDev / kotlin.math.abs(avg) else 1.0
        return (100 - (coefficient * 100).coerceAtMost(100.0)).roundToInt()
    }
    
    private fun getComparisonIndicator(ourValue: Int, theirValue: Int): String = when {
        ourValue > theirValue * 1.2 -> "🟢 ↗️ 우수"
        ourValue > theirValue * 1.05 -> "🔵 ↗️ 약간 우수"
        ourValue > theirValue * 0.95 -> "🟡 ≈ 비슷"
        ourValue > theirValue * 0.8 -> "🟠 ↘️ 약간 부족"
        else -> "🔴 ↘️ 부족"
    }
    
    private fun compareActivityTypes(ourTypes: Map<String, Map<String, Int>>, theirTypes: Map<String, Map<String, Int>>): String {
        val ourTypeCount = ourTypes.size
        val theirTypeCount = theirTypes.size
        val commonTypes = ourTypes.keys.intersect(theirTypes.keys).size
        
        return when {
            ourTypeCount > theirTypeCount && commonTypes > 0 -> "더 다양한 활동 (${commonTypes}개 공통)"
            ourTypeCount < theirTypes.size && commonTypes > 0 -> "활동 다양성 부족 (${commonTypes}개 공통)"
            commonTypes > ourTypeCount / 2 -> "유사한 활동 패턴"
            else -> "서로 다른 활동 특화"
        }
    }
    
    private fun getCompetitivenessAnalysis(ourReport: ApiService.YearlyReportResponse, similarReports: List<ApiService.YearlyReportResponse>): String {
        val ourNet = (ourReport.summary["income"] ?: 0) - (ourReport.summary["expense"] ?: 0)
        val avgSimilarNet = similarReports.map { (it.summary["income"] ?: 0) - (it.summary["expense"] ?: 0) }.average()
        val ranking = (similarReports.count { (it.summary["income"] ?: 0) - (it.summary["expense"] ?: 0) < ourNet } + 1)
        
        return when {
            ranking == 1 -> "🥇 최우수 동아리! 유사 동아리 중 1위를 차지하고 있습니다. 현재 우위를 지속하기 위한 전략이 필요합니다."
            ranking <= similarReports.size / 3 -> "🥈 상위권 동아리입니다. 1위 달성을 위한 추가적인 노력이 필요합니다."
            ranking <= similarReports.size * 2 / 3 -> "🥉 중상위권 동아리입니다. 상위권 진입을 위한 체계적인 개선이 필요합니다."
            else -> "📈 하위권이지만 성장 잠재력이 있습니다. 벤치마킹을 통한 집중적인 개선이 필요합니다."
        }
    }
    
    private fun getCompetitivenessAnalysisWithMembers(ourReport: ApiService.YearlyReportResponse, similarReports: List<ApiService.YearlyReportResponse>, clubDetailsMap: Map<Int, ClubDetailWithMembers>): String {
        val ourNet = (ourReport.summary["income"] ?: 0) - (ourReport.summary["expense"] ?: 0)
        val ourClubDetail = clubDetailsMap[ourReport.club_id]
        val avgSimilarNet = similarReports.map { (it.summary["income"] ?: 0) - (it.summary["expense"] ?: 0) }.average()
        val ranking = (similarReports.count { (it.summary["income"] ?: 0) - (it.summary["expense"] ?: 0) < ourNet } + 1)
        
        val analysisBuilder = StringBuilder()
        
        // 순이익 기준 순위
        val rankingText = when {
            ranking == 1 -> "🥇 최우수 동아리! 순이익 기준 1위"
            ranking <= similarReports.size / 3 -> "🥈 상위권 동아리 (${ranking}위/${similarReports.size + 1}개)"
            ranking <= similarReports.size * 2 / 3 -> "🥉 중상위권 동아리 (${ranking}위/${similarReports.size + 1}개)"
            else -> "📈 하위권이지만 성장 잠재력 보유 (${ranking}위/${similarReports.size + 1}개)"
        }
        
        analysisBuilder.append("💰 순이익 기준 순위: $rankingText\n")
        
        // 멤버당 효율성 분석
        if (ourClubDetail != null && ourClubDetail.memberCount > 0) {
            val ourPerMemberNet = ourNet / ourClubDetail.memberCount
            val similarEfficiencies = mutableListOf<Int>()
            
            similarReports.forEach { similarReport ->
                val similarClubDetail = clubDetailsMap[similarReport.club_id]
                if (similarClubDetail != null && similarClubDetail.memberCount > 0) {
                    val similarNet = (similarReport.summary["income"] ?: 0) - (similarReport.summary["expense"] ?: 0)
                    val similarPerMemberNet = similarNet / similarClubDetail.memberCount
                    similarEfficiencies.add(similarPerMemberNet)
                }
            }
            
            if (similarEfficiencies.isNotEmpty()) {
                val avgEfficiency = similarEfficiencies.average().toInt()
                val efficiencyRanking = similarEfficiencies.count { it < ourPerMemberNet } + 1
                val efficiencyPercent = if (avgEfficiency > 0) {
                    ((ourPerMemberNet - avgEfficiency).toDouble() / avgEfficiency * 100).toInt()
                } else 0
                
                analysisBuilder.append("👥 멤버당 효율성: ${formatPerfectAmount(ourPerMemberNet)} (평균 대비 ${if (efficiencyPercent > 0) "+" else ""}${efficiencyPercent}%)\n")
                analysisBuilder.append("📊 효율성 순위: ${efficiencyRanking}위/${similarEfficiencies.size + 1}개 동아리 중\n")
                
                val efficiencyAdvice = when {
                    efficiencyRanking == 1 -> "⭐ 멤버당 수익성이 가장 우수합니다!"
                    efficiencyPercent > 20 -> "✅ 평균 대비 높은 효율성을 보입니다"
                    efficiencyPercent > 0 -> "📈 평균보다 약간 우수한 효율성"
                    efficiencyPercent > -20 -> "⚠️ 평균 수준의 효율성, 개선 필요"
                    else -> "🔥 효율성 개선이 시급합니다"
                }
                analysisBuilder.append("💡 효율성 평가: $efficiencyAdvice\n")
            }
        }
        
        // 개선 제안
        analysisBuilder.append("\n🎯 개선 전략 제안:\n")
        when {
            ranking == 1 -> {
                analysisBuilder.append("• 리더십 위치를 활용한 동아리 간 네트워킹 확대\n")
                analysisBuilder.append("• 성공 사례를 다른 동아리와 공유하여 브랜드 가치 향상\n")
                analysisBuilder.append("• 지속 가능한 성장을 위한 장기 전략 수립\n")
            }
            ranking <= similarReports.size / 2 -> {
                analysisBuilder.append("• 1위 동아리의 운영 방식 벤치마킹\n")
                analysisBuilder.append("• 수입원 다각화 및 비용 최적화 전략 수립\n")
                analysisBuilder.append("• 멤버 참여도 증대를 통한 활동 활성화\n")
            }
            else -> {
                analysisBuilder.append("• 상위권 동아리들의 성공 요인 집중 분석\n")
                analysisBuilder.append("• 기본적인 재정 관리 체계 정비\n")
                analysisBuilder.append("• 단기적 성과 창출을 위한 핵심 활동 집중\n")
            }
        }
        
        return analysisBuilder.toString()
    }
    
    // 새로운 백엔드 응답용 헬퍼 함수들
    private fun getYearlyAnalysisInsight(income: Int, expense: Int, net: Int): String {
        val profitRate = if (income > 0) ((net.toDouble() / income.toDouble()) * 100).roundToInt() else 0
        val efficiency = if (expense > 0) ((income.toDouble() / expense.toDouble()) * 100).roundToInt() else 100
        
        return when {
            profitRate > 20 && efficiency > 120 -> "🎯 탁월한 재정 운영! 높은 수익률과 효율성을 동시에 달성했습니다. 현재 전략을 유지하며 규모 확장을 고려해보세요."
            profitRate > 10 && efficiency > 100 -> "✅ 건전한 재정 상태입니다. 꾸준한 흑자와 효율적 운영이 인상적입니다. 추가 성장 동력 확보에 집중하세요."
            profitRate > 0 -> "📈 긍정적 추세를 보이고 있습니다. 수익성 개선을 위해 수입 증대와 비용 절감 방안을 병행 추진하세요."
            profitRate > -10 -> "⚠️ 적자 상황이지만 개선 가능합니다. 우선 핵심 지출만 유지하고 수입원 다각화에 집중하세요."
            else -> "🔴 재정 개선이 시급합니다. 비상 계획을 수립하고 전면적인 구조 조정을 검토하세요."
        }
    }
    
    private fun getEventTypeAnalysis(typeName: String, income: Int, expense: Int, net: Int): String {
        val roi = if (expense > 0) ((net.toDouble() / expense.toDouble()) * 100).roundToInt() else 0
        
        return when {
            roi > 50 -> "💎 매우 수익성이 높은 이벤트입니다. 유사한 이벤트를 더 기획하세요."
            roi > 0 -> "✅ 수익성이 양호한 이벤트입니다. 효율성을 더 높일 방법을 찾아보세요."
            roi > -20 -> "📊 손익분기점에 근접했습니다. 비용 절감이나 참가비 조정을 고려하세요."
            else -> "⚠️ 수익성이 낮습니다. 이벤트 형태 재검토가 필요합니다."
        }
    }
    
    private fun getEventStrategyInsight(reportData: ApiService.YearlyReportResponse): String {
        val totalIncome = reportData.summary["income"] ?: 0
        val totalExpense = reportData.summary["expense"] ?: 0
        val monthCount = reportData.by_month.size
        val typeCount = reportData.by_type.size
        
        val avgMonthlyNet = if (monthCount > 0) (totalIncome - totalExpense) / monthCount else 0
        val diversityScore = typeCount * 10 + monthCount * 5
        
        return when {
            avgMonthlyNet > 100000 && diversityScore > 50 -> 
                "🏆 다양하고 수익성 높은 이벤트 포트폴리오를 구축했습니다. 성공 요인을 분석하여 표준화하고, 규모 확장을 통해 더 큰 성과를 노려보세요."
            avgMonthlyNet > 0 && diversityScore > 30 -> 
                "✨ 안정적인 이벤트 운영을 하고 있습니다. 특히 수익성이 좋은 이벤트에 더 많은 자원을 투입하여 성과를 극대화하세요."
            avgMonthlyNet > 0 -> 
                "📈 이벤트가 전반적으로 성공적입니다. 다양성을 높여 리스크를 분산하고 새로운 수입원을 개발해보세요."
            diversityScore > 25 -> 
                "🎯 이벤트 종류가 다양하지만 수익성이 아쉽습니다. 각 이벤트의 ROI를 분석하여 수익성 높은 것들에 집중하세요."
            else -> 
                "⚡ 이벤트 전략의 전면적인 재검토가 필요합니다. 소규모로 다양한 이벤트를 시험해보고 성공 모델을 찾아보세요."
        }
    }
    
    // 기존 헬퍼 함수들 유지
    
    // 추가 헬퍼 함수들 (길어서 일부만 포함)
    private fun getEventCategoryEmoji(eventName: String): String = when {
        eventName.contains("워크샵", true) || eventName.contains("세미나", true) -> "🎓"
        eventName.contains("정기", true) || eventName.contains("모임", true) -> "👥"
        eventName.contains("행사", true) || eventName.contains("축제", true) -> "🎉"
        eventName.contains("공연", true) || eventName.contains("발표", true) -> "🎭"
        else -> "📅"
    }
    
    private fun getEventGrade(roi: Int, efficiency: Int): String = when {
        roi > 100 && efficiency > 85 -> "S+ 최상급"
        roi > 50 && efficiency > 75 -> "A+ 우수"
        roi > 0 && efficiency > 60 -> "B+ 양호"
        roi > -25 && efficiency > 50 -> "C 보통"
        else -> "D 개선필요"
    }
    
    private fun getAdvancedTypeAnalysis(type: String, income: Int, expense: Int, net: Int): String {
        val efficiency = if (expense > 0) (income * 100) / expense else 100
        val category = when {
            type.contains("교육", true) || type.contains("세미나", true) -> "📚 교육투자형"
            type.contains("운영", true) || type.contains("관리", true) -> "⚙️ 운영관리형"
            type.contains("행사", true) || type.contains("이벤트", true) -> "🎉 행사기획형"
            type.contains("장비", true) || type.contains("구매", true) -> "💻 장비투자형"
            else -> "📊 일반운영형"
        }
        
        return when {
            net > 0 && efficiency > 120 -> "$category 초우수 (흑자+고효율)"
            net > 0 -> "$category 우수 (흑자달성)"
            efficiency > 80 -> "$category 양호 (효율적운영)"
            efficiency > 50 -> "$category 보통 (개선여지)"
            else -> "$category 점검필요 (비효율)"
        }
    }
    
    private fun getAdvancedNetEmoji(net: Int): String = when {
        net > 500000 -> "🟢💰"
        net > 100000 -> "🟢"
        net > 0 -> "🔵"
        net > -100000 -> "🟡"
        net > -500000 -> "🟠"
        else -> "🔴⚠️"
    }
    
    private fun getAdvancedPaymentAnalysis(method: String, income: Int, expense: Int, net: Int): String {
        val efficiency = if (expense > 0) (income * 100) / expense else 100
        val methodType = when {
            method.contains("카드", true) -> "💳 카드결제"
            method.contains("현금", true) -> "💵 현금결제"
            method.contains("계좌", true) -> "🏦 계좌이체"
            method.contains("온라인", true) -> "💻 온라인결제"
            else -> "📱 기타결제"
        }
        
        return when {
            efficiency > 100 && net > 0 -> "$methodType 최적화완료"
            efficiency > 80 -> "$methodType 효율적운영"
            efficiency > 60 -> "$methodType 표준수준"
            efficiency > 40 -> "$methodType 개선권장"
            else -> "$methodType 검토필요"
        }
    }
    
    private fun getAdvancedEventAnalysis(eventName: String, income: Int, expense: Int, net: Int): String {
        val roi = if (expense > 0) ((net * 100) / expense) else 0
        val eventType = when {
            eventName.contains("정기", true) -> "🔄 정기행사"
            eventName.contains("특별", true) || eventName.contains("특별", true) -> "✨ 특별행사"
            eventName.contains("교육", true) -> "📚 교육행사"
            eventName.contains("축제", true) -> "🎪 축제행사"
            else -> "📅 일반행사"
        }
        
        return when {
            roi > 50 -> "$eventType 대성공 (ROI ${roi}%)"
            roi > 0 -> "$eventType 성공적 (ROI ${roi}%)"
            roi > -25 -> "$eventType 무난함 (ROI ${roi}%)"
            roi > -50 -> "$eventType 아쉬움 (ROI ${roi}%)"
            else -> "$eventType 재검토필요 (ROI ${roi}%)"
        }
    }
    
    private fun getEventRatioEmoji(income: Int, expense: Int): String {
        val ratio = if (expense > 0) (income * 100) / expense else 100
        return when {
            ratio > 150 -> "🌟💰"
            ratio > 120 -> "✨💚"
            ratio > 100 -> "✅📈"
            ratio > 80 -> "⚡📊"
            ratio > 60 -> "⚠️📉"
            else -> "🚨💸"
        }
    }
    
    private fun generateAdvancedForecast(reportData: ApiService.AIReportResponse): String {
        val totalIncome = reportData.summary["total_income"] as? Int ?: 0
        val totalExpense = reportData.summary["total_expense"] as? Int ?: 0
        val trend = if (totalIncome > totalExpense) "상승" else "하향"
        
        return """
        📈 Hey-Bi AI 재무 예측 분석
        
        ▪ 현재 재무 트렌드: $trend 추세
        ▪ 예상 다음 분기 수익: ${formatPerfectAmount((totalIncome * 1.1).toInt())}
        ▪ 권장 예산 배분: 교육 30%, 운영 50%, 행사 20%
        ▪ 리스크 관리: ${if (totalExpense > totalIncome * 0.8) "고위험" else "안정적"}
        """.trimIndent()
    }
    
    private fun getComparisonEmoji(our: Int, similar: Int): String = when {
        our > similar * 1.2 -> "🏆🌟"
        our > similar * 1.1 -> "🥇✨"
        our > similar -> "🥈📈"
        our > similar * 0.9 -> "🥉📊"
        our > similar * 0.8 -> "⚠️📉"
        else -> "🚨💡"
    }
    
    private fun getRelativePerformance(our: Int, similar: Int): String {
        val ratio = if (similar > 0) (our * 100) / similar else 100
        return when {
            ratio > 120 -> "우수한 성과 (${ratio}%)"
            ratio > 110 -> "평균 이상 (${ratio}%)"
            ratio > 90 -> "평균 수준 (${ratio}%)"
            ratio > 80 -> "개선 필요 (${ratio}%)"
            else -> "집중 관리 필요 (${ratio}%)"
        }
    }
    
    // YearlyReportResponse용 오버로드
    private fun generateBenchmarkingStrategy(reportData: ApiService.YearlyReportResponse): String {
        return """
        🎯 벤치마킹 전략 로드맵
        
        1. 💪 강점 유지 전략
           ▪ 현재 우수한 영역의 노하우 문서화
           ▪ 성공 사례 타 동아리 공유 프로그램
        
        2. ⚡ 약점 개선 계획
           ▪ 상위 동아리 성공 사례 벤치마킹
           ▪ 단계별 개선 목표 설정 및 실행
        
        3. 📈 지속적 성장 전략
           ▪ 월별 성과 모니터링 시스템 구축
           ▪ 정기적인 경쟁력 진단 및 개선
        """.trimIndent()
    }
    
    // 기존 AIReportResponse용 (호환성 유지)
    private fun generateBenchmarkingStrategy(reportData: ApiService.AIReportResponse): String {
        return """
        🎯 벤치마킹 전략 로드맵
        
        1. 💪 강점 유지 전략
           ▪ 현재 우수한 영역의 노하우 문서화
           ▪ 성공 사례 타 동아리 공유 프로그램
        
        2. ⚡ 약점 개선 계획
           ▪ 상위 동아리 성공 사례 벤치마킹
           ▪ 단계별 개선 목표 설정 및 실행
        
        3. 🚀 혁신 기회 발굴
           ▪ 새로운 수익 모델 탐색
           ▪ 디지털 전환을 통한 효율성 증대
        """.trimIndent()
    }
    
    // YearlyReportResponse용 오버로드
    private fun generateCompetitivenessRoadmap(reportData: ApiService.YearlyReportResponse): String {
        val totalIncome = reportData.summary["income"] ?: 0
        val grade = when {
            totalIncome > 5000000 -> "A+ 최상급"
            totalIncome > 3000000 -> "A 상급"
            totalIncome > 1000000 -> "B+ 중상급"
            else -> "B 표준급"
        }
        
        return """
        🏆 경쟁력 강화 로드맵 ($grade)
        
        📊 현재 포지션: $grade 동아리
        
        🚀 단계별 성장 전략:
        1. 🎯 단기 목표 (3개월)
           ▪ 수익성 개선을 위한 핵심 영역 집중
           ▪ 비용 구조 최적화 실행
        
        2. 📈 중기 목표 (6개월)
           ▪ 안정적 수익원 다각화
           ▪ 운영 효율성 극대화
        
        3. 🏆 장기 목표 (1년)
           ▪ 최상위권 동아리로 도약
           ▪ 지속가능한 성장 모델 구축
        """.trimIndent()
    }
    
    // 기존 AIReportResponse용 (호환성 유지)
    private fun generateCompetitivenessRoadmap(reportData: ApiService.AIReportResponse): String {
        val totalIncome = reportData.summary["total_income"] as? Int ?: 0
        val grade = when {
            totalIncome > 5000000 -> "A+ 최상급"
            totalIncome > 3000000 -> "A 상급"
            totalIncome > 1000000 -> "B+ 중상급"
            else -> "B 표준급"
        }
        
        return """
        🏆 경쟁력 강화 로드맵 ($grade)
        
        📊 현재 포지션: $grade 동아리
        🎯 목표 설정: 상위 10% 진입
        
        ✨ 액션 플랜:
        1. 수익 다각화 (3개월 내)
        2. 운영비 효율화 (1개월 내)
        3. 회원 만족도 향상 (지속적)
        4. 브랜드 가치 제고 (6개월 내)
        """.trimIndent()
    }
    
    private fun getEfficiencyEmoji(efficiency: Int): String = when {
        efficiency > 90 -> "⚡🌟"
        efficiency > 80 -> "⚡✨"
        efficiency > 70 -> "✅📈"
        efficiency > 60 -> "📊⚠️"
        efficiency > 50 -> "📉⚠️"
        else -> "🚨💡"
    }
    
    private fun getAdvancedEventStrategy(eventName: String, roi: Int): String {
        val priority = when {
            roi > 50 -> "🌟 최우선 확대"
            roi > 0 -> "✅ 지속 추진"
            roi > -25 -> "⚠️ 개선 후 유지"
            else -> "🚨 재검토 필요"
        }
        
        return "$priority - ROI 기반 전략적 접근"
    }
    
    private fun getOverallEventGrade(totalEvents: Int, avgROI: Int): String = when {
        avgROI > 50 && totalEvents >= 5 -> "S급 이벤트 운영"
        avgROI > 30 && totalEvents >= 3 -> "A급 이벤트 운영"  
        avgROI > 0 && totalEvents >= 2 -> "B급 이벤트 운영"
        avgROI > -20 -> "C급 개선 필요"
        else -> "D급 전면 재검토"
    }
    
    private fun generateEventOptimizationPlan(events: List<Map<String, Any>>): String {
        val totalEvents = events.size
        val avgIncome = events.mapNotNull { (it["income"] as? Number)?.toInt() }.average().toInt()
        val avgExpense = events.mapNotNull { (it["expense"] as? Number)?.toInt() }.average().toInt()
        
        return """
        🎯 이벤트 최적화 마스터플랜
        
        📈 현황 분석:
        ▪ 총 이벤트: ${totalEvents}개
        ▪ 평균 수익: ${formatPerfectAmount(avgIncome)}
        ▪ 평균 비용: ${formatPerfectAmount(avgExpense)}
        
        💡 최적화 전략:
        1. 고수익 이벤트 확대 (ROI >50%)
        2. 저효율 이벤트 개선 (ROI <0%)
        3. 신규 이벤트 기획 (트렌드 반영)
        """.trimIndent()
    }
    
    private fun generateNextYearEventStrategy(events: List<Map<String, Any>>): String {
        val successfulEvents = events.filter { 
            val income = (it["income"] as? Number)?.toInt() ?: 0
            val expense = (it["expense"] as? Number)?.toInt() ?: 0
            income > expense
        }
        
        return """
        🚀 내년도 이벤트 전략 로드맵
        
        🎯 핵심 전략:
        ▪ 성공 이벤트 ${successfulEvents.size}개 확대 재실시
        ▪ 새로운 수익 창출 이벤트 3개 이상 기획
        ▪ 디지털 마케팅 활용한 참여율 20% 증대
        ▪ 협업 이벤트를 통한 비용 절감 및 시너지 창출
        
        📊 목표 KPI:
        ▪ 전체 이벤트 ROI 30% 이상
        ▪ 참가자 만족도 4.5/5.0 이상
        ▪ 연간 이벤트 수익 전년 대비 25% 증가
        """.trimIndent()
    }
}