package com.example.personaltrainer

import android.util.Log
import com.example.personaltrainer.data.FirebaseTokenRepository
import com.example.personaltrainer.data.UserSessionStore
import com.example.personaltrainer.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.i("FCM_TEST", "FCM token обновлён: $token")

        val userSessionStore = UserSessionStore(applicationContext)
        val repository = FirebaseTokenRepository(userSessionStore)

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                repository.sendTokenToBackend(token)
            }.onSuccess {
                Log.i("FCM_TEST", "FCM token отправлен на backend")
            }.onFailure {
                Log.e("FCM_TEST", "Ошибка отправки FCM token на backend", it)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Personal Trainer"

        val text = message.notification?.body
            ?: message.data["text"]
            ?: "Новое уведомление"

        Log.i("FCM_TEST", "Получено push-уведомление: $title / $text")

        NotificationHelper.showServerNotification(
            context = applicationContext,
            notificationId = System.currentTimeMillis().toInt(),
            title = title,
            text = text
        )
    }
}