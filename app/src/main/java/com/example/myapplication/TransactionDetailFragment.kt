package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.BaseActivity
import com.example.myapplication.LedgerEditActivity
import com.example.myapplication.LedgerItem
import com.example.myapplication.api.ApiService
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.TransactionDetailResponse
import com.example.myapplication.TransactionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionDetailFragment : Fragment() {

    private lateinit var contentView: View
    private lateinit var tvCurrentDate: TextView
    private lateinit var btnPrevItem: ImageView
    private lateinit var btnNextItem: ImageView
    private lateinit var btnExpense: TextView
    private lateinit var btnIncome: TextView
    private lateinit var btnType: TextView
    private lateinit var btnModify: TextView
    private lateinit var btnDelete: TextView
    private lateinit var tvTransactionDatetime: TextView
    private lateinit var tvTransactionAmount: TextView
    private lateinit var tvVendorName: TextView
    private lateinit var tvPaymentMethod: TextView
    private lateinit var tvMemo: TextView
    private lateinit var receiptContainer: View
    private lateinit var receiptDivider: View
    private lateinit var ivReceipt: ImageView
    private lateinit var ivHeart: ImageView
    private lateinit var tvLikeCount: TextView

    private var currentTransactionIndex: Int = 0
    private var allTransactions: List<TransactionItem> = listOf() // 전체 거래 목록
    private var clubId: Int = 4  // 하드코딩
    private var ledgerId: Int = 10  // 하드코딩
    private var targetTransactionId: Int = -1  // 클릭한 거래의 ID

    private val apiService: ApiService by lazy {
        ApiClient.getApiService()
    }

    companion object {
        private const val ARG_CLUB_ID = "club_id"
        private const val ARG_LEDGER_ID = "ledger_id"
        private const val ARG_TRANSACTION_ID = "transaction_id"

        fun newInstance(clubId: Int, ledgerId: Int, transactionId: Int): TransactionDetailFragment {
            val fragment = TransactionDetailFragment()
            val args = Bundle()
            args.putInt(ARG_CLUB_ID, clubId)
            args.putInt(ARG_LEDGER_ID, ledgerId)
            args.putInt(ARG_TRANSACTION_ID, transactionId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("TransactionDetailFragment", "onCreate 호출됨")
        arguments?.let {
            clubId = it.getInt(ARG_CLUB_ID)
            ledgerId = it.getInt(ARG_LEDGER_ID)
            targetTransactionId = it.getInt(ARG_TRANSACTION_ID)
            Log.d("TransactionDetailFragment", "onCreate: clubId=$clubId, ledgerId=$ledgerId, targetTransactionId=$targetTransactionId")
        } ?: run {
            Log.e("TransactionDetailFragment", "arguments가 null입니다!")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        contentView = inflater.inflate(R.layout.transaction_detail, container, false)
        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            (activity as? BaseActivity)?.setAppTitle("거래 상세내역")
            (activity as? BaseActivity)?.hideBoardButtons()
            (activity as? BaseActivity)?.showBackButton() // 뒤로가기 버튼 표시

            initializeViews()
            setupClickListeners()
            loadAllTransactionsAndDisplayCurrent() // 전체 목록 로드 후 현재 거래 표시

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initializeViews() {
        tvCurrentDate = contentView.findViewById(R.id.tv_current_date)
        btnPrevItem = contentView.findViewById(R.id.btn_prev_item)
        btnNextItem = contentView.findViewById(R.id.btn_next_item)
        btnExpense = contentView.findViewById(R.id.btn_expense)
        btnIncome = contentView.findViewById(R.id.btn_income)
        btnType = contentView.findViewById(R.id.btn_type)
        btnModify = contentView.findViewById(R.id.btn_modify)
        btnDelete = contentView.findViewById(R.id.btn_delete)
        tvTransactionDatetime = contentView.findViewById(R.id.tv_transaction_datetime)
        tvTransactionAmount = contentView.findViewById(R.id.tv_transaction_amount)
        tvVendorName = contentView.findViewById(R.id.tv_vendor_name)
        tvPaymentMethod = contentView.findViewById(R.id.tv_payment_method)
        tvMemo = contentView.findViewById(R.id.tv_memo)
        receiptContainer = contentView.findViewById(R.id.receipt_container)
        receiptDivider = contentView.findViewById(R.id.receipt_divider)
        ivReceipt = contentView.findViewById(R.id.iv_receipt)
//        ivHeart = contentView.findViewById(R.id.iv_heart)
//        tvLikeCount = contentView.findViewById(R.id.tv_like_count)
        
        // 초기에는 수정하기/삭제하기 버튼 숨기기 (데이터 로드 후 표시)
        btnModify.visibility = View.GONE
        btnDelete.visibility = View.GONE
        
        // 하트 초기 상태 설정
        ivHeart.tag = "unliked"
    }

    private fun setupClickListeners() {
        // 이전/다음 항목 버튼
        btnPrevItem.setOnClickListener {
            if (currentTransactionIndex > 0) {
                currentTransactionIndex--
                displayCurrentTransactionDetail() // 현재 인덱스에 해당하는 거래 상세 표시
            }
        }

        btnNextItem.setOnClickListener {
            if (currentTransactionIndex < allTransactions.size - 1) {
                currentTransactionIndex++
                displayCurrentTransactionDetail() // 현재 인덱스에 해당하는 거래 상세 표시
            }
        }

        // 거래 타입 버튼들은 단순 표시용 (클릭 이벤트 없음)

        // 액션 버튼들
        btnModify.setOnClickListener {
            // 거래 수정 화면으로 이동
            navigateToEditPage()
        }

        btnDelete.setOnClickListener {
            // TODO: 거래 삭제 확인 다이얼로그 표시
        }

        // 하트 클릭 이벤트
        ivHeart.setOnClickListener {
            toggleHeartLike()
        }
    }

    // 거래 수정 페이지로 이동
    private fun navigateToEditPage() {
        try {
            Log.d("TransactionDetailFragment", "navigateToEditPage 시작")
            
            // activity null 체크
            val currentActivity = activity
            if (currentActivity == null) {
                Log.e("TransactionDetailFragment", "activity가 null입니다!")
                return
            }
            
            // allTransactions가 비어있는지 체크
            if (allTransactions.isEmpty()) {
                Log.e("TransactionDetailFragment", "allTransactions가 비어있습니다!")
                return
            }
            
            // currentTransactionIndex 유효성 체크
            if (currentTransactionIndex < 0 || currentTransactionIndex >= allTransactions.size) {
                Log.e("TransactionDetailFragment", "currentTransactionIndex가 유효하지 않습니다: $currentTransactionIndex, 크기: ${allTransactions.size}")
                return
            }
            
            // 현재 거래 정보를 LedgerItem 형태로 변환
            val currentTransaction = allTransactions[currentTransactionIndex]
            Log.d("TransactionDetailFragment", "현재 거래: $currentTransaction")
            
            // UI에서 데이터를 안전하게 가져오기 (현재 표시된 데이터 그대로)
            val vendorName = tvVendorName.text?.toString()?.replace("거래처명: ", "") ?: ""
            val paymentMethod = tvPaymentMethod.text?.toString()?.replace("결제수단: ", "") ?: ""
            val originalAmount = currentTransaction.amount
            val originalType = btnType.text?.toString() ?: "기타"
            val originalDate = tvTransactionDatetime.text?.toString() ?: ""
            val originalMemo = tvMemo.text?.toString()?.replace("메모 : ", "") ?: ""
            
            // 디버깅을 위한 로그 추가
            Log.d("TransactionDetailFragment", "=== 수정 페이지로 전달할 데이터 ===")
            Log.d("TransactionDetailFragment", "거래처명: '$vendorName'")
            Log.d("TransactionDetailFragment", "결제수단: '$paymentMethod'")
            Log.d("TransactionDetailFragment", "원본 금액: $originalAmount")
            Log.d("TransactionDetailFragment", "원본 타입: '$originalType'")
            Log.d("TransactionDetailFragment", "원본 날짜: '$originalDate'")
            Log.d("TransactionDetailFragment", "원본 메모: '$originalMemo'")
            Log.d("TransactionDetailFragment", "UI에서 가져온 금액 텍스트: '${tvTransactionAmount.text}'")
            Log.d("TransactionDetailFragment", "UI에서 가져온 타입 텍스트: '${btnType.text}'")
            Log.d("TransactionDetailFragment", "UI에서 가져온 날짜 텍스트: '${tvTransactionDatetime.text}'")
            Log.d("TransactionDetailFragment", "UI에서 가져온 메모 텍스트: '${tvMemo.text}'")
            
            // LedgerItem 생성 (API 응답 데이터를 기반으로)
            val ledgerItem = LedgerItem(
                type = originalType,
                tags = listOf(), // 빈 태그 리스트
                date = originalDate,
                amount = tvTransactionAmount.text?.toString() ?: "0",
                author = "사용자", // 기본값
                memo = originalMemo,
                hasReceipt = receiptContainer.visibility == View.VISIBLE
            )
            
            Log.d("TransactionDetailFragment", "생성된 LedgerItem: $ledgerItem")
            
            // LedgerEditActivity로 이동 (모든 정보 포함)
            val intent = Intent(currentActivity, LedgerEditActivity::class.java)
            intent.putExtra("LEDGER_ITEM", ledgerItem)
            intent.putExtra("VENDOR_NAME", vendorName)
            intent.putExtra("PAYMENT_METHOD", paymentMethod)
            intent.putExtra("ORIGINAL_AMOUNT", originalAmount)
            intent.putExtra("ORIGINAL_TYPE", originalType)
            intent.putExtra("ORIGINAL_DATE", originalDate)
            intent.putExtra("ORIGINAL_MEMO", originalMemo)
            
            Log.d("TransactionDetailFragment", "Intent 생성 완료, LedgerEditActivity 시작")
            Log.d("TransactionDetailFragment", "Intent extras: ${intent.extras}")
            
            startActivity(intent)
            Log.d("TransactionDetailFragment", "startActivity 호출 완료")
            
        } catch (e: Exception) {
            Log.e("TransactionDetailFragment", "수정 페이지 이동 실패", e)
            e.printStackTrace()
            // 에러 처리 (예: 토스트 메시지)
            Toast.makeText(activity, "수정 페이지 이동 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 하트 좋아요 토글
    private fun toggleHeartLike() {
        try {
            // 현재 하트 상태에 따라 이미지 변경
            val currentTag = ivHeart.tag as? String ?: "unliked"
            
            if (currentTag == "liked") {
                // 좋아요 취소
                ivHeart.setImageResource(R.drawable.ic_heart_outline)
                ivHeart.tag = "unliked"
                // 좋아요 수 감소
                val currentCount = tvLikeCount.text.toString().toIntOrNull() ?: 0
                if (currentCount > 0) {
                    tvLikeCount.text = (currentCount - 1).toString()
                }
            } else {
                // 좋아요 추가
                ivHeart.setImageResource(R.drawable.ic_heart_filled)
                ivHeart.tag = "liked"
                // 좋아요 수 증가
                val currentCount = tvLikeCount.text.toString().toIntOrNull() ?: 0
                tvLikeCount.text = (currentCount + 1).toString()
            }
        } catch (e: Exception) {
            Log.e("TransactionDetailFragment", "하트 토글 실패", e)
        }
    }

    private fun loadAllTransactionsAndDisplayCurrent() {
        Log.d("TransactionDetailFragment", "loadAllTransactionsAndDisplayCurrent: clubId=$clubId, ledgerId=$ledgerId, initialIndex=$currentTransactionIndex")
        lifecycleScope.launch {
            try {
                // 1. 해당 장부의 모든 거래 목록을 가져옴
                Log.d("TransactionDetailFragment", "API 호출: getTransactionsForLedger($clubId, $ledgerId)")
                allTransactions = apiService.getTransactionsForLedger(clubId, ledgerId)
                Log.d("TransactionDetailFragment", "거래 목록 조회 성공: ${allTransactions.size}개, targetTransactionId: $targetTransactionId")

                // 2. targetTransactionId에 해당하는 거래의 인덱스 찾기
                currentTransactionIndex = allTransactions.indexOfFirst { it.id == targetTransactionId }
                if (currentTransactionIndex == -1) {
                    Log.w("TransactionDetailFragment", "targetTransactionId($targetTransactionId)에 해당하는 거래를 찾을 수 없음")
                    currentTransactionIndex = 0 // 첫 번째 거래로 설정
                } else {
                    Log.d("TransactionDetailFragment", "targetTransactionId($targetTransactionId)에 해당하는 거래를 찾음: index=$currentTransactionIndex")
                }

                // 3. 현재 인덱스에 해당하는 거래 상세 정보를 표시
                displayCurrentTransactionDetail()
            } catch (e: Exception) {
                Log.e("TransactionDetailFragment", "거래 목록 조회 실패", e)
                e.printStackTrace()
                // 에러 처리 (예: 토스트 메시지 표시)
            }
        }
    }

    private fun displayCurrentTransactionDetail() {
        if (allTransactions.isEmpty()) {
            // 거래가 없을 경우 처리
            tvCurrentDate.text = ""
            tvTransactionDatetime.text = ""
            tvTransactionAmount.text = ""
            btnExpense.visibility = View.GONE
            btnIncome.visibility = View.GONE
            btnType.text = ""
            tvVendorName.text = "거래처명: "
            tvPaymentMethod.text = "결제수단: "
            tvMemo.text = "메모 : "
            receiptContainer.visibility = View.GONE
            receiptDivider.visibility = View.GONE
            btnPrevItem.isEnabled = false
            btnNextItem.isEnabled = false
            // 수정하기/삭제하기 버튼 숨기기
            btnModify.visibility = View.GONE
            btnDelete.visibility = View.GONE
            return
        }

        // 현재 인덱스 유효성 검사
        Log.d("TransactionDetailFragment", "인덱스 검사: currentTransactionIndex=$currentTransactionIndex, allTransactions.size=${allTransactions.size}")
        if (currentTransactionIndex < 0 || currentTransactionIndex >= allTransactions.size) {
            Log.w("TransactionDetailFragment", "인덱스가 유효하지 않음: $currentTransactionIndex, 크기: ${allTransactions.size}")
            currentTransactionIndex = 0 // 유효하지 않으면 첫 번째 항목으로
        }
        
        // allTransactions가 비어있지 않은지 한 번 더 확인
        if (allTransactions.isEmpty()) {
            Log.w("TransactionDetailFragment", "allTransactions가 비어있습니다")
            return
        }

        val currentTransactionSummary = allTransactions[currentTransactionIndex]
        val transactionId = currentTransactionSummary.id
        Log.d("TransactionDetailFragment", "거래 상세 조회: index=$currentTransactionIndex, transactionId=$transactionId, amount=${currentTransactionSummary.amount}, type=${currentTransactionSummary.type}")

        lifecycleScope.launch {
            try {
                // 3. 현재 거래의 상세 정보를 가져옴
                Log.d("TransactionDetailFragment", "API 호출: getTransactionDetail($clubId, $ledgerId, $transactionId)")
                val detailResponse = apiService.getTransactionDetail(clubId, ledgerId, transactionId)
                Log.d("TransactionDetailFragment", "거래 상세 조회 성공: amount=${detailResponse.amount}, type=${detailResponse.type}, vendor=${detailResponse.vendor}")
                updateUIWithDetail(detailResponse)
            } catch (e: Exception) {
                Log.e("TransactionDetailFragment", "거래 상세 조회 실패", e)
                e.printStackTrace()
                // 에러 처리
            }
        }

        // 이전/다음 버튼 활성화/비활성화
        btnPrevItem.isEnabled = currentTransactionIndex > 0
        btnNextItem.isEnabled = currentTransactionIndex < allTransactions.size - 1
    }

    private fun updateUIWithDetail(detail: TransactionDetailResponse) {
        // 날짜 포맷 변경 (예: 2023-02-18T14:22:18+09:00 -> 02월 18일)
        val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("MM월 dd일", Locale.getDefault())
        val fullDisplayDateFormat = SimpleDateFormat("yyyy년 MM월 dd일 E요일 HH:mm", Locale.KOREA)

        try {
            val date = apiDateFormat.parse(detail.date_time)
            tvCurrentDate.text = date?.let { displayDateFormat.format(it) } ?: ""
            tvTransactionDatetime.text = date?.let { fullDisplayDateFormat.format(it) } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            tvCurrentDate.text = "날짜 오류"
            tvTransactionDatetime.text = "날짜 오류"
        }

        // 금액 표시 (세자리씩 쉼표 표시)
        if (detail.amount < 0) {
            btnExpense.visibility = View.VISIBLE
            btnIncome.visibility = View.GONE
            // API에서 이미 음수로 오므로 절댓값으로 포맷팅
            val formattedAmount = NumberFormat.getNumberInstance(Locale.getDefault()).format(kotlin.math.abs(detail.amount))
            tvTransactionAmount.text = "-${formattedAmount}원"  // 음수는 - 추가하고 쉼표 표시
            tvTransactionAmount.setTextColor(Color.parseColor("#C50000")) // 지출 색상
        } else {
            btnExpense.visibility = View.GONE
            btnIncome.visibility = View.VISIBLE
            val formattedAmount = NumberFormat.getNumberInstance(Locale.getDefault()).format(detail.amount)
            tvTransactionAmount.text = "+${formattedAmount}원"  // 양수는 + 추가하고 쉼표 표시
            tvTransactionAmount.setTextColor(Color.parseColor("#2457C5")) // 수입 색상
        }

        // 카테고리 타입
        btnType.text = detail.type

        // 영수증 이미지 처리
        if (!detail.receipt.isNullOrEmpty()) {
            receiptContainer.visibility = View.VISIBLE
            receiptDivider.visibility = View.VISIBLE
            
            // Glide를 사용하여 이미지 로드 (Glide 의존성 필요)
            try {
                // Glide.with(this).load(detail.receipt).into(ivReceipt)
                // 임시로 배경색 변경으로 표시
                ivReceipt.setBackgroundColor(Color.parseColor("#F0F0F0"))
                ivReceipt.setImageResource(android.R.drawable.ic_menu_gallery)
            } catch (e: Exception) {
                Log.e("TransactionDetailFragment", "영수증 이미지 로드 실패", e)
            }
        } else {
            receiptContainer.visibility = View.GONE
            receiptDivider.visibility = View.GONE
        }

        // 거래 상세 정보
        tvVendorName.text = "거래처명: ${detail.vendor ?: ""}"
        tvPaymentMethod.text = "결제수단: ${detail.payment_method ?: ""}"
        tvMemo.text = "메모 : ${detail.description ?: ""}"
        
        // 수정하기/삭제하기 버튼 가시성 설정
        btnModify.visibility = View.VISIBLE
        btnDelete.visibility = View.VISIBLE
    }
}
