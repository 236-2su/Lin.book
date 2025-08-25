package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.myapplication.api.ApiClient;
import com.example.myapplication.model.Ledger;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EventListFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // event_list.xml 레이아웃 사용
        return inflater.inflate(R.layout.event_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 행사 장부 생성 버튼 클릭 리스너
        View createEventButton = view.findViewById(R.id.btn_create_event);
        if (createEventButton != null) {
            createEventButton.setOnClickListener(v -> {
                // EventCreateFragment로 전환
                getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_container, new EventCreateFragment())
                    .addToBackStack(null)
                    .commit();
            });
        }
        
        // API 호출하여 장부 목록 가져오기
        loadLedgerList();
    }
    
    private void loadLedgerList() {
        // SharedPreferences에서 club_pk 가져오기 (임시로 2 사용)
        int clubPk = 4; // TODO: SharedPreferences에서 실제 club_pk 가져오기
        
        
        ApiClient.getApiService().getLedgerList(clubPk).enqueue(new Callback<List<Ledger>>() {
            @Override
            public void onResponse(Call<List<Ledger>> call, Response<List<Ledger>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Ledger> ledgers = response.body();
                    Log.d("EventListFragment", "API 성공: " + ledgers.size() + "개 장부");
                    
                    // UI 업데이트 (기존 하드코딩된 아이템들을 API 데이터로 교체)
                    updateLedgerItems(ledgers);
                } else {
                    Log.e("EventListFragment", "API 응답 오류: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Ledger>> call, Throwable t) {
                Log.e("EventListFragment", "API 호출 실패: " + t.getMessage());
            }
        });
    }
    
    private void updateLedgerItems(List<Ledger> ledgers) {
        View rootView = getView();
        if (rootView == null) return;
        
        // 기존 하드코딩된 아이템들 숨기기
        int[] existingItemIds = {
            R.id.event_item_1, R.id.event_item_2, R.id.event_item_3,
            R.id.event_item_4, R.id.event_item_5, R.id.event_item_6,
            R.id.event_item_7
        };
        
        for (int itemId : existingItemIds) {
            View item = rootView.findViewById(itemId);
            if (item != null) {
                item.setVisibility(View.GONE);
            }
        }
        
        // API 데이터를 표시할 컨테이너 찾기 (event_list.xml의 메인 LinearLayout)
        LinearLayout mainContainer = (LinearLayout) rootView;
        
        // API 데이터로 동적 아이템 생성
        for (Ledger ledger : ledgers) {
            View ledgerItem = createLedgerItemView(ledger);
            mainContainer.addView(ledgerItem);
        }
    }
    
    private View createLedgerItemView(Ledger ledger) {
        // 동적으로 장부 아이템 생성 (기존 아이템과 동일한 스타일)
        LinearLayout itemLayout = new LinearLayout(getContext());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, dpToPx(6), 0, dpToPx(6));
        itemLayout.setLayoutParams(layoutParams);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        itemLayout.setBackgroundResource(R.drawable.color_box_blue); // 기본 파란색 배경
        itemLayout.setClickable(true);
        itemLayout.setFocusable(true);
        
        // 장부 이름 TextView
        TextView nameTextView = new TextView(getContext());
        nameTextView.setText(ledger.getName());
        nameTextView.setTextSize(14);
        nameTextView.setTextColor(getResources().getColor(R.color.black));
        nameTextView.setPadding(0, 0, 0, dpToPx(3));
        itemLayout.addView(nameTextView);
        
        // 생성일 TextView
        TextView dateTextView = new TextView(getContext());
        dateTextView.setText("생성일: " + ledger.getCreatedAt());
        dateTextView.setTextSize(12);
        dateTextView.setTextColor(getResources().getColor(R.color.black));
        itemLayout.addView(dateTextView);
        
        // 클릭 리스너 설정
        itemLayout.setOnClickListener(v -> {
            // EventDetailFragment로 전환하면서 ledger ID 전달
            Bundle bundle = new Bundle();
            bundle.putInt("ledger_id", ledger.getId());
            bundle.putInt("club_pk", ledger.getClub());
            
            EventDetailFragment fragment = new EventDetailFragment();
            fragment.setArguments(bundle);
            
            getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.content_container, fragment)
                .addToBackStack(null)
                .commit();
        });
        
        return itemLayout;
    }
    
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}