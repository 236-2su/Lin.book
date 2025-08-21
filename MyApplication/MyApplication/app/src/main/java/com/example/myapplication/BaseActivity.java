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
    protected Button backButton;
    
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
}
