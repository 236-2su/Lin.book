package com.example.myapplication.ai

import com.example.myapplication.api.ApiClient
import com.example.myapplication.ClubItem
import com.example.myapplication.LedgerApiItem
import com.example.myapplication.TransactionItem
import com.example.myapplication.api.ApiService.EventItem
import com.example.myapplication.BoardItem
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import android.util.Log
import org.json.JSONObject
import org.json.JSONArray
import kotlin.math.roundToInt

class AIReportDataCollector(private val context: Context) {
    
    data class ClubReportData(
        val clubInfo: ClubItem?,
        val ledgerData: List<LedgerApiItem>?,
        val transactions: List<TransactionItem>?,
        val events: List<EventItem>?,
        val boards: List<BoardItem>?,
        val financialSummary: FinancialSummary?
    )
    
    data class FinancialSummary(
        val totalIncome: Long,
        val totalExpense: Long,
        val netAmount: Long,
        val transactionCount: Int,
        val averageTransactionAmount: Long,
        val monthlyTrend: String
    )
    
    // Enhanced data structures for refined analysis
    data class RefinedFinancialSummary(
        val totalIncome: Int,
        val totalExpense: Int, 
        val netProfit: Int,
        val profitMargin: Double,
        val activeMonths: Int,
        val avgMonthlyIncome: Double,
        val avgMonthlyExpense: Double,
        val cashFlowHealth: String
    )

    data class SpendingPattern(
        val topExpenseTypes: List<Pair<String, Int>>,
        val seasonalTrends: Map<String, Double>,
        val paymentMethodDistribution: Map<String, Int>,
        val eventSpending: Map<String, Int>,
        val riskFactors: List<String>
    )

    data class TrendAnalysis(
        val monthlyGrowth: List<Double>,
        val cashFlowTrend: String, // "improving", "stable", "declining"
        val busyMonths: List<String>,
        val quietMonths: List<String>,
        val yearOverYearComparison: String?,
        val performanceScore: Double
    )

    data class AIAnalysisInput(
        val financialSummary: RefinedFinancialSummary,
        val spendingPatterns: SpendingPattern,
        val trends: TrendAnalysis,
        val contextualInfo: Map<String, Any>,
        val rawDataSize: Int,
        val dataQuality: String
    )
    
    suspend fun collectClubData(clubId: Int, selectedSources: List<String>): ClubReportData = withContext(Dispatchers.IO) {
        val apiService = ApiClient.getApiService()
        
        // 병렬로 데이터 수집
        coroutineScope {
            val clubInfoDeferred = async { 
                try { 
                    Log.d("AIReportDataCollector", "클럽 정보 수집 시작: $clubId")
                    val response = apiService.getClubDetail(clubId).execute()
                    if (response.isSuccessful) {
                        Log.d("AIReportDataCollector", "클럽 정보 수집 성공")
                        response.body()
                    } else {
                        Log.e("AIReportDataCollector", "클럽 정보 수집 실패: ${response.code()}")
                        null
                    }
                } 
                catch (e: Exception) { 
                    Log.e("AIReportDataCollector", "클럽 정보 수집 예외", e)
                    null 
                } 
            }
            
            val ledgerDataDeferred = async {
                if (selectedSources.contains("ledger")) {
                    try { 
                        Log.d("AIReportDataCollector", "장부 데이터 수집 시작")
                        val response = apiService.getLedgerList(clubId).execute()
                        if (response.isSuccessful) {
                            Log.d("AIReportDataCollector", "장부 데이터 수집 성공: ${response.body()?.size}개")
                            response.body()
                        } else {
                            Log.e("AIReportDataCollector", "장부 데이터 수집 실패: ${response.code()}")
                            null
                        }
                    }
                    catch (e: Exception) { 
                        Log.e("AIReportDataCollector", "장부 데이터 수집 예외", e)
                        null 
                    }
                } else null
            }
            
            val eventsDeferred = async {
                if (selectedSources.contains("events")) {
                    try { 
                        Log.d("AIReportDataCollector", "이벤트 데이터 수집 시작")
                        val response = apiService.getEventList(clubId).execute()
                        if (response.isSuccessful) {
                            Log.d("AIReportDataCollector", "이벤트 데이터 수집 성공: ${response.body()?.size}개")
                            response.body()
                        } else {
                            Log.e("AIReportDataCollector", "이벤트 데이터 수집 실패: ${response.code()}")
                            null
                        }
                    }
                    catch (e: Exception) { 
                        Log.e("AIReportDataCollector", "이벤트 데이터 수집 예외", e)
                        null 
                    }
                } else null
            }
            
            // 장부가 있으면 거래 내역도 수집
            val ledgerData = ledgerDataDeferred.await()
            val transactionsDeferred = async {
                if (ledgerData?.isNotEmpty() == true && selectedSources.contains("ledger")) {
                    try { 
                        val firstLedger = ledgerData.first()
                        val userPk = getUserPk() 
                        Log.d("AIReportDataCollector", "거래 내역 수집 시작: ledger=${firstLedger.id}, user=$userPk")
                        val response = apiService.getTransactions(clubId, firstLedger.id, userPk).execute()
                        if (response.isSuccessful) {
                            Log.d("AIReportDataCollector", "거래 내역 수집 성공: ${response.body()?.size}개")
                            response.body()
                        } else {
                            Log.e("AIReportDataCollector", "거래 내역 수집 실패: ${response.code()}")
                            null
                        }
                    }
                    catch (e: Exception) { 
                        Log.e("AIReportDataCollector", "거래 내역 수집 예외", e)
                        null 
                    }
                } else null
            }
            
            val transactions = transactionsDeferred.await()
            val financialSummary = transactions?.let { calculateFinancialSummary(it) }
            
            ClubReportData(
                clubInfo = clubInfoDeferred.await(),
                ledgerData = ledgerData,
                transactions = transactions,
                events = eventsDeferred.await(),
                boards = null, // 게시판 API 구현 시 추가
                financialSummary = financialSummary
            )
        }
    }
    
    private fun calculateFinancialSummary(transactions: List<TransactionItem>): FinancialSummary {
        val totalIncome = transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.amount < 0 }.sumOf { Math.abs(it.amount) }
        val netAmount = totalIncome - totalExpense
        val averageAmount = if (transactions.isNotEmpty()) {
            transactions.sumOf { Math.abs(it.amount) } / transactions.size
        } else 0L
        
        val trend = when {
            netAmount > 50000 -> "매우 양호"
            netAmount > 0 -> "양호"
            netAmount > -50000 -> "주의 필요"
            else -> "위험"
        }
        
        return FinancialSummary(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netAmount = netAmount,
            transactionCount = transactions.size,
            averageTransactionAmount = averageAmount,
            monthlyTrend = trend
        )
    }
    
    private fun getUserPk(): Int {
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getInt("user_pk", 1) // 기본값 1
    }
    
    // Enhanced method to collect and refine yearly report data
    suspend fun collectRefinedYearlyData(clubId: Int, ledgerId: Int, year: Int): AIAnalysisInput? = withContext(Dispatchers.IO) {
        try {
            Log.d("AIReportDataCollector", "🔍 정제된 연간 데이터 수집 시작 - 클럽: $clubId, 연도: $year")
            val apiService = ApiClient.getApiService()
            
            // Get yearly report data
            val yearlyResponse = apiService.createYearlyReport(clubId, ledgerId, year).execute()
            
            if (yearlyResponse.isSuccessful) {
                val responseBody = yearlyResponse.body()
                if (responseBody != null) {
                    Log.d("AIReportDataCollector", "✅ 연간 리포트 API 성공 - 데이터: ${responseBody}")
                    
                    // Parse the yearly data array (first report in array)
                    val jsonArray = JSONArray(responseBody)
                    if (jsonArray.length() > 0) {
                        val yearlyData = jsonArray.getJSONObject(0).getJSONObject("content")
                        
                        // Get club context
                        val clubResponse = apiService.getClubDetail(clubId).execute()
                        val clubData = if (clubResponse.isSuccessful) {
                            JSONObject().apply {
                                val club = clubResponse.body()
                                put("name", club?.name ?: "Unknown Club")
                                put("short_description", club?.shortDescription ?: "")
                                put("member_count", 0) // Add member count if available
                            }
                        } else JSONObject()
                        
                        // Process and refine the data
                        val aiInput = prepareDataForAI(yearlyData, clubData, year)
                        
                        Log.d("AIReportDataCollector", "🎯 AI 분석 입력 데이터 준비 완료:")
                        Log.d("AIReportDataCollector", "  - 수익률: ${aiInput.financialSummary.profitMargin.roundToInt()}%")
                        Log.d("AIReportDataCollector", "  - 활성 월수: ${aiInput.financialSummary.activeMonths}")
                        Log.d("AIReportDataCollector", "  - 현금흐름 상태: ${aiInput.financialSummary.cashFlowHealth}")
                        Log.d("AIReportDataCollector", "  - 데이터 품질: ${aiInput.dataQuality}")
                        
                        return@withContext aiInput
                    }
                }
            }
            
            Log.e("AIReportDataCollector", "❌ 연간 리포트 API 실패: ${yearlyResponse.code()}")
            return@withContext null
            
        } catch (e: Exception) {
            Log.e("AIReportDataCollector", "❌ 정제된 데이터 수집 중 오류", e)
            return@withContext null
        }
    }
    
    // Core data refinement functions
    private fun prepareDataForAI(yearlyData: JSONObject, clubContext: JSONObject, year: Int): AIAnalysisInput {
        val financialSummary = extractFinancialSummary(yearlyData)
        val spendingPatterns = analyzeSpendingPatterns(yearlyData)
        val trends = analyzeTrends(yearlyData)
        
        val contextualInfo = mapOf(
            "club_name" to clubContext.optString("name", "Unknown"),
            "club_description" to clubContext.optString("short_description", ""),
            "analysis_year" to year,
            "data_processed_at" to System.currentTimeMillis()
        )
        
        val rawDataSize = yearlyData.toString().length
        val dataQuality = assessDataQuality(financialSummary, spendingPatterns)
        
        return AIAnalysisInput(
            financialSummary, spendingPatterns, trends,
            contextualInfo, rawDataSize, dataQuality
        )
    }
    
    private fun extractFinancialSummary(yearlyData: JSONObject): RefinedFinancialSummary {
        val summary = yearlyData.optJSONObject("summary") ?: JSONObject()
        val byMonth = yearlyData.optJSONObject("by_month") ?: JSONObject()
        
        val totalIncome = summary.optInt("income", 0)
        val totalExpense = summary.optInt("expense", 0)
        val netProfit = summary.optInt("net", 0)
        val profitMargin = if (totalIncome > 0) (netProfit.toDouble() / totalIncome) * 100 else 0.0
        
        // Count active months
        val activeMonths = countActiveMonths(byMonth)
        val avgMonthlyIncome = if (activeMonths > 0) totalIncome.toDouble() / activeMonths else 0.0
        val avgMonthlyExpense = if (activeMonths > 0) totalExpense.toDouble() / activeMonths else 0.0
        
        val cashFlowHealth = when {
            netProfit > 500000 -> "매우 건강"
            netProfit > 100000 -> "건강"
            netProfit > 0 -> "양호"
            netProfit > -100000 -> "주의 필요"
            else -> "위험"
        }
        
        return RefinedFinancialSummary(
            totalIncome, totalExpense, netProfit, profitMargin,
            activeMonths, avgMonthlyIncome, avgMonthlyExpense, cashFlowHealth
        )
    }
    
    private fun analyzeSpendingPatterns(yearlyData: JSONObject): SpendingPattern {
        val byMonth = yearlyData.optJSONObject("by_month") ?: JSONObject()
        
        val expenseTypeMap = mutableMapOf<String, Int>()
        val paymentMethodMap = mutableMapOf<String, Int>()
        val eventMap = mutableMapOf<String, Int>()
        val seasonalData = mutableMapOf<String, Double>()
        
        // Process each month's data
        for (month in 1..12) {
            val monthData = byMonth.optJSONObject(month.toString())
            if (monthData != null && hasActivity(monthData)) {
                // Process expense types
                val byType = monthData.optJSONArray("by_type")
                if (byType != null) {
                    for (i in 0 until byType.length()) {
                        val typeData = byType.getJSONObject(i)
                        val type = typeData.getString("type")
                        val expense = typeData.getInt("expense")
                        expenseTypeMap[type] = expenseTypeMap.getOrDefault(type, 0) + expense
                    }
                }
                
                // Process payment methods
                val byPayment = monthData.optJSONArray("by_payment_method")
                if (byPayment != null) {
                    for (i in 0 until byPayment.length()) {
                        val paymentData = byPayment.getJSONObject(i)
                        val method = paymentData.getString("payment_method")
                        val expense = paymentData.getInt("expense")
                        paymentMethodMap[method] = paymentMethodMap.getOrDefault(method, 0) + expense
                    }
                }
                
                // Process events
                val byEvent = monthData.optJSONArray("by_event")
                if (byEvent != null) {
                    for (i in 0 until byEvent.length()) {
                        val eventData = byEvent.getJSONObject(i)
                        val eventName = eventData.getString("event_name")
                        val expense = eventData.getInt("expense")
                        eventMap[eventName] = eventMap.getOrDefault(eventName, 0) + expense
                    }
                }
                
                // Calculate seasonal trends
                val monthExpense = monthData.optJSONObject("summary")?.optInt("expense", 0) ?: 0
                val season = when (month) {
                    in 3..5 -> "봄"
                    in 6..8 -> "여름" 
                    in 9..11 -> "가을"
                    else -> "겨울"
                }
                seasonalData[season] = seasonalData.getOrDefault(season, 0.0) + monthExpense
            }
        }
        
        val riskFactors = identifyRiskFactors(expenseTypeMap, paymentMethodMap)
        
        return SpendingPattern(
            topExpenseTypes = expenseTypeMap.toList().sortedByDescending { it.second }.take(5),
            seasonalTrends = seasonalData,
            paymentMethodDistribution = paymentMethodMap,
            eventSpending = eventMap,
            riskFactors = riskFactors
        )
    }
    
    private fun analyzeTrends(yearlyData: JSONObject): TrendAnalysis {
        val byMonth = yearlyData.optJSONObject("by_month") ?: JSONObject()
        val monthlyNetValues = mutableListOf<Int>()
        val monthlyExpenses = mutableMapOf<String, Int>()
        
        // Extract monthly data
        for (month in 1..12) {
            val monthData = byMonth.optJSONObject(month.toString())
            val summary = monthData?.optJSONObject("summary")
            val net = summary?.optInt("net", 0) ?: 0
            val expense = summary?.optInt("expense", 0) ?: 0
            
            monthlyNetValues.add(net)
            if (expense > 0) {
                monthlyExpenses[getMonthName(month)] = expense
            }
        }
        
        val monthlyGrowth = calculateGrowthRates(monthlyNetValues)
        val cashFlowTrend = determineCashFlowTrend(monthlyGrowth)
        val busyMonths = identifyBusyMonths(monthlyExpenses)
        val quietMonths = identifyQuietMonths(monthlyExpenses)
        val performanceScore = calculatePerformanceScore(monthlyNetValues)
        
        return TrendAnalysis(
            monthlyGrowth = monthlyGrowth,
            cashFlowTrend = cashFlowTrend,
            busyMonths = busyMonths,
            quietMonths = quietMonths,
            yearOverYearComparison = null, // Can be enhanced with multi-year data
            performanceScore = performanceScore
        )
    }
    
    // Helper functions
    private fun countActiveMonths(byMonth: JSONObject): Int {
        var activeCount = 0
        for (month in 1..12) {
            val monthData = byMonth.optJSONObject(month.toString())
            if (monthData != null && hasActivity(monthData)) {
                activeCount++
            }
        }
        return activeCount
    }
    
    private fun hasActivity(monthData: JSONObject): Boolean {
        val summary = monthData.optJSONObject("summary")
        val income = summary?.optInt("income", 0) ?: 0
        val expense = summary?.optInt("expense", 0) ?: 0
        return income > 0 || expense > 0
    }
    
    private fun identifyRiskFactors(expenses: Map<String, Int>, payments: Map<String, Int>): List<String> {
        val risks = mutableListOf<String>()
        val totalExpense = expenses.values.sum()
        
        if (totalExpense == 0) return risks
        
        // Check for concentration risk
        expenses.forEach { (type, amount) ->
            if (amount.toDouble() / totalExpense > 0.5) {
                risks.add("$type 지출 집중도 높음 (${(amount.toDouble()/totalExpense*100).roundToInt()}%)")
            }
        }
        
        // Check payment method risks
        if (payments.size == 1) {
            risks.add("단일 결제수단 의존")
        }
        
        return risks
    }
    
    private fun calculateGrowthRates(values: List<Int>): List<Double> {
        val growthRates = mutableListOf<Double>()
        for (i in 1 until values.size) {
            val prev = values[i-1]
            val curr = values[i]
            val growth = if (prev != 0) ((curr - prev).toDouble() / prev * 100) else 0.0
            growthRates.add(growth)
        }
        return growthRates
    }
    
    private fun determineCashFlowTrend(growthRates: List<Double>): String {
        if (growthRates.isEmpty()) return "데이터 부족"
        
        val recentGrowth = growthRates.takeLast(3).average()
        return when {
            recentGrowth > 10 -> "개선"
            recentGrowth > -10 -> "안정"
            else -> "악화"
        }
    }
    
    private fun identifyBusyMonths(monthlyExpenses: Map<String, Int>): List<String> {
        val avgExpense = monthlyExpenses.values.average()
        return monthlyExpenses.filter { it.value > avgExpense * 1.2 }
            .toList().sortedByDescending { it.second }.take(3).map { it.first }
    }
    
    private fun identifyQuietMonths(monthlyExpenses: Map<String, Int>): List<String> {
        val avgExpense = monthlyExpenses.values.average()
        return monthlyExpenses.filter { it.value < avgExpense * 0.5 }
            .toList().sortedBy { it.second }.take(3).map { it.first }
    }
    
    private fun calculatePerformanceScore(monthlyValues: List<Int>): Double {
        val positiveMonths = monthlyValues.count { it > 0 }
        val consistency = if (monthlyValues.isNotEmpty()) positiveMonths.toDouble() / monthlyValues.size else 0.0
        return consistency * 100
    }
    
    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "1월"; 2 -> "2월"; 3 -> "3월"; 4 -> "4월"
            5 -> "5월"; 6 -> "6월"; 7 -> "7월"; 8 -> "8월"
            9 -> "9월"; 10 -> "10월"; 11 -> "11월"; 12 -> "12월"
            else -> "${month}월"
        }
    }
    
    private fun assessDataQuality(financial: RefinedFinancialSummary, patterns: SpendingPattern): String {
        val score = when {
            financial.activeMonths >= 8 && patterns.topExpenseTypes.size >= 3 -> "높음"
            financial.activeMonths >= 4 && patterns.topExpenseTypes.size >= 2 -> "보통"
            financial.activeMonths >= 1 -> "낮음"
            else -> "매우 낮음"
        }
        return score
    }
}
