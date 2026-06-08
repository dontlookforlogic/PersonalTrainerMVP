package com.example.personaltrainer.data

import com.example.personaltrainer.data.remote.ApiClient
import com.example.personaltrainer.data.remote.dto.HistoryCreateRequestDto
import com.example.personaltrainer.data.remote.dto.HistoryResponseDto
import com.example.personaltrainer.data.remote.dto.WorkoutStatsResponseDto
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.first

class HistoryRemoteRepository(
    private val userSessionStore: UserSessionStore
) {
    suspend fun getHistory(): List<HistoryResponseDto> {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        return ApiClient.client.get("${ApiClient.BASE_URL}/history") {
            bearerAuth(token)
        }.body()
    }

    suspend fun getStats(): WorkoutStatsResponseDto {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        return ApiClient.client.get("${ApiClient.BASE_URL}/history/stats") {
            bearerAuth(token)
        }.body()
    }

    suspend fun saveHistory(
        workoutId: Int?,
        workoutTitle: String,
        durationSec: Int,
        calories: Int = 0
    ): HistoryResponseDto {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        return ApiClient.client.post("${ApiClient.BASE_URL}/history") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                HistoryCreateRequestDto(
                    workoutId = workoutId,
                    workoutTitle = workoutTitle,
                    durationSec = durationSec,
                    calories = calories,
                    status = "completed"
                )
            )
        }.body()
    }
}