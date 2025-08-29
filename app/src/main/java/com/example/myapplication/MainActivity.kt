package com.example.myapplication

import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.myapplication.ClubForumBoardListFragment

class MainActivity : BaseActivity() {

    override fun setupContent(savedInstanceState: Bundle?) {
        // MainActivity 내용을 content_container에 추가
        val contentContainer = findViewById<android.widget.FrameLayout>(R.id.content_container)

        // 로그인에서 넘어온 경우 ClubListFragment 표시
        val showClubList = intent?.getBooleanExtra("show_club_list", false) == true
        if (showClubList) {
            replaceFragment(ClubListFragment())
            return
        }

        // 동아리 내에서 공개장부 버튼을 클릭한 경우 LedgerContentFragment 표시
        val showPublicLedger = intent?.getBooleanExtra("show_public_ledger", false) == true
        if (showPublicLedger) {
            val clubPk = intent?.getIntExtra("club_pk", -1) ?: -1
            val ledgerPk = intent?.getIntExtra("ledger_pk", 10) ?: 10 // ledger_pk도 Intent에서 받아옴
            if (clubPk > 0) {
                replaceFragment(LedgerContentFragment.newInstance(clubPk, ledgerPk))
                return
            }
        }
        
        // 동아리 내에서 모임통장 버튼을 클릭한 경우 MeetingAccountFragment 표시
        val showMeetingAccount = intent?.getBooleanExtra("show_meeting_account", false) == true
        if (showMeetingAccount) {
            val clubPk = intent?.getIntExtra("club_pk", -1) ?: -1
            if (clubPk > 0) {
                replaceFragment(MeetingAccountFragment.newInstance(clubPk))
                return
            }
        }
        
        // 동아리 내에서 자유게시판 버튼을 클릭한 경우 ClubForumBoardListFragment 표시
        val showFreeBoard = intent?.getBooleanExtra("show_free_board", false) == true
        if (showFreeBoard) {
            val clubPk = intent?.getIntExtra("club_pk", -1) ?: -1
            if (clubPk > 0) {
                replaceFragment(ClubForumBoardListFragment.newInstance(clubPk))
                return
            }
        }

        // 초기 상태에서는 ReferenceFragment를 표시 (reference.xml을 직접 추가하지 않음)
        replaceFragment(ReferenceFragment())
    }
    
    override fun onResume() {
        super.onResume()
        
        // MainActivity 로직 실행
        setupMainActivityLogic()
    }
    
    private fun setupMainActivityLogic() {
        // 현재 표시된 ReferenceFragment에서 카테고리 버튼들을 찾아서 설정
        val currentFragment = supportFragmentManager.findFragmentById(R.id.content_container)
        if (currentFragment is ReferenceFragment) {
            setupCategoryButtons(currentFragment)
        }
    }
    
    private fun setupCategoryButtons(referenceFragment: ReferenceFragment) {
        // ReferenceFragment에서 카테고리 버튼들을 찾아서 설정
        val btnAll = referenceFragment.view?.findViewById<TextView>(R.id.btn_all)
        val btnAcademic = referenceFragment.view?.findViewById<TextView>(R.id.btn_academic)
        val btnSports = referenceFragment.view?.findViewById<TextView>(R.id.btn_sports)
        val btnVolunteer = referenceFragment.view?.findViewById<TextView>(R.id.btn_volunteer)
        val btnArt = referenceFragment.view?.findViewById<TextView>(R.id.btn_art)
        
        val buttons = listOfNotNull(btnAll, btnAcademic, btnSports, btnVolunteer, btnArt)
        
        // 카테고리 버튼들 초기 상태 설정 및 클릭 리스너 설정
        buttons.forEach { btn ->
            if (btn.id == R.id.btn_all) {
                btn.setBackgroundResource(R.drawable.btn_selected)
                btn.setTextColor(Color.parseColor("#FFFFFF"))
            } else {
                btn.setBackgroundResource(R.drawable.btn_unselected)
                btn.setTextColor(Color.parseColor("#333333"))
            }
            
            btn.setOnClickListener {
                updateSelectedButton(btn, buttons)
            }
        }
        
        // 토글 스위치 설정
        val toggleSwitch = referenceFragment.view?.findViewById<Switch>(R.id.toggle_switch)
        val textOn = referenceFragment.view?.findViewById<TextView>(R.id.text_on)
        val textOff = referenceFragment.view?.findViewById<TextView>(R.id.text_off)
        
        toggleSwitch?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // ON 상태일 때
                textOn?.setTextColor(Color.parseColor("#1976D2")) // 파란색
                textOff?.setTextColor(Color.parseColor("#999999")) // 회색
            } else {
                // OFF 상태일 때
                textOn?.setTextColor(Color.parseColor("#999999")) // 회색
                textOff?.setTextColor(Color.parseColor("#1976D2")) // 파란색
            }
        }
    }
    
    private fun updateSelectedButton(selectedButton: TextView, allButtons: List<TextView>) {
        allButtons.forEach { btn ->
            if (btn == selectedButton) {
                btn.setBackgroundResource(R.drawable.btn_selected)
                btn.setTextColor(Color.parseColor("#FFFFFF"))
            } else {
                btn.setBackgroundResource(R.drawable.btn_unselected)
                btn.setTextColor(Color.parseColor("#111111"))
            }
        }
    }



    override fun replaceFragment(fragment: Fragment) {
        // supportFragmentManager를 사용하여 프래그먼트 트랜잭션을 시작합니다.
        val transaction = supportFragmentManager.beginTransaction()
        
        // 현재 프래그먼트를 백스택에 추가 (뒤로가기 지원)
        transaction.addToBackStack(null)
        
        // 프래그먼트 교체
        transaction.replace(R.id.content_container, fragment)
        transaction.commit()
    }
    
    // 뒤로가기 처리
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            // 백스택에 프래그먼트가 2개 이상 있으면 뒤로가기
            supportFragmentManager.popBackStack()
        } else {
            // 백스택에 프래그먼트가 1개 이하면 앱 종료
            super.onBackPressed()
        }
    }

}
