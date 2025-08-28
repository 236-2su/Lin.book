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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import com.example.myapplication.api.ApiClient
import okhttp3.Request
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
            val baseUrl = com.example.myapplication.BuildConfig.BASE_URL.trimEnd('/')
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
        if (pkList.isEmpty()) return

        val myClubs = clubs.filter { pkList.contains(it.id) }
        val rv = contentView.findViewById<RecyclerView>(R.id.my_club_recycler) ?: return
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
    }

    private class MyClubVH(view: View) : RecyclerView.ViewHolder(view)

    private fun createMyClubCardView(parent: ViewGroup): View {
        val card = LinearLayout(parent.context).apply {
            // 아래 일반 동아리 카드와 동일 폭/마진 적용
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 16.dpToPx()) }
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
        val parent = contentView.findViewById<CoordinatorLayout>(R.id.root_club_list) ?: return
        if (parent.findViewWithTag<View>("fab_create_club") != null) return
        val fab = FloatingActionButton(requireContext()).apply {
            tag = "fab_create_club"
            val lp = CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
            lp.setMargins(0, 0, 24.dpToPx(), 24.dpToPx())
            layoutParams = lp
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
        parent.addView(fab)
    }

    private fun setupAIFab() {
        contentView.findViewById<View>(R.id.fab_ai_helper)?.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_ai_recommend, null)
            val dialog = android.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.setCanceledOnTouchOutside(true)
            dialog.show()
            dialog.window?.setGravity(android.view.Gravity.BOTTOM)
            dialog.window?.attributes?.windowAnimations = R.style.Animation_Dialog

            val tabPersonal = dialogView.findViewById<TextView>(R.id.tab_personal)
            val tabSearch = dialogView.findViewById<TextView>(R.id.tab_search)
            val panelPersonal = dialogView.findViewById<android.widget.LinearLayout>(R.id.panel_personal)
            val panelSearch = dialogView.findViewById<android.widget.LinearLayout>(R.id.panel_search)

            fun selectTab(personal: Boolean) {
                if (personal) {
                    tabPersonal.setBackgroundResource(R.drawable.bg_ai_tab_selected)
                    tabPersonal.setTextColor(android.graphics.Color.WHITE)
                    tabSearch.setBackgroundResource(R.drawable.bg_ai_tab_unselected)
                    tabSearch.setTextColor(android.graphics.Color.parseColor("#7B61FF"))
                    panelPersonal.visibility = android.view.View.VISIBLE
                    panelSearch.visibility = android.view.View.GONE
                } else {
                    tabSearch.setBackgroundResource(R.drawable.bg_ai_tab_selected_search)
                    tabSearch.setTextColor(android.graphics.Color.WHITE)
                    tabPersonal.setBackgroundResource(R.drawable.bg_ai_tab_unselected)
                    tabPersonal.setTextColor(android.graphics.Color.parseColor("#7B61FF"))
                    panelPersonal.visibility = android.view.View.GONE
                    panelSearch.visibility = android.view.View.VISIBLE
                }
            }

            tabPersonal.setOnClickListener { selectTab(true) }
            tabSearch.setOnClickListener { selectTab(false) }
            selectTab(true)

            dialogView.findViewById<android.widget.Button>(R.id.btn_ai_personal)?.setOnClickListener {
                android.widget.Toast.makeText(requireContext(), "맞춤 추천 준비 중", android.widget.Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            dialogView.findViewById<android.widget.Button>(R.id.btn_ai_search)?.setOnClickListener {
                val query = dialogView.findViewById<android.widget.EditText>(R.id.et_ai_query)?.text?.toString()?.trim()
                if (query.isNullOrEmpty()) {
                    android.widget.Toast.makeText(requireContext(), "질문을 입력해주세요.", android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                android.widget.Toast.makeText(requireContext(), "검색: $query", android.widget.Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
    }
    
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
