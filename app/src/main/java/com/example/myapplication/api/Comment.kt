package com.example.myapplication.api

data class Comment(
    val id: Int,
    val author_name: String,
    val author_major: String,
    val content: String,
    val created_at: String,
    val updated_at: String,
    val author: Int,
    val transaction: Int
)

data class CommentRequest(
    val content: String,
    val author: Int
)