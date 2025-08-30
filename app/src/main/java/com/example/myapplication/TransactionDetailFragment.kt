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
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.BaseActivity
import com.example.myapplication.LedgerEditActivity
import com.example.myapplication.LedgerItem
import com.example.myapplication.CommentAdapter
import com.example.myapplication.UserManager
import com.example.myapplication.api.ApiService
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.TransactionDetailResponse
import com.example.myapplication.api.ReceiptResponse
import com.example.myapplication.api.Comment
import com.example.myapplication.api.CommentRequest
import com.example.myapplication.TransactionItem
import kotlinx.coroutines.Dispatchers
import com.bumptech.glide.Glide
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

    private lateinit var tvTransactionDatetime: TextView
    private lateinit var tvTransactionAmount: TextView
    private lateinit var tvVendorName: TextView
    private lateinit var tvPaymentMethod: TextView
    private lateinit var tvMemo: TextView
    private lateinit var receiptContainer: View
    private lateinit var receiptDivider: View
    private lateinit var ivReceipt: ImageView
    private lateinit var rvComments: RecyclerView
    private lateinit var etCommentInput: EditText
    private lateinit var btnSendComment: ImageView
    private lateinit var commentAdapter: CommentAdapter
    private var currentUserMemberPk: Int? = null
    // private lateinit var ivHeart: ImageView
    // private lateinit var tvLikeCount: TextView

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
            initializeCurrentUserMemberPk()
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

        tvTransactionDatetime = contentView.findViewById(R.id.tv_transaction_datetime)
        tvTransactionAmount = contentView.findViewById(R.id.tv_transaction_amount)
        tvVendorName = contentView.findViewById(R.id.tv_vendor_name)
        tvPaymentMethod = contentView.findViewById(R.id.tv_payment_method)
        tvMemo = contentView.findViewById(R.id.tv_memo)
        receiptContainer = contentView.findViewById(R.id.receipt_container)
        receiptDivider = contentView.findViewById(R.id.receipt_divider)
        ivReceipt = contentView.findViewById(R.id.iv_receipt)
        rvComments = contentView.findViewById(R.id.rv_comments)
        etCommentInput = contentView.findViewById(R.id.et_comment_input)
        btnSendComment = contentView.findViewById(R.id.btn_send_comment)
//        ivHeart = contentView.findViewById(R.id.iv_heart)
//        tvLikeCount = contentView.findViewById(R.id.tv_like_count)

        setupCommentsRecyclerView()
        

        
        // 초기에는 지출/수입 버튼도 숨기기 (데이터 로드 후 표시)
        btnExpense.visibility = View.GONE
        btnIncome.visibility = View.GONE
        
        // 하트 초기 상태 설정
        // ivHeart.tag = "unliked"
    }

    private fun setupCommentsRecyclerView() {
        commentAdapter = CommentAdapter(emptyList(), currentUserMemberPk) { comment ->
            showDeleteConfirmDialog(comment)
        }
        rvComments.adapter = commentAdapter
        rvComments.layoutManager = LinearLayoutManager(context)
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



        // 댓글 전송 버튼 클릭 이벤트
        btnSendComment.setOnClickListener {
            sendComment()
        }

        // 하트 클릭 이벤트
        // ivHeart.setOnClickListener {
        //     toggleHeartLike()
        // }
    }



    // 하트 좋아요 토글
    private fun toggleHeartLike() {
        // 하트 기능은 현재 사용하지 않음
        // try {
        //     // 현재 하트 상태에 따라 이미지 변경
        //     val currentTag = ivHeart.tag as? String ?: "unliked"
        //     
        //     if (currentTag == "liked") {
        //         // 좋아요 취소
        //         ivHeart.setImageResource(R.drawable.ic_heart_outline)
        //         ivHeart.tag = "unliked"
        //         // 좋아요 수 감소
        //         val currentCount = tvLikeCount.text.toString().toIntOrNull() ?: 0
        //         if (currentCount > 0) {
        //             tvLikeCount.text = (currentCount - 1).toString()
        //         }
        //     } else {
        //         // 좋아요 추가
        //         ivHeart.setImageResource(R.drawable.ic_heart_filled)
        //         ivHeart.tag = "liked"
        //         // 좋아요 수 증가
        //         val currentCount = tvLikeCount.text.toString().toIntOrNull() ?: 0
        //         tvLikeCount.text = (currentCount + 1).toString()
        //     }
        // } catch (e: Exception) {
        //     Log.e("TransactionDetailFragment", "하트 토글 실패", e)
        // }
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
//            btnModify.visibility = View.GONE
//            btnDelete.visibility = View.GONE
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
                loadComments(transactionId)
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
        val displayDateFormat = SimpleDateFormat("MM월 dd일", Locale.getDefault())
        val fullDisplayDateFormat = SimpleDateFormat("yyyy년 MM월 dd일 E요일 HH:mm", Locale.KOREA)

        // API에서 오는 날짜 형식을 로그로 확인
        Log.d("TransactionDetailFragment", "API 날짜 형식: '${detail.date_time}'")
        
        val date = parseDateWithMultipleFormats(detail.date_time)
        if (date != null) {
            tvCurrentDate.text = displayDateFormat.format(date)
            tvTransactionDatetime.text = fullDisplayDateFormat.format(date)
            Log.d("TransactionDetailFragment", "날짜 파싱 성공: ${date}")
        } else {
            tvCurrentDate.text = "날짜 오류"
            tvTransactionDatetime.text = "날짜 오류"
            Log.e("TransactionDetailFragment", "모든 날짜 형식으로 파싱 실패: '${detail.date_time}'")
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
        if (!detail.type.isNullOrEmpty()) {
            btnType.visibility = View.VISIBLE
        btnType.text = detail.type
        } else {
            btnType.visibility = View.GONE
        }

        // 영수증 이미지 처리
        val receiptId = detail.receipt?.toIntOrNull()
        if (receiptId != null && receiptId > 0) {
            receiptContainer.visibility = View.VISIBLE
            receiptDivider.visibility = View.VISIBLE
            
            // 영수증 ID가 있으면 API를 호출하여 이미지 URL 가져오기
            loadReceiptImage(receiptId)
        } else {
            receiptContainer.visibility = View.GONE
            receiptDivider.visibility = View.GONE
        }

        // 거래 상세 정보
        tvVendorName.text = "거래처명: ${detail.vendor ?: ""}"
        tvPaymentMethod.text = "결제수단: ${detail.payment_method ?: ""}"
        tvMemo.text = "메모 : ${detail.description ?: ""}"
        

    }

    // 여러 날짜 형식을 시도하여 파싱
    private fun parseDateWithMultipleFormats(dateString: String): java.util.Date? {
        val dateFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",      // 2023-02-18T14:22:18+09:00
            "yyyy-MM-dd'T'HH:mm:ss'Z'",      // 2023-02-18T14:22:18Z
            "yyyy-MM-dd'T'HH:mm:ss",         // 2023-02-18T14:22:18
            "yyyy-MM-dd HH:mm:ss",           // 2023-02-18 14:22:18
            "yyyy-MM-dd",                    // 2023-02-18
            "dd/MM/yyyy",                    // 18/02/2023
            "MM/dd/yyyy",                    // 02/18/2023
            "yyyy/MM/dd"                     // 2023/02/18
        )
        
        for (format in dateFormats) {
            try {
                val dateFormat = SimpleDateFormat(format, Locale.getDefault())
                dateFormat.isLenient = false
                val date = dateFormat.parse(dateString)
                if (date != null) {
                    Log.d("TransactionDetailFragment", "날짜 파싱 성공: '$dateString' -> $format -> $date")
                    return date
                }
            } catch (e: Exception) {
                Log.d("TransactionDetailFragment", "날짜 형식 '$format'로 파싱 실패: '$dateString' - ${e.message}")
            }
        }
        
        Log.e("TransactionDetailFragment", "모든 날짜 형식으로 파싱 실패: '$dateString'")
        return null
    }

    // EXIF 회전 정보를 고려하여 이미지 회전
    private fun rotateImageIfNeeded(bitmap: android.graphics.Bitmap, imageUrl: String): android.graphics.Bitmap {
        try {
            // URL에서 이미지 다운로드하여 EXIF 정보 읽기
            val url = java.net.URL(imageUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            if (url.protocol == "https") {
                setupTrustAllCertificates()
            }
            
            val responseCode = connection.responseCode
            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                
                // EXIF 정보 읽기
                val exif = android.media.ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )
                
                inputStream.close()
                connection.disconnect()
                
                // 회전 각도 계산
                val rotationAngle = when (orientation) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                
                if (rotationAngle != 0f) {
                    Log.d("TransactionDetailFragment", "이미지 회전 적용: ${rotationAngle}도")
                    
                    // 이미지 회전
                    val matrix = android.graphics.Matrix()
                    matrix.postRotate(rotationAngle)
                    
                    val rotatedBitmap = android.graphics.Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                    )
                    
                    // 원본 비트맵이 회전된 비트맵과 다르면 원본 해제
                    if (rotatedBitmap != bitmap) {
                        bitmap.recycle()
                    }
                    
                    return rotatedBitmap
                }
            }
            
            connection.disconnect()
        } catch (e: Exception) {
            Log.e("TransactionDetailFragment", "EXIF 회전 정보 읽기 실패", e)
        }
        
        // 회전이 필요하지 않거나 실패한 경우 원본 반환
        return bitmap
    }

    // 영수증 이미지 로드
    private fun loadReceiptImage(receiptId: Int) {
        lifecycleScope.launch {
            try {
                Log.d("TransactionDetailFragment", "영수증 이미지 로드 시작: receiptId=$receiptId")
                val receiptResponse = apiService.getReceiptDetail(clubId, ledgerId, receiptId)
                
                if (!receiptResponse.image.isNullOrEmpty()) {
                    val imageUrl = receiptResponse.image
                    Log.d("TransactionDetailFragment", "영수증 이미지 URL: '$imageUrl'")
                    
                    // 백그라운드에서 HTTP 연결로 이미지 로드
                    withContext(Dispatchers.IO) {
                        try {
                            loadImageWithHttpUrlConnection(imageUrl)
                        } catch (e: Exception) {
                            Log.e("TransactionDetailFragment", "HTTP 이미지 로드 실패", e)
                            withContext(Dispatchers.Main) {
                                ivReceipt.setImageResource(android.R.drawable.ic_menu_gallery)
                            }
                        }
                    }
                } else {
                    Log.w("TransactionDetailFragment", "영수증 이미지 URL이 비어있음")
                    withContext(Dispatchers.Main) {
                        ivReceipt.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                }
            } catch (e: Exception) {
                Log.e("TransactionDetailFragment", "영수증 API 호출 실패", e)
                withContext(Dispatchers.Main) {
                    ivReceipt.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }
        }
    }

    // HTTP 연결을 직접 사용한 이미지 로드 (EXIF 회전 정보 처리 포함)
    private suspend fun loadImageWithHttpUrlConnection(imageUrl: String) {
        Log.d("TransactionDetailFragment", "HTTP 이미지 로드 시작: $imageUrl")
        
        withContext(Dispatchers.IO) {
            try {
                val url = java.net.URL(imageUrl)
                
                // HTTPS URL인 경우 SSL 검증 우회
                if (url.protocol == "https") {
                    Log.d("TransactionDetailFragment", "HTTPS URL 감지, SSL 검증 우회 설정")
                    setupTrustAllCertificates()
                }
                
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000 // 15초 타임아웃
                connection.readTimeout = 15000 // 15초 타임아웃
                connection.instanceFollowRedirects = true
                
                // User-Agent 설정으로 일부 서버 차단 방지
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                
                val responseCode = connection.responseCode
                Log.d("TransactionDetailFragment", "HTTP 응답 코드: $responseCode")

                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    
                    if (bitmap != null) {
                        // EXIF 회전 정보를 고려하여 이미지 회전
                        val rotatedBitmap = rotateImageIfNeeded(bitmap, imageUrl)
                        
                        // UI 스레드에서 이미지 설정
                        withContext(Dispatchers.Main) {
                            ivReceipt.setImageBitmap(rotatedBitmap)
                            Log.d("TransactionDetailFragment", "HTTP 이미지 로드 성공")
                        }
                    } else {
                        Log.e("TransactionDetailFragment", "비트맵 디코딩 실패")
                        withContext(Dispatchers.Main) {
                            ivReceipt.setImageResource(android.R.drawable.ic_menu_gallery)
                        }
                    }
                } else if (responseCode == java.net.HttpURLConnection.HTTP_MOVED_PERM || 
                           responseCode == java.net.HttpURLConnection.HTTP_MOVED_TEMP) {
                    // 리다이렉트 처리
                    val newLocation = connection.getHeaderField("Location")
                    Log.d("TransactionDetailFragment", "리다이렉트 감지: $newLocation")
                    
                    if (!newLocation.isNullOrEmpty()) {
                        // 새로운 URL로 재시도
                        loadImageWithHttpUrlConnection(newLocation)
                    } else {
                        Log.e("TransactionDetailFragment", "리다이렉트 URL이 비어있음")
                        withContext(Dispatchers.Main) {
                            ivReceipt.setImageResource(android.R.drawable.ic_menu_gallery)
                        }
                    }
                } else {
                    Log.e("TransactionDetailFragment", "HTTP 이미지 로드 실패, 응답 코드: $responseCode")
                    withContext(Dispatchers.Main) {
                        ivReceipt.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("TransactionDetailFragment", "HTTP 이미지 로드 실패", e)
                withContext(Dispatchers.Main) {
                    ivReceipt.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }
        }
    }

    // 모든 SSL 인증서를 신뢰하도록 설정
    private fun setupTrustAllCertificates() {
        try {
            // 모든 인증서를 신뢰하는 TrustManager 생성
            val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(object : javax.net.ssl.X509TrustManager {
                override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })

            // SSL 컨텍스트 설정
            val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            
            // HttpsURLConnection의 기본 SSL 소켓 팩토리 설정
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            
            // 호스트명 검증 비활성화
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
            
            Log.d("TransactionDetailFragment", "SSL 검증 우회 설정 완료")
        } catch (e: Exception) {
            Log.e("TransactionDetailFragment", "SSL 검증 우회 설정 실패", e)
        }
    }

    private fun loadComments(transactionId: Int) {
        Log.d("TransactionDetailFragment", "댓글 로드 시작: clubId=$clubId, ledgerId=$ledgerId, transactionId=$transactionId")
        apiService.getTransactionComments(clubId, ledgerId, transactionId).enqueue(object : retrofit2.Callback<List<Comment>> {
            override fun onResponse(
                call: retrofit2.Call<List<Comment>>,
                response: retrofit2.Response<List<Comment>>
            ) {
                if (response.isSuccessful) {
                    val comments = response.body() ?: emptyList()
                    commentAdapter.updateComments(comments)
                    Log.d("TransactionDetailFragment", "댓글 로드 성공: ${comments.size}개")
                    if (comments.isNotEmpty()) {
                        Log.d("TransactionDetailFragment", "첫 번째 댓글: ${comments[0].content} by ${comments[0].author_name}")
                    }
                } else {
                    Log.e("TransactionDetailFragment", "댓글 로드 실패: ${response.code()}")
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e("TransactionDetailFragment", "댓글 로드 에러 응답: $errorBody")
                    } catch (ex: Exception) {
                        Log.e("TransactionDetailFragment", "에러 응답 읽기 실패", ex)
                    }
                }
            }

            override fun onFailure(call: retrofit2.Call<List<Comment>>, t: Throwable) {
                Log.e("TransactionDetailFragment", "댓글 로드 네트워크 실패", t)
            }
        })
    }

    private fun sendComment() {
        val content = etCommentInput.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(context, "댓글을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = UserManager.getUserPk(requireContext())
        if (currentUserId == null) {
            Toast.makeText(context, "로그인이 필요합니다", Toast.LENGTH_SHORT).show()
            return
        }

        if (allTransactions.isEmpty()) {
            Toast.makeText(context, "거래 정보를 불러오는 중입니다", Toast.LENGTH_SHORT).show()
            return
        }

        val currentTransactionId = allTransactions[currentTransactionIndex].id

        fetchMyClubMemberPk(clubId, currentUserId) { memberPk ->
            if (memberPk == null) {
                Toast.makeText(context, "동아리 멤버 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                return@fetchMyClubMemberPk
            }

            val request = CommentRequest(content, memberPk)
            Log.d("TransactionDetailFragment", "댓글 작성 요청: clubId=$clubId, ledgerId=$ledgerId, transactionId=$currentTransactionId, memberPk=$memberPk, content='$content'")

            apiService.postTransactionComment(clubId, ledgerId, currentTransactionId, request).enqueue(object : retrofit2.Callback<Comment> {
                override fun onResponse(
                    call: retrofit2.Call<Comment>,
                    response: retrofit2.Response<Comment>
                ) {
                    if (response.isSuccessful) {
                        val newComment = response.body()
                        etCommentInput.text.clear()
                        loadComments(currentTransactionId)
                        Toast.makeText(context, "댓글이 등록되었습니다", Toast.LENGTH_SHORT).show()
                        Log.d("TransactionDetailFragment", "댓글 작성 성공: ${newComment?.content}")
                    } else {
                        Log.e("TransactionDetailFragment", "댓글 작성 실패: ${response.code()}")
                        try {
                            val errorBody = response.errorBody()?.string()
                            Log.e("TransactionDetailFragment", "에러 응답: $errorBody")
                        } catch (ex: Exception) {
                            Log.e("TransactionDetailFragment", "에러 응답 읽기 실패", ex)
                        }
                        Toast.makeText(context, "댓글 작성에 실패했습니다", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: retrofit2.Call<Comment>, t: Throwable) {
                    Log.e("TransactionDetailFragment", "댓글 작성 실패", t)
                    Toast.makeText(context, "댓글 작성에 실패했습니다", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun fetchMyClubMemberPk(clubPk: Int, userId: Int, cb: (Int?) -> Unit) {
        apiService.getClubMembers(clubPk).enqueue(object : retrofit2.Callback<List<com.example.myapplication.MemberResponse>> {
            override fun onResponse(
                call: retrofit2.Call<List<com.example.myapplication.MemberResponse>>,
                response: retrofit2.Response<List<com.example.myapplication.MemberResponse>>
            ) {
                if (response.isSuccessful) {
                    val members = response.body() ?: emptyList()
                    val memberPk = members.firstOrNull { it.user == userId }?.id
                    cb(memberPk)
                } else {
                    Log.e("TransactionDetailFragment", "ClubMember 조회 실패: ${response.code()}")
                    cb(null)
                }
            }

            override fun onFailure(call: retrofit2.Call<List<com.example.myapplication.MemberResponse>>, t: Throwable) {
                Log.e("TransactionDetailFragment", "ClubMember PK 조회 실패", t)
                cb(null)
            }
        })
    }

    private fun initializeCurrentUserMemberPk() {
        val currentUserId = UserManager.getUserPk(requireContext())
        if (currentUserId != null) {
            fetchMyClubMemberPk(clubId, currentUserId) { memberPk ->
                currentUserMemberPk = memberPk
                commentAdapter = CommentAdapter(emptyList(), currentUserMemberPk) { comment ->
                    showDeleteConfirmDialog(comment)
                }
                rvComments.adapter = commentAdapter
            }
        }
    }

    private fun showDeleteConfirmDialog(comment: Comment) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("댓글 삭제")
        builder.setMessage("이 댓글을 삭제하시겠습니까?")
        builder.setPositiveButton("삭제") { _, _ ->
            deleteComment(comment)
        }
        builder.setNegativeButton("취소", null)
        builder.show()
    }

    private fun deleteComment(comment: Comment) {
        if (allTransactions.isEmpty()) {
            Toast.makeText(context, "거래 정보를 불러오는 중입니다", Toast.LENGTH_SHORT).show()
            return
        }

        val currentTransactionId = allTransactions[currentTransactionIndex].id
        Log.d("TransactionDetailFragment", "댓글 삭제 요청: clubId=$clubId, ledgerId=$ledgerId, transactionId=$currentTransactionId, commentId=${comment.id}")

        apiService.deleteTransactionComment(clubId, ledgerId, currentTransactionId, comment.id).enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {
            override fun onResponse(
                call: retrofit2.Call<okhttp3.ResponseBody>,
                response: retrofit2.Response<okhttp3.ResponseBody>
            ) {
                if (response.isSuccessful) {
                    loadComments(currentTransactionId)
                    Toast.makeText(context, "댓글이 삭제되었습니다", Toast.LENGTH_SHORT).show()
                    Log.d("TransactionDetailFragment", "댓글 삭제 성공: commentId=${comment.id}")
                } else {
                    Log.e("TransactionDetailFragment", "댓글 삭제 실패: ${response.code()}")
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e("TransactionDetailFragment", "댓글 삭제 에러 응답: $errorBody")
                    } catch (ex: Exception) {
                        Log.e("TransactionDetailFragment", "에러 응답 읽기 실패", ex)
                    }
                    Toast.makeText(context, "댓글 삭제에 실패했습니다", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                Log.e("TransactionDetailFragment", "댓글 삭제 네트워크 실패", t)
                Toast.makeText(context, "댓글 삭제에 실패했습니다", Toast.LENGTH_SHORT).show()
            }
        })
    }


}
