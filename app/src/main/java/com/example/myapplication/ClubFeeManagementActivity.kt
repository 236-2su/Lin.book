package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class ClubFeeManagementActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CLUB_PK = "club_pk"
    }

    private var isMember: Boolean = false
    private var isOfficerOrLeader: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activitiy_club_fee_management)

        findViewById<Button>(R.id.btn_back)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 기본 타이틀: 권한 조회 전에는 조회로 표시
        findViewById<TextView>(R.id.tv_club_title)?.text = "회비 조회"

        // 햄버거 메뉴: 구성원/회비 메뉴를 역할에 따라 다르게 표시
        findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_menu)?.setOnClickListener { v ->
            val popup = android.widget.PopupMenu(this, v)
            val membersLabel = if (isOfficerOrLeader) "구성원 관리" else "구성원 조회"
            val feeLabel = if (isOfficerOrLeader) "회비 관리" else "회비 조회"
            popup.menu.add(0, 1, 0, membersLabel)
            popup.menu.add(0, 2, 1, feeLabel)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
                        val userPk = UserManager.getUserPk(this) ?: -1
                        val i = android.content.Intent(this, ClubMemberManagementActivity::class.java)
                        i.putExtra(ClubMemberManagementActivity.EXTRA_CLUB_PK, clubPk)
                        i.putExtra(ClubMemberManagementActivity.EXTRA_USER_PK, userPk)
                        startActivity(i)
                        true
                    }
                    2 -> {
                        val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
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
        // 기본으로 햄버거는 숨김, 멤버십 확인 후 표시
        findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_menu)?.visibility = android.view.View.GONE

        findViewById<android.view.View>(R.id.btn_fee_batch_billing)?.setOnClickListener {
            val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val i = android.content.Intent(this, ClubFeeBatchBillingActivity::class.java)
            i.putExtra(ClubFeeManagementActivity.EXTRA_CLUB_PK, clubPk)
            startActivity(i)
        }

        findViewById<android.view.View>(R.id.btn_fee_batch_list)?.setOnClickListener {
            val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val i = android.content.Intent(this, ClubFeeMonthlyBillingListActivity::class.java)
            i.putExtra(ClubFeeManagementActivity.EXTRA_CLUB_PK, clubPk)
            startActivity(i)
        }

        // 권한에 따라 UI 제어 및 타이틀/버튼 가시성 조정
        val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
        applyMembershipUi(clubPk)

        // 미납 회비 불러오기
        loadUnpaidDues()
    }

    private fun applyMembershipUi(clubPk: Int) {
        if (clubPk <= 0) {
            // 비정상 값이면 비회원 기준으로 표시
            isMember = false
            isOfficerOrLeader = false
            findViewById<TextView>(R.id.tv_club_title)?.text = "회비 조회"
            findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_menu)?.visibility = android.view.View.GONE
            // 운영진 전용 버튼/구분선 숨김
            findViewById<android.view.View>(R.id.btn_fee_batch_billing)?.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.btn_fee_batch_list)?.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.divider_admin_only)?.visibility = android.view.View.GONE
            return
        }

        val userPk = UserManager.getUserPk(this)
        if (userPk == null) {
            // 로그인 안 한 경우: 비회원 처리
            isMember = false
            isOfficerOrLeader = false
            findViewById<TextView>(R.id.tv_club_title)?.text = "회비 조회"
            findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_menu)?.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.btn_fee_batch_billing)?.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.btn_fee_batch_list)?.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.divider_admin_only)?.visibility = android.view.View.GONE
            return
        }

        val api = ApiClient.getApiService()
        api.getClubMembers(clubPk).enqueue(object : Callback<List<com.example.myapplication.MemberResponse>> {
            override fun onResponse(
                call: Call<List<com.example.myapplication.MemberResponse>>,
                response: Response<List<com.example.myapplication.MemberResponse>>
            ) {
                val members = response.body().orEmpty()
                val mine = members.firstOrNull { it.user == userPk && it.status == "active" }
                val isOfficer = mine?.role == "leader" || mine?.role == "officer"
                isMember = mine != null
                isOfficerOrLeader = isOfficer

                // 타이틀
                findViewById<TextView>(R.id.tv_club_title)?.text = if (isOfficer) "회비 관리" else "회비 조회"
                // 햄버거: 회원만 표시
                findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_menu)?.visibility = if (isMember) android.view.View.VISIBLE else android.view.View.GONE
                // 운영진 전용 버튼/구분선 표시
                val adminVisibility = if (isOfficer) android.view.View.VISIBLE else android.view.View.GONE
                findViewById<android.view.View>(R.id.btn_fee_batch_billing)?.visibility = adminVisibility
                findViewById<android.view.View>(R.id.btn_fee_batch_list)?.visibility = adminVisibility
                findViewById<android.view.View>(R.id.divider_admin_only)?.visibility = adminVisibility
            }

            override fun onFailure(
                call: Call<List<com.example.myapplication.MemberResponse>>,
                t: Throwable
            ) {
                // 실패 시 비회원 기준으로 처리
                isMember = false
                isOfficerOrLeader = false
                findViewById<TextView>(R.id.tv_club_title)?.text = "회비 조회"
                findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_menu)?.visibility = android.view.View.GONE
                findViewById<android.view.View>(R.id.btn_fee_batch_billing)?.visibility = android.view.View.GONE
                findViewById<android.view.View>(R.id.btn_fee_batch_list)?.visibility = android.view.View.GONE
                findViewById<android.view.View>(R.id.divider_admin_only)?.visibility = android.view.View.GONE
            }
        })
    }

    private fun loadUnpaidDues() {
        val api = ApiClient.getApiService()
        val userId = UserManager.getUserPk(this) ?: run {
            android.widget.Toast.makeText(this, "로그인이 필요합니다.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        api.getUnpaidDues(userId).enqueue(object : Callback<List<ApiService.UnpaidDueItem>> {
            override fun onResponse(
                call: Call<List<ApiService.UnpaidDueItem>>, response: Response<List<ApiService.UnpaidDueItem>>
            ) {
                if (!response.isSuccessful) return
                val items = response.body().orEmpty()
                val container = findViewById<android.widget.LinearLayout>(R.id.container_my_fee_cards)
                val emptyContainer = findViewById<android.widget.LinearLayout>(R.id.empty_unpaid_container)
                val tvEmpty = findViewById<TextView>(R.id.tv_unpaid_empty)
                container?.removeAllViews()

                val inflater = android.view.LayoutInflater.from(this@ClubFeeManagementActivity)
                val sanitized = items.filter {
                    (it.description ?: "").isNotBlank()
                }
                if (sanitized.isEmpty()) {
                    emptyContainer?.visibility = android.view.View.VISIBLE
                    return
                } else {
                    emptyContainer?.visibility = android.view.View.GONE
                }

                sanitized.forEach { item ->
                    val view = inflater.inflate(R.layout.item_unpaid_due, container, false)
                    val tvClub = view.findViewById<TextView>(R.id.tv_club_name)
                    val tvDesc = view.findViewById<TextView>(R.id.tv_description)
                    val tvAmount = view.findViewById<TextView>(R.id.tv_amount)
                    val btnPay = view.findViewById<Button>(R.id.btn_pay)

                    tvClub.text = item.club_name ?: ""
                    tvDesc.text = item.description ?: ""

                    val chargedInt = (item.charged_amount ?: "0").replace(",", "").toIntOrNull() ?: 0
                    val formatted = try {
                        NumberFormat.getNumberInstance(Locale.getDefault()).format(chargedInt)
                    } catch (_: Exception) { chargedInt.toString() }
                    tvAmount.text = "요청금액: ${formatted}원"

                    btnPay.setOnClickListener {
                        var clubPk = (item.club_pk?.toIntOrNull() ?: -1)
                        if (clubPk <= 0) {
                            val clubIdFromCard = try {
                                val field = ApiService.UnpaidDueItem::class.java.getDeclaredField("club_pk")
                                (item.club_pk ?: "").toIntOrNull()
                            } catch (_: Exception) { null }
                            if (clubIdFromCard != null && clubIdFromCard > 0) clubPk = clubIdFromCard
                        }
                        if (clubPk <= 0) clubPk = getFallbackClubPk()

                        val monthParsed = (item.month ?: "").toIntOrNull()
                            ?: kotlin.run {
                                val m = Regex("(\\d{1,2})\\s*월").find(item.description ?: "")?.groupValues?.getOrNull(1)
                                m?.toIntOrNull()
                            }
                        val monthInt = monthParsed ?: 0

                        if (clubPk <= 0) {
                            android.util.Log.e("ClubFeeManagement", "유효하지 않은 clubPk (payload='${item.club_pk}', fallback='${getFallbackClubPk()}')")
                            android.widget.Toast.makeText(this@ClubFeeManagementActivity, "클럽 정보를 확인할 수 없어 납부를 진행할 수 없습니다.", android.widget.Toast.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                        if (monthInt !in 1..12) {
                            android.util.Log.e("ClubFeeManagement", "유효하지 않은 month (description='${item.description}', raw='${item.month}')")
                            android.widget.Toast.makeText(this@ClubFeeManagementActivity, "월 정보를 확인할 수 없습니다.", android.widget.Toast.LENGTH_LONG).show()
                            return@setOnClickListener
                        }

                        android.util.Log.d("ClubFeeManagement", "payDues clubPk=${clubPk}, month=${monthInt}, amount=${chargedInt}")
                        payDues(clubPk, userId, monthInt, chargedInt)
                    }

                    container?.addView(view)
                }
            }

            override fun onFailure(call: Call<List<ApiService.UnpaidDueItem>>, t: Throwable) {
                val emptyContainer = findViewById<android.widget.LinearLayout>(R.id.empty_unpaid_container)
                val tvEmpty = findViewById<TextView>(R.id.tv_unpaid_empty)
                tvEmpty?.text = "미납된 회비가 없습니다."
                emptyContainer?.visibility = android.view.View.VISIBLE
            }
        })
    }

    private fun getFallbackClubPk(): Int {
        // 1) Intent Extra
        val fromExtra = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
        if (fromExtra > 0) return fromExtra

        // 2) 로그인 시 저장한 내 클럽 목록 중 첫 번째
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        val listStr = prefs.getString("club_pks", null)
        val first = listStr?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.firstOrNull()
        if (first != null && first > 0) return first

        return -1
    }

    private fun payDues(clubPk: Int, userId: Int, month: Int, chargedAmount: Int) {
        val api = ApiClient.getApiService()
        val body = ApiService.DuesPayRequest(user_id = userId, month = month)
        api.payDues(clubPk, body).enqueue(object : Callback<okhttp3.ResponseBody> {
            override fun onResponse(
                call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>
            ) {
                if (response.isSuccessful) {
                    val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault())
                    android.widget.Toast.makeText(this@ClubFeeManagementActivity, "${nf.format(chargedAmount)}원이 정상적으로 납부되었습니다.", android.widget.Toast.LENGTH_LONG).show()
                    // 성공 후 갱신
                    loadUnpaidDues()
                } else {
                    android.widget.Toast.makeText(this@ClubFeeManagementActivity, "납부 실패: ${response.code()}", android.widget.Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                android.widget.Toast.makeText(this@ClubFeeManagementActivity, "네트워크 오류: ${t.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        })
    }
}


