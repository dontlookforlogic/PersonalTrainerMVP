package com.example.personaltrainer.data

import com.example.personaltrainer.data.remote.ApiClient
import com.example.personaltrainer.data.remote.dto.LoginRequestDto
import com.example.personaltrainer.data.remote.dto.RegisterRequestDto
import com.example.personaltrainer.data.remote.dto.TokenResponseDto
import com.example.personaltrainer.data.remote.dto.UserDto
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.first

class AuthRepository(
    private val userSessionStore: UserSessionStore
) {

    suspend fun login(email: String, password: String) {
        val response: TokenResponseDto = ApiClient.client.post("${ApiClient.BASE_URL}/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(
                LoginRequestDto(
                    email = email.trim(),
                    password = password
                )
            )
        }.body()

        userSessionStore.saveServerSession(response)
    }

    suspend fun register(name: String, email: String, password: String) {
        val response: TokenResponseDto = ApiClient.client.post("${ApiClient.BASE_URL}/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequestDto(
                    name = name.trim(),
                    email = email.trim(),
                    password = password
                )
            )
        }.body()

        userSessionStore.saveServerSession(response)
    }

    suspend fun refreshCurrentUser(): Boolean {
        val token = userSessionStore.accessToken.first()

        if (token.isNullOrBlank()) {
            return false
        }

        return runCatching {
            val user: UserDto = ApiClient.client.get("${ApiClient.BASE_URL}/users/me") {
                bearerAuth(token)
            }.body()

            userSessionStore.saveServerUser(user)
            true
        }.getOrElse {
            userSessionStore.logout()
            false
        }
    }

    suspend fun logout() {
        userSessionStore.logout()
    }
}