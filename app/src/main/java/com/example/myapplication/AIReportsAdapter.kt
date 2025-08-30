package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.app.AlertDialog
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AIReportsAdapter(
    private val onItemClick: (String) -> Unit,
    private val onDeleteClick: ((String, Int) -> Unit)? = null
) : RecyclerView.Adapter<AIReportsAdapter.ReportViewHolder>() {

    private var reports = listOf<String>()

    fun updateReports(newReports: List<String>) {
        android.util.Log.d("AIReportsAdapter", "=== 어댑터 업데이트 ===")
        android.util.Log.d("AIReportsAdapter", "기존 리포트 수: ${reports.size}")
        android.util.Log.d("AIReportsAdapter", "새 리포트 수: ${newReports.size}")
        
        newReports.forEachIndexed { index, report ->
            try {
                val reportObj = JSONObject(report)
                android.util.Log.d("AIReportsAdapter", "리포트 $index: ${reportObj.optString("title")}")
            } catch (e: Exception) {
                android.util.Log.e("AIReportsAdapter", "리포트 $index 파싱 실패: $e")
            }
        }
        
        reports = newReports
        notifyDataSetChanged()
        android.util.Log.d("AIReportsAdapter", "✅ notifyDataSetChanged() 호출 완료")
    }
    
    fun removeReport(position: Int) {
        if (position >= 0 && position < reports.size) {
            val mutableReports = reports.toMutableList()
            mutableReports.removeAt(position)
            reports = mutableReports
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, reports.size)
            android.util.Log.d("AIReportsAdapter", "리포트 제거 완료 - 위치: $position, 남은 개수: ${reports.size}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        android.util.Log.d("AIReportsAdapter", "🏗️ onCreateViewHolder 호출됨")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ai_report, parent, false)
        android.util.Log.d("AIReportsAdapter", "✅ ViewHolder 생성 완료")
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        android.util.Log.d("AIReportsAdapter", "🔗 onBindViewHolder 호출됨 - 위치: $position")
        if (position < reports.size) {
            holder.bind(reports[position], onItemClick, onDeleteClick, position)
            android.util.Log.d("AIReportsAdapter", "✅ ViewHolder 바인딩 완료 - 위치: $position")
        } else {
            android.util.Log.e("AIReportsAdapter", "❌ 잘못된 위치: $position, 리포트 수: ${reports.size}")
        }
    }

    override fun getItemCount(): Int = reports.size

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvReportType: TextView = itemView.findViewById(R.id.tv_report_type)
        private val tvCreatedDate: TextView = itemView.findViewById(R.id.tv_created_date)
        private val tvReportTitle: TextView = itemView.findViewById(R.id.tv_report_title)
        private val tvReportPreview: TextView = itemView.findViewById(R.id.tv_report_preview)
        private val btnDeleteReport: TextView = itemView.findViewById(R.id.btn_delete_report)

                fun bind(reportJson: String, onItemClick: (String) -> Unit, onDeleteClick: ((String, Int) -> Unit)? = null, position: Int = -1) {
            try {
                val reportData = JSONObject(reportJson)
                
                // 디버깅을 위한 로깅
                android.util.Log.d("AIReportsAdapter", "리포트 바인딩 시작")
                android.util.Log.d("AIReportsAdapter", "원본 JSON: $reportJson")

                // 리포트 타입 설정
                val type = reportData.optString("type", "general")
                android.util.Log.d("AIReportsAdapter", "추출된 타입: $type")
                when (type) {
                    "financial_analysis" -> {
                        tvReportType.text = "재정분석"
                        tvReportType.setTextColor(0xFF4CAF50.toInt()) // 초록색
                    }
                    "activity_analysis" -> {
                        tvReportType.text = "활동분석"
                        tvReportType.setTextColor(0xFF2196F3.toInt()) // 파랑색
                    }
                    "general" -> {
                        tvReportType.text = "일반분석"
                        tvReportType.setTextColor(0xFF9C27B0.toInt()) // 보라색
                    }
                    "comprehensive" -> {
                        tvReportType.text = "종합분석"
                        tvReportType.setTextColor(0xFFFF9800.toInt()) // 주황색
                    }
                    "comparison" -> {
                        tvReportType.text = "비교분석"
                        tvReportType.setTextColor(0xFF607D8B.toInt()) // 회색
                    }
                    "similar_clubs" -> {
                        tvReportType.text = "유사동아리"
                        tvReportType.setTextColor(0xFF9C27B0.toInt()) // 보라색
                    }
                    "gemini_analysis" -> {
                        tvReportType.text = "AI재무조언"
                        tvReportType.setTextColor(0xFF4CAF50.toInt()) // 초록색
                    }
                    "three_year_events" -> {
                        tvReportType.text = "3년이벤트"
                        tvReportType.setTextColor(0xFF2196F3.toInt()) // 파랑색
                    }
                    "yearly" -> {
                        tvReportType.text = "연간종합"
                        tvReportType.setTextColor(0xFFFF9800.toInt()) // 주황색
                    }
                    "event_comparison" -> {
                        tvReportType.text = "이벤트비교"
                        tvReportType.setTextColor(0xFF607D8B.toInt()) // 회색
                    }
                    else -> {
                        tvReportType.text = "AI분석"
                        tvReportType.setTextColor(0xFF333333.toInt()) // 기본색
                    }
                }
                
                // 생성 날짜 설정
                val createdAt = reportData.optLong("created_at", System.currentTimeMillis())
                val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA)
                tvCreatedDate.text = dateFormat.format(Date(createdAt))
                
                // 리포트 제목 설정
                tvReportTitle.text = reportData.optString("title", "AI 리포트")
                
                // 리포트 미리보기 설정 (내용의 처음 3줄)
                val content = reportData.optString("content", "AI가 생성한 리포트입니다.")
                val previewLines = content.split("\n").take(3)
                tvReportPreview.text = previewLines.joinToString("\n") + 
                    if (content.split("\n").size > 3) "..." else ""
                
                // 클릭 이벤트 설정
                itemView.setOnClickListener {
                    android.util.Log.d("AIReportsAdapter", "🔥 리포트 아이템 클릭됨!")
                    android.util.Log.d("AIReportsAdapter", "   제목: ${reportData.optString("title")}")
                    android.util.Log.d("AIReportsAdapter", "   타입: ${reportData.optString("type")}")
                    android.util.Log.d("AIReportsAdapter", "   전달할 JSON 길이: ${reportJson.length}")
                    android.util.Log.d("AIReportsAdapter", "   전달할 JSON: $reportJson")
                    
                    // JSON 유효성 검사
                    try {
                        val testJson = org.json.JSONObject(reportJson)
                        android.util.Log.d("AIReportsAdapter", "✅ JSON 유효함")
                        android.util.Log.d("AIReportsAdapter", "   파싱된 제목: ${testJson.optString("title")}")
                        android.util.Log.d("AIReportsAdapter", "   파싱된 내용 길이: ${testJson.optString("content", "").length}자")
                    } catch (e: Exception) {
                        android.util.Log.e("AIReportsAdapter", "❌ JSON 파싱 실패", e)
                    }
                    
                    onItemClick(reportJson)
                }
                
                // 삭제 버튼 클릭 처리
                btnDeleteReport.setOnClickListener {
                    val title = reportData.optString("title", "이 리포트")
                    AlertDialog.Builder(itemView.context)
                        .setTitle("리포트 삭제")
                        .setMessage("'$title'을(를) 정말 삭제하시겠습니까?")
                        .setPositiveButton("삭제") { _, _ ->
                            onDeleteClick?.invoke(reportJson, position)
                        }
                        .setNegativeButton("취소", null)
                        .show()
                }
                
                android.util.Log.d("AIReportsAdapter", "리포트 바인딩 완료: ${reportData.optString("title")} (타입: $type)")
                
            } catch (e: Exception) {
                // JSON 파싱 실패 시 기본값 설정
                android.util.Log.e("AIReportsAdapter", "리포트 바인딩 실패", e)
                android.util.Log.e("AIReportsAdapter", "실패한 JSON: $reportJson")
                
                tvReportType.text = "AI 리포트"
                tvCreatedDate.text = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Date())
                tvReportTitle.text = "AI 분석 리포트"
                tvReportPreview.text = "AI가 생성한 분석 리포트입니다."
                
                itemView.setOnClickListener {
                    onItemClick(reportJson)
                }
                
                // 삭제 버튼 클릭 처리 (예외 처리 블록)
                btnDeleteReport.setOnClickListener {
                    AlertDialog.Builder(itemView.context)
                        .setTitle("리포트 삭제")
                        .setMessage("이 리포트를 정말 삭제하시겠습니까?")
                        .setPositiveButton("삭제") { _, _ ->
                            onDeleteClick?.invoke(reportJson, position)
                        }
                        .setNegativeButton("취소", null)
                        .show()
                }
            }
        }
    }
}
