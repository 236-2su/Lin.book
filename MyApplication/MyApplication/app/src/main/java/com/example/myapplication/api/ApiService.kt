package com.example.myapplication.api

import com.example.myapplication.LedgerApiItem
import com.example.myapplication.ClubItem
import com.example.myapplication.TransactionItem
import com.example.myapplication.model.Ledger
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("club/{club_pk}/ledger/")
    fun getLedgerList(@Path("club_pk") clubId: Int): Call<List<LedgerApiItem>>
    
    @GET("club/{club_pk}/ledger/{ledger_pk}/")
    fun getLedgerDetail(@Path("club_pk") clubId: Int, @Path("ledger_pk") ledgerId: Int): Call<Ledger>

    @GET("club/")
    fun getClubList(): Call<List<ClubItem>>
    
    @GET("club/{club_pk}/ledger/{ledger_pk}/transactions/")
    fun getTransactions(
        @Path("club_pk") clubId: Int,
        @Path("ledger_pk") ledgerId: Int
    ): Call<List<TransactionItem>>
}
