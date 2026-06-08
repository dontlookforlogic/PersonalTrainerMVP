package com.example.personaltrainer.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    @SerialName("is_active")
    val isActive: Boolean
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class TokenResponseDto(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String = "bearer",
    val user: UserDto
)