package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.example.myapplication.model.Ledger;

public class EventUpdateFragment extends Fragment {
    
    private Ledger currentLedger;
    private int clubPk;
    private int ledgerId;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.event_update, container, false);
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Bundle에서 데이터 가져오기
        if (getArguments() != null) {
            clubPk = getArguments().getInt("club_pk", -1);
            ledgerId = getArguments().getInt("ledger_id", -1);
            String ledgerName = getArguments().getString("ledger_name", "");
            int budget = getArguments().getInt("budget", 0);
            String createdAt = getArguments().getString("created_at", "");
            
            // 필드에 기존 값 설정
            EditText etEventName = view.findViewById(R.id.et_event_name);
            EditText etBudget = view.findViewById(R.id.et_budget);
            EditText etStartDate = view.findViewById(R.id.et_start_date);
            EditText etEndDate = view.findViewById(R.id.et_end_date);
            
            if (etEventName != null) {
                etEventName.setText(ledgerName);
            }
            if (etBudget != null) {
                etBudget.setText(String.valueOf(budget));
            }
            if (etStartDate != null && !createdAt.isEmpty()) {
                etStartDate.setText(createdAt);
            }
            
            Log.d("EventUpdateFragment", "장부 정보 로드: " + ledgerName);
        }
        
        // 취소 버튼
        TextView btnCancel = view.findViewById(R.id.btn_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                // 이전 화면으로 돌아가기
                getParentFragmentManager().popBackStack();
            });
        }
        
        // 수정하기 버튼
        TextView btnCreate = view.findViewById(R.id.btn_create);
        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> {
                updateLedger();
            });
        }
    }
    
    private void updateLedger() {
        View view = getView();
        if (view == null) return;
        
        EditText etEventName = view.findViewById(R.id.et_event_name);
        EditText etBudget = view.findViewById(R.id.et_budget);
        EditText etMemo = view.findViewById(R.id.et_memo);
        
        String eventName = etEventName != null ? etEventName.getText().toString().trim() : "";
        String budgetStr = etBudget != null ? etBudget.getText().toString().trim() : "";
        String memo = etMemo != null ? etMemo.getText().toString().trim() : "";
        
        // 유효성 검사
        if (eventName.isEmpty()) {
            Toast.makeText(getContext(), "행사명을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (budgetStr.isEmpty()) {
            Toast.makeText(getContext(), "예산 금액을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int budget = Integer.parseInt(budgetStr);
            
            // TODO: API 호출하여 장부 정보 업데이트
            // PUT /club/{club_pk}/ledger/{ledger_id}/
            
            Toast.makeText(getContext(), "장부 정보가 수정되었습니다.", Toast.LENGTH_SHORT).show();
            
            // 이전 화면으로 돌아가기
            getParentFragmentManager().popBackStack();
            
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "올바른 예산 금액을 입력해주세요.", Toast.LENGTH_SHORT).show();
        }
    }
}