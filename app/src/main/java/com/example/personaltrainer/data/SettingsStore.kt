package com.example.personaltrainer.data

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private val restSecondsKey = intPreferencesKey("rest_seconds")

    val restSeconds: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[restSecondsKey] ?: 60
    }

    suspend fun setRestSeconds(seconds: Int) {
        context.dataStore.edit { it[restSecondsKey] = seconds.coerceIn(10, 600) }
    }
}
