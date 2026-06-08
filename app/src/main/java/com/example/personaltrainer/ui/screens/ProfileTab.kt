package com.example.personaltrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personaltrainer.data.AuthRepository
import com.example.personaltrainer.data.HistoryRemoteRepository
import com.example.personaltrainer.data.ProfileRepository
import com.example.personaltrainer.data.Repository
import com.example.personaltrainer.data.UserRole
import com.example.personaltrainer.data.UserSessionStore
import com.example.personaltrainer.data.remote.dto.HistoryResponseDto
import com.example.personaltrainer.data.remote.dto.ProfileDto
import com.example.personaltrainer.data.remote.dto.WorkoutStatsResponseDto
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.personaltrainer.util.LogTest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTab(
    userSessionStore: UserSessionStore,
    authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    historyRemoteRepository: HistoryRemoteRepository,
    repo: Repository,
    onOpenDetailedStats: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser by userSessionStore.currentUser.collectAsState(initial = null)

    val workouts by repo.workouts.collectAsState(initial = emptyList())
    val exercises by repo.exercises.collectAsState(initial = emptyList())
    val builtinExercises by repo.builtinExercises.collectAsState(initial = emptyList())
    val customExercises by repo.customExercises.collectAsState(initial = emptyList())
    val localHistory by repo.history.collectAsState(initial = emptyList())

    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    LogTest.profileLoaded()

    var stats by remember { mutableStateOf<WorkoutStatsResponseDto?>(null) }
    var history by remember { mutableStateOf<List<HistoryResponseDto>>(emptyList()) }

    var loading by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }
    var statsLoading by rememberSaveable { mutableStateOf(false) }

    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var statsError by rememberSaveable { mutableStateOf<String?>(null) }

    var heightText by rememberSaveable { mutableStateOf("") }
    var weightText by rememberSaveable { mutableStateOf("") }
    var genderText by rememberSaveable { mutableStateOf("") }
    var birthDateText by rememberSaveable { mutableStateOf("") }
    var goalText by rememberSaveable { mutableStateOf("") }

    fun loadProfile() {
        scope.launch {
            loading = true
            error = null

            runCatching {
                profileRepository.getProfile()
            }.onSuccess { loadedProfile ->
                profile = loadedProfile
                heightText = loadedProfile.height?.toString().orEmpty()
                weightText = loadedProfile.weight?.toString().orEmpty()
                genderText = loadedProfile.gender.orEmpty()
                birthDateText = loadedProfile.birthDate.orEmpty()
                goalText = loadedProfile.goal.orEmpty()
            }.onFailure {
                error = "Не удалось загрузить профиль"
            }

            loading = false
        }
    }

    fun loadWorkoutStats() {
        scope.launch {
            statsLoading = true
            statsError = null

            runCatching {
                val loadedStats = historyRemoteRepository.getStats()
                val loadedHistory = historyRemoteRepository.getHistory()
                loadedStats to loadedHistory
            }.onSuccess { result ->
                stats = result.first
                history = result.second.sortedByDescending { it.completedAt }
            }.onFailure {
                statsError = "Не удалось загрузить статистику тренировок"
            }

            statsLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadProfile()
        loadWorkoutStats()
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Профиль") },
            actions = {
                TextButton(
                    onClick = {
                        loadProfile()
                        loadWorkoutStats()
                    }
                ) {
                    Text("Обновить")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val user = currentUser

            if (user == null) {
                Text("Пользователь не авторизован.")
                return@Column
            }

            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Данные пользователя",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("Имя: ${user.name}")
                    Text("Email: ${user.email}")

                    val roleText = when (user.role) {
                        UserRole.ADMIN -> "Администратор"
                        UserRole.USER -> "Пользователь"
                    }

                    Text("Роль: $roleText")
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Анкета профиля",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (loading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Рост, см") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Вес, кг") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = genderText,
                        onValueChange = { genderText = it },
                        label = { Text("Пол") },
                        placeholder = { Text("male / female") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = birthDateText,
                        onValueChange = { birthDateText = it },
                        label = { Text("Дата рождения") },
                        placeholder = { Text("2000-01-01") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { goalText = it },
                        label = { Text("Цель") },
                        placeholder = { Text("Набор мышечной массы") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        enabled = !saving && !loading,
                        onClick = {
                            scope.launch {
                                saving = true
                                error = null

                                val height = heightText.toIntOrNull()
                                val weight = weightText.toIntOrNull()

                                runCatching {
                                    profileRepository.updateProfile(
                                        height = height,
                                        weight = weight,
                                        gender = genderText.ifBlank { null },
                                        birthDate = birthDateText.ifBlank { null },
                                        goal = goalText.ifBlank { null }
                                    )
                                }.onSuccess { updated ->
                                    profile = updated
                                    error = null
                                }.onFailure {
                                    error = "Не удалось сохранить профиль. Проверьте дату и сервер."
                                }

                                saving = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (saving) "Сохранение..." else "Сохранить профиль")
                    }
                }
            }

            WorkoutStatsCard(
                stats = stats,
                history = history,
                loading = statsLoading,
                error = statsError,
                onRetry = { loadWorkoutStats() },
                onOpenDetailedStats = onOpenDetailedStats
            )

            if (user.role == UserRole.ADMIN) {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Административный раздел",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            "Общая информация о локальных данных приложения",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider()

                        AdminInfoRow(
                            title = "Локальных тренировок",
                            value = workouts.size.toString()
                        )

                        AdminInfoRow(
                            title = "Локальных упражнений",
                            value = exercises.size.toString()
                        )

                        AdminInfoRow(
                            title = "Локальных встроенных упражнений",
                            value = builtinExercises.size.toString()
                        )

                        AdminInfoRow(
                            title = "Локальных пользовательских упражнений",
                            value = customExercises.size.toString()
                        )

                        AdminInfoRow(
                            title = "Локальных записей истории",
                            value = localHistory.size.toString()
                        )

                        HorizontalDivider()

                        Text(
                            "Основные данные приложения уже перенесены на сервер. Этот локальный блок можно позже заменить web-админкой.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        authRepository.logout()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Выйти")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WorkoutStatsCard(
    stats: WorkoutStatsResponseDto?,
    history: List<HistoryResponseDto>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onOpenDetailedStats: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Статистика тренировок",
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

                TextButton(onClick = onRetry) {
                    Text("Повторить")
                }
            }

            val totalWorkouts = stats?.totalWorkouts ?: history.size
            val totalDurationSec = stats?.totalDurationSec ?: history.sumOf { it.durationSec }
            val totalCalories = stats?.totalCalories ?: history.sumOf { it.calories }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileMetricCard(
                    title = "Всего тренировок",
                    value = totalWorkouts.toString(),
                    modifier = Modifier.weight(1f)
                )

                ProfileMetricCard(
                    title = "Общее время",
                    value = formatDuration(totalDurationSec.toLong()),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileMetricCard(
                    title = "Калории",
                    value = totalCalories.toString(),
                    modifier = Modifier.weight(1f)
                )

                ProfileMetricCard(
                    title = "Последних записей",
                    value = history.take(5).size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            Text(
                "Последние тренировки",
                style = MaterialTheme.typography.titleSmall
            )

            if (history.isEmpty() && !loading) {
                Text(
                    "История пока пустая.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                history.take(5).forEach { item ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            item.workoutTitle.ifBlank { "Удалённая тренировка" },
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            "${formatServerDate(item.completedAt)} • ${formatDuration(item.durationSec.toLong())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Button(
                onClick = onOpenDetailedStats,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Подробная статистика")
            }
        }
    }
}

@Composable
private fun ProfileMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                value,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
private fun AdminInfoRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatDuration(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60

    return if (h > 0) {
        "%dч %02dм".format(h, m)
    } else {
        "%dм %02dс".format(m, s)
    }
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