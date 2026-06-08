package com.example.personaltrainer.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ProfileDto(
    val id: Int,
    @SerialName("user_id")
    val userId: Int,
    val height: Int? = null,
    val weight: Int? = null,
    val gender: String? = null,
    @SerialName("birth_date")
    val birthDate: String? = null,
    val goal: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ProfileUpdateRequestDto(
    val height: Int? = null,
    val weight: Int? = null,
    val gender: String? = null,
    @SerialName("birth_date")
    val birthDate: String? = null,
    val goal: String? = null
)