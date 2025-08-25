package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CommentItem(
    val id: Int,
    val likes: String?,
    val content: String,
    val created_at: String,
    val updated_at: String,
    val board: Int,
    val author: Int
) : Parcelable


