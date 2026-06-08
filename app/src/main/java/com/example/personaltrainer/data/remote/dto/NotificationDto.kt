package com.example.personaltrainer.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NotificationDto(
    val id: Int,
    @SerialName("user_id")
    val userId: Int,
    val title: String,
    val text: String,
    val type: String,
    @SerialName("send_status")
    val sendStatus: String,
    @SerialName("created_at")
    val createdAt: String? = null
)