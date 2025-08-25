package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LedgerEditActivity : BaseActivity() {

    private lateinit var contentView: View
    private lateinit var etAmount: EditText
    private lateinit var etMemo: EditText
    private lateinit var etReceipt: EditText
    private lateinit var etPartnerName: EditText
    private lateinit var etTransactionDatetime: EditText
    private lateinit var spinnerType: Spinner
    private lateinit var spinnerPaymentMethod: Spinner
    
    private var isFormatting = false
    private var originalAmount = ""
    private var ledgerItem: LedgerItem? = null

    override fun setupContent(savedInstanceState: Bundle?) {
        // 앱 제목을 "장부 수정"으로 설정
        setAppTitle("장부 수정")

        // LedgerEditActivity의 내용을 content_container에 추가
        val contentContainer = findViewById<android.widget.FrameLayout>(R.id.content_container)
        contentView = layoutInflater.inflate(R.layout.activity_ledger_edit, null)
        contentContainer.addView(contentView)

        // 게시판 버튼들 숨기기
        hideBoardButtons()

        // 전달받은 데이터 설정
        setupInitialData()

        // 뷰 초기화
        initializeViews()

        // 스피너 설정
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
        ledgerItem = intent.getSerializableExtra("LEDGER_ITEM") as? LedgerItem
        if (ledgerItem == null) {
            Toast.makeText(this, "수정할 장부 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    private fun initializeViews() {
        etAmount = contentView.findViewById(R.id.et_amount)
        etMemo = contentView.findViewById(R.id.et_memo)
        etReceipt = contentView.findViewById(R.id.et_receipt)
        etPartnerName = contentView.findViewById(R.id.et_partner_name)
        etTransactionDatetime = contentView.findViewById(R.id.et_transaction_datetime)
        spinnerType = contentView.findViewById(R.id.spinner_type)
        spinnerPaymentMethod = contentView.findViewById(R.id.spinner_payment_method)

        // 기존 데이터로 초기화
        ledgerItem?.let { item ->
            etAmount.setText(item.amount.replace("+ ", "").replace("- ", "").replace("원", ""))
            etMemo.setText(item.memo.replace("· 메모 : ", ""))
            etReceipt.setText("") // 영수증은 기존 데이터에 없으므로 빈 값
            etPartnerName.setText("") // 거래처명은 기존 데이터에 없으므로 빈 값
            etTransactionDatetime.setText(item.date) // 전체 날짜/시간을 하나의 필드에 설정
            
            // 타입 설정
            val typePosition = when (item.type) {
                "수입" -> 0
                "지출" -> 1
                else -> 0
            }
            spinnerType.setSelection(typePosition)
            
            // 결제수단은 기본값으로 설정
            spinnerPaymentMethod.setSelection(0)
        }
    }

    private fun setupSpinners() {
        // 타입 스피너
        val typeAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.ledger_types,
            R.layout.spinner_item_custom
        )
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_custom)
        spinnerType.adapter = typeAdapter

        // 결제수단 스피너
        val paymentMethodAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.payment_methods,
            R.layout.spinner_item_custom
        )
        paymentMethodAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_custom)
        spinnerPaymentMethod.adapter = paymentMethodAdapter
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
        val btnSave = contentView.findViewById<TextView>(R.id.btn_save)
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
}
