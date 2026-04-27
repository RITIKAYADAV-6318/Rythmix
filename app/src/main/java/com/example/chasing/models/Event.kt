package com.example.chasing.models

data class Event(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val description: String = "",
    val location: String = "",
    val createdBy: String = "" // Society Member ID
)