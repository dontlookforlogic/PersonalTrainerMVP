package com.example.personaltrainer.data

data class CurrentUser(
    val name: String,
    val email: String,
    val role: UserRole
)

enum class UserRole {
    USER,
    ADMIN
}