package com.example.chasing.models

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderProfilePic: String = "", // Added for profile pics in chat
    val message: String = "",
    val timestamp: Long = 0
)