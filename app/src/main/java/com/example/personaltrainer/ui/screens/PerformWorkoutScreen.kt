@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.personaltrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personaltrainer.data.ExerciseRemoteRepository
import com.example.personaltrainer.data.HistoryRemoteRepository
import com.example.personaltrainer.data.Repository
import com.example.personaltrainer.data.SettingsStore
import com.example.personaltrainer.data.WorkoutRemoteRepository
import com.example.personaltrainer.util.TimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

private data class Step(
    val exerciseId: Int,
    val name: String,
    val sets: Int,
    val reps: Int
)

@Composable
fun PerformWorkoutScreen(
    repo: Repository,
    settings: SettingsStore,
    workoutRemoteRepository: WorkoutRemoteRepository,
    exerciseRemoteRepository: ExerciseRemoteRepository,
    historyRemoteRepository: HistoryRemoteRepository,
    workoutId: Long,
    onFinished: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val restSecondsSetting by settings.restSeconds.collectAsState(initial = 60)

    var steps by remember { mutableStateOf<List<Step>>(emptyList()) }
    var workoutName by remember { mutableStateOf("Тренировка") }

    var currentIndex by remember { mutableStateOf(0) }
    var currentSet by remember { mutableStateOf(1) }
    var restLeft by remember { mutableStateOf(0) }

    var startedAt by remember { mutableStateOf<Long?>(null) }

    var elapsedSec by remember { mutableStateOf(0L) }
    var activeSec by remember { mutableStateOf(0L) }
    var lastActiveTickMs by remember { mutableStateOf<Long?>(null) }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(workoutId) {
        loading = true
        error = null

        runCatching {
            val workout = workoutRemoteRepository.getWorkout(workoutId.toInt())
            val exercises = exerciseRemoteRepository.getExercises()

            workoutName = workout.title

            val mapped = workout.exercises
                .sortedBy { it.orderNumber }
                .map { workoutExercise ->
                    val exerciseName = exercises
                        .firstOrNull { it.id == workoutExercise.exerciseId }
                        ?.title
                        ?: "Упражнение #${workoutExercise.exerciseId}"

                    Step(
                        exerciseId = workoutExercise.exerciseId,
                        name = exerciseName,
                        sets = workoutExercise.sets,
                        reps = workoutExercise.repetitions
                    )
                }

            steps = mapped

            val now = TimeUtils.nowEpochMs()
            startedAt = now
            lastActiveTickMs = now
        }.onFailure {
            error = "Не удалось загрузить тренировку с сервера"
        }

        loading = false
    }

    LaunchedEffect(startedAt) {
        val start = startedAt ?: return@LaunchedEffect

        while (true) {
            elapsedSec = (TimeUtils.nowEpochMs() - start) / 1000
            delay(1000)
        }
    }

    LaunchedEffect(startedAt, restLeft) {
        if (startedAt == null) return@LaunchedEffect

        if (restLeft > 0) {
            lastActiveTickMs = null
            return@LaunchedEffect
        }

        if (lastActiveTickMs == null) {
            lastActiveTickMs = TimeUtils.nowEpochMs()
        }

        while (restLeft == 0 && startedAt != null) {
            delay(1000)
            val now = TimeUtils.nowEpochMs()
            val last = lastActiveTickMs ?: now
            val deltaSec = ((now - last) / 1000).coerceAtLeast(0)

            if (deltaSec > 0) {
                activeSec += deltaSec
                lastActiveTickMs = now
            }
        }
    }

    LaunchedEffect(restLeft) {
        if (restLeft <= 0) return@LaunchedEffect

        while (restLeft > 0) {
            delay(1000)
            restLeft = max(0, restLeft - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(workoutName) })
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onFinished) {
                            Text("Назад")
                        }
                    }
                }

                steps.isEmpty() -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("В тренировке нет упражнений")
                        Button(onClick = onFinished) {
                            Text("Назад")
                        }
                    }
                }

                else -> {
                    val step = steps[currentIndex]
                    val isLastSet = currentSet >= step.sets
                    val isLastExercise = currentIndex >= steps.lastIndex
                    val canPressDone = restLeft == 0 && !saving

                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Упражнение", style = MaterialTheme.typography.labelLarge)
                                Text(step.name, style = MaterialTheme.typography.headlineSmall)
                                Text("Подход $currentSet из ${step.sets}", style = MaterialTheme.typography.titleMedium)
                                Text("${step.reps} повторений", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        Card {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Время тренировки")
                                Text(formatHms(elapsedSec))
                            }
                        }

                        Card {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Время подходов")
                                Text(formatHms(activeSec))
                            }
                        }

                        if (restLeft > 0) {
                            Card {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Отдых")
                                    Text("$restLeft сек.")
                                }
                            }

                            Text(
                                "Кнопка «Сделано» заблокирована до конца отдыха.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (saving) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        Spacer(Modifier.weight(1f))

                        Button(
                            onClick = {
                                if (!canPressDone) return@Button

                                if (!isLastSet) {
                                    currentSet += 1
                                    restLeft = restSecondsSetting
                                } else {
                                    if (!isLastExercise) {
                                        currentIndex += 1
                                        currentSet = 1
                                        restLeft = restSecondsSetting
                                    } else {
                                        scope.launch {
                                            saving = true

                                            val start = startedAt ?: TimeUtils.nowEpochMs()
                                            val now = TimeUtils.nowEpochMs()
                                            val durationSec = ((now - start) / 1000).toInt()

                                            runCatching {
                                                historyRemoteRepository.saveHistory(
                                                    workoutId = workoutId.toInt(),
                                                    workoutTitle = workoutName,
                                                    durationSec = durationSec,
                                                    calories = 0
                                                )
                                            }.onSuccess {
                                                onFinished()
                                            }.onFailure {
                                                error = "Не удалось сохранить историю на сервере"
                                            }

                                            saving = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canPressDone
                        ) {
                            Text(
                                if (isLastSet && isLastExercise) {
                                    if (saving) "Сохранение..." else "Завершить тренировку"
                                } else {
                                    "Сделано"
                                }
                            )
                        }

                        OutlinedButton(
                            onClick = onFinished,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !saving
                        ) {
                            Text("Выйти без сохранения")
                        }
                    }
                }
            }
        }
    }
}

private fun formatHms(totalSec: Long): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return "%02d:%02d:%02d".format(h, m, s)
}