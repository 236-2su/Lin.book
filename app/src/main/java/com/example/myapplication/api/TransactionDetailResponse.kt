package com.example.myapplication.api

data class TransactionDetailResponse(
    val id: Int,
    val created_at: String,
    val updated_at: String,
    val date_time: String, // 예: "2023-02-18T14:22:18+09:00"
    val amount: Long, // 예: -36000
    val type: String, // 예: "비품"
    val payment_method: String, // 예: "카드"
    val description: String, // 예: "꽃다발(소) 4개 × 9,000원"
    val vendor: String, // 예: "플로라블라썸"
    val ledger: Int, // 예: 10
    val receipt: String?, // 영수증 (null 가능)
    val event: Int // 예: 2
)
