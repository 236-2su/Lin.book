package com.example.myapplication

import com.google.gson.annotations.SerializedName

data class EventTransactionItem(
    @SerializedName("id")
    val id: Int,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    @SerializedName("date_time")
    val dateTime: String?,
    @SerializedName("amount")
    val amount: Long,
    @SerializedName("type")
    val type: String,
    @SerializedName("payment_method")
    val paymentMethod: String?,
    @SerializedName("description")
    val description: String,
    @SerializedName("vendor")
    val vendor: String,
    @SerializedName("ledger")
    val ledger: Int,
    @SerializedName("receipt")
    val receipt: Int?,
    @SerializedName("event")
    val event: Int
)