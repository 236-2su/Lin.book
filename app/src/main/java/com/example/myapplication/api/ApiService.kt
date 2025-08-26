package com.example.myapplication.api

import com.example.myapplication.LedgerApiItem
import com.example.myapplication.ClubItem
import com.example.myapplication.TransactionItem
import com.example.myapplication.model.Ledger
import com.example.myapplication.model.Transaction
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path

interface ApiService {
    // 로그인 API
    @POST("auth/login/")
    fun login(@Body loginRequest: LoginRequest): Call<LoginResponse>
    
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

    @POST("/club/{club_pk}/boards/{id}/like/")
    fun toggleBoardLike(
        @Path("club_pk") clubId: Int,
        @Path("id") boardId: Int
    ): Call<okhttp3.ResponseBody>

    @GET("/club/{club_pk}/boards/{id}/")
    fun getBoardDetail(
        @Path("club_pk") clubId: Int,
        @Path("id") boardId: Int
    ): Call<com.example.myapplication.BoardItem>
}
