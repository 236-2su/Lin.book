package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText
import android.widget.Spinner
import android.widget.Button
import android.widget.ArrayAdapter

class ClubUpdateActivity : AppCompatActivity() {
    private var existingShortDescription: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_update)

        val clubPk = intent?.getIntExtra("club_pk", -1) ?: -1

        findViewById<android.widget.Button>(R.id.btn_back)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 대분류 스피너는 xml entries로 바인딩되어 있음. 필요 시 어댑터 커스터마이즈 가능

        // 기존 정보 로드
        if (clubPk > 0) {
            com.example.myapplication.api.ApiClient.getApiService().getClubDetail(clubPk)
                .enqueue(object : retrofit2.Callback<com.example.myapplication.ClubItem> {
                    override fun onResponse(
                        call: retrofit2.Call<com.example.myapplication.ClubItem>,
                        response: retrofit2.Response<com.example.myapplication.ClubItem>
                    ) {
                        val club = response.body() ?: return
                        findViewById<EditText>(R.id.et_club_name)?.setText(club.name)
                        findViewById<EditText>(R.id.et_department)?.setText(club.department)
                        // major_category 스피너 선택
                        val spinner = findViewById<Spinner>(R.id.spinner_major_category)
                        (spinner?.adapter as? ArrayAdapter<CharSequence>)?.let { adapter ->
                            val pos = (0 until adapter.count).firstOrNull { adapter.getItem(it).toString().contains(club.majorCategory) } ?: 0
                            spinner.setSelection(pos)
                        }
                        findViewById<EditText>(R.id.et_minor_category)?.setText(club.minorCategory)
                        findViewById<EditText>(R.id.et_description)?.setText(club.description)
                        findViewById<EditText>(R.id.et_hashtags)?.setText(club.hashtags)
                        findViewById<EditText>(R.id.et_location)?.setText(club.location)
                        // 웰컴 문구 (short_description) 매핑
                        existingShortDescription = club.shortDescription
                        findViewById<EditText>(R.id.et_welcome_intro_text)?.setText(existingShortDescription ?: "")
                    }
                    override fun onFailure(
                        call: retrofit2.Call<com.example.myapplication.ClubItem>,
                        t: Throwable
                    ) { }
                })
        }

        // 수정하기 버튼 → PUT /club/{id}/ 업데이트 후 공지 목록으로 복귀
        findViewById<Button>(R.id.btn_create_club)?.apply {
            text = "수정하기"
            setOnClickListener {
                if (clubPk <= 0) { finish(); return@setOnClickListener }
                val name = findViewById<EditText>(R.id.et_club_name)?.text?.toString()?.trim().orEmpty()
                val department = findViewById<EditText>(R.id.et_department)?.text?.toString()?.trim().orEmpty()
                val major = findViewById<Spinner>(R.id.spinner_major_category)?.selectedItem?.toString()?.let {
                    // strings.xml에 한글이 있을 수 있어 서버코드값으로 매핑 필요시 추가
                    when {
                        it.contains("학술") -> "academic"
                        it.contains("체육") -> "sports"
                        it.contains("문화") -> "culture"
                        it.contains("봉사") -> "volunteer"
                        it.contains("창업") -> "entrepreneur"
                        it.contains("종교") -> "religion"
                        else -> "academic"
                    }
                } ?: "academic"
                val minor = findViewById<EditText>(R.id.et_minor_category)?.text?.toString()?.trim().orEmpty()
                val description = findViewById<EditText>(R.id.et_description)?.text?.toString()?.trim().orEmpty()
                val hashtags = findViewById<EditText>(R.id.et_hashtags)?.text?.toString()?.trim().orEmpty()
                val location = findViewById<EditText>(R.id.et_location)?.text?.toString()?.trim().orEmpty()
                var shortDesc = findViewById<EditText>(R.id.et_welcome_intro_text)?.text?.toString()?.trim().orEmpty()
                if (shortDesc.isBlank()) {
                    shortDesc = existingShortDescription?.takeIf { it.isNotBlank() } ?: description.take(50)
                }

                val api = com.example.myapplication.api.ApiClient.getApiService()
                // 빈 필드를 제외하고 변경된 값만 PATCH로 전송
                val fields = linkedMapOf<String, String>()
                if (name.isNotBlank()) fields["name"] = name
                if (department.isNotBlank()) fields["department"] = department
                if (major.isNotBlank()) fields["major_category"] = major
                if (minor.isNotBlank()) fields["minor_category"] = minor
                if (description.isNotBlank()) fields["description"] = description
                if (hashtags.isNotBlank()) fields["hashtags"] = hashtags
                if (location.isNotBlank()) fields["location"] = location
                if (shortDesc.isNotBlank()) fields["short_description"] = shortDesc

                api.patchClubForm(clubPk, fields)
                    .enqueue(object : retrofit2.Callback<com.example.myapplication.ClubItem> {
                        override fun onResponse(
                            call: retrofit2.Call<com.example.myapplication.ClubItem>,
                            response: retrofit2.Response<com.example.myapplication.ClubItem>
                        ) {
                            if (response.isSuccessful) {
                                // 성공 시 공지 목록으로 이동 후 새 데이터가 보이도록
                                val intent = android.content.Intent(this@ClubUpdateActivity, ClubAnnouncementBoardListActivity::class.java)
                                intent.putExtra("club_pk", clubPk)
                                startActivity(intent)
                                finish()
                            } else {
                                val bodyStr = try { response.errorBody()?.string() } catch (_: Exception) { null }
                                android.widget.Toast.makeText(this@ClubUpdateActivity, "수정 실패: ${response.code()} ${bodyStr ?: ""}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                        override fun onFailure(
                            call: retrofit2.Call<com.example.myapplication.ClubItem>,
                            t: Throwable
                        ) {
                            android.widget.Toast.makeText(this@ClubUpdateActivity, "네트워크 오류: ${t.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    })
            }
        }
    }
}


