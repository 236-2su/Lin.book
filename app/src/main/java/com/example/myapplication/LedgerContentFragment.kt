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
import com.example.myapplication.TransactionDetailFragment
import com.example.myapplication.BaseActivity

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

    private var clubId: Int = -1  // arguments에서 받아올 예정
    private var ledgerId: Int = 0  // 고정값

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
            Log.d("LedgerContentFragment", "onCreate - arguments에서 받은 값: clubId=$clubId, ledgerId=$ledgerId")
        } ?: run {
            Log.e("LedgerContentFragment", "onCreate - arguments가 null입니다!")
        }
    }
    
    // clubId를 반환하는 메서드 (MainActivity에서 이모티콘 버튼 클릭 시 사용)
    fun getClubPk(): Int = clubId

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        contentView = inflater.inflate(R.layout.ledger_content, container, false)
        return contentView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        
        // 현재 FAB 제거
        fab?.let {
            try {
                val parent = it.parent as? ViewGroup
                parent?.removeView(it)
            } catch (e: Exception) {
                Log.e("LedgerContentFragment", "FAB 제거 실패", e)
            }
        }
        
        // root_layout에서 모든 FloatingActionButton 제거 (중복 방지)
        val rootLayout = activity?.findViewById<View>(R.id.content_container) as? ViewGroup
        rootLayout?.let { layout ->
            for (i in layout.childCount - 1 downTo 0) {
                val child = layout.getChildAt(i)
                if (child is FloatingActionButton) {
                    try {
                        layout.removeView(child)
                        Log.d("LedgerContentFragment", "FAB 제거됨: ${child.id}")
                    } catch (e: Exception) {
                        Log.e("LedgerContentFragment", "FAB 제거 중 오류", e)
                    }
                }
            }
        }
        
        fab = null
        Log.d("LedgerContentFragment", "onDestroyView 완료: 모든 FAB 제거됨")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            // 동아리 정보를 가져와서 제목 설정
            fetchClubNameAndSetTitle()
            
            // 디버깅: clubId와 ledgerId 값 확인
            Log.d("LedgerContentFragment", "onViewCreated - clubId: $clubId, ledgerId: $ledgerId")
            Log.d("LedgerContentFragment", "onViewCreated - arguments 확인: ${arguments}")
            Log.d("LedgerContentFragment", "onViewCreated - ARG_CLUB_ID: ${arguments?.getInt(ARG_CLUB_ID, -1)}")
            Log.d("LedgerContentFragment", "onViewCreated - ARG_LEDGER_ID: ${arguments?.getInt(ARG_LEDGER_ID, -1)}")

            // 상단 네비게이션 버튼들의 상태 설정 (공개장부 버튼이 선택된 상태로 표시)
            setupNavigationButtonStates()

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
             
             // RecyclerView 항목 클릭 이벤트 설정
             setupRecyclerViewClickListeners()
             
             updateDate() // 이제 transactionAdapter가 초기화된 후에 호출

            // 년/월 변경 리스너 설정
            btnPrevMonth.setOnClickListener {
                if (canMoveToPreviousMonth()) {
                    calendar.add(Calendar.MONTH, -1)
                    updateDate()
                    updateFinancialSummary()
                    updateMonthNavigationButtons()
                }
            }
            btnNextMonth.setOnClickListener {
                if (canMoveToNextMonth()) {
                    calendar.add(Calendar.MONTH, 1)
                    updateDate()
                    updateFinancialSummary()
                    updateMonthNavigationButtons()
                }
            }

            // API 호출
            fetchTransactions()
            
            // 초기 월 이동 버튼 상태 설정 (거래 내역이 로드된 후 업데이트됨)
            updateMonthNavigationButtons()

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
        // 기존 FAB이 있다면 제거
        fab?.let { existingFab ->
            try {
                (activity?.findViewById<View>(android.R.id.content) as? ViewGroup)?.removeView(existingFab)
            } catch (e: Exception) {
                Log.e("LedgerContentFragment", "기존 FAB 제거 실패", e)
            }
        }
        
        // root_layout에서 기존 FAB들 모두 제거 (중복 방지)
        val rootLayout = activity?.findViewById<View>(android.R.id.content) as? ViewGroup
        rootLayout?.let { layout ->
            // FAB ID로 등록된 모든 뷰 제거
            val existingFabs = layout.findViewById<FloatingActionButton>(R.id.fab_register)
            existingFabs?.let { layout.removeView(it) }
            
            // 또는 모든 FloatingActionButton 찾아서 제거
            for (i in layout.childCount - 1 downTo 0) {
                val child = layout.getChildAt(i)
                if (child is FloatingActionButton) {
                    layout.removeView(child)
                }
            }
        }
        
        fab = FloatingActionButton(requireContext()).apply {
            id = R.id.fab_register  // ID 설정
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                setMargins(0, 0, 80, 80)
            }
            setImageResource(android.R.drawable.ic_input_add)
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
        Log.d("LedgerContentFragment", "FAB 생성 완료: id=${fab?.id}")
    }

    private fun fetchTransactions() {
        Log.d("LedgerContentFragment", "fetchTransactions called with clubId: $clubId, ledgerId: $ledgerId")
        Log.d("LedgerContentFragment", "fetchTransactions - arguments 확인: ${arguments}")
        Log.d("LedgerContentFragment", "fetchTransactions - ARG_CLUB_ID: ${arguments?.getInt(ARG_CLUB_ID, -1)}")
        Log.d("LedgerContentFragment", "fetchTransactions - ARG_LEDGER_ID: ${arguments?.getInt(ARG_LEDGER_ID, -1)}")
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
                        updateMonthNavigationButtons() // 월 이동 버튼 상태 업데이트
                    } else {
                        Log.e("LedgerContentFragment", "서버 응답 오류: ${response.code()}")
                        allTransactions = emptyList()
                        updateFinancialSummary()
                        applyFiltersAndSort()
                        updateMonthNavigationButtons() // 월 이동 버튼 상태 업데이트
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

    private fun setupRecyclerViewClickListeners() {
        Log.d("LedgerContentFragment", "setupRecyclerViewClickListeners 호출됨")
        transactionAdapter.setOnItemClickListener { position ->
            Log.d("LedgerContentFragment", "아이템 클릭 이벤트 발생: position=$position")
            
            // 현재 표시된 거래 목록에서 해당 위치의 거래 ID 가져오기
            val currentList = transactionAdapter.getCurrentList()
            Log.d("LedgerContentFragment", "현재 리스트 크기: ${currentList.size}")
            
            if (position < currentList.size) {
                val clickedTransaction = currentList[position]
                Log.d("LedgerContentFragment", "거래 클릭: position=$position, transactionId=${clickedTransaction.id}, amount=${clickedTransaction.amount}")
                
                // 거래 상세 페이지로 이동
                val transactionDetailFragment = TransactionDetailFragment.newInstance(
                    clubId, 
                    ledgerId, 
                    clickedTransaction.id  // position 대신 실제 거래 ID 전달
                )
                Log.d("LedgerContentFragment", "TransactionDetailFragment 생성됨: clubId=$clubId, ledgerId=$ledgerId, transactionId=${clickedTransaction.id}")
                
                if (activity is MainActivity) {
                    Log.d("LedgerContentFragment", "MainActivity로 캐스팅 성공, replaceFragment 호출")
                    (activity as MainActivity).replaceFragment(transactionDetailFragment)
                } else {
                    Log.e("LedgerContentFragment", "activity가 MainActivity가 아님: ${activity?.javaClass?.simpleName}")
                }
            } else {
                Log.e("LedgerContentFragment", "잘못된 position: $position, 리스트 크기: ${currentList.size}")
            }
        }
        Log.d("LedgerContentFragment", "클릭 리스너 설정 완료")
    }
    
    // 이전 달로 이동할 수 있는지 확인하는 함수
    private fun canMoveToPreviousMonth(): Boolean {
        // 현재 달이 너무 과거가 아니면 항상 이동 가능
        // 예: 2020년 1월 이후로는 항상 이동 가능하도록 설정
        val minDate = Calendar.getInstance().apply {
            set(2020, Calendar.JANUARY, 1)
        }
        
        val canMove = calendar.after(minDate)
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
        
        Log.d("LedgerContentFragment", "이전 달 이동 가능 여부: $canMove (현재 월: $currentMonth)")
        return canMove
    }
    
    // 다음 달로 이동할 수 있는지 확인하는 함수
    private fun canMoveToNextMonth(): Boolean {
        // 현재 달이 현재 시점보다 이전이면 이동 가능
        val currentTime = Calendar.getInstance()
        val canMove = calendar.before(currentTime) || calendar.get(Calendar.YEAR) == currentTime.get(Calendar.YEAR) && calendar.get(Calendar.MONTH) == currentTime.get(Calendar.MONTH)
        
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
        val thisMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentTime.time)
        
        Log.d("LedgerContentFragment", "다음 달 이동 가능 여부: $canMove (현재 월: $currentMonth, 오늘: $thisMonth)")
        return canMove
    }
    
    // 월 이동 버튼들의 상태를 업데이트하는 함수
    private fun updateMonthNavigationButtons() {
        val canMovePrev = canMoveToPreviousMonth()
        val canMoveNext = canMoveToNextMonth()
        
        // 이전 달 버튼 상태 설정
        btnPrevMonth.isEnabled = canMovePrev
        btnPrevMonth.alpha = if (canMovePrev) 1.0f else 0.5f
        
        // 다음 달 버튼 상태 설정
        btnNextMonth.isEnabled = canMoveNext
        btnNextMonth.alpha = if (canMoveNext) 1.0f else 0.5f
        
        Log.d("LedgerContentFragment", "월 이동 버튼 상태 업데이트: 이전달=$canMovePrev, 다음달=$canMoveNext")
    }
    
    // 상단 네비게이션 버튼들의 상태를 설정하는 함수
    private fun setupNavigationButtonStates() {
        try {
            val baseActivity = activity as? BaseActivity
            if (baseActivity != null) {
                // BaseActivity에서 상단 네비게이션 버튼들을 찾아서 공개장부 버튼을 선택된 상태로 설정
                val btnPublicAccount = baseActivity.findViewById<TextView>(R.id.btn_public_account)
                val btnNotice = baseActivity.findViewById<TextView>(R.id.btn_notice)
                val btnFreeBoard = baseActivity.findViewById<TextView>(R.id.btn_free_board)
                val btnEventAccount = baseActivity.findViewById<TextView>(R.id.btn_event_account)
                val btnMeetingAccount = baseActivity.findViewById<TextView>(R.id.btn_meeting_account)
                val btnAiReport = baseActivity.findViewById<TextView>(R.id.btn_ai_report)
                
                // 공개장부 버튼을 선택된 상태로 설정하고 나머지는 선택되지 않은 상태로 설정
                if (btnPublicAccount != null) {
                    btnPublicAccount.setBackgroundResource(R.drawable.btn_board_selected)
                    btnPublicAccount.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                }
                
                // 나머지 버튼들을 선택되지 않은 상태로 설정
                listOfNotNull(btnNotice, btnFreeBoard, btnEventAccount, btnMeetingAccount, btnAiReport).forEach { btn ->
                    btn.setBackgroundResource(R.drawable.btn_unselected)
                    btn.setTextColor(android.graphics.Color.parseColor("#333333"))
                }
                
                Log.d("LedgerContentFragment", "상단 네비게이션 버튼 상태 설정 완료")
            } else {
                Log.e("LedgerContentFragment", "BaseActivity를 찾을 수 없습니다")
            }
        } catch (e: Exception) {
            Log.e("LedgerContentFragment", "상단 네비게이션 버튼 상태 설정 중 오류 발생", e)
        }
    }

    private fun fetchClubNameAndSetTitle() {
        if (clubId <= 0) {
            Log.e("LedgerContentFragment", "유효하지 않은 clubId: $clubId")
            (activity as? BaseActivity)?.setAppTitle("장부 상세내역")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = ApiClient.getApiService().getClubDetail(clubId)
                val response = call.execute()
                
                if (response.isSuccessful && response.body() != null) {
                    val club = response.body()!!
                    CoroutineScope(Dispatchers.Main).launch {
                        (activity as? BaseActivity)?.setAppTitle(club.name)
                        Log.d("LedgerContentFragment", "동아리 이름으로 제목 설정 완료: ${club.name}")
                    }
                } else {
                    Log.e("LedgerContentFragment", "동아리 정보 조회 실패: ${response.code()}")
                    CoroutineScope(Dispatchers.Main).launch {
                        (activity as? BaseActivity)?.setAppTitle("장부 상세내역")
                    }
                }
            } catch (e: Exception) {
                Log.e("LedgerContentFragment", "동아리 정보 조회 중 오류 발생", e)
                CoroutineScope(Dispatchers.Main).launch {
                    (activity as? BaseActivity)?.setAppTitle("장부 상세내역")
                }
            }
        }
    }
}
