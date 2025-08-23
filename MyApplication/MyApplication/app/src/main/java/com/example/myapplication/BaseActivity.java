package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.LinearLayout;

public abstract class BaseActivity extends AppCompatActivity {
    
    // 공통 패딩 값들
    protected static final int COMMON_PADDING_START = 24;
    protected static final int COMMON_PADDING_END = 24;
    protected static final int COMMON_PADDING_TOP = 16;
    protected static final int COMMON_PADDING_BOTTOM = 16;
    protected static final int COMMON_MARGIN_TOP = 32;
    
    // 뒤로가기 버튼 참조
    protected TextView backButton;
    
    // 게시판 버튼들 참조
    protected TextView btnNotice, btnFreeBoard, btnPublicAccount, btnMeetingAccount, btnAiReport;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.root_page);
        setupHeader();
        setupContent();
    }
    
    // 상단 헤더 설정 (로고, 제목 등)
    private void setupHeader() {
        // 로고 설정
        ImageView logoImage = findViewById(R.id.iv_logo);
        logoImage.setImageResource(R.drawable.logo);
        
        // 뒤로가기 버튼 설정
        backButton = findViewById(R.id.btn_back);
        setupBackButton();
        
        // 기본적으로 뒤로가기 버튼 숨김
        hideBackButton();
        
        // 게시판 버튼들 설정
        setupBoardButtons();
        
//        // 앱 제목 설정
//        TextView titleText = findViewById(R.id.tv_app_title);
//        TextView titleText.setText(R.string.app_title);
    }
    
    // 뒤로가기 버튼 기본 설정
    private void setupBackButton() {
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }
    
    // 뒤로가기 버튼 표시
    protected void showBackButton() {
        if (backButton != null) {
            backButton.setVisibility(View.VISIBLE);
        }
    }
    
    // 뒤로가기 버튼 숨김
    protected void hideBackButton() {
        if (backButton != null) {
            backButton.setVisibility(View.GONE);
        }
    }
    
    // 각 페이지의 내용을 설정하는 추상 메서드
    protected abstract void setupContent();
    
    // 공통 여백 설정 헬퍼 메서드
    protected void applyCommonMargins(View view) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = COMMON_MARGIN_TOP;
        view.setLayoutParams(params);
    }
    
    // 공통 기능들 (예: 뒤로가기, 메뉴 등)
    protected void setupCommonFeatures() {
        // 공통으로 사용할 기능들
    }
    
    // 게시판 버튼들 설정
    private void setupBoardButtons() {
        // 게시판 버튼들 찾기
        btnNotice = findViewById(R.id.btn_notice);
        btnFreeBoard = findViewById(R.id.btn_free_board);
        btnPublicAccount = findViewById(R.id.btn_public_account);
        btnMeetingAccount = findViewById(R.id.btn_meeting_account);
        btnAiReport = findViewById(R.id.btn_ai_report);
        
        // 초기 상태: 공지사항 버튼이 선택된 상태로 시작
        if (btnNotice != null) {
            btnNotice.setBackgroundResource(R.drawable.btn_board_selected);
            btnNotice.setTextColor(getResources().getColor(android.R.color.white));
        }
        
        // 나머지 버튼들은 기본 상태로 설정
        if (btnFreeBoard != null) {
            btnFreeBoard.setBackgroundResource(R.drawable.btn_board_background);
            btnFreeBoard.setTextColor(android.graphics.Color.parseColor("#333333"));
        }
        if (btnPublicAccount != null) {
            btnPublicAccount.setBackgroundResource(R.drawable.btn_board_background);
            btnPublicAccount.setTextColor(android.graphics.Color.parseColor("#333333"));
        }
        if (btnMeetingAccount != null) {
            btnMeetingAccount.setBackgroundResource(R.drawable.btn_board_background);
            btnMeetingAccount.setTextColor(android.graphics.Color.parseColor("#333333"));
        }
        if (btnAiReport != null) {
            btnAiReport.setBackgroundResource(R.drawable.btn_board_background);
            btnAiReport.setTextColor(android.graphics.Color.parseColor("#333333"));
        }
        
        // 게시판 버튼 클릭 리스너 설정
        setupBoardButtonListeners();
    }
    
    // 게시판 버튼 클릭 리스너 설정
    private void setupBoardButtonListeners() {
        if (btnNotice != null) {
            btnNotice.setOnClickListener(v -> updateBoardButton(btnNotice, 
                new TextView[]{btnFreeBoard, btnPublicAccount, btnMeetingAccount, btnAiReport}));
        }
        
        if (btnFreeBoard != null) {
            btnFreeBoard.setOnClickListener(v -> updateBoardButton(btnFreeBoard, 
                new TextView[]{btnNotice, btnPublicAccount, btnMeetingAccount, btnAiReport}));
        }
        
        if (btnPublicAccount != null) {
            btnPublicAccount.setOnClickListener(v -> updateBoardButton(btnPublicAccount, 
                new TextView[]{btnNotice, btnFreeBoard, btnMeetingAccount, btnAiReport}));
        }
        
        if (btnMeetingAccount != null) {
            btnMeetingAccount.setOnClickListener(v -> updateBoardButton(btnMeetingAccount, 
                new TextView[]{btnNotice, btnFreeBoard, btnPublicAccount, btnAiReport}));
        }
        
        if (btnAiReport != null) {
            btnAiReport.setOnClickListener(v -> updateBoardButton(btnAiReport, 
                new TextView[]{btnNotice, btnFreeBoard, btnPublicAccount, btnMeetingAccount}));
        }
    }
    
    // 게시판 버튼 상태 업데이트
    private void updateBoardButton(TextView selectedButton, TextView[] unselectedButtons) {
        // 선택된 버튼을 파란색으로 변경
        selectedButton.setBackgroundResource(R.drawable.btn_board_selected);
        selectedButton.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
        
        // 선택되지 않은 모든 버튼을 원래대로 변경
        for (TextView btn : unselectedButtons) {
            if (btn != null) {
                btn.setBackgroundResource(R.drawable.btn_board_background);
                btn.setTextColor(android.graphics.Color.parseColor("#333333"));
            }
        }
    }
}
