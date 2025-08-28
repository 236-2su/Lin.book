package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.ApiService
import androidx.appcompat.app.AppCompatActivity

class AccountHistoryActivity : AppCompatActivity() {
    private var currentYear: Int = 0
    private var currentMonth: Int = 0 // 1~12
    private var todayYear: Int = 0
    private var todayMonth: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_history)

        // 헤더의 동아리명 표기 (공지/자유 리스트와 동일한 방식)
        val clubPk = intent?.getIntExtra("club_pk", -1) ?: -1
        if (clubPk > 0) {
            com.example.myapplication.api.ApiClient.getApiService().getClubDetail(clubPk)
                .enqueue(object : retrofit2.Callback<com.example.myapplication.ClubItem> {
                    override fun onResponse(
                        call: retrofit2.Call<com.example.myapplication.ClubItem>,
                        response: retrofit2.Response<com.example.myapplication.ClubItem>
                    ) {
                        val club = response.body()
                        if (response.isSuccessful && club != null) {
                            findViewById<TextView>(R.id.tv_club_title)?.text = club.name
                        }
                    }
                    override fun onFailure(
                        call: retrofit2.Call<com.example.myapplication.ClubItem>,
                        t: Throwable
                    ) { }
                })
        }

        // 계좌 정보 불러오기
        fetchAndBindAccountInfo()

        findViewById<android.widget.Button>(R.id.btn_back)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 상단 카테고리 네비게이션
        findViewById<TextView>(R.id.btn_notice)?.setOnClickListener {
            val intent = Intent(this, ClubAnnouncementBoardListActivity::class.java)
            intent.putExtra("club_pk", clubPk)
            startActivity(intent)
            finish()
        }
        findViewById<TextView>(R.id.btn_free_board)?.setOnClickListener {
            val intent = Intent(this, ClubForumBoardListActivity::class.java)
            intent.putExtra("club_pk", clubPk)
            startActivity(intent)
            finish()
        }
        findViewById<TextView>(R.id.btn_event_account)?.setOnClickListener {
            val intent = Intent(this, ClubEventLedgerListActivity::class.java)
            intent.putExtra(ClubEventLedgerListActivity.EXTRA_CLUB_PK, clubPk)
            startActivity(intent)
            finish()
        }

        // 초기 년/월 세팅 (오늘 기준)
        val now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"))
        todayYear = now.year
        todayMonth = now.monthValue
        currentYear = todayYear
        currentMonth = todayMonth
        updateYearMonthUi()

        // 월 이동 버튼
        findViewById<android.widget.ImageView>(R.id.btn_prev_month)?.setOnClickListener {
            moveMonth(-1)
        }
        findViewById<android.widget.ImageView>(R.id.btn_next_month)?.setOnClickListener {
            moveMonth(1)
        }

        // 계좌 연동 토글: ON 기본, OFF로 내릴 때 확인 다이얼로그
        val switch = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.sw_account_visible)
        switch?.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                showAccountUnlinkDialog(onCancelled = { switch.isChecked = true }, onConfirm = {
                    // TODO: 연동 해제 API 호출이 있다면 연결
                })
            }
        }
    }

    private fun moveMonth(delta: Int) {
        // delta: -1 이전달, +1 다음달
        var y = currentYear
        var m = currentMonth + delta
        if (m < 1) { m = 12; y -= 1 }
        if (m > 12) { m = 1; y += 1 }
        // 미래로 이동 금지
        if (y > todayYear || (y == todayYear && m > todayMonth)) {
            updateArrowsEnabled()
            return
        }
        currentYear = y
        currentMonth = m
        updateYearMonthUi()
        // TODO: 해당 년월의 내역/요약 API를 호출하여 데이터 갱신
    }

    private fun updateYearMonthUi() {
        val tvYear = findViewById<TextView>(R.id.tv_year)
        val tvMonth = findViewById<TextView>(R.id.tv_month)
        tvYear?.text = String.format("%d년", currentYear)
        tvMonth?.text = String.format("%02d월", currentMonth)
        updateArrowsEnabled()
    }

    private fun showAccountUnlinkDialog(onCancelled: () -> Unit, onConfirm: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_confirm, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setGravity(android.view.Gravity.BOTTOM)
        dialog.window?.attributes?.windowAnimations = R.style.Animation_Dialog
        dialog.setCanceledOnTouchOutside(true)

        // 텍스트 교체
        dialogView.findViewById<TextView>(R.id.tv_message)?.text = "계좌 연동을 해제하시겠습니까"
        dialogView.findViewById<android.widget.Button>(R.id.btn_delete)?.apply {
            text = "해제하기"
            setOnClickListener {
                onConfirm()
                dialog.dismiss()
            }
        }

        dialog.setOnCancelListener { onCancelled() }
        dialog.show()
    }

    private fun updateArrowsEnabled() {
        val next = findViewById<android.widget.ImageView>(R.id.btn_next_month)
        val atToday = (currentYear == todayYear && currentMonth == todayMonth)
        next?.isEnabled = !atToday
        next?.alpha = if (atToday) 0.3f else 1.0f
    }

    private fun fetchAndBindAccountInfo() {
        val userPk = UserManager.getUserPk(this)
        if (userPk == null) {
            android.util.Log.e("AccountHistory", "user_pk가 없습니다. 로그인 필요")
            return
        }

        // 우선 사용자의 첫 번째 계좌를 조회해 사용. accounts_id가 Intent로 넘어오면 우선 사용
        val explicitAccountId = intent?.getIntExtra("accounts_id", -1) ?: -1
        val api = ApiClient.getApiService()
        if (explicitAccountId > 0) {
            requestAccountDetail(api, userPk, explicitAccountId)
            return
        }

        api.getAccounts(userPk).enqueue(object: retrofit2.Callback<List<ApiService.AccountItem>> {
            override fun onResponse(
                call: retrofit2.Call<List<ApiService.AccountItem>>,
                response: retrofit2.Response<List<ApiService.AccountItem>>
            ) {
                val list = response.body()
                if (response.isSuccessful && !list.isNullOrEmpty()) {
                    val first = list.first()
                    requestAccountDetail(api, userPk, first.id)
                }
            }
            override fun onFailure(
                call: retrofit2.Call<List<ApiService.AccountItem>>,
                t: Throwable
            ) {
                android.util.Log.e("AccountHistory", "getAccounts 실패", t)
            }
        })
    }

    private fun requestAccountDetail(api: ApiService, userPk: Int, accountId: Int) {
        api.getAccountDetail(userPk, accountId).enqueue(object: retrofit2.Callback<ApiService.AccountItem> {
            override fun onResponse(
                call: retrofit2.Call<ApiService.AccountItem>,
                response: retrofit2.Response<ApiService.AccountItem>
            ) {
                val item = response.body()
                if (response.isSuccessful && item != null) {
                    // code, created_at, user_name 바인딩
                    findViewById<TextView>(R.id.tv_account_number)?.text = item.code
                    findViewById<TextView>(R.id.tv_holder)?.text = item.user_name
                    findViewById<TextView>(R.id.tv_bank)?.text = item.created_at
                }
            }
            override fun onFailure(
                call: retrofit2.Call<ApiService.AccountItem>,
                t: Throwable
            ) {
                android.util.Log.e("AccountHistory", "getAccountDetail 실패", t)
            }
        })
    }
}


