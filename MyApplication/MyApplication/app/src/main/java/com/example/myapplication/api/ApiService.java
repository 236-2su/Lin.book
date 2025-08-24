package com.example.myapplication.api;

import com.example.myapplication.model.Ledger;
import com.example.myapplication.model.Transaction;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    
    @GET("club/{club_pk}/ledger/")
    Call<List<Ledger>> getLedgerList(@Path("club_pk") int clubPk);
    
    @GET("club/{club_pk}/ledger/{id}/")
    Call<Ledger> getLedgerDetail(@Path("club_pk") int clubPk, @Path("id") int ledgerId);
    
    @GET("club/{club_pk}/ledger/{ledger_pk}/transactions/")
    Call<List<Transaction>> getTransactionList(@Path("club_pk") int clubPk, @Path("ledger_pk") int ledgerPk);
    
}