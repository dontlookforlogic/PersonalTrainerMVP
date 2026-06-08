@file:OptIn(ExperimentalMaterial3Api::class)

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
import com.example.personaltrainer.data.HistoryRemoteRepository
import com.example.personaltrainer.data.Repository
import com.example.personaltrainer.data.remote.dto.HistoryResponseDto
import com.example.personaltrainer.data.remote.dto.WorkoutStatsResponseDto
import com.example.personaltrainer.ui.components.SimpleBarChart
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

private enum class Period(val days: Long, val label: String) {
    Week(7, "7 дней"),
    Month(30, "30 дней")
}

private enum class HistoryMode(val label: String) {
    All("Вся история"),
    PeriodOnly("Только период")
}

private enum class HistoryOrder(val label: String) {
    NewFirst("Сначала новые"),
    OldFirst("Сначала старые")
}

@Composable
fun StatsTab(
    repo: Repository,
    historyRemoteRepository: HistoryRemoteRepository,
    onBack: (() -> Unit)? = null
){
    val scope = rememberCoroutineScope()

    var serverHistory by remember { mutableStateOf<List<HistoryResponseDto>>(emptyList()) }
    var serverStats by remember { mutableStateOf<WorkoutStatsResponseDto?>(null) }

    var loading by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    var period by rememberSaveable { mutableStateOf(Period.Week) }
    var historyMode by rememberSaveable { mutableStateOf(HistoryMode.All) }
    var order by rememberSaveable { mutableStateOf(HistoryOrder.NewFirst) }

    fun loadStats() {
        scope.launch {
            loading = true
            error = null

            runCatching {
                val history = historyRemoteRepository.getHistory()
                val stats = historyRemoteRepository.getStats()
                history to stats
            }.onSuccess { result ->
                serverHistory = result.first
                serverStats = result.second
            }.onFailure {
                error = "Не удалось загрузить статистику с сервера"
            }

            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadStats()
    }

    val now = remember { LocalDate.now() }

    val fromDate = remember(period, now) {
        now.minusDays(period.days - 1)
    }

    val periodHistory = remember(serverHistory, fromDate) {
        serverHistory.filter { item ->
            parseHistoryDate(item.completedAt)?.let { date ->
                !date.isBefore(fromDate)
            } ?: false
        }
    }

    val totalCount = serverStats?.totalWorkouts ?: serverHistory.size
    val periodCount = periodHistory.size

    val periodDurationSec = remember(periodHistory) {
        periodHistory.sumOf { it.durationSec }
    }

    val periodDurationText = remember(periodDurationSec) {
        formatMmSs(periodDurationSec.toLong())
    }

    val avgDurationSec = remember(periodHistory) {
        if (periodHistory.isEmpty()) {
            0
        } else {
            periodHistory.sumOf { it.durationSec } / periodHistory.size
        }
    }

    val streakDays = remember(serverHistory) {
        calcServerStreakDays(serverHistory)
    }

    val goal = remember(period) {
        if (period == Period.Week) 3 else 10
    }

    val progress = remember(periodCount, goal) {
        periodCount.coerceAtMost(goal).toFloat() / goal.toFloat()
    }

    val dayStats = remember(periodHistory, fromDate, period) {
        val grouped = periodHistory.groupBy {
            parseHistoryDate(it.completedAt)
        }

        (0 until period.days).map { offset ->
            val day = fromDate.plusDays(offset)
            val items = grouped[day].orEmpty()
            day to items.size
        }
    }

    val bestDayText = remember(dayStats) {
        val best = dayStats.maxByOrNull { it.second }
        if (best == null || best.second == 0) {
            null
        } else {
            "Лучший день: ${best.first} (×${best.second})"
        }
    }

    val shownHistory = remember(serverHistory, periodHistory, historyMode, order) {
        val base = if (historyMode == HistoryMode.All) serverHistory else periodHistory

        val sorted = when (order) {
            HistoryOrder.NewFirst -> base.sortedByDescending { it.completedAt }
            HistoryOrder.OldFirst -> base.sortedBy { it.completedAt }
        }

        sorted.take(30)
    }

    val topWorkouts = remember(serverHistory) {
        serverHistory
            .groupingBy { it.workoutTitle.ifBlank { "Удалённая тренировка" } }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Подробная статистика") },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            },
            actions = {
                TextButton(onClick = { loadStats() }) {
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

            if (error != null) {
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
                                error ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )

                            TextButton(onClick = { loadStats() }) {
                                Text("Повторить")
                            }
                        }
                    }
                }
            }

            item {
                PeriodPicker(period = period, onChange = { period = it })
            }

            item {
                SummaryRow(
                    periodLabel = period.label,
                    periodCount = periodCount,
                    periodDurationText = periodDurationText,
                    totalCount = totalCount
                )
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryCard(
                        title = "Серия\n(дней подряд)",
                        value = streakDays.toString(),
                        modifier = Modifier.weight(1f)
                    )

                    SummaryCard(
                        title = "Средняя\nдлительность",
                        value = if (avgDurationSec == 0) "—" else formatMmSs(avgDurationSec.toLong()),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Цель на период", style = MaterialTheme.typography.titleMedium)

                        Text(
                            "Тренировок: $periodCount из $goal",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            if (periodCount >= goal) {
                                "Цель достигнута 🎉"
                            } else {
                                "Осталось: ${goal - periodCount}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Активность за ${period.label}", style = MaterialTheme.typography.titleMedium)

                        if (loading) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (dayStats.all { it.second == 0 }) {
                            Text(
                                "Пока нет данных за выбранный период.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            val values = dayStats.map { it.second }
                            val firstDay = dayStats.first().first.toString()
                            val lastDay = dayStats.last().first.toString()

                            SimpleBarChart(
                                title = "Тренировки по дням",
                                values = values,
                                labels = listOf(firstDay, lastDay),
                                modifier = Modifier.fillMaxWidth()
                            )

                            val minutes = periodDurationSec / 60

                            Text(
                                "Всего минут: $minutes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            bestDayText?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            if (topWorkouts.isNotEmpty()) {
                item {
                    Card {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Топ тренировок", style = MaterialTheme.typography.titleMedium)

                            topWorkouts.forEach { (name, count) ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Text("×$count", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("История", style = MaterialTheme.typography.titleMedium)

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = historyMode == HistoryMode.All,
                                onClick = { historyMode = HistoryMode.All },
                                label = { Text(HistoryMode.All.label) }
                            )

                            FilterChip(
                                selected = historyMode == HistoryMode.PeriodOnly,
                                onClick = { historyMode = HistoryMode.PeriodOnly },
                                label = { Text(HistoryMode.PeriodOnly.label) }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = order == HistoryOrder.NewFirst,
                                onClick = { order = HistoryOrder.NewFirst },
                                label = { Text(HistoryOrder.NewFirst.label) }
                            )

                            FilterChip(
                                selected = order == HistoryOrder.OldFirst,
                                onClick = { order = HistoryOrder.OldFirst },
                                label = { Text(HistoryOrder.OldFirst.label) }
                            )
                        }

                        Text(
                            "Показано: ${shownHistory.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (serverHistory.isEmpty() && !loading) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("История пока пустая.")
                        }
                    }
                }
            } else {
                items(shownHistory, key = { it.id }) { h ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                h.workoutTitle.ifBlank { "Удалённая тренировка" },
                                style = MaterialTheme.typography.titleSmall
                            )

                            Text(
                                formatServerDate(h.completedAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                "Длительность: ${formatDuration(h.durationSec.toLong())}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            if (h.calories > 0) {
                                Text(
                                    "Калории: ${h.calories}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodPicker(period: Period, onChange: (Period) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = period == Period.Week,
                onClick = { onChange(Period.Week) },
                label = { Text("7 дней") }
            )

            FilterChip(
                selected = period == Period.Month,
                onClick = { onChange(Period.Month) },
                label = { Text("30 дней") }
            )
        }
    }
}

@Composable
private fun SummaryRow(
    periodLabel: String,
    periodCount: Int,
    periodDurationText: String,
    totalCount: Int
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryCard(
            title = "Тренировок\n($periodLabel)",
            value = periodCount.toString(),
            modifier = Modifier.weight(1f)
        )

        SummaryCard(
            title = "Время\n($periodLabel)",
            value = periodDurationText,
            modifier = Modifier.weight(1f)
        )

        SummaryCard(
            title = "Всего\nтренировок",
            value = totalCount.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

private fun formatDuration(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    return "${m}м ${s}с"
}

private fun formatMmSs(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    return "%d:%02d".format(m, s)
}

private fun parseHistoryDate(value: String): LocalDate? {
    return runCatching {
        OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.systemDefault())
            .toLocalDate()
    }.getOrElse {
        runCatching {
            LocalDateTime.parse(value)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }.getOrNull()
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

private fun calcServerStreakDays(history: List<HistoryResponseDto>): Int {
    if (history.isEmpty()) return 0

    val days = history.mapNotNull {
        parseHistoryDate(it.completedAt)
    }.toSet()

    var streak = 0
    var d = LocalDate.now()

    while (days.contains(d)) {
        streak++
        d = d.minusDays(1)
    }

    return streak
}