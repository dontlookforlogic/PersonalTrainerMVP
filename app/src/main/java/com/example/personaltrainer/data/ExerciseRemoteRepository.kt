package com.example.personaltrainer.data

import com.example.personaltrainer.data.remote.ApiClient
import com.example.personaltrainer.data.remote.dto.ExerciseCreateRequestDto
import com.example.personaltrainer.data.remote.dto.ExerciseDto
import com.example.personaltrainer.data.remote.dto.ExerciseUpdateRequestDto
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.first

class ExerciseRemoteRepository(
    private val userSessionStore: UserSessionStore
) {
    suspend fun getExercises(): List<ExerciseDto> {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        return ApiClient.client.get("${ApiClient.BASE_URL}/exercises") {
            bearerAuth(token)
        }.body()
    }

    suspend fun getMyExercises(): List<ExerciseDto> {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        return ApiClient.client.get("${ApiClient.BASE_URL}/exercises/my") {
            bearerAuth(token)
        }.body()
    }

    suspend fun createMyExercise(
        title: String,
        muscleGroup: String,
        description: String
    ): ExerciseDto {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        return ApiClient.client.post("${ApiClient.BASE_URL}/exercises/my") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                ExerciseCreateRequestDto(
                    title = title,
                    muscleGroup = muscleGroup,
                    description = description,
                    image = null,
                    exerciseType = "strength"
                )
            )
        }.body()
    }

    suspend fun updateMyExercise(
        exerciseId: Int,
        title: String,
        muscleGroup: String,
        description: String
    ): ExerciseDto {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        return ApiClient.client.put("${ApiClient.BASE_URL}/exercises/my/$exerciseId") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                ExerciseUpdateRequestDto(
                    title = title,
                    muscleGroup = muscleGroup,
                    description = description,
                    image = null,
                    exerciseType = "strength"
                )
            )
        }.body()
    }

    suspend fun deleteMyExercise(exerciseId: Int) {
        val token = userSessionStore.accessToken.first()
            ?: error("Пользователь не авторизован")

        ApiClient.client.delete("${ApiClient.BASE_URL}/exercises/my/$exerciseId") {
            bearerAuth(token)
        }
    }
}