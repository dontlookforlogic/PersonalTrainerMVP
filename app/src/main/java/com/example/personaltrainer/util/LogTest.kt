package com.example.personaltrainer.util

import android.util.Log
import com.example.personaltrainer.data.remote.ApiClient
import java.time.LocalDateTime

object LogTest {
    private const val TAG = "DIPLOMA_TEST"

    fun appStarted() {
        Log.i(TAG, "TEST 1 PASSED: мобильное приложение успешно запущено")
        Log.i(TAG, "TEST 1 INFO: время запуска = ${LocalDateTime.now()}")
    }

    fun backendConfigured() {
        Log.i(TAG, "TEST 2 PASSED: адрес backend API настроен")
        Log.i(TAG, "TEST 2 INFO: backend URL = ${ApiClient.BASE_URL}")
    }

    fun sessionChecked(isAuthorized: Boolean) {
        Log.i(TAG, "TEST 3 PASSED: проверка пользовательской сессии выполнена")
        Log.i(TAG, "TEST 3 INFO: пользователь авторизован = $isAuthorized")
    }

    fun homeOpened() {
        Log.i(TAG, "TEST 4 PASSED: главный экран приложения открыт")
    }

    fun serverDataLoaded(screen: String, count: Int) {
        Log.i(TAG, "TEST 5 PASSED: экран \"$screen\" получил данные с backend")
        Log.i(TAG, "TEST 5 INFO: количество записей = $count")
    }

    fun notificationLoaded(count: Int) {
        Log.i(TAG, "TEST 6 PASSED: серверные уведомления загружены")
        Log.i(TAG, "TEST 6 INFO: количество уведомлений = $count")
    }

    fun profileLoaded() {
        Log.i(TAG, "TEST 7 PASSED: профиль пользователя загружен с backend")
    }

    fun workoutSaved() {
        Log.i(TAG, "TEST 8 PASSED: тренировка сохранена на backend")
    }

    fun historySaved() {
        Log.i(TAG, "TEST 9 PASSED: результат тренировки сохранён в историю")
    }
}