package com.example.personaltrainer.data

import com.example.personaltrainer.data.remote.ApiClient
import com.example.personaltrainer.data.remote.dto.WorkoutCreateRequestDto
import com.example.personaltrainer.data.remote.dto.WorkoutDto
import com.example.personaltrainer.data.remote.dto.WorkoutExerciseRequestDto
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.first
import com.example.personaltrainer.data.remote.dto.WorkoutUpdateRequestDto
import io.ktor.client.request.put

class WorkoutRemoteRepository(
    private val userSessionStore: UserSessionStore
) {
    suspend fun getWorkouts(): List<WorkoutDto> {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        return ApiClient.client.get("${ApiClient.BASE_URL}/workouts") {
            bearerAuth(token)
        }.body()
    }

    suspend fun createWorkout(
        title: String,
        daysMask: Int,
        exercises: List<WorkoutExerciseRequestDto>
    ): WorkoutDto {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        return ApiClient.client.post("${ApiClient.BASE_URL}/workouts") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                WorkoutCreateRequestDto(
                    title = title,
                    description = "",
                    daysMask = daysMask,
                    exercises = exercises
                )
            )
        }.body()
    }

    suspend fun deleteWorkout(workoutId: Int) {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        ApiClient.client.delete("${ApiClient.BASE_URL}/workouts/$workoutId") {
            bearerAuth(token)
        }
    }

    suspend fun getWorkout(workoutId: Int): WorkoutDto {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        return ApiClient.client.get("${ApiClient.BASE_URL}/workouts/$workoutId") {
            bearerAuth(token)
        }.body()
    }

    suspend fun updateWorkout(
        workoutId: Int,
        title: String,
        daysMask: Int,
        exercises: List<WorkoutExerciseRequestDto>
    ): WorkoutDto {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        return ApiClient.client.put("${ApiClient.BASE_URL}/workouts/$workoutId") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                WorkoutUpdateRequestDto(
                    title = title,
                    description = "",
                    daysMask = daysMask,
                    exercises = exercises
                )
            )
        }.body()
    }
}