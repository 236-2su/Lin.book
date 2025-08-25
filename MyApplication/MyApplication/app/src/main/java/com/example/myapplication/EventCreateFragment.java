package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

public class EventCreateFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // event_create.xml 레이아웃 사용
        return inflater.inflate(R.layout.event_create, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 취소 버튼 클릭 리스너 (뒤로가기)
        View cancelButton = view.findViewById(R.id.btn_cancel);
        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> {
                // 뒤로가기 (자동으로 EventListFragment로 돌아감)
                getParentFragmentManager().popBackStack();
            });
        }
        
        // 생성하기 버튼 클릭 리스너
        View createButton = view.findViewById(R.id.btn_create);
        if (createButton != null) {
            createButton.setOnClickListener(v -> {
                // TODO: 행사 생성 로직 구현
                // 성공 시 뒤로가기
                getParentFragmentManager().popBackStack();
            });
        }
    }
}