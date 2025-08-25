package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.myapplication.api.ApiClient
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Color

class LedgerContentFragment : Fragment() {

    private lateinit var contentView: View
    private lateinit var tvYear: TextView
    private lateinit var tvMonth: TextView
    private lateinit var btnPrevMonth: View
    private lateinit var btnNextMonth: View
    private val calendar: Calendar = Calendar.getInstance()

    private lateinit var recyclerView: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private var fab: FloatingActionButton? = null

    private var allTransactions: List<TransactionItem> = listOf() // 전체 거래 내역 저장
    private var currentFilterType = "전체" // 현재 필터 타입 ("전체", "수입", "지출")

    private var clubId: Int = -1
    private var ledgerId: Int = -1

    companion object {
        private const val ARG_CLUB_ID = "club_id"
        private const val ARG_LEDGER_ID = "ledger_id"

        fun newInstance(clubId: Int, ledgerId: Int): LedgerContentFragment {
            val fragment = LedgerContentFragment()
            val args = Bundle()
            args.putInt(ARG_CLUB_ID, clubId)
            args.putInt(ARG_LEDGER_ID, ledgerId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            clubId = it.getInt(ARG_CLUB_ID)
            ledgerId = it.getInt(ARG_LEDGER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        contentView = inflater.inflate(R.layout.ledger_content, container, false)
        return contentView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fab?.let {
            (activity?.findViewById<View>(android.R.id.content) as? ViewGroup)?.removeView(it)
        }
        fab = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            (activity as? BaseActivity)?.setAppTitle("장부 상세내역")

            setupFloatingActionButton()

            // 년/월 뷰 초기화
            tvYear = view.findViewById(R.id.tv_year)
            tvMonth = view.findViewById(R.id.tv_month)
            btnPrevMonth = view.findViewById(R.id.btn_prev_month)
            btnNextMonth = view.findViewById(R.id.btn_next_month)
            updateDate() // updateDate 내부에서 필터링을 호출하므로 초기 날짜 설정이 중요

            // 년/월 변경 리스너 설정
            btnPrevMonth.setOnClickListener {
                calendar.add(Calendar.MONTH, -1)
                updateDate()
            }
            btnNextMonth.setOnClickListener {
                calendar.add(Calendar.MONTH, 1)
                updateDate()
            }

            // 타입 필터 버튼 리스너 설정
            setupFilterButtons(view)

            // RecyclerView 초기화
            recyclerView = view.findViewById(R.id.recycler_view_ledger)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            transactionAdapter = TransactionAdapter(emptyList())
            recyclerView.adapter = transactionAdapter

            // API 호출
            fetchTransactions()

        } catch (e: Exception) {
            Log.e("LedgerContentFragment", "onViewCreated에서 오류 발생!", e)
        }
    }
    
    private fun updateDate() {
        val yearFormat = SimpleDateFormat("yyyy년", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MM월", Locale.getDefault())
        tvYear.text = yearFormat.format(calendar.time)
        tvMonth.text = monthFormat.format(calendar.time)
        applyFiltersAndSort() // 날짜가 변경될 때마다 필터링 다시 적용
    }

    private fun setupFilterButtons(view: View) {
        val btnAll = view.findViewById<TextView>(R.id.btn_filter_all)
        val btnIncome = view.findViewById<TextView>(R.id.btn_filter_income)
        val btnExpense = view.findViewById<TextView>(R.id.btn_filter_expense)
        val buttons = listOf(btnAll, btnIncome, btnExpense)

        btnAll.setOnClickListener { 
            currentFilterType = "전체"
            updateButtonStyles(buttons, btnAll)
            applyFiltersAndSort()
        }
        btnIncome.setOnClickListener { 
            currentFilterType = "수입"
            updateButtonStyles(buttons, btnIncome)
            applyFiltersAndSort()
        }
        btnExpense.setOnClickListener { 
            currentFilterType = "지출"
            updateButtonStyles(buttons, btnExpense)
            applyFiltersAndSort()
        }
    }

    private fun updateButtonStyles(buttons: List<TextView>, selectedButton: TextView) {
        buttons.forEach { button ->
            if (button == selectedButton) {
                button.setBackgroundResource(R.drawable.btn_selected)
                button.setTextColor(Color.WHITE)
            } else {
                button.setBackgroundResource(R.drawable.btn_unselected)
                button.setTextColor(Color.parseColor("#333333"))
            }
        }
    }
    
    private fun applyFiltersAndSort() {
        // 1. 년/월 필터링
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val selectedMonth = sdf.format(calendar.time)
        
        val monthlyFiltered = allTransactions.filter {
            try {
                // API에서 받은 날짜(예: "2024-08-21T14:00:00")를 "yyyy-MM" 형식으로 변환하여 비교
                it.date.startsWith(selectedMonth)
            } catch (e: Exception) {
                false
            }
        }

        // 2. 타입 필터링
        val typeFiltered = if (currentFilterType == "전체") {
            monthlyFiltered
        } else {
            monthlyFiltered.filter { it.type == currentFilterType }
        }

        // 3. 날짜 역순으로 정렬 (최신순)
        val sortedList = typeFiltered.sortedByDescending { it.date }

        // 4. 어댑터에 데이터 업데이트
        transactionAdapter.updateData(sortedList)
    }

    private fun setupFloatingActionButton() {
        val rootLayout = activity?.findViewById<View>(android.R.id.content) as? ViewGroup
        
        fab = FloatingActionButton(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                setMargins(0, 0, 32, 32)
            }
            setImageResource(R.drawable.pencil)
            backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2457C5"))
            imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            setOnClickListener {
                val intent = Intent(activity, LedgerCreateActivity::class.java)
                // TODO: clubId와 ledgerId를 LedgerCreateActivity로 전달해야 함
                startActivity(intent)
                activity?.overridePendingTransition(0, 0)
            }
        }
        
        rootLayout?.addView(fab)
    }

    private fun fetchTransactions() {
        if (clubId == -1 || ledgerId == -1) {
            Log.e("LedgerContentFragment", "Club ID or Ledger ID is missing.")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ApiClient.getApiService()
                val response = apiService.getTransactions(clubId, ledgerId).execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        allTransactions = response.body() ?: emptyList()
                        applyFiltersAndSort() // 필터링 및 정렬 적용하여 화면에 표시
                    } else {
                        Log.e("LedgerContentFragment", "서버 응답 오류: ${response.code()}")
                        allTransactions = emptyList()
                        applyFiltersAndSort()
                    }
                }
            } catch (e: Exception) {
                Log.e("LedgerContentFragment", "API 호출 중 에러 발생", e)
                withContext(Dispatchers.Main) {
                    allTransactions = emptyList()
                    applyFiltersAndSort()
                }
            }
        }
    }
}
