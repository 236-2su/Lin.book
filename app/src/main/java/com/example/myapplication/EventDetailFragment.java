package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.myapplication.api.ApiClient;
import com.example.myapplication.model.Ledger;
import com.example.myapplication.model.Transaction;
import com.example.myapplication.TransactionItem; // TransactionItem 임포트 추가
import com.example.myapplication.UserManager; // UserManager 임포트 추가
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EventDetailFragment extends Fragment {
    
    private int clubPk;
    private int ledgerId;
    private int totalBudget = 0;  // 총 예산 저장
    private List<Transaction> allTransactions;  // 전체 거래내역 저장
    private int currentYear;  // 현재 선택된 년도
    private int currentMonth;  // 현재 선택된 월
    private Ledger currentLedger;  // 현재 장부 정보 저장

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // event_detail.xml 레이아웃 사용
        return inflater.inflate(R.layout.event_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 버튼 이벤트 리스너 설정
        setupButtonListeners();
        
        // Bundle에서 ledger ID와 club_pk 가져오기
        if (getArguments() != null) {
            ledgerId = getArguments().getInt("ledger_id", -1);
            clubPk = getArguments().getInt("club_pk", -1);
            
            if (ledgerId != -1 && clubPk != -1) {
                // 장부 상세 정보 로드
                loadLedgerDetail(clubPk, ledgerId);
                // 거래 내역 로드
                loadTransactions(clubPk, ledgerId);
            } else {
                Log.e("EventDetailFragment", "ledger_id 또는 club_pk가 전달되지 않았습니다.");
            }
        }
    }
    
    private void setupButtonListeners() {
        View rootView = getView();
        if (rootView == null) return;
        
        // 이전 월 버튼
        rootView.findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            // 이전 월로 이동
            currentMonth--;
            if (currentMonth < 1) {
                currentMonth = 12;
                currentYear--;
            }
            
            // UI 업데이트
            updateYearMonthUI();
            refreshTransactionDisplay();
            
            Log.d("EventDetailFragment", "이전 월로 이동: " + currentYear + "-" + currentMonth);
        });
        
        // 다음 월 버튼
        rootView.findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            // 다음 월로 이동
            currentMonth++;
            if (currentMonth > 12) {
                currentMonth = 1;
                currentYear++;
            }
            
            // UI 업데이트
            updateYearMonthUI();
            refreshTransactionDisplay();
            
            Log.d("EventDetailFragment", "다음 월로 이동: " + currentYear + "-" + currentMonth);
        });
    }
    
    private void loadLedgerDetail(int clubPk, int ledgerId) {
        ApiClient.getApiService().getLedgerDetail(clubPk, ledgerId).enqueue(new Callback<Ledger>() {
            @Override
            public void onResponse(Call<Ledger> call, Response<Ledger> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Ledger ledger = response.body();
                    Log.d("EventDetailFragment", "장부 상세 조회 성공: " + ledger.getName());
                    
                    // UI 업데이트
                    updateUI(ledger);
                } else {
                    Log.e("EventDetailFragment", "API 응답 오류: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Ledger> call, Throwable t) {
                Log.e("EventDetailFragment", "API 호출 실패: " + t.getMessage());
            }
        });
    }
    
    private void loadTransactions(int clubPk, int ledgerId) {
        // user_pk 가져오기
        Integer userPk = UserManager.INSTANCE.getUserPk(requireContext());
        if (userPk == null) {
            Log.e("EventDetailFragment", "user_pk가 없습니다. 로그인이 필요합니다.");
            return;
        }
        
        ApiClient.getApiService().getTransactions(clubPk, ledgerId, userPk).enqueue(new Callback<List<TransactionItem>>() {
            @Override
            public void onResponse(Call<List<TransactionItem>> call, Response<List<TransactionItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TransactionItem> transactions = response.body();
                    Log.d("EventDetailFragment", "거래내역 조회 성공: " + transactions.size() + "개");
                    
                    // 거래내역 UI 업데이트
                    updateTransactionList(transactions);
                    
                } else {
                    Log.e("EventDetailFragment", "거래내역 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<TransactionItem>> call, Throwable t) {
                Log.e("EventDetailFragment", "API 호출 실패: " + t.getMessage());
            }
        });
    }
    
    private void updateUI(Ledger ledger) {
        View rootView = getView();
        if (rootView == null) return;
        
        // 현재 장부 정보 저장
        this.currentLedger = ledger;
        
        // event_detail.xml의 상단 행사 정보 카드 업데이트
        TextView eventTitle = rootView.findViewById(R.id.tv_event_title);
        TextView eventPeriod = rootView.findViewById(R.id.tv_event_period);
        TextView totalBudgetView = rootView.findViewById(R.id.tv_total_budget);
        TextView currentExpense = rootView.findViewById(R.id.tv_current_expense);
        
        // 상단 행사 정보 카드에 클릭 이벤트 추가
        if (rootView instanceof LinearLayout) {
            LinearLayout rootLayout = (LinearLayout) rootView;
            if (rootLayout.getChildCount() > 0) {
                View firstChild = rootLayout.getChildAt(0);
                if (firstChild instanceof LinearLayout) {
                    firstChild.setOnClickListener(v -> {
                        // EventUpdateFragment로 전환
                        Bundle bundle = new Bundle();
                        bundle.putInt("club_pk", this.clubPk);
                        bundle.putInt("ledger_id", this.ledgerId);
                        bundle.putString("ledger_name", ledger.getName());
                        bundle.putInt("budget", ledger.getAmount());
                        bundle.putString("created_at", ledger.getCreatedAt());
                        
                        EventUpdateFragment fragment = new EventUpdateFragment();
                        fragment.setArguments(bundle);
                        
                        getParentFragmentManager()
                            .beginTransaction()
                            .replace(R.id.content_container, fragment)
                            .addToBackStack(null)
                            .commit();
                        
                        Log.d("EventDetailFragment", "EventUpdateFragment로 전환");
                    });
                }
            }
        }
        
        // 총 예산 저장
        this.totalBudget = ledger.getAmount();
        
        // 장부 정보로 UI 업데이트
        if (eventTitle != null) {
            eventTitle.setText(ledger.getName());
        }
        if (eventPeriod != null) {
            eventPeriod.setText("생성일: " + ledger.getCreatedAt());
        }
        if (totalBudgetView != null) {
            totalBudgetView.setText(formatCurrency(this.totalBudget) + "원");
        }
        // 현재 잔액은 거래내역 로드 후 업데이트됨
        
        Log.d("EventDetailFragment", "UI 업데이트 완료");
    }
    
    private void updateTransactionList(List<TransactionItem> transactions) {
        View rootView = getView();
        if (rootView == null) return;
        
        // 거래내역을 최신순으로 정렬 (역순)
        Collections.reverse(transactions);
        
        // 전체 거래내역 저장
        this.allTransactions = new ArrayList<>(); // TransactionItem을 Transaction으로 변환
        for (TransactionItem item : transactions) {
            Transaction transaction = new Transaction();
            transaction.setId(item.getId());
            transaction.setDate(item.getDateTime());
            transaction.setAmount(item.getAmount());
            transaction.setType(item.getType());
            transaction.setPaymentMethod(item.getPaymentMethod());
            transaction.setVendor(item.getVendor());
            transaction.setDescription(item.getDescription());
            transaction.setLedgerId(item.getLedgerId());
            transaction.setClubPk(item.getClubPk());
            this.allTransactions.add(transaction);
        }
        
        // 가장 최근 거래의 날짜로 초기 연월 설정
        if (!this.allTransactions.isEmpty()) {
            String recentDate = this.allTransactions.get(0).getDate();
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = sdf.parse(recentDate);
                SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
                SimpleDateFormat monthFormat = new SimpleDateFormat("MM", Locale.getDefault());
                this.currentYear = Integer.parseInt(yearFormat.format(date));
                this.currentMonth = Integer.parseInt(monthFormat.format(date));
                
                // 연월 UI 업데이트
                updateYearMonthUI();
            } catch (ParseException e) {
                Log.e("EventDetailFragment", "날짜 파싱 오류: " + e.getMessage());
            }
        }
        
        // 현재 선택된 월의 거래내역만 필터링
        List<Transaction> filteredTransactions = filterTransactionsByMonth();
        
        // 필터링된 거래 내역 합산하여 현재 잔액 계산
        long totalTransactions = 0;
        for (Transaction transaction : this.allTransactions) {  // 전체 거래내역으로 잔액 계산
            totalTransactions += transaction.getAmount();
        }
        long currentBalance = this.totalBudget + totalTransactions;
        
        // 현재 잔액 업데이트
        TextView currentExpenseView = rootView.findViewById(R.id.tv_current_expense);
        if (currentExpenseView != null) {
            currentExpenseView.setText(formatCurrency(currentBalance) + "원");
            // 잔액이 음수면 빨간색, 양수면 파란색
            currentExpenseView.setTextColor(currentBalance < 0 ? 
                Color.parseColor("#FF0000") : Color.parseColor("#0000FF"));
        }
        
        // ScrollView는 레이아웃에서 마지막 child
        // event_detail.xml을 보면 ScrollView가 LinearLayout의 마지막 child
        LinearLayout rootLayout = (LinearLayout) rootView;
        ScrollView scrollView = null;
        
        // ScrollView 찾기 (LinearLayout의 마지막 child)
        for (int i = 0; i < rootLayout.getChildCount(); i++) {
            View child = rootLayout.getChildAt(i);
            if (child instanceof ScrollView) {
                scrollView = (ScrollView) child;
                break;
            }
        }
        
        if (scrollView != null) {
            LinearLayout transactionContainer = (LinearLayout) scrollView.getChildAt(0);
            
            // 기존 하드코딩된 아이템들 모두 제거
            transactionContainer.removeAllViews();
            
            // 필터링된 거래내역 동적 생성
            if (filteredTransactions.isEmpty()) {
                // 해당 월에 거래내역이 없는 경우
                TextView emptyView = new TextView(getContext());
                emptyView.setText("이 달의 거래내역이 없습니다.");
                emptyView.setTextSize(14);
                emptyView.setTextColor(Color.parseColor("#999999"));
                emptyView.setGravity(Gravity.CENTER);
                emptyView.setPadding(0, dpToPx(50), 0, dpToPx(50));
                transactionContainer.addView(emptyView);
            } else {
                for (int i = 0; i < filteredTransactions.size(); i++) {
                    Transaction transaction = filteredTransactions.get(i);
                    View transactionView = createTransactionView(transaction);
                    transactionContainer.addView(transactionView);
                    
                    // 마지막 아이템이 아니면 구분선 추가
                    if (i < filteredTransactions.size() - 1) {
                        View divider = createDivider();
                        transactionContainer.addView(divider);
                    }
                }
            }
        }
        
        Log.d("EventDetailFragment", "현재 잔액: " + currentBalance + "원 (총예산: " + this.totalBudget + ", 거래합계: " + totalTransactions + ")");
    }
    
    private View createTransactionView(Transaction transaction) {
        LinearLayout itemLayout = new LinearLayout(getContext());
        itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setPadding(0, dpToPx(16), 0, dpToPx(16));
        
        // 태그 컨테이너
        LinearLayout tagContainer = new LinearLayout(getContext());
        tagContainer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        tagContainer.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tagContainerParams = (LinearLayout.LayoutParams) tagContainer.getLayoutParams();
        tagContainerParams.setMargins(0, 0, 0, dpToPx(8));
        
        // 지출/수입 태그
        TextView typeTag = createTag();
        boolean isExpense = transaction.getAmount() < 0;
        typeTag.setText(isExpense ? "지출" : "수입");
        if (isExpense) {
            typeTag.setTextColor(Color.WHITE);
            typeTag.setBackgroundResource(R.drawable.btn_delete_background);
        } else {
            typeTag.setTextColor(Color.WHITE);
            typeTag.setBackgroundResource(R.drawable.color_box_blue);
        }
        tagContainer.addView(typeTag);
        
        // 타입 태그
        TextView categoryTag = createTag();
        categoryTag.setText(transaction.getType());
        categoryTag.setTextColor(Color.parseColor("#666666"));
        categoryTag.setBackgroundResource(R.drawable.btn_delete_background);
        tagContainer.addView(categoryTag);
        
        // 결제방법 태그
        TextView paymentTag = createTag();
        paymentTag.setText(transaction.getPaymentMethod());
        paymentTag.setTextColor(Color.parseColor("#333333"));
        paymentTag.setBackgroundResource(R.drawable.btn_delete_background);
        tagContainer.addView(paymentTag);
        
        itemLayout.addView(tagContainer);
        
        // 날짜 TextView
        TextView dateText = new TextView(getContext());
        dateText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        dateText.setText(formatDate(transaction.getDate()));
        dateText.setTextSize(10.7f);
        dateText.setTextColor(Color.parseColor("#666666"));
        LinearLayout.LayoutParams dateParams = (LinearLayout.LayoutParams) dateText.getLayoutParams();
        dateParams.setMargins(0, 0, 0, dpToPx(4));
        itemLayout.addView(dateText);
        
        // 금액 TextView
        TextView amountText = new TextView(getContext());
        amountText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        String amountStr = (isExpense ? "" : "+") + formatCurrency(transaction.getAmount()) + "원";
        amountText.setText(amountStr);
        amountText.setTextSize(15.3f);
        amountText.setTypeface(null, android.graphics.Typeface.BOLD);
        amountText.setTextColor(isExpense ? Color.parseColor("#FF0000") : Color.parseColor("#0000FF"));
        LinearLayout.LayoutParams amountParams = (LinearLayout.LayoutParams) amountText.getLayoutParams();
        amountParams.setMargins(0, 0, 0, dpToPx(8));
        itemLayout.addView(amountText);
        
        // 거래처 TextView
        if (transaction.getVendor() != null && !transaction.getVendor().isEmpty()) {
            TextView vendorText = new TextView(getContext());
            vendorText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            vendorText.setText("• 거래처: " + transaction.getVendor());
            vendorText.setTextSize(12);
            vendorText.setTextColor(Color.parseColor("#888888"));
            LinearLayout.LayoutParams vendorParams = (LinearLayout.LayoutParams) vendorText.getLayoutParams();
            vendorParams.setMargins(0, 0, 0, dpToPx(4));
            itemLayout.addView(vendorText);
        }
        
        // 설명 TextView
        if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
            TextView descText = new TextView(getContext());
            descText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            descText.setText("• 메모: " + transaction.getDescription());
            descText.setTextSize(12);
            descText.setTextColor(Color.parseColor("#888888"));
            itemLayout.addView(descText);
        }
        
        return itemLayout;
    }
    
    private TextView createTag() {
        TextView tag = new TextView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, dpToPx(8), 0);
        tag.setLayoutParams(params);
        tag.setTextSize(12);
        tag.setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4));
        return tag;
    }
    
    private View createDivider() {
        View divider = new View(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(1)
        );
        params.setMargins(0, dpToPx(8), 0, dpToPx(8));
        divider.setLayoutParams(params);
        divider.setBackgroundColor(Color.parseColor("#E0E0E0"));
        return divider;
    }
    
    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            
            // 요일 계산
            SimpleDateFormat dayFormat = new SimpleDateFormat("E요일", Locale.KOREAN);
            String dayOfWeek = dayFormat.format(date);
            
            return outputFormat.format(date) + " " + dayOfWeek;
        } catch (ParseException e) {
            return dateStr;
        }
    }
    
    private String formatCurrency(long amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.KOREA);
        return formatter.format(Math.abs(amount));
    }
    
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    // 월별 필터링 메서드
    private List<Transaction> filterTransactionsByMonth() {
        List<Transaction> filtered = new java.util.ArrayList<>();
        if (allTransactions == null) return filtered;
        
        String targetMonth = String.format(Locale.getDefault(), "%d-%02d", currentYear, currentMonth);
        
        for (Transaction transaction : allTransactions) {
            if (transaction.getDate() != null && transaction.getDate().startsWith(targetMonth)) {
                filtered.add(transaction);
            }
        }
        
        return filtered;
    }
    
    // 연월 UI 업데이트 메서드
    private void updateYearMonthUI() {
        View rootView = getView();
        if (rootView == null) return;
        
        TextView monthView = rootView.findViewById(R.id.tv_current_month);
        
        // 년도 TextView는 ID가 없으므로 직접 찾기
        LinearLayout monthContainer = (LinearLayout) rootView.findViewById(R.id.btn_prev_month).getParent();
        if (monthContainer != null) {
            LinearLayout centerLayout = (LinearLayout) monthContainer.getChildAt(1);
            if (centerLayout != null && centerLayout.getChildCount() >= 2) {
                TextView yearText = (TextView) centerLayout.getChildAt(0);
                if (yearText != null) {
                    yearText.setText(currentYear + "년");
                }
            }
        }
        
        if (monthView != null) {
            monthView.setText(String.format(Locale.getDefault(), "%02d월", currentMonth));
        }
    }
    
    // 거래내역 화면 새로고침
    private void refreshTransactionDisplay() {
        if (allTransactions == null) return;
        
        View rootView = getView();
        if (rootView == null) return;
        
        // 현재 선택된 월의 거래내역만 필터링
        List<Transaction> filteredTransactions = filterTransactionsByMonth();
        
        // ScrollView 찾기
        LinearLayout rootLayout = (LinearLayout) rootView;
        ScrollView scrollView = null;
        
        for (int i = 0; i < rootLayout.getChildCount(); i++) {
            View child = rootLayout.getChildAt(i);
            if (child instanceof ScrollView) {
                scrollView = (ScrollView) child;
                break;
            }
        }
        
        if (scrollView != null) {
            LinearLayout transactionContainer = (LinearLayout) scrollView.getChildAt(0);
            
            // 기존 아이템들 모두 제거
            transactionContainer.removeAllViews();
            
            // 필터링된 거래내역 표시
            if (filteredTransactions.isEmpty()) {
                TextView emptyView = new TextView(getContext());
                emptyView.setText("이 달의 거래내역이 없습니다.");
                emptyView.setTextSize(14);
                emptyView.setTextColor(Color.parseColor("#999999"));
                emptyView.setGravity(Gravity.CENTER);
                emptyView.setPadding(0, dpToPx(50), 0, dpToPx(50));
                transactionContainer.addView(emptyView);
            } else {
                for (int i = 0; i < filteredTransactions.size(); i++) {
                    Transaction transaction = filteredTransactions.get(i);
                    View transactionView = createTransactionView(transaction);
                    transactionContainer.addView(transactionView);
                    
                    if (i < filteredTransactions.size() - 1) {
                        View divider = createDivider();
                        transactionContainer.addView(divider);
                    }
                }
            }
        }
    }
}