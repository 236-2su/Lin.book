package com.example.myapplication.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AIAnalysisService {
    
    data class AIReportResult(
        val success: Boolean,
        val content: String,
        val error: String? = null
    )
    
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
    
    private fun analyzeDataLocally(
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
        when (reportType) {
            "financial_analysis" -> generateFinancialAnalysis(sb, clubData)
            "activity_analysis" -> generateActivityAnalysis(sb, clubData)
            "comprehensive" -> generateComprehensiveAnalysis(sb, clubData)
            "comparison" -> generateComparisonAnalysis(sb, clubData)
            else -> generateGeneralAnalysis(sb, clubData)
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
            else -> "📋 일반 종합 분석"
        }
    }
    
    private fun generateFinancialAnalysis(sb: StringBuilder, clubData: AIReportDataCollector.ClubReportData) {
        sb.append("💰 재정 현황 심층 분석\n")
        sb.append("=".repeat(30) + "\n\n")
        
        clubData.financialSummary?.let { summary ->
            // 📊 핵심 지표
            sb.append("📊 핵심 재정 지표\n")
            sb.append("• 총 수입: ${formatMoney(summary.totalIncome)}원 💚\n")
            sb.append("• 총 지출: ${formatMoney(summary.totalExpense)}원 💸\n")
            sb.append("• 순손익: ${formatMoney(summary.netAmount)}원 ${if (summary.netAmount >= 0) "📈" else "📉"}\n")
            sb.append("• 거래 건수: ${summary.transactionCount}건 📝\n")
            sb.append("• 평균 거래액: ${formatMoney(summary.averageTransactionAmount)}원 💫\n\n")
            
            // 🎯 재정 건전성 평가
            val healthScore = calculateHealthScore(summary)
            sb.append("🎯 재정 건전성 평가\n")
            sb.append("• 종합 점수: $healthScore/100점 ${getScoreEmoji(healthScore)}\n")
            sb.append("• 재정 상태: ${summary.monthlyTrend} ${getTrendEmoji(summary.monthlyTrend)}\n\n")
            
            // 💡 상세 분석
            sb.append("💡 상세 분석 및 인사이트\n")
            when {
                summary.netAmount > 100000 -> {
                    sb.append("🌟 우수한 재정 관리!\n")
                    sb.append("• 현재 흑자 규모가 매우 양호합니다\n")
                    sb.append("• 안정적인 동아리 운영이 가능한 상태입니다\n")
                    sb.append("• 여유 자금을 활용한 신규 프로젝트를 고려해보세요\n")
                    sb.append("• 장기적인 발전 계획 수립을 권장합니다\n")
                }
                summary.netAmount > 0 -> {
                    sb.append("😊 안정적인 재정 상태\n")
                    sb.append("• 수입과 지출의 균형이 잘 맞고 있습니다\n")
                    sb.append("• 현재 수준의 재정 관리를 지속하세요\n")
                    sb.append("• 예비비 확보를 통한 리스크 관리를 권장합니다\n")
                }
                summary.netAmount > -50000 -> {
                    sb.append("⚠️ 주의가 필요한 상황\n")
                    sb.append("• 지출이 수입을 약간 초과하고 있습니다\n")
                    sb.append("• 불필요한 지출 항목을 점검해보세요\n")
                    sb.append("• 수입원 다각화 방안을 모색하세요\n")
                    sb.append("• 단기적인 절약 계획이 필요합니다\n")
                }
                else -> {
                    sb.append("🚨 긴급 재정 개선 필요\n")
                    sb.append("• 즉시 예산 재조정이 필요한 상황입니다\n")
                    sb.append("• 필수 지출 외의 모든 항목을 재검토하세요\n")
                    sb.append("• 추가 수입원 확보가 시급합니다\n")
                    sb.append("• 임원진 회의를 통한 대책 마련을 권장합니다\n")
                }
            }
            
            // 📈 월별 트렌드 분석
            if (summary.transactionCount > 10) {
                sb.append("\n📈 거래 패턴 분석\n")
                sb.append("• 거래 활성도: ${if (summary.transactionCount > 20) "높음" else "보통"} 📊\n")
                sb.append("• 평균 거래 규모: ${if (summary.averageTransactionAmount > 10000) "중대형" else "소규모"} 💳\n")
                sb.append("• 재정 관리 수준: ${if (healthScore > 70) "체계적" else "개선 필요"} 📋\n")
            }
            
        } ?: run {
            sb.append("❌ 재정 데이터 부족\n")
            sb.append("현재 분석할 수 있는 재정 데이터가 부족합니다.\n")
            sb.append("장부 데이터를 추가하여 다시 분석해주세요.\n\n")
            sb.append("📝 권장사항:\n")
            sb.append("• 최근 3개월간의 거래 내역 입력\n")
            sb.append("• 정기적인 회비 및 지출 기록\n")
            sb.append("• 행사별 예산 및 실제 지출 관리\n")
        }
    }
    
    private fun generateActivityAnalysis(sb: StringBuilder, clubData: AIReportDataCollector.ClubReportData) {
        sb.append("🎯 활동 현황 종합 분석\n")
        sb.append("=".repeat(30) + "\n\n")
        
        // 🏢 동아리 기본 정보
        clubData.clubInfo?.let { club ->
            sb.append("🏢 동아리 프로필\n")
            sb.append("• 분야: ${club.majorCategory} → ${club.minorCategory} 🎨\n")
            sb.append("• 소속: ${club.department} 🏫\n")
            if (club.location.isNotBlank()) {
                sb.append("• 활동 장소: ${club.location} 📍\n")
            }
            sb.append("• 설립일: ${club.createdAt} 📅\n")
            if (club.hashtags.isNotBlank()) {
                sb.append("• 특징: ${club.hashtags} 🏷️\n")
            }
            sb.append("\n📝 동아리 소개\n")
            sb.append("${club.description}\n\n")
        }
        
        // 🎪 행사 활동 분석
        clubData.events?.let { events ->
            sb.append("🎪 행사 활동 분석\n")
            sb.append("• 총 행사 수: ${events.size}건 📊\n")
            
            if (events.isNotEmpty()) {
                val totalBudget = events.sumOf { it.budget }
                val avgBudget = totalBudget / events.size
                
                sb.append("• 총 예산 규모: ${formatMoney(totalBudget.toLong())}원 💰\n")
                sb.append("• 평균 행사 예산: ${formatMoney(avgBudget.toLong())}원 📈\n\n")
                
                // 📅 최근 주요 행사
                sb.append("📅 최근 주요 행사 목록\n")
                events.take(5).forEach { event ->
                    sb.append("• ${event.name} 🎉\n")
                    sb.append("  └ 기간: ${event.start_date} ~ ${event.end_date}\n")
                    sb.append("  └ 예산: ${formatMoney(event.budget.toLong())}원\n")
                }
                
                // 📊 활동 수준 평가
                sb.append("\n📊 활동 수준 평가\n")
                val activityLevel = when {
                    events.size >= 10 -> "매우 활발 🔥"
                    events.size >= 5 -> "활발 ⚡"
                    events.size >= 2 -> "보통 📊"
                    else -> "저조 😴"
                }
                sb.append("• 활동 빈도: $activityLevel\n")
                
                val budgetLevel = when {
                    avgBudget >= 100000 -> "대규모 🎯"
                    avgBudget >= 50000 -> "중규모 📊"
                    avgBudget >= 20000 -> "소규모 💫"
                    else -> "미니 🌱"
                }
                sb.append("• 행사 규모: $budgetLevel\n")
                
                // 💡 활동 인사이트
                sb.append("\n💡 활동 분석 인사이트\n")
                when {
                    events.size >= 8 -> {
                        sb.append("🌟 매우 활발한 동아리 운영!\n")
                        sb.append("• 정기적인 행사 개최로 높은 참여도가 예상됩니다\n")
                        sb.append("• 다양한 활동으로 멤버들의 만족도가 높을 것입니다\n")
                        sb.append("• 현재 활동 수준을 유지하며 질적 개선에 집중하세요\n")
                    }
                    events.size >= 4 -> {
                        sb.append("😊 양호한 활동 수준\n")
                        sb.append("• 적절한 빈도의 행사로 안정적인 운영이 이뤄지고 있습니다\n")
                        sb.append("• 멤버들의 참여도 향상을 위한 추가 활동을 고려해보세요\n")
                        sb.append("• 정기 모임과 특별 이벤트의 균형을 맞춰보세요\n")
                    }
                    else -> {
                        sb.append("📈 활동 증진 기회\n")
                        sb.append("• 행사 빈도를 늘려 멤버들의 참여도를 높여보세요\n")
                        sb.append("• 소규모 정기 모임부터 시작하는 것을 권장합니다\n")
                        sb.append("• 멤버들의 관심사를 반영한 활동을 기획해보세요\n")
                    }
                }
                
                // 🎯 예산 효율성 분석
                if (events.size >= 3) {
                    sb.append("\n🎯 예산 효율성 분석\n")
                    val maxBudgetEvent = events.maxByOrNull { it.budget }
                    val minBudgetEvent = events.minByOrNull { it.budget }
                    
                    sb.append("• 최대 예산 행사: ${maxBudgetEvent?.name} (${formatMoney(maxBudgetEvent?.budget?.toLong() ?: 0)}원)\n")
                    sb.append("• 최소 예산 행사: ${minBudgetEvent?.name} (${formatMoney(minBudgetEvent?.budget?.toLong() ?: 0)}원)\n")
                    
                    val budgetVariance = (maxBudgetEvent?.budget ?: 0) - (minBudgetEvent?.budget ?: 0)
                    sb.append("• 예산 편차: ${formatMoney(budgetVariance.toLong())}원\n")
                }
                
            } else {
                sb.append("현재 등록된 행사가 없습니다.\n")
            }
        } ?: run {
            sb.append("🎪 행사 데이터 없음\n")
            sb.append("현재 분석할 수 있는 행사 데이터가 없습니다.\n\n")
            sb.append("📝 권장사항:\n")
            sb.append("• 최근 진행한 행사 정보를 입력해주세요\n")
            sb.append("• 정기 모임 및 특별 이벤트 계획을 수립하세요\n")
            sb.append("• 행사별 예산과 목표를 명확히 설정하세요\n")
        }
    }
    
    private fun generateComprehensiveAnalysis(sb: StringBuilder, clubData: AIReportDataCollector.ClubReportData) {
        sb.append("📊 동아리 운영 종합 평가\n")
        sb.append("=".repeat(30) + "\n\n")
        
        // 🎯 종합 점수 계산
        val overallScore = calculateOverallScore(clubData)
        sb.append("🎯 종합 운영 점수\n")
        sb.append("• 총점: $overallScore/100점 ${getScoreEmoji(overallScore)}\n")
        sb.append("• 등급: ${getGradeFromScore(overallScore)} ${getGradeEmoji(overallScore)}\n\n")
        
        // 📈 영역별 평가
        sb.append("📈 영역별 세부 평가\n")
        
        // 재정 영역
        val financialScore = clubData.financialSummary?.let { calculateHealthScore(it) } ?: 0
        sb.append("💰 재정 관리: $financialScore/100점 ${getScoreEmoji(financialScore)}\n")
        
        // 활동 영역  
        val activityScore = clubData.events?.let { events ->
            when {
                events.size >= 10 -> 90
                events.size >= 5 -> 75
                events.size >= 2 -> 60
                events.size >= 1 -> 40
                else -> 20
            }
        } ?: 0
        sb.append("🎯 활동 수준: $activityScore/100점 ${getScoreEmoji(activityScore)}\n")
        
        // 정보 완성도
        val infoScore = calculateInfoCompleteness(clubData)
        sb.append("📋 정보 완성도: $infoScore/100점 ${getScoreEmoji(infoScore)}\n\n")
        
        // 🌟 종합 평가 및 피드백
        sb.append("🌟 종합 평가 및 피드백\n")
        when {
            overallScore >= 85 -> {
                sb.append("🏆 우수한 동아리 운영!\n")
                sb.append("• 모든 영역에서 높은 수준의 관리가 이뤄지고 있습니다\n")
                sb.append("• 현재 운영 방식을 지속하며 세부적인 개선에 집중하세요\n")
                sb.append("• 다른 동아리의 벤치마킹 대상이 될 수 있습니다\n")
                sb.append("• 멤버들의 만족도가 매우 높을 것으로 예상됩니다\n")
            }
            overallScore >= 70 -> {
                sb.append("👍 양호한 운영 상태\n")
                sb.append("• 전반적으로 안정적인 동아리 운영이 이뤄지고 있습니다\n")
                sb.append("• 몇 가지 영역에서 개선 여지가 있습니다\n")
                sb.append("• 체계적인 발전 계획 수립을 권장합니다\n")
                sb.append("• 멤버들의 의견을 반영한 개선 방안을 모색하세요\n")
            }
            overallScore >= 50 -> {
                sb.append("⚠️ 개선이 필요한 상태\n")
                sb.append("• 여러 영역에서 주의가 필요한 상황입니다\n")
                sb.append("• 우선순위를 정해 단계적으로 개선해나가세요\n")
                sb.append("• 임원진 회의를 통한 구체적인 대책 마련이 필요합니다\n")
                sb.append("• 멤버들과의 소통을 늘려 참여도를 높여보세요\n")
            }
            else -> {
                sb.append("🚨 전면적인 개선 필요\n")
                sb.append("• 동아리 운영 전반에 대한 재검토가 필요합니다\n")
                sb.append("• 기본적인 시스템부터 차근차근 구축하세요\n")
                sb.append("• 전문가나 선배 동아리의 조언을 구하는 것을 권장합니다\n")
                sb.append("• 작은 목표부터 시작해 점진적으로 발전시켜나가세요\n")
            }
        }
        
        // 📊 강점 및 개선점
        sb.append("\n📊 강점 및 개선점 분석\n")
        
        // 강점 분석
        val strengths = mutableListOf<String>()
        if (financialScore >= 70) strengths.add("재정 관리")
        if (activityScore >= 70) strengths.add("활동 기획")
        if (infoScore >= 70) strengths.add("정보 관리")
        
        if (strengths.isNotEmpty()) {
            sb.append("💪 주요 강점:\n")
            strengths.forEach { strength ->
                sb.append("• $strength: 우수한 수준으로 관리되고 있습니다 ✨\n")
            }
        }
        
        // 개선점 분석
        val improvements = mutableListOf<String>()
        if (financialScore < 60) improvements.add("재정 관리 체계화")
        if (activityScore < 60) improvements.add("활동 다양화 및 빈도 증가")
        if (infoScore < 60) improvements.add("기본 정보 및 기록 관리")
        
        if (improvements.isNotEmpty()) {
            sb.append("\n🎯 개선 권장사항:\n")
            improvements.forEach { improvement ->
                sb.append("• $improvement: 우선적으로 개선이 필요합니다 📈\n")
            }
        }
        
        // 📅 단계별 발전 로드맵
        sb.append("\n📅 3개월 발전 로드맵\n")
        sb.append("1️⃣ 1개월차: ${getMonthlyGoal(overallScore, 1)}\n")
        sb.append("2️⃣ 2개월차: ${getMonthlyGoal(overallScore, 2)}\n")
        sb.append("3️⃣ 3개월차: ${getMonthlyGoal(overallScore, 3)}\n")
    }
    
    private fun generateComparisonAnalysis(sb: StringBuilder, clubData: AIReportDataCollector.ClubReportData) {
        sb.append("🏆 동종 동아리 비교 분석\n")
        sb.append("=".repeat(30) + "\n\n")
        
        sb.append("📊 벤치마킹 분석 결과\n")
        sb.append("(${clubData.clubInfo?.majorCategory ?: "일반"} 분야 동아리 대비)\n\n")
        
        // 시뮬레이션된 비교 데이터
        clubData.financialSummary?.let { summary ->
            val avgIncome = 600000L // 평균 수입
            val avgExpense = 500000L // 평균 지출
            val avgTransactions = 25 // 평균 거래 수
            
            val incomeRatio = (summary.totalIncome.toDouble() / avgIncome * 100).toInt()
            val expenseRatio = (summary.totalExpense.toDouble() / avgExpense * 100).toInt()
            val transactionRatio = (summary.transactionCount.toDouble() / avgTransactions * 100).toInt()
            
            sb.append("💰 재정 현황 비교\n")
            sb.append("• 수입 수준: 동종 동아리 대비 ${incomeRatio}% ${getComparisonEmoji(incomeRatio)}\n")
            sb.append("• 지출 수준: 동종 동아리 대비 ${expenseRatio}% ${getComparisonEmoji(expenseRatio)}\n")
            sb.append("• 거래 활성도: 동종 동아리 대비 ${transactionRatio}% ${getComparisonEmoji(transactionRatio)}\n\n")
            
            // 상세 비교 분석
            sb.append("📈 상세 비교 분석\n")
            when {
                incomeRatio >= 120 -> sb.append("• 수입: 상위 20% 우수 동아리 수준 🏆\n")
                incomeRatio >= 100 -> sb.append("• 수입: 평균 이상의 안정적인 수준 👍\n")
                incomeRatio >= 80 -> sb.append("• 수입: 평균 수준으로 무난함 📊\n")
                else -> sb.append("• 수입: 평균 이하로 개선 필요 📈\n")
            }
            
            when {
                expenseRatio <= 80 -> sb.append("• 지출: 효율적인 예산 관리 우수 💎\n")
                expenseRatio <= 100 -> sb.append("• 지출: 적정 수준의 예산 집행 ✅\n")
                expenseRatio <= 120 -> sb.append("• 지출: 약간 높은 편, 절약 검토 필요 ⚠️\n")
                else -> sb.append("• 지출: 과도한 지출, 즉시 조정 필요 🚨\n")
            }
        }
        
        clubData.events?.let { events ->
            val avgEvents = 6 // 평균 행사 수
            val eventRatio = (events.size.toDouble() / avgEvents * 100).toInt()
            
            sb.append("\n🎯 활동 현황 비교\n")
            sb.append("• 행사 빈도: 동종 동아리 대비 ${eventRatio}% ${getComparisonEmoji(eventRatio)}\n")
            
            when {
                events.size >= avgEvents * 1.5 -> sb.append("• 활동량: 매우 활발한 상위 10% 동아리 🔥\n")
                events.size >= avgEvents -> sb.append("• 활동량: 평균 이상의 활발한 운영 ⚡\n")
                events.size >= avgEvents * 0.7 -> sb.append("• 활동량: 평균 수준의 적정 활동 📊\n")
                else -> sb.append("• 활동량: 평균 이하로 활성화 필요 📈\n")
            }
        }
        
        // 🎯 벤치마킹 포인트
        sb.append("\n🎯 성공 동아리 벤치마킹 포인트\n")
        sb.append("• 📊 데이터 기반 의사결정: 정기적인 현황 분석 및 개선\n")
        sb.append("• 💰 투명한 재정 관리: 수입원 다각화 및 효율적 지출\n")
        sb.append("• 🎪 다양한 활동 기획: 멤버 니즈 반영 및 창의적 기획\n")
        sb.append("• 🤝 적극적인 소통: 정기 회의 및 피드백 시스템\n")
        sb.append("• 📈 지속적인 발전: 장기 비전 수립 및 단계적 성장\n")
        
        // 🏅 동아리 등급 평가
        val overallScore = calculateOverallScore(clubData)
        sb.append("\n🏅 종합 등급 평가\n")
        val (grade, tier) = when {
            overallScore >= 90 -> "S등급" to "최우수 동아리"
            overallScore >= 80 -> "A등급" to "우수 동아리"
            overallScore >= 70 -> "B등급" to "양호한 동아리"
            overallScore >= 60 -> "C등급" to "보통 동아리"
            else -> "D등급" to "개선 필요 동아리"
        }
        sb.append("• 현재 등급: $grade ($tier) ${getGradeEmoji(overallScore)}\n")
        sb.append("• 상위 동아리로 발전하기 위한 맞춤 전략을 제시해드립니다\n")
    }
    
    private fun generateGeneralAnalysis(sb: StringBuilder, clubData: AIReportDataCollector.ClubReportData) {
        sb.append("📋 종합 현황 분석\n")
        sb.append("=".repeat(30) + "\n\n")
        
        // 간단한 재정 + 활동 현황
        generateFinancialAnalysis(sb, clubData)
        sb.append("\n\n")
        generateActivityAnalysis(sb, clubData)
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
    
    private fun getTrendEmoji(trend: String): String = when (trend) {
        "매우 양호" -> "🌟"
        "양호" -> "😊"
        "주의 필요" -> "⚠️"
        "위험" -> "🚨"
        else -> "📊"
    }
    
    private fun getComparisonEmoji(ratio: Int): String = when {
        ratio >= 120 -> "🔥"
        ratio >= 100 -> "👍"
        ratio >= 80 -> "📊"
        else -> "📈"
    }
    
    private fun getGradeEmoji(score: Int): String = when {
        score >= 90 -> "🏆"
        score >= 80 -> "🥇"
        score >= 70 -> "🥈"
        score >= 60 -> "🥉"
        else -> "📈"
    }
    
    private fun getGradeFromScore(score: Int): String = when {
        score >= 90 -> "S등급"
        score >= 80 -> "A등급"
        score >= 70 -> "B등급"
        score >= 60 -> "C등급"
        else -> "D등급"
    }
    
    private fun getMonthlyGoal(score: Int, month: Int): String = when (month) {
        1 -> if (score < 60) "기본 체계 구축 및 현황 파악" else "현재 강점 유지 및 약점 분석"
        2 -> if (score < 60) "우선순위 개선사항 실행" else "개선 계획 실행 및 중간 점검"
        3 -> if (score < 60) "성과 평가 및 다음 단계 계획" else "성과 평가 및 고도화 전략 수립"
        else -> "지속적인 개선 및 발전"
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