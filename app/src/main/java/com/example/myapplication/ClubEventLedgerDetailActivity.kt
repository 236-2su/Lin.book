package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Intent

class ClubEventLedgerDetailActivity : AppCompatActivity() {
    
    private var clubPk: Int = -1
    private var eventPk: Int = -1
    private lateinit var eventName: String
    private lateinit var eventStartDate: String
    private lateinit var eventEndDate: String
    
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
        
        if (clubPk != -1) {
            fetchClubDetail(clubPk)
        }
    }
    
    private fun setupViews() {
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        
        findViewById<TextView>(R.id.tv_event_title).text = eventName
        findViewById<TextView>(R.id.tv_event_period).text = "행사 예정 기간: $eventStartDate ~ $eventEndDate"
        
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add_transaction).setOnClickListener {
        }
        
        findViewById<TextView>(R.id.btn_prev_month).setOnClickListener {
        }
        
        findViewById<TextView>(R.id.btn_next_month).setOnClickListener {
        }
        
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
            // MainActivity로 이동하여 MeetingAccountFragment 표시 (root_page와 동일한 과정)
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("show_meeting_account", true)
            intent.putExtra("club_pk", clubPk)
            startActivity(intent)
            finish()
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
}