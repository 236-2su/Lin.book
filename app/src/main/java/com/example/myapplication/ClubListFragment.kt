package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.text.TextUtils
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
        // Fragment가 detach되었으면 종료
        if (!isAdded) return
        
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
        // Fragment가 detach되었으면 종료
        if (!isAdded) return
        val sampleClubs = listOf(
            ClubItem(1, "방구석 경제", "경제학부", "academic", "경제", "경제를 좋아하는 사람이라면 누구나...", "#분위기가 좋은", "2025-08-23", "학생회관 421호", "1줄 소개"),
            ClubItem(2, "짱구네 코딩", "컴퓨터학부", "academic", "프로그래밍", "코딩을 좋아하는 사람들이 모여...", "#분위기가좋은 #동아리실이 편한", "2025-08-23", "학생회관 421호", "1줄 소개")
        )
        displayClubList(sampleClubs)
    }

    private fun createClubCard(club: ClubItem, singleLineHashtags: Boolean = false): View {
        // Fragment가 attach되지 않았으면 빈 View 반환
        if (!isAdded) return View(context)
        
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
        if (singleLineHashtags) {
            hashtagText.maxLines = 1
            hashtagText.ellipsize = TextUtils.TruncateAt.END
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
            lp.setMargins(0, 0, 8.dpToPx(), 24.dpToPx())
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
            selectTab(true)

            // 맞춤추천: 내가 가입한 동아리 목록으로 스피너 채우기
            val spinner = dialogView.findViewById<android.widget.Spinner>(R.id.spinner_my_clubs)
            val prefs = requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
            val pksStr = prefs.getString("club_pks", null)
            val myClubIds = pksStr?.split(',')?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()

            val api = com.example.myapplication.api.ApiClient.getApiService()
            api.getClubList().enqueue(object : retrofit2.Callback<List<ClubItem>> {
                override fun onResponse(
                    call: retrofit2.Call<List<ClubItem>>,
                    response: retrofit2.Response<List<ClubItem>>
                ) {
                    val all = response.body().orEmpty()
                    val mine = all.filter { myClubIds.contains(it.id) }
                    data class SpinnerClub(val id: Int, val name: String) { override fun toString(): String = name }
                    val items = mutableListOf(SpinnerClub(-1, "추천에 사용할 동아리를 선택하세요"))
                    items.addAll(mine.map { SpinnerClub(it.id, it.name) })
                    val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, items)
                    spinner?.adapter = adapter
                }
                override fun onFailure(call: retrofit2.Call<List<ClubItem>>, t: Throwable) { /* ignore */ }
            })

            dialogView.findViewById<android.widget.Button>(R.id.btn_ai_personal)?.setOnClickListener {
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
                    android.widget.Toast.makeText(requireContext(), "동아리를 선택하세요.", android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val resultView = layoutInflater.inflate(R.layout.dialog_ai_search_results, null)
                val resultDialog = android.app.AlertDialog.Builder(requireContext()).setView(resultView).create()
                resultDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                resultDialog.setCanceledOnTouchOutside(true)
                resultDialog.show()
                resultDialog.window?.setGravity(android.view.Gravity.BOTTOM)
                resultDialog.window?.attributes?.windowAnimations = R.style.Animation_Dialog

                val dragHandle = resultView.findViewById<View>(R.id.top_bar)
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
                    panelLoading.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
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

                api.getSimilarClubsByClub(selectedId).enqueue(object : retrofit2.Callback<List<com.example.myapplication.api.ApiService.SimilarClubItem>> {
                    override fun onResponse(
                        call: retrofit2.Call<List<com.example.myapplication.api.ApiService.SimilarClubItem>>,
                        response: retrofit2.Response<List<com.example.myapplication.api.ApiService.SimilarClubItem>>
                    ) {
                        if (!response.isSuccessful) {
                            android.util.Log.w("AI_PERSONAL", "Retrofit 응답 비성공 code=${response.code()}")
                            requestSimilarFallbackInFragment(selectedId, listContainer, ::showLoading, api, resultView)
                            return
                        }
                        val ids = response.body()?.map { it.id }?.toSet() ?: emptySet()
                        if (ids.isEmpty()) {
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
                        api.getClubList().enqueue(object : retrofit2.Callback<List<ClubItem>> {
                            override fun onResponse(
                                call: retrofit2.Call<List<ClubItem>>,
                                response2: retrofit2.Response<List<ClubItem>>
                            ) {
                                val all2 = response2.body().orEmpty()
                                val matched = all2.filter { ids.contains(it.id) }
                                listContainer.removeAllViews()
                                matched.forEach { club -> listContainer.addView(createClubCard(club, singleLineHashtags = true)) }
                                showLoading(false)
                            }
                            override fun onFailure(call: retrofit2.Call<List<ClubItem>>, t: Throwable) {
                                showLoading(false)
                                android.widget.Toast.makeText(requireContext(), "클럽 목록 요청 실패", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                    override fun onFailure(
                        call: retrofit2.Call<List<com.example.myapplication.api.ApiService.SimilarClubItem>>,
                        t: Throwable
                    ) {
                        android.util.Log.e("AI_PERSONAL", "Retrofit 실패: ${t.message}", t)
                        requestSimilarFallbackInFragment(selectedId, listContainer, ::showLoading, api, resultView)
                    }
                })
                dialog.dismiss()
            }
            dialogView.findViewById<android.widget.Button>(R.id.btn_ai_search)?.setOnClickListener {
                val query = dialogView.findViewById<android.widget.EditText>(R.id.et_ai_query)?.text?.toString()?.trim()
                if (query.isNullOrEmpty()) {
                    android.widget.Toast.makeText(requireContext(), "질문을 입력해주세요.", android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // 결과 다이얼로그 표시 (로딩 → 결과)
                val resultView = layoutInflater.inflate(R.layout.dialog_ai_search_results, null)
                val resultDialog = android.app.AlertDialog.Builder(requireContext())
                    .setView(resultView)
                    .create()
                resultDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                resultDialog.setCanceledOnTouchOutside(true)
                resultDialog.show()
                resultDialog.window?.setGravity(android.view.Gravity.BOTTOM)
                resultDialog.window?.attributes?.windowAnimations = R.style.Animation_Dialog

                // 스와이프 다운으로 닫기 (상단 타이틀에서만 제스처 감지)
                val dragHandle = resultView.findViewById<View>(R.id.top_bar)
                var startY = 0f
                var totalDy = 0f
                dragHandle?.setOnTouchListener { _, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            startY = event.rawY
                            totalDy = 0f
                            true
                        }
                        android.view.MotionEvent.ACTION_MOVE -> {
                            val dy = event.rawY - startY
                            if (dy > 0) {
                                resultView.translationY = dy
                                totalDy = dy
                            }
                            true
                        }
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            if (totalDy > 200f) {
                                resultView.animate()
                                    .translationY(resultView.height.toFloat())
                                    .setDuration(180)
                                    .withEndAction { resultDialog.dismiss() }
                                    .start()
                            } else {
                                resultView.animate().translationY(0f).setDuration(180).start()
                            }
                            true
                        }
                        else -> false
                    }
                }

                val panelLoading = resultView.findViewById<android.widget.LinearLayout>(R.id.panel_loading)
                val scrollResults = resultView.findViewById<android.widget.ScrollView>(R.id.scroll_results)
                val listContainer = resultView.findViewById<android.widget.LinearLayout>(R.id.club_list_container)
                resultView.findViewById<android.widget.ImageView>(R.id.btn_close)?.setOnClickListener {
                    resultDialog.dismiss()
                }

                fun showLoading(show: Boolean) {
                    panelLoading.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
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

                // 1) /club/similar/ 호출 → 실패 시 파라미터명을 바꿔 재시도 (query → text)
                val api = com.example.myapplication.api.ApiClient.getApiService()
                val client = com.example.myapplication.api.ApiClient.createUnsafeOkHttpClient()
                val baseUrl = com.example.myapplication.BuildConfig.BASE_URL.trimEnd('/')

                fun parseIdsFromJson(json: String): Set<Int> {
                    return try {
                        val element = com.google.gson.JsonParser().parse(json)
                        fun extractFromArray(arr: com.google.gson.JsonArray): MutableSet<Int> {
                            val out = mutableSetOf<Int>()
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
                            return out
                        }
                        if (element.isJsonArray) {
                            extractFromArray(element.asJsonArray)
                        } else if (element.isJsonObject) {
                            val obj = element.asJsonObject
                            val keys = listOf("results", "items", "data")
                            val arrKey = keys.firstOrNull { obj.has(it) && obj.get(it).isJsonArray }
                            if (arrKey != null) extractFromArray(obj.getAsJsonArray(arrKey)) else {
                                val idVal = when {
                                    obj.has("id") -> obj.get("id")
                                    obj.has("club_id") -> obj.get("club_id")
                                    obj.has("pk") -> obj.get("pk")
                                    else -> null
                                }
                                val set = mutableSetOf<Int>()
                                if (idVal != null && idVal.isJsonPrimitive) {
                                    try { set.add(idVal.asInt) } catch (_: Exception) {}
                                }
                                set
                            }
                        } else emptySet()
                    } catch (_: Exception) { emptySet() }
                }

                fun requestSimilar(url: String, onDone: (Set<Int>?) -> Unit) {
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
                                if (ids.isEmpty()) {
                                    showLoading(false)
                                    listContainer.removeAllViews()
                                    val empty = android.widget.TextView(requireContext()).apply {
                                        text = "검색 결과가 없습니다."
                                        setTextColor(android.graphics.Color.parseColor("#666666"))
                                        textSize = 16f
                                        gravity = android.view.Gravity.CENTER
                                        setPadding(0, 100, 0, 100)
                                    }
                                    listContainer.addView(empty)
                                } else {
                                    api.getClubList().enqueue(object : retrofit2.Callback<List<ClubItem>> {
                                        override fun onResponse(
                                            call: retrofit2.Call<List<ClubItem>>,
                                            response2: retrofit2.Response<List<ClubItem>>
                                        ) {
                                            val all = response2.body().orEmpty()
                                            val matched = all.filter { ids.contains(it.id) }
                                            android.util.Log.d("AI_SEARCH", "all clubs=${all.size} matched=${matched.size}")
                                            listContainer.removeAllViews()
                                            matched.forEach { club ->
                                                val card = createClubCard(club, singleLineHashtags = true)
                                                listContainer.addView(card)
                                            }
                                            showLoading(false)
                                        }
                                        override fun onFailure(call: retrofit2.Call<List<ClubItem>>, t: Throwable) {
                                            showLoading(false)
                                            android.widget.Toast.makeText(requireContext(), "클럽 목록 요청 실패", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    })
                                }
                            }
                        }
                    } else {
                        activity?.runOnUiThread {
                            val ids = idsOrNull
                            api.getClubList().enqueue(object : retrofit2.Callback<List<ClubItem>> {
                                override fun onResponse(
                                    call: retrofit2.Call<List<ClubItem>>,
                                    response2: retrofit2.Response<List<ClubItem>>
                                ) {
                                    val all = response2.body().orEmpty()
                                    val matched = all.filter { ids.contains(it.id) }
                                    android.util.Log.d("AI_SEARCH", "all clubs=${all.size} matched=${matched.size}")
                                    listContainer.removeAllViews()
                                    matched.forEach { club ->
                                        val card = createClubCard(club, singleLineHashtags = true)
                                        listContainer.addView(card)
                                    }
                                    showLoading(false)
                                }
                                override fun onFailure(call: retrofit2.Call<List<ClubItem>>, t: Throwable) {
                                    showLoading(false)
                                    android.widget.Toast.makeText(requireContext(), "클럽 목록 요청 실패", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    }
                }

                dialog.dismiss()
            }
        }
    }
    
    private fun requestSimilarFallbackInFragment(selectedId: Int, listContainer: android.widget.LinearLayout, showLoading: (Boolean) -> Unit, api: com.example.myapplication.api.ApiService, resultView: View) {
        val client = com.example.myapplication.api.ApiClient.createUnsafeOkHttpClient()
        val baseUrl = com.example.myapplication.BuildConfig.BASE_URL.trimEnd('/')
        val url = "$baseUrl/club/$selectedId/similar/"
        android.util.Log.d("AI_PERSONAL", "폴백 요청 URL: $url")
        val req = okhttp3.Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .build()
        client.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                activity?.runOnUiThread {
                    showLoading(false)
                    android.widget.Toast.makeText(requireContext(), "추천 요청 실패: ${e.message ?: "네트워크 오류"}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use { resp ->
                    val body = resp.body?.string()
                    android.util.Log.d("AI_PERSONAL", "폴백 응답 코드=${resp.code} body=${body?.take(300)}")
                    fun parseIdsFromJson(json: String): kotlin.collections.Set<Int> {
                        return try {
                            val element = com.google.gson.JsonParser().parse(json)
                            fun extractFromArray(arr: com.google.gson.JsonArray): kotlin.collections.MutableSet<Int> {
                                val out = mutableSetOf<Int>()
                                var i = 0
                                while (i < arr.size()) {
                                    val el = arr.get(i)
                                    try {
                                        if (el.isJsonPrimitive) {
                                            val prim = el.asJsonPrimitive
                                            if (prim.isNumber) out.add(prim.asInt)
                                            else if (prim.isString) prim.asString.toIntOrNull()?.let { out.add(it) }
                                        } else if (el.isJsonObject) {
                                            val obj = el.asJsonObject
                                            val idVal = when {
                                                obj.has("id") -> obj.get("id")
                                                obj.has("club_id") -> obj.get("club_id")
                                                obj.has("pk") -> obj.get("pk")
                                                else -> null
                                            }
                                            if (idVal != null) {
                                                if (idVal.isJsonPrimitive && idVal.asJsonPrimitive.isNumber) out.add(idVal.asInt)
                                                else if (idVal.isJsonPrimitive && idVal.asJsonPrimitive.isString) idVal.asString.toIntOrNull()?.let { out.add(it) }
                                            }
                                        }
                                    } catch (_: Exception) {}
                                    i++
                                }
                                return out
                            }
                            if (element.isJsonArray) {
                                extractFromArray(element.asJsonArray)
                            } else if (element.isJsonObject) {
                                val obj = element.asJsonObject
                                val key = listOf("results", "items", "data").firstOrNull { k -> obj.has(k) && obj.get(k).isJsonArray }
                                if (key != null) extractFromArray(obj.getAsJsonArray(key)) else {
                                    val idVal = when {
                                        obj.has("id") -> obj.get("id")
                                        obj.has("club_id") -> obj.get("club_id")
                                        obj.has("pk") -> obj.get("pk")
                                        else -> null
                                    }
                                    val set = mutableSetOf<Int>()
                                    if (idVal != null) {
                                        if (idVal.isJsonPrimitive && idVal.asJsonPrimitive.isNumber) set.add(idVal.asInt)
                                        else if (idVal.isJsonPrimitive && idVal.asJsonPrimitive.isString) idVal.asString.toIntOrNull()?.let { set.add(it) }
                                    }
                                    set
                                }
                            } else emptySet()
                        } catch (_: Exception) { emptySet() }
                    }
                    val ids: kotlin.collections.Set<Int> = try {
                        if (!resp.isSuccessful || body == null) emptySet() else parseIdsFromJson(body)
                    } catch (_: Exception) { emptySet() }
                    activity?.runOnUiThread {
                        if (ids.isEmpty()) {
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
                        } else {
                            api.getClubList().enqueue(object : retrofit2.Callback<List<ClubItem>> {
                                override fun onResponse(
                                    call: retrofit2.Call<List<ClubItem>>,
                                    response2: retrofit2.Response<List<ClubItem>>
                                ) {
                                    val all2 = response2.body().orEmpty()
                                    val matched = all2.filter { ids.contains(it.id) }
                                    listContainer.removeAllViews()
                                    matched.forEach { club -> listContainer.addView(createClubCard(club, singleLineHashtags = true)) }
                                    showLoading(false)
                                }
                                override fun onFailure(call: retrofit2.Call<List<ClubItem>>, t: Throwable) {
                                    showLoading(false)
                                    android.widget.Toast.makeText(requireContext(), "클럽 목록 요청 실패", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    }
                }
            }
        })
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
