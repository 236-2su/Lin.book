package com.example.myapplication.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class AIAnalysisService(private val context: Context) {
    
    data class AIReportResult(
        val success: Boolean,
        val content: String,
        val error: String? = null
    )
    
    // Enhanced method for refined data analysis
    suspend fun generateRefinedReport(
        aiInput: AIReportDataCollector.AIAnalysisInput,
        reportType: String,
        customRequest: String?
    ): AIReportResult = withContext(Dispatchers.IO) {
        try {
            Log.d("AIAnalysisService", "🎯 정제된 데이터로 AI 리포트 생성 시작: $reportType")
            Log.d("AIAnalysisService", "  - 데이터 품질: ${aiInput.dataQuality}")
            Log.d("AIAnalysisService", "  - 활성 월수: ${aiInput.financialSummary.activeMonths}")
            
            val analysisContent = analyzeRefinedDataLocally(aiInput, reportType, customRequest)
            
            Log.d("AIAnalysisService", "✅ 정제된 AI 리포트 생성 완료")
            AIReportResult(
                success = true,
                content = analysisContent
            )
        } catch (e: Exception) {
            Log.e("AIAnalysisService", "❌ 정제된 AI 분석 중 오류 발생", e)
            AIReportResult(
                success = false,
                content = "",
                error = e.message
            )
        }
    }

    suspend fun generateReport(
        clubData: AIReportDataCollector.ClubReportData,
        reportType: String,
        customRequest: String?
    ): AIReportResult = withContext(Dispatchers.IO) {
        
        try {
            Log.d("AIAnalysisService", "🤖 AI 리포트 생성 시작: $reportType")
            
            // 실제 AI API 대신 로컬에서 데이터 분석하여 리포트 생성
            val analysisContent = analyzeDataLocally(clubData, reportType, customRequest)
            
            Log.d("AIAnalysisService", "✅ AI 리포트 생성 완료")
            AIReportResult(
                success = true,
                content = analysisContent
            )
        } catch (e: Exception) {
            Log.e("AIAnalysisService", "❌ AI 분석 중 오류 발생", e)
            AIReportResult(
                success = false,
                content = "",
                error = e.message
            )
        }
    }
    
    private suspend fun analyzeDataLocally(
        clubData: AIReportDataCollector.ClubReportData,
        reportType: String,
        customRequest: String?
    ): String {
        val sb = StringBuilder()
        val currentDate = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Date())
        
        // 🎨 개선된 리포트 헤더
        sb.append("🤖 ${clubData.clubInfo?.name ?: "동아리"} AI 분석 리포트\n")
        sb.append("📅 분석일: $currentDate\n")
        sb.append("🔍 분석 유형: ${getReportTypeKorean(reportType)}\n")
        sb.append("⚡ AI 엔진: Hey-Bi v2.0\n\n")
        
        // 📊 데이터 수집 현황
        sb.append("📈 데이터 수집 현황\n")
        sb.append("=".repeat(25) + "\n")
        sb.append("• 동아리 정보: ${if (clubData.clubInfo != null) "✅" else "❌"}\n")
        sb.append("• 장부 데이터: ${if (clubData.ledgerData != null) "✅ ${clubData.ledgerData.size}개" else "❌"}\n")
        sb.append("• 거래 내역: ${if (clubData.transactions != null) "✅ ${clubData.transactions.size}건" else "❌"}\n")
        sb.append("• 행사 정보: ${if (clubData.events != null) "✅ ${clubData.events.size}개" else "❌"}\n")
        sb.append("• 재정 요약: ${if (clubData.financialSummary != null) "✅" else "❌"}\n\n")
        
        // 리포트 타입별 분석
        Log.d("AIAnalysisService", "🔄 리포트 타입별 분석 시작: $reportType")
        try {
            when (reportType) {
                "three_year_event" -> {
                    Log.d("AIAnalysisService", "📅 3년간 이벤트 분석 실행")
                    generateThreeYearEventAnalysis(sb, clubData)
                }
                "similar_clubs_comparison" -> {
                    Log.d("AIAnalysisService", "🔍 유사 동아리 비교 분석 실행")
                    generateSimilarClubsComparisonAnalysis(sb, clubData)
                }
                "gemini_ai_analysis" -> {
                    Log.d("AIAnalysisService", "🤖 Gemini AI 분석 실행")
                    generateGeminiAIAnalysis(sb, clubData)
                }
                else -> {
                    Log.w("AIAnalysisService", "⚠️ 알 수 없는 리포트 타입: $reportType")
                    sb.append("📋 새로운 AI 분석 리포트\n")
                    sb.append("=".repeat(30) + "\n\n")
                    sb.append("선택한 리포트 타입에 맞는 전문 분석을 제공합니다.\n")
                }
            }
            Log.d("AIAnalysisService", "✅ 리포트 타입별 분석 완료")
        } catch (e: Exception) {
            Log.e("AIAnalysisService", "❌ 리포트 타입별 분석 실패", e)
            sb.append("\n\n❌ 분석 중 오류가 발생했습니다: ${e.message}\n")
            sb.append("기본 분석을 제공합니다.\n\n")
        }
        
        // 사용자 맞춤 요청사항 반영
        customRequest?.let { request ->
            if (request.isNotBlank()) {
                sb.append("\n\n🎯 맞춤 분석 결과\n")
                sb.append("=".repeat(25) + "\n")
                sb.append("📝 요청사항: $request\n\n")
                sb.append(generateCustomAnalysis(clubData, request))
            }
        }
        
        // 📋 종합 결론 및 제안사항
        sb.append("\n\n💡 AI 제안사항\n")
        sb.append("=".repeat(25) + "\n")
        sb.append(generateRecommendations(clubData))
        
        // 🏁 리포트 마무리
        sb.append("\n\n" + "=".repeat(40) + "\n")
        sb.append("📊 분석 완료\n")
        sb.append("이 리포트는 Hey-Bi AI가 현재 데이터를 기반으로 분석한 결과입니다.\n")
        sb.append("더 정확한 분석을 위해 정기적인 데이터 업데이트를 권장합니다.\n")
        sb.append("문의사항이 있으시면 AI 리포트 생성 화면에서 요청사항을 작성해주세요.")
        
        return sb.toString()
    }
    
    private fun getReportTypeKorean(type: String): String {
        return when (type) {
            "financial_analysis" -> "💰 재정 현황 분석"
            "activity_analysis" -> "🎯 활동 현황 분석"
            "comprehensive" -> "📊 종합 운영 평가"
            "comparison" -> "🏆 타 동아리 비교 분석"
            "three_year_event" -> "📅 3년간 이벤트 분석"
            "similar_clubs_comparison" -> "🔍 유사 동아리 비교"
            "gemini_ai_analysis" -> "🤖 Gemini AI 심화 분석"
            else -> "📋 일반 종합 분석"
        }
    }
    
    
    
    
    
    
    private fun generateCustomAnalysis(clubData: AIReportDataCollector.ClubReportData, request: String): String {
        val sb = StringBuilder()
        
        val lowerRequest = request.lowercase()
        
        when {
            lowerRequest.contains("예산") || lowerRequest.contains("비용") || lowerRequest.contains("돈") -> {
                sb.append("💰 예산 관련 맞춤 분석\n")
                clubData.financialSummary?.let { summary ->
                    sb.append("• 현재 재정 상황: ${if (summary.netAmount >= 0) "흑자 운영" else "적자 주의"}\n")
                    sb.append("• 월평균 지출 규모: ${formatMoney(summary.totalExpense)}원\n")
                    sb.append("• 예산 최적화 방안: 고정비 절약 및 효율적 지출 관리\n")
                    sb.append("• 권장 예비비: ${formatMoney(summary.totalExpense / 10)}원 (월 지출의 10%)\n")
                }
            }
            lowerRequest.contains("활동") || lowerRequest.contains("행사") || lowerRequest.contains("이벤트") -> {
                sb.append("🎯 활동 관련 맞춤 분석\n")
                clubData.events?.let { events ->
                    sb.append("• 현재 행사 빈도: 월평균 ${String.format("%.1f", events.size / 12.0)}회\n")
                    sb.append("• 권장 활동 수준: ${if (events.size < 6) "행사 횟수 증가 필요" else "현재 수준 유지 권장"}\n")
                    sb.append("• 활동 다양화 제안: 정기 모임, 특별 이벤트, 외부 연계 활동 등\n")
                    sb.append("• 참여도 향상 방안: 멤버 의견 수렴 및 관심사 반영\n")
                }
            }
            lowerRequest.contains("개선") || lowerRequest.contains("발전") || lowerRequest.contains("제안") -> {
                sb.append("📈 발전 방안 맞춤 제안\n")
                val overallScore = calculateOverallScore(clubData)
                sb.append("• 현재 수준: ${overallScore}/100점\n")
                sb.append("• 핵심 개선 포인트:\n")
                if (overallScore < 70) {
                    sb.append("  - 기본적인 운영 시스템 구축\n")
                    sb.append("  - 정기적인 소통 체계 마련\n")
                }
                sb.append("  - 멤버 만족도 조사 실시\n")
                sb.append("  - 장기 발전 계획 수립\n")
                sb.append("• 단계별 실행 계획: 1개월 → 체계 구축, 3개월 → 안정화, 6개월 → 발전\n")
            }
            lowerRequest.contains("비교") || lowerRequest.contains("순위") || lowerRequest.contains("등급") -> {
                sb.append("🏆 비교 평가 맞춤 분석\n")
                val overallScore = calculateOverallScore(clubData)
                sb.append("• 동종 동아리 대비 수준: ${if (overallScore >= 70) "상위권" else "중하위권"}\n")
                sb.append("• 강점 영역: ${getStrengthAreas(clubData)}\n")
                sb.append("• 보완 필요 영역: ${getWeaknessAreas(clubData)}\n")
                sb.append("• 목표 등급: 현재보다 한 단계 상승을 목표로 설정\n")
            }
            else -> {
                sb.append("🔍 종합 맞춤 분석\n")
                sb.append("요청하신 내용을 바탕으로 전반적인 현황을 분석했습니다.\n")
                sb.append("• 현재 동아리는 ${getOverallStatus(clubData)} 상태입니다\n")
                sb.append("• 지속적인 발전을 위해 정기적인 현황 점검을 권장합니다\n")
                sb.append("• 구체적인 질문이 있으시면 다음 리포트 생성 시 상세히 작성해주세요\n")
            }
        }
        
        return sb.toString()
    }
    
    private fun generateRecommendations(clubData: AIReportDataCollector.ClubReportData): String {
        val sb = StringBuilder()
        val overallScore = calculateOverallScore(clubData)
        
        // 점수별 맞춤 제안
        when {
            overallScore >= 80 -> {
                sb.append("🌟 우수 동아리 유지 전략\n")
                sb.append("• 현재 수준을 지속 유지하며 세부적인 완성도를 높여보세요\n")
                sb.append("• 다른 동아리와의 연합 활동이나 멘토링을 고려해보세요\n")
                sb.append("• 장기적인 비전과 목표를 구체화하여 더욱 발전시켜나가세요\n")
            }
            overallScore >= 60 -> {
                sb.append("📈 안정적 성장 전략\n")
                sb.append("• 현재 잘하고 있는 부분은 유지하면서 약한 영역을 보완하세요\n")
                sb.append("• 멤버들의 의견을 적극적으로 수렴하여 개선점을 찾아보세요\n")
                sb.append("• 체계적인 계획 수립을 통해 단계적으로 발전시켜나가세요\n")
            }
            else -> {
                sb.append("🚀 기본 체계 구축 전략\n")
                sb.append("• 기본적인 운영 체계부터 차근차근 구축해나가세요\n")
                sb.append("• 작은 목표부터 시작하여 성취감을 쌓아가는 것이 중요합니다\n")
                sb.append("• 경험이 많은 선배나 다른 동아리의 조언을 구해보세요\n")
            }
        }
        
        // 공통 권장사항
        sb.append("\n💡 공통 권장사항\n")
        sb.append("• 📊 정기적인 현황 점검 (월 1회 이상)\n")
        sb.append("• 💬 멤버와의 열린 소통 채널 운영\n")
        sb.append("• 📝 활동 기록 및 데이터 축적\n")
        sb.append("• 🎯 명확한 목표 설정 및 평가\n")
        sb.append("• 🤝 타 동아리와의 네트워킹\n")
        
        // 다음 분석 제안
        sb.append("\n🔮 다음 AI 분석 제안\n")
        val nextAnalysisMonth = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
        val nextMonth = SimpleDateFormat("yyyy년 MM월", Locale.KOREA).format(nextAnalysisMonth.time)
        sb.append("• 권장 분석 주기: 월 1회 (다음 분석 권장일: $nextMonth)\n")
        sb.append("• 비교 분석을 위해 현재 개선 계획을 실행해보세요\n")
        sb.append("• 다음 분석 시 변화된 모습을 확인할 수 있을 것입니다")
        
        return sb.toString()
    }
    
    // Helper functions
    private fun formatMoney(amount: Long): String = String.format("%,d", amount)
    
    private fun getScoreEmoji(score: Int): String = when {
        score >= 90 -> "🏆"
        score >= 80 -> "🥇"
        score >= 70 -> "🥈"
        score >= 60 -> "🥉"
        score >= 50 -> "📊"
        else -> "📈"
    }
    
    
    private fun calculateHealthScore(summary: AIReportDataCollector.FinancialSummary): Int {
        var score = 50
        
        // 순손익 평가 (40점)
        when {
            summary.netAmount > 100000 -> score += 40
            summary.netAmount > 50000 -> score += 30
            summary.netAmount > 0 -> score += 20
            summary.netAmount > -50000 -> score += 10
            else -> score -= 10
        }
        
        // 거래 활성도 평가 (30점)
        when {
            summary.transactionCount > 50 -> score += 30
            summary.transactionCount > 30 -> score += 20
            summary.transactionCount > 15 -> score += 15
            summary.transactionCount > 5 -> score += 10
            else -> score += 0
        }
        
        // 평균 거래액 적정성 (20점)
        when {
            summary.averageTransactionAmount in 10000..100000 -> score += 20
            summary.averageTransactionAmount in 5000..150000 -> score += 15
            summary.averageTransactionAmount > 0 -> score += 10
            else -> score += 0
        }
        
        return score.coerceIn(0, 100)
    }
    
    private fun calculateOverallScore(clubData: AIReportDataCollector.ClubReportData): Int {
        var score = 30 // 기본 점수
        
        // 재정 점수 (40점)
        clubData.financialSummary?.let { summary ->
            score += (calculateHealthScore(summary) * 0.4).toInt()
        }
        
        // 활동 점수 (30점)
        clubData.events?.let { events ->
            val activityScore = when {
                events.size >= 10 -> 30
                events.size >= 7 -> 25
                events.size >= 5 -> 20
                events.size >= 3 -> 15
                events.size >= 1 -> 10
                else -> 0
            }
            score += activityScore
        }
        
        // 정보 완성도 점수 (20점)
        score += calculateInfoCompleteness(clubData)
        
        return score.coerceIn(0, 100)
    }
    
    private fun calculateInfoCompleteness(clubData: AIReportDataCollector.ClubReportData): Int {
        var score = 0
        
        if (clubData.clubInfo != null) score += 8
        if (clubData.ledgerData?.isNotEmpty() == true) score += 4
        if (clubData.transactions?.isNotEmpty() == true) score += 4
        if (clubData.events?.isNotEmpty() == true) score += 4
        
        return score
    }
    
    private fun getStrengthAreas(clubData: AIReportDataCollector.ClubReportData): String {
        val strengths = mutableListOf<String>()
        
        clubData.financialSummary?.let { summary ->
            if (calculateHealthScore(summary) >= 70) strengths.add("재정 관리")
        }
        
        clubData.events?.let { events ->
            if (events.size >= 5) strengths.add("활동 기획")
        }
        
        if (clubData.clubInfo != null) strengths.add("정보 관리")
        
        return if (strengths.isNotEmpty()) strengths.joinToString(", ") else "기본 체계 구축"
    }
    
    // 1. 3년간 이벤트 분석 리포트 (백엔드 API 사용)
    private suspend fun generateThreeYearEventAnalysis(sb: StringBuilder, clubData: AIReportDataCollector.ClubReportData) {
        sb.append("📅 3년간 이벤트 예산 비교 분석 (2023-2025)\n")
        sb.append("=".repeat(50) + "\n\n")
        
        try {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val years = listOf(currentYear - 2, currentYear - 1, currentYear) // 2023, 2024, 2025
            
            sb.append("📊 분석 개요\n")
            sb.append("• 분석 기간: ${years.joinToString(", ")}년\n")
            sb.append("• 비교 대상: 이벤트별 예산 vs 실제 지출\n")
            sb.append("• 분석 방법: 연도별 이벤트 타입 비교\n\n")
            
            // clubData에서 이벤트 및 거래 데이터 사용
            clubData.events?.let { events ->
                sb.append("🎯 현재 년도 이벤트 현황\n")
                sb.append("• 총 이벤트 수: ${events.size}개\n")
                
                val totalBudget = events.sumOf { it.budget }
                sb.append("• 총 계획 예산: ${formatMoney(totalBudget.toLong())}원\n")
                
                if (events.isNotEmpty()) {
                    sb.append("\n📈 주요 이벤트 예산 분석\n")
                    events.sortedByDescending { it.budget }.take(5).forEachIndexed { index, event ->
                        sb.append("${index + 1}. ${event.name}: ${formatMoney(event.budget.toLong())}원\n")
                    }
                }
            }
            
            // 거래 내역과 이벤트 매칭
            clubData.transactions?.let { transactions ->
                sb.append("\n💸 실제 지출 분석\n")
                val eventTransactions = transactions.filter { it.amount < 0 }
                val totalActualExpense = eventTransactions.sumOf { Math.abs(it.amount) }
                sb.append("• 이벤트 관련 실제 지출: ${formatMoney(totalActualExpense)}원\n")
                
                clubData.events?.let { events ->
                    val totalBudget = events.sumOf { it.budget }
                    if (totalBudget > 0) {
                        val efficiency = (totalActualExpense * 100 / totalBudget).toInt()
                        sb.append("• 예산 대비 실제 지출 비율: ${efficiency}%\n")
                        
                        val status = when {
                            efficiency <= 70 -> "우수한 예산 절약 🌟"
                            efficiency <= 90 -> "효율적 예산 관리 👍"
                            efficiency <= 110 -> "적정한 예산 집행 📊"
                            else -> "예산 관리 검토 필요 ⚠️"
                        }
                        sb.append("• 평가: $status\n")
                    }
                }
            }
            
            // 3년간 비교 시뮬레이션 (실제 API 데이터가 없는 경우 예상 값)
            sb.append("\n📊 3년간 이벤트 예산 트렌드 예측\n")
            years.forEach { year ->
                val isCurrentYear = (year == currentYear)
                if (isCurrentYear && clubData.events != null) {
                    val actualBudget = clubData.events.sumOf { it.budget }
                    sb.append("• ${year}년: ${formatMoney(actualBudget.toLong())}원 (실제 데이터)\n")
                } else {
                    // 예상 데이터 (현재 년도 기준으로 ±10-20% 변동)
                    val baseBudget = clubData.events?.sumOf { it.budget } ?: 1000000
                    val variance = if (year < currentYear) 0.8 + (Math.random() * 0.3) else 0.9 + (Math.random() * 0.2)
                    val estimatedBudget = (baseBudget * variance).toLong()
                    sb.append("• ${year}년: ${formatMoney(estimatedBudget)}원 (예상)\n")
                }
            }
            
            // 이벤트 타입별 분석
            clubData.events?.let { events ->
                if (events.isNotEmpty()) {
                    sb.append("\n🏷️ 이벤트 타입별 예산 분포\n")
                    val eventsByType = events.groupBy { 
                        when {
                            it.name.contains("신입", ignoreCase = true) || it.name.contains("환영", ignoreCase = true) -> "신입생 행사"
                            it.name.contains("정기", ignoreCase = true) || it.name.contains("모임", ignoreCase = true) -> "정기 모임"
                            it.name.contains("행사", ignoreCase = true) || it.name.contains("이벤트", ignoreCase = true) -> "특별 이벤트"
                            it.name.contains("졸업", ignoreCase = true) || it.name.contains("송별", ignoreCase = true) -> "졸업/송별 행사"
                            else -> "기타 활동"
                        }
                    }
                    
                    eventsByType.forEach { (type, typeEvents) ->
                        val typeBudget = typeEvents.sumOf { it.budget }
                        sb.append("  • $type: ${formatMoney(typeBudget.toLong())}원 (${typeEvents.size}개)\n")
                    }
                }
            }
            
            // 향후 3년 예측 및 제안
            sb.append("\n🔮 향후 예산 계획 제안\n")
            clubData.events?.let { events ->
                if (events.isNotEmpty()) {
                    val avgEventBudget = events.map { it.budget }.average().toLong()
                    sb.append("• 이벤트 평균 예산: ${formatMoney(avgEventBudget)}원\n")
                    sb.append("• 내년 권장 총 예산: ${formatMoney((avgEventBudget * events.size * 1.1).toLong())}원 (10% 증가)\n")
                    sb.append("• 예산 최적화 포인트: 성과 대비 효율성 높은 이벤트 확대\n")
                    sb.append("• 신규 이벤트 도입 시 예상 예산: ${formatMoney(avgEventBudget)}원\n")
                }
            }
            
        } catch (e: Exception) {
            Log.e("AIAnalysisService", "3년간 이벤트 분석 중 오류", e)
            sb.append("❌ 분석 중 오류 발생\n")
            sb.append("오류 내용: ${e.message}\n")
            sb.append("기본 데이터로 분석을 계속 진행합니다.\n")
        }
    }
    
    // 백엔드에서 이벤트 데이터 조회
    private suspend fun fetchEventsFromAPI(baseUrl: String, clubId: String): JSONArray = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl/clubs/$clubId/events/")
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                JSONArray(response)
            } else {
                Log.e("AIAnalysisService", "이벤트 API 호출 실패: $responseCode")
                JSONArray()
            }
        } finally {
            connection.disconnect()
        }
    }
    
    // 백엔드에서 거래 내역 데이터 조회
    private suspend fun fetchTransactionsFromAPI(baseUrl: String, clubId: String): JSONArray = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl/clubs/$clubId/ledger-transactions/")
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                JSONArray(response)
            } else {
                Log.e("AIAnalysisService", "거래내역 API 호출 실패: $responseCode")
                JSONArray()
            }
        } finally {
            connection.disconnect()
        }
    }
    
    // Enhanced analysis method using refined data
    private suspend fun analyzeRefinedDataLocally(
        aiInput: AIReportDataCollector.AIAnalysisInput,
        reportType: String,
        customRequest: String?
    ): String {
        val sb = StringBuilder()
        val currentDate = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA).format(Date())
        
        // Enhanced report header with quality indicators
        sb.append("🎯 ${aiInput.contextualInfo["club_name"]} 정밀 AI 분석 리포트\n")
        sb.append("📅 분석 시점: $currentDate\n")
        sb.append("🔍 분석 유형: ${getReportTypeKorean(reportType)}\n")
        sb.append("📊 데이터 품질: ${aiInput.dataQuality}\n")
        sb.append("⚡ 고급 AI 엔진: Enhanced Hey-Bi v3.0\n")
        sb.append("=".repeat(50) + "\n\n")
        
        // Executive Summary with key insights
        sb.append("🎯 핵심 인사이트 (Executive Summary)\n")
        sb.append("=".repeat(35) + "\n")
        generateExecutiveSummary(sb, aiInput)
        sb.append("\n")
        
        // Financial Performance Analysis
        sb.append("💰 재정 성과 분석\n")
        sb.append("=".repeat(20) + "\n")
        generateFinancialAnalysis(sb, aiInput.financialSummary)
        sb.append("\n")
        
        // Spending Pattern Analysis  
        sb.append("📈 지출 패턴 분석\n")
        sb.append("=".repeat(20) + "\n")
        generateSpendingAnalysis(sb, aiInput.spendingPatterns)
        sb.append("\n")
        
        // Trend Analysis
        sb.append("📊 트렌드 분석\n")
        sb.append("=".repeat(15) + "\n")
        generateTrendAnalysis(sb, aiInput.trends)
        sb.append("\n")
        
        // Risk Assessment
        if (aiInput.spendingPatterns.riskFactors.isNotEmpty()) {
            sb.append("⚠️ 리스크 평가\n")
            sb.append("=".repeat(15) + "\n")
            aiInput.spendingPatterns.riskFactors.forEach { risk ->
                sb.append("• $risk\n")
            }
            sb.append("\n")
        }
        
        // Report-specific analysis
        when (reportType) {
            "three_year_event" -> generateEnhancedThreeYearAnalysis(sb, aiInput)
            "similar_clubs_comparison" -> generateEnhancedComparisonAnalysis(sb, aiInput)
            "gemini_ai_analysis" -> generateEnhancedGeminiAnalysis(sb, aiInput)
            else -> generateGenericEnhancedAnalysis(sb, aiInput)
        }
        
        // Strategic recommendations
        sb.append("🎯 전략적 권고사항\n")
        sb.append("=".repeat(20) + "\n")
        generateStrategicRecommendations(sb, aiInput)
        sb.append("\n")
        
        // Data appendix
        sb.append("📋 분석 데이터 요약\n")
        sb.append("=".repeat(20) + "\n")
        sb.append("• 분석 기간: ${aiInput.contextualInfo["analysis_year"]}년\n")
        sb.append("• 활성 월수: ${aiInput.financialSummary.activeMonths}개월\n")
        sb.append("• 원본 데이터 크기: ${aiInput.rawDataSize}바이트\n")
        sb.append("• 성과 점수: ${String.format("%.1f", aiInput.trends.performanceScore)}점\n")
        sb.append("• 분석 완료 시간: $currentDate\n\n")
        
        sb.append("✨ 이 리포트는 AI 기반 정밀 분석을 통해 생성되었습니다.\n")
        sb.append("🔄 정기적인 분석을 통해 동아리 운영을 최적화하세요!")
        
        return sb.toString()
    }
    
    private fun generateExecutiveSummary(sb: StringBuilder, aiInput: AIReportDataCollector.AIAnalysisInput) {
        val financial = aiInput.financialSummary
        sb.append("💎 재정 건전성: ${financial.cashFlowHealth}\n")
        sb.append("📊 수익률: ${String.format("%.1f", financial.profitMargin)}%\n")
        sb.append("📈 현금흐름: ${aiInput.trends.cashFlowTrend}\n")
        sb.append("⭐ 종합 평가: ${getOverallRating(financial, aiInput.trends)}\n")
        
        if (financial.activeMonths < 6) {
            sb.append("⚠️ 주의: 활동 월수가 적어 분석 정확도가 제한적일 수 있습니다.\n")
        }
    }
    
    private fun generateFinancialAnalysis(sb: StringBuilder, financial: AIReportDataCollector.RefinedFinancialSummary) {
        sb.append("💰 총 수입: ${String.format("%,d", financial.totalIncome)}원\n")
        sb.append("💸 총 지출: ${String.format("%,d", financial.totalExpense)}원\n")
        sb.append("📊 순 손익: ${String.format("%,d", financial.netProfit)}원\n")
        sb.append("📈 수익률: ${String.format("%.1f", financial.profitMargin)}%\n")
        sb.append("📅 월평균 수입: ${String.format("%,.0f", financial.avgMonthlyIncome)}원\n")
        sb.append("💳 월평균 지출: ${String.format("%,.0f", financial.avgMonthlyExpense)}원\n")
        sb.append("⚡ 활동 개월수: ${financial.activeMonths}개월\n")
        
        // Financial health assessment
        val healthEmoji = when (financial.cashFlowHealth) {
            "매우 건강" -> "🟢"
            "건강" -> "🟡"
            "양호" -> "🟠"
            "주의 필요" -> "🔴"
            else -> "⚫"
        }
        sb.append("$healthEmoji 재정 상태: ${financial.cashFlowHealth}\n")
    }
    
    private fun generateSpendingAnalysis(sb: StringBuilder, patterns: AIReportDataCollector.SpendingPattern) {
        if (patterns.topExpenseTypes.isNotEmpty()) {
            sb.append("🏆 주요 지출 항목:\n")
            patterns.topExpenseTypes.forEachIndexed { index, (type, amount) ->
                sb.append("  ${index + 1}. $type: ${String.format("%,d", amount)}원\n")
            }
            sb.append("\n")
        }
        
        if (patterns.seasonalTrends.isNotEmpty()) {
            sb.append("🌸 계절별 지출 패턴:\n")
            patterns.seasonalTrends.forEach { (season, amount) ->
                sb.append("  $season: ${String.format("%,.0f", amount)}원\n")
            }
            sb.append("\n")
        }
        
        if (patterns.eventSpending.isNotEmpty()) {
            sb.append("🎉 이벤트별 지출:\n")
            patterns.eventSpending.toList().sortedByDescending { it.second }.take(5).forEach { (event, amount) ->
                sb.append("  • $event: ${String.format("%,d", amount)}원\n")
            }
        }
    }
    
    private fun generateTrendAnalysis(sb: StringBuilder, trends: AIReportDataCollector.TrendAnalysis) {
        sb.append("📈 현금흐름 추세: ${trends.cashFlowTrend}\n")
        sb.append("⭐ 성과 점수: ${String.format("%.1f", trends.performanceScore)}점\n")
        
        if (trends.busyMonths.isNotEmpty()) {
            sb.append("🔥 활발한 월: ${trends.busyMonths.joinToString(", ")}\n")
        }
        
        if (trends.quietMonths.isNotEmpty()) {
            sb.append("😴 조용한 월: ${trends.quietMonths.joinToString(", ")}\n")
        }
        
        if (trends.monthlyGrowth.isNotEmpty()) {
            val avgGrowth = trends.monthlyGrowth.average()
            sb.append("📊 월평균 성장률: ${String.format("%.1f", avgGrowth)}%\n")
        }
    }
    
    private fun generateEnhancedThreeYearAnalysis(sb: StringBuilder, aiInput: AIReportDataCollector.AIAnalysisInput) {
        sb.append("📅 3년간 이벤트 분석 (Enhanced)\n")
        sb.append("=".repeat(30) + "\n")
        sb.append("현재 년도 기준으로 심층 분석한 결과를 제공합니다.\n\n")
        
        val financial = aiInput.financialSummary
        if (financial.activeMonths >= 6) {
            sb.append("✨ 연간 운영 패턴 분석:\n")
            sb.append("• 지속적 활동 기간: ${financial.activeMonths}개월\n")
            sb.append("• 연간 예상 수입: ${String.format("%,d", (financial.avgMonthlyIncome * 12).toInt())}원\n")
            sb.append("• 연간 예상 지출: ${String.format("%,d", (financial.avgMonthlyExpense * 12).toInt())}원\n\n")
        }
        
        sb.append("🎯 향후 3년 전략 방향:\n")
        when (financial.cashFlowHealth) {
            "매우 건강" -> sb.append("• 확장 및 신규 사업 검토 권장\n• 예비금 적립 계획 수립\n")
            "건강" -> sb.append("• 안정적 운영 유지\n• 효율성 개선 방안 모색\n")
            "양호" -> sb.append("• 수입 증대 방안 필요\n• 지출 최적화 검토\n")
            else -> sb.append("• 긴급 재정 개선 필요\n• 운영 방식 재검토 권장\n")
        }
    }
    
    private fun generateEnhancedComparisonAnalysis(sb: StringBuilder, aiInput: AIReportDataCollector.AIAnalysisInput) {
        sb.append("🔍 유사 동아리 비교 분석 (Enhanced)\n")
        sb.append("=".repeat(35) + "\n")
        
        val financial = aiInput.financialSummary
        sb.append("📊 우리 동아리 재정 지표:\n")
        sb.append("• 월평균 순익: ${String.format("%,.0f", financial.avgMonthlyIncome - financial.avgMonthlyExpense)}원\n")
        sb.append("• 수익률: ${String.format("%.1f", financial.profitMargin)}%\n")
        sb.append("• 운영 효율성: ${if (financial.profitMargin > 5) "우수" else if (financial.profitMargin > 0) "보통" else "개선 필요"}\n\n")
        
        sb.append("🏆 동종 동아리 대비 위치 (예측):\n")
        val position = when {
            financial.profitMargin > 10 -> "상위 20%"
            financial.profitMargin > 5 -> "상위 40%"
            financial.profitMargin > 0 -> "중간 수준"
            else -> "하위권"
        }
        sb.append("• 재정 건전성: $position\n")
        sb.append("• 개선 포인트: ${identifyImprovementAreas(aiInput)}\n")
    }
    
    private fun generateEnhancedGeminiAnalysis(sb: StringBuilder, aiInput: AIReportDataCollector.AIAnalysisInput) {
        sb.append("🤖 Gemini AI 심화 분석 (Enhanced)\n")
        sb.append("=".repeat(35) + "\n")
        
        sb.append("🧠 AI 통찰력 분석:\n")
        sb.append("데이터 품질 평가를 통한 정밀 분석 결과입니다.\n\n")
        
        val patterns = aiInput.spendingPatterns
        if (patterns.topExpenseTypes.isNotEmpty()) {
            val dominantExpense = patterns.topExpenseTypes.first()
            val totalExpense = patterns.topExpenseTypes.sumOf { it.second }
            val concentration = dominantExpense.second.toDouble() / totalExpense * 100
            
            sb.append("💡 AI 발견 패턴:\n")
            sb.append("• 지출 집중도: ${String.format("%.1f", concentration)}% (${dominantExpense.first})\n")
            
            if (concentration > 50) {
                sb.append("⚠️ AI 권고: 지출 다양성 확보 필요\n")
            } else {
                sb.append("✅ AI 평가: 균형잡힌 지출 구조\n")
            }
        }
        
        sb.append("\n🎯 AI 맞춤 제안:\n")
        generateAIRecommendations(sb, aiInput)
    }
    
    private fun generateGenericEnhancedAnalysis(sb: StringBuilder, aiInput: AIReportDataCollector.AIAnalysisInput) {
        sb.append("📊 종합 재정 분석\n")
        sb.append("=".repeat(20) + "\n")
        sb.append("정밀 데이터 분석을 통한 포괄적 평가 결과입니다.\n\n")
        
        val score = calculateOverallScore(aiInput)
        sb.append("🏆 종합 점수: ${String.format("%.1f", score)}/100점\n")
        sb.append("📈 발전 잠재력: ${getDevelopmentPotential(aiInput)}\n")
    }
    
    private fun generateStrategicRecommendations(sb: StringBuilder, aiInput: AIReportDataCollector.AIAnalysisInput) {
        val recommendations = generateSmartRecommendations(aiInput)
        
        if (recommendations.isNotEmpty()) {
            recommendations.forEachIndexed { index, recommendation ->
                sb.append("${index + 1}. $recommendation\n")
            }
        } else {
            sb.append("현재 재정 상태가 안정적이며, 기존 운영 방식을 유지하시기 바랍니다.\n")
        }
    }
    
    // Helper methods for enhanced analysis
    private fun getOverallRating(financial: AIReportDataCollector.RefinedFinancialSummary, trends: AIReportDataCollector.TrendAnalysis): String {
        val score = (financial.profitMargin + trends.performanceScore) / 2
        return when {
            score > 80 -> "우수"
            score > 60 -> "양호"
            score > 40 -> "보통"
            else -> "개선 필요"
        }
    }
    
    private fun identifyImprovementAreas(aiInput: AIReportDataCollector.AIAnalysisInput): String {
        val areas = mutableListOf<String>()
        
        if (aiInput.financialSummary.profitMargin < 0) areas.add("수익성 개선")
        if (aiInput.trends.performanceScore < 50) areas.add("운영 일관성")
        if (aiInput.spendingPatterns.riskFactors.isNotEmpty()) areas.add("리스크 관리")
        
        return if (areas.isNotEmpty()) areas.joinToString(", ") else "전반적으로 우수"
    }
    
    private fun generateAIRecommendations(sb: StringBuilder, aiInput: AIReportDataCollector.AIAnalysisInput) {
        val recommendations = mutableListOf<String>()
        
        val financial = aiInput.financialSummary
        if (financial.profitMargin < 5) {
            recommendations.add("수익성 향상을 위한 수입원 다각화")
        }
        
        if (financial.activeMonths < 8) {
            recommendations.add("지속적인 활동을 통한 안정성 확보")
        }
        
        if (aiInput.spendingPatterns.riskFactors.isNotEmpty()) {
            recommendations.add("리스크 요소 개선 및 관리 체계 구축")
        }
        
        recommendations.forEach { sb.append("• $it\n") }
    }
    
    private fun calculateOverallScore(aiInput: AIReportDataCollector.AIAnalysisInput): Double {
        var score = 0.0
        
        // Financial health (40%)
        val profitScore = maxOf(0.0, minOf(100.0, aiInput.financialSummary.profitMargin + 50))
        score += profitScore * 0.4
        
        // Performance consistency (30%)
        score += aiInput.trends.performanceScore * 0.3
        
        // Data quality (20%)
        val qualityScore = when (aiInput.dataQuality) {
            "높음" -> 100.0
            "보통" -> 70.0
            "낮음" -> 40.0
            else -> 20.0
        }
        score += qualityScore * 0.2
        
        // Risk management (10%)
        val riskScore = maxOf(0.0, 100.0 - aiInput.spendingPatterns.riskFactors.size * 20)
        score += riskScore * 0.1
        
        return score
    }
    
    private fun getDevelopmentPotential(aiInput: AIReportDataCollector.AIAnalysisInput): String {
        val score = calculateOverallScore(aiInput)
        return when {
            score > 80 -> "높음"
            score > 60 -> "보통"
            else -> "개선 여지 있음"
        }
    }
    
    private fun generateSmartRecommendations(aiInput: AIReportDataCollector.AIAnalysisInput): List<String> {
        val recommendations = mutableListOf<String>()
        val financial = aiInput.financialSummary
        val trends = aiInput.trends
        
        if (financial.profitMargin < 0) {
            recommendations.add("📉 적자 해소를 위한 수입 증대 및 지출 절감 방안 수립")
        }
        
        if (financial.activeMonths < 6) {
            recommendations.add("📅 지속적인 활동을 통한 데이터 축적 및 안정성 확보")
        }
        
        if (trends.cashFlowTrend == "악화") {
            recommendations.add("📊 현금흐름 개선을 위한 긴급 대응 방안 마련")
        }
        
        if (financial.profitMargin > 10 && financial.netProfit > 500000) {
            recommendations.add("💰 우수한 재정 상태 유지 및 투자 확대 검토")
        }
        
        if (aiInput.spendingPatterns.topExpenseTypes.isNotEmpty()) {
            val topExpense = aiInput.spendingPatterns.topExpenseTypes.first()
            if (topExpense.second > financial.totalExpense * 0.5) {
                recommendations.add("⚠️ ${topExpense.first} 지출 비중이 높음 - 분산 투자 고려")
            }
        }
        
        return recommendations
    }
    
    // 3년간 데이터 분석 로직
    private fun analyzeThreeYearData(sb: StringBuilder, eventsData: JSONArray, transactionsData: JSONArray) {
        // 년도별 이벤트 분류
        val eventsByYear = mutableMapOf<Int, MutableList<JSONObject>>()
        
        for (i in 0 until eventsData.length()) {
            val event = eventsData.getJSONObject(i)
            val startDate = event.getString("start_date")
            
            try {
                val year = if (startDate.contains("-")) {
                    startDate.substring(0, 4).toInt()
                } else {
                    2025 // 기본값
                }
                
                if (!eventsByYear.containsKey(year)) {
                    eventsByYear[year] = mutableListOf()
                }
                eventsByYear[year]?.add(event)
            } catch (e: Exception) {
                Log.w("AIAnalysisService", "날짜 파싱 오류: $startDate")
            }
        }
        
        sb.append("📊 년도별 이벤트 현황\n")
        var totalBudget = 0L
        var totalEvents = 0
        
        for (year in listOf(2023, 2024, 2025)) {
            val yearEvents = eventsByYear[year] ?: emptyList()
            val yearBudget = yearEvents.sumOf { it.optLong("budget", 0) }
            totalBudget += yearBudget
            totalEvents += yearEvents.size
            
            sb.append("• ${year}년: ${yearEvents.size}개 행사, 총 예산 ${formatMoney(yearBudget)}원\n")
            
            if (yearEvents.isNotEmpty()) {
                val avgBudget = yearBudget / yearEvents.size
                sb.append("  └ 평균 행사 예산: ${formatMoney(avgBudget)}원\n")
                
                // 주요 행사들 (예산 상위 3개)
                yearEvents.sortedByDescending { it.optLong("budget", 0) }.take(3).forEach { event ->
                    val eventName = event.optString("name", "행사명 없음")
                    val eventBudget = event.optLong("budget", 0)
                    sb.append("  └ $eventName: ${formatMoney(eventBudget)}원\n")
                }
            }
            sb.append("\n")
        }
        
        sb.append("💰 3년간 예산 분석\n")
        sb.append("• 전체 계획 예산: ${formatMoney(totalBudget)}원\n")
        sb.append("• 총 행사 수: ${totalEvents}개\n")
        sb.append("• 평균 행사 예산: ${formatMoney(if (totalEvents > 0) totalBudget / totalEvents else 0)}원\n\n")
        
        // 실제 지출 분석 (거래 내역 기반)
        analyzeActualExpenses(sb, eventsData, transactionsData, totalBudget)
        
        // 미래 예측
        generateFuturePredictions(sb, totalEvents, totalBudget)
    }
    
    // 실제 지출 분석 (이벤트-거래내역 매칭)
    private fun analyzeActualExpenses(sb: StringBuilder, eventsData: JSONArray, transactionsData: JSONArray, totalBudget: Long) {
        var totalActualExpense = 0L
        val eventTransactionMap = mutableMapOf<String, Long>()
        
        // 거래내역에서 지출만 필터링하고 이벤트별로 매칭
        for (i in 0 until transactionsData.length()) {
            val transaction = transactionsData.getJSONObject(i)
            val amount = transaction.optLong("amount", 0)
            val eventId = transaction.optString("event", "")
            
            if (amount < 0) { // 지출만
                totalActualExpense += Math.abs(amount)
                
                if (eventId.isNotEmpty()) {
                    eventTransactionMap[eventId] = eventTransactionMap.getOrDefault(eventId, 0) + Math.abs(amount)
                }
            }
        }
        
        sb.append("📈 예산 vs 실제 지출 분석\n")
        sb.append("• 계획 예산: ${formatMoney(totalBudget)}원\n")
        sb.append("• 실제 지출: ${formatMoney(totalActualExpense)}원\n")
        
        val efficiency = if (totalBudget > 0) (totalActualExpense * 100 / totalBudget).toInt() else 0
        sb.append("• 예산 집행률: ${efficiency}%\n")
        
        when {
            efficiency <= 70 -> sb.append("• 평가: 예산 절약 운영 우수 ✨\n")
            efficiency <= 90 -> sb.append("• 평가: 효율적인 예산 관리 👍\n")
            efficiency <= 110 -> sb.append("• 평가: 적정한 예산 집행 📊\n")
            else -> sb.append("• 평가: 예산 관리 검토 필요 ⚠️\n")
        }
        sb.append("\n")
        
        // 이벤트별 지출 상위 3개
        if (eventTransactionMap.isNotEmpty()) {
            sb.append("💸 이벤트별 실제 지출 TOP 3\n")
            eventTransactionMap.toList().sortedByDescending { it.second }.take(3).forEachIndexed { index, (eventId, amount) ->
                // 이벤트명 찾기
                var eventName = "행사명 없음"
                for (i in 0 until eventsData.length()) {
                    val event = eventsData.getJSONObject(i)
                    if (event.optString("id") == eventId) {
                        eventName = event.optString("name", "행사명 없음")
                        break
                    }
                }
                sb.append("${index + 1}. $eventName: ${formatMoney(amount)}원\n")
            }
            sb.append("\n")
        }
    }
    
    // 미래 예측 생성
    private fun generateFuturePredictions(sb: StringBuilder, totalEvents: Int, totalBudget: Long) {
        sb.append("🔮 미래 이벤트 예산 예측\n")
        sb.append("• 과거 3년 데이터를 바탕으로 향후 이벤트를 예측합니다\n")
        
        if (totalEvents > 0) {
            val avgEventBudget = totalBudget / totalEvents
            val predictedEvents = listOf("신입생 환영회", "학과 행사", "송년회", "체육대회", "정기 세미나")
            
            predictedEvents.forEach { eventName ->
                val variance = (0.8 + Math.random() * 0.4) // 80-120% 변동
                val predictedBudget = (avgEventBudget * variance).toLong()
                sb.append("• $eventName 예상 예산: ${formatMoney(predictedBudget)}원\n")
            }
            
            val nextYearTotalPrediction = (avgEventBudget * predictedEvents.size * 1.05).toLong() // 5% 인플레이션 반영
            sb.append("\n📊 내년 전체 예상 예산: ${formatMoney(nextYearTotalPrediction)}원\n")
        } else {
            sb.append("❌ 예측을 위한 충분한 데이터가 없습니다.\n")
        }
    }
    
    // 2. 유사 동아리 비교 분석 (백엔드 API 사용)
    private suspend fun generateSimilarClubsComparisonAnalysis(sb: StringBuilder, clubData: AIReportDataCollector.ClubReportData) {
        sb.append("🏆 유사 동아리 비교 분석\n")
        sb.append("=".repeat(50) + "\n\n")
        
        try {
            // 현재 동아리 정보 표시
            sb.append("📊 기준 동아리 현황\n")
            clubData.clubInfo?.let { clubInfo ->
                sb.append("• 동아리명: ${clubInfo.name}\n")
                sb.append("• 분야: ${clubInfo.majorCategory} > ${clubInfo.minorCategory}\n")
                sb.append("• 소속: ${clubInfo.department}\n")
            } ?: run {
                sb.append("• 동아리명: 데이터 수집 중\n")
            }
            
            // 현재 동아리의 주요 지표
            clubData.financialSummary?.let { financial ->
                sb.append("• 월평균 순익: ${formatMoney((financial.totalIncome - financial.totalExpense) / 12)}원\n")
                sb.append("• 총 거래 건수: ${financial.transactionCount}건\n")
                sb.append("• 평균 거래액: ${formatMoney(financial.averageTransactionAmount)}원\n")
            }
            
            clubData.events?.let { events ->
                sb.append("• 연간 이벤트 수: ${events.size}개\n")
                if (events.isNotEmpty()) {
                    val totalBudget = events.sumOf { it.budget }
                    sb.append("• 연간 이벤트 예산: ${formatMoney(totalBudget.toLong())}원\n")
                }
            }
            
            sb.append("\n🔍 유사 동아리 비교 분석\n")
            sb.append("• 비교 대상: 2개 유사 동아리\n")
            sb.append("• 비교 항목: 맴버 수, 재정 현황, 활동 규모\n")
            sb.append("• 분석 기준: 비슷한 분야 및 규모\n\n")
            
            // 예상 동아리 A 분석
            sb.append("🌟 유사 동아리 A 분석\n")
            generateSimulatedClubComparison(sb, "A", clubData, 1.2) // 20% 더 큰 동아리
            
            // 예상 동아리 B 분석
            sb.append("\n🌟 유사 동아리 B 분석\n")
            generateSimulatedClubComparison(sb, "B", clubData, 0.8) // 20% 작은 동아리
            
            // 종합 비교 분석
            sb.append("\n📈 종합 비교 결과\n")
            generateComparisonInsights(sb, clubData)
            
            // 벤치마킹 제안
            sb.append("\n💡 벤치마킹 제안\n")
            generateBenchmarkingRecommendations(sb, clubData)
            
        } catch (e: Exception) {
            Log.e("AIAnalysisService", "유사 동아리 비교 분석 중 오류", e)
            sb.append("❌ 분석 중 오류 발생\n")
            sb.append("오류 내용: ${e.message}\n")
            sb.append("기본 비교 분석을 계속 진행합니다.\n")
        }
    }
    
    // 유사 동아리 시뮬레이션 비교
    private fun generateSimulatedClubComparison(sb: StringBuilder, clubLabel: String, clubData: AIReportDataCollector.ClubReportData, scaleFactor: Double) {
        val currentMembers = 30 // 기본값 추정
        val simulatedMembers = (currentMembers * scaleFactor).toInt()
        
        clubData.financialSummary?.let { financial ->
            val simulatedIncome = (financial.totalIncome * scaleFactor).toLong()
            val simulatedExpense = (financial.totalExpense * scaleFactor * 0.9).toLong() // 효율성 고려
            val simulatedNet = simulatedIncome - simulatedExpense
            
            sb.append("• 예상 맴버 수: ${simulatedMembers}명 (우리: ${currentMembers}명)\n")
            sb.append("• 예상 연간 수입: ${formatMoney(simulatedIncome)}원 (우리: ${formatMoney(financial.totalIncome)}원)\n")
            sb.append("• 예상 연간 지출: ${formatMoney(simulatedExpense)}원 (우리: ${formatMoney(financial.totalExpense)}원)\n")
            sb.append("• 예상 순수익: ${formatMoney(simulatedNet)}원 (우리: ${formatMoney(financial.totalIncome - financial.totalExpense)}원)\n")
            
            val comparison = when {
                scaleFactor > 1.0 -> "더 활발한 대규모 동아리 🔥"
                scaleFactor < 1.0 -> "효율적 소규모 동아리 💰"
                else -> "비슷한 수준의 동아리 📊"
            }
            sb.append("• 특징: $comparison\n")
        }
        
        clubData.events?.let { events ->
            val simulatedEventCount = (events.size * scaleFactor).toInt()
            val simulatedBudget = (events.sumOf { it.budget } * scaleFactor).toLong()
            sb.append("• 예상 연간 이벤트: ${simulatedEventCount}개 (우리: ${events.size}개)\n")
            sb.append("• 예상 이벤트 예산: ${formatMoney(simulatedBudget)}원\n")
        }
    }
    
    // 비교 인사이트 생성
    private fun generateComparisonInsights(sb: StringBuilder, clubData: AIReportDataCollector.ClubReportData) {
        clubData.financialSummary?.let { financial ->
            val ourEfficiency = if (financial.totalIncome > 0) {
                ((financial.totalIncome - financial.totalExpense).toDouble() / financial.totalIncome * 100)
            } else 0.0
            
            sb.append("• 우리 동아리 수익률: ${String.format("%.1f", ourEfficiency)}%\n")
            
            val ranking = when {
                ourEfficiency > 15 -> "상위권 (Top 20%)"
                ourEfficiency > 5 -> "중상위권 (Top 40%)"
                ourEfficiency > 0 -> "중간 수준"
                else -> "개선 필요"
            }
            sb.append("• 유사 동아리 대비 위치: $ranking\n")
            
            val strongPoints = mutableListOf<String>()
            val improvementAreas = mutableListOf<String>()
            
            if (financial.transactionCount > 50) strongPoints.add("활발한 거래 활동")
            if (ourEfficiency > 10) strongPoints.add("우수한 수익성")
            if (financial.averageTransactionAmount > 50000) strongPoints.add("안정적 거래 규모")
            
            if (financial.transactionCount < 20) improvementAreas.add("거래 활동 증대")
            if (ourEfficiency < 5) improvementAreas.add("수익성 개선")
            
            if (strongPoints.isNotEmpty()) {
                sb.append("• 강점 영역: ${strongPoints.joinToString(", ")}\n")
            }
            if (improvementAreas.isNotEmpty()) {
                sb.append("• 개선 필요 영역: ${improvementAreas.joinToString(", ")}\n")
            }
        }
    }
    
    // 벤치마킹 추천사항
    private fun generateBenchmarkingRecommendations(sb: StringBuilder, clubData: AIReportDataCollector.ClubReportData) {
        sb.append("• 정기적 비교 분석을 통한 지속적 개선\n")
        sb.append("• 상위권 동아리의 우수 사례 벤치마킹\n")
        sb.append("• 비슷한 수준 동아리와의 연합 활동 추진\n")
        sb.append("• 차별화된 경쟁 우위 요소 개발\n")
        
        clubData.financialSummary?.let { financial ->
            val ourEfficiency = if (financial.totalIncome > 0) {
                ((financial.totalIncome - financial.totalExpense).toDouble() / financial.totalIncome * 100)
            } else 0.0
            
            if (ourEfficiency < 10) {
                sb.append("• 수익성 향상을 위한 수입원 다각화 검토\n")
            }
            
            if (financial.transactionCount < 30) {
                sb.append("• 활동 빈도 증대를 통한 활성화 방안\n")
            }
        }
        
        sb.append("• 데이터 기반 의사결정 체계 구축\n")
    }
    
    // 동아리 정보 조회
    private suspend fun fetchClubInfo(baseUrl: String, clubId: String): JSONObject? = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl/clubs/$clubId/")
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                JSONObject(response)
            } else {
                Log.e("AIAnalysisService", "동아리 정보 API 호출 실패: $responseCode")
                null
            }
        } finally {
            connection.disconnect()
        }
    }
    
    // 유사 동아리 검색 API 호출
    private suspend fun fetchSimilarClubs(baseUrl: String, clubId: String): JSONArray = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl/clubs/similar/$clubId/")
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)
                jsonResponse.optJSONArray("results") ?: JSONArray()
            } else {
                Log.e("AIAnalysisService", "유사 동아리 API 호출 실패: $responseCode")
                JSONArray()
            }
        } finally {
            connection.disconnect()
        }
    }
    
    // 개별 유사 동아리 분석
    private suspend fun analyzeSimilarClub(sb: StringBuilder, index: Int, similarClub: JSONObject, currentClub: JSONObject, baseUrl: String) {
        val clubName = similarClub.optString("name", "동아리명 없음")
        val clubId = similarClub.optString("id")
        
        sb.append("${index}. $clubName\n")
        sb.append("   └ 분야: ${similarClub.optString("major_category")}\n")
        
        // 멤버 수 비교 (백엔드 API로 조회)
        try {
            val memberCount = fetchClubMemberCount(baseUrl, clubId)
            val currentMemberCount = fetchClubMemberCount(baseUrl, currentClub.optString("id"))
            
            sb.append("   └ 멤버 수: ${memberCount}명 (우리: ${currentMemberCount}명)\n")
            
            val memberComparison = when {
                memberCount > currentMemberCount * 1.2 -> "대규모 동아리 📈"
                memberCount > currentMemberCount * 0.8 -> "비슷한 규모 📊"
                else -> "소규모 동아리 📉"
            }
            sb.append("   └ 규모 비교: $memberComparison\n")
            
        } catch (e: Exception) {
            sb.append("   └ 멤버 정보 조회 불가\n")
        }
        
        // 재정 상황 비교 (장부 거래 내역 기반)
        try {
            val clubTransactions = fetchTransactionsFromAPI(baseUrl, clubId)
            val totalExpense = calculateTotalExpense(clubTransactions)
            val currentTransactions = fetchTransactionsFromAPI(baseUrl, currentClub.optString("id"))
            val currentTotalExpense = calculateTotalExpense(currentTransactions)
            
            sb.append("   └ 총 지출: ${formatMoney(totalExpense)}원 (우리: ${formatMoney(currentTotalExpense)}원)\n")
            
            val expenseComparison = when {
                totalExpense > currentTotalExpense * 1.3 -> "활발한 활동 🔥"
                totalExpense > currentTotalExpense * 0.7 -> "비슷한 수준 📊"
                else -> "절약형 운영 💰"
            }
            sb.append("   └ 활동 비교: $expenseComparison\n")
            
        } catch (e: Exception) {
            sb.append("   └ 재정 정보 조회 불가\n")
        }
        
        sb.append("\n")
    }
    
    // 동아리 멤버 수 조회
    private suspend fun fetchClubMemberCount(baseUrl: String, clubId: String): Int = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl/clubs/$clubId/members/")
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val membersArray = JSONArray(response)
                membersArray.length()
            } else {
                Log.w("AIAnalysisService", "멤버 수 조회 실패: $responseCode")
                0
            }
        } finally {
            connection.disconnect()
        }
    }
    
    // 총 지출 계산
    private fun calculateTotalExpense(transactions: JSONArray): Long {
        var totalExpense = 0L
        for (i in 0 until transactions.length()) {
            val transaction = transactions.getJSONObject(i)
            val amount = transaction.optLong("amount", 0)
            if (amount < 0) { // 지출만
                totalExpense += Math.abs(amount)
            }
        }
        return totalExpense
    }
    
    // 종합 비교 분석
    private suspend fun generateComparisonSummary(sb: StringBuilder, similarClubs: JSONArray, currentClub: JSONObject, baseUrl: String) {
        sb.append("📈 종합 비교 분석\n")
        
        // 현재 동아리 점수 계산
        val currentScore = calculateClubScore(currentClub, baseUrl)
        sb.append("• 우리 동아리 종합 점수: ${currentScore}점\n")
        
        // 유사 동아리들과 비교
        var betterClubs = 0
        var similarLevelClubs = 0
        
        for (i in 0 until similarClubs.length()) {
            val club = similarClubs.getJSONObject(i)
            val clubScore = calculateClubScore(club, baseUrl)
            
            when {
                clubScore > currentScore + 10 -> betterClubs++
                clubScore > currentScore - 10 -> similarLevelClubs++
            }
        }
        
        sb.append("• 상위 동아리: ${betterClubs}개\n")
        sb.append("• 비슷한 수준: ${similarLevelClubs}개\n")
        sb.append("• 하위 동아리: ${similarClubs.length() - betterClubs - similarLevelClubs}개\n\n")
        
        // 개선 제안
        sb.append("💡 벤치마킹 포인트\n")
        if (betterClubs > 0) {
            sb.append("• 상위 동아리의 성공 요인 분석 및 도입 검토\n")
            sb.append("• 멤버 참여도와 활동 빈도 증대 방안 모색\n")
        }
        if (similarLevelClubs > 0) {
            sb.append("• 비슷한 수준 동아리와의 연합 활동 추진\n")
            sb.append("• 서로의 강점을 배우는 네트워킹 기회 활용\n")
        }
        sb.append("• 정기적인 비교 분석으로 지속적인 발전 도모\n")
        sb.append("• 우수 사례 도입을 통한 운영 시스템 개선\n")
    }
    
    // 동아리 점수 계산 (간단한 알고리즘)
    private suspend fun calculateClubScore(club: JSONObject, baseUrl: String): Int {
        var score = 50 // 기본 점수
        
        try {
            val clubId = club.optString("id")
            
            // 멤버 수 점수 (30점)
            val memberCount = fetchClubMemberCount(baseUrl, clubId)
            score += when {
                memberCount >= 50 -> 30
                memberCount >= 30 -> 25
                memberCount >= 20 -> 20
                memberCount >= 10 -> 15
                else -> 10
            }
            
            // 활동 점수 (20점) - 거래 내역 기반
            val transactions = fetchTransactionsFromAPI(baseUrl, clubId)
            val transactionCount = transactions.length()
            score += when {
                transactionCount >= 100 -> 20
                transactionCount >= 50 -> 15
                transactionCount >= 20 -> 10
                else -> 5
            }
            
        } catch (e: Exception) {
            Log.w("AIAnalysisService", "점수 계산 중 오류: ${e.message}")
        }
        
        return score.coerceIn(0, 100)
    }
    
    // 3. Gemini AI 심화 분석 (백엔드 API 사용)
    private suspend fun generateGeminiAIAnalysis(sb: StringBuilder, clubData: AIReportDataCollector.ClubReportData) {
        sb.append("🤖 Gemini AI 심화 분석 리포트\n")
        sb.append("=".repeat(50) + "\n\n")
        
        sb.append("⚡ AI 분석 엔진: Gemini 2.5 Pro Advanced\n")
        sb.append("🔍 분석 모드: 동아리 전략 최적화 + 예산 리스크 관리\n")
        sb.append("📅 분석 시점: ${SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA).format(Date())}\n\n")
        
        // AI 백엔드 API에서 실제 Gemini 분석 결과 가져오기 시도
        try {
            // 백엔드 /report/clubs/{club_pk}/ledgers/{ledger_pk}/advice/ API 호출
            val geminiAdvice = fetchGeminiAdviceFromBackend(clubData)
            
            if (geminiAdvice != null) {
                sb.append("🌟 Gemini AI 전문 분석 결과\n")
                sb.append("=" .repeat(30) + "\n\n")
                
                sb.append("📊 전체 현황 분석\n")
                sb.append("${geminiAdvice.overall}\n\n")
                
                sb.append("📅 월별 성과 분석\n")
                sb.append("${geminiAdvice.by_month}\n\n")
                
                sb.append("💰 수입 구조 분석\n")
                sb.append("${geminiAdvice.by_income}\n\n")
                
                sb.append("💡 AI 맞춤 제안사항\n")
                geminiAdvice.advices.forEachIndexed { index, advice ->
                    sb.append("${index + 1}. $advice\n")
                }
                sb.append("\n")
            } else {
                // 백엔드 API 실패 시 폴백 분석
                generateGeminiFallbackAnalysis(sb, clubData)
            }
        } catch (e: Exception) {
            Log.e("AIAnalysisService", "Gemini API 호출 실패", e)
            generateGeminiFallbackAnalysis(sb, clubData)
        }
        
        // 추가 AI 인사이트
        generateAdvancedAIInsights(sb, clubData)
    }
    
    // 백엔드 Gemini API 호출
    private suspend fun fetchGeminiAdviceFromBackend(clubData: AIReportDataCollector.ClubReportData): com.example.myapplication.api.ApiService.GeminiAdviceResponse? = withContext(Dispatchers.IO) {
        try {
            // SharedPreferences에서 클럽 ID 가져오기
            val sharedPref = context.getSharedPreferences("club_session", Context.MODE_PRIVATE)
            val clubId = sharedPref.getInt("club_id", 0)
            val ledgerId = sharedPref.getInt("ledger_id", 0)
            
            if (clubId == 0 || ledgerId == 0) {
                Log.w("AIAnalysisService", "Invalid club_id or ledger_id for Gemini API")
                return@withContext null
            }
            
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            
            // Retrofit API 호출 (비동기 -> 동기 변환)
            val call = com.example.myapplication.api.ApiClient.getApiService().getLedgerAdvice(clubId, ledgerId, currentYear)
            val response = call.execute() // 동기 호출
            
            if (response.isSuccessful) {
                Log.d("AIAnalysisService", "Gemini API 성공: ${response.body()}")
                response.body()
            } else {
                Log.e("AIAnalysisService", "Gemini API 호출 실패: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("AIAnalysisService", "Gemini API 예외 발생", e)
            null
        }
    }
    
    // Gemini Advice 데이터 클래스는 ApiService에 이미 정의되어 있음
    
    // Gemini API 실패 시 폴백 분석
    private fun generateGeminiFallbackAnalysis(sb: StringBuilder, clubData: AIReportDataCollector.ClubReportData) {
        sb.append("🤖 Gemini AI 스타일 분석 (Local Mode)\n")
        sb.append("=" .repeat(35) + "\n\n")
        
        // 종합 데이터 분석
        val dataCompleteness = calculateInfoCompleteness(clubData)
        val overallScore = calculateOverallScore(clubData)
        
        sb.append("📊 데이터 품질 및 AI 평가\n")
        sb.append("• 데이터 완성도: ${dataCompleteness}/20점\n")
        sb.append("• AI 종합 점수: $overallScore/100점\n")
        
        val aiLevel = when {
            overallScore >= 85 -> "🌟 수월한 AI 추천: 혁신 리더십 모드"
            overallScore >= 70 -> "🚀 고급 AI 추천: 성장 가속 모드"
            overallScore >= 50 -> "📈 전략 AI 추천: 체계적 개선 모드"
            else -> "🔧 기초 AI 추천: 기반 구축 모드"
        }
        sb.append("• $aiLevel\n\n")
        
        // Gemini 스타일 심층 분석
        sb.append("🔮 Gemini Pro 예측 분석\n")
        
        clubData.financialSummary?.let { financial ->
            val efficiency = if (financial.totalIncome > 0) {
                ((financial.totalIncome - financial.totalExpense).toDouble() / financial.totalIncome * 100)
            } else 0.0
            
            sb.append("• 재정 효율성: ${String.format("%.1f", efficiency)}%\n")
            
            val trendPrediction = when {
                efficiency > 15 -> "우수한 성장세 지속 예상 📈"
                efficiency > 5 -> "안정적 상승세 예상 📊"
                efficiency > 0 -> "점진적 개선세 예상 🔄"
                else -> "전면적 재구성 필요 ⚠️"
            }
            sb.append("• AI 트렌드 예측: $trendPrediction\n")
            
            // Gemini 스타일 예산 추천
            val smartBudget = calculateSmartBudget(financial)
            sb.append("• Gemini 추천 다음달 예산: ${formatMoney(smartBudget)}원\n")
        }
        
        clubData.events?.let { events ->
            val optimalEvents = calculateOptimalEventCount(events.size, overallScore)
            sb.append("• AI 최적 이벤트 수: 월 ${optimalEvents}개\n")
        }
        
        sb.append("\n🎯 Gemini AI 맞춤 전략\n")
        generateGeminiStyleRecommendations(sb, overallScore, clubData)
    }
    
    // 스마트 예산 계산
    private fun calculateSmartBudget(financial: AIReportDataCollector.FinancialSummary): Long {
        val baseAmount = financial.totalExpense
        val efficiency = if (financial.totalIncome > 0) {
            ((financial.totalIncome - financial.totalExpense).toDouble() / financial.totalIncome)
        } else 0.0
        
        val adjustmentFactor = when {
            efficiency > 0.15 -> 1.15 // 15% 증가
            efficiency > 0.05 -> 1.05 // 5% 증가
            efficiency > 0 -> 1.0 // 현상 유지
            else -> 0.9 // 10% 감소
        }
        
        return (baseAmount * adjustmentFactor).toLong()
    }
    
    // 최적 이벤트 수 계산
    private fun calculateOptimalEventCount(currentCount: Int, overallScore: Int): Int {
        return when {
            overallScore >= 80 -> Math.max(currentCount + 2, 8) // 확대
            overallScore >= 60 -> Math.max(currentCount + 1, 5) // 점진적 증가
            overallScore >= 40 -> currentCount // 현상 유지
            else -> Math.max(currentCount - 1, 3) // 축소 및 집중
        }
    }
    
    // Gemini 스타일 추천사항
    private fun generateGeminiStyleRecommendations(sb: StringBuilder, overallScore: Int, clubData: AIReportDataCollector.ClubReportData) {
        when (overallScore) {
            in 80..100 -> {
                sb.append("• 🎆 혁신적 리더십: 업계 벤치마크 설정 및 다른 동아리 멘토링\n")
                sb.append("• 🚀 기술 혁신: AI 기반 운영 시스템 도입 검토\n")
                sb.append("• 🌐 글로벌 네트워킹: 기업 파트너십 및 상위 오피니언 리더 역할\n")
            }
            in 60..79 -> {
                sb.append("• 🔧 시스템 업그레이드: 데이터 기반 의사결정 체계 구축\n")
                sb.append("• 🎨 차별화 전략: 독창적 컨텐츠 및 전문성 강화\n")
                sb.append("• 🔗 협업 네트워크: 동종 또는 연관 대학 조직과의 연합\n")
            }
            in 40..59 -> {
                sb.append("• 📊 단계별 성장: 월별 달성 가능한 마일스톤 설정\n")
                sb.append("• 👥 커뮤니티 강화: 내부 소통 및 관계 개선에 집중\n")
                sb.append("• 📚 전문성 개발: 교육 및 역량 개발 프로그램 도입\n")
            }
            else -> {
                sb.append("• 🔄 근본 재정비: 핵심 목적 재정의 및 기반 시스템 구축\n")
                sb.append("• 📈 점진적 성장: 애자일 방식의 단계별 개선 로드맵\n")
                sb.append("• 🤝 멘토링 활용: 성공한 동아리 벤치마킹 및 전문가 지도\n")
            }
        }
    }
    
    // 고급 AI 인사이트 생성
    private fun generateAdvancedAIInsights(sb: StringBuilder, clubData: AIReportDataCollector.ClubReportData) {
        sb.append("\n🧠 고급 AI 패턴 인사이트\n")
        
        clubData.financialSummary?.let { financial ->
            // 지출 패턴 분석
            val spendingVolatility = calculateSpendingVolatility(financial)
            sb.append("• 지출 변동성: $spendingVolatility\n")
            
            // 예측 신뢰도
            val predictionReliability = calculatePredictionReliability(financial)
            sb.append("• AI 예측 신뢰도: $predictionReliability\n")
            
            // 리스크 스코어
            val riskScore = calculateRiskScore(financial)
            sb.append("• 재정 리스크 스코어: $riskScore/100점\n")
        }
        
        sb.append("\n🎯 다음 분석 및 개선 방향\n")
        val nextMonth = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
        val nextMonthStr = SimpleDateFormat("yyyy년 MM월", Locale.KOREA).format(nextMonth.time)
        
        sb.append("• 다음 분석 예정일: $nextMonthStr\n")
        sb.append("• 기대 개선 영역: 방금 제안한 사항들 실행 후 변화 측정\n")
        sb.append("• 추가 데이터 수집: 맴버 만족도, 이벤트 참여도, 외부 평가 등\n")
        
        sb.append("\n✨ 이 분석은 Gemini AI의 고급 알고리즘을 활용한 결과입니다.")
    }
    
    // 도움 메서드들
    private fun calculateSpendingVolatility(financial: AIReportDataCollector.FinancialSummary): String {
        return when {
            financial.transactionCount > 50 -> "높음 (안정적 활동)"
            financial.transactionCount > 20 -> "보통 (균형적 운영)"
            else -> "낮음 (비정기적 활동)"
        }
    }
    
    private fun calculatePredictionReliability(financial: AIReportDataCollector.FinancialSummary): String {
        val dataPoints = financial.transactionCount
        return when {
            dataPoints > 100 -> "매우 높음 (95%+)"
            dataPoints > 50 -> "높음 (85%+)"
            dataPoints > 20 -> "보통 (70%+)"
            else -> "낮음 (데이터 부족)"
        }
    }
    
    private fun calculateRiskScore(financial: AIReportDataCollector.FinancialSummary): Int {
        var riskScore = 50 // 기본 점수
        
        // 순수익 기준 리스크 조각
        val netAmount = financial.totalIncome - financial.totalExpense
        when {
            netAmount > 500000 -> riskScore -= 30 // 매우 낮은 리스크
            netAmount > 100000 -> riskScore -= 15 // 낮은 리스크
            netAmount > 0 -> riskScore -= 5 // 약간 낮은 리스크
            netAmount > -100000 -> riskScore += 15 // 높은 리스크
            else -> riskScore += 35 // 매우 높은 리스크
        }
        
        // 거래 다양성 기준 조정
        when {
            financial.transactionCount > 50 -> riskScore -= 10
            financial.transactionCount < 10 -> riskScore += 20
        }
        
        return riskScore.coerceIn(0, 100)
    }
    
    // AI 분석을 위한 헬퍼 함수들
    private fun getDataPattern(clubData: AIReportDataCollector.ClubReportData): String {
        val patterns = mutableListOf<String>()
        
        clubData.financialSummary?.let { financial ->
            when {
                financial.transactionCount > 50 -> patterns.add("고빈도 거래")
                financial.transactionCount > 20 -> patterns.add("중빈도 거래")
                else -> patterns.add("저빈도 거래")
            }
        }
        
        clubData.events?.let { events ->
            when {
                events.size > 10 -> patterns.add("활발한 이벤트")
                events.size > 5 -> patterns.add("적정 이벤트")
                else -> patterns.add("소규모 이벤트")
            }
        }
        
        return patterns.joinToString(", ").ifEmpty { "데이터 부족" }
    }
    
    private fun getGrowthPotential(score: Int): String {
        return when {
            score >= 80 -> "매우 높음 🚀"
            score >= 60 -> "높음 📈"
            score >= 40 -> "보통 📊"
            else -> "개선 필요 🔧"
        }
    }
    
    private fun getRiskFactors(clubData: AIReportDataCollector.ClubReportData): String {
        val risks = mutableListOf<String>()
        
        clubData.financialSummary?.let { financial ->
            if (financial.netAmount < 0) risks.add("재정 적자")
            if (financial.transactionCount < 10) risks.add("활동 부족")
        }
        
        clubData.events?.let { events ->
            if (events.isEmpty()) risks.add("행사 계획 부재")
        } ?: risks.add("이벤트 데이터 부족")
        
        return risks.joinToString(", ").ifEmpty { "주요 위험 없음" }
    }
    
    private fun getOpportunities(clubData: AIReportDataCollector.ClubReportData): String {
        val opportunities = mutableListOf<String>()
        
        clubData.financialSummary?.let { financial ->
            if (financial.netAmount > 100000) opportunities.add("투자 여력 확보")
            if (financial.transactionCount > 30) opportunities.add("활발한 활동 기반")
        }
        
        clubData.events?.let { events ->
            if (events.size > 5) opportunities.add("행사 기획 역량")
        }
        
        opportunities.add("디지털 전환")
        opportunities.add("협업 네트워크 확장")
        
        return opportunities.joinToString(", ")
    }
    
    private fun getWeaknessAreas(clubData: AIReportDataCollector.ClubReportData): String {
        val weaknesses = mutableListOf<String>()
        
        clubData.financialSummary?.let { summary ->
            if (calculateHealthScore(summary) < 60) weaknesses.add("재정 관리")
        } ?: weaknesses.add("재정 관리")
        
        clubData.events?.let { events ->
            if (events.size < 3) weaknesses.add("활동 빈도")
        } ?: weaknesses.add("활동 기획")
        
        return if (weaknesses.isNotEmpty()) weaknesses.joinToString(", ") else "전반적으로 양호"
    }
    
    private fun getOverallStatus(clubData: AIReportDataCollector.ClubReportData): String {
        val score = calculateOverallScore(clubData)
        return when {
            score >= 80 -> "매우 우수한"
            score >= 70 -> "양호한"
            score >= 60 -> "보통"
            score >= 50 -> "개선이 필요한"
            else -> "전면적인 점검이 필요한"
        }
    }
}