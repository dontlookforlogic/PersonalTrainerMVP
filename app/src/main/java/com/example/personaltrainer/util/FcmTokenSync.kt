package com.example.personaltrainer.util

import android.content.Context
import android.util.Log
import com.example.personaltrainer.data.FirebaseTokenRepository
import com.example.personaltrainer.data.UserSessionStore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FcmTokenSync {

    suspend fun syncToken(context: Context, userSessionStore: UserSessionStore) {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()

            Log.i("FCM_TEST", "Текущий FCM token: $token")

            FirebaseTokenRepository(userSessionStore)
                .sendTokenToBackend(token)

            Log.i("FCM_TEST", "Текущий FCM token отправлен на backend")
        }.onFailure {
            Log.e("FCM_TEST", "Не удалось получить/отправить FCM token", it)
        }
    }
}