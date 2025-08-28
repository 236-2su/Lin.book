package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.api.ApiClient
import com.example.myapplication.api.ApiService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException

class ClubAnnouncementBoardListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var boardAdapter: BoardAdapter
    private val boardList = mutableListOf<BoardItem>()

    companion object {
        private const val EXTRA_CLUB_PK = "club_pk"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_announcement_board_list)

        // 뒤로가기 버튼 설정: 시스템 백스택으로 이전 페이지 이동
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 게시판 버튼 설정
        setupBoardButtons()

        // Floating Action Button 설정
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add_post).setOnClickListener {
            val currentClubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val intent = Intent(this, ClubAnnouncementBoardCreateActivity::class.java)
            intent.putExtra(EXTRA_CLUB_PK, currentClubPk)
            startActivity(intent)
        }

        // AI 버튼 클릭 시 다이얼로그 표시
        findViewById<android.widget.TextView>(R.id.eaturefab_ai_helper)?.apply {
            setOnClickListener {
                val dialogView = layoutInflater.inflate(R.layout.dialog_ai_recommend, null)
                val dialog = android.app.AlertDialog.Builder(this@ClubAnnouncementBoardListActivity)
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
                val prefs = this@ClubAnnouncementBoardListActivity.getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
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
                        val adapter = android.widget.ArrayAdapter(this@ClubAnnouncementBoardListActivity, android.R.layout.simple_spinner_dropdown_item, items)
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
                        Toast.makeText(this@ClubAnnouncementBoardListActivity, "동아리를 선택하세요.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val resultView = layoutInflater.inflate(R.layout.dialog_ai_search_results, null)
                    val resultDialog = android.app.AlertDialog.Builder(this@ClubAnnouncementBoardListActivity).setView(resultView).create()
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
                    fun showLoading(show: Boolean) { panelLoading.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE; scrollResults.visibility = if (show) android.view.View.GONE else android.view.View.VISIBLE
                        val botImage = resultView.findViewById<android.widget.ImageView>(R.id.img_ai_bot)
                        if (show) {
                            try {
                                val anim = android.view.animation.AnimationUtils.loadAnimation(this@ClubAnnouncementBoardListActivity, R.anim.bounce_ai_bot)
                                botImage?.startAnimation(anim)
                            } catch (_: Exception) { }
                        } else {
                            botImage?.clearAnimation()
                        }
                    }
                    showLoading(true)

                    api.getSimilarClubsByClub(selectedId).enqueue(object : retrofit2.Callback<kotlin.collections.List<ApiService.SimilarClubItem>> {
                        override fun onResponse(
                            call: retrofit2.Call<kotlin.collections.List<ApiService.SimilarClubItem>>,
                            response: retrofit2.Response<kotlin.collections.List<ApiService.SimilarClubItem>>
                        ) {
                            if (!response.isSuccessful) {
                                android.util.Log.w("AI_PERSONAL", "Retrofit 응답 비성공 code=${response.code()}")
                                requestSimilarFallback(selectedId, listContainer, ::showLoading, api)
                                return
                            }
                            val ids = response.body()?.map { it.id }?.toSet() ?: emptySet()
                            if (ids.isEmpty()) {
                                showLoading(false)
                                listContainer.removeAllViews()
                                val empty = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                    text = "추천 결과가 없습니다."
                                    setTextColor(android.graphics.Color.parseColor("#666666"))
                                    textSize = 16f
                                    gravity = android.view.Gravity.CENTER
                                    setPadding(0, 100, 0, 100)
                                }
                                listContainer.addView(empty)
                                return
                            }
                            api.getClubList().enqueue(object : retrofit2.Callback<kotlin.collections.List<ClubItem>> {
                                override fun onResponse(
                                    call: retrofit2.Call<kotlin.collections.List<ClubItem>>,
                                    response2: retrofit2.Response<kotlin.collections.List<ClubItem>>
                                ) {
                                    val all2 = response2.body() ?: emptyList()
                                    val matched = all2.filter { ids.contains(it.id) }
                                    listContainer.removeAllViews()
                                    matched.forEach { club ->
                                        val card = android.widget.LinearLayout(this@ClubAnnouncementBoardListActivity).apply {
                                            orientation = android.widget.LinearLayout.VERTICAL
                                            setBackgroundResource(R.drawable.card_box_fafa)
                                            setPadding(40, 40, 40, 40)
                                            val tvName = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                                text = club.name
                                                setTextColor(android.graphics.Color.BLACK)
                                                textSize = 18f
                                                setTypeface(null, android.graphics.Typeface.BOLD)
                                            }
                                            val tvDept = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                                text = "${club.department} / ${club.location}"
                                                setTextColor(android.graphics.Color.parseColor("#666666"))
                                                textSize = 12f
                                            }
                                            val tvDesc = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                                text = club.shortDescription
                                                setTextColor(android.graphics.Color.parseColor("#333333"))
                                                textSize = 12f
                                            }
                                            addView(tvName)
                                            addView(tvDept)
                                            addView(tvDesc)
                                            setOnClickListener {
                                                val intent = Intent(this@ClubAnnouncementBoardListActivity, ClubAnnouncementBoardListActivity::class.java)
                                                intent.putExtra("club_pk", club.id)
                                                startActivity(intent)
                                            }
                                        }
                                        val lp = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                                        lp.setMargins(4, 4, 4, 16)
                                        card.layoutParams = lp
                                        listContainer.addView(card)
                                    }
                                    showLoading(false)
                                }
                                override fun onFailure(call: retrofit2.Call<kotlin.collections.List<ClubItem>>, t: Throwable) {
                                    showLoading(false)
                                    Toast.makeText(this@ClubAnnouncementBoardListActivity, "클럽 목록 요청 실패", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                        override fun onFailure(
                            call: retrofit2.Call<kotlin.collections.List<ApiService.SimilarClubItem>>,
                            t: Throwable
                        ) {
                            android.util.Log.e("AI_PERSONAL", "Retrofit 실패: ${t.message}", t)
                            requestSimilarFallback(selectedId, listContainer, ::showLoading, api)
                        }
                    })
                    dialog.dismiss()
                }

                dialogView.findViewById<Button>(R.id.btn_ai_search)?.setOnClickListener {
                    val query = dialogView.findViewById<android.widget.EditText>(R.id.et_ai_query)?.text?.toString()?.trim()
                    if (query.isNullOrEmpty()) {
                        Toast.makeText(this@ClubAnnouncementBoardListActivity, "질문을 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    val resultView = layoutInflater.inflate(R.layout.dialog_ai_search_results, null)
                    val resultDialog = android.app.AlertDialog.Builder(this@ClubAnnouncementBoardListActivity)
                        .setView(resultView)
                        .create()
                    resultDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                    resultDialog.setCanceledOnTouchOutside(true)
                    resultDialog.show()
                    resultDialog.window?.setGravity(android.view.Gravity.BOTTOM)
                    resultDialog.window?.attributes?.windowAnimations = R.style.Animation_Dialog

                    // 스와이프 다운으로 닫기 (상단 타이틀에서만 제스처 감지)
                    val dragHandle = resultView.findViewById<android.view.View>(R.id.top_bar)
                    var startY = 0f
                    var totalDy = 0f
                    dragHandle?.setOnTouchListener { _, event ->
                        when (event.action) {
                            android.view.MotionEvent.ACTION_DOWN -> { startY = event.rawY; totalDy = 0f; true }
                            android.view.MotionEvent.ACTION_MOVE -> {
                                val dy = event.rawY - startY
                                if (dy > 0) { resultView.translationY = dy; totalDy = dy }
                                true
                            }
                            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                                if (totalDy > 200f) {
                                    resultView.animate().translationY(resultView.height.toFloat()).setDuration(180).withEndAction { resultDialog.dismiss() }.start()
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
                    resultView.findViewById<android.widget.ImageView>(R.id.btn_close)?.setOnClickListener { resultDialog.dismiss() }
                    fun showLoading(show: Boolean) {
                        panelLoading.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
                        scrollResults.visibility = if (show) android.view.View.GONE else android.view.View.VISIBLE
                        val botImage = resultView.findViewById<android.widget.ImageView>(R.id.img_ai_bot)
                        if (show) {
                            try {
                                val anim = android.view.animation.AnimationUtils.loadAnimation(this@ClubAnnouncementBoardListActivity, R.anim.bounce_ai_bot)
                                botImage?.startAnimation(anim)
                            } catch (_: Exception) { }
                        } else {
                            botImage?.clearAnimation()
                        }
                    }
                    showLoading(true)

                    val api = ApiClient.getApiService()
                    val client = ApiClient.createUnsafeOkHttpClient()
                    val baseUrl = BuildConfig.BASE_URL.trimEnd('/')

                    fun parseIdsFromJson(json: String): kotlin.collections.Set<Int> {
                        return try {
                            val element = com.google.gson.JsonParser().parse(json)
                            fun extractFromArray(arr: com.google.gson.JsonArray): kotlin.collections.MutableSet<Int> {
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
                                runOnUiThread {
                                    val ids = idsOrNull2 ?: emptySet()
                                    if (ids.isEmpty()) {
                                        showLoading(false)
                                        listContainer.removeAllViews()
                                        val empty = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                            text = "검색 결과가 없습니다."
                                            setTextColor(android.graphics.Color.parseColor("#666666"))
                                            textSize = 16f
                                            gravity = android.view.Gravity.CENTER
                                            setPadding(0, 100, 0, 100)
                                        }
                                        listContainer.addView(empty)
                                    } else {
                                        api.getClubList().enqueue(object : retrofit2.Callback<kotlin.collections.List<ClubItem>> {
                                            override fun onResponse(
                                                call: retrofit2.Call<kotlin.collections.List<ClubItem>>,
                                                response2: retrofit2.Response<kotlin.collections.List<ClubItem>>
                                            ) {
                                                val all = response2.body() ?: emptyList()
                                                val matched = all.filter { ids.contains(it.id) }
                                                android.util.Log.d("AI_SEARCH", "all clubs=${all.size} matched=${matched.size}")
                                                listContainer.removeAllViews()
                                                matched.forEach { club ->
                                                    val card = android.widget.LinearLayout(this@ClubAnnouncementBoardListActivity).apply {
                                                        orientation = android.widget.LinearLayout.VERTICAL
                                                        setBackgroundResource(R.drawable.card_box_fafa)
                                                        setPadding(40, 40, 40, 40)
                                                        val tvName = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                                            text = club.name
                                                            setTextColor(android.graphics.Color.BLACK)
                                                            textSize = 18f
                                                            setTypeface(null, android.graphics.Typeface.BOLD)
                                                        }
                                                        val tvDept = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                                            text = "${club.department} / ${club.location}"
                                                            setTextColor(android.graphics.Color.parseColor("#666666"))
                                                            textSize = 12f
                                                        }
                                                        val tvDesc = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                                            text = club.shortDescription
                                                            setTextColor(android.graphics.Color.parseColor("#333333"))
                                                            textSize = 12f
                                                        }
                                                        addView(tvName)
                                                        addView(tvDept)
                                                        addView(tvDesc)
                                                        setOnClickListener {
                                                            val intent = Intent(this@ClubAnnouncementBoardListActivity, ClubAnnouncementBoardListActivity::class.java)
                                                            intent.putExtra("club_pk", club.id)
                                                            startActivity(intent)
                                                        }
                                                    }
                                                    val lp = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                                                    lp.setMargins(4, 4, 4, 16)
                                                    card.layoutParams = lp
                                                    listContainer.addView(card)
                                                }
                                                showLoading(false)
                                            }
                                            override fun onFailure(call: retrofit2.Call<kotlin.collections.List<ClubItem>>, t: Throwable) {
                                                showLoading(false)
                                                Toast.makeText(this@ClubAnnouncementBoardListActivity, "클럽 목록 요청 실패", Toast.LENGTH_SHORT).show()
                                            }
                                        })
                                    }
                                }
                            }
                        } else {
                            runOnUiThread {
                                val ids = idsOrNull
                                api.getClubList().enqueue(object : retrofit2.Callback<kotlin.collections.List<ClubItem>> {
                                    override fun onResponse(
                                        call: retrofit2.Call<kotlin.collections.List<ClubItem>>,
                                        response2: retrofit2.Response<kotlin.collections.List<ClubItem>>
                                    ) {
                                        val all = response2.body() ?: emptyList()
                                        val matched = all.filter { ids.contains(it.id) }
                                        android.util.Log.d("AI_SEARCH", "all clubs=${all.size} matched=${matched.size}")
                                        listContainer.removeAllViews()
                                        matched.forEach { club ->
                                            val card = android.widget.LinearLayout(this@ClubAnnouncementBoardListActivity).apply {
                                                orientation = android.widget.LinearLayout.VERTICAL
                                                setBackgroundResource(R.drawable.card_box_fafa)
                                                setPadding(40, 40, 40, 40)
                                                val tvName = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                                    text = club.name
                                                    setTextColor(android.graphics.Color.BLACK)
                                                    textSize = 18f
                                                    setTypeface(null, android.graphics.Typeface.BOLD)
                                                }
                                                val tvDept = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                                    text = "${club.department} / ${club.location}"
                                                    setTextColor(android.graphics.Color.parseColor("#666666"))
                                                    textSize = 12f
                                                }
                                                val tvDesc = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                                    text = club.shortDescription
                                                    setTextColor(android.graphics.Color.parseColor("#333333"))
                                                    textSize = 12f
                                                }
                                                addView(tvName)
                                                addView(tvDept)
                                                addView(tvDesc)
                                                setOnClickListener {
                                                    val intent = Intent(this@ClubAnnouncementBoardListActivity, ClubAnnouncementBoardListActivity::class.java)
                                                    intent.putExtra("club_pk", club.id)
                                                    startActivity(intent)
                                                }
                                            }
                                            val lp = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                                            lp.setMargins(4, 4, 4, 16)
                                            card.layoutParams = lp
                                            listContainer.addView(card)
                                        }
                                        showLoading(false)
                                    }
                                    override fun onFailure(call: retrofit2.Call<kotlin.collections.List<ClubItem>>, t: Throwable) {
                                        showLoading(false)
                                        Toast.makeText(this@ClubAnnouncementBoardListActivity, "클럽 목록 요청 실패", Toast.LENGTH_SHORT).show()
                                    }
                                })
                            }
                        }
                    }

                    dialog.dismiss()
                }
            }
        }

        // 공유하기 버튼: 동아리 URL을 클립보드에 복사하고 토스트 안내
        findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_share)?.setOnClickListener {
            val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            if (clubPk > 0) {
                val url = BuildConfig.BASE_URL.trimEnd('/') + "/club/" + clubPk + "/"
                val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("club_url", url)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(this, "동아리 URL이 저장되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        // 멤버 버튼: 멤버 리스트 화면으로 이동
        findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_member)?.setOnClickListener {
            val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val userPk = UserManager.getUserPk(this) ?: -1
            val intent = Intent(this, ClubMemberManagementActivity::class.java)
            intent.putExtra(ClubMemberManagementActivity.EXTRA_CLUB_PK, clubPk)
            intent.putExtra(ClubMemberManagementActivity.EXTRA_USER_PK, userPk)
            startActivity(intent)
        }

        // 설정 버튼: 동아리 정보 수정 화면으로 이동
        findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.btn_settings)?.setOnClickListener {
            val updateIntent = Intent(this, ClubUpdateActivity::class.java)
            // 현재 액티비티의 intent에서 club_pk를 가져와 전달
            val currentClubPk = this.intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            updateIntent.putExtra("club_pk", currentClubPk)
            startActivity(updateIntent)
        }

        // RecyclerView 설정
        recyclerView = findViewById(R.id.rv_board_list)
        recyclerView.layoutManager = LinearLayoutManager(this)

        boardAdapter = BoardAdapter(boardList) { boardItem ->
            // 아이템 클릭 시 상세 페이지로 이동
            val intent = Intent(this, ClubAnnouncementBoardDetailActivity::class.java)
            intent.putExtra("board_item", boardItem)
            val currentClubPk = this.intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            intent.putExtra("club_pk", currentClubPk)
            startActivity(intent)
        }

        recyclerView.adapter = boardAdapter

        // API 호출
        val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
        // 클럽 기본 정보 로드
        fetchClubDetail(clubPk)
        fetchBoardList(clubPk)
    }

    override fun onResume() {
        super.onResume()
        val clubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
        fetchBoardList(clubPk)
    }

    private fun setupBoardButtons() {
        // 공지사항 버튼 (현재 화면이므로 아무것도 하지 않음)
        findViewById<TextView>(R.id.btn_notice).setOnClickListener {
            // 이미 공지사항 화면이므로 아무것도 하지 않음
        }

        // 자유게시판 버튼
        findViewById<TextView>(R.id.btn_free_board).setOnClickListener {
            val currentClubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val intent = Intent(this, ClubForumBoardListActivity::class.java)
            intent.putExtra(EXTRA_CLUB_PK, currentClubPk)
            startActivity(intent)
            finish()
        }

        // 행사장부 버튼
        findViewById<TextView>(R.id.btn_event_account).setOnClickListener {
            val currentClubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val intent = Intent(this, ClubEventLedgerListActivity::class.java)
            intent.putExtra(ClubEventLedgerListActivity.EXTRA_CLUB_PK, currentClubPk)
            startActivity(intent)
            finish()
        }

        // AI 리포트 버튼
        findViewById<TextView>(R.id.btn_ai_report).setOnClickListener {
            val currentClubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val intent = Intent(this, LedgerReportActivity::class.java)
            intent.putExtra("club_id", currentClubPk)
            startActivity(intent)
            finish()
        }

        // 모임통장 버튼
        findViewById<TextView>(R.id.btn_meeting_account).setOnClickListener {
            val currentClubPk = intent?.getIntExtra(EXTRA_CLUB_PK, -1) ?: -1
            val intent = Intent(this, AccountHistoryActivity::class.java)
            intent.putExtra("club_pk", currentClubPk)
            startActivity(intent)
        }
    }

    private fun fetchBoardList(clubPk: Int) {
        if (clubPk <= 0) {
            android.util.Log.e("API_ERROR", "유효하지 않은 club_pk: $clubPk")
            Toast.makeText(this, "동아리 정보를 확인할 수 없습니다.", Toast.LENGTH_LONG).show()
            return
        }
        val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
        val primaryUrl = "$baseUrl/club/$clubPk/boards"
        val fallbackUrl = "$baseUrl/club/$clubPk/boards/"
        android.util.Log.d("API_REQUEST", "요청 URL(우선): $primaryUrl")

        // HTTPS(자가서명 등) 환경에서도 동작하도록 개발용 클라이언트 사용
        val client = ApiClient.createUnsafeOkHttpClient()
        fun buildRequest(targetUrl: String): Request =
            Request.Builder()
                .url(targetUrl)
                .get()
                .addHeader("Accept", "application/json")
                .build()

        fun handleResponse(response: Response, usedUrl: String) {
            val responseBody = response.body?.string()
            android.util.Log.d("API_RESPONSE", "응답 코드: ${response.code} (URL: $usedUrl)")
            android.util.Log.d("API_RESPONSE", "응답 본문: $responseBody")
            runOnUiThread {
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val gson = Gson()
                        val type = object : TypeToken<List<BoardItem>>() {}.type
                        val boards = gson.fromJson<List<BoardItem>>(responseBody, type)
                        android.util.Log.d("API_SUCCESS", "파싱된 게시글 수: ${boards.size}")
                        val announcementBoards = boards.filter { it.type == "announcement" && it.club == clubPk }
                        val sorted = announcementBoards.sortedByDescending { parseBoardDate(it.updated_at ?: it.created_at) }
                        android.util.Log.d("API_SUCCESS", "공지사항 게시글 수: ${sorted.size}")
                        boardList.clear()
                        boardList.addAll(sorted)
                        boardAdapter.notifyDataSetChanged()
                    } catch (e: Exception) {
                        android.util.Log.e("API_ERROR", "데이터 파싱 오류: ${e.message}")
                        Toast.makeText(this@ClubAnnouncementBoardListActivity,
                            "데이터 파싱 오류: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    android.util.Log.e("API_ERROR", "서버 오류: ${response.code} - $responseBody")
                    Toast.makeText(this@ClubAnnouncementBoardListActivity,
                        "서버 오류: ${response.code} - ${responseBody ?: "응답 없음"}", Toast.LENGTH_LONG).show()
                }
            }
        }

        client.newCall(buildRequest(primaryUrl)).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("API_ERROR", "네트워크 오류: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@ClubAnnouncementBoardListActivity,
                        "네트워크 오류: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful && response.code == 400) {
                    android.util.Log.w("API_RETRY", "400 발생. 대체 URL로 재시도: $fallbackUrl")
                    client.newCall(buildRequest(fallbackUrl)).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            android.util.Log.e("API_ERROR", "재시도 네트워크 오류: ${e.message}")
                            runOnUiThread {
                                Toast.makeText(this@ClubAnnouncementBoardListActivity,
                                    "재시도 네트워크 오류: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        override fun onResponse(call: Call, retryResponse: Response) {
                            handleResponse(retryResponse, fallbackUrl)
                        }
                    })
                } else {
                    handleResponse(response, primaryUrl)
                }
            }
        })
    }

    private fun parseBoardDate(dateString: String?): Long {
        if (dateString.isNullOrBlank()) return 0L
        return try {
            val instant = try {
                java.time.OffsetDateTime.parse(
                    dateString,
                    java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
                ).toInstant()
            } catch (_: Exception) {
                val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
                java.time.LocalDateTime.parse(dateString.substring(0, 26), fmt)
                    .atZone(java.time.ZoneOffset.UTC)
                    .toInstant()
            }
            instant.toEpochMilli()
        } catch (_: Exception) {
            try {
                val input = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", java.util.Locale.getDefault())
                input.parse(dateString)?.time ?: 0L
            } catch (_: Exception) { 0L }
        }
    }

    private fun fetchClubDetail(clubPk: Int) {
        if (clubPk <= 0) {
            return
        }
        val api = ApiClient.getApiService()
        api.getClubDetail(clubPk).enqueue(object : retrofit2.Callback<com.example.myapplication.ClubItem> {
            override fun onResponse(
                call: retrofit2.Call<com.example.myapplication.ClubItem>,
                response: retrofit2.Response<com.example.myapplication.ClubItem>
            ) {
                if (!this@ClubAnnouncementBoardListActivity.isFinishing && !this@ClubAnnouncementBoardListActivity.isDestroyed) {
                    val club = response.body()
                    if (response.isSuccessful && club != null) {
                        bindClubHeader(club)
                    }
                }
            }
            override fun onFailure(
                call: retrofit2.Call<com.example.myapplication.ClubItem>,
                t: Throwable
            ) { /* 무시: 헤더만 미표시 */ }
        })
    }

    private fun bindClubHeader(club: com.example.myapplication.ClubItem) {
        findViewById<TextView>(R.id.tv_club_title)?.text = club.name
        findViewById<TextView>(R.id.tv_welcome)?.text = "🖐🏻 Welcome"
        // Welcome 아래 설명은 short_description으로 표시
        findViewById<TextView>(R.id.tv_club_description)?.text = club.shortDescription
        // 커버 이미지가 API에 없다면 기본 이미지를 유지
    }

    // helper to request similar via OkHttp fallback (클래스 내부 메서드)
    private fun requestSimilarFallback(selectedId: Int, listContainer: android.widget.LinearLayout, showLoading: (Boolean) -> Unit, api: ApiService) {
        val client = ApiClient.createUnsafeOkHttpClient()
        val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
        val url = "$baseUrl/club/$selectedId/similar/"
        android.util.Log.d("AI_PERSONAL", "폴백 요청 URL: $url")
        val req = okhttp3.Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .build()
        client.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this@ClubAnnouncementBoardListActivity, "추천 요청 실패: ${e.message ?: "네트워크 오류"}", Toast.LENGTH_SHORT).show()
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
                    runOnUiThread {
                        if (ids.isEmpty()) {
                            showLoading(false)
                            listContainer.removeAllViews()
                            val empty = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                text = "추천 결과가 없습니다."
                                setTextColor(android.graphics.Color.parseColor("#666666"))
                                textSize = 16f
                                gravity = android.view.Gravity.CENTER
                                setPadding(0, 100, 0, 100)
                            }
                            listContainer.addView(empty)
                        } else {
                            api.getClubList().enqueue(object : retrofit2.Callback<kotlin.collections.List<ClubItem>> {
                                override fun onResponse(
                                    call: retrofit2.Call<kotlin.collections.List<ClubItem>>,
                                    response2: retrofit2.Response<kotlin.collections.List<ClubItem>>
                                ) {
                                    val all2 = response2.body() ?: emptyList()
                                    val matched = all2.filter { ids.contains(it.id) }
                                    listContainer.removeAllViews()
                                    matched.forEach { club ->
                                        val card = android.widget.LinearLayout(this@ClubAnnouncementBoardListActivity).apply {
                                            orientation = android.widget.LinearLayout.VERTICAL
                                            setBackgroundResource(R.drawable.card_box_fafa)
                                            setPadding(40, 40, 40, 40)
                                            val tvName = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                                text = club.name
                                                setTextColor(android.graphics.Color.BLACK)
                                                textSize = 18f
                                                setTypeface(null, android.graphics.Typeface.BOLD)
                                            }
                                            val tvDept = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                                text = "${club.department} / ${club.location}"
                                                setTextColor(android.graphics.Color.parseColor("#666666"))
                                                textSize = 12f
                                            }
                                            val tvDesc = android.widget.TextView(this@ClubAnnouncementBoardListActivity).apply {
                                                text = club.shortDescription
                                                setTextColor(android.graphics.Color.parseColor("#333333"))
                                                textSize = 12f
                                            }
                                            addView(tvName)
                                            addView(tvDept)
                                            addView(tvDesc)
                                            setOnClickListener {
                                                val intent = Intent(this@ClubAnnouncementBoardListActivity, ClubAnnouncementBoardListActivity::class.java)
                                                intent.putExtra("club_pk", club.id)
                                                startActivity(intent)
                                            }
                                        }
                                        val lp = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                                        lp.setMargins(4, 4, 4, 16)
                                        card.layoutParams = lp
                                        listContainer.addView(card)
                                    }
                                    showLoading(false)
                                }
                                override fun onFailure(call: retrofit2.Call<kotlin.collections.List<ClubItem>>, t: Throwable) {
                                    showLoading(false)
                                    Toast.makeText(this@ClubAnnouncementBoardListActivity, "클럽 목록 요청 실패", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    }
                }
            }
        })
    }
}