package com.example.personaltrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personaltrainer.data.ExerciseRemoteRepository
import com.example.personaltrainer.data.Repository
import com.example.personaltrainer.data.remote.dto.ExerciseDto
import kotlinx.coroutines.launch
import java.util.Locale

private enum class CatalogSource {
    BUILTIN,
    CUSTOM
}

private sealed interface CatalogView {
    data object Root : CatalogView
    data class Groups(val source: CatalogSource) : CatalogView
    data class List(val source: CatalogSource, val group: String) : CatalogView
    data class Detail(val source: CatalogSource, val group: String, val id: Int) : CatalogView
    data object AddCustom : CatalogView
    data class EditCustom(val id: Int) : CatalogView
}

@Composable
fun CatalogTab(
    repo: Repository,
    exerciseRemoteRepository: ExerciseRemoteRepository
) {
    val scope = rememberCoroutineScope()

    var view by remember { mutableStateOf<CatalogView>(CatalogView.Root) }

    var serverBuiltin by remember { mutableStateOf<List<ExerciseDto>>(emptyList()) }
    var serverCustom by remember { mutableStateOf<List<ExerciseDto>>(emptyList()) }

    var serverLoading by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf<String?>(null) }

    suspend fun reloadExercises() {
        serverLoading = true
        serverError = null

        runCatching {
            val all = exerciseRemoteRepository.getExercises()
            val my = exerciseRemoteRepository.getMyExercises()
            all to my
        }.onSuccess { result ->
            serverBuiltin = result.first.filter { it.isBuiltin }
            serverCustom = result.second
        }.onFailure {
            serverError = "Не удалось загрузить упражнения с сервера"
        }

        serverLoading = false
    }

    LaunchedEffect(Unit) {
        reloadExercises()
    }

    when (val v = view) {
        CatalogView.Root -> {
            CatalogRootScreen(
                onBuiltin = { view = CatalogView.Groups(CatalogSource.BUILTIN) },
                onCustom = { view = CatalogView.Groups(CatalogSource.CUSTOM) }
            )
        }

        is CatalogView.Groups -> {
            val list = when (v.source) {
                CatalogSource.BUILTIN -> serverBuiltin
                CatalogSource.CUSTOM -> serverCustom
            }

            val groups = remember(list) {
                list
                    .map { groupFromMuscles(it.muscleGroup) }
                    .distinct()
                    .sorted()
            }

            CatalogGroupsScreen(
                title = if (v.source == CatalogSource.BUILTIN) {
                    "Встроенные"
                } else {
                    "Мои упражнения"
                },
                groups = groups,
                loading = serverLoading,
                error = serverError,
                onBack = { view = CatalogView.Root },
                onGroupClick = { group ->
                    view = CatalogView.List(v.source, group)
                },
                topAction = if (v.source == CatalogSource.CUSTOM) {
                    {
                        IconButton(onClick = { view = CatalogView.AddCustom }) {
                            Icon(Icons.Filled.Add, contentDescription = "Добавить")
                        }
                    }
                } else {
                    null
                }
            )
        }

        is CatalogView.List -> {
            val list = when (v.source) {
                CatalogSource.BUILTIN -> serverBuiltin
                CatalogSource.CUSTOM -> serverCustom
            }

            val filtered = remember(list, v.group) {
                list
                    .filter { groupFromMuscles(it.muscleGroup) == v.group }
                    .sortedBy { it.title }
            }

            ServerExerciseListScreen(
                title = v.group,
                exercises = filtered,
                loading = serverLoading,
                error = serverError,
                showMenu = v.source == CatalogSource.CUSTOM,
                onBack = { view = CatalogView.Groups(v.source) },
                onOpen = { id ->
                    view = CatalogView.Detail(v.source, v.group, id)
                },
                onEdit = { id ->
                    view = CatalogView.EditCustom(id)
                },
                onDelete = { id ->
                    scope.launch {
                        runCatching {
                            exerciseRemoteRepository.deleteMyExercise(id)
                        }.onSuccess {
                            reloadExercises()
                        }.onFailure {
                            serverError = "Не удалось удалить упражнение"
                        }
                    }
                }
            )
        }

        is CatalogView.Detail -> {
            val list = when (v.source) {
                CatalogSource.BUILTIN -> serverBuiltin
                CatalogSource.CUSTOM -> serverCustom
            }

            val exercise = list.firstOrNull { it.id == v.id }

            ServerExerciseDetailScreen(
                exercise = exercise,
                isCustom = v.source == CatalogSource.CUSTOM,
                onBack = { view = CatalogView.List(v.source, v.group) },
                onEdit = { id -> view = CatalogView.EditCustom(id) },
                onDelete = { id ->
                    scope.launch {
                        runCatching {
                            exerciseRemoteRepository.deleteMyExercise(id)
                        }.onSuccess {
                            reloadExercises()
                            view = CatalogView.Groups(CatalogSource.CUSTOM)
                        }.onFailure {
                            serverError = "Не удалось удалить упражнение"
                        }
                    }
                }
            )
        }

        CatalogView.AddCustom -> {
            AddExerciseScreen(
                exerciseRemoteRepository = exerciseRemoteRepository,
                onDone = {
                    scope.launch {
                        reloadExercises()
                        view = CatalogView.Groups(CatalogSource.CUSTOM)
                    }
                },
                onCancel = {
                    view = CatalogView.Groups(CatalogSource.CUSTOM)
                }
            )
        }

        is CatalogView.EditCustom -> {
            EditExerciseScreen(
                exerciseRemoteRepository = exerciseRemoteRepository,
                exercise = serverCustom.firstOrNull { it.id == v.id },
                onDone = {
                    scope.launch {
                        reloadExercises()
                        view = CatalogView.Groups(CatalogSource.CUSTOM)
                    }
                },
                onCancel = {
                    view = CatalogView.Groups(CatalogSource.CUSTOM)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogRootScreen(
    onBuiltin: () -> Unit,
    onCustom: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Каталог") })

        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                onClick = onBuiltin,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Встроенные упражнения",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        "Готовая база упражнений с сервера",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Card(
                onClick = onCustom,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Мои упражнения",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        "Пользовательские упражнения, сохранённые на сервере",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogGroupsScreen(
    title: String,
    groups: List<String>,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onGroupClick: (String) -> Unit,
    topAction: (@Composable (() -> Unit))? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    topAction?.invoke()
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (groups.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Пока пусто")
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(groups, key = { it }) { group ->
                        Card(
                            onClick = { onGroupClick(group) },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerExerciseListScreen(
    title: String,
    exercises: List<ExerciseDto>,
    loading: Boolean,
    error: String?,
    showMenu: Boolean,
    onBack: () -> Unit,
    onOpen: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }

    var deleteId by remember { mutableStateOf<Int?>(null) }
    var deleteName by remember { mutableStateOf("") }

    val filtered = remember(exercises, query) {
        val q = query.trim().lowercase(Locale("ru"))

        if (q.isEmpty()) {
            exercises.sortedBy { it.title }
        } else {
            exercises
                .filter { exercise ->
                    exercise.title.lowercase(Locale("ru")).contains(q) ||
                            exercise.muscleGroup.lowercase(Locale("ru")).contains(q) ||
                            exercise.description.lowercase(Locale("ru")).contains(q)
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
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
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
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

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
                LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { exercise ->
                        ServerExerciseRowCard(
                            exercise = exercise,
                            showMenu = showMenu,
                            onOpen = { onOpen(exercise.id) },
                            onEdit = { onEdit(exercise.id) },
                            onDelete = {
                                deleteId = exercise.id
                                deleteName = exercise.title
                            }
                        )
                    }
                }
            }
        }
    }

    if (deleteId != null) {
        AlertDialog(
            onDismissRequest = {
                deleteId = null
            },
            title = {
                Text("Удалить упражнение?")
            },
            text = {
                Text("Упражнение \"$deleteName\" будет удалено навсегда.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = deleteId ?: return@TextButton
                        deleteId = null
                        onDelete(id)
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteId = null
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun ServerExerciseRowCard(
    exercise: ExerciseDto,
    showMenu: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }

    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
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
                            text = preview.take(90) + if (preview.length > 90) "…" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (showMenu) {
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Text("⋮", style = MaterialTheme.typography.titleLarge)
                        }

                        DropdownMenu(
                            expanded = menu,
                            onDismissRequest = { menu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Редактировать") },
                                onClick = {
                                    menu = false
                                    onEdit()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Удалить") },
                                onClick = {
                                    menu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerExerciseDetailScreen(
    exercise: ExerciseDto?,
    isCustom: Boolean,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Упражнение") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            if (isCustom && exercise != null) {
                Surface(tonalElevation = 2.dp) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onEdit(exercise.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Редактировать")
                        }

                        Button(
                            onClick = { confirmDelete = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Удалить")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (exercise == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Упражнение не найдено")
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                exercise.title,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                exercise.muscleGroup,
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider()

            Text(
                exercise.description.ifBlank {
                    "Описание пока не задано."
                }
            )

            Text(
                "Тип: ${exercise.exerciseType}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (confirmDelete && exercise != null) {
        AlertDialog(
            onDismissRequest = {
                confirmDelete = false
            },
            title = {
                Text("Удалить упражнение?")
            },
            text = {
                Text("Упражнение \"${exercise.title}\" будет удалено навсегда.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete(exercise.id)
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseScreen(
    exerciseRemoteRepository: ExerciseRemoteRepository,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf("") }
    var muscles by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by rememberSaveable { mutableStateOf(false) }

    fun validate(): String? {
        if (name.trim().isEmpty()) return "Введите название"
        if (muscles.trim().isEmpty()) return "Введите группу мышц"
        return null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавить упражнение") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        enabled = !saving
                    ) {
                        Text("Отмена")
                    }

                    Button(
                        onClick = {
                            val validationError = validate()

                            if (validationError != null) {
                                error = validationError
                                return@Button
                            }

                            scope.launch {
                                saving = true
                                error = null

                                runCatching {
                                    exerciseRemoteRepository.createMyExercise(
                                        title = name.trim(),
                                        muscleGroup = muscles.trim(),
                                        description = description.trim()
                                    )
                                }.onSuccess {
                                    onDone()
                                }.onFailure {
                                    error = "Не удалось сохранить упражнение на сервере"
                                }

                                saving = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !saving
                    ) {
                        Text(
                            if (saving) {
                                "Сохранение..."
                            } else {
                                "Сохранить"
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        error ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                label = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = muscles,
                onValueChange = {
                    muscles = it
                    error = null
                },
                label = { Text("Мышцы/группа") },
                placeholder = { Text("Например: Грудь, трицепс") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                },
                label = { Text("Описание") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditExerciseScreen(
    exerciseRemoteRepository: ExerciseRemoteRepository,
    exercise: ExerciseDto?,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var name by remember(exercise) { mutableStateOf(exercise?.title.orEmpty()) }
    var muscles by remember(exercise) { mutableStateOf(exercise?.muscleGroup.orEmpty()) }
    var description by remember(exercise) { mutableStateOf(exercise?.description.orEmpty()) }

    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by rememberSaveable { mutableStateOf(false) }

    fun validate(): String? {
        if (name.trim().isEmpty()) return "Введите название"
        if (muscles.trim().isEmpty()) return "Введите группу мышц"
        return null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактировать упражнение") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        enabled = !saving
                    ) {
                        Text("Отмена")
                    }

                    Button(
                        onClick = {
                            val current = exercise

                            if (current == null) {
                                error = "Упражнение не найдено"
                                return@Button
                            }

                            val validationError = validate()

                            if (validationError != null) {
                                error = validationError
                                return@Button
                            }

                            scope.launch {
                                saving = true
                                error = null

                                runCatching {
                                    exerciseRemoteRepository.updateMyExercise(
                                        exerciseId = current.id,
                                        title = name.trim(),
                                        muscleGroup = muscles.trim(),
                                        description = description.trim()
                                    )
                                }.onSuccess {
                                    onDone()
                                }.onFailure {
                                    error = "Не удалось обновить упражнение на сервере"
                                }

                                saving = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !saving && exercise != null
                    ) {
                        Text(
                            if (saving) {
                                "Сохранение..."
                            } else {
                                "Сохранить"
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (exercise == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Упражнение не найдено")
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        error ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                label = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = muscles,
                onValueChange = {
                    muscles = it
                    error = null
                },
                label = { Text("Мышцы/группа") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                },
                label = { Text("Описание") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
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