package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class ClubFeeBatchBillingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_fee_batch_billing)

        findViewById<Button>(R.id.btn_back)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 헤더 햄버거 메뉴: 구성원 관리 / 회비 관리 (공지 목록과 동일 경로)
        findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_menu)?.setOnClickListener { v ->
            val popup = android.widget.PopupMenu(this, v)
            popup.menu.add(0, 1, 0, "구성원 관리")
            popup.menu.add(0, 2, 1, "회비 관리")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        val clubPk = intent?.getIntExtra(ClubFeeManagementActivity.EXTRA_CLUB_PK, -1) ?: -1
                        val userPk = UserManager.getUserPk(this) ?: -1
                        val i = android.content.Intent(this, ClubMemberManagementActivity::class.java)
                        i.putExtra(ClubMemberManagementActivity.EXTRA_CLUB_PK, clubPk)
                        i.putExtra(ClubMemberManagementActivity.EXTRA_USER_PK, userPk)
                        startActivity(i)
                        true
                    }
                    2 -> {
                        val clubPk = intent?.getIntExtra(ClubFeeManagementActivity.EXTRA_CLUB_PK, -1) ?: -1
                        val i = android.content.Intent(this, ClubFeeManagementActivity::class.java)
                        i.putExtra(ClubFeeManagementActivity.EXTRA_CLUB_PK, clubPk)
                        startActivity(i)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        // 금액 입력 시 1,000 단위 콤마 표시
        val etAmount = findViewById<android.widget.EditText>(R.id.et_billing_amount)
        etAmount?.addTextChangedListener(object : android.text.TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s == null) return
                val str = s.toString()
                if (str == current) return
                // 숫자만 추출
                val digits = str.replace(",", "").replace("[^0-9]".toRegex(), "")
                if (digits.isEmpty()) { current = ""; etAmount.setText(""); return }
                val formatted = try {
                    val n = java.math.BigInteger(digits)
                    java.text.NumberFormat.getInstance().format(n)
                } catch (_: Exception) { digits }
                current = formatted
                etAmount.removeTextChangedListener(this)
                etAmount.setText(formatted)
                etAmount.setSelection(formatted.length)
                etAmount.addTextChangedListener(this)
            }
        })

        // 스피너에 힌트 추가 (첫 항목)
        val spinner = findViewById<android.widget.Spinner>(R.id.spinner_billing_month)
        val months = resources.getStringArray(R.array.club_fee_months).toMutableList()
        months.add(0, "청구 월을 선택하세요")
        val adapter = object : android.widget.ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_dropdown_item, months
        ) {
            override fun isEnabled(position: Int): Boolean {
                // 힌트 항목은 선택 비활성화
                return position != 0
            }
        }
        spinner?.adapter = adapter
        spinner?.setSelection(0)

        // 일괄 청구 요청 API 연결
        findViewById<Button>(R.id.btn_request_batch_billing)?.setOnClickListener {
            val monthSpinner = findViewById<android.widget.Spinner>(R.id.spinner_billing_month)
            val monthLabel = monthSpinner?.selectedItem?.toString() ?: ""
            val month = monthLabel.replace("월", "").trim()
            val amountPlain = etAmount?.text?.toString()?.replace(",", "")?.trim()
            val amount = amountPlain?.toIntOrNull() ?: 0

            if (month.isEmpty() || amount <= 0) {
                android.widget.Toast.makeText(this, "월과 금액을 확인하세요.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val clubPk = intent?.getIntExtra(ClubFeeManagementActivity.EXTRA_CLUB_PK, -1) ?: -1
            if (clubPk <= 0) {
                android.widget.Toast.makeText(this, "동아리 정보를 확인할 수 없습니다.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
            val url = "$baseUrl/club/$clubPk/dues/batch_claim/"

            val json = com.google.gson.JsonObject().apply {
                addProperty("month", month)
                addProperty("amount", amount)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            val req = okhttp3.Request.Builder().url(url).post(body).build()

            val client = com.example.myapplication.api.ApiClient.createUnsafeOkHttpClient()
            client.newCall(req).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    runOnUiThread {
                        android.widget.Toast.makeText(this@ClubFeeBatchBillingActivity, "요청 실패: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    runOnUiThread {
                        if (response.isSuccessful) {
                            android.widget.Toast.makeText(this@ClubFeeBatchBillingActivity, "성공적으로 회비가 청구되었습니다", android.widget.Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            val bodyStr = try { response.body?.string() } catch (_: Exception) { null }
                            android.widget.Toast.makeText(this@ClubFeeBatchBillingActivity, "실패: ${response.code} ${bodyStr ?: ""}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
        }
    }
}


