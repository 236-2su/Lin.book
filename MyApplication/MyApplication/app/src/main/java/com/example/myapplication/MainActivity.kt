package com.example.myapplication

import android.content.Intent
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.graphics.Color
import android.view.View

class MainActivity : BaseActivity() {

    private lateinit var buttons: List<Button>
    private lateinit var contentView: View

    override fun setupContent() {
        // MainActivity 내용을 content_container에 추가
        val contentContainer = findViewById<android.widget.FrameLayout>(R.id.content_container)
        contentView = layoutInflater.inflate(R.layout.reference, null)
        contentContainer.addView(contentView)
        
        // MainActivity 로직 실행
        setupMainActivityLogic()
    }
    
    private fun setupMainActivityLogic() {

        // 모든 버튼들을 리스트에 추가
        buttons = listOf(
            contentView.findViewById(R.id.btn_all),
            contentView.findViewById(R.id.btn_academic),
            contentView.findViewById(R.id.btn_sports),
            contentView.findViewById(R.id.btn_volunteer),
            contentView.findViewById(R.id.btn_art)
        )

        // 온/오프 토글 스위치 이벤트
        val toggleSwitch = contentView.findViewById<Switch>(R.id.toggle_switch)
        val textOff = contentView.findViewById<TextView>(R.id.text_off)
        val textOn = contentView.findViewById<TextView>(R.id.text_on)
        
        // 초기 상태 설정 (OFF)
        updateToggleLabels(false, textOff, textOn)
        
        toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateToggleLabels(isChecked, textOff, textOn)
            if (isChecked) {
                android.widget.Toast.makeText(this, "토글이 ON 되었습니다!", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(this, "토글이 OFF 되었습니다!", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        // 각 버튼에 클릭 리스너 설정
        buttons.forEach { btn ->
            btn.setOnClickListener {
                updateSelectedButton(btn)
            }
        }

        // 넘어가기 버튼 클릭 이벤트
        val btnNavigate = contentView.findViewById<Button>(R.id.btn_navigate)
        btnNavigate.setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateSelectedButton(selectedButton: Button) {
        buttons.forEach { btn ->
            if (btn == selectedButton) {
                btn.setBackgroundResource(R.drawable.btn_selected)
                btn.setTextColor(Color.WHITE)
            } else {
                btn.setBackgroundResource(R.drawable.btn_unselected)
                btn.setTextColor(Color.parseColor("#111111"))
            }
        }
    }
    
    private fun updateToggleLabels(isChecked: Boolean, textOff: TextView, textOn: TextView) {
        if (isChecked) {
            // ON 상태
            textOff.setTextColor(Color.parseColor("#CCCCCC"))
            textOn.setTextColor(Color.parseColor("#1976D2"))
        } else {
            // OFF 상태
            textOff.setTextColor(Color.parseColor("#1976D2"))
            textOn.setTextColor(Color.parseColor("#CCCCCC"))
        }
    }
}
