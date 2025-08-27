package com.example.myapplication

data class CommentCreateRequest(
    val content: String,
    val board: Int,
    val author: Int
)


