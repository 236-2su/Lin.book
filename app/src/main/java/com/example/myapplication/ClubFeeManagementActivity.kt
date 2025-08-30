package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ClubFeeManagementActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CLUB_PK = "club_pk"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activitiy_club_fee_management)

        findViewById<Button>(R.id.btn_back)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 임시 타이틀 표기 (추후 동아리명으로 대체 가능)
        findViewById<TextView>(R.id.tv_club_title)?.text = "회비 관리"

        findViewById<android.view.View>(R.id.btn_fee_batch_billing)?.setOnClickListener {
            val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val i = android.content.Intent(this, ClubFeeBatchBillingActivity::class.java)
            i.putExtra(ClubFeeManagementActivity.EXTRA_CLUB_PK, clubPk)
            startActivity(i)
        }
    }
}


