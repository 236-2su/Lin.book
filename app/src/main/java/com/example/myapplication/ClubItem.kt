package com.example.myapplication

data class ClubItem(
    @com.google.gson.annotations.SerializedName("id")
    val id: Int,
    @com.google.gson.annotations.SerializedName("name")
    val name: String,
    @com.google.gson.annotations.SerializedName("department")
    val department: String,
    @com.google.gson.annotations.SerializedName("major_category")
    val majorCategory: String,
    @com.google.gson.annotations.SerializedName("minor_category")
    val minorCategory: String,
    @com.google.gson.annotations.SerializedName("description")
    val description: String,
    @com.google.gson.annotations.SerializedName("hashtags")
    val hashtags: String,
    @com.google.gson.annotations.SerializedName("created_at")
    val createdAt: String,
    @com.google.gson.annotations.SerializedName("location")
    val location: String,
    @com.google.gson.annotations.SerializedName("short_description")
    val shortDescription: String
)
