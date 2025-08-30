package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.util.Log
import com.google.gson.Gson
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone

class AccountHistoryActivity : AppCompatActivity() {
    private var currentYear: Int = 0
    private var currentMonth: Int = 0 // 1~12
    private var todayYear: Int = 0
    private var todayMonth: Int = 0

    // 거래 내역 관련 변수
    private lateinit var transactionRecyclerView: RecyclerView
    private var accountNo: String = ""
    private var userName: String = ""
    private var allTransactions = listOf<Transaction>() // 전체 거래 내역 저장

    // 거래 내역 데이터 클래스
    data class Transaction(
        val transactionUniqueNo: Long,
        val transactionDate: String,
        val transactionTime: String,
        val transactionType: String, // "1" = 입금(수입), "2" = 출금(지출)
        val transactionTypeName: String,
        val transactionBalance: Long,
        val transactionAfterBalance: Long,
        val transactionSummary: String?,
        val transactionMemo: String?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_history)

        // 계좌번호와 사용자 이름 받아오기
        accountNo = intent?.getStringExtra("accountNo") ?: ""
        userName = intent?.getStringExtra("userName") ?: "사용자"

        // 계좌번호가 있으면 표시
        if (accountNo.isNotEmpty()) {
            findViewById<TextView>(R.id.tv_account_number)?.text = accountNo
        }

        // 예금주명 표시
        findViewById<TextView>(R.id.tv_holder)?.text = userName

        // 은행명을 신한은행으로 하드코딩
        findViewById<TextView>(R.id.tv_bank)?.text = "신한은행"

        // RecyclerView 초기화
        setupTransactionRecyclerView()

        // 거래 내역 로드
        if (accountNo.isNotEmpty()) {
            loadTransactionHistory()
        }

        // 헤더의 동아리명 표기 (공지/자유 리스트와 동일한 방식)
        val clubPk = intent?.getIntExtra("club_pk", -1) ?: -1
        if (clubPk > 0) {
            com.example.myapplication.api.ApiClient.getApiService().getClubDetail(clubPk)
                .enqueue(object : retrofit2.Callback<com.example.myapplication.ClubItem> {
                    override fun onResponse(
                        call: retrofit2.Call<com.example.myapplication.ClubItem>,
                        response: retrofit2.Response<com.example.myapplication.ClubItem>
                    ) {
                        val club = response.body()
                        if (response.isSuccessful && club != null) {
                            findViewById<TextView>(R.id.tv_club_title)?.text = club.name
                        }
                    }
                    override fun onFailure(
                        call: retrofit2.Call<com.example.myapplication.ClubItem>,
                        t: Throwable
                    ) { }
                })
        }

        findViewById<android.widget.Button>(R.id.btn_back)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 상단 카테고리 네비게이션
        findViewById<TextView>(R.id.btn_notice)?.setOnClickListener {
            val intent = Intent(this, ClubAnnouncementBoardListActivity::class.java)
            intent.putExtra("club_pk", clubPk)
            startActivity(intent)
            finish()
        }
        findViewById<TextView>(R.id.btn_free_board)?.setOnClickListener {
            val intent = Intent(this, ClubForumBoardListActivity::class.java)
            intent.putExtra("club_pk", clubPk)
            startActivity(intent)
            finish()
        }
        findViewById<TextView>(R.id.btn_event_account)?.setOnClickListener {
            val intent = Intent(this, ClubEventLedgerListActivity::class.java)
            intent.putExtra(ClubEventLedgerListActivity.EXTRA_CLUB_PK, clubPk)
            startActivity(intent)
            finish()
        }

        // 공개 장부 버튼 클릭 시 MainActivity로 이동하여 LedgerContentFragment 표시
        findViewById<TextView>(R.id.btn_public_account)?.setOnClickListener {
            // MainActivity로 이동하여 LedgerContentFragment 표시 (root_page와 동일한 과정)
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("show_public_ledger", true)
            intent.putExtra("club_pk", clubPk)
            // ledger_pk는 MainActivity에서 동아리 ID를 기반으로 조회하도록 수정
            startActivity(intent)
            finish()
        }

        // 초기 년/월 세팅 (오늘 기준)
        val now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"))
        todayYear = now.year
        todayMonth = now.monthValue
        currentYear = todayYear
        currentMonth = todayMonth
        updateYearMonthUi()

        // 월 이동 버튼
        findViewById<android.widget.ImageView>(R.id.btn_prev_month)?.setOnClickListener {
            moveMonth(-1)
        }
        findViewById<android.widget.ImageView>(R.id.btn_next_month)?.setOnClickListener {
            moveMonth(1)
        }

        // 계좌 연동 토글: ON 기본, OFF로 내릴 때 확인 다이얼로그
        val switch = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.sw_account_visible)
        switch?.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                showAccountUnlinkDialog(onCancelled = { switch.isChecked = true }, onConfirm = {
                    // TODO: 연동 해제 API 호출이 있다면 연결
                })
            }
        }
    }

    // RecyclerView 설정
    private fun setupTransactionRecyclerView() {
        transactionRecyclerView = findViewById(R.id.rv_transactions)
        transactionRecyclerView.layoutManager = LinearLayoutManager(this)

        // Adapter 설정
        val adapter = TransactionAdapter()
        transactionRecyclerView.adapter = adapter
    }

    // 거래 내역 로드
    private fun loadTransactionHistory() {
        // 현재 날짜와 시간 가져오기 (한국 시간대)
        val koreaTimeZone = TimeZone.getTimeZone("Asia/Seoul")
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).apply { timeZone = koreaTimeZone }
        val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault()).apply { timeZone = koreaTimeZone }

        val currentDate = dateFormat.format(Date())
        val currentTime = timeFormat.format(Date())

        // 20자리 랜덤 난수 생성
        val randomNumber = generateUnique20DigitNumber()

        // API 요청 데이터 구성
        val requestData = mapOf(
            "Header" to mapOf(
                "apiName" to "inquireTransactionHistoryList",
                "transmissionDate" to currentDate,
                "transmissionTime" to currentTime,
                "institutionCode" to "00100",
                "fintechAppNo" to "001",
                "apiServiceCode" to "inquireTransactionHistoryList",
                "institutionTransactionUniqueNo" to randomNumber,
                "apiKey" to "7f9fc447584741399a5dfab7dd3ea443",
                "userKey" to "1607a094-72cc-4d4f-9ed3-e7cd1d264e2d"
            ),
            "accountNo" to accountNo,
            "startDate" to "20240101", // 2024년 1월 1일부터
            "endDate" to currentDate,  // 오늘까지
            "transactionType" to "A",  // 모든 거래 내역
            "orderByType" to "DESC"    // 내림차순 (최신순)
        )

        Log.d("AccountHistoryActivity", "거래 내역 API 요청: ${Gson().toJson(requestData)}")

        // 백그라운드에서 API 호출
        Thread {
            try {
                callTransactionHistoryAPI(requestData)
            } catch (e: Exception) {
                Log.e("AccountHistoryActivity", "거래 내역 로드 실패", e)
            }
        }.start()
    }

    // 20자리 고유 난수 생성
    private fun generateUnique20DigitNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1..999999L).random()
        val combined = "$timestamp$random"

        return if (combined.length >= 20) {
            combined.takeLast(20)
        } else {
            combined.padStart(20, '0')
        }
    }

    // 거래 내역 API 호출
    private fun callTransactionHistoryAPI(requestData: Map<String, Any>) {
        try {
            val url = "https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireTransactionHistoryList"
            val connection = URL(url).openConnection() as HttpURLConnection

            // POST 요청 설정
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // JSON 데이터 전송
            val jsonData = Gson().toJson(requestData)
            connection.outputStream.use { os ->
                os.write(jsonData.toByteArray())
            }

            // 응답 받기
            val responseCode = connection.responseCode
            Log.d("AccountHistoryActivity", "거래 내역 API 응답 코드: $responseCode")

            if (responseCode == 200 || responseCode == 201) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("AccountHistoryActivity", "거래 내역 API 응답: $response")

                // JSON 응답 파싱 및 UI 업데이트
                parseTransactionHistoryResponse(response)
            } else {
                Log.e("AccountHistoryActivity", "거래 내역 API 호출 실패: $responseCode")
            }

        } catch (e: Exception) {
            Log.e("AccountHistoryActivity", "거래 내역 API 호출 중 오류 발생", e)
        }
    }

    // 거래 내역 응답 파싱
    private fun parseTransactionHistoryResponse(response: String) {
        try {
            val responseJson = Gson().fromJson(response, Map::class.java)
            Log.d("AccountHistoryActivity", "전체 응답: $responseJson")

            val rec = responseJson["REC"] as? Map<*, *>
            Log.d("AccountHistoryActivity", "REC 객체: $rec")

            if (rec != null) {
                val transactionList = rec["list"] as? List<Map<*, *>>
                Log.d("AccountHistoryActivity", "거래 내역 리스트: $transactionList")

                if (transactionList != null) {
                    val transactions = transactionList.mapNotNull { transaction ->
                        try {
                            Log.d("AccountHistoryActivity", "개별 거래 데이터: $transaction")

                            val transactionBalance = transaction["transactionBalance"]
                            val transactionAfterBalance = transaction["transactionAfterBalance"]

                            Log.d("AccountHistoryActivity", "거래금액 원본: $transactionBalance (타입: ${transactionBalance?.javaClass?.simpleName})")
                            Log.d("AccountHistoryActivity", "거래후잔액 원본: $transactionAfterBalance (타입: ${transactionAfterBalance?.javaClass?.simpleName})")

                            val parsedTransaction = Transaction(
                                transactionUniqueNo = (transaction["transactionUniqueNo"] as? Number)?.toLong() ?: 0L,
                                transactionDate = transaction["transactionDate"]?.toString() ?: "",
                                transactionTime = transaction["transactionTime"]?.toString() ?: "",
                                transactionType = transaction["transactionType"]?.toString() ?: "",
                                transactionTypeName = transaction["transactionTypeName"]?.toString() ?: "",
                                transactionBalance = parseAmount(transactionBalance),
                                transactionAfterBalance = parseAmount(transactionAfterBalance),
                                transactionSummary = transaction["transactionSummary"]?.toString(),
                                transactionMemo = transaction["transactionMemo"]?.toString()
                            )

                            Log.d("AccountHistoryActivity", "파싱된 거래: $parsedTransaction")
                            parsedTransaction
                        } catch (e: Exception) {
                            Log.e("AccountHistoryActivity", "거래 내역 파싱 오류", e)
                            null
                        }
                    }

                    // 메인 스레드에서 UI 업데이트
                    runOnUiThread {
                        updateTransactionList(transactions)
                    }
                } else {
                    Log.e("AccountHistoryActivity", "거래 내역 리스트가 null입니다")
                }
            } else {
                Log.e("AccountHistoryActivity", "REC 객체가 null입니다")
            }
        } catch (e: Exception) {
            Log.e("AccountHistoryActivity", "거래 내역 응답 파싱 오류", e)
        }
    }

    // 금액 파싱 헬퍼 함수
    private fun parseAmount(amount: Any?): Long {
        return when (amount) {
            is Number -> amount.toLong()
            is String -> {
                try {
                    amount.toLong()
                } catch (e: NumberFormatException) {
                    Log.e("AccountHistoryActivity", "문자열을 숫자로 변환 실패: $amount", e)
                    0L
                }
            }
            else -> {
                Log.e("AccountHistoryActivity", "알 수 없는 금액 타입: $amount (${amount?.javaClass?.simpleName})")
                0L
            }
        }
    }

    // 금액을 3자리씩 끊어서 표시하는 함수
    private fun formatAmount(amount: Long): String {
        return String.format("%,d", amount)
    }

    // 거래 내역 목록 UI 업데이트
    private fun updateTransactionList(transactions: List<Transaction>) {
        Log.d("AccountHistoryActivity", "거래 내역 ${transactions.size}건 로드됨")

        // 전체 거래 내역 저장
        allTransactions = transactions

        // 현재 월의 거래 내역만 필터링하여 표시
        filterTransactionsByMonth()

        // 로그 출력
        transactions.forEach { transaction ->
            val amountText = when (transaction.transactionType) {
                "1" -> "+${transaction.transactionBalance}" // 입금(수입)
                "2" -> "-${transaction.transactionBalance}" // 출금(지출)
                else -> "${transaction.transactionBalance}"
            }
            val summary = transaction.transactionSummary ?: transaction.transactionTypeName
            Log.d("AccountHistoryActivity", "${transaction.transactionDate} ${summary}: $amountText (잔액: ${transaction.transactionAfterBalance})")
        }
    }

    // 재무 요약 표 업데이트
    private fun updateFinancialSummary(transactions: List<Transaction>) {
        // 총 자산 (현재 선택된 월의 거래 내역 기준으로 계산)
        val totalBalance = if (transactions.isNotEmpty()) {
            // 해당 월에 거래가 있으면 가장 최근 거래의 잔액
            transactions.firstOrNull()?.transactionAfterBalance ?: 0L
        } else {
            // 해당 월에 거래가 없으면 0원
            0L
        }
        findViewById<TextView>(R.id.tv_total_balance)?.text = "${formatAmount(totalBalance)}원"

        // 당월 수입 (현재 선택된 월의 입금 거래 합계)
        val monthIncome = transactions
            .filter { it.transactionType == "1" }
            .sumOf { it.transactionBalance }
        findViewById<TextView>(R.id.tv_month_income)?.text = "${formatAmount(monthIncome)}원"

        // 당월 지출 (현재 선택된 월의 출금 거래 합계)
        val monthExpense = transactions
            .filter { it.transactionType == "2" }
            .sumOf { it.transactionBalance }
        findViewById<TextView>(R.id.tv_month_expense)?.text = "${formatAmount(monthExpense)}원"

        // 전월 대비 변화량 계산 (간단한 예시)
        // TODO: 실제로는 전월 데이터와 비교해야 함
        val totalDiff = 0L // 전월 대비 총 자산 변화
        val incomeDiff = 0L // 전월 대비 수입 변화
        val expenseDiff = 0L // 전월 대비 지출 변화

        // 변화량 표시
        findViewById<TextView>(R.id.tv_total_diff)?.text = "전월 대비 ${if (totalDiff >= 0) "▲" else "▼"}${formatAmount(kotlin.math.abs(totalDiff))}"
        findViewById<TextView>(R.id.tv_month_income_diff)?.text = "전월 대비 ${if (incomeDiff >= 0) "▲" else "▼"}${formatAmount(kotlin.math.abs(incomeDiff))}"
        findViewById<TextView>(R.id.tv_month_expense_diff)?.text = "전월 대비 ${if (expenseDiff >= 0) "▲" else "▼"}${formatAmount(kotlin.math.abs(expenseDiff))}"

        // 색상 설정
        findViewById<TextView>(R.id.tv_total_diff)?.setTextColor(
            if (totalDiff >= 0) 0xFF2457C5.toInt() else 0xFFD32F2F.toInt()
        )
        findViewById<TextView>(R.id.tv_month_income_diff)?.setTextColor(
            if (incomeDiff >= 0) 0xFF2457C5.toInt() else 0xFFD32F2F.toInt()
        )
        findViewById<TextView>(R.id.tv_month_expense_diff)?.setTextColor(
            if (expenseDiff >= 0) 0xFF2457C5.toInt() else 0xFFD32F2F.toInt()
        )

        Log.d("AccountHistoryActivity", "${currentYear}년 ${currentMonth}월 재무 요약: 총자산=${formatAmount(totalBalance)}, 당월수입=${formatAmount(monthIncome)}, 당월지출=${formatAmount(monthExpense)}")
    }

    // 현재 월의 거래 내역만 필터링하여 표시
    private fun filterTransactionsByMonth() {
        if (allTransactions.isEmpty()) return

        // 현재 월의 거래만 필터링
        val currentMonthTransactions = allTransactions.filter { transaction ->
            val transactionDate = transaction.transactionDate
            if (transactionDate.length == 8) {
                val transactionYear = transactionDate.substring(0, 4).toIntOrNull()
                val transactionMonth = transactionDate.substring(4, 6).toIntOrNull()
                transactionYear == currentYear && transactionMonth == currentMonth
            } else false
        }

        Log.d("AccountHistoryActivity", "${currentYear}년 ${currentMonth}월 거래 내역 ${currentMonthTransactions.size}건 필터링됨")

        // RecyclerView Adapter로 현재 월 거래 내역 표시
        val adapter = transactionRecyclerView.adapter as? TransactionAdapter
        adapter?.updateTransactions(currentMonthTransactions)

        // 재무 요약 표 업데이트
        // 총 자산은 전체 거래 내역 기준, 당월 수입/지출은 현재 월 거래만 계산
        updateFinancialSummary(currentMonthTransactions)

        // 거래 내역이 없는 경우 안내 메시지 표시
        if (currentMonthTransactions.isEmpty()) {
            Log.d("AccountHistoryActivity", "${currentYear}년 ${currentMonth}월에는 거래 내역이 없습니다")
        }
    }

    private fun moveMonth(delta: Int) {
        // delta: -1 이전달, +1 다음달
        var y = currentYear
        var m = currentMonth + delta
        if (m < 1) { m = 12; y -= 1 }
        if (m > 12) { m = 1; y += 1 }
        // 미래로 이동 금지
        if (y > todayYear || (y == todayYear && m > todayMonth)) {
            updateArrowsEnabled()
            return
        }
        currentYear = y
        currentMonth = m
        updateYearMonthUi()

        // 해당 월의 거래 내역만 필터링하여 표시
        filterTransactionsByMonth()
    }

    private fun updateYearMonthUi() {
        val tvYear = findViewById<TextView>(R.id.tv_year)
        val tvMonth = findViewById<TextView>(R.id.tv_month)
        tvYear?.text = String.format("%d년", currentYear)
        tvMonth?.text = String.format("%02d월", currentMonth)
        updateArrowsEnabled()
    }

    private fun showAccountUnlinkDialog(onCancelled: () -> Unit, onConfirm: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_confirm, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setGravity(android.view.Gravity.BOTTOM)
        dialog.window?.attributes?.windowAnimations = R.style.Animation_Dialog
        dialog.setCanceledOnTouchOutside(true)

        // 텍스트 교체
        dialogView.findViewById<TextView>(R.id.tv_message)?.text = "계좌 연동을 해제하시겠습니까"
        dialogView.findViewById<android.widget.Button>(R.id.btn_delete)?.apply {
            text = "해제하기"
            setOnClickListener {
                onConfirm()
                dialog.dismiss()
            }
        }

        dialog.setOnCancelListener { onCancelled() }
        dialog.show()
    }

    private fun updateArrowsEnabled() {
        val next = findViewById<android.widget.ImageView>(R.id.btn_next_month)
        val atToday = (currentYear == todayYear && currentMonth == todayMonth)
        next?.isEnabled = !atToday
        next?.alpha = if (atToday) 0.3f else 1.0f
    }

    // 현재 월의 거래 내역을 TransactionItem 형태로 변환
    private fun getCurrentMonthTransactionsAsTransactionItems(): List<TransactionItem> {
        if (allTransactions.isEmpty()) return emptyList()

        // 현재 월의 거래만 필터링
        val currentMonthTransactions = allTransactions.filter { transaction ->
            val transactionDate = transaction.transactionDate
            if (transactionDate.length == 8) {
                val transactionYear = transactionDate.substring(0, 4).toIntOrNull()
                val transactionMonth = transactionDate.substring(4, 6).toIntOrNull()
                transactionYear == currentYear && transactionMonth == currentMonth
            } else false
        }

        // Transaction을 TransactionItem으로 변환
        return currentMonthTransactions.map { transaction ->
            TransactionItem(
                id = transaction.transactionUniqueNo.toInt(),
                amount = when (transaction.transactionType) {
                    "1" -> transaction.transactionBalance // 입금(수입)
                    "2" -> -transaction.transactionBalance // 출금(지출)
                    else -> 0L
                },
                category = transaction.transactionTypeName,
                type = when (transaction.transactionType) {
                    "1" -> "입금"
                    "2" -> "출금"
                    else -> "기타"
                },
                dateTime = "${transaction.transactionDate.substring(0, 4)}-${transaction.transactionDate.substring(4, 6)}-${transaction.transactionDate.substring(6, 8)}T${transaction.transactionTime.substring(0, 2)}:${transaction.transactionTime.substring(2, 4)}:${transaction.transactionTime.substring(4, 6)}",
                vendor = transaction.transactionSummary ?: transaction.transactionTypeName,
                description = transaction.transactionMemo ?: "",
                paymentMethod = "계좌이체", // 기본값 설정
                receipt = null,
                author = userName
            )
        }
    }
}


