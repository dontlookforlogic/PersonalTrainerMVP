package com.example.personaltrainer.data

import com.example.personaltrainer.data.remote.ApiClient
import com.example.personaltrainer.data.remote.dto.NotificationDto
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import kotlinx.coroutines.flow.first

class NotificationRemoteRepository(
    private val userSessionStore: UserSessionStore
) {
    suspend fun getNotifications(): List<NotificationDto> {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        return ApiClient.client.get("${ApiClient.BASE_URL}/notifications") {
            bearerAuth(token)
        }.body()
    }
}