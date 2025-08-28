package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.example.myapplication.UserManager

class PublicLedgerFragment : Fragment() {

    private lateinit var tvYear: TextView
    private lateinit var tvMonth: TextView
    private lateinit var btnPrevMonth: TextView
    private lateinit var btnNextMonth: TextView
    
    private lateinit var tvTotalAssets: TextView
    private lateinit var tvAssetsChange: TextView
    private lateinit var tvMonthlyIncome: TextView
    private lateinit var tvIncomeChange: TextView
    private lateinit var tvMonthlyExpense: TextView
    private lateinit var tvExpenseChange: TextView
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var transactionAdapter: PublicLedgerTransactionAdapter
    
    private val calendar: Calendar = Calendar.getInstance()
    private var allTransactions: List<TransactionItem> = listOf()
    
    private var clubPk: Int = -1
    private var ledgerPk: Int = -1

    companion object {
        private const val ARG_CLUB_PK = "club_pk"
        private const val ARG_LEDGER_PK = "ledger_pk"
        private const val ARG_TRANSACTIONS = "transactions"

        fun newInstance(clubPk: Int, ledgerPk: Int, transactions: List<TransactionItem>? = null): PublicLedgerFragment {
            val fragment = PublicLedgerFragment()
            val args = Bundle()
            args.putInt(ARG_CLUB_PK, clubPk)
            args.putInt(ARG_LEDGER_PK, ledgerPk)
            if (transactions != null) {
                // TransactionItem을 직렬화하여 전달
                args.putSerializable(ARG_TRANSACTIONS, ArrayList(transactions))
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            clubPk = it.getInt(ARG_CLUB_PK, -1)
            ledgerPk = it.getInt(ARG_LEDGER_PK, -1)
            
            // 전달받은 거래 내역이 있으면 사용
            val passedTransactions = it.getSerializable(ARG_TRANSACTIONS) as? ArrayList<TransactionItem>
            if (passedTransactions != null) {
                allTransactions = passedTransactions.toList()
                Log.d("PublicLedgerFragment", "전달받은 거래 내역: ${allTransactions.size}건")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_public_ledger, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        if (clubPk == -1 || ledgerPk == -1) {
            Log.e("PublicLedgerFragment", "club_pk 또는 ledger_pk가 전달되지 않았습니다.")
            return
        }
        
        initializeViews(view)
        setupMonthNavigation()
        setupRecyclerView()
        fetchTransactions()
    }

    private fun initializeViews(view: View) {
        tvYear = view.findViewById(R.id.tv_year)
        tvMonth = view.findViewById(R.id.tv_month)
        btnPrevMonth = view.findViewById(R.id.btn_prev_month)
        btnNextMonth = view.findViewById(R.id.btn_next_month)
        
        tvTotalAssets = view.findViewById(R.id.tv_total_assets)
        tvAssetsChange = view.findViewById(R.id.tv_assets_change)
        tvMonthlyIncome = view.findViewById(R.id.tv_monthly_income)
        tvIncomeChange = view.findViewById(R.id.tv_income_change)
        tvMonthlyExpense = view.findViewById(R.id.tv_monthly_expense)
        tvExpenseChange = view.findViewById(R.id.tv_expense_change)
        
        recyclerView = view.findViewById(R.id.recycler_view_transactions)
        
        updateDateDisplay()
    }

    private fun setupMonthNavigation() {
        btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateDateDisplay()
            updateFinancialSummary()
            updateTransactionList()
        }
        
        btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateDateDisplay()
            updateFinancialSummary()
            updateTransactionList()
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        transactionAdapter = PublicLedgerTransactionAdapter(emptyList())
        recyclerView.adapter = transactionAdapter
    }

    private fun updateDateDisplay() {
        val yearFormat = SimpleDateFormat("yyyy년", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MM월", Locale.getDefault())
        tvYear.text = yearFormat.format(calendar.time)
        tvMonth.text = monthFormat.format(calendar.time)
        
        updateFinancialSummary()
        updateTransactionList()
    }

    private fun fetchTransactions() {
        // 전달받은 거래 내역이 있으면 API 호출 건너뛰기
        if (allTransactions.isNotEmpty()) {
            Log.d("PublicLedgerFragment", "전달받은 거래 내역 사용: ${allTransactions.size}건")
            updateFinancialSummary()
            updateTransactionList()
            return
        }
        
        // user_pk 가져오기
        val userPk = UserManager.getUserPk(requireContext())
        if (userPk == null) {
            Log.e("PublicLedgerFragment", "user_pk가 없습니다. 로그인이 필요합니다.")
            return
        }
        
        ApiClient.getApiService().getTransactions(clubPk, ledgerPk, userPk).enqueue(object : Callback<List<TransactionItem>> {
            override fun onResponse(call: Call<List<TransactionItem>>, response: Response<List<TransactionItem>>) {
                if (response.isSuccessful && response.body() != null) {
                    allTransactions = response.body()!!
                    Log.d("PublicLedgerFragment", "거래내역 조회 성공: ${allTransactions.size}개")
                    
                    updateFinancialSummary()
                    updateTransactionList()
                } else {
                    Log.e("PublicLedgerFragment", "거래내역 조출 실패: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<TransactionItem>>, t: Throwable) {
                Log.e("PublicLedgerFragment", "API 호출 실패: ${t.message}")
            }
        })
    }

    private fun updateFinancialSummary() {
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
        val previousMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).apply {
            calendar.add(Calendar.MONTH, -1)
            format(calendar.time)
            calendar.add(Calendar.MONTH, 1) // 원래 위치로 복원
        }.toString()
        
        // 현재 월 거래내역 필터링 - dateTime이 null이 아닌 경우만 처리
        val currentMonthTransactions = allTransactions.filter {
            it.dateTime?.startsWith(currentMonth) == true
        }
        
        // 이전 월 거래내역 필터링 - dateTime이 null이 아닌 경우만 처리
        val previousMonthTransactions = allTransactions.filter {
            it.dateTime?.startsWith(previousMonth) == true
        }
        
        // 수입/지출 계산
        val currentIncome = currentMonthTransactions.filter { it.amount > 0 }.sumOf { it.amount }
        val currentExpense = currentMonthTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
        val currentBalance = currentIncome - currentExpense
        
        val previousIncome = previousMonthTransactions.filter { it.amount > 0 }.sumOf { it.amount }
        val previousExpense = previousMonthTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
        val previousBalance = previousIncome - previousExpense
        
        // UI 업데이트
        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
        
        tvTotalAssets.text = "${numberFormat.format(currentBalance)}원"
        tvMonthlyIncome.text = "${numberFormat.format(currentIncome)}원"
        tvMonthlyExpense.text = "${numberFormat.format(currentExpense)}원"
        
        // 변화량 계산 및 표시
        val balanceChange = currentBalance - previousBalance
        val incomeChange = currentIncome - previousIncome
        val expenseChange = currentExpense - previousExpense
        
        tvAssetsChange.text = "전월 대비 ${if (balanceChange >= 0) "▲" else "▼"} ${numberFormat.format(kotlin.math.abs(balanceChange))}"
        tvAssetsChange.setTextColor(requireContext().getColor(if (balanceChange >= 0) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
        
        tvIncomeChange.text = "전월 대비 ${if (incomeChange >= 0) "▲" else "▼"} ${numberFormat.format(kotlin.math.abs(incomeChange))}"
        tvIncomeChange.setTextColor(requireContext().getColor(if (incomeChange >= 0) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
        
        tvExpenseChange.text = "전월 대비 ${if (expenseChange >= 0) "▲" else "▼"} ${numberFormat.format(kotlin.math.abs(expenseChange))}"
        tvExpenseChange.setTextColor(requireContext().getColor(if (expenseChange >= 0) android.R.color.holo_red_dark else android.R.color.holo_green_dark))
    }

    private fun updateTransactionList() {
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
        
        val monthlyTransactions = allTransactions.filter {
            it.dateTime?.startsWith(currentMonth) == true
        }.sortedByDescending { it.dateTime }
        
        transactionAdapter.updateData(monthlyTransactions)
    }
}
