package com.example.personaltrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.personaltrainer.data.ExerciseRemoteRepository
import com.example.personaltrainer.data.Repository
import com.example.personaltrainer.data.WorkoutRemoteRepository
import com.example.personaltrainer.data.remote.dto.ExerciseDto
import com.example.personaltrainer.data.remote.dto.WorkoutExerciseRequestDto
import kotlinx.coroutines.launch
import java.util.Locale
import com.example.personaltrainer.util.LogTest

private data class DraftItem(
    val exerciseId: Int,
    val exerciseName: String,
    var sets: Int,
    var reps: Int
)

private enum class PickerSource {
    BUILTIN,
    CUSTOM
}

private sealed interface PickerView {
    data object Root : PickerView
    data class Groups(val source: PickerSource) : PickerView
    data class List(val source: PickerSource, val group: String) : PickerView
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditorScreen(
    repo: Repository,
    exerciseRemoteRepository: ExerciseRemoteRepository,
    workoutRemoteRepository: WorkoutRemoteRepository,
    workoutId: Long?,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var serverBuiltin by remember { mutableStateOf<List<ExerciseDto>>(emptyList()) }
    var serverCustom by remember { mutableStateOf<List<ExerciseDto>>(emptyList()) }

    var exercisesLoading by rememberSaveable { mutableStateOf(false) }
    var exercisesError by rememberSaveable { mutableStateOf<String?>(null) }

    var workoutLoading by rememberSaveable { mutableStateOf(false) }
    var workoutLoaded by rememberSaveable { mutableStateOf(workoutId == null) }

    var name by rememberSaveable { mutableStateOf("") }
    var daysMask by rememberSaveable { mutableStateOf(0) }
    var items by remember { mutableStateOf(listOf<DraftItem>()) }

    var showPicker by remember { mutableStateOf(false) }
    var pendingExerciseId by remember { mutableStateOf<Int?>(null) }
    var showSetsDialog by remember { mutableStateOf(false) }

    var saving by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val allServerExercises = remember(serverBuiltin, serverCustom) {
        serverBuiltin + serverCustom
    }

    LaunchedEffect(Unit) {
        exercisesLoading = true
        exercisesError = null

        runCatching {
            val all = exerciseRemoteRepository.getExercises()
            val my = exerciseRemoteRepository.getMyExercises()
            all to my
        }.onSuccess { result ->
            serverBuiltin = result.first.filter { it.isBuiltin }
            serverCustom = result.second
        }.onFailure {
            exercisesError = "Не удалось загрузить упражнения с сервера"
        }

        exercisesLoading = false
    }

    LaunchedEffect(workoutId, allServerExercises) {
        if (workoutId == null) return@LaunchedEffect
        if (allServerExercises.isEmpty()) return@LaunchedEffect
        if (workoutLoaded) return@LaunchedEffect

        workoutLoading = true
        error = null

        runCatching {
            workoutRemoteRepository.getWorkout(workoutId.toInt())
        }.onSuccess { workout ->
            name = workout.title
            daysMask = workout.daysMask

            items = workout.exercises
                .sortedBy { it.orderNumber }
                .map { item ->
                    val exercise = allServerExercises.firstOrNull { it.id == item.exerciseId }

                    DraftItem(
                        exerciseId = item.exerciseId,
                        exerciseName = exercise?.title ?: "Упражнение #${item.exerciseId}",
                        sets = item.sets,
                        reps = item.repetitions
                    )
                }

            workoutLoaded = true
        }.onFailure {
            error = "Не удалось загрузить тренировку"
        }

        workoutLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (workoutId == null) {
                            "Создать тренировку"
                        } else {
                            "Редактировать тренировку"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        enabled = !saving && !workoutLoading,
                        onClick = {
                            scope.launch {
                                error = null

                                if (name.isBlank()) {
                                    error = "Введите название"
                                    return@launch
                                }

                                if (items.isEmpty()) {
                                    error = "Добавьте хотя бы одно упражнение"
                                    return@launch
                                }

                                if (items.any { it.sets <= 0 || it.reps <= 0 }) {
                                    error = "Подходы и повторения должны быть больше 0"
                                    return@launch
                                }

                                saving = true

                                val requestItems = items.mapIndexed { index, item ->
                                    WorkoutExerciseRequestDto(
                                        exerciseId = item.exerciseId,
                                        orderNumber = index,
                                        sets = item.sets,
                                        repetitions = item.reps,
                                        restTimeSec = 60
                                    )
                                }

                                runCatching {
                                    if (workoutId == null) {
                                        workoutRemoteRepository.createWorkout(
                                            title = name.trim(),
                                            daysMask = daysMask,
                                            exercises = requestItems
                                        )
                                    } else {
                                        workoutRemoteRepository.updateWorkout(
                                            workoutId = workoutId.toInt(),
                                            title = name.trim(),
                                            daysMask = daysMask,
                                            exercises = requestItems
                                        )
                                    }
                                }.onSuccess {
                                    LogTest.workoutSaved()
                                    onDone()
                                }.onFailure {
                                    error = "Не удалось сохранить тренировку на сервере"
                                }

                                saving = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Done, contentDescription = "Сохранить")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (allServerExercises.isEmpty()) {
                        error = "Список упражнений ещё не загружен"
                    } else {
                        showPicker = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить упражнение")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (exercisesLoading || workoutLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            exercisesError?.let {
                AssistChip(onClick = {}, label = { Text(it) })
            }

            error?.let {
                AssistChip(onClick = {}, label = { Text(it) })
            }

            if (saving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (workoutId != null && workoutLoading && items.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название тренировки") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Дни недели", style = MaterialTheme.typography.titleMedium)

            DaysSelector(
                daysMask = daysMask,
                onChange = { daysMask = it }
            )

            HorizontalDivider()

            Text("Упражнения", style = MaterialTheme.typography.titleMedium)

            if (items.isEmpty()) {
                Text("Пока нет упражнений. Нажмите + чтобы добавить.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    itemsIndexed(items) { index, item ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(
                                Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${index + 1}. ${item.exerciseName}",
                                        style = MaterialTheme.typography.titleSmall
                                    )

                                    TextButton(
                                        onClick = {
                                            items = items.toMutableList().also {
                                                it.removeAt(index)
                                            }
                                        }
                                    ) {
                                        Text("Убрать")
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    NumberField(
                                        label = "Подходы",
                                        value = item.sets,
                                        onValueChange = { value ->
                                            item.sets = value
                                            items = items.toList()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    NumberField(
                                        label = "Повторения",
                                        value = item.reps,
                                        onValueChange = { value ->
                                            item.reps = value
                                            items = items.toList()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        ServerExercisePickerDialog(
            builtinExercises = serverBuiltin,
            customExercises = serverCustom,
            onDismiss = { showPicker = false },
            onPick = { exerciseId ->
                pendingExerciseId = exerciseId
                showPicker = false
                showSetsDialog = true
            }
        )
    }

    if (showSetsDialog && pendingExerciseId != null) {
        val exerciseName = allServerExercises
            .firstOrNull { it.id == pendingExerciseId }
            ?.title
            ?: "Упражнение"

        AddSetsRepsDialog(
            exerciseName = exerciseName,
            onDismiss = {
                showSetsDialog = false
                pendingExerciseId = null
            },
            onAdd = { sets, reps ->
                val exerciseId = pendingExerciseId ?: return@AddSetsRepsDialog

                val resolvedName = allServerExercises
                    .firstOrNull { it.id == exerciseId }
                    ?.title
                    ?: exerciseName

                items = items + DraftItem(
                    exerciseId = exerciseId,
                    exerciseName = resolvedName,
                    sets = sets,
                    reps = reps
                )

                showSetsDialog = false
                pendingExerciseId = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DaysSelector(
    daysMask: Int,
    onChange: (Int) -> Unit
) {
    val days = listOf(
        1 to "Пн",
        2 to "Вт",
        4 to "Ср",
        8 to "Чт",
        16 to "Пт",
        32 to "Сб",
        64 to "Вс"
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { (bit, label) ->
            val selected = daysMask and bit != 0

            FilterChip(
                selected = selected,
                onClick = {
                    onChange(
                        if (selected) {
                            daysMask and bit.inv()
                        } else {
                            daysMask or bit
                        }
                    )
                },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.filter { char -> char.isDigit() }.take(3)
            onValueChange(text.toIntOrNull() ?: 0)
        },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerExercisePickerDialog(
    builtinExercises: List<ExerciseDto>,
    customExercises: List<ExerciseDto>,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit
) {
    var view by remember { mutableStateOf<PickerView>(PickerView.Root) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 2.dp
        ) {
            when (val currentView = view) {
                PickerView.Root -> {
                    Column(Modifier.fillMaxSize()) {
                        TopAppBar(
                            title = { Text("Выбор упражнения") },
                            navigationIcon = {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Закрыть")
                                }
                            }
                        )

                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                onClick = {
                                    view = PickerView.Groups(PickerSource.BUILTIN)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "Встроенные упражнения",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        "Каталог упражнений с сервера",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Card(
                                onClick = {
                                    view = PickerView.Groups(PickerSource.CUSTOM)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(
                                    Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "Мои упражнения",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        "Пользовательские упражнения с сервера",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                is PickerView.Groups -> {
                    val exercises = when (currentView.source) {
                        PickerSource.BUILTIN -> builtinExercises
                        PickerSource.CUSTOM -> customExercises
                    }

                    val groups = remember(exercises) {
                        exercises
                            .map { groupFromMuscles(it.muscleGroup) }
                            .distinct()
                            .sorted()
                    }

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        if (currentView.source == PickerSource.BUILTIN) {
                                            "Встроенные"
                                        } else {
                                            "Мои упражнения"
                                        }
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { view = PickerView.Root }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                                    }
                                }
                            )
                        }
                    ) { padding ->
                        if (groups.isEmpty()) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(padding),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Пока пусто")
                            }
                        } else {
                            LazyColumn(
                                Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(groups, key = { it }) { group ->
                                    Card(
                                        onClick = {
                                            view = PickerView.List(currentView.source, group)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(16.dp)) {
                                            Text(
                                                group,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is PickerView.List -> {
                    val exercises = when (currentView.source) {
                        PickerSource.BUILTIN -> builtinExercises
                        PickerSource.CUSTOM -> customExercises
                    }

                    val filtered = remember(exercises, currentView.group) {
                        exercises
                            .filter { groupFromMuscles(it.muscleGroup) == currentView.group }
                            .sortedBy { it.title }
                    }

                    ServerExercisePickerListScreen(
                        title = currentView.group,
                        exercises = filtered,
                        onBack = {
                            view = PickerView.Groups(currentView.source)
                        },
                        onPick = onPick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerExercisePickerListScreen(
    title: String,
    exercises: List<ExerciseDto>,
    onBack: () -> Unit,
    onPick: (Int) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }

    val filtered = remember(exercises, query) {
        val queryValue = query.trim().lowercase(Locale("ru"))

        if (queryValue.isEmpty()) {
            exercises.sortedBy { it.title }
        } else {
            exercises
                .filter { exercise ->
                    exercise.title.lowercase(Locale("ru")).contains(queryValue) ||
                            exercise.muscleGroup.lowercase(Locale("ru")).contains(queryValue) ||
                            exercise.description.lowercase(Locale("ru")).contains(queryValue)
                }
                .sortedBy { it.title }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Поиск") },
                placeholder = { Text("Название, мышцы, описание") }
            )

            if (filtered.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (query.isBlank()) {
                            "В этой группе упражнений пока нет"
                        } else {
                            "Ничего не найдено"
                        }
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered, key = { it.id }) { exercise ->
                        Card(
                            onClick = { onPick(exercise.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    exercise.title,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    exercise.muscleGroup,
                                    style = MaterialTheme.typography.bodySmall
                                )

                                val preview = exercise.description.trim()

                                if (preview.isNotEmpty()) {
                                    Text(
                                        preview.take(90) + if (preview.length > 90) "…" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSetsRepsDialog(
    exerciseName: String,
    onDismiss: () -> Unit,
    onAdd: (sets: Int, reps: Int) -> Unit
) {
    var sets by remember { mutableStateOf(3) }
    var reps by remember { mutableStateOf(10) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Параметры упражнения") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(exerciseName, style = MaterialTheme.typography.titleMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(
                        label = "Подходы",
                        value = sets,
                        onValueChange = { sets = it },
                        modifier = Modifier.weight(1f)
                    )

                    NumberField(
                        label = "Повторения",
                        value = reps,
                        onValueChange = { reps = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(
                        sets.coerceAtLeast(1),
                        reps.coerceAtLeast(1)
                    )
                }
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

private fun groupFromMuscles(muscles: String): String {
    val value = muscles.trim()

    if (value.isEmpty()) {
        return "Другое"
    }

    return value
        .split(",")
        .first()
        .trim()
        .ifEmpty { "Другое" }
}