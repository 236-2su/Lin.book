package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.ApiService
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.myapplication.ClubItem

class ClubListFragment : Fragment() {

    private lateinit var contentView: View
    private val clubItems = mutableListOf<ClubItem>()
    private val gson = Gson()
    private val TAG = "ClubListFragment"

    override fun onResume() {
        super.onResume()
        // 동아리 목록 화면에서는 상단 게시판 카테고리(공지/자유/행사장부)를 숨깁니다.
        (activity as? BaseActivity)?.hideBoardButtons()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 이 프래그먼트의 레이아웃을 인플레이트합니다.
        contentView = inflater.inflate(R.layout.activity_club_list, container, false)
        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated 시작")

        // setupContent의 내용을 여기에 배치
        (activity as? BaseActivity)?.setAppTitle("동아리")
        
        setupCategoryButtons()
        setupFloatingActionButton()
        setupAIFab()
        setupTempRootButton()
        Log.d(TAG, "API 호출 시작")
        fetchClubData()
    }

    private fun setupCategoryButtons() {
        val btnAll = contentView.findViewById<TextView>(R.id.btn_all)
        val btnAcademic = contentView.findViewById<TextView>(R.id.btn_academic)
        val btnSports = contentView.findViewById<TextView>(R.id.btn_sports)
        val btnCultureArt = contentView.findViewById<TextView>(R.id.btn_culture_art)
        val btnVolunteer = contentView.findViewById<TextView>(R.id.btn_volunteer)
        val btnStartup = contentView.findViewById<TextView>(R.id.btn_startup)
        val btnReligion = contentView.findViewById<TextView>(R.id.btn_religion)

        btnAll?.setOnClickListener { filterClubsByCategory("all"); updateButtonSelection(btnAll) }
        btnAcademic?.setOnClickListener { filterClubsByCategory("academic"); updateButtonSelection(btnAcademic) }
        btnSports?.setOnClickListener { filterClubsByCategory("sports"); updateButtonSelection(btnSports) }
        btnCultureArt?.setOnClickListener { filterClubsByCategory("culture"); updateButtonSelection(btnCultureArt) }
        btnVolunteer?.setOnClickListener { filterClubsByCategory("volunteer"); updateButtonSelection(btnVolunteer) }
        btnStartup?.setOnClickListener { filterClubsByCategory("entrepreneur"); updateButtonSelection(btnStartup) }
        btnReligion?.setOnClickListener { filterClubsByCategory("religion"); updateButtonSelection(btnReligion) }

        updateButtonSelection(btnAll)
    }

    private fun fetchClubData() {
        // ... (이하 로직은 ClubListActivity와 동일)
        Log.d(TAG, "fetchClubData 시작")
        
        try {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val apiService = ApiClient.getApiService()
                    val call = apiService.getClubList()
                    val response = call.execute()
                    
                    if (response.isSuccessful) {
                        val clubs = response.body()
                        if (clubs != null) {
                            withContext(Dispatchers.Main) {
                                displayClubList(clubs)
                            }
                        } else {
                            withContext(Dispatchers.Main) { displaySampleData() }
                        }
                    } else {
                        tryHttpFallback()
                    }
                } catch (e: Exception) {
                    tryHttpFallback()
                }
            }
        } catch (e: Exception) {
            activity?.runOnUiThread { displaySampleData() }
        }
    }
    
    private suspend fun tryHttpFallback() {
        // ... (이하 로직은 ClubListActivity와 동일)
        try {
            val client = ApiClient.createUnsafeOkHttpClient()
            val baseUrl = "https://finopenapi.ssafy.io"  // BuildConfig 대신 직접 URL 사용
            val request = Request.Builder().url("$baseUrl/club/").build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val type = object : TypeToken<List<ClubItem>>() {}.type
                    val clubs: List<ClubItem> = gson.fromJson(responseBody, type)
                    withContext(Dispatchers.Main) {
                        displayClubList(clubs)
                    }
                } else {
                    withContext(Dispatchers.Main) { displaySampleData() }
                }
            } else {
                withContext(Dispatchers.Main) { displaySampleData() }
            }
            response.close()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { displaySampleData() }
        }
    }

    private fun displayClubList(clubs: List<ClubItem>) {
        if (clubItems.isEmpty()) {
            clubItems.clear()
            clubItems.addAll(clubs)
        }

        // 1) 내 동아리 섹션 채우기 (SharedPreferences의 club_pks 기반)
        //    내 동아리는 카테고리 선택과 무관하게 항상 전체 목록을 기준으로 표시
        runCatching { fillMyClubsSection(clubItems) }.onFailure { /* ignore */ }

        // 2) 전체 동아리 목록 채우기
        val clubListContainer = contentView.findViewById<LinearLayout>(R.id.club_list_container) ?: return
        clubListContainer.removeAllViews()

        clubs.forEach { club ->
            val clubCard = createClubCard(club)
            clubListContainer.addView(clubCard)
        }
    }

    private fun fillMyClubsSection(clubs: List<ClubItem>) {
        val prefs = requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
        val pksStr = prefs.getString("club_pks", null) ?: return
        val pkList = pksStr.split(',').mapNotNull { it.trim().toIntOrNull() }
        
        val rv = contentView.findViewById<RecyclerView>(R.id.my_club_recycler) ?: return
        
        if (pkList.isEmpty()) {
            // 가입한 동아리가 없을 때
            showNoMyClubsMessage(rv)
            return
        }

        val myClubs = clubs.filter { pkList.contains(it.id) }
        
        if (myClubs.isEmpty()) {
            // 가입한 동아리가 없을 때
            showNoMyClubsMessage(rv)
            return
        }
        
        // 내 동아리가 있을 때 메시지 컨테이너 숨기기
        val messageContainer = contentView.findViewById<LinearLayout>(R.id.my_club_message_container)
        messageContainer?.visibility = View.GONE
        
        // RecyclerView 보이기
        rv.visibility = View.VISIBLE
        
        rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        if (rv.onFlingListener == null) {
            PagerSnapHelper().attachToRecyclerView(rv)
        }
        rv.adapter = object : RecyclerView.Adapter<MyClubVH>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyClubVH {
                val v = createMyClubCardView(parent)
                return MyClubVH(v)
            }
            override fun getItemCount(): Int = myClubs.size
            override fun onBindViewHolder(holder: MyClubVH, position: Int) {
                bindMyClubCard(holder.itemView as LinearLayout, myClubs[position])
            }
        }
        
        // 페이지 인디케이터 생성
        createPageIndicators(myClubs.size)
        
        // RecyclerView 스크롤 리스너 추가
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val position = layoutManager.findFirstVisibleItemPosition()
                updatePageIndicators(position)
            }
        })
    }

    private class MyClubVH(view: View) : RecyclerView.ViewHolder(view)
    
    // 페이지 인디케이터 관련 변수
    private val pageIndicators = mutableListOf<View>()
    private var currentPagePosition = 0
    
    // 가입한 동아리가 없을 때 메시지 표시
    private fun showNoMyClubsMessage(recyclerView: RecyclerView) {
        // RecyclerView를 숨기고 메시지 표시
        recyclerView.visibility = View.GONE
        
        // 메시지 컨테이너 표시
        val messageContainer = contentView.findViewById<LinearLayout>(R.id.my_club_message_container)
        messageContainer?.visibility = View.VISIBLE
        
        // 페이지 인디케이터 숨기기
        val container = contentView.findViewById<LinearLayout>(R.id.page_indicators_container)
        container?.visibility = View.GONE
    }

    private fun createMyClubCardView(parent: ViewGroup): View {
        val card = LinearLayout(parent.context).apply {
            // 아래 일반 동아리 카드와 동일 폭/마진 적용
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 8.dpToPx()) }
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.card_box_light_blue)
            setPadding(60, 40, 60, 40)
        }
        // 내부 컨테이너(뷰홀더 바인딩 시 채움)
        card.addView(LinearLayout(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        })
        return card
    }

    private fun bindMyClubCard(root: LinearLayout, club: ClubItem) {
        root.removeAllViews()
        val row = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }
        val info = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT).apply { weight = 1f }
            orientation = LinearLayout.VERTICAL
        }
        val hashtags = TextView(requireContext()).apply {
            text = club.hashtags
            setTextColor(android.graphics.Color.parseColor("#2457C5"))
            textSize = 12f
        }
        val name = TextView(requireContext()).apply {
            text = club.name
            setTextColor(android.graphics.Color.BLACK)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val deptLoc = TextView(requireContext()).apply {
            text = "${club.department} / ${club.location}"
            setTextColor(android.graphics.Color.parseColor("#666666"))
            textSize = 13f
        }
        info.addView(hashtags)
        info.addView(name)
        info.addView(deptLoc)
        
        val image = androidx.cardview.widget.CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(100.dpToPx(), 80.dpToPx())
            radius = 4f
            cardElevation = 0f
            addView(ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                setImageResource(R.drawable.club)
                scaleType = ImageView.ScaleType.CENTER_CROP
            })
        }
        row.addView(info)
        row.addView(image)
        root.addView(row)

        root.setOnClickListener {
            val intent = Intent(activity, ClubAnnouncementBoardListActivity::class.java)
            intent.putExtra("club_pk", club.id)
            startActivity(intent)
        }
    }
    
    // 페이지 인디케이터 생성
    private fun createPageIndicators(count: Int) {
        val container = contentView.findViewById<LinearLayout>(R.id.page_indicators_container) ?: return
        
        // 기존 인디케이터 제거
        container.removeAllViews()
        pageIndicators.clear()
        
        // 동아리 개수만큼 점 생성
        repeat(count) { index ->
            val indicator = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.page_indicator_size),
                    resources.getDimensionPixelSize(R.dimen.page_indicator_size)
                ).apply {
                    marginEnd = resources.getDimensionPixelSize(R.dimen.page_indicator_margin)
                }
                background = resources.getDrawable(
                    if (index == 0) R.drawable.page_indicator_selected 
                    else R.drawable.page_indicator_unselected, 
                    null
                )
            }
            
            pageIndicators.add(indicator)
            container.addView(indicator)
        }
        
        currentPagePosition = 0
    }
    
    // 페이지 인디케이터 업데이트
    private fun updatePageIndicators(position: Int) {
        if (position < 0 || position >= pageIndicators.size) return
        
        // 이전 위치의 인디케이터를 비활성화
        if (currentPagePosition < pageIndicators.size) {
            pageIndicators[currentPagePosition].background = resources.getDrawable(
                R.drawable.page_indicator_unselected, null
            )
        }
        
        // 현재 위치의 인디케이터를 활성화
        pageIndicators[position].background = resources.getDrawable(
            R.drawable.page_indicator_selected, null
        )
        
        currentPagePosition = position
    }

    private fun displaySampleData() {
        val sampleClubs = listOf(
            ClubItem(1, "방구석 경제", "경제학부", "academic", "경제", "경제를 좋아하는 사람이라면 누구나...", "#분위기가 좋은", "2025-08-23", "학생회관 421호", "1줄 소개"),
            ClubItem(2, "짱구네 코딩", "컴퓨터학부", "academic", "프로그래밍", "코딩을 좋아하는 사람들이 모여...", "#분위기가좋은 #동아리실이 편한", "2025-08-23", "학생회관 421호", "1줄 소개")
        )
        displayClubList(sampleClubs)
    }

    private fun createClubCard(club: ClubItem): View {
        // This part needs context (this -> requireContext())
        val cardView = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(4, 4, 4, 16) }
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.card_box_fafa)
        }
        val cardContent = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        // ... (and so on for all Views created with `this`)
        // 상단: 동아리 정보와 이미지
        val topSection = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 32)
        }

        // 왼쪽: 동아리 정보
        val infoSection = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
            }
            orientation = LinearLayout.VERTICAL
        }

        // 해시태그 (hashtags만 표시)
        val hashtagText = TextView(requireContext()).apply {
            text = club.hashtags
            setTextColor(android.graphics.Color.parseColor("#2457C5"))
            textSize = 12f // 글자 크기를 12sp로 수정
            setPadding(0, 0, 0, 8)
        }

        // 동아리 이름
        val nameText = TextView(requireContext()).apply {
            text = club.name
            setTextColor(android.graphics.Color.BLACK)
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        }

        // 학부/학과와 위치 정보를 하나로 합쳐서 표시
        val departmentLocationText = TextView(requireContext()).apply {
            text = "${club.department} / ${club.location}"
            setTextColor(android.graphics.Color.parseColor("#666666"))
            textSize = 12f
        }

        infoSection.addView(hashtagText)
        infoSection.addView(nameText)
        infoSection.addView(departmentLocationText)

        // 오른쪽: 동아리 이미지
        val imageCard = androidx.cardview.widget.CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(240, 180)
            radius = 4f
            elevation = 0f
        }

        val imageView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setImageResource(R.drawable.club)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        imageCard.addView(imageView)

        topSection.addView(infoSection)
        topSection.addView(imageCard)

        // 하단: 동아리 소개
        val introSection = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.card_box_gray)
            setPadding(20, 24, 20, 32)
        }

        // 제목
        val titleSection = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 12)
        }

        // 빨간색 압정 아이콘 (크기 증가 및 45도 회전)
        val redPinIcon = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(60, 60)
            setImageResource(R.drawable.ic_pushpin)
            setPadding(0, 0, 6, 0)
            rotation = 45f
        }

        val introTitle = TextView(requireContext()).apply {
            text = "동아리 소개"
            setTextColor(android.graphics.Color.BLACK)
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        titleSection.addView(redPinIcon)
        titleSection.addView(introTitle)

        // 내용
        val contentSection = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.TOP
        }

        val emojiIcon = TextView(requireContext()).apply {
            text = "💬"
            textSize = 16f
            setPadding(0, 0, 8, 0)
        }

        val descriptionText = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT).apply { weight = 1f }
            text = club.description
            setTextColor(android.graphics.Color.parseColor("#333333"))
            textSize = 12f
            setPadding(0, 0, 0, 4)
        }

        contentSection.addView(emojiIcon)
        contentSection.addView(descriptionText)

        introSection.addView(titleSection)
        introSection.addView(contentSection)

        cardContent.addView(topSection)
        cardContent.addView(introSection)
        cardView.addView(cardContent)

        // 카드 클릭 시 공지사항 리스트 화면으로 이동
        cardView.setOnClickListener {
            val intent = android.content.Intent(activity, ClubAnnouncementBoardListActivity::class.java)
            intent.putExtra("club_pk", club.id)
            startActivity(intent)
        }

        return cardView
    }

    private fun filterClubsByCategory(category: String) {
        if (category == "all") {
            displayClubList(clubItems)
        } else {
            val normalizedCategory = category.trim().lowercase()
            val filteredClubs = clubItems.filter { 
                it.majorCategory.trim().lowercase() == normalizedCategory
            }
            
            if (filteredClubs.isEmpty()) {
                displayEmptyCategoryMessage(category)
            } else {
                displayClubList(filteredClubs)
            }
        }
    }
    
    private fun displayEmptyCategoryMessage(category: String) {
        val clubListContainer = contentView.findViewById<LinearLayout>(R.id.club_list_container) ?: return
        clubListContainer.removeAllViews()
        
        val emptyMessage = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            text = "해당 카테고리의 동아리가 없습니다."
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#666666"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 100, 0, 0)
        }
        
        clubListContainer.addView(emptyMessage)
    }

    private fun updateButtonSelection(selectedButton: TextView?) {
        val allButtons = listOf(
            contentView.findViewById(R.id.btn_all) as? TextView,
            contentView.findViewById(R.id.btn_academic) as? TextView,
            contentView.findViewById(R.id.btn_sports) as? TextView,
            contentView.findViewById(R.id.btn_culture_art) as? TextView,
            contentView.findViewById(R.id.btn_volunteer) as? TextView,
            contentView.findViewById(R.id.btn_startup) as? TextView,
            contentView.findViewById(R.id.btn_religion) as? TextView
        )
        allButtons.forEach { button ->
            button?.setBackgroundResource(R.drawable.btn_unselected)
            button?.setTextColor(android.graphics.Color.parseColor("#333333"))
        }
        selectedButton?.setBackgroundResource(R.drawable.btn_selected)
        selectedButton?.setTextColor(android.graphics.Color.WHITE)
    }
    
    private fun setupTempRootButton() {
        val btnTempRoot = contentView.findViewById<android.widget.Button>(R.id.btn_temp_root)
        btnTempRoot?.setOnClickListener {
            // ReferenceFragment로 이동
            (activity as? MainActivity)?.replaceFragment(ReferenceFragment())
        }
    }
    
    private fun setupFloatingActionButton() {
        val rootLayout = activity?.findViewById<View>(android.R.id.content)
        val fab = FloatingActionButton(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                setMargins(0, 0, 40.dpToPx(), 40.dpToPx())
            }
            setImageResource(R.drawable.pencil)
            backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2457C5"))
            imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            elevation = 8f
            translationZ = 8f
            
            setOnClickListener {
                val intent = Intent(activity, ClubCreateActivity::class.java)
                startActivity(intent)
            }
        }
        
        if (rootLayout is ViewGroup) {
            rootLayout.addView(fab)
        }
    }

    // AI 추천 버튼 설정
    private fun setupAIFab() {
        contentView.findViewById<android.widget.TextView>(R.id.fab_ai_helper)?.apply {
            setOnClickListener {
                val dialogView = layoutInflater.inflate(R.layout.dialog_ai_recommend, null)
                val dialog = android.app.AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create()
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dialog.setCanceledOnTouchOutside(true)
                dialog.show()
                dialog.window?.setGravity(android.view.Gravity.BOTTOM)
                dialog.window?.attributes?.windowAnimations = R.style.Animation_Dialog

                // 탭 전환: 맞춤추천 / 검색 추천
                val tabPersonal = dialogView.findViewById<TextView>(R.id.tab_personal)
                val tabSearch = dialogView.findViewById<TextView>(R.id.tab_search)
                val panelPersonal = dialogView.findViewById<android.widget.LinearLayout>(R.id.panel_personal)
                val panelSearch = dialogView.findViewById<android.widget.LinearLayout>(R.id.panel_search)

                fun selectTab(personal: Boolean) {
                    if (personal) {
                        tabPersonal.setBackgroundResource(R.drawable.bg_ai_tab_selected)
                        tabPersonal.setTextColor(android.graphics.Color.WHITE)
                        tabSearch.setBackgroundResource(R.drawable.bg_ai_tab_unselected)
                        tabSearch.setTextColor(android.graphics.Color.parseColor("#707070"))
                        panelPersonal.visibility = android.view.View.VISIBLE
                        panelSearch.visibility = android.view.View.GONE
                    } else {
                        tabSearch.setBackgroundResource(R.drawable.bg_ai_tab_selected_search)
                        tabSearch.setTextColor(android.graphics.Color.WHITE)
                        tabPersonal.setBackgroundResource(R.drawable.bg_ai_tab_unselected)
                        tabPersonal.setTextColor(android.graphics.Color.parseColor("#707070"))
                        panelPersonal.visibility = android.view.View.GONE
                        panelSearch.visibility = android.view.View.VISIBLE
                    }
                }

                tabPersonal.setOnClickListener { selectTab(true) }
                tabSearch.setOnClickListener { selectTab(false) }
                // 기본: 맞춤추천 탭
                selectTab(true)

                // 맞춤추천: 스피너 구성 및 호출
                val spinner = dialogView.findViewById<android.widget.Spinner>(R.id.spinner_my_clubs)
                val prefs = requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
                val pksStr = prefs.getString("club_pks", null)
                val myClubIds = pksStr?.split(',')?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()
                val api = ApiClient.getApiService()
                api.getClubList().enqueue(object : retrofit2.Callback<kotlin.collections.List<ClubItem>> {
                    override fun onResponse(
                        call: retrofit2.Call<kotlin.collections.List<ClubItem>>,
                        response: retrofit2.Response<kotlin.collections.List<ClubItem>>
                    ) {
                        val all = response.body() ?: emptyList()
                        val mine = all.filter { myClubIds.contains(it.id) }
                        data class SpinnerClub(val id: Int, val name: String) { override fun toString(): String = name }
                        val items = mutableListOf(SpinnerClub(-1, "추천에 사용할 동아리를 선택하세요"))
                        items.addAll(mine.map { SpinnerClub(it.id, it.name) })
                        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, items)
                        spinner?.adapter = adapter
                    }
                    override fun onFailure(call: retrofit2.Call<kotlin.collections.List<ClubItem>>, t: Throwable) { }
                })

                dialogView.findViewById<Button>(R.id.btn_ai_personal)?.setOnClickListener {
                    val selected = spinner?.selectedItem
                    val selectedId = (selected as? Any)?.let {
                        when (it) {
                            is Int -> it
                            is String -> -1
                            else -> try { it.javaClass.getDeclaredField("id").apply { isAccessible = true }.get(it) as? Int ?: -1 } catch (_: Exception) { -1 }
                        }
                    } ?: -1
                    val selectedIndex = spinner?.selectedItemPosition ?: 0
                    if (selectedIndex <= 0 || selectedId <= 0) {
                        Toast.makeText(requireContext(), "동아리를 선택하세요.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    showAIRecommendationResults(dialog, selectedId, api, isPersonalized = true)
                }

                dialogView.findViewById<Button>(R.id.btn_ai_search)?.setOnClickListener {
                    val query = dialogView.findViewById<android.widget.EditText>(R.id.et_ai_query)?.text?.toString()?.trim()
                    if (query.isNullOrEmpty()) {
                        Toast.makeText(requireContext(), "질문을 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    
                    showAIRecommendationResults(dialog, query, api, isPersonalized = false)
                }
            }
        }
    }

    // AI 추천 결과 표시
    private fun showAIRecommendationResults(parentDialog: android.app.AlertDialog, queryOrId: Any, api: ApiService, isPersonalized: Boolean) {
        val resultView = layoutInflater.inflate(R.layout.dialog_ai_search_results, null)
        val resultDialog = android.app.AlertDialog.Builder(requireContext()).setView(resultView).create()
        resultDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        resultDialog.setCanceledOnTouchOutside(true)
        resultDialog.show()
        resultDialog.window?.setGravity(android.view.Gravity.BOTTOM)
        resultDialog.window?.attributes?.windowAnimations = R.style.Animation_Dialog

        val dragHandle = resultView.findViewById<android.view.View>(R.id.top_bar)
        var startY = 0f
        var totalDy = 0f
        dragHandle?.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> { startY = event.rawY; totalDy = 0f; true }
                android.view.MotionEvent.ACTION_MOVE -> { val dy = event.rawY - startY; if (dy > 0) { resultView.translationY = dy; totalDy = dy }; true }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    if (totalDy > 200f) { resultView.animate().translationY(resultView.height.toFloat()).setDuration(180).withEndAction { resultDialog.dismiss() }.start() }
                    else { resultView.animate().translationY(0f).setDuration(180).start() }
                    true
                }
                else -> false
            }
        }
        resultView.findViewById<android.widget.ImageView>(R.id.btn_close)?.setOnClickListener { resultDialog.dismiss() }

        val panelLoading = resultView.findViewById<android.widget.LinearLayout>(R.id.panel_loading)
        val scrollResults = resultView.findViewById<android.widget.ScrollView>(R.id.scroll_results)
        val listContainer = resultView.findViewById<android.widget.LinearLayout>(R.id.club_list_container)
        fun showLoading(show: Boolean) { 
            panelLoading.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE; 
            scrollResults.visibility = if (show) android.view.View.GONE else android.view.View.VISIBLE
            val botImage = resultView.findViewById<android.widget.ImageView>(R.id.img_ai_bot)
            if (show) {
                try {
                    val anim = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.bounce_ai_bot)
                    botImage?.startAnimation(anim)
                } catch (_: Exception) { }
            } else {
                botImage?.clearAnimation()
            }
        }
        showLoading(true)

        if (isPersonalized) {
            // 맞춤추천: getSimilarClubsByClub 사용
            val selectedId = queryOrId as Int
            api.getSimilarClubsByClub(selectedId).enqueue(object : retrofit2.Callback<ApiService.SimilarClubResponse> {
                override fun onResponse(
                    call: retrofit2.Call<ApiService.SimilarClubResponse>,
                    response: retrofit2.Response<ApiService.SimilarClubResponse>
                ) {
                    if (!response.isSuccessful) {
                        android.util.Log.w("AI_PERSONAL", "Retrofit 응답 비성공 code=${response.code()}")
                        showLoading(false)
                        Toast.makeText(requireContext(), "추천 요청 실패", Toast.LENGTH_SHORT).show()
                        return
                    }
                    val responseBody = response.body()
                    if (responseBody == null) {
                        android.util.Log.w("AI_PERSONAL", "응답 본문이 null입니다")
                        showLoading(false)
                        Toast.makeText(requireContext(), "추천 결과가 없습니다", Toast.LENGTH_SHORT).show()
                        return
                    }
                    
                    val similarClubs = responseBody.getSimilarClubs()
                    android.util.Log.d("AI_PERSONAL", "받은 추천 클럽 수: ${similarClubs.size}")
                    
                    // Extract club IDs and metadata from AI response
                    val clubIds = similarClubs.map { it.id }.toSet()
                    val aiMetadata = similarClubs.associate { it.id to Pair(it.score_hint, it.snippet) }
                    
                    // Use helper function to fetch club details efficiently
                    fetchClubDetailsForRecommendation(clubIds, listContainer, ::showLoading, api, aiMetadata)
                }
                override fun onFailure(
                    call: retrofit2.Call<ApiService.SimilarClubResponse>,
                    t: Throwable
                ) {
                    android.util.Log.e("AI_PERSONAL", "Retrofit 실패: ${t.message}", t)
                    showLoading(false)
                    Toast.makeText(requireContext(), "추천 요청 실패: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            // 검색 추천: HTTP 직접 호출 사용
            val query = queryOrId as String
            val client = ApiClient.createUnsafeOkHttpClient()
            val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
            
            fun parseIdsFromJson(json: String): kotlin.collections.Set<Int> {
                return try {
                    android.util.Log.d("AI_SEARCH", "Parsing JSON: ${json.take(200)}")
                    val element = com.google.gson.JsonParser().parse(json)
                    android.util.Log.d("AI_SEARCH", "JSON element type: ${element.javaClass.simpleName}")
                    
                    fun extractFromArray(arr: com.google.gson.JsonArray): kotlin.collections.MutableSet<Int> {
                        val out = mutableSetOf<Int>()
                        android.util.Log.d("AI_SEARCH", "Extracting from array, size: ${arr.size()}")
                        for (el in arr) {
                            if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) {
                                out.add(el.asInt)
                            } else if (el.isJsonObject) {
                                val obj = el.asJsonObject
                                val idVal = when {
                                    obj.has("id") -> obj.get("id")
                                    obj.has("club_id") -> obj.get("club_id")
                                    obj.has("pk") -> obj.get("pk")
                                    else -> null
                                }
                                if (idVal != null && idVal.isJsonPrimitive) {
                                    try { out.add(idVal.asInt) } catch (_: Exception) {}
                                }
                            }
                        }
                        android.util.Log.d("AI_SEARCH", "Extracted ${out.size} IDs from array")
                        return out
                    }
                    
                    fun extractFromObject(obj: com.google.gson.JsonObject): kotlin.collections.MutableSet<Int> {
                        val out = mutableSetOf<Int>()
                        android.util.Log.d("AI_SEARCH", "Extracting from object, keys: ${obj.keySet()}")
                        
                        // Try different possible keys for arrays
                        val arrayKeys = listOf("results", "items", "data", "clubs", "recommendations", "similar")
                        for (key in arrayKeys) {
                            if (obj.has(key)) {
                                val value = obj.get(key)
                                android.util.Log.d("AI_SEARCH", "Found key '$key': ${value.javaClass.simpleName}")
                                if (value.isJsonArray) {
                                    return extractFromArray(value.asJsonArray)
                                }
                            }
                        }
                        
                        // If object has direct id field
                        val idVal = when {
                            obj.has("id") -> obj.get("id")
                            obj.has("club_id") -> obj.get("club_id")
                            obj.has("pk") -> obj.get("pk")
                            else -> null
                        }
                        if (idVal != null && idVal.isJsonPrimitive) {
                            try { 
                                out.add(idVal.asInt)
                                android.util.Log.d("AI_SEARCH", "Added single ID from object: ${idVal.asInt}")
                            } catch (_: Exception) {}
                        }
                        return out
                    }
                    
                    val result = if (element.isJsonArray) {
                        extractFromArray(element.asJsonArray)
                    } else if (element.isJsonObject) {
                        extractFromObject(element.asJsonObject)
                    } else {
                        android.util.Log.w("AI_SEARCH", "Unexpected JSON element type: ${element.javaClass.simpleName}")
                        emptySet()
                    }
                    
                    android.util.Log.d("AI_SEARCH", "Final parsed IDs: $result")
                    result
                } catch (e: Exception) { 
                    android.util.Log.e("AI_SEARCH", "JSON parsing failed: ${e.message}", e)
                    emptySet() 
                }
            }

            fun requestSimilar(url: String, onDone: (kotlin.collections.Set<Int>?) -> Unit) {
                val req = okhttp3.Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Accept", "application/json")
                    .build()
                client.newCall(req).enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        android.util.Log.e("AI_SEARCH", "similar request failed: ${e.message}")
                        onDone(null)
                    }
                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        response.use { resp ->
                            val body = resp.body?.string()
                            android.util.Log.d("AI_SEARCH", "similar code=${resp.code} url=${url} body=${body?.take(300)}")
                            if (!resp.isSuccessful || body == null) { onDone(null); return }
                            val ids = parseIdsFromJson(body)
                            android.util.Log.d("AI_SEARCH", "parsed ids size=${ids.size} ids=${ids}")
                            onDone(ids)
                        }
                    }
                })
            }

            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val urlPrimary = "$baseUrl/club/similar/?query=$encoded"
            val urlFallback = "$baseUrl/club/similar/?text=$encoded"

            requestSimilar(urlPrimary) { idsOrNull ->
                if (idsOrNull == null) {
                    requestSimilar(urlFallback) { idsOrNull2 ->
                        activity?.runOnUiThread {
                            val ids = idsOrNull2 ?: emptySet()
                            // Use helper function for efficient club detail fetching
                            fetchClubDetailsForRecommendation(ids, listContainer, ::showLoading, api)
                        }
                    }
                } else {
                    activity?.runOnUiThread {
                        val ids = idsOrNull
                        // Use helper function for efficient club detail fetching
                        fetchClubDetailsForRecommendation(ids, listContainer, ::showLoading, api)
                    }
                }
            }
        }
        parentDialog.dismiss()
    }

    // helper to fetch club details efficiently for AI recommendations
    private fun fetchClubDetailsForRecommendation(
        clubIds: Set<Int>, 
        listContainer: android.widget.LinearLayout, 
        showLoading: (Boolean) -> Unit,
        api: ApiService,
        aiMetadata: Map<Int, Pair<Float?, String?>>? = null
    ) {
        if (clubIds.isEmpty()) {
            showLoading(false)
            listContainer.removeAllViews()
            val empty = android.widget.TextView(requireContext()).apply {
                text = "추천 결과가 없습니다."
                setTextColor(android.graphics.Color.parseColor("#666666"))
                textSize = 16f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 100, 0, 100)
            }
            listContainer.addView(empty)
            return
        }

        var completedCount = 0
        val totalCount = clubIds.size
        listContainer.removeAllViews()

        clubIds.forEach { clubId ->
            api.getClubDetail(clubId).enqueue(object : retrofit2.Callback<ClubItem> {
                override fun onResponse(
                    call: retrofit2.Call<ClubItem>,
                    detailResponse: retrofit2.Response<ClubItem>
                ) {
                    completedCount++
                    if (detailResponse.isSuccessful && detailResponse.body() != null) {
                        val club = detailResponse.body()!!
                        val metadata = aiMetadata?.get(clubId)
                        
                        // Create card for this recommended club
                        val card = android.widget.LinearLayout(requireContext()).apply {
                            orientation = android.widget.LinearLayout.VERTICAL
                            setBackgroundResource(R.drawable.card_box_fafa)
                            setPadding(40, 40, 40, 40)
                            
                            // Add AI score hint if available
                            metadata?.first?.let { score ->
                                val tvScore = android.widget.TextView(requireContext()).apply {
                                    text = "🤖 AI 추천도: ${(score * 100).toInt()}%"
                                    setTextColor(android.graphics.Color.parseColor("#1976D2"))
                                    textSize = 12f
                                    setTypeface(null, android.graphics.Typeface.BOLD)
                                }
                                addView(tvScore)
                            }
                            
                            // AI snippet removed per user request
                            
                            val tvName = android.widget.TextView(requireContext()).apply {
                                text = club.name
                                setTextColor(android.graphics.Color.BLACK)
                                textSize = 18f
                                setTypeface(null, android.graphics.Typeface.BOLD)
                            }
                            val tvDept = android.widget.TextView(requireContext()).apply {
                                text = "${club.department} / ${club.location}"
                                setTextColor(android.graphics.Color.parseColor("#666666"))
                                textSize = 12f
                            }
                            val tvDesc = android.widget.TextView(requireContext()).apply {
                                text = club.shortDescription
                                setTextColor(android.graphics.Color.parseColor("#333333"))
                                textSize = 12f
                            }
                            
                            addView(tvName)
                            addView(tvDept)
                            addView(tvDesc)
                            
                            setOnClickListener {
                                val intent = Intent(requireContext(), ClubAnnouncementBoardListActivity::class.java)
                                intent.putExtra("club_pk", club.id)
                                startActivity(intent)
                            }
                        }
                        val lp = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                        lp.setMargins(4, 4, 4, 16)
                        card.layoutParams = lp
                        listContainer.addView(card)
                    }
                    
                    // Hide loading when all requests completed
                    if (completedCount >= totalCount) {
                        showLoading(false)
                    }
                }
                override fun onFailure(call: retrofit2.Call<ClubItem>, t: Throwable) {
                    completedCount++
                    android.util.Log.e("AI_RECOMMENDATION", "Club detail request failed for ID $clubId: ${t.message}")
                    
                    // Hide loading when all requests completed
                    if (completedCount >= totalCount) {
                        showLoading(false)
                    }
                }
            })
        }
    }
    
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
