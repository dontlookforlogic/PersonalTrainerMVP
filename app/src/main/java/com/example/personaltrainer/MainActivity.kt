package com.example.personaltrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.personaltrainer.ui.AppRoot
import com.example.personaltrainer.ui.theme.PersonaltrainerTheme
import com.example.personaltrainer.util.AppLog
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.personaltrainer.util.NotificationHelper

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            AppLog.i("Notifications: разрешение на уведомления выдано")
        } else {
            AppLog.i("Notifications: разрешение на уведомления отклонено")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.i("MainActivity: onCreate() — запуск приложения")
        NotificationHelper.createChannels(this)
        requestNotificationPermissionIfNeeded()

        setContent {
            AppLog.i("Compose: setContent() — UI инициализирован")
            PersonaltrainerTheme { AppRoot() }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
