package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ClubCreateActivity : BaseActivity() {

    private lateinit var etClubName: EditText
    private lateinit var etDepartment: EditText
    private lateinit var spinnerMajorCategory: Spinner
    private lateinit var etMinorCategory: EditText
    private lateinit var etHashtags: EditText
    private lateinit var etLocation: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnRegister: Button

    override fun setupContent(savedInstanceState: Bundle?) {
        Log.d("ClubCreateActivity", "setupContent 시작")
        
        // ClubCreateActivity의 내용을 content_container에 추가
        val contentContainer = findViewById<android.widget.FrameLayout>(R.id.content_container)
        if (contentContainer != null) {
            Log.d("ClubCreateActivity", "content_container 찾음")
            
            val contentView = layoutInflater.inflate(R.layout.activity_club_create, null)
            Log.d("ClubCreateActivity", "activity_club_create 레이아웃 인플레이트 완료")
            
            contentContainer.addView(contentView)
            Log.d("ClubCreateActivity", "content_container에 레이아웃 추가 완료")

            // root_page.xml의 제목을 "동아리"로 설정
            setAppTitle("동아리")
            Log.d("ClubCreateActivity", "앱 제목 설정 완료")

            // 게시판 버튼 숨기기
            hideBoardButtons()
            Log.d("ClubCreateActivity", "게시판 버튼 숨김 완료")

            // 입력 필드 초기화
            if (initializeViews()) {
                Log.d("ClubCreateActivity", "입력 필드 초기화 완료")
                
                // 등록 버튼 클릭 리스너 설정
                setupRegisterButton()
                Log.d("ClubCreateActivity", "등록 버튼 리스너 설정 완료")
            } else {
                Log.e("ClubCreateActivity", "입력 필드 초기화 실패!")
            }
            
            Log.d("ClubCreateActivity", "ClubCreateActivity 설정 완료")
        } else {
            Log.e("ClubCreateActivity", "content_container를 찾을 수 없음!")
        }
    }

    private fun initializeViews(): Boolean {
        try {
            Log.d("ClubCreateActivity", "initializeViews 시작")
            
            // contentView에서 입력 필드들을 찾아야 합니다
            val contentView = findViewById<android.widget.FrameLayout>(R.id.content_container)
            if (contentView != null) {
                Log.d("ClubCreateActivity", "content_container 찾음")
                
                val clubCreateView = contentView.getChildAt(0) // 첫 번째 자식이 activity_club_create 레이아웃
                if (clubCreateView != null) {
                    Log.d("ClubCreateActivity", "clubCreateView 찾음")
                    
                    // 각 입력 필드를 찾아서 초기화
                    etClubName = clubCreateView.findViewById(R.id.et_club_name)
                    etDepartment = clubCreateView.findViewById(R.id.et_department)
                    spinnerMajorCategory = clubCreateView.findViewById(R.id.spinner_major_category)
                    etMinorCategory = clubCreateView.findViewById(R.id.et_minor_category)
                    etHashtags = clubCreateView.findViewById(R.id.et_hashtags)
                    etLocation = clubCreateView.findViewById(R.id.et_location)
                    etDescription = clubCreateView.findViewById(R.id.et_description)
                    btnRegister = clubCreateView.findViewById(R.id.btn_create_club)
                    
                    // 모든 필드가 제대로 찾아졌는지 확인
                    if (etClubName != null && etDepartment != null && spinnerMajorCategory != null && 
                        etMinorCategory != null && etHashtags != null && etLocation != null && 
                        etDescription != null && btnRegister != null) {
                        Log.d("ClubCreateActivity", "모든 입력 필드 초기화 완료")
                        return true
                    } else {
                        Log.e("ClubCreateActivity", "일부 입력 필드를 찾을 수 없음!")
                        return false
                    }
                } else {
                    Log.e("ClubCreateActivity", "clubCreateView를 찾을 수 없음!")
                    return false
                }
            } else {
                Log.e("ClubCreateActivity", "content_container를 찾을 수 없음!")
                return false
            }
        } catch (e: Exception) {
            Log.e("ClubCreateActivity", "initializeViews에서 오류 발생", e)
            return false
        }
    }

    private fun setupRegisterButton() {
        try {
            if (::btnRegister.isInitialized && btnRegister != null) {
                btnRegister.setOnClickListener {
                    Log.d("ClubCreateActivity", "등록하기 버튼 클릭됨")
                    if (validateInputs()) {
                        createClub()
                    }
                }
                Log.d("ClubCreateActivity", "등록 버튼 리스너 설정 완료")
            } else {
                Log.e("ClubCreateActivity", "btnRegister가 초기화되지 않음!")
            }
        } catch (e: Exception) {
            Log.e("ClubCreateActivity", "setupRegisterButton에서 오류 발생", e)
        }
    }

    private fun validateInputs(): Boolean {
        if (etClubName.text.isNullOrBlank()) {
            Toast.makeText(this, "동아리명을 입력해주세요", Toast.LENGTH_SHORT).show()
            return false
        }
        if (etDepartment.text.isNullOrBlank()) {
            Toast.makeText(this, "학과/부문을 입력해주세요", Toast.LENGTH_SHORT).show()
            return false
        }
        if (etMinorCategory.text.isNullOrBlank()) {
            Toast.makeText(this, "소분류를 입력해주세요", Toast.LENGTH_SHORT).show()
            return false
        }
        if (etHashtags.text.isNullOrBlank()) {
            Toast.makeText(this, "해시태그를 입력해주세요", Toast.LENGTH_SHORT).show()
            return false
        }
        if (etLocation.text.isNullOrBlank()) {
            Toast.makeText(this, "동아리실 위치를 입력해주세요", Toast.LENGTH_SHORT).show()
            return false
        }
        if (etDescription.text.isNullOrBlank()) {
            Toast.makeText(this, "소개를 입력해주세요", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun createClub() {
        Log.d("ClubCreateActivity", "createClub 시작")
        
        val majorCategory = when (spinnerMajorCategory.selectedItemPosition) {
            0 -> "academic"      // 학술
            1 -> "sports"        // 체육
            2 -> "culture"       // 문화예술
            3 -> "volunteer"     // 봉사
            4 -> "entrepreneur"  // 창업
            5 -> "religion"      // 종교
            else -> "academic"
        }

        val clubData = JSONObject().apply {
            put("name", etClubName.text.toString())
            put("department", etDepartment.text.toString())
            put("major_category", majorCategory)
            put("minor_category", etMinorCategory.text.toString())
            put("description", etDescription.text.toString())
            put("hashtags", etHashtags.text.toString())
            put("location", etLocation.text.toString())
            put("short_description", etDescription.text.toString().take(50)) // 소개에서 50자만 추출
        }

        Log.d("ClubCreateActivity", "POST 요청 데이터: ${clubData}")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("ClubCreateActivity", "API 호출 시작: http://13.211.124.186/club")
                
                val url = URL("http://13.211.124.186/club")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val outputStream = connection.outputStream
                val writer = OutputStreamWriter(outputStream)
                writer.write(clubData.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                Log.d("ClubCreateActivity", "응답 코드: $responseCode")
                
                withContext(Dispatchers.Main) {
                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                        Toast.makeText(this@ClubCreateActivity, "동아리가 성공적으로 등록되었습니다!", Toast.LENGTH_LONG).show()
                        Log.d("ClubCreateActivity", "동아리 등록 성공")
                        finish() // 액티비티 종료
                    } else {
                        Toast.makeText(this@ClubCreateActivity, "동아리 등록에 실패했습니다. (응답 코드: $responseCode)", Toast.LENGTH_LONG).show()
                        Log.e("ClubCreateActivity", "동아리 등록 실패 - 응답 코드: $responseCode")
                    }
                }
                
                connection.disconnect()
                
            } catch (e: Exception) {
                Log.e("ClubCreateActivity", "동아리 생성 중 오류 발생", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ClubCreateActivity, "동아리 등록 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
