package com.example.personaltrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personaltrainer.data.ActivityRemoteRepository
import com.example.personaltrainer.data.remote.dto.ActivityDto
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTab(
    activityRemoteRepository: ActivityRemoteRepository
) {
    val scope = rememberCoroutineScope()

    var activityList by remember { mutableStateOf<List<ActivityDto>>(emptyList()) }
    var todayActivity by remember { mutableStateOf<ActivityDto?>(null) }

    var loading by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun loadActivity() {
        scope.launch {
            loading = true
            error = null

            runCatching {
                val today = activityRemoteRepository.getTodayActivity()
                val all = activityRemoteRepository.getActivity()
                today to all
            }.onSuccess { result ->
                todayActivity = result.first
                activityList = result.second.sortedByDescending { it.date }
            }.onFailure {
                error = "Не удалось загрузить активность"
            }

            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadActivity()
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Активность") },
            actions = {
                TextButton(onClick = { loadActivity() }) {
                    Text("Обновить")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loading) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            error?.let { message ->
                item {
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
                                text = message,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )

                            TextButton(onClick = { loadActivity() }) {
                                Text("Повторить")
                            }
                        }
                    }
                }
            }

            item {
                TodayActivityCard(todayActivity = todayActivity)
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Тестовое заполнение",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            "Пока активность добавляется вручную. Позже можно подключить Health Connect или Firebase.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            enabled = !saving,
                            onClick = {
                                scope.launch {
                                    saving = true
                                    error = null

                                    val today = LocalDate.now().toString()

                                    runCatching {
                                        activityRemoteRepository.saveActivity(
                                            date = today,
                                            stepCount = 6500,
                                            calories = 320,
                                            activityTimeMin = 45
                                        )
                                    }.onSuccess {
                                        loadActivity()
                                    }.onFailure {
                                        error = "Не удалось сохранить активность"
                                    }

                                    saving = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (saving) "Сохранение..." else "Добавить тестовую активность за сегодня")
                        }
                    }
                }
            }

            item {
                Text(
                    "История активности",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (activityList.isEmpty() && !loading) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Активность пока пустая.")
                        }
                    }
                }
            } else {
                items(activityList, key = { it.id }) { activity ->
                    ActivityHistoryCard(activity)
                }
            }
        }
    }
}

@Composable
private fun TodayActivityCard(
    todayActivity: ActivityDto?
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Сегодня",
                style = MaterialTheme.typography.titleMedium
            )

            if (todayActivity == null) {
                Text(
                    "За сегодня пока нет данных активности.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActivityMetricCard(
                        title = "Шаги",
                        value = todayActivity.stepCount.toString(),
                        modifier = Modifier.weight(1f)
                    )

                    ActivityMetricCard(
                        title = "Калории",
                        value = todayActivity.calories.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActivityMetricCard(
                        title = "Активное время",
                        value = "${todayActivity.activityTimeMin} мин",
                        modifier = Modifier.weight(1f)
                    )

                    ActivityMetricCard(
                        title = "Дата",
                        value = todayActivity.date,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityMetricCard(
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
private fun ActivityHistoryCard(
    activity: ActivityDto
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                activity.date,
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                "Шаги: ${activity.stepCount}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "Калории: ${activity.calories}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "Активное время: ${activity.activityTimeMin} мин",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}