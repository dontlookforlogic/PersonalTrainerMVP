package com.example.personaltrainer.data

import com.example.personaltrainer.data.remote.ApiClient
import com.example.personaltrainer.data.remote.dto.ProfileDto
import com.example.personaltrainer.data.remote.dto.ProfileUpdateRequestDto
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.first

class ProfileRepository(
    private val userSessionStore: UserSessionStore
) {

    suspend fun getProfile(): ProfileDto {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        return ApiClient.client.get("${ApiClient.BASE_URL}/profile") {
            bearerAuth(token)
        }.body()
    }

    suspend fun updateProfile(
        height: Int?,
        weight: Int?,
        gender: String?,
        birthDate: String?,
        goal: String?
    ): ProfileDto {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        return ApiClient.client.put("${ApiClient.BASE_URL}/profile") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                ProfileUpdateRequestDto(
                    height = height,
                    weight = weight,
                    gender = gender,
                    birthDate = birthDate,
                    goal = goal
                )
            )
        }.body()
    }
}