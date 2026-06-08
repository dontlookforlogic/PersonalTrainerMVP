package com.example.personaltrainer.data

import com.example.personaltrainer.data.remote.ApiClient
import com.example.personaltrainer.data.remote.dto.ActivityCreateRequestDto
import com.example.personaltrainer.data.remote.dto.ActivityDto
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.first

class ActivityRemoteRepository(
    private val userSessionStore: UserSessionStore
) {
    suspend fun getActivity(): List<ActivityDto> {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        return ApiClient.client.get("${ApiClient.BASE_URL}/activity") {
            bearerAuth(token)
        }.body()
    }

    suspend fun getTodayActivity(): ActivityDto? {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        return ApiClient.client.get("${ApiClient.BASE_URL}/activity/today") {
            bearerAuth(token)
        }.body()
    }

    suspend fun saveActivity(
        date: String,
        stepCount: Int,
        calories: Int,
        activityTimeMin: Int
    ): ActivityDto {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        return ApiClient.client.post("${ApiClient.BASE_URL}/activity") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                ActivityCreateRequestDto(
                    date = date,
                    stepCount = stepCount,
                    calories = calories,
                    activityTimeMin = activityTimeMin
                )
            )
        }.body()
    }
}