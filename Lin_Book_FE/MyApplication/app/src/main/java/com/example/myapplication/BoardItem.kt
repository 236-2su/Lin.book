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
    val club: Int
) : Parcelable
