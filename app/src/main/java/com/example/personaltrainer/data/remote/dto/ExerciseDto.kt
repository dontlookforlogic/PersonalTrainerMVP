package com.example.personaltrainer.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ExerciseDto(
    val id: Int,
    val title: String,
    @SerialName("muscle_group")
    val muscleGroup: String,
    val description: String,
    val image: String? = null,
    @SerialName("exercise_type")
    val exerciseType: String,
    @SerialName("is_builtin")
    val isBuiltin: Boolean,
    @SerialName("created_by_user_id")
    val createdByUserId: Int? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ExerciseCreateRequestDto(
    val title: String,
    @SerialName("muscle_group")
    val muscleGroup: String,
    val description: String,
    val image: String? = null,
    @SerialName("exercise_type")
    val exerciseType: String = "strength"
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ExerciseUpdateRequestDto(
    val title: String,
    @SerialName("muscle_group")
    val muscleGroup: String,
    val description: String,
    val image: String? = null,
    @SerialName("exercise_type")
    val exerciseType: String = "strength"
)