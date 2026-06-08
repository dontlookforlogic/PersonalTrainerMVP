package com.example.personaltrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.personaltrainer.data.HistoryRemoteRepository
import com.example.personaltrainer.data.NotificationPrefsStore
import com.example.personaltrainer.data.NotificationRemoteRepository
import com.example.personaltrainer.data.SettingsStore
import com.example.personaltrainer.data.WorkoutRemoteRepository
import com.example.personaltrainer.data.remote.dto.HistoryResponseDto
import com.example.personaltrainer.data.remote.dto.NotificationDto
import com.example.personaltrainer.data.remote.dto.WorkoutDto
import com.example.personaltrainer.util.NotificationHelper
import com.example.personaltrainer.util.TimeUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.personaltrainer.data.Repository
import java.time.format.DateTimeFormatter

@Composable
fun TodayTab(
    settings: SettingsStore,
    onStartWorkout: (Long) -> Unit,
    onOpenWorkouts: () -> Unit,
    onOpenCatalog: () -> Unit,
    notificationPrefsStore: NotificationPrefsStore,
    notificationRemoteRepository: NotificationRemoteRepository,
    workoutRemoteRepository: WorkoutRemoteRepository,
    historyRemoteRepository: HistoryRemoteRepository,
    repo: Repository
) {
    val today = remember { LocalDate.now() }
    val todayMask = remember { TimeUtils.todayDayMask(today) }

    val restSeconds by settings.restSeconds.collectAsState(initial = 60)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var serverWorkouts by remember { mutableStateOf<List<WorkoutDto>>(emptyList()) }
    var serverHistory by remember { mutableStateOf<List<HistoryResponseDto>>(emptyList()) }

    var serverLoading by rememberSaveable { mutableStateOf(false) }
    var serverError by rememberSaveable { mutableStateOf<String?>(null) }

    var serverNotifications by remember { mutableStateOf<List<NotificationDto>>(emptyList()) }
    var notificationsLoading by rememberSaveable { mutableStateOf(false) }
    var notificationsError by rememberSaveable { mutableStateOf<String?>(null) }

    val todays = remember(serverWorkouts, todayMask) {
        serverWorkouts
            .filter { workout -> workout.daysMask and todayMask != 0 }
            .sortedBy { it.title }
    }

    val lastHistory = remember(serverHistory) {
        serverHistory.maxByOrNull { it.completedAt }
    }

    val lastWorkout = remember(lastHistory, serverWorkouts) {
        val workoutId = lastHistory?.workoutId
        if (workoutId == null) {
            null
        } else {
            serverWorkouts.firstOrNull { it.id == workoutId }
        }
    }

    val dayName = remember(today) {
        today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("ru"))
            .replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString()
            }
    }

    fun loadTodayData() {
        scope.launch {
            serverLoading = true
            serverError = null

            runCatching {
                val workouts = workoutRemoteRepository.getWorkouts()
                val history = historyRemoteRepository.getHistory()
                workouts to history
            }.onSuccess { result ->
                serverWorkouts = result.first
                serverHistory = result.second
            }.onFailure {
                serverError = "Не удалось загрузить данные с сервера"
            }

            serverLoading = false
        }
    }

    fun loadServerNotifications() {
        scope.launch {
            notificationsLoading = true
            notificationsError = null

            runCatching {
                notificationRemoteRepository.getNotifications()
            }.onSuccess { loaded ->
                val sorted = loaded.sortedByDescending { it.id }
                serverNotifications = sorted

                val lastShownId = notificationPrefsStore.getLastShownServerNotificationId()

                val newNotifications = sorted
                    .filter { it.id > lastShownId }
                    .sortedBy { it.id }

                newNotifications.forEach { notification ->
                    NotificationHelper.showServerNotification(
                        context = context,
                        notificationId = notification.id,
                        title = notification.title,
                        text = notification.text
                    )
                }

                val maxId = sorted.maxOfOrNull { it.id }

                if (maxId != null && maxId > lastShownId) {
                    notificationPrefsStore.setLastShownServerNotificationId(maxId)
                }
            }.onFailure {
                notificationsError = "Не удалось загрузить уведомления"
            }

            notificationsLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadTodayData()
        loadServerNotifications()
    }

    LaunchedEffect(todays.size, today) {
        if (todays.isNotEmpty()) {
            val todayKey = today.toString()
            val lastShownDate = notificationPrefsStore.getLastTodayReminderDate()

            if (lastShownDate != todayKey) {
                NotificationHelper.showTodayWorkoutReminder(
                    context = context,
                    workoutCount = todays.size
                )

                notificationPrefsStore.setLastTodayReminderDate(todayKey)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Сегодня", style = MaterialTheme.typography.titleLarge)

                Text(
                    "$dayName • тренировок: ${todays.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        if (serverLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (serverError != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = serverError ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    TextButton(onClick = { loadTodayData() }) {
                        Text("Повторить")
                    }
                }
            }
        }

        ServerNotificationsCard(
            notifications = serverNotifications,
            loading = notificationsLoading,
            error = notificationsError
        )

        OutlinedButton(
            onClick = {
                loadTodayData()
                loadServerNotifications()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Обновить")
        }

        if (todays.isNotEmpty()) {
            Button(
                onClick = {
                    NotificationHelper.showTodayWorkoutReminder(
                        context = context,
                        workoutCount = todays.size
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Напомнить о тренировках")
            }
        }

        if (lastHistory != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Последняя тренировка", style = MaterialTheme.typography.titleMedium)

                    val workoutTitle = lastHistory.workoutTitle.ifBlank {
                        lastWorkout?.title ?: "Удалённая тренировка"
                    }

                    Text(
                        "$workoutTitle • ${formatDuration(lastHistory.durationSec.toLong())}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        formatServerDate(lastHistory.completedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (lastWorkout != null) {
                        Button(onClick = { onStartWorkout(lastWorkout.id.toLong()) }) {
                            Text("Повторить")
                        }
                    } else {
                        Text(
                            "Исходная тренировка была удалена, поэтому повтор недоступен.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Text("Тренировки на сегодня", style = MaterialTheme.typography.titleMedium)

        if (todays.isEmpty() && !serverLoading) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "На сегодня ничего не запланировано.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        "Создай тренировку и назначь день недели — она появится здесь автоматически.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(onClick = onOpenWorkouts) {
                        Text("Перейти к тренировкам")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                todays.forEach { workout ->
                    Card(
                        onClick = { onStartWorkout(workout.id.toLong()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                workout.title,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                "Упражнений: ${workout.exercises.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(onClick = { onStartWorkout(workout.id.toLong()) }) {
                                Text("Начать")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Настройки", style = MaterialTheme.typography.titleMedium)

                Text(
                    "Таймер отдыха: $restSeconds сек.",
                    style = MaterialTheme.typography.bodyMedium
                )

                val sliderValue = restSeconds.coerceIn(10, 180).toFloat()

                Slider(
                    value = sliderValue,
                    onValueChange = { value ->
                        val snapped = (value / 10f).toInt() * 10
                        scope.launch {
                            settings.setRestSeconds(snapped.coerceIn(10, 180))
                        }
                    },
                    valueRange = 10f..180f
                )

                Text(
                    "Подсказка: таймер отдыха запускается автоматически между подходами.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ServerNotificationsCard(
    notifications: List<NotificationDto>,
    loading: Boolean,
    error: String?
) {
    val latestNotification = notifications.maxByOrNull { it.id }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Уведомления",
                style = MaterialTheme.typography.titleMedium
            )

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (latestNotification == null && !loading) {
                Text(
                    "Новых уведомлений нет.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            latestNotification?.let { notification ->
                Text(
                    notification.title,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    notification.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    "Тип: ${notification.type}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: NotificationDto
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            notification.title,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            notification.text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            "Тип: ${notification.type}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    return "${m}м ${s}с"
}

private fun formatServerDate(value: String): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    return runCatching {
        OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(formatter)
    }.getOrElse {
        runCatching {
            LocalDateTime.parse(value)
                .format(formatter)
        }.getOrElse {
            value
        }
    }
}