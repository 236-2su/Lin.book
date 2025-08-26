package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BoardItem(
    val id: Int,
    val type: String,
    val title: String,
    val content: String,
    val views: Int,
    val created_at: String,
    val club: Int,
    @com.google.gson.annotations.SerializedName("likes")
    val likes: String? = "",
    @com.google.gson.annotations.SerializedName("comments")
    val comments: String? = "",
    @com.google.gson.annotations.SerializedName("updated_at")
    val updated_at: String? = "",
    @com.google.gson.annotations.SerializedName("author")
    val author: Int? = 0,
    @com.google.gson.annotations.SerializedName("author_name")
    val author_name: String? = null,
    @com.google.gson.annotations.SerializedName("author_student_short")
    val author_student_short: String? = null,
    @com.google.gson.annotations.SerializedName("author_major")
    val author_major: String? = null
) : Parcelable
