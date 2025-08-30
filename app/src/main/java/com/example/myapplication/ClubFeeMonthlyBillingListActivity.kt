package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Spinner
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView as Tv
import androidx.core.content.ContextCompat
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ClubFeeMonthlyBillingListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_fee_monthly_billing_list)

        findViewById<Button>(R.id.btn_back)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        findViewById<TextView>(R.id.tv_club_title)?.text = "요청 회비 납부 현황"

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

        setupSpinners()
    }

    private fun setupSpinners() {
        val spinnerYear = findViewById<Spinner>(R.id.spinner_year)
        val spinnerMonth = findViewById<Spinner>(R.id.spinner_month)
        val container = findViewById<android.widget.LinearLayout>(R.id.container_claims)
        val tvEmpty = findViewById<Tv>(R.id.tv_empty_claims)
        val now = java.util.Calendar.getInstance()
        val thisYear = now.get(java.util.Calendar.YEAR)
        val thisMonth = now.get(java.util.Calendar.MONTH) + 1 // 1..12

        // 연도: 올해 포함 과거 5개
        val years = mutableListOf("연도를 선택하세요").apply {
            for (y in 0 until 5) add((thisYear - y).toString())
        }

        spinnerYear?.adapter = object : ArrayAdapter<String>(
            this, R.layout.item_spinner_hint, R.id.tv_label, years
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                val isHint = position == 0
                val tv = v.findViewById<Tv>(R.id.tv_label)
                tv?.text = if (isHint) "연도를 선택하세요" else years[position]
                bindItemView(v, isHint)
                return v
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = layoutInflater.inflate(R.layout.item_spinner_dropdown, parent, false)
                val tv = v.findViewById<Tv>(R.id.tv_label)
                tv.text = years[position]
                return v
            }
        }

        // 초기 월 목록: 힌트만. 연도 선택 후 동적으로 구성
        val months = mutableListOf("월을 선택하세요")
        spinnerMonth?.adapter = object : ArrayAdapter<String>(
            this, R.layout.item_spinner_hint, R.id.tv_label, months
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                val isHint = position == 0
                val tv = v.findViewById<Tv>(R.id.tv_label)
                tv?.text = if (isHint) "월을 선택하세요" else months[position]
                bindItemView(v, isHint)
                return v
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = layoutInflater.inflate(R.layout.item_spinner_dropdown, parent, false)
                val tv = v.findViewById<Tv>(R.id.tv_label)
                tv.text = months[position]
                return v
            }
        }

        spinnerYear?.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                val isHint = position == 0
                bindItemView(findViewById(R.id.spinner_year), isHint)
                // 배경 전환
                spinnerYear.background = if (isHint)
                    ContextCompat.getDrawable(this@ClubFeeMonthlyBillingListActivity, R.drawable.bg_spinner_outline_gray)
                else
                    ContextCompat.getDrawable(this@ClubFeeMonthlyBillingListActivity, R.drawable.bg_spinner_outline_blue)

                // 연도 선택 시 월 목록 재구성
                val monthAdapter = spinnerMonth.adapter as ArrayAdapter<String>
                monthAdapter.clear()
                monthAdapter.add("월을 선택하세요")

                if (!isHint) {
                    val selectedYear = years[position].toInt()
                    val lastMonth = if (selectedYear == thisYear) kotlin.math.min(12, thisMonth + 1) else 12
                    for (m in 1..lastMonth) monthAdapter.add("${m}월")
                }
                monthAdapter.notifyDataSetChanged()
                spinnerMonth.setSelection(0)

                // 빈 상태 문구 표시 (연도만 선택된 상태)
                tvEmpty?.visibility = android.view.View.VISIBLE
                tvEmpty?.text = "연도와 월을 선택하세요"
                container?.removeAllViews()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        spinnerMonth?.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                val isHint = position == 0
                bindItemView(findViewById(R.id.spinner_month), isHint)
                spinnerMonth.background = if (isHint)
                    ContextCompat.getDrawable(this@ClubFeeMonthlyBillingListActivity, R.drawable.bg_spinner_outline_gray)
                else
                    ContextCompat.getDrawable(this@ClubFeeMonthlyBillingListActivity, R.drawable.bg_spinner_outline_blue)

                // 월이 선택되면 목록 호출
                val yearPos = spinnerYear.selectedItemPosition
                if (!isHint && yearPos > 0) {
                    val selectedYear = years[yearPos].toInt()
                    val monthValue = months[position].replace("월", "").toInt()
                    fetchClaims(selectedYear, monthValue, container, tvEmpty)
                } else {
                    container?.removeAllViews()
                    tvEmpty?.visibility = android.view.View.VISIBLE
                    tvEmpty?.text = "연도와 월을 선택하세요"
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }

    private fun bindItemView(view: View?, isHint: Boolean) {
        if (view == null) return
        val tv = view.findViewById<Tv>(R.id.tv_label)
        val iv = view.findViewById<ImageView>(R.id.iv_arrow)
        if (tv != null && iv != null) {
            if (isHint) {
                tv.setTextColor(0xFF808080.toInt())
                iv.setColorFilter(0xFF808080.toInt())
            } else {
                tv.setTextColor(0xFF000000.toInt())
                iv.setColorFilter(0xFF000000.toInt())
            }
        }
    }

    private fun fetchClaims(year: Int, month: Int, container: android.widget.LinearLayout?, tvEmpty: Tv?) {
        val clubPk = intent?.getIntExtra(ClubFeeManagementActivity.EXTRA_CLUB_PK, -1) ?: -1
        if (clubPk <= 0) return

        val api = ApiClient.getApiService()
        api.getDuesClaimsByMonth(clubPk, month).enqueue(object : Callback<List<ApiService.DuesClaimItem>> {
            override fun onResponse(
                call: Call<List<ApiService.DuesClaimItem>>, response: Response<List<ApiService.DuesClaimItem>>
            ) {
                if (!response.isSuccessful) return
                val all = response.body().orEmpty()
                // description에서 연도 필터링
                val yearStr = year.toString()
                val filtered = all.filter { it.description.contains(yearStr) }
                // 정렬: unpaid 먼저, unpaid 내에서 이름 오름차순
                val sorted = filtered.sortedWith(compareBy<ApiService.DuesClaimItem> { it.paid }.thenBy { it.member_name })
                if (sorted.isEmpty()) {
                    tvEmpty?.visibility = android.view.View.VISIBLE
                    tvEmpty?.text = "해당 월에 대해 회비를 요청하신 현황이 없습니다"
                    container?.removeAllViews()
                } else {
                    tvEmpty?.visibility = android.view.View.GONE
                    renderClaims(sorted, container)
                }
            }

            override fun onFailure(call: Call<List<ApiService.DuesClaimItem>>, t: Throwable) {
                container?.removeAllViews()
                tvEmpty?.visibility = android.view.View.VISIBLE
                tvEmpty?.text = "해당 월에 대해 회비를 요청하신 현황이 없습니다"
            }
        })
    }

    private fun renderClaims(items: List<ApiService.DuesClaimItem>, container: android.widget.LinearLayout?) {
        if (container == null) return
        container.removeAllViews()
        val inflater = layoutInflater
        val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault())
        items.forEach { item ->
            val v = inflater.inflate(R.layout.item_claim_due, container, false)
            val tvName = v.findViewById<Tv>(R.id.tv_member_name)
            val tvNum = v.findViewById<Tv>(R.id.tv_member_number)
            val tvDesc = v.findViewById<Tv>(R.id.tv_description)
            val tvAmt = v.findViewById<Tv>(R.id.tv_amount)
            val tvStatus = v.findViewById<Tv>(R.id.tv_status)

            tvName.text = item.member_name
            tvNum.text = item.member_student_number ?: ""
            tvDesc.text = item.description
            tvAmt.text = "요청금액: ${nf.format(item.charged_amount)}원"

            if (item.paid) {
                tvStatus.text = "납부완료"
                tvStatus.background = ContextCompat.getDrawable(this, R.drawable.bg_status_paid)
            } else {
                tvStatus.text = "미납"
                tvStatus.background = ContextCompat.getDrawable(this, R.drawable.bg_status_unpaid)
            }

            container.addView(v)
        }
    }
}



