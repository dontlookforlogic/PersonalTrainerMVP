@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.personaltrainer.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personaltrainer.data.Repository
import com.example.personaltrainer.data.WorkoutRemoteRepository
import com.example.personaltrainer.data.WorkoutSummary
import com.example.personaltrainer.util.TimeUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import com.example.personaltrainer.data.remote.dto.WorkoutDto

@Composable
fun WorkoutsTab(
    repo: Repository,
    workoutRemoteRepository: WorkoutRemoteRepository,
    onCreateWorkout: () -> Unit,
    onEditWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit
) {
    val workouts by repo.workoutSummaries.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var serverWorkouts by remember { mutableStateOf<List<WorkoutDto>>(emptyList()) }
    var serverLoading by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf<String?>(null) }

    fun loadServerWorkouts() {
        scope.launch {
            serverLoading = true
            serverError = null

            runCatching {
                workoutRemoteRepository.getWorkouts()
            }.onSuccess {
                serverWorkouts = it
            }.onFailure {
                serverError = "Не удалось загрузить тренировки с сервера"
            }

            serverLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadServerWorkouts()
    }

    var deleteId by remember { mutableStateOf<Long?>(null) }
    var deleteName by remember { mutableStateOf("") }

    var query by rememberSaveable { mutableStateOf("") }
    var onlyToday by rememberSaveable { mutableStateOf(false) }

    val todayBit = remember { TimeUtils.todayDayMask(LocalDate.now()) }

    val filtered = remember(serverWorkouts, query, onlyToday, todayBit) {
        val q = query.trim().lowercase(Locale.getDefault())

        serverWorkouts.asSequence()
            .filter { w ->
                if (!onlyToday) true else (w.daysMask and todayBit) != 0
            }
            .filter { w ->
                if (q.isEmpty()) true
                else w.title.lowercase(Locale.getDefault()).contains(q) ||
                        daysMaskToText(w.daysMask).lowercase(Locale.getDefault()).contains(q)
            }
            .sortedBy { it.title }
            .toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Тренировки") },
                actions = {
                    IconButton(onClick = { onlyToday = !onlyToday }) {
                        Icon(
                            Icons.Default.FilterAlt,
                            contentDescription = "Фильтр",
                            tint = if (onlyToday) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = onCreateWorkout) {
                        Icon(Icons.Default.Add, contentDescription = "Создать")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Поиск") },
                placeholder = { Text("Например: ноги, пн, силовая…") }
            )

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
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )

                        TextButton(onClick = { loadServerWorkouts() }) {
                            Text("Повторить")
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Найдено: ${filtered.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onlyToday) {
                    AssistChip(
                        onClick = { onlyToday = false },
                        label = { Text("Только на сегодня") }
                    )
                }
            }

            when {
                serverWorkouts.isEmpty() && !serverLoading -> {
                    EmptyStateCard(
                        title = "Пока нет тренировок",
                        text = "Тренировки будут загружаться с сервера. Нажми +, чтобы создать первую тренировку."
                    )
                }

                filtered.isEmpty() && !serverLoading -> {
                    EmptyStateCard(
                        title = "Ничего не найдено",
                        text = "Попробуй изменить запрос или отключить фильтр."
                    )
                }

                else -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { w ->
                            ServerWorkoutCard(
                                workout = w,
                                onEditWorkout = {
                                    onEditWorkout(w.id.toLong())
                                },
                                onStartWorkout = {
                                    onStartWorkout(w.id.toLong())
                                },
                                onDelete = {
                                    deleteId = w.id.toLong()
                                    deleteName = w.title
                                }
                            )
                        }
                        item { Spacer(Modifier.height(6.dp)) }
                    }
                }
            }
        }
    }

    if (deleteId != null) {
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("Удалить тренировку?") },
            text = { Text("Тренировка \"$deleteName\" будет удалена. История выполнений останется.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = deleteId ?: return@TextButton
                    scope.launch {
                        runCatching {
                            workoutRemoteRepository.deleteWorkout(id.toInt())
                        }.onSuccess {
                            deleteId = null
                            loadServerWorkouts()
                        }.onFailure {
                            serverError = "Не удалось удалить тренировку"
                            deleteId = null
                        }
                    }
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun EmptyStateCard(title: String, text: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: WorkoutSummary,
    onEditWorkout: () -> Unit,
    onStartWorkout: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(workout.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))

                    DaysChips(mask = workout.daysMask)

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Упражнений: ${workout.exerciseCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Редактировать") },
                            onClick = { menu = false; onEditWorkout() }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить") },
                            onClick = { menu = false; onDelete() }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onEditWorkout, modifier = Modifier.weight(1f)) {
                    Text("Изменить")
                }
                Button(onClick = onStartWorkout, modifier = Modifier.weight(1f)) {
                    Text("Начать")
                }
            }
        }
    }
}

@Composable
private fun ServerWorkoutCard(
    workout: WorkoutDto,
    onEditWorkout: () -> Unit,
    onStartWorkout: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(workout.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))

                    DaysChips(mask = workout.daysMask)

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Упражнений: ${workout.exercises.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (workout.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            workout.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Редактировать") },
                            onClick = { menu = false; onEditWorkout() }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить") },
                            onClick = { menu = false; onDelete() }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onEditWorkout,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Изменить")
                }

                Button(
                    onClick = onStartWorkout,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Начать")
                }
            }

            Text(
                "Редактирование и запуск серверных тренировок подключим следующим шагом.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DaysChips(mask: Int) {
    val days = listOf(
        1 to "Пн",
        2 to "Вт",
        4 to "Ср",
        8 to "Чт",
        16 to "Пт",
        32 to "Сб",
        64 to "Вс"
    )

    val selected = days.filter { (bit, _) -> mask and bit != 0 }.map { it.second }

    if (selected.isEmpty()) {
        AssistChip(onClick = { }, label = { Text("Дни не выбраны") }, enabled = false)
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        selected.forEach { d ->
            AssistChip(onClick = { }, label = { Text(d) }, enabled = false)
        }
    }
}

/** 1=Пн,2=Вт,4=Ср,8=Чт,16=Пт,32=Сб,64=Вс */
private fun daysMaskToText(mask: Int): String {
    val days = listOf(
        1 to "Пн",
        2 to "Вт",
        4 to "Ср",
        8 to "Чт",
        16 to "Пт",
        32 to "Сб",
        64 to "Вс"
    ).filter { (bit, _) -> mask and bit != 0 }.map { it.second }

    return if (days.isEmpty()) "Дни не выбраны" else "Дни: " + days.joinToString(", ")
}
