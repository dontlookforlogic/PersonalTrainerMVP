package com.example.personaltrainer.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.personaltrainer.data.remote.dto.TokenResponseDto
import com.example.personaltrainer.data.remote.dto.UserDto

private val Context.userSessionDataStore by preferencesDataStore(name = "user_session")

class UserSessionStore(private val context: Context) {

    private val isLoggedInKey = booleanPreferencesKey("is_logged_in")
    private val userNameKey = stringPreferencesKey("user_name")
    private val userEmailKey = stringPreferencesKey("user_email")
    private val userRoleKey = stringPreferencesKey("user_role")
    private val accessTokenKey = stringPreferencesKey("access_token")

    val accessToken: Flow<String?> = context.userSessionDataStore.data.map { prefs ->
        prefs[accessTokenKey]
    }

    val currentUser: Flow<CurrentUser?> = context.userSessionDataStore.data.map { prefs ->
        val isLoggedIn = prefs[isLoggedInKey] ?: false

        if (!isLoggedIn) {
            null
        } else {
            val name = prefs[userNameKey] ?: "Пользователь"
            val email = prefs[userEmailKey] ?: ""
            val role = when (prefs[userRoleKey]) {
                UserRole.ADMIN.name -> UserRole.ADMIN
                else -> UserRole.USER
            }

            CurrentUser(
                name = name,
                email = email,
                role = role
            )
        }
    }

    suspend fun login(email: String, password: String) {
        val cleanEmail = email.trim()

        val role = if (cleanEmail.equals("admin@test.com", ignoreCase = true)) {
            UserRole.ADMIN
        } else {
            UserRole.USER
        }

        val name = cleanEmail.substringBefore("@").ifBlank {
            "Пользователь"
        }

        context.userSessionDataStore.edit { prefs ->
            prefs[isLoggedInKey] = true
            prefs[userNameKey] = name
            prefs[userEmailKey] = cleanEmail
            prefs[userRoleKey] = role.name
        }
    }

    suspend fun register(name: String, email: String, password: String) {
        val cleanName = name.trim().ifBlank { "Пользователь" }
        val cleanEmail = email.trim()

        val role = if (cleanEmail.equals("admin@test.com", ignoreCase = true)) {
            UserRole.ADMIN
        } else {
            UserRole.USER
        }

        context.userSessionDataStore.edit { prefs ->
            prefs[isLoggedInKey] = true
            prefs[userNameKey] = cleanName
            prefs[userEmailKey] = cleanEmail
            prefs[userRoleKey] = role.name
        }
    }

    suspend fun logout() {
        context.userSessionDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun saveServerSession(response: TokenResponseDto) {
        val role = when (response.user.role) {
            UserRole.ADMIN.name -> UserRole.ADMIN
            else -> UserRole.USER
        }

        context.userSessionDataStore.edit { prefs ->
            prefs[isLoggedInKey] = true
            prefs[userNameKey] = response.user.name
            prefs[userEmailKey] = response.user.email
            prefs[userRoleKey] = role.name
            prefs[accessTokenKey] = response.accessToken
        }
    }

    suspend fun saveServerUser(user: UserDto) {
        val role = when (user.role) {
            UserRole.ADMIN.name -> UserRole.ADMIN
            else -> UserRole.USER
        }

        context.userSessionDataStore.edit { prefs ->
            prefs[isLoggedInKey] = true
            prefs[userNameKey] = user.name
            prefs[userEmailKey] = user.email
            prefs[userRoleKey] = role.name
        }
    }
}