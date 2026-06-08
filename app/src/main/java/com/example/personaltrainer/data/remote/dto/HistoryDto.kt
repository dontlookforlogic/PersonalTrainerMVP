package com.example.personaltrainer.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class HistoryCreateRequestDto(
    @SerialName("workout_id")
    val workoutId: Int? = null,
    @SerialName("workout_title")
    val workoutTitle: String,
    @SerialName("duration_sec")
    val durationSec: Int,
    val calories: Int = 0,
    val status: String = "completed"
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class HistoryResponseDto(
    val id: Int,
    @SerialName("user_id")
    val userId: Int,
    @SerialName("workout_id")
    val workoutId: Int? = null,
    @SerialName("workout_title")
    val workoutTitle: String,
    @SerialName("completed_at")
    val completedAt: String,
    @SerialName("duration_sec")
    val durationSec: Int,
    val status: String,
    val calories: Int
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class WorkoutStatsResponseDto(
    @SerialName("total_workouts")
    val totalWorkouts: Int,
    @SerialName("total_duration_sec")
    val totalDurationSec: Int,
    @SerialName("total_calories")
    val totalCalories: Int
)