package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.LinearLayout;
import android.content.Intent;
import com.example.myapplication.LedgerReportActivity;
import com.example.myapplication.LedgerListActivity;
import android.graphics.Color;

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
    
    // 앱 제목 설정
    protected void setAppTitle(String title) {
        TextView titleText = findViewById(R.id.tv_app_title);
        if (titleText != null) {
            titleText.setText(title);
        }
    }
    
    // 게시판 버튼들 숨김
    protected void hideBoardButtons() {
        View boardButtons = findViewById(R.id.board_buttons_container);
        if (boardButtons != null) {
            boardButtons.setVisibility(View.GONE);
        }
    }

    protected void hideToolbar() {
        View toolbar = findViewById(R.id.toolbar_root);
        if (toolbar != null) {
            toolbar.setVisibility(View.GONE);
        }
    }

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
            btnNotice.setOnClickListener(v -> {
                // 이미 현재 페이지인 경우 아무것도 하지 않음
                if (this instanceof MainActivity) {
                    return;
                }
                updateBoardButton(btnNotice, 
                    new TextView[]{btnFreeBoard, btnPublicAccount, btnMeetingAccount, btnAiReport});
                
                // 공지사항 버튼 클릭 시 MainActivity로 이동
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);

                // 페이지 전환 애니메이션 제거
                overridePendingTransition(0, 0);
            });
        }
        
        if (btnFreeBoard != null) {
            btnFreeBoard.setOnClickListener(v -> updateBoardButton(btnFreeBoard, 
                new TextView[]{btnNotice, btnPublicAccount, btnMeetingAccount, btnAiReport}));
        }
        
        if (btnPublicAccount != null) {
            btnPublicAccount.setOnClickListener(v -> {
                Intent intent = new Intent(this, LedgerListActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0); // 애니메이션 제거
            });
        }
        
        if (btnMeetingAccount != null) {
            btnMeetingAccount.setOnClickListener(v -> updateBoardButton(btnMeetingAccount, 
                new TextView[]{btnNotice, btnFreeBoard, btnPublicAccount, btnAiReport}));
        }
        
        if (btnAiReport != null) {
            btnAiReport.setOnClickListener(v -> {
                // 이미 현재 페이지인 경우 아무것도 하지 않음
                if (this instanceof LedgerReportActivity) {
                    return;
                }

                updateBoardButton(btnAiReport, 
                    new TextView[]{btnNotice, btnFreeBoard, btnPublicAccount, btnMeetingAccount});
                
                // 스크롤 위치 저장
                saveBoardButtonScrollPosition();

                // AI 리포트 버튼 클릭 시 ledger_report로 이동
                Intent intent = new Intent(this, LedgerReportActivity.class);
                
                // 현재 스크롤 위치를 Intent에 추가
                intent.putExtra("scroll_position", getCurrentBoardScrollPosition());
                startActivity(intent);
                
                // 페이지 전환 애니메이션 제거
                overridePendingTransition(0, 0);
            });
        }
    }
    
    // 게시판 버튼 상태를 업데이트하는 메서드
    protected void updateBoardButton(TextView selectedButton, TextView... unselectedButtons) {
        // 선택된 버튼 스타일 적용
        selectedButton.setBackgroundResource(R.drawable.btn_board_selected);
        selectedButton.setTextColor(Color.parseColor("#FFFFFF"));

        // 선택되지 않은 버튼들 스타일 초기화
        for (TextView button : unselectedButtons) {
            if (button != null) {
                button.setBackgroundResource(R.drawable.btn_board_background);
                button.setTextColor(Color.parseColor("#333333"));
            }
        }
    }
    
    // 특정 게시판 버튼을 선택된 상태로 설정하는 메서드
    protected void selectBoardButton(TextView buttonToSelect) {
        if (buttonToSelect != null) {
            // 모든 버튼을 기본 상태로 초기화
            if (btnNotice != null) {
                btnNotice.setBackgroundResource(R.drawable.btn_board_background);
                btnNotice.setTextColor(android.graphics.Color.parseColor("#333333"));
            }
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
            
            // 지정된 버튼을 선택된 상태로 변경
            buttonToSelect.setBackgroundResource(R.drawable.btn_board_selected);
            buttonToSelect.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
        }
    }
    
    // 스크롤 위치를 저장하는 메서드
    protected void saveScrollPosition(String key, int position) {
        android.content.SharedPreferences prefs = getSharedPreferences("scroll_positions", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, position);
        editor.apply();
    }
    
    // 스크롤 위치를 복원하는 메서드
    protected int getScrollPosition(String key) {
        android.content.SharedPreferences prefs = getSharedPreferences("scroll_positions", MODE_PRIVATE);
        return prefs.getInt(key, 0);
    }
    
    // 게시판 버튼 스크롤 위치 저장
    protected void saveBoardButtonScrollPosition() {
        android.widget.HorizontalScrollView boardScrollView = findViewById(R.id.board_buttons_scroll_view);
        if (boardScrollView != null) {
            int scrollX = boardScrollView.getScrollX();
            saveScrollPosition("board_buttons_scroll", scrollX);
        }
    }
    
    // 게시판 버튼 스크롤 위치 복원
    protected void restoreBoardButtonScrollPosition() {
        android.widget.HorizontalScrollView boardScrollView = findViewById(R.id.board_buttons_scroll_view);
        if (boardScrollView != null) {
            int savedPosition = getScrollPosition("board_buttons_scroll");
            boardScrollView.post(() -> boardScrollView.scrollTo(savedPosition, 0));
        }
    }
    
    // 현재 게시판 버튼 스크롤 위치 가져오기
    protected int getCurrentBoardScrollPosition() {
        android.widget.HorizontalScrollView boardScrollView = findViewById(R.id.board_buttons_scroll_view);
        if (boardScrollView != null) {
            return boardScrollView.getScrollX();
        }
        return 0;
    }
    
    // Intent로 전달받은 스크롤 위치로 복원
    protected void restoreBoardButtonScrollPositionFromIntent(int scrollPosition) {
        android.widget.HorizontalScrollView boardScrollView = findViewById(R.id.board_buttons_scroll_view);
        if (boardScrollView != null) {
            boardScrollView.post(() -> boardScrollView.scrollTo(scrollPosition, 0));
        }
    }

    @Override
    public void finish() {
        super.finish();
        // 뒤로가기 시 페이지 전환 애니메이션 제거
        overridePendingTransition(0, 0);
    }
}
