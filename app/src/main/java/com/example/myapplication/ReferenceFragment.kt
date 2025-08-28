package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment

class ReferenceFragment : Fragment() {

    private lateinit var buttons: List<View>
    private lateinit var contentView: View

    override fun onResume() {
        super.onResume()
        // 이 프래그먼트가 화면에 나타날 때 게시판 버튼들을 표시합니다.
        (activity as? BaseActivity)?.showBoardButtons()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 이 프래그먼트의 레이아웃을 인플레이트합니다.
        contentView = inflater.inflate(R.layout.reference, container, false)
        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 뷰가 생성된 후 UI 로직을 설정합니다.
        setupReferenceLogic()
        
        // 동아리 생성 버튼 숨기기
        hideLocalRegisterButton()
    }

    private fun setupReferenceLogic() {
        // 카테고리 버튼들
        buttons = listOf(
            contentView.findViewById(R.id.btn_all),
            contentView.findViewById(R.id.btn_academic),
            contentView.findViewById(R.id.btn_sports),
            contentView.findViewById(R.id.btn_volunteer),
            contentView.findViewById(R.id.btn_art)
        )

        // 토글 스위치
        val toggleSwitch = contentView.findViewById<Switch>(R.id.toggle_switch)
        val textOn = contentView.findViewById<TextView>(R.id.text_on)
        val textOff = contentView.findViewById<TextView>(R.id.text_off)

        toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                textOn.setTextColor(Color.parseColor("#1976D2")) // 파란색
                textOff.setTextColor(Color.parseColor("#999999")) // 회색
            } else {
                textOn.setTextColor(Color.parseColor("#999999")) // 회색
                textOff.setTextColor(Color.parseColor("#1976D2")) // 파란색
            }
        }
        
        // 카테고리 버튼들의 초기 상태 및 클릭 리스너 설정
        buttons.forEach { btn ->
            if (btn.id == R.id.btn_all) {
                btn.setBackgroundResource(R.drawable.btn_selected)
                if (btn is TextView) {
                    btn.setTextColor(Color.WHITE)
                }
            } else {
                btn.setBackgroundResource(R.drawable.btn_unselected)
                if (btn is TextView) {
                    btn.setTextColor(Color.parseColor("#333333"))
                }
            }
            
            btn.setOnClickListener {
                updateSelectedButton(btn)
            }
        }

        // "동아리 목록" 버튼 클릭 리스너
        val btnNextPage = contentView.findViewById<Button>(R.id.btn_next_page)
        btnNextPage.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(ClubListFragment())
        }
    }
    
    // 동아리 생성 버튼 숨기기
    private fun hideLocalRegisterButton() {
        try {
            // ReferenceFragment 내부의 동아리 생성 버튼 숨기기
            val registerButton = contentView.findViewById<View>(R.id.btn_register)
            if (registerButton != null) {
                registerButton.visibility = View.GONE
                android.util.Log.d("ReferenceFragment", "동아리 생성 버튼 숨김 완료")
            }
            
            // BaseActivity의 전역 동아리 생성 버튼 숨김 메서드도 호출
            val baseActivity = activity as? BaseActivity
            baseActivity?.hideAllRegisterButtonsGlobally()
            
        } catch (e: Exception) {
            android.util.Log.e("ReferenceFragment", "동아리 생성 버튼 숨기기 오류", e)
        }
    }

    private fun updateSelectedButton(selectedButton: View) {
        buttons.forEach { btn ->
            if (btn == selectedButton) {
                btn.setBackgroundResource(R.drawable.btn_selected)
                if (btn is TextView) {
                    btn.setTextColor(Color.WHITE)
                } else if (btn is Button) {
                    btn.setTextColor(Color.WHITE)
                }
            } else {
                btn.setBackgroundResource(R.drawable.btn_unselected)
                if (btn is TextView) {
                    btn.setTextColor(Color.parseColor("#111111"))
                } else if (btn is Button) {
                    btn.setTextColor(Color.parseColor("#111111"))
                }
            }
        }
    }
}
