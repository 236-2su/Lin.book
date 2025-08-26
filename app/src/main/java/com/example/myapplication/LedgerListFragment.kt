package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.FrameLayout
import com.example.myapplication.LedgerContentFragment

class LedgerListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var ledgerAdapter: LedgerAdapter

    override fun onResume() {
        super.onResume()
        // 이 프래그먼트가 화면에 나타날 때 게시판 버튼들을 표시합니다.
        (activity as? BaseActivity)?.showBoardButtons()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 프래그먼트의 UI로 RecyclerView를 생성하여 반환합니다.
        recyclerView = RecyclerView(requireContext()).apply {
            // LayoutParams를 설정하여 상단 마진을 추가합니다.
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                topMargin = (16 * resources.displayMetrics.density).toInt() // 16dp를 px로 변환
            }
            layoutParams = params
            
            // 좌우 패딩을 0으로 설정합니다.
            setPadding(0, 0, 0, 0)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        return recyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 어댑터를 빈 리스트로 초기화합니다.
        ledgerAdapter = LedgerAdapter(mutableListOf())
        recyclerView.adapter = ledgerAdapter
        
        // 어댑터에 클릭 리스너 설정
        ledgerAdapter.setOnItemClickListener(object : LedgerAdapter.OnItemClickListener {
            override fun onItemClick(ledger: LedgerApiItem) {
                // 클릭된 장부의 ID를 담아 LedgerContentFragment로 교체합니다.
                // clubId는 4로 고정, ledgerId는 클릭된 아이템의 id를 사용합니다.
                val fragment = LedgerContentFragment.newInstance(4, ledger.id)
                (activity as? MainActivity)?.replaceFragment(fragment)
            }
        })
        
        // 동아리 ID를 기반으로 장부 데이터를 가져옵니다. (현재는 4로 고정)
        fetchLedgerData(4)
    }
    
    private fun fetchLedgerData(clubId: Int) {
        Log.d("LedgerListFragment", "Fetching ledger data for club ID: $clubId")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ApiClient.getApiService()
                val response = apiService.getLedgerList(clubId).execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val ledgers = response.body()
                        if (ledgers != null && ledgers.isNotEmpty()) {
                            Log.d("LedgerListFragment", "Data fetched successfully: ${ledgers.size} items")
                            // 각 장부의 정보를 로그로 출력
                            ledgers.forEach { ledger ->
                                Log.d("LedgerListFragment", "Ledger ID: ${ledger.id}, Name: ${ledger.name}")
                            }
                            ledgerAdapter.updateData(ledgers)
                        } else {
                            Log.d("LedgerListFragment", "No data found.")
                            Toast.makeText(requireContext(), "장부 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("LedgerListFragment", "API Error: ${response.code()}")
                        Toast.makeText(requireContext(), "데이터를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("LedgerListFragment", "Exception in fetchLedgerData", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
