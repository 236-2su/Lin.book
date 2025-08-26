package com.example.myapplication

import com.google.gson.annotations.SerializedName

// API 응답을 위한 장부 데이터 클래스
data class LedgerApiItem(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)
