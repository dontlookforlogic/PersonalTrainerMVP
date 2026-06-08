package com.example.personaltrainer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import androidx.datastore.preferences.core.intPreferencesKey

private val Context.notificationPrefsDataStore by preferencesDataStore(name = "notification_prefs")

class NotificationPrefsStore(private val context: Context) {

    private val lastTodayReminderDateKey = stringPreferencesKey("last_today_reminder_date")
    private val lastShownServerNotificationIdKey = intPreferencesKey("last_shown_server_notification_id")

    suspend fun getLastTodayReminderDate(): String? {
        val prefs = context.notificationPrefsDataStore.data.first()
        return prefs[lastTodayReminderDateKey]
    }

    suspend fun setLastTodayReminderDate(date: String) {
        context.notificationPrefsDataStore.edit { prefs ->
            prefs[lastTodayReminderDateKey] = date
        }
    }

    suspend fun getLastShownServerNotificationId(): Int {
        val prefs = context.notificationPrefsDataStore.data.first()
        return prefs[lastShownServerNotificationIdKey] ?: 0
    }

    suspend fun setLastShownServerNotificationId(id: Int) {
        context.notificationPrefsDataStore.edit { prefs ->
            prefs[lastShownServerNotificationIdKey] = id
        }
    }
}