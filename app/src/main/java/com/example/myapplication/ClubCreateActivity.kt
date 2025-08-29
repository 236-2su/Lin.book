package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import java.io.InputStream
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ClubCreateActivity : BaseActivity() {

    private lateinit var etClubName: EditText
    private lateinit var etDepartment: EditText
    private lateinit var spinnerMajorCategory: Spinner
    private lateinit var etMinorCategory: EditText
    private lateinit var etHashtags: EditText
    private lateinit var etLocation: EditText
    private lateinit var etDescription: EditText
    private lateinit var etWelcomeImage: EditText
    private lateinit var etWelcomeIntro: EditText
    private lateinit var btnRegister: Button
    private var pickedImageBase64: String? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    pickedImageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    Toast.makeText(this, "이미지 첨부 완료", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "이미지를 읽을 수 없습니다", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ClubCreateActivity", "이미지 인코딩 실패", e)
                Toast.makeText(this, "이미지 처리 중 오류", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                    // 파일 첨부 버튼
                    val pickWelcomeImageBtn: android.widget.TextView = clubCreateView.findViewById(R.id.btn_pick_welcome_image)
                    pickWelcomeImageBtn.setOnClickListener {
                        pickImageLauncher.launch("image/*")
                    }
                    etWelcomeIntro = clubCreateView.findViewById(R.id.et_welcome_intro_text)
                    btnRegister = clubCreateView.findViewById(R.id.btn_create_club)
                    
                    // 모든 필드가 제대로 찾아졌는지 확인
                    if (etClubName != null && etDepartment != null && spinnerMajorCategory != null && 
                        etMinorCategory != null && etHashtags != null && etLocation != null && 
                        etDescription != null && etWelcomeIntro != null && btnRegister != null) {
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
            0 -> "academic"
            1 -> "sports"
            2 -> "culture"
            3 -> "volunteer"
            4 -> "entrepreneur"
            5 -> "religion"
            else -> "academic"
        }

        val welcomeIntro = etWelcomeIntro.text?.toString()?.trim().orEmpty()

        val service = ApiClient.getApiService()

        // admin(user pk) 확보
        val adminPk = UserManager.getUserPk(this)
        if (adminPk == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_LONG).show()
            return
        }

        // 텍스트 필드들을 RequestBody로 변환
        fun textBody(text: String): RequestBody = text.toRequestBody("text/plain".toMediaTypeOrNull())

        val nameBody = textBody(etClubName.text.toString())
        val deptBody = textBody(etDepartment.text.toString())
        val majorBody = textBody(majorCategory)
        val minorBody = textBody(etMinorCategory.text.toString())
        val descBody = textBody(etDescription.text.toString())
        val hashtagsBody = textBody(etHashtags.text.toString())
        val locationBody = textBody(etLocation.text.toString())
        val shortDescBody = textBody(if (welcomeIntro.isNotBlank()) welcomeIntro else etDescription.text.toString().take(50))
        val adminBody = textBody(adminPk.toString())

        // 이미지 파트 구성: 사용자가 업로드하지 않으면 기본 이미지 사용
        val imageBytes: ByteArray? = try {
            if (!pickedImageBase64.isNullOrBlank()) {
                Base64.decode(pickedImageBase64, Base64.NO_WRAP)
            } else {
                val ins = resources.openRawResource(R.drawable.default_club_welcome_img)
                val bytes = ins.readBytes()
                ins.close()
                bytes
            }
        } catch (e: Exception) {
            Log.w("ClubCreateActivity", "이미지 준비 중 오류. 이미지 없이 전송합니다.", e)
            null
        }

        val imagePart: MultipartBody.Part? = imageBytes?.let { bytes ->
            val mediaType = "image/jpeg".toMediaTypeOrNull()
            val body = bytes.toRequestBody(mediaType)
            MultipartBody.Part.createFormData("image", "welcome.jpg", body)
        }

        Log.d("ClubCreateActivity", "멀티파트로 동아리 생성 요청 전송 (admin 포함)")

        service.createClubMultipart(
            name = nameBody,
            department = deptBody,
            majorCategory = majorBody,
            minorCategory = minorBody,
            description = descBody,
            hashtags = hashtagsBody,
            location = locationBody,
            shortDescription = shortDescBody,
            admin = adminBody,
            image = imagePart
        ).enqueue(object : Callback<com.example.myapplication.ClubItem> {
            override fun onResponse(
                call: Call<com.example.myapplication.ClubItem>,
                response: Response<com.example.myapplication.ClubItem>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ClubCreateActivity, "동아리가 성공적으로 등록되었습니다!", Toast.LENGTH_LONG).show()
                    Log.d("ClubCreateActivity", "동아리 등록 성공: ${response.body()}")
                    finish()
                } else {
                    val code = response.code()
                    val err = try { response.errorBody()?.string() } catch (_: Exception) { null }
                    Log.e("ClubCreateActivity", "동아리 등록 실패: code=$code, body=$err")
                    Toast.makeText(this@ClubCreateActivity, "동아리 등록 실패 ($code)", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<com.example.myapplication.ClubItem>, t: Throwable) {
                Log.e("ClubCreateActivity", "동아리 등록 네트워크 오류", t)
                Toast.makeText(this@ClubCreateActivity, "동아리 등록 중 오류: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
