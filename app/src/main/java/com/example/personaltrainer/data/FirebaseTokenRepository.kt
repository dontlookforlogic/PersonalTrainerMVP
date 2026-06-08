package com.example.personaltrainer.data

import android.annotation.SuppressLint
import com.example.personaltrainer.data.remote.ApiClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NotificationTokenRequestDto(
    val token: String,
    val platform: String = "android"
)

class FirebaseTokenRepository(
    private val userSessionStore: UserSessionStore
) {
    suspend fun sendTokenToBackend(token: String) {
        val accessToken = userSessionStore.accessToken.first()
            ?: return

        ApiClient.client.post("${ApiClient.BASE_URL}/notifications/token") {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(
                NotificationTokenRequestDto(
                    token = token,
                    platform = "android"
                )
            )
        }
    }
}