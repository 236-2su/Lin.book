package com.example.myapplication

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.ApiService.EventItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class EventCreateActivity : AppCompatActivity() {

    private lateinit var etEventName: EditText
    private lateinit var etStartDate: EditText
    private lateinit var etEndDate: EditText
    private lateinit var etBudget: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnCancel: TextView
    private lateinit var btnCreate: TextView
    
    private var clubPk: Int = -1

    companion object {
        const val EXTRA_CLUB_PK = "club_pk"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_ledger_create)

        clubPk = intent.getIntExtra(EXTRA_CLUB_PK, -1)

        setupViews()
        setupTabButtons()
        
        if (clubPk != -1) {
            fetchClubDetail(clubPk)
        }
    }

    private fun setupViews() {
        // 뒤로가기 버튼
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 입력 필드들
        etEventName = findViewById(R.id.et_event_name)
        etStartDate = findViewById(R.id.et_start_date)
        etEndDate = findViewById(R.id.et_end_date)
        etBudget = findViewById(R.id.et_budget)
        etDescription = findViewById(R.id.et_description)
        
        // 버튼들
        btnCancel = findViewById(R.id.btn_cancel)
        btnCreate = findViewById(R.id.btn_create)

        // 날짜 선택 리스너
        etStartDate.setOnClickListener {
            showDatePicker { date ->
                etStartDate.setText(date)
            }
        }

        etEndDate.setOnClickListener {
            showDatePicker { date ->
                etEndDate.setText(date)
            }
        }

        // 버튼 리스너
        btnCancel.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnCreate.setOnClickListener {
            createEvent()
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
        
        // 행사장부 버튼
        findViewById<TextView>(R.id.btn_event_account).setOnClickListener {
            val intent = Intent(this, ClubEventLedgerListActivity::class.java)
            intent.putExtra(EXTRA_CLUB_PK, clubPk)
            startActivity(intent)
            finish()
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(selectedYear, selectedMonth, selectedDay)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            onDateSelected(dateFormat.format(selectedDate.time))
        }, year, month, day).show()
    }

    private fun createEvent() {
        val eventName = etEventName.text.toString().trim()
        val startDate = etStartDate.text.toString().trim()
        val endDate = etEndDate.text.toString().trim()
        val budgetText = etBudget.text.toString().trim()
        val description = etDescription.text.toString().trim()

        // 유효성 검사
        if (eventName.isEmpty()) {
            Toast.makeText(this, "행사명을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (startDate.isEmpty()) {
            Toast.makeText(this, "행사 시작일자를 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (endDate.isEmpty()) {
            Toast.makeText(this, "행사 종료일자를 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (budgetText.isEmpty()) {
            Toast.makeText(this, "예산 금액을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val budget = try {
            budgetText.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "올바른 예산 금액을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val request = EventCreateRequest(
            name = eventName,
            start_date = startDate,
            end_date = endDate,
            description = description,
            budget = budget
        )

        // API 호출
        ApiClient.getApiService().createEvent(clubPk, request).enqueue(object : Callback<EventItem> {
            override fun onResponse(call: Call<EventItem>, response: Response<EventItem>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@EventCreateActivity, "행사장부가 생성되었습니다", Toast.LENGTH_SHORT).show()
                    
                    // ClubEventLedgerListActivity로 이동
                    val intent = Intent(this@EventCreateActivity, ClubEventLedgerListActivity::class.java)
                    intent.putExtra(EXTRA_CLUB_PK, clubPk)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@EventCreateActivity, "행사장부 생성에 실패했습니다: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<EventItem>, t: Throwable) {
                Toast.makeText(this@EventCreateActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("EventCreate", "API 호출 실패", t)
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
                Log.e("EventCreate", "동아리 정보 로드 실패: ${t.message}")
            }
        })
    }
}