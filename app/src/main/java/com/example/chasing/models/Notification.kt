package com.example.chasing.models

data class Notification(
    val id: String = "",
    val toUserId: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val type: String = "", // "tag" or "approval_request" or "approved"
    val message: String = "",
    val societyId: String = "",
    val societyName: String = "",
    val timestamp: Long = 0,
    val status: String = "pending" // "pending", "approved", "rejected"
)