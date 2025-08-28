package com.example.myapplication.api

import com.example.myapplication.LedgerApiItem
import com.example.myapplication.ClubItem
import com.example.myapplication.EventItem
import com.example.myapplication.EventCreateRequest
import com.example.myapplication.TransactionItem
import com.example.myapplication.model.Ledger
import com.example.myapplication.model.Transaction
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import retrofit2.http.PATCH
import retrofit2.http.Query
import retrofit2.http.FieldMap

interface ApiService {
    // 로그인
    data class LoginRequest(val email: String)
    data class LoginResponse(val pk: Int, val club_pks: List<Int>?)

    @POST("user/login/")
    fun login(@Body req: LoginRequest): Call<LoginResponse>

    // Kotlin에서 사용하는 API 응답 모델
    @GET("club/{club_pk}/ledger/")
    fun getLedgerApiList(@Path("club_pk") clubId: Int): Call<List<LedgerApiItem>>
    
    @GET("club/")
    fun getClubList(): Call<List<ClubItem>>
    
    @GET("club/{id}/")
    fun getClubDetail(@Path("id") clubId: Int): Call<ClubItem>
    @retrofit2.http.Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @PUT("club/{id}/")
    fun updateClub(@Path("id") clubId: Int, @Body body: ClubUpdateRequest): Call<ClubItem>

    // 일부 서버에서 JSON 파서를 허용하지 않는 경우를 대비한 폼 인코딩 버전
    @FormUrlEncoded
    @PUT("club/{id}/")
    fun updateClubForm(
        @Path("id") clubId: Int,
        @Field("name") name: String,
        @Field("department") department: String,
        @Field("major_category") majorCategory: String,
        @Field("minor_category") minorCategory: String,
        @Field("description") description: String,
        @Field("hashtags") hashtags: String,
        @Field("location") location: String,
        @Field("short_description") shortDescription: String,
    ): Call<ClubItem>

    // 부분 수정 (PATCH) - 변경된 필드만 전송
    @FormUrlEncoded
    @PATCH("club/{id}/")
    fun patchClubForm(
        @Path("id") clubId: Int,
        @FieldMap fields: Map<String, String>,
    ): Call<ClubItem>

    data class ClubUpdateRequest(
        val name: String,
        val department: String,
        val major_category: String,
        val minor_category: String,
        val description: String,
        val hashtags: String,
        val location: String,
        val short_description: String
    )

    @GET("/club/{club_pk}/ledger/{ledger_pk}/transactions/")
    fun getTransactions(
        @Path("club_pk") clubId: Int,
        @Path("ledger_pk") ledgerId: Int,
        @retrofit2.http.Query("user_pk") userPk: Int
    ): Call<List<TransactionItem>>

    // Java 코드에서 사용하는 API 시그니처들
    @GET("club/{club_pk}/ledger/")
    fun getLedgerList(@Path("club_pk") clubId: Int): Call<List<LedgerApiItem>>
    
    @GET("club/{club_pk}/events/")
    fun getEventList(@Path("club_pk") clubId: Int): Call<List<EventItem>>
    
    @POST("club/{club_pk}/events/")
    fun createEvent(@Path("club_pk") clubId: Int, @Body request: EventCreateRequest): Call<EventItem>

    @GET("club/{club_pk}/ledger/{id}/")
    fun getLedgerDetail(
        @Path("club_pk") clubPk: Int,
        @Path("id") ledgerId: Int
    ): Call<Ledger>

    @GET("/club/{club_pk}/ledger/{ledger_pk}/transactions/")
    fun getTransactionList(
        @Path("club_pk") clubId: Int,
        @Path("ledger_pk") ledgerId: Int
    ): Call<List<Transaction>>

    @GET("/club/{club_pk}/boards/{board_pk}/comments/")
    fun getComments(
        @Path("club_pk") clubId: Int,
        @Path("board_pk") boardId: Int
    ): Call<List<com.example.myapplication.CommentItem>>

    @POST("/club/{club_pk}/boards/{board_pk}/comments/")
    fun postComment(
        @Path("club_pk") clubId: Int,
        @Path("board_pk") boardId: Int,
        @Body body: com.example.myapplication.CommentCreateRequest
    ): Call<com.example.myapplication.CommentItem>

    // 댓글 수정
    @PUT("/club/{club_pk}/boards/{board_pk}/comments/{id}/")
    fun updateComment(
        @Path("club_pk") clubId: Int,
        @Path("board_pk") boardId: Int,
        @Path("id") commentId: Int,
        @Body body: com.example.myapplication.CommentCreateRequest
    ): Call<com.example.myapplication.CommentItem>

    // 댓글 삭제
    @DELETE("/club/{club_pk}/boards/{board_pk}/comments/{id}/")
    fun deleteComment(
        @Path("club_pk") clubId: Int,
        @Path("board_pk") boardId: Int,
        @Path("id") commentId: Int
    ): Call<okhttp3.ResponseBody>

    // 댓글 좋아요 (백엔드 스펙: { user_id })
    @retrofit2.http.Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @POST("/club/{club_pk}/boards/{board_pk}/comments/{id}/like/")
    fun likeComment(
        @Path("club_pk") clubId: Int,
        @Path("board_pk") boardId: Int,
        @Path("id") commentId: Int,
        @Body body: LikeRequest
    ): Call<okhttp3.ResponseBody>

    data class LikeRequest(val user_id: Int)

    @retrofit2.http.Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @POST("/club/{club_pk}/boards/{id}/like/")
    fun toggleBoardLike(
        @Path("club_pk") clubId: Int,
        @Path("id") boardId: Int,
        @Body body: LikeRequest
    ): Call<okhttp3.ResponseBody>

    @GET("/club/{club_pk}/boards/{id}/")
    fun getBoardDetail(
        @Path("club_pk") clubId: Int,
        @Path("id") boardId: Int
    ): Call<com.example.myapplication.BoardItem>

    // 게시글 삭제
    @DELETE("/club/{club_pk}/boards/{id}/")
    fun deleteBoard(
        @Path("club_pk") clubId: Int,
        @Path("id") boardId: Int
    ): Call<okhttp3.ResponseBody>

    // 검색 추천 (유사 동아리) - 쿼리 기반
    data class SimilarClubItem(
        val id: Int,
        val score_hint: Float?,
        val snippet: String?
    )

    @GET("club/similar/")
    fun getSimilarClubs(
        @Query("query") query: String
    ): Call<List<SimilarClubItem>>

    @GET("club/{id}/similar/")
    fun getSimilarClubsByClub(
        @Path("id") clubId: Int
    ): Call<List<SimilarClubItem>>

    // 사용자 상세
    @GET("user/{id}/")
    fun getUserDetail(@Path("id") userId: Int): Call<com.example.myapplication.UserDetail>

    // 사용자 목록 조회
    @GET("user/")
    fun getUserList(): Call<List<com.example.myapplication.UserResponse>>

    // 클럽 멤버 목록 조회
    @GET("club/{club_pk}/members/")
    fun getClubMembers(@Path("club_pk") clubId: Int): Call<List<com.example.myapplication.MemberResponse>>

    // 가입 대기 멤버 목록 조회
    @GET("club/{club_pk}/members/waiting/")
    fun getWaitingMembers(@Path("club_pk") clubId: Int): Call<List<com.example.myapplication.MemberResponse>>

    // AI 리포트 관련 API
    // 월간 리포트 생성
    @POST("report/clubs/{club_pk}/ledgers/{ledger_pk}/reports/monthly/")
    fun createMonthlyReport(
        @Path("club_pk") clubId: Int,
        @Path("ledger_pk") ledgerId: Int,
        @retrofit2.http.Query("year") year: Int,
        @retrofit2.http.Query("month") month: Int
    ): Call<AIReportResponse>

    // 연간 리포트 생성
    @POST("report/clubs/{club_pk}/ledgers/{ledger_pk}/reports/yearly/")
    fun createYearlyReport(
        @Path("club_pk") clubId: Int,
        @Path("ledger_pk") ledgerId: Int,
        @retrofit2.http.Query("year") year: Int
    ): Call<AIReportResponse>

    // 유사 동아리 비교 리포트 생성
    @POST("report/similar-clubs/club/{club_id}/year/{year}/")
    fun createSimilarClubsReport(
        @Path("club_id") clubId: Int,
        @Path("year") year: Int
    ): Call<AIReportResponse>

    // 월간 리포트 목록 조회
    @GET("report/clubs/{club_pk}/ledgers/{ledger_pk}/reports/monthly/")
    fun getMonthlyReports(
        @Path("club_pk") clubId: Int,
        @Path("ledger_pk") ledgerId: Int,
        @retrofit2.http.Query("year") year: Int,
        @retrofit2.http.Query("month") month: Int
    ): Call<List<BackendReportItem>>

    // 연간 리포트 목록 조회
    @GET("report/clubs/{club_pk}/ledgers/{ledger_pk}/reports/yearly/")
    fun getYearlyReports(
        @Path("club_pk") clubId: Int,
        @Path("ledger_pk") ledgerId: Int,
        @retrofit2.http.Query("year") year: Int
    ): Call<List<BackendReportItem>>

    // 백엔드 실제 응답 구조 (리포트 생성 시)
    data class AIReportResponse(
        val ledger_id: Int,
        val club_id: Int,
        val year: Int,
        val month: Int?,
        val period: Map<String, String>?,
        val summary: Map<String, Int>,
        val by_type: List<Map<String, Any>>,
        val by_payment_method: List<Map<String, Any>>,
        val by_event: List<Map<String, Any>>,
        val daily_series: List<Map<String, Any>>?
    )

    // 백엔드 저장된 리포트 데이터 클래스
    data class BackendReportItem(
        val id: Int,
        val ledger: Int,
        val title: String,
        val content: Map<String, Any> // JSONField는 Map으로 받음
    )

    // --- Accounts ---
    data class AccountItem(
        val id: Int,
        val user: Int,
        val amount: Long?,
        val code: String,
        val created_at: String,
        val user_name: String
    )

    @GET("user/{user_pk}/accounts/")
    fun getAccounts(
        @Path("user_pk") userPk: Int
    ): Call<List<AccountItem>>

    @GET("user/{user_pk}/accounts/{accounts_id}/")
    fun getAccountDetail(
        @Path("user_pk") userPk: Int,
        @Path("accounts_id") accountId: Int
    ): Call<AccountItem>
}
