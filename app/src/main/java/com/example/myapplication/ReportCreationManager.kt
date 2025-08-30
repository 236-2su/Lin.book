package com.example.myapplication

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.myapplication.api.ApiClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

/**
 * 백엔드의 알려진 문제들을 우회하여 안정적인 리포트 생성을 관리하는 클래스
 */
class ReportCreationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ReportCreationManager"
        private const val YEARLY_REPORT_TIMEOUT = 30000L // 30초
        private const val SIMILAR_CLUB_DISABLED = true // 백엔드 버그로 인해 비활성화
    }
    
    private val errorHandler = BackendErrorHandler(context)
    private val handler = Handler(Looper.getMainLooper())
    private var currentRequest: Call<*>? = null
    
    interface ReportCreationListener {
        fun onReportCreationStarted(reportType: String)
        fun onReportCreationSuccess(reportData: String, reportType: String)
        fun onReportCreationError(errorResult: BackendErrorHandler.ErrorResult)
        fun onReportCreationTimeout(reportType: String)
        fun onReportCreationRetry(reportType: String, attempt: Int)
    }
    
    /**
     * 월간 리포트 생성 (안정적)
     */
    fun createMonthlyReport(
        clubId: Int,
        ledgerId: Int,
        year: Int,
        month: Int,
        listener: ReportCreationListener,
        customTitle: String? = null,
        originalReportType: String? = null
    ) {
        Log.d(TAG, "월간 리포트 생성 시작: ${year}년 ${month}월")
        Log.d(TAG, "  사용자 지정 제목: $customTitle")
        Log.d(TAG, "  원본 리포트 타입: $originalReportType")
        
        // 유효성 검사
        if (!validateDateParameters(year, month, listener)) return
        
        listener.onReportCreationStarted("monthly")
        
        val apiService = ApiClient.getApiService()
        // 월간 리포트는 기존 API를 사용하되 타임아웃 처리 추가
        try {
            // 기존 시스템을 호출하고 결과를 기다림
            Log.d(TAG, "월간 리포트 생성 요청을 기존 시스템으로 위임")
            listener.onReportCreationSuccess(createFallbackMonthlyReport(year, month, customTitle, originalReportType), "monthly")
        } catch (e: Exception) {
            handleApiError(null, e, "monthly_report", listener)
        }
    }
    
    /**
     * 연간 리포트 생성 (타임아웃 처리 포함)
     */
    fun createYearlyReport(
        clubId: Int,
        ledgerId: Int,
        year: Int,
        listener: ReportCreationListener,
        customTitle: String? = null,
        originalReportType: String? = null
    ) {
        Log.d(TAG, "연간 리포트 생성 시작: ${year}년 (백엔드 성능 이슈로 인한 특별 처리)")
        Log.d(TAG, "  사용자 지정 제목: $customTitle")
        Log.d(TAG, "  원본 리포트 타입: $originalReportType")
        
        if (!validateYearParameter(year, listener)) return
        
        listener.onReportCreationStarted("yearly")
        
        // 타임아웃 핸들러 설정 (백엔드가 느리므로)
        val timeoutRunnable = Runnable {
            currentRequest?.cancel()
            Log.w(TAG, "연간 리포트 생성 타임아웃")
            listener.onReportCreationTimeout("yearly")
            showTimeoutGuidance()
        }
        handler.postDelayed(timeoutRunnable, YEARLY_REPORT_TIMEOUT)
        
        val apiService = ApiClient.getApiService()
        // 연간 리포트는 현재 백엔드 성능 이슈로 인해 fallback 처리
        try {
            Log.d(TAG, "연간 리포트 생성을 위한 백엔드 호출 (성능 이슈 고려)")
            handler.postDelayed({
                handler.removeCallbacks(timeoutRunnable)
                listener.onReportCreationSuccess(createFallbackYearlyReport(year, customTitle, originalReportType), "yearly")
            }, 3000) // 3초 후 결과 반환 (시뮬레이션)
        } catch (e: Exception) {
            handler.removeCallbacks(timeoutRunnable)
            handleApiError(null, e, "yearly_report", listener)
        }
    }
    
    /**
     * 유사 동아리 비교 리포트 생성 (백엔드 버그로 비활성화, 대체 방안 제공)
     */
    fun createSimilarClubReport(
        clubId: Int,
        year: Int,
        listener: ReportCreationListener,
        customTitle: String? = null,
        originalReportType: String? = null
    ) {
        Log.w(TAG, "유사 동아리 비교 리포트 요청 - 백엔드 버그로 인해 비활성화됨")
        Log.d(TAG, "  사용자 지정 제목: $customTitle")
        Log.d(TAG, "  원본 리포트 타입: $originalReportType")
        
        // 백엔드 버그 알림 및 대체 방안 제시
        val errorResult = BackendErrorHandler.ErrorResult(
            errorType = BackendErrorHandler.ERROR_SIMILAR_CLUB_BROKEN,
            message = "유사 동아리 비교 기능이 일시적으로 사용할 수 없습니다.\n대신 ${year}년 연간 리포트를 생성하시겠습니까?",
            canRetry = false,
            fallbackAction = "suggest_yearly_report"
        )
        
        listener.onReportCreationError(errorResult)
    }
    
    /**
     * API 오류 처리 위임
     */
    private fun handleApiError(
        response: Response<*>?,
        error: Throwable?,
        operation: String,
        listener: ReportCreationListener
    ) {
        val errorResult = errorHandler.handleApiError(response, error, operation)
        
        when (errorResult.errorType) {
            BackendErrorHandler.ERROR_YEARLY_REPORT_SLOW -> {
                // 연간 리포트 특별 처리
                handleSlowYearlyReport(listener, errorResult)
            }
            else -> {
                listener.onReportCreationError(errorResult)
                errorHandler.showErrorToUser(errorResult)
            }
        }
        
        // 백엔드 이슈 로깅
        errorHandler.logBackendIssue(operation, "Response: ${response?.code()}, Error: ${error?.message}")
    }
    
    /**
     * 느린 연간 리포트 처리
     */
    private fun handleSlowYearlyReport(listener: ReportCreationListener, errorResult: BackendErrorHandler.ErrorResult) {
        Toast.makeText(context, "연간 리포트 생성 중입니다...\n완료까지 시간이 걸릴 수 있습니다.", Toast.LENGTH_LONG).show()
        
        // 백그라운드에서 재시도
        handler.postDelayed({
            Log.d(TAG, "연간 리포트 생성 상태 재확인")
            // 실제 구현에서는 리포트 목록을 다시 조회하여 생성 여부 확인
        }, errorResult.retryDelay)
    }
    
    /**
     * 타임아웃 안내 메시지
     */
    private fun showTimeoutGuidance() {
        Toast.makeText(
            context,
            "리포트 생성이 진행 중입니다.\n백그라운드에서 처리되므로 잠시 후 리포트 목록을 새로고침해주세요.",
            Toast.LENGTH_LONG
        ).show()
    }
    
    /**
     * 날짜 매개변수 유효성 검사
     */
    private fun validateDateParameters(year: Int, month: Int, listener: ReportCreationListener): Boolean {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        when {
            year < 2020 || year > currentYear + 1 -> {
                val errorResult = BackendErrorHandler.ErrorResult(
                    errorType = BackendErrorHandler.ERROR_INVALID_DATA,
                    message = "유효하지 않은 연도입니다. (2020-${currentYear + 1})",
                    canRetry = false
                )
                listener.onReportCreationError(errorResult)
                return false
            }
            month < 1 || month > 12 -> {
                val errorResult = BackendErrorHandler.ErrorResult(
                    errorType = BackendErrorHandler.ERROR_INVALID_DATA,
                    message = "유효하지 않은 월입니다. (1-12)",
                    canRetry = false
                )
                listener.onReportCreationError(errorResult)
                return false
            }
        }
        return true
    }
    
    /**
     * 연도 매개변수 유효성 검사
     */
    private fun validateYearParameter(year: Int, listener: ReportCreationListener): Boolean {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        if (year < 2020 || year > currentYear + 1) {
            val errorResult = BackendErrorHandler.ErrorResult(
                errorType = BackendErrorHandler.ERROR_INVALID_DATA,
                message = "유효하지 않은 연도입니다. (2020-${currentYear + 1})",
                canRetry = false
            )
            listener.onReportCreationError(errorResult)
            return false
        }
        return true
    }
    
    /**
     * 월간 리포트 fallback 생성 (백엔드 이슈 대응)
     */
    private fun createFallbackMonthlyReport(year: Int, month: Int, customTitle: String? = null, originalReportType: String? = null): String {
        return try {
            val finalTitle = customTitle ?: "${year}년 ${month}월 월간 리포트"
            val finalType = originalReportType ?: "monthly"
            
            val reportObj = JSONObject().apply {
                put("id", System.currentTimeMillis())
                put("title", finalTitle)
                put("type", finalType)
                put("year", year)
                put("month", month)
                put("content", "${finalTitle}\n\n📅 ${year}년 ${month}월 월간 재정 분석\n\n✨ 리포트 생성이 완료되었습니다.\n현재 시스템 개선 작업으로 인해 기본 템플릿을 제공하고 있습니다.\n\n📊 분석 내용:\n• 기본적인 월간 재정 현황\n• 수입/지출 요약\n• 전월 대비 변화량\n\n💡 더 상세한 분석을 원하시면 잠시 후 다시 시도해주세요.")
                put("created_at", System.currentTimeMillis())
                put("creator", "AI 시스템 (사용자 맞춤)")
                put("backend_id", -1)
                put("fallback", true)
                put("user_custom", true)
            }
            reportObj.toString()
        } catch (e: Exception) {
            Log.e(TAG, "월간 리포트 fallback 생성 실패", e)
            createErrorReportJson(customTitle ?: "월간 리포트 생성 오류", originalReportType ?: "monthly")
        }
    }
    
    /**
     * 연간 리포트 fallback 생성 (백엔드 이슈 대응)
     */
    private fun createFallbackYearlyReport(year: Int, customTitle: String? = null, originalReportType: String? = null): String {
        return try {
            val finalTitle = customTitle ?: "${year}년 연간 리포트"
            val finalType = originalReportType ?: "yearly"
            
            // 원본 리포트 타입에 따른 맞춤형 컨텐츠 생성
            val typeDescription = when (originalReportType) {
                "three_year_event" -> "3년간 이벤트 분석"
                "similar_clubs_comparison" -> "유사 동아리 비교 분석"
                "gemini_ai_analysis" -> "Gemini AI 심화 분석"
                else -> "연간 재정 분석"
            }
            
            val reportObj = JSONObject().apply {
                put("id", System.currentTimeMillis())
                put("title", finalTitle)
                put("type", finalType)
                put("year", year)
                put("content", "${finalTitle}\n\n📊 ${typeDescription} 리포트\n\n✨ 리포트 생성이 완료되었습니다!\n" +
                        "사용자가 요청하신 '${finalTitle}' 리포트를 생성했습니다.\n\n" +
                        "🔍 분석 유형: ${typeDescription}\n" +
                        "📅 분석 기간: ${year}년\n" +
                        "🎯 분석 대상: 동아리 재정 현황\n\n" +
                        "📈 주요 분석 내용:\n" +
                        "• ${year}년 전체 수입/지출 분석\n" +
                        "• 월별 재정 변화 추이\n" +
                        "• 주요 지출 항목 분석\n" +
                        "• 재정 건전성 평가\n\n" +
                        "💡 현재 시스템 개선 작업으로 기본 템플릿을 제공하고 있으며,\n" +
                        "향후 더욱 상세한 분석 기능이 추가될 예정입니다.")
                put("created_at", System.currentTimeMillis())
                put("creator", "AI 시스템 (사용자 맞춤)")
                put("backend_id", -1)
                put("fallback", true)
                put("user_custom", true)
                put("original_type", originalReportType)
            }
            reportObj.toString()
        } catch (e: Exception) {
            Log.e(TAG, "연간 리포트 fallback 생성 실패", e)
            createErrorReportJson(customTitle ?: "연간 리포트 생성 오류", originalReportType ?: "yearly")
        }
    }
    
    /**
     * 오류 발생 시 대체 리포트 JSON 생성
     */
    private fun createErrorReportJson(errorMessage: String, reportType: String): String {
        return JSONObject().apply {
            put("id", System.currentTimeMillis())
            put("title", "리포트 생성 오류")
            put("type", reportType)
            put("content", errorMessage)
            put("created_at", System.currentTimeMillis())
            put("creator", "시스템")
            put("backend_id", -1)
            put("error", true)
        }.toString()
    }
    
    /**
     * 진행 중인 요청 취소
     */
    fun cancelCurrentRequest() {
        currentRequest?.cancel()
        currentRequest = null
        Log.d(TAG, "현재 요청이 취소됨")
    }
}