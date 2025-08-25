package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.myapplication.LedgerListFragment
import com.example.myapplication.ReferenceFragment

class MainActivity : BaseActivity() {

    override fun setupContent(savedInstanceState: Bundle?) {
        // savedInstanceState가 null인 경우에만, 즉 액티비티가 처음 생성될 때만 프래그먼트를 추가합니다.
        // 이렇게 하면 화면 회전 등 상태 변경 시 프래그먼트가 중복으로 생성되는 것을 방지할 수 있습니다.
        if (savedInstanceState == null) {
            replaceFragment(ReferenceFragment())
        }
    }

    fun replaceFragment(fragment: Fragment) {
        // supportFragmentManager를 사용하여 프래그먼트 트랜잭션을 시작합니다.
        val transaction = supportFragmentManager.beginTransaction()
        // content_container ID를 가진 레이아웃을 새로운 프래그먼트로 교체합니다.
        transaction.replace(R.id.content_container, fragment)
        // 트랜잭션을 커밋하여 변경사항을 적용합니다.
        transaction.commit()
    }
}
