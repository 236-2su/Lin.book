package com.example.myapplication.ai

import com.example.myapplication.api.ApiClient
import com.example.myapplication.ClubItem
import com.example.myapplication.LedgerApiItem
import com.example.myapplication.TransactionItem
import com.example.myapplication.EventItem
import com.example.myapplication.BoardItem
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import android.util.Log

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
}
