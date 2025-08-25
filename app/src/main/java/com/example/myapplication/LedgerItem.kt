package com.example.myapplication

import java.io.Serializable

data class LedgerItem(
    val type: String, // "수입" 또는 "지출"
    val tags: List<String>,
    val date: String,
    val amount: String,
    val author: String,
    val memo: String,
    val hasReceipt: Boolean
) : Serializable
