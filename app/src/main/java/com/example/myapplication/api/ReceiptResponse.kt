package com.example.myapplication.api

data class ReceiptResponse(
    val id: Int,
    val image: String, // 영수증 이미지 URL
    val created_at: String,
    val updated_at: String
)
