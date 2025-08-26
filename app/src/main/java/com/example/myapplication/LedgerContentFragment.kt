package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.myapplication.api.ApiClient
import kotlinx.coroutines.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.example.myapplication.PublicLedgerTransactionAdapter
import com.example.myapplication.UserManager

class LedgerContentFragment : Fragment() {

    private lateinit var contentView: View
    private lateinit var tvYear: TextView
    private lateinit var tvMonth: TextView
    private lateinit var btnPrevMonth: ImageView
    private lateinit var btnNextMonth: ImageView
    private val calendar: Calendar = Calendar.getInstance()

    private lateinit var tvTotalAssets: TextView
    private lateinit var tvTotalAssetsAmount: TextView
    private lateinit var tvAssetsChange: TextView
    private lateinit var tvMonthlyIncome: TextView
    private lateinit var tvMonthlyIncomeAmount: TextView
    private lateinit var tvIncomeChange: TextView
    private lateinit var tvMonthlyExpense: TextView
    private lateinit var tvMonthlyExpenseAmount: TextView
    private lateinit var tvExpenseChange: TextView

    private lateinit var recyclerView: RecyclerView
    private lateinit var transactionAdapter: PublicLedgerTransactionAdapter
    private var fab: FloatingActionButton? = null

    private var allTransactions: List<TransactionItem> = listOf() // 전체 거래 내역 저장

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
            
            // 디버깅: clubId와 ledgerId 값 확인
            Log.d("LedgerContentFragment", "onViewCreated - clubId: $clubId, ledgerId: $ledgerId")

            setupFloatingActionButton()

            // 년/월 뷰 초기화
            tvYear = view.findViewById(R.id.tv_year)
            tvMonth = view.findViewById(R.id.tv_month)
            btnPrevMonth = view.findViewById(R.id.btn_prev_month)
            btnNextMonth = view.findViewById(R.id.btn_next_month)
            
            // 재무 요약 뷰 초기화
            tvTotalAssets = view.findViewById(R.id.tv_total_assets)
            tvTotalAssetsAmount = view.findViewById(R.id.tv_total_assets_amount)
            tvAssetsChange = view.findViewById(R.id.tv_assets_change)
            tvMonthlyIncome = view.findViewById(R.id.tv_monthly_income)
            tvMonthlyIncomeAmount = view.findViewById(R.id.tv_monthly_income_amount)
            tvIncomeChange = view.findViewById(R.id.tv_income_change)
            tvMonthlyExpense = view.findViewById(R.id.tv_monthly_expense)
            tvMonthlyExpenseAmount = view.findViewById(R.id.tv_monthly_expense_amount)
            tvExpenseChange = view.findViewById(R.id.tv_expense_change)
            
                         // RecyclerView 초기화
             recyclerView = view.findViewById(R.id.recycler_view_ledger)
             recyclerView.layoutManager = LinearLayoutManager(requireContext())
             transactionAdapter = PublicLedgerTransactionAdapter(emptyList())
             recyclerView.adapter = transactionAdapter
             
             // 스와이프 삭제 기능 설정
             setupSwipeToDelete()
             
             updateDate() // 이제 transactionAdapter가 초기화된 후에 호출

            // 년/월 변경 리스너 설정
            btnPrevMonth.setOnClickListener {
                calendar.add(Calendar.MONTH, -1)
                updateDate()
                updateFinancialSummary()
            }
            btnNextMonth.setOnClickListener {
                calendar.add(Calendar.MONTH, 1)
                updateDate()
                updateFinancialSummary()
            }

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
        
        tvTotalAssetsAmount.text = "${numberFormat.format(currentBalance)}원"
        tvMonthlyIncomeAmount.text = "${numberFormat.format(currentIncome)}원"
        tvMonthlyExpenseAmount.text = "${numberFormat.format(currentExpense)}원"
        
        // 변화량 계산 및 표시
        val balanceChange = currentBalance - previousBalance
        val incomeChange = currentIncome - previousIncome
        val expenseChange = currentExpense - previousExpense
        
        tvAssetsChange.text = "${if (balanceChange >= 0) "▲" else "▼"} ${numberFormat.format(kotlin.math.abs(balanceChange))}"
        tvAssetsChange.setTextColor(android.graphics.Color.parseColor(if (balanceChange >= 0) "#C50000" else "#2457C5"))
        
        tvIncomeChange.text = "${if (incomeChange >= 0) "▲" else "▼"} ${numberFormat.format(kotlin.math.abs(incomeChange))}"
        tvIncomeChange.setTextColor(android.graphics.Color.parseColor(if (incomeChange >= 0) "#C50000" else "#2457C5"))
        
        tvExpenseChange.text = "${if (expenseChange >= 0) "▲" else "▼"} ${numberFormat.format(kotlin.math.abs(expenseChange))}"
        tvExpenseChange.setTextColor(android.graphics.Color.parseColor(if (expenseChange >= 0) "#C50000" else "#2457C5"))
    }

    private fun applyFiltersAndSort() {
        Log.d("LedgerContentFragment", "applyFiltersAndSort called with ${allTransactions.size} transactions")
        
        // 1. 년/월 필터링
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val selectedMonth = sdf.format(calendar.time)
        Log.d("LedgerContentFragment", "Selected month: $selectedMonth")
        
        val monthlyFiltered = allTransactions.filter {
            try {
                // API에서 받은 날짜(예: "2024-08-21T14:00:00")를 "yyyy-MM" 형식으로 변환하여 비교
                it.dateTime?.startsWith(selectedMonth) == true
            } catch (e: Exception) {
                false
            }
        }
        Log.d("LedgerContentFragment", "Monthly filtered: ${monthlyFiltered.size} transactions")

        // 2. 타입 필터링 (현재는 전체만 표시)
        val typeFiltered = monthlyFiltered

        // 3. 날짜 역순으로 정렬 (최신순)
        val sortedList = typeFiltered.sortedByDescending { it.dateTime }
        Log.d("LedgerContentFragment", "Final sorted list: ${sortedList.size} transactions")

        // 4. 어댑터에 데이터 업데이트
        transactionAdapter.updateData(sortedList)
    }

         private fun setupSwipeToDelete() {
         val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
             0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
         ) {
             override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                 return 0.3f // 30% 스와이프하면 삭제 트리거
             }
             
             override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
                 return defaultValue * 0.5f // 스와이프 속도 감소
             }
             override fun onMove(
                 recyclerView: RecyclerView,
                 viewHolder: RecyclerView.ViewHolder,
                 target: RecyclerView.ViewHolder
             ): Boolean {
                 return false
             }
 
             override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                 val position = viewHolder.adapterPosition
                 if (position != RecyclerView.NO_POSITION) {
                     // 삭제 확인 다이얼로그 표시
                     showDeleteConfirmationDialog(position)
                 }
             }
 
             override fun onChildDraw(
                 c: android.graphics.Canvas,
                 recyclerView: RecyclerView,
                 viewHolder: RecyclerView.ViewHolder,
                 dX: Float,
                 dY: Float,
                 actionState: Int,
                 isCurrentlyActive: Boolean
             ) {
                 if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                     val itemView = viewHolder.itemView
                     val background = android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#B70000"))
                     
                     // 스와이프 방향에 따라 배경 그리기
                     if (dX > 0) { // 오른쪽으로 스와이프
                         background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                     } else if (dX < 0) { // 왼쪽으로 스와이프
                         background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                     }
                     
                     background.draw(c)
                     
                     // 휴지통 아이콘 그리기 (시스템 기본 아이콘 사용)
                     val icon = android.content.res.Resources.getSystem().getDrawable(android.R.drawable.ic_menu_delete, null)
                     if (icon != null) {
                         val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                         val iconTop = itemView.top + iconMargin
                         val iconBottom = iconTop + icon.intrinsicHeight
                         
                         if (dX > 0) { // 오른쪽으로 스와이프
                             val iconLeft = itemView.left + iconMargin
                             val iconRight = iconLeft + icon.intrinsicWidth
                             icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                         } else if (dX < 0) { // 왼쪽으로 스와이프
                             val iconRight = itemView.right - iconMargin
                             val iconLeft = iconRight - icon.intrinsicWidth
                             icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                         }
                         
                         icon.draw(c)
                     }
                 }
                 
                 super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
             }
         })
         
         itemTouchHelper.attachToRecyclerView(recyclerView)
     }
 
     private fun showDeleteConfirmationDialog(position: Int) {
         androidx.appcompat.app.AlertDialog.Builder(requireContext())
             .setTitle("거래 내역 삭제")
             .setMessage("이 거래 내역을 삭제하시겠습니까?")
             .setPositiveButton("삭제") { _, _ ->
                 deleteTransaction(position)
             }
             .setNegativeButton("취소") { _, _ ->
                 // 취소 시 아이템 복원
                 transactionAdapter.notifyItemChanged(position)
             }
             .setCancelable(false)
             .show()
     }
 
     private fun deleteTransaction(position: Int) {
         try {
             // 현재 표시된 거래 내역에서 해당 항목 제거
             val currentList = transactionAdapter.getCurrentList().toMutableList()
             if (position < currentList.size) {
                 val deletedTransaction = currentList[position]
                 
                 // API 호출하여 서버에서 삭제
                 deleteTransactionFromServer(deletedTransaction.id)
                 
                 // 로컬 리스트에서 제거
                 currentList.removeAt(position)
                 transactionAdapter.updateData(currentList)
                 
                 // 전체 거래 내역에서도 제거
                 allTransactions = allTransactions.filter { it.id != deletedTransaction.id }
                 
                 // 재무 요약 업데이트
                 updateFinancialSummary()
             }
         } catch (e: Exception) {
             Log.e("LedgerContentFragment", "거래 내역 삭제 중 오류 발생", e)
             // 오류 발생 시 아이템 복원
             transactionAdapter.notifyItemChanged(position)
         }
     }
 
     private fun deleteTransactionFromServer(transactionId: Int) {
         CoroutineScope(Dispatchers.IO).launch {
             try {
                 val apiService = ApiClient.getApiService()
                 // TODO: 실제 삭제 API 엔드포인트 구현 필요
                 // val response = apiService.deleteTransaction(transactionId).execute()
                 Log.d("LedgerContentFragment", "거래 내역 삭제 API 호출: $transactionId")
             } catch (e: Exception) {
                 Log.e("LedgerContentFragment", "서버 삭제 API 호출 중 오류 발생", e)
             }
         }
     }
 
     private fun setupFloatingActionButton() {
        val rootLayout = activity?.findViewById<View>(android.R.id.content) as? ViewGroup
        
        fab = FloatingActionButton(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
                         ).apply {
                 gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                 setMargins(0, 0, 80, 80)
             }
            setImageResource(R.drawable.pencil)
            backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2457C5"))
            imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            setOnClickListener {
                val intent = Intent(activity, LedgerCreateActivity::class.java)
                // ledgerId만 LedgerCreateActivity로 전달 (clubId는 4로 하드코딩)
                intent.putExtra("ledger_pk", ledgerId.toString())
                startActivity(intent)
                activity?.overridePendingTransition(0, 0)
            }
        }
        
        rootLayout?.addView(fab)
    }

    private fun fetchTransactions() {
        Log.d("LedgerContentFragment", "fetchTransactions called with clubId: $clubId, ledgerId: $ledgerId")
        if (clubId == -1 || ledgerId == -1) {
            Log.e("LedgerContentFragment", "Club ID or Ledger ID is missing.")
            return
        }
        
        // user_pk 가져오기
        val userPk = UserManager.getUserPk(requireContext())
        if (userPk == null) {
            Log.e("LedgerContentFragment", "user_pk가 없습니다. 로그인이 필요합니다.")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ApiClient.getApiService()
                Log.d("LedgerContentFragment", "Calling API: getTransactions($clubId, $ledgerId, $userPk)")
                val response = apiService.getTransactions(clubId, ledgerId, userPk).execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        allTransactions = response.body() ?: emptyList()
                        Log.d("LedgerContentFragment", "API 호출 성공: ${allTransactions.size}개의 거래 내역")
                        updateFinancialSummary()
                        applyFiltersAndSort() // 필터링 및 정렬 적용하여 화면에 표시
                    } else {
                        Log.e("LedgerContentFragment", "서버 응답 오류: ${response.code()}")
                        allTransactions = emptyList()
                        updateFinancialSummary()
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
