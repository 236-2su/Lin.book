package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.myapplication.LedgerListFragment
import com.example.myapplication.ReferenceFragment

class MainActivity : BaseActivity() {

    private lateinit var buttons: List<View>
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

                   // 카테고리 버튼들을 리스트에 추가 (나중에 초기화)
                   buttons = listOf(
                       contentView.findViewById(R.id.btn_all),
                       contentView.findViewById(R.id.btn_academic),
                       contentView.findViewById(R.id.btn_sports),
                       contentView.findViewById(R.id.btn_volunteer),
                       contentView.findViewById(R.id.btn_art)
                   )

                   // 전체/가입요청 버튼들 설정 (reference.xml에서 삭제됨)
                   // val btnAllNew = contentView.findViewById<TextView>(R.id.btn_all_new)
                   // val btnJoinRequest = contentView.findViewById<TextView>(R.id.btn_join_request)
                   
                   // 초기 상태: 전체 버튼이 선택된 상태로 시작
                   // btnAllNew.setBackgroundResource(R.drawable.btn_all_selected)
                   // btnAllNew.setTextColor(Color.WHITE)
                   
                   // btnAllNew.setOnClickListener {
                   //     updateFilterButton(btnAllNew, btnJoinRequest)
                   // }
                   
                   // btnJoinRequest.setOnClickListener {
                   //     updateFilterButton(btnJoinRequest, btnAllNew)
                   // }

                   // BaseActivity의 게시판 버튼들을 사용
                   // (BaseActivity에서 이미 설정되어 있음)



        // 토글 스위치 설정
        val toggleSwitch = contentView.findViewById<Switch>(R.id.toggle_switch)
        val textOn = contentView.findViewById<TextView>(R.id.text_on)
        val textOff = contentView.findViewById<TextView>(R.id.text_off)

        toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // ON 상태일 때
                textOn.setTextColor(Color.parseColor("#1976D2")) // 파란색
                textOff.setTextColor(Color.parseColor("#999999")) // 회색
            } else {
                // OFF 상태일 때
                textOn.setTextColor(Color.parseColor("#999999")) // 회색
                textOff.setTextColor(Color.parseColor("#1976D2")) // 파란색
            }
        }

        // 카테고리 버튼들 초기 상태 설정 및 클릭 리스너 설정
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

               // private fun updateFilterButton(selectedButton: TextView, unselectedButton: TextView) {
               //     // 선택된 버튼을 파란색으로 변경
               //     if (selectedButton.id == R.id.btn_all_new) {
               //         selectedButton.setBackgroundResource(R.drawable.btn_all_selected)
               //     } else {
               //         selectedButton.setBackgroundResource(R.drawable.btn_join_request_selected)
               //     }
               //     selectedButton.setTextColor(Color.WHITE)
               //     
               //     // 선택되지 않은 버튼을 원래대로 변경
               //     if (unselectedButton.id == R.id.btn_all_new) {
               //         unselectedButton.setBackgroundResource(R.drawable.btn_all_background)
               //     } else {
               //         unselectedButton.setBackgroundResource(R.drawable.btn_join_request_background)
               //     }
               //     unselectedButton.setTextColor(Color.parseColor("#333333"))
               // }

               private fun updateBoardButton(selectedButton: TextView, unselectedButtons: List<TextView>) {
                   // 선택된 버튼을 파란색으로 변경
                   selectedButton.setBackgroundResource(R.drawable.btn_board_selected)
                   selectedButton.setTextColor(Color.WHITE)
                   
                   // 선택되지 않은 모든 버튼을 원래대로 변경
                   unselectedButtons.forEach { btn ->
                       btn.setBackgroundResource(R.drawable.btn_board_background)
                       btn.setTextColor(Color.parseColor("#333333"))
                   }
               }
    

}
