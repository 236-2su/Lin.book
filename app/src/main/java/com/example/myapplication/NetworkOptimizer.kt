package com.example.myapplication

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 네트워크 요청 최적화 클래스
 * 백엔드의 성능 이슈를 고려하여 요청 순서와 타이밍을 조절
 */
class NetworkOptimizer(private val context: Context) {
    
    companion object {
        private const val TAG = "NetworkOptimizer"
        private const val MAX_CONCURRENT_YEARLY_REQUESTS = 1 // 연간 리포트는 순차적으로
        private const val YEARLY_REQUEST_DELAY = 2000L // 2초 간격
        private const val REQUEST_TIMEOUT = 45000L // 45초 타임아웃
    }
    
    private val yearlyRequestQueue = mutableListOf<YearlyRequestInfo>()
    private val activeYearlyRequests = AtomicInteger(0)
    private val requestTimestamps = ConcurrentHashMap<String, Long>()
    private val handler = Handler(Looper.getMainLooper())
    
    /**
     * 연간 리포트 요청 정보
     */
    data class YearlyRequestInfo(
        val clubId: Int,
        val ledgerId: Int,
        val year: Int,
        val callback: (Boolean, String?) -> Unit,
        val priority: Int = 0
    )
    
    /**
     * 연간 리포트 요청을 최적화하여 실행
     */
    fun requestYearlyReport(
        clubId: Int,
        ledgerId: Int, 
        year: Int,
        callback: (Boolean, String?) -> Unit,
        priority: Int = 0
    ) {
        Log.d(TAG, "연간 리포트 요청 큐에 추가: ${year}년 (우선순위: $priority)")
        
        val requestInfo = YearlyRequestInfo(clubId, ledgerId, year, callback, priority)
        
        synchronized(yearlyRequestQueue) {
            // 우선순위에 따라 정렬하여 삽입
            val insertIndex = yearlyRequestQueue.indexOfFirst { it.priority < priority }
            if (insertIndex == -1) {
                yearlyRequestQueue.add(requestInfo)
            } else {
                yearlyRequestQueue.add(insertIndex, requestInfo)
            }
        }
        
        processYearlyRequestQueue()
    }
    
    /**
     * 연간 리포트 요청 큐를 순차적으로 처리
     */
    private fun processYearlyRequestQueue() {
        if (activeYearlyRequests.get() >= MAX_CONCURRENT_YEARLY_REQUESTS) {
            Log.d(TAG, "연간 리포트 요청 제한 중 (활성: ${activeYearlyRequests.get()})")
            return
        }
        
        val nextRequest = synchronized(yearlyRequestQueue) {
            if (yearlyRequestQueue.isEmpty()) return
            yearlyRequestQueue.removeAt(0)
        }
        
        // 이전 요청과의 간격 확보
        val lastRequestTime = requestTimestamps["yearly"] ?: 0
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRequest = currentTime - lastRequestTime
        
        val delay = if (timeSinceLastRequest < YEARLY_REQUEST_DELAY) {
            YEARLY_REQUEST_DELAY - timeSinceLastRequest
        } else 0
        
        handler.postDelayed({
            executeYearlyRequest(nextRequest)
        }, delay)
    }
    
    /**
     * 실제 연간 리포트 요청 실행
     */
    private fun executeYearlyRequest(requestInfo: YearlyRequestInfo) {
        activeYearlyRequests.incrementAndGet()
        requestTimestamps["yearly"] = System.currentTimeMillis()
        
        Log.d(TAG, "연간 리포트 실행: ${requestInfo.year}년 (활성 요청: ${activeYearlyRequests.get()})")
        
        val apiService = com.example.myapplication.api.ApiClient.getApiService()
        
        // 타임아웃 처리
        val timeoutRunnable = Runnable {
            Log.w(TAG, "연간 리포트 요청 타임아웃: ${requestInfo.year}년")
            handleRequestCompletion(requestInfo, false, "요청 시간 초과")
        }
        handler.postDelayed(timeoutRunnable, REQUEST_TIMEOUT)
        
        apiService.getYearlyReports(requestInfo.clubId, requestInfo.ledgerId, requestInfo.year)
            .enqueue(object : retrofit2.Callback<List<com.example.myapplication.api.ApiService.BackendReportItem>> {
                override fun onResponse(
                    call: retrofit2.Call<List<com.example.myapplication.api.ApiService.BackendReportItem>>,
                    response: retrofit2.Response<List<com.example.myapplication.api.ApiService.BackendReportItem>>
                ) {
                    handler.removeCallbacks(timeoutRunnable)
                    
                    if (response.isSuccessful && response.body() != null) {
                        val reports = response.body()!!
                        Log.d(TAG, "연간 리포트 조회 성공: ${requestInfo.year}년 (${reports.size}개)")
                        
                        // 리포트 데이터를 JSON으로 변환
                        val reportData = convertReportsToJson(reports, requestInfo.year)
                        handleRequestCompletion(requestInfo, true, reportData)
                    } else {
                        Log.w(TAG, "연간 리포트 조회 실패: ${requestInfo.year}년, 코드: ${response.code()}")
                        handleRequestCompletion(requestInfo, false, "HTTP ${response.code()}")
                    }
                }
                
                override fun onFailure(call: retrofit2.Call<List<com.example.myapplication.api.ApiService.BackendReportItem>>, t: Throwable) {
                    handler.removeCallbacks(timeoutRunnable)
                    Log.e(TAG, "연간 리포트 네트워크 오류: ${requestInfo.year}년", t)
                    handleRequestCompletion(requestInfo, false, "네트워크 오류: ${t.message}")
                }
            })
    }
    
    /**
     * 요청 완료 처리
     */
    private fun handleRequestCompletion(requestInfo: YearlyRequestInfo, success: Boolean, data: String?) {
        activeYearlyRequests.decrementAndGet()
        
        try {
            requestInfo.callback(success, data)
        } catch (e: Exception) {
            Log.e(TAG, "콜백 실행 오류", e)
        }
        
        // 다음 요청 처리
        handler.post {
            processYearlyRequestQueue()
        }
        
        Log.d(TAG, "요청 완료: ${requestInfo.year}년 (성공: $success, 남은 큐: ${yearlyRequestQueue.size})")
    }
    
    /**
     * 리포트 목록을 JSON 문자열로 변환
     */
    private fun convertReportsToJson(reports: List<com.example.myapplication.api.ApiService.BackendReportItem>, year: Int): String {
        return try {
            if (reports.isEmpty()) {
                "[]"
            } else {
                val converter = BackendReportConverter()
                val convertedReports = reports.map { report ->
                    converter.convert(report, "yearly")
                }
                org.json.JSONArray(convertedReports).toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "리포트 JSON 변환 실패", e)
            "[]"
        }
    }
    
    /**
     * 월간 리포트 요청 (비교적 빠르므로 일반 처리)
     */
    fun requestMonthlyReport(
        clubId: Int,
        ledgerId: Int,
        year: Int,
        month: Int,
        callback: (Boolean, String?) -> Unit
    ) {
        Log.d(TAG, "월간 리포트 요청: ${year}년 ${month}월")
        
        val apiService = com.example.myapplication.api.ApiClient.getApiService()
        apiService.getMonthlyReports(clubId, ledgerId, year, month)
            .enqueue(object : retrofit2.Callback<List<com.example.myapplication.api.ApiService.BackendReportItem>> {
                override fun onResponse(
                    call: retrofit2.Call<List<com.example.myapplication.api.ApiService.BackendReportItem>>,
                    response: retrofit2.Response<List<com.example.myapplication.api.ApiService.BackendReportItem>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val reports = response.body()!!
                        val reportData = convertReportsToJson(reports, year)
                        callback(true, reportData)
                    } else {
                        callback(false, "HTTP ${response.code()}")
                    }
                }
                
                override fun onFailure(call: retrofit2.Call<List<com.example.myapplication.api.ApiService.BackendReportItem>>, t: Throwable) {
                    callback(false, "네트워크 오류: ${t.message}")
                }
            })
    }
    
    /**
     * 진행 중인 모든 요청 취소
     */
    fun cancelAllRequests() {
        synchronized(yearlyRequestQueue) {
            yearlyRequestQueue.clear()
        }
        activeYearlyRequests.set(0)
        requestTimestamps.clear()
        Log.d(TAG, "모든 네트워크 요청 취소됨")
    }
    
    /**
     * 큐 상태 조회
     */
    fun getQueueStatus(): QueueStatus {
        return QueueStatus(
            queuedRequests = yearlyRequestQueue.size,
            activeRequests = activeYearlyRequests.get(),
            lastRequestTime = requestTimestamps["yearly"] ?: 0
        )
    }
    
    data class QueueStatus(
        val queuedRequests: Int,
        val activeRequests: Int,
        val lastRequestTime: Long
    )
}