package com.example.chasing.models

data class MusicPost(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfilePic: String = "",
    val title: String = "",
    val fileUrl: String = "",
    val fileType: String = "", // "audio" or "document"
    val timestamp: Long = 0,
    val likes: Map<String, Boolean> = emptyMap(),
    val comments: Map<String, Comment> = emptyMap()
)

data class Comment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val timestamp: Long = 0
)