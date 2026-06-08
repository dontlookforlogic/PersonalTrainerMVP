package com.example.personaltrainer.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class WorkoutExerciseDto(
    val id: Int,
    @SerialName("exercise_id")
    val exerciseId: Int,
    @SerialName("order_number")
    val orderNumber: Int,
    val sets: Int,
    val repetitions: Int,
    @SerialName("rest_time_sec")
    val restTimeSec: Int
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class WorkoutDto(
    val id: Int,
    @SerialName("user_id")
    val userId: Int,
    val title: String,
    val description: String,
    @SerialName("days_mask")
    val daysMask: Int,
    val exercises: List<WorkoutExerciseDto> = emptyList()
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class WorkoutExerciseRequestDto(
    @SerialName("exercise_id")
    val exerciseId: Int,
    @SerialName("order_number")
    val orderNumber: Int,
    val sets: Int,
    val repetitions: Int,
    @SerialName("rest_time_sec")
    val restTimeSec: Int = 60
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class WorkoutCreateRequestDto(
    val title: String,
    val description: String = "",
    @SerialName("days_mask")
    val daysMask: Int,
    val exercises: List<WorkoutExerciseRequestDto>
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class WorkoutUpdateRequestDto(
    val title: String,
    val description: String = "",
    @SerialName("days_mask")
    val daysMask: Int,
    val exercises: List<WorkoutExerciseRequestDto>
)