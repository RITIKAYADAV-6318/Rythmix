package com.example.chasing.models

data class Society(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val collegeName: String = "",
    val createdBy: String = "",
    val members: Map<String, Boolean> = emptyMap()
)