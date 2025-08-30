package com.example.myapplication

import android.util.Log
import org.json.JSONObject
import java.text.NumberFormat
import java.util.*

/**
 * 백엔드 리포트 데이터를 프론트엔드 형식으로 효율적으로 변환하는 클래스
 */
class BackendReportConverter {
    
    companion object {
        private const val TAG = "BackendReportConverter"
    }
    
    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    
    /**
     * 백엔드 리포트를 프론트엔드 JSON 형식으로 변환합니다
     */
    fun convert(backendReport: com.example.myapplication.api.ApiService.BackendReportItem, type: String): String {
        try {
            Log.d(TAG, "리포트 변환 시작: ${backendReport.title}")
            
            val formattedContent = when (type) {
                "yearly" -> formatYearlyReport(backendReport.content)
                "monthly" -> formatMonthlyReport(backendReport.content)
                "comparison" -> formatComparisonReport(backendReport.content)
                "three_year_comparison" -> formatThreeYearComparisonReport(backendReport.content)
                else -> formatGeneralReport(backendReport.content)
            }
            
            val reportJson = JSONObject().apply {
                put("id", backendReport.id)
                put("title", backendReport.title)
                put("content", formattedContent)
                put("type", type)
                put("created_at", System.currentTimeMillis())
                put("creator", "AI 시스템")
                put("backend_id", backendReport.id)
            }
            
            Log.d(TAG, "리포트 변환 완료: ${backendReport.title}")
            return reportJson.toString()
            
        } catch (e: Exception) {
            Log.e(TAG, "리포트 변환 실패: ${backendReport.title}", e)
            throw e
        }
    }
    
    /**
     * JSON 배열에서 3년간 데이터를 처리하여 비교 리포트 생성
     */
    fun convertThreeYearLedgerData(
        data2023: String,
        data2024: String, 
        data2025: String
    ): String {
        try {
            Log.d(TAG, "3년간 비교 리포트 변환 시작")
            
            val year2023 = parseJsonLedgerReport(data2023)
            val year2024 = parseJsonLedgerReport(data2024)
            val year2025 = parseJsonLedgerReport(data2025)
            
            val comparisonContent = formatThreeYearData(
                mapOf(
                    2023 to year2023,
                    2024 to year2024,
                    2025 to year2025
                )
            )
            
            val reportJson = JSONObject().apply {
                put("id", System.currentTimeMillis())
                put("title", "SSAFY 앱메이커 3년간 재정 비교 분석")
                put("content", comparisonContent)
                put("type", "three_year_comparison")
                put("created_at", System.currentTimeMillis())
                put("creator", "AI 시스템 (3년 비교)")
                put("backend_id", -1)
            }
            
            Log.d(TAG, "3년간 비교 리포트 변환 완료")
            return reportJson.toString()
            
        } catch (e: Exception) {
            Log.e(TAG, "3년간 비교 리포트 변환 실패", e)
            throw e
        }
    }
    
    /**
     * JSON 문자열에서 ledger report 데이터 파싱
     */
    private fun parseJsonLedgerReport(jsonData: String): Map<String, Any> {
        try {
            val jsonArray = org.json.JSONArray(jsonData)
            if (jsonArray.length() > 0) {
                val reportObject = jsonArray.getJSONObject(0)
                val content = reportObject.getJSONObject("content")
                
                // content를 Map으로 변환
                val result = mutableMapOf<String, Any>()
                val keys = content.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    result[key] = content.get(key)
                }
                return result
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON 파싱 실패", e)
        }
        return emptyMap()
    }
    
    /**
     * 연간 리포트 포맷팅
     */
    private fun formatYearlyReport(content: Map<String, Any>): String {
        val year = content["year"] as? Int ?: 2025
        val summary = content["summary"] as? Map<String, Any> ?: emptyMap()
        val income = (summary["income"] as? Number)?.toInt() ?: 0
        val expense = (summary["expense"] as? Number)?.toInt() ?: 0
        val net = (summary["net"] as? Number)?.toInt() ?: 0
        
        return buildString {
            appendLine("📊 ${year}년 연간 재정 분석 리포트")
            appendLine()
            appendLine("💰 재정 현황 요약")
            appendLine("• 총 수입: ${formatAmount(income)}")
            appendLine("• 총 지출: ${formatAmount(expense)}")
            appendLine("• 순수익: ${formatAmount(net)}")
            appendLine()
            
            // 거래 유형별 분석
            formatByTypeSection(content, this)
            
            // 결제 수단별 분석
            formatByPaymentSection(content, this)
            
            // 월별 추이 분석
            formatMonthlyTrendsSection(content, this)
            
            appendLine("✨ ${year}년 동아리 활동이 성공적으로 마무리되었습니다.")
        }
    }
    
    /**
     * 월간 리포트 포맷팅
     */
    private fun formatMonthlyReport(content: Map<String, Any>): String {
        val year = content["year"] as? Int ?: 2025
        val month = content["month"] as? Int ?: 1
        val summary = content["summary"] as? Map<String, Any> ?: emptyMap()
        val income = (summary["income"] as? Number)?.toInt() ?: 0
        val expense = (summary["expense"] as? Number)?.toInt() ?: 0
        val net = (summary["net"] as? Number)?.toInt() ?: 0
        
        return buildString {
            appendLine("📅 ${year}년 ${month}월 월간 재정 분석")
            appendLine()
            appendLine("💰 이번 달 재정 현황")
            appendLine("• 총 수입: ${formatAmount(income)}")
            appendLine("• 총 지출: ${formatAmount(expense)}")
            appendLine("• 순수익: ${formatAmount(net)}")
            appendLine()
            
            // 거래 유형별 분석
            formatByTypeSection(content, this)
            
            // 이벤트별 분석
            formatByEventSection(content, this)
            
            // 일별 추이
            formatDailyTrendsSection(content, this)
            
            appendLine("📈 ${month}월 동아리 활동 분석이 완료되었습니다.")
        }
    }
    
    /**
     * 비교 리포트 포맷팅
     */
    private fun formatComparisonReport(content: Map<String, Any>): String {
        return buildString {
            appendLine("🏆 동아리 비교 분석 리포트")
            appendLine()
            appendLine("📊 유사한 동아리들과의 재정 비교")
            appendLine("• 이 분석은 비슷한 규모와 활동을 하는 동아리들과 비교한 결과입니다.")
            appendLine()
            
            // 기본 재정 정보만 표시
            val summary = content["summary"] as? Map<String, Any>
            if (summary != null) {
                val income = (summary["income"] as? Number)?.toInt() ?: 0
                val expense = (summary["expense"] as? Number)?.toInt() ?: 0
                val net = (summary["net"] as? Number)?.toInt() ?: 0
                
                appendLine("💰 우리 동아리 재정 현황")
                appendLine("• 총 수입: ${formatAmount(income)}")
                appendLine("• 총 지출: ${formatAmount(expense)}")
                appendLine("• 순수익: ${formatAmount(net)}")
                appendLine()
            }
            
            appendLine("🎯 비교 분석 결과")
            appendLine("• 동아리별 상세 비교 데이터는 별도 분석을 통해 제공됩니다.")
            appendLine("• 재정 운영 효율성 및 개선 방안이 포함되어 있습니다.")
        }
    }
    
    /**
     * 일반 리포트 포맷팅
     */
    private fun formatGeneralReport(content: Map<String, Any>): String {
        return buildString {
            appendLine("📋 재정 분석 리포트")
            appendLine()
            appendLine("📊 기본 재정 정보")
            
            val summary = content["summary"] as? Map<String, Any>
            if (summary != null) {
                val income = (summary["income"] as? Number)?.toInt() ?: 0
                val expense = (summary["expense"] as? Number)?.toInt() ?: 0
                val net = (summary["net"] as? Number)?.toInt() ?: 0
                
                appendLine("• 총 수입: ${formatAmount(income)}")
                appendLine("• 총 지출: ${formatAmount(expense)}")
                appendLine("• 순수익: ${formatAmount(net)}")
                appendLine()
            }
            
            appendLine("✨ 상세 분석 데이터를 확인하여 동아리 운영에 활용하세요.")
        }
    }
    
    /**
     * 거래 유형별 섹션 포맷팅
     */
    private fun formatByTypeSection(content: Map<String, Any>, builder: StringBuilder) {
        val byTypeList = content["by_type"] as? List<Map<String, Any>>
        if (!byTypeList.isNullOrEmpty()) {
            builder.appendLine("📋 거래 유형별 분석")
            byTypeList.take(5).forEach { typeData -> // 상위 5개만 표시
                val typeName = typeData["type"] as? String ?: "기타"
                val typeIncome = (typeData["income"] as? Number)?.toInt() ?: 0
                val typeExpense = (typeData["expense"] as? Number)?.toInt() ?: 0
                builder.appendLine("• $typeName: 수입 ${formatAmount(typeIncome)}, 지출 ${formatAmount(typeExpense)}")
            }
            builder.appendLine()
        }
    }
    
    /**
     * 결제 수단별 섹션 포맷팅
     */
    private fun formatByPaymentSection(content: Map<String, Any>, builder: StringBuilder) {
        val byPaymentList = content["by_payment_method"] as? List<Map<String, Any>>
        if (!byPaymentList.isNullOrEmpty()) {
            builder.appendLine("💳 결제 수단별 분석")
            byPaymentList.take(3).forEach { paymentData -> // 상위 3개만 표시
                val method = paymentData["payment_method"] as? String ?: "기타"
                val methodIncome = (paymentData["income"] as? Number)?.toInt() ?: 0
                val methodExpense = (paymentData["expense"] as? Number)?.toInt() ?: 0
                builder.appendLine("• $method: 수입 ${formatAmount(methodIncome)}, 지출 ${formatAmount(methodExpense)}")
            }
            builder.appendLine()
        }
    }
    
    /**
     * 이벤트별 섹션 포맷팅
     */
    private fun formatByEventSection(content: Map<String, Any>, builder: StringBuilder) {
        val byEventList = content["by_event"] as? List<Map<String, Any>>
        if (!byEventList.isNullOrEmpty()) {
            builder.appendLine("🎯 행사별 분석")
            byEventList.take(5).forEach { eventData -> // 상위 5개만 표시
                val eventName = eventData["event_name"] as? String ?: "일반 활동"
                val eventIncome = (eventData["income"] as? Number)?.toInt() ?: 0
                val eventExpense = (eventData["expense"] as? Number)?.toInt() ?: 0
                val eventNet = eventIncome - eventExpense
                builder.appendLine("• $eventName: 순수익 ${formatAmount(eventNet)}")
            }
            builder.appendLine()
        }
    }
    
    /**
     * 월별 추이 섹션 포맷팅
     */
    private fun formatMonthlyTrendsSection(content: Map<String, Any>, builder: StringBuilder) {
        val byMonthData = content["by_month"] as? Map<String, Any>
        if (byMonthData != null) {
            builder.appendLine("📈 월별 재정 추이")
            
            // 주요 월별 데이터만 요약 표시
            val months = byMonthData.keys.sortedBy { it.toIntOrNull() ?: 0 }
            months.take(4).forEach { monthKey ->
                try {
                    val monthData = byMonthData[monthKey] as? Map<String, Any>
                    val summary = monthData?.get("summary") as? Map<String, Any>
                    if (summary != null) {
                        val net = (summary["net"] as? Number)?.toInt() ?: 0
                        builder.appendLine("• ${monthKey}월: 순수익 ${formatAmount(net)}")
                    }
                } catch (e: Exception) {
                    // 개별 월 데이터 파싱 오류는 무시
                }
            }
            builder.appendLine()
        }
    }
    
    /**
     * 일별 추이 섹션 포맷팅 (간소화)
     */
    private fun formatDailyTrendsSection(content: Map<String, Any>, builder: StringBuilder) {
        val dailySeries = content["daily_series"] as? List<Map<String, Any>>
        if (!dailySeries.isNullOrEmpty()) {
            builder.appendLine("📊 주요 거래일 분석")
            
            // 거래가 많았던 상위 3일만 표시
            val significantDays = dailySeries
                .filter { (it["total"] as? Number)?.toInt() != 0 }
                .sortedByDescending { (it["total"] as? Number)?.toInt() ?: 0 }
                .take(3)
                
            if (significantDays.isNotEmpty()) {
                significantDays.forEach { dayData ->
                    val date = dayData["date"] as? String ?: "날짜미상"
                    val total = (dayData["total"] as? Number)?.toInt() ?: 0
                    builder.appendLine("• $date: ${formatAmount(total)}")
                }
                builder.appendLine()
            }
        }
    }
    
    /**
     * 3년간 비교 리포트 포맷팅
     */
    private fun formatThreeYearComparisonReport(content: Map<String, Any>): String {
        return buildString {
            appendLine("📊 3년간 재정 비교 분석 리포트")
            appendLine()
            appendLine("이 리포트는 실제 데이터를 기반으로 생성되었습니다.")
            appendLine()
            
            // 기본 정보 표시
            val year = content["year"] as? Int ?: 2025
            val summary = content["summary"] as? Map<String, Any>
            if (summary != null) {
                val income = (summary["income"] as? Number)?.toInt() ?: 0
                val expense = (summary["expense"] as? Number)?.toInt() ?: 0  
                val net = (summary["net"] as? Number)?.toInt() ?: 0
                
                appendLine("💰 ${year}년 재정 현황")
                appendLine("• 총 수입: ${formatAmount(income)}")
                appendLine("• 총 지출: ${formatAmount(expense)}")
                appendLine("• 순수익: ${formatAmount(net)}")
                appendLine()
            }
            
            // 거래 유형별 분석
            formatByTypeSection(content, this)
            
            appendLine("📈 상세 3년간 비교 분석은 전용 기능에서 확인하세요.")
        }
    }
    
    /**
     * 3년간 데이터를 종합하여 포맷팅
     */
    private fun formatThreeYearData(yearlyData: Map<Int, Map<String, Any>>): String {
        return buildString {
            appendLine("📊 SSAFY 앱메이커 3년간 재정 비교 분석")
            appendLine("━".repeat(26))
            appendLine("📅 분석기간: 2023년 ~ 2025년 (3년간)")
            appendLine("🔍 데이터 출처: 실제 장부 데이터 기반")
            appendLine()
            
            // 1. 연도별 재정 현황
            appendLine("💰 연도별 재정 현황 비교")
            appendLine("━".repeat(26))
            
            val summaryData = mutableMapOf<Int, Triple<Long, Long, Long>>()
            
            yearlyData.forEach { (year, data) ->
                val summary = data["summary"] as? Map<String, Any> ?: emptyMap()
                val income = (summary["income"] as? Number)?.toLong() ?: 0L
                val expense = (summary["expense"] as? Number)?.toLong() ?: 0L
                val net = income - expense
                
                summaryData[year] = Triple(income, expense, net)
                
                appendLine("📅 ${year}년")
                appendLine("  • 총 수입: ${formatAmount(income.toInt())}")
                appendLine("  • 총 지출: ${formatAmount(expense.toInt())}")  
                appendLine("  • 순수익: ${formatAmount(net.toInt())} ${if (net >= 0) "🟢" else "🔴"}")
                appendLine()
            }
            
            // 2. 성장률 분석
            if (summaryData.size >= 2) {
                appendLine("📈 연도별 성장률 분석")
                appendLine("━".repeat(26))
                
                val years = summaryData.keys.sorted()
                for (i in 1 until years.size) {
                    val prevYear = years[i-1]
                    val currentYear = years[i]
                    val prevData = summaryData[prevYear]!!
                    val currentData = summaryData[currentYear]!!
                    
                    val incomeGrowth = if (prevData.first > 0) {
                        ((currentData.first - prevData.first).toDouble() / prevData.first * 100).toInt()
                    } else 0
                    
                    val expenseGrowth = if (prevData.second > 0) {
                        ((currentData.second - prevData.second).toDouble() / prevData.second * 100).toInt()
                    } else 0
                    
                    appendLine("📊 ${prevYear}년 → ${currentYear}년 변화:")
                    appendLine("  • 수입 증감: ${if (incomeGrowth >= 0) "+" else ""}${incomeGrowth}%")
                    appendLine("  • 지출 증감: ${if (expenseGrowth >= 0) "+" else ""}${expenseGrowth}%")
                    appendLine("  • 순수익 변화: ${formatAmount((currentData.third - prevData.third).toInt())}")
                    appendLine()
                }
            }
            
            // 3. 거래 유형별 3년간 비교
            appendLine("🏷️ 거래 유형별 3년간 비교")
            appendLine("━".repeat(26))
            
            val typeComparison = mutableMapOf<String, MutableMap<Int, Pair<Int, Int>>>()
            
            yearlyData.forEach { (year, data) ->
                val byType = data["by_type"] as? Map<String, Any> ?: emptyMap()
                byType.forEach { (typeName, typeData) ->
                    if (typeData is Map<*, *>) {
                        val income = (typeData["income"] as? Number)?.toInt() ?: 0
                        val expense = (typeData["expense"] as? Number)?.toInt() ?: 0
                        typeComparison.getOrPut(typeName) { mutableMapOf() }[year] = Pair(income, expense)
                    }
                }
            }
            
            typeComparison.forEach { (typeName, yearData) ->
                appendLine("📋 ${typeName}")
                yearData.keys.sorted().forEach { year ->
                    val data = yearData[year]!!
                    val net = data.first - data.second
                    appendLine("  ${year}년: 수입 ${formatAmount(data.first)}, 지출 ${formatAmount(data.second)}, 순액 ${formatAmount(net)}")
                }
                appendLine()
            }
            
            // 4. AI 분석 결론
            appendLine("🤖 AI 종합 분석 결론")
            appendLine("━".repeat(26))
            
            val totalYears = summaryData.keys.sorted()
            if (totalYears.size >= 2) {
                val firstYear = totalYears.first()
                val lastYear = totalYears.last()
                val firstYearNet = summaryData[firstYear]!!.third
                val lastYearNet = summaryData[lastYear]!!.third
                
                if (lastYearNet > firstYearNet) {
                    appendLine("✅ 긍정적 성장: ${firstYear}년 대비 ${lastYear}년 순수익이 증가했습니다.")
                    appendLine("💡 지속적인 성장을 위한 전략적 접근을 권장합니다.")
                } else if (lastYearNet < firstYearNet) {
                    appendLine("⚠️ 개선 필요: ${firstYear}년 대비 순수익이 감소했습니다.")
                    appendLine("💡 비용 최적화 및 수익 다각화 전략이 필요합니다.")
                } else {
                    appendLine("📊 안정적 운영: 일정한 수준의 재정 상태를 유지하고 있습니다.")
                    appendLine("💡 성장 동력 확보를 위한 새로운 전략을 고려해보세요.")
                }
            }
            
            appendLine()
            appendLine("📈 이 분석은 실제 동아리 장부 데이터를 기반으로 생성되었습니다.")
            appendLine("━".repeat(26))
        }
    }
    
    /**
     * 금액 포맷팅
     */
    private fun formatAmount(amount: Int): String {
        return "${numberFormat.format(amount)}원"
    }
}