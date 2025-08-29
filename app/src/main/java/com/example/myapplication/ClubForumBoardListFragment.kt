package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.api.ApiClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException

class ClubForumBoardListFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var boardAdapter: BoardAdapter
    private val boardList = mutableListOf<BoardItem>()
    private var clubPk: Int = -1
    
    companion object {
        private const val ARG_CLUB_PK = "club_pk"
        
        fun newInstance(clubPk: Int): ClubForumBoardListFragment {
            val fragment = ClubForumBoardListFragment()
            val args = Bundle()
            args.putInt(ARG_CLUB_PK, clubPk)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            clubPk = it.getInt(ARG_CLUB_PK, -1)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_club_forum_board_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Floating Action Button 설정
        view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add_post).setOnClickListener {
            val intent = Intent(requireContext(), ClubForumBoardCreateActivity::class.java)
            intent.putExtra("club_pk", clubPk)
            startActivity(intent)
        }
        
        // RecyclerView 설정
        recyclerView = view.findViewById(R.id.rv_board_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        boardAdapter = BoardAdapter(boardList) { boardItem ->
            // 아이템 클릭 시 상세 페이지로 이동
            val intent = Intent(requireContext(), ClubForumBoardDetailActivity::class.java)
            intent.putExtra("board_item", boardItem)
            intent.putExtra("club_pk", clubPk)
            startActivity(intent)
        }
        
        recyclerView.adapter = boardAdapter
        
        // API 호출
        fetchClubDetail(clubPk)
        fetchBoardList(clubPk)
    }
    
    override fun onResume() {
        super.onResume()
        fetchBoardList(clubPk)
    }
    
    private fun fetchClubDetail(clubPk: Int) {
        // 클럽 상세 정보 가져오기 (필요한 경우)
    }
    
    private fun fetchBoardList(clubPk: Int) {
        val client = OkHttpClient()
        val url = "https://finopenapi.ssafy.io/api/v1/clubs/$clubPk/boards?category=free"
        
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "게시판 목록을 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    responseBody?.let { body ->
                        try {
                            val gson = Gson()
                            val boardResponse = gson.fromJson(body, BoardResponse::class.java)
                            
                            requireActivity().runOnUiThread {
                                boardList.clear()
                                boardList.addAll(boardResponse.data)
                                boardAdapter.notifyDataSetChanged()
                            }
                        } catch (e: Exception) {
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "데이터 파싱 오류", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "게시판 목록을 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
    
    // BoardResponse 데이터 클래스
    data class BoardResponse(
        val data: List<BoardItem>
    )
}
