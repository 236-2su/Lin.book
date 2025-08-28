package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ClubEventLedgerDetailActivity : AppCompatActivity() {
    
    private var clubPk: Int = -1
    private var eventPk: Int = -1
    private lateinit var eventName: String
    private lateinit var eventStartDate: String
    private lateinit var eventEndDate: String
    private lateinit var transactionAdapter: EventTransactionAdapter
    private lateinit var recyclerView: RecyclerView
    private var eventBudget: Long = 0L
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_event_ledger_detail)
        
        clubPk = intent.getIntExtra("club_pk", -1)
        eventPk = intent.getIntExtra("event_pk", -1)
        eventName = intent.getStringExtra("event_name") ?: ""
        eventStartDate = intent.getStringExtra("event_start_date") ?: ""
        eventEndDate = intent.getStringExtra("event_end_date") ?: ""
        
        setupViews()
        setupTabButtons()
        setupRecyclerView()
        
        if (clubPk != -1) {
            fetchClubDetail(clubPk)
        }
        
        if (clubPk != -1 && eventPk != -1) {
            fetchEventDetail()
            fetchEventTransactions()
        }
    }
    
    private fun setupViews() {
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        
        findViewById<TextView>(R.id.tv_event_title).text = eventName
        findViewById<TextView>(R.id.tv_event_period).text = "행사 예정 기간: $eventStartDate ~ $eventEndDate"
        
        // 월별 네비게이션 버튼들은 현재 레이아웃에서 주석 처리됨
        // findViewById<TextView>(R.id.btn_prev_month).setOnClickListener {
        // }
        // 
        // findViewById<TextView>(R.id.btn_next_month).setOnClickListener {
        // }
        
        findViewById<TextView>(R.id.tv_total_budget).text = "0원"
        findViewById<TextView>(R.id.tv_current_expense).text = "0원"
    }
    
    private fun setupTabButtons() {
        findViewById<TextView>(R.id.btn_notice).setOnClickListener {
            val intent = android.content.Intent(this, ClubAnnouncementBoardListActivity::class.java)
            intent.putExtra("club_pk", clubPk)
            startActivity(intent)
            finish()
        }
        
        findViewById<TextView>(R.id.btn_free_board).setOnClickListener {
            val intent = android.content.Intent(this, ClubForumBoardListActivity::class.java)
            intent.putExtra("club_pk", clubPk)
            startActivity(intent)
            finish()
        }
        
        findViewById<TextView>(R.id.btn_event_account).setOnClickListener {
        }
        
        findViewById<TextView>(R.id.btn_public_account).setOnClickListener {
        }
        
        findViewById<TextView>(R.id.btn_meeting_account).setOnClickListener {
        }
        
        findViewById<TextView>(R.id.btn_ai_report).setOnClickListener {
        }
    }
    
    private fun fetchClubDetail(clubPk: Int) {
        if (clubPk <= 0) return
        
        ApiClient.getApiService().getClubDetail(clubPk).enqueue(object : Callback<ClubItem> {
            override fun onResponse(call: Call<ClubItem>, response: Response<ClubItem>) {
                if (!isFinishing && !isDestroyed) {
                    val club = response.body()
                    if (response.isSuccessful && club != null) {
                        findViewById<TextView>(R.id.tv_club_title)?.text = club.name
                    }
                }
            }
            
            override fun onFailure(call: Call<ClubItem>, t: Throwable) {
                Log.e("ClubEventLedgerDetail", "동아리 정보 로드 실패: ${t.message}")
            }
        })
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rv_transaction_list)
        transactionAdapter = EventTransactionAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = transactionAdapter
    }
    
    private fun fetchEventTransactions() {
        ApiClient.getApiService().getEventTransactions(clubPk, eventPk)
            .enqueue(object : Callback<List<EventTransactionItem>> {
                override fun onResponse(
                    call: Call<List<EventTransactionItem>>,
                    response: Response<List<EventTransactionItem>>
                ) {
                    if (response.isSuccessful) {
                        val transactions = response.body() ?: emptyList()
                        Log.d("ClubEventLedgerDetail", "거래내역 ${transactions.size}개 로드 성공")
                        transactionAdapter.updateData(transactions)
                        
                        // 총 예산과 현재 지출 계산
                        updateBudgetSummary(transactions)
                    } else {
                        Log.e("ClubEventLedgerDetail", "거래내역 로드 실패: ${response.code()}")
                    }
                }
                
                override fun onFailure(
                    call: Call<List<EventTransactionItem>>,
                    t: Throwable
                ) {
                    Log.e("ClubEventLedgerDetail", "거래내역 로드 실패: ${t.message}")
                }
            })
    }
    
    private fun updateBudgetSummary(transactions: List<EventTransactionItem>) {
        val totalExpense = transactions.filter { it.amount < 0 }.sumOf { Math.abs(it.amount) }
        
        val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA)
        // 총 예산은 fetchEventDetail에서 설정됨
        findViewById<TextView>(R.id.tv_current_expense).text = "${formatter.format(totalExpense)}원"
    }
    
    private fun fetchEventDetail() {
        ApiClient.getApiService().getEventDetail(clubPk, eventPk)
            .enqueue(object : Callback<ApiService.EventDetailResponse> {
                override fun onResponse(
                    call: Call<ApiService.EventDetailResponse>,
                    response: Response<ApiService.EventDetailResponse>
                ) {
                    if (response.isSuccessful) {
                        val event = response.body()
                        if (event != null) {
                            eventBudget = event.budget
                            val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA)
                            findViewById<TextView>(R.id.tv_total_budget).text = "${formatter.format(eventBudget)}원"
                            
                            // 이벤트 정보 업데이트
                            findViewById<TextView>(R.id.tv_event_title).text = event.name
                            findViewById<TextView>(R.id.tv_event_period).text = "행사 예정 기간: ${event.start_date} ~ ${event.end_date}"
                        }
                    } else {
                        Log.e("ClubEventLedgerDetail", "이벤트 상세 로드 실패: ${response.code()}")
                    }
                }
                
                override fun onFailure(
                    call: Call<ApiService.EventDetailResponse>,
                    t: Throwable
                ) {
                    Log.e("ClubEventLedgerDetail", "이벤트 상세 로드 실패: ${t.message}")
                }
            })
    }
}