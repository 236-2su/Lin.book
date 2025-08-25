package com.example.myapplication

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Button
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.content.Intent
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.provider.MediaStore
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class LedgerCreateActivity : BaseActivity() {

    private lateinit var etTransactionDateTime: EditText
    private lateinit var etAmount: EditText
    private lateinit var contentView: android.view.View
    private val calendar: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
    private var isReceiptCaptured = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // BaseActivity의 기본 설정이 완료된 후 게시판 버튼들을 숨김
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            hideBoardButtons()
        }, 100)
    }
    
    override fun setupContent(savedInstanceState: Bundle?) {
        val contentContainer = findViewById<android.widget.FrameLayout>(R.id.content_container)
        contentContainer.removeAllViews()
        
        contentView = layoutInflater.inflate(R.layout.activity_ledger_create, contentContainer, false)
        contentContainer.addView(contentView)
        
        setAppTitle("공개장부")
        
        setupViews(contentView)
        setupDateTimePicker()
    }
    
    private fun setupViews(contentView: android.view.View) {
        etTransactionDateTime = contentView.findViewById(R.id.et_transaction_datetime)
        etAmount = contentView.findViewById(R.id.et_amount)
        
        // 금액 입력창에 쉼표 자동 포맷팅 적용
        setupAmountFormatting()
        
        // 영수증 등록 입력창 설정
        setupReceiptInput(contentView)
        
        // 스피너들 설정
        setupSpinnersFromView(contentView)
        
        // 등록하기 버튼 설정
        setupRegisterButton(contentView)
    }
    
    // 영수증 등록 입력창 설정
    private fun setupReceiptInput(contentView: android.view.View) {
        val etReceipt = contentView.findViewById<EditText>(R.id.et_receipt)
        
        // 기존 이벤트 리스너 제거
        etReceipt?.setOnClickListener(null)
        etReceipt?.setOnTouchListener(null)
        etReceipt?.setOnLongClickListener(null)
        
        // 촬영 완료 상태가 아닐 때만 카메라 실행
        etReceipt?.setOnClickListener {
            android.util.Log.d("LedgerCreate", "영수증 클릭 이벤트: isReceiptCaptured = $isReceiptCaptured")
            if (!isReceiptCaptured && checkCameraPermission()) {
                android.util.Log.d("LedgerCreate", "카메라 실행")
                openReceiptCamera()
            } else {
                android.util.Log.d("LedgerCreate", "카메라 실행 차단됨")
            }
        }
        
        // 키보드가 뜨지 않도록 설정
        etReceipt?.isFocusable = false
        etReceipt?.isFocusableInTouchMode = false
        etReceipt?.isClickable = true
        etReceipt?.isCursorVisible = false
        etReceipt?.hint = "클릭하여 영수증 촬영"
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RECEIPT_CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
                    // 카메라 촬영 결과 처리
            val imageBitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra("data", Bitmap::class.java)
                    } else {
                @Suppress("DEPRECATION")
                data?.getParcelableExtra("data")
            }
            
            if (imageBitmap != null) {
                // 영수증 입력창에 촬영 완료 메시지 표시
                val etReceipt = contentView.findViewById<EditText>(R.id.et_receipt)
                etReceipt?.setText("영수증 촬영 완료")
                
                // 키보드 방지 설정 유지
                etReceipt?.isFocusable = false
                etReceipt?.isFocusableInTouchMode = false
                etReceipt?.isCursorVisible = false
                
                // 촬영 완료 후 모든 터치 이벤트 비활성화 (중복 촬영 방지)
                etReceipt?.setOnTouchListener(null)
                etReceipt?.setOnClickListener(null)
                etReceipt?.setOnLongClickListener(null)
                
                // 길게 누르면 다시 촬영할 수 있도록 설정
                etReceipt?.setOnLongClickListener {
                    // 다시 촬영 확인 다이얼로그
                    android.app.AlertDialog.Builder(this)
                        .setTitle("영수증 재촬영")
                        .setMessage("영수증을 다시 촬영하시겠습니까?")
                        .setPositiveButton("재촬영") { _, _ ->
                            isReceiptCaptured = false
                            setupReceiptInput(contentView)
                            etReceipt?.setText("클릭하여 영수증 촬영")
                            etReceipt?.hint = "클릭하여 영수증 촬영"
                        }
                        .setNegativeButton("취소", null)
                        .show()
                    true
                }
                
                // 촬영 완료 상태를 저장
                isReceiptCaptured = true
                
                android.util.Log.d("LedgerCreate", "영수증 촬영 완료")
                android.widget.Toast.makeText(this, "영수증 이미지가 촬영되었습니다", Toast.LENGTH_SHORT).show()
                
                // 여기에 이미지 저장 및 OCR 처리 로직 추가 가능
                // imageBitmap을 사용하여 이미지 처리
            }
        }
    }
    
    // Android 기본 카메라로 영수증 촬영
    private fun openReceiptCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            // Android 13+ 호환성을 위해 registerForActivityResult 사용 권장
            // 하지만 간단한 구현을 위해 startActivityForResult 사용
            @Suppress("DEPRECATION")
            startActivityForResult(intent, RECEIPT_CAMERA_REQUEST_CODE)
        } catch (e: Exception) {
            android.util.Log.e("LedgerCreate", "카메라 앱 열기 실패: ${e.message}")
            android.widget.Toast.makeText(this, "카메라를 열 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 카메라 권한 체크 및 요청
    private fun checkCameraPermission(): Boolean {
        return when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                true
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
                // 권한이 필요한 이유를 설명하는 다이얼로그 표시
                showPermissionRationaleDialog()
                false
            }
            else -> {
                // 권한 요청
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
                false
            }
        }
    }
    
    // 권한이 필요한 이유를 설명하는 다이얼로그
    private fun showPermissionRationaleDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("카메라 권한 필요")
            .setMessage("영수증을 촬영하기 위해 카메라 권한이 필요합니다.")
            .setPositiveButton("권한 요청") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    android.widget.Toast.makeText(this, "카메라 권한이 허용되었습니다", Toast.LENGTH_SHORT).show()
                    // 권한이 허용되면 카메라 열기
                    openReceiptCamera()
                } else {
                    android.widget.Toast.makeText(this, "카메라 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
                    // 권한이 거부된 경우 사용자에게 안내
                    android.widget.Toast.makeText(this, "설정에서 카메라 권한을 허용해주세요", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun setupRegisterButton(contentView: android.view.View) {
        val btnRegister = contentView.findViewById<android.widget.Button>(R.id.btn_register)
        btnRegister?.setOnClickListener {
            android.util.Log.d("LedgerCreate", "등록하기 버튼 클릭됨")
            
            // 입력 데이터 검증
            if (validateInputs()) {
                // 데이터 저장 및 페이지 전환
                saveLedgerData()
                navigateToLedgerList()
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        // 수입/지출 선택 확인
        val incomeExpenseSpinner = contentView.findViewById<Spinner>(R.id.spinner_income_expense)
        val selectedPosition = incomeExpenseSpinner?.selectedItemPosition ?: 0
        if (selectedPosition == 0) {
            android.widget.Toast.makeText(this, "수입/지출을 선택해주세요", android.widget.Toast.LENGTH_SHORT).show()
            return false
        }
        
        // 거래처명 확인
        val partnerName = contentView.findViewById<EditText>(R.id.et_partner_name)?.text.toString()
        if (partnerName.trim().isEmpty()) {
            android.widget.Toast.makeText(this, "거래처명을 입력해주세요", android.widget.Toast.LENGTH_SHORT).show()
            return false
        }
        
        // 금액 확인
        val amount = contentView.findViewById<EditText>(R.id.et_amount)?.text.toString()
        if (amount.trim().isEmpty()) {
            android.widget.Toast.makeText(this, "금액을 입력해주세요", android.widget.Toast.LENGTH_SHORT).show()
            return false
        }
        
        // 거래일시 확인
        val transactionDateTime = contentView.findViewById<EditText>(R.id.et_transaction_datetime)?.text.toString()
        if (transactionDateTime.trim().isEmpty()) {
            android.widget.Toast.makeText(this, "거래일시를 선택해주세요", android.widget.Toast.LENGTH_SHORT).show()
            return false
        }
        
        // 결제수단 확인
        val paymentMethodSpinner = contentView.findViewById<Spinner>(R.id.spinner_payment_method)
        val paymentPosition = paymentMethodSpinner?.selectedItemPosition ?: 0
        if (paymentPosition == 0) {
            android.widget.Toast.makeText(this, "결제수단을 선택해주세요", android.widget.Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun saveLedgerData() {
        // 입력된 데이터를 수집
        val incomeExpenseSpinner = contentView.findViewById<Spinner>(R.id.spinner_income_expense)
        val partnerName = contentView.findViewById<EditText>(R.id.et_partner_name)?.text.toString()
        val amount = contentView.findViewById<EditText>(R.id.et_amount)?.text.toString()
        val transactionDateTime = contentView.findViewById<EditText>(R.id.et_transaction_datetime)?.text.toString()
        val paymentMethodSpinner = contentView.findViewById<Spinner>(R.id.spinner_payment_method)
        val memo = contentView.findViewById<EditText>(R.id.et_memo)?.text.toString()
        
        android.util.Log.d("LedgerCreate", "장부 데이터 수집:")
        android.util.Log.d("LedgerCreate", "수입/지출: ${incomeExpenseSpinner?.selectedItem}")
        android.util.Log.d("LedgerCreate", "거래처명: $partnerName")
        android.util.Log.d("LedgerCreate", "금액: $amount")
        android.util.Log.d("LedgerCreate", "거래일시: $transactionDateTime")
        android.util.Log.d("LedgerCreate", "결제수단: ${paymentMethodSpinner?.selectedItem}")
        android.util.Log.d("LedgerCreate", "메모: $memo")
        
        // POST 요청으로 API에 데이터 전송
        sendLedgerDataToAPI(
            date = transactionDateTime,
            amount = amount,
            type = incomeExpenseSpinner?.selectedItem.toString(),
            paymentMethod = paymentMethodSpinner?.selectedItem.toString(),
            description = memo,
            receipt = if (isReceiptCaptured) "captured" else "",
            name = partnerName
        )
    }
    
    private fun navigateToLedgerList() {
        try {
            val intent = android.content.Intent(this, com.example.myapplication.MainActivity::class.java)
            // MainActivity가 이미 스택에 있으면 새로 만들지 않고 기존 것을 사용합니다.
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish() // 현재 생성 액티비티는 종료
        } catch (e: Exception) {
            android.util.Log.e("LedgerCreate", "MainActivity로 이동 실패: ${e.message}")
            android.widget.Toast.makeText(this, "페이지 이동에 실패했습니다", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupSpinnersFromView(contentView: android.view.View) {
        android.util.Log.d("LedgerCreate", "setupSpinnersFromView 시작")
        
        // 수입/지출 스피너 설정
        val incomeExpenseSpinner = contentView.findViewById<Spinner>(R.id.spinner_income_expense)
        
        if (incomeExpenseSpinner != null) {
            val incomeExpenseOptions = arrayOf("항목을 선택하세요", "수입", "지출")
            
            // 간단한 어댑터 사용
            val incomeExpenseAdapter = createSimpleSpinnerAdapter(incomeExpenseOptions)
            
            incomeExpenseSpinner.adapter = incomeExpenseAdapter
            
            // 기본 선택 항목 설정 (첫 번째 항목)
            incomeExpenseSpinner.setSelection(0)
            
            // 스피너 클릭 이벤트 설정
            incomeExpenseSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    android.util.Log.d("LedgerCreate", "수입/지출 선택됨: position=$position, item=${incomeExpenseOptions[position]}")
                }
                
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                    android.util.Log.d("LedgerCreate", "수입/지출 아무것도 선택되지 않음")
                }
            })
        } else {
            android.util.Log.e("LedgerCreate", "수입/지출 스피너를 찾을 수 없음!")
        }
        
        // 결제수단 스피너 설정
        val paymentMethodSpinner = contentView.findViewById<Spinner>(R.id.spinner_payment_method)
        
        if (paymentMethodSpinner != null) {
            val paymentMethodOptions = arrayOf("항목을 선택하세요", "현금", "카드", "계좌이체", "기타")
            
            // 간단한 어댑터 사용
            val paymentMethodAdapter = createSimpleSpinnerAdapter(paymentMethodOptions)
            
            paymentMethodSpinner.adapter = paymentMethodAdapter
            
            // 기본 선택 항목 설정 (첫 번째 항목)
            paymentMethodSpinner.setSelection(0)
            
            // 결제수단 스피너에도 OnItemSelectedListener 추가
            paymentMethodSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    android.util.Log.d("LedgerCreate", "결제수단 선택됨: position=$position, item=${paymentMethodOptions[position]}")
                }
                
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                    android.util.Log.d("LedgerCreate", "결제수단 아무것도 선택되지 않음")
                }
            })
        } else {
            android.util.Log.e("LedgerCreate", "결제수단 스피너를 찾을 수 없음!")
        }
    }
    
    private fun setupAmountFormatting() {
        etAmount.addTextChangedListener(object : android.text.TextWatcher {
            private var isFormatting = false
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isFormatting) return
                
                val str = s.toString().replace(",", "")
                if (str.isNotEmpty()) {
                    try {
                        val number = str.toLong()
                        val formatted = String.format("%,d", number)
                        if (formatted != s.toString()) {
                            isFormatting = true
                            etAmount.setText(formatted)
                            etAmount.setSelection(formatted.length)
                            isFormatting = false
                        }
                    } catch (e: NumberFormatException) {
                        // 숫자가 아닌 문자가 입력된 경우 무시
                    }
                }
            }
        })
    }
    
    private fun setupDateTimePicker() {
        etTransactionDateTime.setOnClickListener {
            showDateTimePickerDialog()
        }
    }
    
    private fun showDateTimePickerDialog() {
        val currentDate = Calendar.getInstance()
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                // 날짜 선택 후 시간 입력 다이얼로그 표시
                showTimeInputDialog(year, month, dayOfMonth)
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
    }
    
    private fun showTimeInputDialog(year: Int, month: Int, dayOfMonth: Int) {
        val timeInputDialog = android.app.AlertDialog.Builder(this)
            .setTitle("시간 입력")
            .setMessage("시간을 입력해주세요 (예: 14:30)")
            .setView(createTimeInputView())
            .setPositiveButton("확인") { dialog, _ ->
                val timeInputView = (dialog as android.app.AlertDialog).findViewById<EditText>(android.R.id.text1)
                val timeText = timeInputView?.text.toString()
                
                if (timeText.isNotEmpty()) {
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, month, dayOfMonth)
                    
                    // 시간 파싱 및 설정
                    try {
                        val timeParts = timeText.split(":")
                        if (timeParts.size == 2) {
                            val hour = timeParts[0].toInt()
                            val minute = timeParts[1].toInt()
                            
                            if (hour in 0..23 && minute in 0..59) {
                                selectedDate.set(Calendar.HOUR_OF_DAY, hour)
                                selectedDate.set(Calendar.MINUTE, minute)
                                
                                // 선택된 날짜와 시간을 입력창에 표시
                                val dateTimeText = "${dateFormat.format(selectedDate.time)} ${String.format("%02d:%02d", hour, minute)}"
                                etTransactionDateTime.setText(dateTimeText)
                                
                                // 키보드 숨기기
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.hideSoftInputFromWindow(etTransactionDateTime.windowToken, 0)
                            } else {
                                android.widget.Toast.makeText(this, "올바른 시간을 입력해주세요 (00:00 ~ 23:59)", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            android.widget.Toast.makeText(this, "올바른 시간 형식을 입력해주세요 (예: 14:30)", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: NumberFormatException) {
                        android.widget.Toast.makeText(this, "올바른 시간을 입력해주세요", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        
        timeInputDialog.show()
        
        // 다이얼로그가 표시된 후 키보드 자동 표시
        timeInputDialog.setOnShowListener {
            val timeInputView = timeInputDialog.findViewById<EditText>(android.R.id.text1)
            timeInputView?.requestFocus()
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(timeInputView, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    
    private fun createTimeInputView(): EditText {
        val timeInput = EditText(this)
        timeInput.hint = "시간 입력 (예: 14:30)"
        timeInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_CLASS_DATETIME
        timeInput.setPadding(50, 50, 50, 50)
        return timeInput
    }
    
    // 간단한 스피너 어댑터 (문제 해결을 위한 백업)
    private fun createSimpleSpinnerAdapter(options: Array<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<android.widget.TextView>(android.R.id.text1)
                
                // 첫 번째 항목(힌트)은 회색으로, 나머지는 검정색으로 설정
                if (position == 0) {
                    textView.setTextColor(android.graphics.Color.parseColor("#999999")) // 힌트 색상
                    textView.textSize = 12f // 힌트 텍스트와 동일한 크기
                } else {
                    textView.setTextColor(android.graphics.Color.parseColor("#333333")) // 일반 텍스트 색상
                    textView.textSize = 16f // 일반 텍스트 크기
                }
                
                return view
            }
            
            override fun getDropDownView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View? {
                // null을 반환하지 않고 정상적인 뷰를 반환하되, 첫 번째 항목은 다른 스타일 적용
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view.findViewById<android.widget.TextView>(android.R.id.text1)
                
                if (position == 0) {
                    // 힌트 항목도 드롭다운에 표시하되 회색으로 표시
                    textView.setTextColor(android.graphics.Color.parseColor("#999999"))
                    textView.textSize = 12f
                } else {
                    // 일반 항목들은 검정색으로 표시
                    textView.setTextColor(android.graphics.Color.parseColor("#333333"))
                    textView.textSize = 16f
                }
                
                // 드롭다운 항목들 사이에 여백 추가 (하단 여백은 줄임)
                textView.setPadding(
                    textView.paddingLeft + 16,  // 왼쪽 패딩 증가
                    textView.paddingTop + 8,    // 위쪽 패딩 줄임 (12 -> 8)
                    textView.paddingRight + 16, // 오른쪽 패딩 증가
                    textView.paddingBottom + 4  // 아래쪽 패딩 줄임 (12 -> 4)
                )
                
                return view
            }
        }
    }

    companion object {
        private const val RECEIPT_CAMERA_REQUEST_CODE = 1002
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1003
    }
    
    private fun sendLedgerDataToAPI(
        date: String,
        amount: String,
        type: String,
        paymentMethod: String,
        description: String,
        receipt: String,
        name: String
    ) {
        android.util.Log.d("LedgerCreate", "API 호출 시작")
        
        // 기본값 설정 (실제로는 동적으로 가져와야 함)
        val clubPk = "1" // 임시 값
        val ledgerPk = "1" // 임시 값
        
        val transactionData = JSONObject().apply {
            put("date", date)
            put("amount", amount.replace(",", "")) // 쉼표 제거
            put("type", type)
            put("payment_method", paymentMethod)
            put("description", description)
            put("receipt", receipt)
            put("name", name)
        }
        
        android.util.Log.d("LedgerCreate", "POST 요청 데이터: ${transactionData}")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("LedgerCreate", "API 호출 시작: http://54.206.122.170/club/$clubPk/ledger/$ledgerPk/transactions/")
                
                val url = URL("http://54.206.122.170/club/$clubPk/ledger/$ledgerPk/transactions/")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                
                val outputStream = connection.outputStream
                val writer = OutputStreamWriter(outputStream)
                writer.write(transactionData.toString())
                writer.flush()
                writer.close()
                
                val responseCode = connection.responseCode
                android.util.Log.d("LedgerCreate", "응답 코드: $responseCode")
                
                withContext(Dispatchers.Main) {
                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                        android.widget.Toast.makeText(this@LedgerCreateActivity, "장부 데이터가 성공적으로 등록되었습니다!", android.widget.Toast.LENGTH_LONG).show()
                        android.util.Log.d("LedgerCreate", "장부 데이터 등록 성공")
                        // 성공 시 장부 리스트로 이동
                        navigateToLedgerList()
                    } else {
                        android.widget.Toast.makeText(this@LedgerCreateActivity, "장부 데이터 등록에 실패했습니다. (응답 코드: $responseCode)", android.widget.Toast.LENGTH_LONG).show()
                        android.util.Log.e("LedgerCreate", "장부 데이터 등록 실패 - 응답 코드: $responseCode")
                    }
                }
                
                connection.disconnect()
                
            } catch (e: Exception) {
                android.util.Log.e("LedgerCreate", "장부 데이터 등록 중 오류 발생", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@LedgerCreateActivity, "장부 데이터 등록 중 오류가 발생했습니다: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
