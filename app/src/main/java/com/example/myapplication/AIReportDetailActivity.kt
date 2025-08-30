package com.example.myapplication

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AIReportDetailActivity : BaseActivity() {
    
    override fun setupContent(savedInstanceState: Bundle?) {
        android.util.Log.d("AIReportDetail", "🎯 AIReportDetailActivity 시작!")
        android.util.Log.d("AIReportDetail", "Intent 전체 정보: ${intent.extras}")
        
        setAppTitle("AI 리포트 상세")
        showBackButton()
        
        try {
            val contentContainer = findViewById<android.widget.FrameLayout>(R.id.content_container)
            android.util.Log.d("AIReportDetail", "contentContainer 찾기 성공: ${contentContainer != null}")
            
            if (contentContainer == null) {
                android.util.Log.e("AIReportDetail", "❌ contentContainer가 null입니다!")
                Toast.makeText(this, "화면 초기화 실패", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            
            val contentView = layoutInflater.inflate(R.layout.ledger_report_detail, null)
            android.util.Log.d("AIReportDetail", "ledger_report_detail 레이아웃 inflate 성공")
            
            contentContainer.addView(contentView)
            android.util.Log.d("AIReportDetail", "contentView 추가 성공")
            
            // Intent에서 리포트 데이터 받아오기 (두 가지 키 모두 확인)
            val reportData = intent.getStringExtra("report_data") ?: intent.getStringExtra("report_content")
            android.util.Log.d("AIReportDetail", "Intent에서 받은 데이터 길이: ${reportData?.length ?: 0}")
            android.util.Log.d("AIReportDetail", "Intent에서 받은 데이터 내용: $reportData")
            android.util.Log.d("AIReportDetail", "사용된 키: ${if (intent.getStringExtra("report_data") != null) "report_data" else "report_content"}")
            
            if (reportData.isNullOrEmpty()) {
                android.util.Log.e("AIReportDetail", "❌ Intent에서 받은 데이터가 비어있습니다!")
                Toast.makeText(this, "리포트 데이터가 없습니다", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            
            displayReportContent(contentView, reportData)
        } catch (e: Exception) {
            android.util.Log.e("AIReportDetail", "❌ setupContent에서 오류 발생", e)
            android.util.Log.e("AIReportDetail", "오류 스택트레이스: ${e.stackTrace.joinToString("\n")}")
            Toast.makeText(this, "Detail 페이지 초기화 실패: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun displayReportContent(contentView: View, reportData: String?) {
        if (reportData != null) {
            try {
                val report = JSONObject(reportData)
                
                // 앱 제목을 리포트 제목으로 설정 (안전한 접근)
                val title = report.optString("title", "AI 리포트")
                setAppTitle(title)
                
                // AI 리포트 내용 설정 (안전한 접근)
                val aiContent = report.optString("content", "리포트 내용을 불러올 수 없습니다.")
                val creator = report.optString("creator", "AI 시스템")
                val createdAt = report.optLong("created_at", System.currentTimeMillis())
                
                // XML 요소들에 데이터 설정
                populateXMLContent(contentView, title, aiContent, creator, createdAt)
                
                android.util.Log.d("AIReportDetail", "리포트 표시 완료 - 제목: $title")
                
            } catch (e: Exception) {
                android.util.Log.e("AIReportDetail", "JSON 파싱 오류", e)
                android.util.Log.e("AIReportDetail", "원본 데이터: $reportData")
                Toast.makeText(this, "리포트를 불러오는데 실패했습니다: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        } else {
            android.util.Log.e("AIReportDetail", "리포트 데이터가 null")
            Toast.makeText(this, "리포트 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun populateXMLContent(contentView: View, title: String, content: String, creator: String, createdAt: Long) {
        try {
            // XML의 기존 요소들을 찾아서 데이터 설정
            val tvReportDate = contentView.findViewById<TextView>(R.id.tv_report_date)
            val tvReportTitle = contentView.findViewById<TextView>(R.id.tv_report_title)
            val tvReportCreator = contentView.findViewById<TextView>(R.id.tv_report_creator)
            val tvReportContent = contentView.findViewById<TextView>(R.id.tv_report_content)
            
            // 날짜 설정
            if (tvReportDate != null) {
                val dateFormat = SimpleDateFormat("yyyy. MM. dd(E) HH:mm", Locale.KOREA)
                tvReportDate.text = dateFormat.format(Date(createdAt))
                android.util.Log.d("AIReportDetail", "날짜 설정 완료")
            }
            
            // 제목 설정
            if (tvReportTitle != null) {
                tvReportTitle.text = title
                android.util.Log.d("AIReportDetail", "제목 설정 완료: $title")
            }
            
            // 생성자 설정
            if (tvReportCreator != null) {
                tvReportCreator.text = "생성자: $creator"
                android.util.Log.d("AIReportDetail", "생성자 설정 완료")
            }
            
            // 내용 설정 (향상된 포매팅)
            if (tvReportContent != null) {
                val formattedContent = formatReportContent(content, title)
                tvReportContent.text = formattedContent
                android.util.Log.d("AIReportDetail", "내용 설정 완료 (길이: ${formattedContent.length})")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("AIReportDetail", "XML 요소 설정 중 오류", e)
            // 오류 발생 시 fallback으로 기존 방식 사용
            displayAIContent(contentView, content)
        }
    }
    
    private fun displayAIContent(contentView: View, aiContent: String) {
        // 기존 XML 내용을 완전히 교체 (fallback 용도)
        val parentContainer = contentView as android.widget.LinearLayout
        
        // 기존 내용 모두 제거
        parentContainer.removeAllViews()
        
        // AI 리포트 전용 레이아웃 생성
        val aiContentView = createAIContentView(aiContent)
        parentContainer.addView(aiContentView)
    }
    
    private fun createAIContentView(content: String): View {
        // 메인 컨테이너
        val mainContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }
        
        // 스크롤 가능한 AI 리포트 내용 생성
        val scrollView = android.widget.ScrollView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        val contentContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val textView = TextView(this).apply {
            text = content
            textSize = 14f
            setTextColor(Color.parseColor("#333333"))
            setLineSpacing(8f, 1.2f) // lineSpacingExtra 대신 setLineSpacing 사용
            
            // 텍스트 스타일링
            typeface = android.graphics.Typeface.DEFAULT
            
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        contentContainer.addView(textView)
        scrollView.addView(contentContainer)
        mainContainer.addView(scrollView)
        
        return mainContainer
    }
    
    private fun formatReportContent(content: String, title: String): String {
        return try {
            android.util.Log.d("AIReportDetail", "리포트 내용 포매팅 시작")
            android.util.Log.d("AIReportDetail", "제목: $title")
            android.util.Log.d("AIReportDetail", "원본 내용 길이: ${content.length}")
            
            // 제목에 따라 리포트 타입 구분
            val reportType = when {
                title.contains("연간") -> "yearly"
                title.contains("비교") -> "comparison"
                title.contains("AI") || title.contains("Gemini") -> "ai_analysis"
                title.contains("종합") -> "comprehensive"
                else -> "general"
            }
            
            android.util.Log.d("AIReportDetail", "감지된 리포트 타입: $reportType")
            
            // 한국어 표시 및 가독성 향상
            val formattedContent = content
                .replace("=".repeat(50), "━".repeat(26))
                .replace("=".repeat(40), "━".repeat(26))
                .replace("=".repeat(30), "━".repeat(26))
                .replace("\\*\\*(.+?)\\*\\*".toRegex(), "【$1】")  // **텍스트** -> 【텍스트】
                .replace("###\\s*(.+)".toRegex(), "\n▶ $1\n")     // ### 헤딩 -> ▶ 헤딩
                .replace("##\\s*(.+)".toRegex(), "\n■ $1\n")      // ## 헤딩 -> ■ 헤딩  
                .replace("#\\s*(.+)".toRegex(), "\n◆ $1\n")       // # 헤딩 -> ◆ 헤딩
                .replace("•", "▪") // 불릿 포인트 한국어 스타일
                .replace("- ", "▪ ") // 하이픈 불릿을 한국어 스타일로
            
            // 숫자 포맷팅 (천단위 콤마)
            val numberPattern = "\\b(\\d{4,})원?\\b".toRegex()
            val finalContent = numberPattern.replace(formattedContent) { matchResult ->
                val number = matchResult.groupValues[1].toLongOrNull()
                if (number != null) {
                    String.format("%,d", number) + "원"
                } else {
                    matchResult.value
                }
            }
            
            // 빈 줄 정리 (3개 이상의 연속 줄바꿈을 2개로 제한)
            val cleanedContent = finalContent
                .replace("\n{3,}".toRegex(), "\n\n")
                .trim()
            
            android.util.Log.d("AIReportDetail", "포매팅 완료 - 최종 길이: ${cleanedContent.length}")
            
            // 최종 검증
            if (cleanedContent.isBlank()) {
                android.util.Log.w("AIReportDetail", "포매팅 후 내용이 비어있음, 원본 반환")
                return content
            }
            
            cleanedContent
            
        } catch (e: Exception) {
            android.util.Log.e("AIReportDetail", "리포트 내용 포매팅 실패", e)
            // 포매팅 실패 시 원본 반환
            content
        }
    }
}
