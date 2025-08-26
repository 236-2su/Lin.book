package com.example.myapplication.api

// 로그인 요청 데이터 클래스
data class LoginRequest(
    val username: String
)

// 로그인 응답 데이터 클래스
data class LoginResponse(
    val pk: Int,
    val username: String,
    val message: String
)
