package com.example.personaltrainer.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ActivityDto(
    val id: Int,
    @SerialName("user_id")
    val userId: Int,
    val date: String,
    @SerialName("step_count")
    val stepCount: Int,
    val calories: Int,
    @SerialName("activity_time_min")
    val activityTimeMin: Int
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ActivityCreateRequestDto(
    val date: String,
    @SerialName("step_count")
    val stepCount: Int,
    val calories: Int,
    @SerialName("activity_time_min")
    val activityTimeMin: Int
)