package com.example.myapplication

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("user_pk")
    val userPk: Int,
    
    @SerializedName("message")
    val message: String? = null
)
