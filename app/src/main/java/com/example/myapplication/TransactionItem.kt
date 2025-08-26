package com.example.myapplication

import com.google.gson.annotations.SerializedName

data class TransactionItem(
    @SerializedName("id")
    val id: Int,
    @SerializedName("date_time")
    val dateTime: String?, // ex) "2024-08-21T14:00:00" - nullable로 변경
    @SerializedName("amount")
    val amount: Long, // ex) 20000
    @SerializedName("type")
    val type: String, // ex) "수입" or "지출"
    @SerializedName("payment_method")
    val paymentMethod: String?, // 결제 수단
    @SerializedName("vendor")
    val vendor: String, // ex) "김싸피"
    @SerializedName("description")
    val description: String, // ex) "신입 동아리원..."
    @SerializedName("category")
    val category: String?, // ex) "회비"
    @SerializedName("ledger_id")
    val ledgerId: Int, // 장부 ID
    @SerializedName("club_pk")
    val clubPk: Int, // 클럽 ID
    @SerializedName("receipt")
    val receipt: String? // 영수증 이미지 URL
)
