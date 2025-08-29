package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.widget.Button

class LedgerEditActivity : BaseActivity() {

    private lateinit var contentView: View
    private lateinit var etAmount: EditText
    private lateinit var etMemo: EditText
    private lateinit var etReceipt: EditText
    private lateinit var etPartnerName: EditText
    private lateinit var etTransactionCategory: EditText
    private lateinit var etTransactionDatetime: EditText
    private lateinit var spinnerType: Spinner
    private lateinit var spinnerPaymentMethod: Spinner
    
    private var isFormatting = false
    private var originalAmount = ""

    override fun setupContent(savedInstanceState: Bundle?) {
        // 앱 제목을 "장부 수정"으로 설정
        setAppTitle("장부 수정")

        // LedgerEditActivity의 내용을 content_container에 추가
        val contentContainer = findViewById<android.widget.FrameLayout>(R.id.content_container)
        contentView = layoutInflater.inflate(R.layout.activity_ledger_edit, null)
        contentContainer.addView(contentView)

        // 게시판 버튼들 숨기기
        hideBoardButtons()
        
        // 뒤로 가기 버튼 표시
        showBackButton()

        // 전달받은 데이터 설정
        setupInitialData()

        // 뷰 초기화 (스피너 설정 전에 먼저 실행)
        initializeViews()

        // 스피너 설정 (뷰 초기화 후에 실행)
        setupSpinners()

        // 금액 포맷팅 설정
        setupAmountFormatting()

        // 거래일시 설정
        setupTransactionDatetime()

        // 영수증 설정
        setupReceipt()

        // 저장 버튼 설정
        setupSaveButton()
    }

    private fun setupInitialData() {
        // 디버깅을 위한 로그 추가
        Log.d("LedgerEditActivity", "setupInitialData 호출")
        Log.d("LedgerEditActivity", "=== Intent로 받은 모든 데이터 ===")
        Log.d("LedgerEditActivity", "VENDOR_NAME: '${intent.getStringExtra("VENDOR_NAME")}'")
        Log.d("LedgerEditActivity", "PAYMENT_METHOD: '${intent.getStringExtra("PAYMENT_METHOD")}'")
        Log.d("LedgerEditActivity", "ORIGINAL_AMOUNT: ${intent.getLongExtra("ORIGINAL_AMOUNT", -1L)}")
        Log.d("LedgerEditActivity", "ORIGINAL_TYPE: '${intent.getStringExtra("ORIGINAL_TYPE")}'")
        Log.d("LedgerEditActivity", "ORIGINAL_DATE: '${intent.getStringExtra("ORIGINAL_DATE")}'")
        Log.d("LedgerEditActivity", "ORIGINAL_MEMO: '${intent.getStringExtra("ORIGINAL_MEMO")}'")
        Log.d("LedgerEditActivity", "TRANSACTION_CATEGORY: '${intent.getStringExtra("TRANSACTION_CATEGORY")}'")
        Log.d("LedgerEditActivity", "=== Intent 데이터 확인 완료 ===")
        
        // 필수 데이터가 없으면 종료
        if (intent.getLongExtra("ORIGINAL_AMOUNT", -1L) == -1L) {
            Toast.makeText(this, "수정할 장부 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    private fun initializeViews() {
        etAmount = findViewById(R.id.et_amount)
        etMemo = findViewById(R.id.et_memo)
        etReceipt = findViewById(R.id.et_receipt)
        etPartnerName = findViewById(R.id.et_partner_name)
        etTransactionCategory = findViewById(R.id.et_transaction_category)
        etTransactionDatetime = findViewById(R.id.et_transaction_datetime)
        spinnerType = findViewById(R.id.spinner_type)
        spinnerPaymentMethod = findViewById(R.id.spinner_payment_method)

        // 원본 데이터를 Intent에서 직접 가져와서 설정
        val originalAmount = intent.getLongExtra("ORIGINAL_AMOUNT", 0L)
        val originalType = intent.getStringExtra("ORIGINAL_TYPE") ?: ""
        val originalDate = intent.getStringExtra("ORIGINAL_DATE") ?: ""
        val originalMemo = intent.getStringExtra("ORIGINAL_MEMO") ?: ""
        val vendorName = intent.getStringExtra("VENDOR_NAME") ?: ""
        val paymentMethod = intent.getStringExtra("PAYMENT_METHOD") ?: ""
        
        // 디버깅을 위한 로그 추가
        Log.d("LedgerEditActivity", "initializeViews에서 설정할 데이터:")
        Log.d("LedgerEditActivity", "originalAmount: $originalAmount")
        Log.d("LedgerEditActivity", "originalType: $originalType")
        Log.d("LedgerEditActivity", "originalDate: $originalDate")
        Log.d("LedgerEditActivity", "originalMemo: '$originalMemo'")
        Log.d("LedgerEditActivity", "vendorName: '$vendorName'")
        Log.d("LedgerEditActivity", "paymentMethod: '$paymentMethod'")
        
        // 금액 설정 (원본 금액의 부호 유지)
        if (originalAmount != 0L) {
            val amountWithSign = if (originalAmount > 0) "+${originalAmount}" else originalAmount.toString()
            etAmount.setText(amountWithSign)
            Log.d("LedgerEditActivity", "금액 설정: $amountWithSign")
        } else {
            Log.w("LedgerEditActivity", "원본 금액이 0이거나 설정되지 않음")
        }
        
        // 메모 설정
        if (originalMemo.isNotEmpty()) {
            etMemo.setText(originalMemo)
            Log.d("LedgerEditActivity", "메모 설정: '$originalMemo'")
        } else {
            Log.w("LedgerEditActivity", "원본 메모가 비어있음")
        }
        
        // 영수증은 기존 데이터에 없으므로 빈 값
        etReceipt.setText("")
        
        // 거래처명 설정
        if (vendorName.isNotEmpty()) {
            etPartnerName.setText(vendorName)
            Log.d("LedgerEditActivity", "거래처명 설정: '$vendorName'")
        } else {
            Log.w("LedgerEditActivity", "거래처명이 비어있음")
        }
        
        // 내역 구분 설정 (TRANSACTION_CATEGORY 값 사용)
        val transactionCategory = intent.getStringExtra("TRANSACTION_CATEGORY") ?: ""
        Log.d("LedgerEditActivity", "TRANSACTION_CATEGORY로 받은 값: '$transactionCategory'")
        
        if (transactionCategory.isNotEmpty()) {
            etTransactionCategory.setText(transactionCategory)
            Log.d("LedgerEditActivity", "내역 구분 설정 완료: '$transactionCategory'")
        } else {
            Log.w("LedgerEditActivity", "내역 구분이 비어있음 - TRANSACTION_CATEGORY가 비어있음")
        }
        
        // 거래일시 설정
        if (originalDate.isNotEmpty()) {
            etTransactionDatetime.setText(originalDate)
            Log.d("LedgerEditActivity", "거래일시 설정: '$originalDate'")
        } else {
            Log.w("LedgerEditActivity", "거래일시가 비어있음")
        }
        
        // 모든 데이터 설정 완료 후 로그
        Log.d("LedgerEditActivity", "=== 데이터 설정 완료 ===")
        Log.d("LedgerEditActivity", "금액 입력창: ${etAmount.text}")
        Log.d("LedgerEditActivity", "메모 입력창: ${etMemo.text}")
        Log.d("LedgerEditActivity", "거래처명 입력창: ${etPartnerName.text}")
        Log.d("LedgerEditActivity", "거래일시 입력창: ${etTransactionDatetime.text}")
        

    }

    private fun setupSpinners() {
        // 타입 스피너 설정
        val typeOptions = arrayOf("항목을 선택하세요", "수입", "지출")
        val typeAdapter = createSimpleSpinnerAdapter(typeOptions)
        spinnerType.adapter = typeAdapter
        spinnerType.setSelection(0)
        
        // 타입 스피너 클릭 이벤트 설정
        spinnerType.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedItem = typeOptions[position]
                Log.d("LedgerEdit", "카테고리 타입 선택됨: position=$position, item=$selectedItem")
                
                // 내역 구분 입력창은 원래 내용 유지 (스피너 선택으로 덮어쓰지 않음)
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                Log.d("LedgerEdit", "카테고리 타입 아무것도 선택되지 않음")
            }
        })

        // 결제수단 스피너 설정
        val paymentMethodOptions = arrayOf("항목을 선택하세요", "현금", "카드", "계좌이체", "기타")
        val paymentMethodAdapter = createSimpleSpinnerAdapter(paymentMethodOptions)
        spinnerPaymentMethod.adapter = paymentMethodAdapter
        spinnerPaymentMethod.setSelection(0)
        
        // 결제수단 스피너에도 OnItemSelectedListener 추가
        spinnerPaymentMethod.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedItem = paymentMethodOptions[position]
                Log.d("LedgerEdit", "결제수단 선택됨: position=$position, item=$selectedItem")
                
                // 메모 입력창은 원래 내용 유지 (결제수단으로 덮어쓰지 않음)
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                Log.d("LedgerEdit", "결제수단 아무것도 선택되지 않음")
            }
        })
        
        // 어댑터 설정이 완료된 후에 스피너 데이터 설정
        setupSpinnerData()
    }
    
    private fun setupSpinnerData() {
        // 금액을 기반으로 수입/지출 자동 설정
        val originalAmount = intent.getLongExtra("ORIGINAL_AMOUNT", 0L)
        val typePosition = when {
            originalAmount > 0 -> 1  // 양수면 "수입" 선택
            originalAmount < 0 -> 2  // 음수면 "지출" 선택
            else -> 0                // 0이면 "항목을 선택하세요"
        }
        spinnerType.setSelection(typePosition)
        
        // 결제수단 설정 (Intent에서 받은 값 사용)
        val paymentMethod = intent.getStringExtra("PAYMENT_METHOD") ?: ""
        val paymentMethodPosition = when (paymentMethod) {
            "현금" -> 1
            "카드" -> 2
            "계좌이체" -> 3
            "기타" -> 4
            else -> 0  // 찾을 수 없으면 기본값
        }
        spinnerPaymentMethod.setSelection(paymentMethodPosition)
        
        // 내역 구분 입력창에 ORIGINAL_TYPE 값 설정 (공모전 등)
        val originalType = intent.getStringExtra("ORIGINAL_TYPE") ?: ""
        if (originalType.isNotEmpty()) {
            etTransactionCategory.setText(originalType)
            Log.d("LedgerEditActivity", "내역 구분 입력창 설정: '$originalType'")
        } else {
            Log.w("LedgerEditActivity", "ORIGINAL_TYPE이 비어있음")
        }
        
        // 디버깅을 위한 로그
        Log.d("LedgerEditActivity", "스피너 데이터 설정 완료:")
        Log.d("LedgerEditActivity", "원본 금액: $originalAmount, 타입 위치: $typePosition")
        Log.d("LedgerEditActivity", "결제수단: '$paymentMethod' (위치: $paymentMethodPosition)")
        Log.d("LedgerEditActivity", "내역 구분: '$originalType'")
        
        // 스피너 상태 확인을 위한 추가 로그 (post로 지연 실행)
        spinnerType.post {
            Log.d("LedgerEditActivity", "타입 스피너 선택된 항목: ${spinnerType.selectedItem}")
        }
        spinnerPaymentMethod.post {
            Log.d("LedgerEditActivity", "결제수단 스피너 선택된 항목: ${spinnerPaymentMethod.selectedItem}")
        }
    }

    private fun setupAmountFormatting() {
        etAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (!isFormatting) {
                    isFormatting = true
                    val str = s.toString().replace(",", "")
                    if (str.isNotEmpty()) {
                        try {
                            val amount = str.toLong()
                            val formatted = String.format("%,d", amount)
                            if (formatted != s.toString()) {
                                etAmount.setText(formatted)
                                etAmount.setSelection(formatted.length)
                            }
                        } catch (e: NumberFormatException) {
                            // 숫자가 아닌 경우 처리
                        }
                    }
                    isFormatting = false
                }
            }
        })
    }

    private fun setupTransactionDatetime() {
        etTransactionDatetime.setOnClickListener {
            showDateTimePicker()
        }
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = android.app.DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val monthStr = String.format("%02d", month + 1)
                val dayStr = String.format("%02d", dayOfMonth)
                val dateText = "${year}년 ${monthStr}월 ${dayStr}일"
                
                // 시간 선택 다이얼로그 표시
                val timePickerDialog = android.app.TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        val hourStr = String.format("%02d", hourOfDay)
                        val minuteStr = String.format("%02d", minute)
                        etTransactionDatetime.setText("$dateText $hourStr:$minuteStr")
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                )
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun setupReceipt() {
        etReceipt.setOnClickListener {
            // 영수증 촬영 기능 (현재는 단순히 텍스트만 표시)
            etReceipt.setText("영수증 촬영 완료")
        }
    }

    private fun setupSaveButton() {
        val btnSave = findViewById<Button>(R.id.btn_save)
        btnSave.setOnClickListener {
            if (validateInput()) {
                saveLedger()
            }
        }
    }

    private fun validateInput(): Boolean {
        val amount = etAmount.text.toString().replace(",", "")
        val memo = etMemo.text.toString()

        if (amount.isEmpty()) {
            Toast.makeText(this, "금액을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (memo.isEmpty()) {
            Toast.makeText(this, "메모를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun saveLedger() {
        val amount = etAmount.text.toString().replace(",", "")
        val memo = etMemo.text.toString()
        val receipt = etReceipt.text.toString()
        val partnerName = etPartnerName.text.toString()
        val type = spinnerType.selectedItem.toString()
        val paymentMethod = spinnerPaymentMethod.selectedItem.toString()
        val transactionDatetime = etTransactionDatetime.text.toString()

        // 수정된 데이터를 Intent로 전달
        val resultIntent = Intent()
        resultIntent.putExtra("EDITED_AMOUNT", amount)
        resultIntent.putExtra("EDITED_MEMO", memo)
        resultIntent.putExtra("EDITED_RECEIPT", receipt)
        resultIntent.putExtra("EDITED_PARTNER_NAME", partnerName)
        resultIntent.putExtra("EDITED_TYPE", type)
        resultIntent.putExtra("EDITED_PAYMENT_METHOD", paymentMethod)
        resultIntent.putExtra("EDITED_DATE", transactionDatetime)
        
        setResult(RESULT_OK, resultIntent)
        finish()
        
        Toast.makeText(this, "장부가 수정되었습니다.", Toast.LENGTH_SHORT).show()
    }
    
    // 간단한 스피너 어댑터 (거래 내역 추가 등록 페이지와 동일)
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
}
