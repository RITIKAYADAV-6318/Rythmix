package com.example.chasing.models

data class User(
    val id: String = "",
    val name: String = "",
    val collegeId: String = "",
    val email: String = "",
    val role: String = "", // "student" or "society_member"
    val profilePic: String = "" // URL from Firebase Storage
)