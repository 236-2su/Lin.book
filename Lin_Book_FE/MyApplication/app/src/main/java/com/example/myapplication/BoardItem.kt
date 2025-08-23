package com.example.myapplication

data class BoardItem(
    val id: Int,
    val type: String,
    val title: String,
    val content: String,
    val views: Int,
    val created_at: String,
    val club: Int
)
