package com.example.myapplication

// API 응답을 위한 장부 데이터 클래스
data class LedgerApiItem(
    val id: Int,
    val name: String,
    val description: String?,
    val created_at: String,
    val updated_at: String
)
