package com.example.myapplication

import android.content.Context
import android.util.Log
import android.widget.Toast
import org.json.JSONObject
import retrofit2.Response

/**
 * 백엔드 오류 처리를 위한 유틸리티 클래스
 * 백엔드의 알려진 버그들을 프론트엔드에서 우회하여 처리
 */
class BackendErrorHandler(private val context: Context) {
    
    companion object {
        private const val TAG = "BackendErrorHandler"
        
        // 백엔드 오류 타입 상수
        const val ERROR_SIMILAR_CLUB_BROKEN = "similar_club_broken"
        const val ERROR_YEARLY_REPORT_SLOW = "yearly_report_slow"
        const val ERROR_EMPTY_LEDGER = "empty_ledger"
        const val ERROR_NETWORK_TIMEOUT = "network_timeout"
        const val ERROR_INVALID_DATA = "invalid_data"
    }
    
    /**
     * API 응답 오류를 분석하고 적절한 처리를 수행
     */
    fun handleApiError(response: Response<*>?, error: Throwable?, operation: String): ErrorResult {
        Log.d(TAG, "API 오류 처리 시작 - 작업: $operation")
        
        return when {
            // 네트워크 타임아웃 (yearly report 생성 시)
            isTimeoutError(error) -> handleTimeoutError(operation)
            
            // HTTP 500 오류 (similar club report 버그)
            response?.code() == 500 && operation.contains("similar") -> handleSimilarClubError()
            
            // HTTP 404 오류 (빈 장부)
            response?.code() == 404 -> handleNotFoundError(operation)
            
            // HTTP 400 오류 (잘못된 데이터)
            response?.code() == 400 -> handleBadRequestError(response, operation)
            
            // 기타 오류
            else -> handleGeneralError(response, error, operation)
        }
    }
    
    /**
     * 타임아웃 오류 처리 (주로 yearly report)
     */
    private fun handleTimeoutError(operation: String): ErrorResult {
        Log.w(TAG, "타임아웃 오류 감지 - $operation")
        
        return when {
            operation.contains("yearly") -> {
                ErrorResult(
                    errorType = ERROR_YEARLY_REPORT_SLOW,
                    message = "연간 리포트 생성에 시간이 오래 걸립니다.\n백그라운드에서 처리 중이니 잠시 후 다시 확인해주세요.",
                    canRetry = true,
                    retryDelay = 10000L, // 10초 후 재시도
                    fallbackAction = "show_loading_message"
                )
            }
            else -> {
                ErrorResult(
                    errorType = ERROR_NETWORK_TIMEOUT,
                    message = "요청 처리 시간이 초과되었습니다.",
                    canRetry = true,
                    retryDelay = 3000L
                )
            }
        }
    }
    
    /**
     * Similar Club 리포트 오류 처리 (백엔드 버그로 인한)
     */
    private fun handleSimilarClubError(): ErrorResult {
        Log.e(TAG, "Similar Club 리포트 백엔드 버그 감지")
        
        return ErrorResult(
            errorType = ERROR_SIMILAR_CLUB_BROKEN,
            message = "유사 동아리 비교 기능이 일시적으로 사용할 수 없습니다.\n대신 연간 리포트를 생성하시겠습니까?",
            canRetry = false,
            fallbackAction = "suggest_yearly_report",
            userMessage = "시스템 점검 중인 기능입니다"
        )
    }
    
    /**
     * 404 오류 처리 (빈 장부 등)
     */
    private fun handleNotFoundError(operation: String): ErrorResult {
        Log.w(TAG, "404 오류 - $operation")
        
        return when {
            operation.contains("ledger") -> {
                ErrorResult(
                    errorType = ERROR_EMPTY_LEDGER,
                    message = "아직 거래 내역이 없어서 리포트를 생성할 수 없습니다.\n거래 내역을 추가한 후 다시 시도해주세요.",
                    canRetry = false,
                    fallbackAction = "suggest_add_transaction"
                )
            }
            else -> {
                ErrorResult(
                    errorType = ERROR_INVALID_DATA,
                    message = "요청한 데이터를 찾을 수 없습니다.",
                    canRetry = true
                )
            }
        }
    }
    
    /**
     * 400 오류 처리 (잘못된 요청)
     */
    private fun handleBadRequestError(response: Response<*>, operation: String): ErrorResult {
        Log.w(TAG, "400 오류 - $operation")
        
        val errorMessage = try {
            response.errorBody()?.string()?.let { errorBody ->
                JSONObject(errorBody).optString("detail", "잘못된 요청입니다")
            } ?: "잘못된 요청입니다"
        } catch (e: Exception) {
            "요청 형식이 올바르지 않습니다"
        }
        
        return ErrorResult(
            errorType = ERROR_INVALID_DATA,
            message = errorMessage,
            canRetry = false,
            fallbackAction = if (operation.contains("report")) "suggest_check_date" else null
        )
    }
    
    /**
     * 일반 오류 처리
     */
    private fun handleGeneralError(response: Response<*>?, error: Throwable?, operation: String): ErrorResult {
        Log.e(TAG, "일반 오류 - $operation", error)
        
        val message = when {
            error?.message?.contains("timeout", ignoreCase = true) == true -> 
                "서버 응답 시간이 초과되었습니다"
            response?.code() in 500..599 -> 
                "서버에서 일시적인 오류가 발생했습니다"
            else -> 
                "리포트 생성 중 오류가 발생했습니다"
        }
        
        return ErrorResult(
            errorType = "general_error",
            message = message,
            canRetry = true,
            retryDelay = 5000L
        )
    }
    
    /**
     * 타임아웃 오류인지 확인
     */
    private fun isTimeoutError(error: Throwable?): Boolean {
        return error?.message?.contains("timeout", ignoreCase = true) == true ||
                error is java.net.SocketTimeoutException ||
                error is java.util.concurrent.TimeoutException
    }
    
    /**
     * 사용자에게 오류 메시지 표시
     */
    fun showErrorToUser(errorResult: ErrorResult) {
        val displayMessage = errorResult.userMessage ?: errorResult.message
        Toast.makeText(context, displayMessage, Toast.LENGTH_LONG).show()
        Log.d(TAG, "사용자에게 오류 메시지 표시: $displayMessage")
    }
    
    /**
     * 백엔드 오류에 대한 상세 정보를 로깅
     */
    fun logBackendIssue(operation: String, details: String) {
        Log.w(TAG, "백엔드 알려진 이슈 - 작업: $operation, 상세: $details")
        // 필요시 크래시리틱스나 분석 도구에 전송 가능
    }
    
    /**
     * 오류 결과를 나타내는 데이터 클래스
     */
    data class ErrorResult(
        val errorType: String,
        val message: String,
        val canRetry: Boolean = false,
        val retryDelay: Long = 3000L,
        val fallbackAction: String? = null,
        val userMessage: String? = null
    )
}