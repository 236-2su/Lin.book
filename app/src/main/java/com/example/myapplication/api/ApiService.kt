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

    // 댓글 좋아요
    @POST("/club/{club_pk}/boards/{board_pk}/comments/{id}/like/")
    fun likeComment(
        @Path("club_pk") clubId: Int,
        @Path("board_pk") boardId: Int,
        @Path("id") commentId: Int,
        @Body body: com.example.myapplication.CommentCreateRequest
    ): Call<com.example.myapplication.CommentItem>

    data class LikeRequest(val user_id: Int)
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

    // 사용자 상세
    @GET("user/{id}/")
    fun getUserDetail(@Path("id") userId: Int): Call<com.example.myapplication.UserDetail>
}
