package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.ApiService.EventItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ClubEventLedgerListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var eventAdapter: EventLedgerAdapter
    private val eventList = mutableListOf<EventItem>()
    private var clubPk: Int = -1

    companion object {
        const val EXTRA_CLUB_PK = "club_pk"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_event_ledger_list)

        clubPk = intent.getIntExtra(EXTRA_CLUB_PK, -1)

        setupViews()
        setupTabButtons()
        
        if (clubPk != -1) {
            loadEventList(clubPk)
            fetchClubDetail(clubPk)
        }
    }

    private fun setupViews() {
        // 뒤로가기 버튼
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // RecyclerView 설정
        recyclerView = findViewById(R.id.rv_ledger_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        eventAdapter = EventLedgerAdapter(eventList)
        recyclerView.adapter = eventAdapter

        // FAB 클릭 리스너
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add_ledger).setOnClickListener {
            val intent = Intent(this, EventCreateActivity::class.java)
            intent.putExtra(EventCreateActivity.EXTRA_CLUB_PK, clubPk)
            startActivity(intent)
        }
    }

    private fun setupTabButtons() {
        // 공지사항 버튼
        findViewById<TextView>(R.id.btn_notice).setOnClickListener {
            val intent = Intent(this, ClubAnnouncementBoardListActivity::class.java)
            intent.putExtra(EXTRA_CLUB_PK, clubPk)
            startActivity(intent)
            finish()
        }
        
        // 자유게시판 버튼
        findViewById<TextView>(R.id.btn_free_board).setOnClickListener {
            val intent = Intent(this, ClubForumBoardListActivity::class.java)
            intent.putExtra(EXTRA_CLUB_PK, clubPk)
            startActivity(intent)
            finish()
        }
        
        // 행사장부 버튼 (현재 페이지이므로 아무것도 안함)
        findViewById<TextView>(R.id.btn_event_account).setOnClickListener {
            // 현재 페이지
        }
        
        // AI 리포트 버튼
        findViewById<TextView>(R.id.btn_ai_report).setOnClickListener {
            val intent = Intent(this, LedgerReportActivity::class.java)
            intent.putExtra("club_id", clubPk)
            startActivity(intent)
            finish()
        }
    }

    private fun loadEventList(clubPk: Int) {
        ApiClient.getApiService().getEventList(clubPk).enqueue(object : Callback<List<EventItem>> {
            override fun onResponse(
                call: Call<List<EventItem>>,
                response: Response<List<EventItem>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val events = response.body()!!
                    Log.d("ClubEventLedgerList", "API 성공: ${events.size}개 행사")
                    
                    eventList.clear()
                    eventList.addAll(events)
                    eventAdapter.notifyDataSetChanged()
                } else {
                    Log.e("ClubEventLedgerList", "API 응답 오류: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<EventItem>>, t: Throwable) {
                Log.e("ClubEventLedgerList", "API 호출 실패: ${t.message}")
            }
        })
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
                Log.e("ClubEventLedgerList", "동아리 정보 로드 실패: ${t.message}")
            }
        })
    }
}