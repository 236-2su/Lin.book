package com.example.myapplication

import com.google.gson.annotations.SerializedName

data class TransactionItem(
    @SerializedName("id")
    val id: Int,
    @SerializedName("date")
    val date: String, // ex) "2024-08-21T14:00:00"
    @SerializedName("amount")
    val amount: Long, // ex) 20000
    @SerializedName("type")
    val type: String, // ex) "수입" or "지출"
    @SerializedName("vendor")
    val vendor: String, // ex) "김싸피"
    @SerializedName("description")
    val description: String, // ex) "신입 동아리원..."
    @SerializedName("category")
    val category: String? // ex) "회비"
)
