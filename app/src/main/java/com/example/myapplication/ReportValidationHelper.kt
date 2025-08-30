package com.example.myapplication

import android.util.Log
import org.json.JSONObject
import java.util.Calendar

/**
 * 리포트 생성 및 데이터 검증을 위한 헬퍼 클래스
 * 백엔드 오류를 방지하기 위한 사전 검증 로직 포함
 */
object ReportValidationHelper {
    
    private const val TAG = "ReportValidationHelper"
    
    /**
     * 검증 결과를 나타내는 데이터 클래스
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null,
        val suggestion: String? = null,
        val validatedData: Map<String, Any>? = null
    )
    
    /**
     * 월간 리포트 생성 전 검증
     */
    fun validateMonthlyReportRequest(
        clubId: Int,
        ledgerId: Int,
        year: Int,
        month: Int,
        hasTransactions: Boolean = true
    ): ValidationResult {
        Log.d(TAG, "월간 리포트 요청 검증: ${year}년 ${month}월")
        
        // 기본 매개변수 검증
        val basicValidation = validateBasicParameters(clubId, ledgerId, year)
        if (!basicValidation.isValid) return basicValidation
        
        // 월 검증
        if (month < 1 || month > 12) {
            return ValidationResult(
                isValid = false,
                errorMessage = "잘못된 월입니다. 1~12 사이의 값을 입력해주세요.",
                suggestion = "현재 월인 ${getCurrentMonth()}월로 설정하시겠습니까?"
            )
        }
        
        // 미래 날짜 검증
        val currentDate = Calendar.getInstance()
        val currentYear = currentDate.get(Calendar.YEAR)
        val currentMonth = currentDate.get(Calendar.MONTH) + 1
        
        if (year > currentYear || (year == currentYear && month > currentMonth)) {
            return ValidationResult(
                isValid = false,
                errorMessage = "미래 월의 리포트는 생성할 수 없습니다.",
                suggestion = "최근 완료된 ${if (currentMonth > 1) "${currentMonth - 1}월" else "이전 년도 12월"} 리포트를 생성해보세요."
            )
        }
        
        // 거래 내역 존재 여부 (선택적 검증)
        if (!hasTransactions) {
            return ValidationResult(
                isValid = false,
                errorMessage = "${year}년 ${month}월에 거래 내역이 없습니다.",
                suggestion = "거래 내역을 먼저 추가하거나 다른 월을 선택해주세요."
            )
        }
        
        return ValidationResult(
            isValid = true,
            validatedData = mapOf(
                "clubId" to clubId,
                "ledgerId" to ledgerId,
                "year" to year,
                "month" to month
            )
        )
    }
    
    /**
     * 연간 리포트 생성 전 검증
     */
    fun validateYearlyReportRequest(
        clubId: Int,
        ledgerId: Int,
        year: Int,
        hasTransactions: Boolean = true
    ): ValidationResult {
        Log.d(TAG, "연간 리포트 요청 검증: ${year}년")
        
        // 기본 매개변수 검증
        val basicValidation = validateBasicParameters(clubId, ledgerId, year)
        if (!basicValidation.isValid) return basicValidation
        
        // 현재 연도 초과 검증
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (year > currentYear) {
            return ValidationResult(
                isValid = false,
                errorMessage = "미래 연도의 리포트는 생성할 수 없습니다.",
                suggestion = "${currentYear}년 또는 ${currentYear - 1}년 리포트를 생성해보세요."
            )
        }
        
        // 너무 오래된 연도 검증
        if (year < 2020) {
            return ValidationResult(
                isValid = false,
                errorMessage = "2020년 이전 데이터는 지원되지 않습니다.",
                suggestion = "2020년 이후 연도로 선택해주세요."
            )
        }
        
        // 현재 연도 첫 달 검증
        if (year == currentYear) {
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            if (currentMonth <= 2) {
                return ValidationResult(
                    isValid = true,
                    suggestion = "현재 연도 초반입니다. 데이터가 부족할 수 있어 이전 연도 리포트를 권장합니다.",
                    validatedData = mapOf(
                        "clubId" to clubId,
                        "ledgerId" to ledgerId,
                        "year" to year,
                        "warning" to "limited_data"
                    )
                )
            }
        }
        
        // 거래 내역 존재 여부
        if (!hasTransactions) {
            return ValidationResult(
                isValid = false,
                errorMessage = "${year}년에 거래 내역이 없습니다.",
                suggestion = "거래 내역을 먼저 추가하거나 다른 연도를 선택해주세요."
            )
        }
        
        return ValidationResult(
            isValid = true,
            validatedData = mapOf(
                "clubId" to clubId,
                "ledgerId" to ledgerId,
                "year" to year
            )
        )
    }
    
    /**
     * 유사 동아리 비교 리포트 검증 (현재 백엔드 버그로 비활성화)
     */
    fun validateSimilarClubReportRequest(
        clubId: Int,
        year: Int
    ): ValidationResult {
        Log.w(TAG, "유사 동아리 비교 리포트 요청 - 백엔드 이슈로 비활성화")
        
        return ValidationResult(
            isValid = false,
            errorMessage = "유사 동아리 비교 기능이 일시적으로 사용할 수 없습니다.",
            suggestion = "대신 ${year}년 연간 리포트를 생성하시겠습니까?"
        )
    }
    
    /**
     * 기본 매개변수 검증 (clubId, ledgerId, year)
     */
    private fun validateBasicParameters(clubId: Int, ledgerId: Int, year: Int): ValidationResult {
        when {
            clubId <= 0 -> return ValidationResult(
                isValid = false,
                errorMessage = "유효하지 않은 클럽 정보입니다.",
                suggestion = "클럽을 다시 선택해주세요."
            )
            ledgerId <= 0 -> return ValidationResult(
                isValid = false,
                errorMessage = "유효하지 않은 장부 정보입니다.",
                suggestion = "장부를 다시 선택해주세요."
            )
            year <= 0 -> return ValidationResult(
                isValid = false,
                errorMessage = "유효하지 않은 연도입니다.",
                suggestion = "올바른 연도를 입력해주세요."
            )
        }
        
        return ValidationResult(isValid = true)
    }
    
    /**
     * 리포트 데이터 유효성 검증
     */
    fun validateReportData(reportJson: String): ValidationResult {
        return try {
            val reportObj = JSONObject(reportJson)
            
            // 필수 필드 검증
            val requiredFields = listOf("id", "title", "content", "type", "created_at")
            val missingFields = requiredFields.filter { !reportObj.has(it) }
            
            if (missingFields.isNotEmpty()) {
                return ValidationResult(
                    isValid = false,
                    errorMessage = "리포트 데이터가 불완전합니다: ${missingFields.joinToString(", ")}",
                    suggestion = "리포트를 다시 생성해주세요."
                )
            }
            
            // 콘텐츠 길이 검증
            val content = reportObj.optString("content", "")
            if (content.isEmpty()) {
                return ValidationResult(
                    isValid = false,
                    errorMessage = "리포트 내용이 비어있습니다.",
                    suggestion = "리포트 생성을 다시 시도해주세요."
                )
            }
            
            // 제목 검증
            val title = reportObj.optString("title", "")
            if (title.isEmpty() || title.length < 5) {
                Log.w(TAG, "리포트 제목이 너무 짧음: $title")
                // 제목은 경고만 하고 통과
            }
            
            // 타입 검증
            val type = reportObj.optString("type", "")
            val validTypes = listOf("monthly", "yearly", "comparison", "general", "test")
            if (!validTypes.contains(type)) {
                Log.w(TAG, "알 수 없는 리포트 타입: $type")
                // 타입 오류는 경고만 하고 통과
            }
            
            ValidationResult(
                isValid = true,
                validatedData = mapOf(
                    "title" to title,
                    "type" to type,
                    "contentLength" to content.length,
                    "hasBackendId" to reportObj.has("backend_id")
                )
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "리포트 데이터 검증 실패", e)
            ValidationResult(
                isValid = false,
                errorMessage = "리포트 데이터 형식이 올바르지 않습니다.",
                suggestion = "리포트를 다시 생성해주세요."
            )
        }
    }
    
    /**
     * 사용자 입력값 정제 (년도, 월 등)
     */
    fun sanitizeUserInput(input: String): String {
        return input.trim()
            .replace("[^0-9]".toRegex(), "")  // 숫자만 남김
            .take(4)  // 최대 4자리
    }
    
    /**
     * 권장 날짜 범위 제공
     */
    fun getRecommendedDateRange(): RecommendedDateRange {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        
        return RecommendedDateRange(
            recommendedYear = if (currentMonth <= 3) currentYear - 1 else currentYear,
            recommendedMonth = if (currentMonth <= 1) 12 else currentMonth - 1,
            availableYears = (2020..currentYear).toList(),
            availableMonths = (1..12).toList()
        )
    }
    
    /**
     * 현재 월 반환
     */
    private fun getCurrentMonth(): Int {
        return Calendar.getInstance().get(Calendar.MONTH) + 1
    }
    
    /**
     * 권장 날짜 범위 데이터 클래스
     */
    data class RecommendedDateRange(
        val recommendedYear: Int,
        val recommendedMonth: Int,
        val availableYears: List<Int>,
        val availableMonths: List<Int>
    )
    
    /**
     * 백엔드 응답 검증
     */
    fun validateBackendResponse(responseBody: String?): ValidationResult {
        return when {
            responseBody.isNullOrEmpty() -> ValidationResult(
                isValid = false,
                errorMessage = "서버로부터 응답을 받지 못했습니다.",
                suggestion = "네트워크 연결을 확인하고 다시 시도해주세요."
            )
            responseBody.contains("error", ignoreCase = true) -> ValidationResult(
                isValid = false,
                errorMessage = "서버에서 오류가 발생했습니다.",
                suggestion = "잠시 후 다시 시도해주세요."
            )
            responseBody.length < 50 -> ValidationResult(
                isValid = false,
                errorMessage = "서버 응답이 너무 짧습니다.",
                suggestion = "리포트 생성을 다시 시도해주세요."
            )
            else -> ValidationResult(isValid = true)
        }
    }
}