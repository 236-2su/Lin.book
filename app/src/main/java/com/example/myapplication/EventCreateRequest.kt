package com.example.myapplication

data class EventCreateRequest(
    val name: String,
    val start_date: String,
    val end_date: String,
    val description: String,
    val budget: Int
)