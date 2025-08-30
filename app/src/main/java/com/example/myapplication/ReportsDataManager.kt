package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * 리포트 데이터 관리를 위한 유틸리티 클래스
 * SharedPreferences 최적화 및 데이터 정합성 보장
 */
class ReportsDataManager(private val context: Context, private val clubId: Int) {
    
    companion object {
        private const val TAG = "ReportsDataManager"
        private const val PREFS_PREFIX = "ai_reports_club_"
        private const val REPORTS_KEY = "reports_json"
    }
    
    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("${PREFS_PREFIX}$clubId", Context.MODE_PRIVATE)
    }
    
    /**
     * 로컬 저장된 리포트 목록을 가져옵니다
     */
    fun getLocalReports(): Set<String> {
        return try {
            val reportsJson = sharedPrefs.getString(REPORTS_KEY, "[]") ?: "[]"
            val reportsArray = JSONArray(reportsJson)
            val reports = mutableSetOf<String>()
            
            for (i in 0 until reportsArray.length()) {
                val reportObj = reportsArray.getJSONObject(i)
                val title = reportObj.optString("title", "")
                
                // 자동 생성된 버전 리포트는 제외
                if (!isVersionReport(title)) {
                    reports.add(reportObj.toString())
                }
            }
            
            Log.d(TAG, "로컬 리포트 로드 완료: ${reports.size}개")
            reports
        } catch (e: Exception) {
            Log.e(TAG, "로컬 리포트 로드 실패", e)
            emptySet()
        }
    }
    
    /**
     * 백엔드 리포트와 로컬 리포트를 병합합니다
     * 백엔드 데이터를 우선으로 하며 중복을 제거합니다
     */
    fun mergeReports(backendReports: Set<String>): Set<String> {
        return try {
            val localReports = getLocalReports()
            val mergedReports = mutableSetOf<String>()
            val processedBackendIds = mutableSetOf<Int>()
            
            // 1. 백엔드 리포트 먼저 처리 (우선순위)
            backendReports.forEach { backendReport ->
                try {
                    val reportObj = JSONObject(backendReport)
                    val title = reportObj.optString("title", "")
                    val backendId = reportObj.optInt("backend_id", -1)
                    
                    if (!isVersionReport(title)) {
                        mergedReports.add(backendReport)
                        if (backendId != -1) {
                            processedBackendIds.add(backendId)
                        }
                        Log.d(TAG, "백엔드 리포트 추가: $title")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "백엔드 리포트 처리 오류", e)
                }
            }
            
            // 2. 로컬 전용 리포트 추가 (backend_id가 -1인 것들)
            localReports.forEach { localReport ->
                try {
                    val reportObj = JSONObject(localReport)
                    val backendId = reportObj.optInt("backend_id", -1)
                    
                    // 백엔드에 없는 로컬 전용 리포트만 추가
                    if (backendId == -1 || !processedBackendIds.contains(backendId)) {
                        mergedReports.add(localReport)
                        Log.d(TAG, "로컬 전용 리포트 추가: ${reportObj.optString("title")}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "로컬 리포트 처리 오류", e)
                }
            }
            
            Log.d(TAG, "리포트 병합 완료: ${mergedReports.size}개")
            mergedReports
        } catch (e: Exception) {
            Log.e(TAG, "리포트 병합 실패", e)
            emptySet()
        }
    }
    
    /**
     * 리포트 목록을 저장합니다
     */
    fun saveReports(reports: Set<String>): Boolean {
        return try {
            val reportsArray = JSONArray()
            reports.forEach { report ->
                reportsArray.put(JSONObject(report))
            }
            
            val success = sharedPrefs.edit()
                .putString(REPORTS_KEY, reportsArray.toString())
                .commit() // apply() 대신 commit() 사용으로 즉시 반영
            
            if (success) {
                Log.d(TAG, "리포트 저장 완료: ${reports.size}개")
            } else {
                Log.e(TAG, "리포트 저장 실패")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "리포트 저장 오류", e)
            false
        }
    }
    
    /**
     * 특정 리포트를 삭제합니다
     */
    fun deleteReport(reportToDelete: String): Boolean {
        return try {
            val currentReports = getLocalReports().toMutableSet()
            val removed = currentReports.remove(reportToDelete)
            
            if (removed) {
                val success = saveReports(currentReports)
                Log.d(TAG, "리포트 삭제 ${if (success) "성공" else "실패"}")
                success
            } else {
                Log.w(TAG, "삭제할 리포트를 찾을 수 없음")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "리포트 삭제 오류", e)
            false
        }
    }
    
    /**
     * 백엔드 리포트를 로컬에 저장합니다 (중복 방지)
     */
    fun saveBackendReport(reportJson: String): Boolean {
        return try {
            val newReport = JSONObject(reportJson)
            val backendId = newReport.optInt("backend_id", -1)
            
            // backend_id == -1인 경우는 로컬에서 생성된 리포트이므로 로컬 리포트로 저장
            if (backendId == -1) {
                Log.d(TAG, "로컬 생성 리포트를 로컬 저장소에 저장")
                return saveLocalReport(reportJson)
            }
            
            val currentReports = getLocalReports().toMutableSet()
            
            // 중복 체크
            val isDuplicate = currentReports.any { reportStr ->
                try {
                    val existingReport = JSONObject(reportStr)
                    existingReport.optInt("backend_id", -1) == backendId
                } catch (e: Exception) {
                    false
                }
            }
            
            if (!isDuplicate) {
                currentReports.add(reportJson)
                val success = saveReports(currentReports)
                Log.d(TAG, "백엔드 리포트 저장: ${newReport.optString("title")}")
                success
            } else {
                Log.d(TAG, "중복된 백엔드 리포트 무시: ${newReport.optString("title")}")
                true // 중복은 정상적인 상황으로 처리
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "백엔드 리포트 저장 오류", e)
            false
        }
    }
    
    /**
     * 로컬에서 생성된 리포트를 저장합니다
     */
    fun saveLocalReport(reportJson: String): Boolean {
        return try {
            val newReport = JSONObject(reportJson)
            val reportTitle = newReport.optString("title", "새 리포트")
            val currentTime = System.currentTimeMillis()
            
            val currentReports = getLocalReports().toMutableSet()
            
            // 로컬 리포트는 ID와 시간을 기준으로 중복 체크
            val reportId = newReport.optLong("id", currentTime)
            val isDuplicate = currentReports.any { reportStr ->
                try {
                    val existingReport = JSONObject(reportStr)
                    val existingId = existingReport.optLong("id", 0)
                    existingId == reportId
                } catch (e: Exception) {
                    false
                }
            }
            
            if (!isDuplicate) {
                currentReports.add(reportJson)
                val success = saveReports(currentReports)
                Log.d(TAG, "로컬 리포트 저장 성공: $reportTitle")
                success
            } else {
                Log.d(TAG, "중복된 로컬 리포트 무시: $reportTitle")
                true
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "로컬 리포트 저장 오류", e)
            false
        }
    }
    
    /**
     * 버전 리포트인지 확인합니다 (자동 생성된 리포트 제외용)
     */
    private fun isVersionReport(title: String): Boolean {
        return title.contains("_ver_") || title.contains("ver_")
    }
    
    /**
     * 저장된 리포트 통계를 가져옵니다
     */
    fun getReportStats(): ReportStats {
        val reports = getLocalReports()
        var latestTimestamp = 0L
        
        reports.forEach { reportStr ->
            try {
                val report = JSONObject(reportStr)
                val createdAt = report.optLong("created_at", 0)
                if (createdAt > latestTimestamp) {
                    latestTimestamp = createdAt
                }
            } catch (e: Exception) {
                // 개별 리포트 파싱 오류 무시
            }
        }
        
        return ReportStats(
            totalCount = reports.size,
            latestTimestamp = latestTimestamp
        )
    }
    
    /**
     * 모든 리포트 데이터를 삭제합니다
     */
    fun clearAllReports(): Boolean {
        return sharedPrefs.edit().remove(REPORTS_KEY).commit()
    }
    
    data class ReportStats(
        val totalCount: Int,
        val latestTimestamp: Long
    )
}