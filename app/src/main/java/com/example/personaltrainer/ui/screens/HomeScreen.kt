package com.example.personaltrainer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.personaltrainer.data.Repository
import com.example.personaltrainer.data.SettingsStore
import androidx.compose.material.icons.filled.Person
import com.example.personaltrainer.data.ActivityRemoteRepository
import com.example.personaltrainer.data.NotificationPrefsStore
import com.example.personaltrainer.data.UserSessionStore
import com.example.personaltrainer.data.AuthRepository
import com.example.personaltrainer.data.ExerciseRemoteRepository
import com.example.personaltrainer.data.HistoryRemoteRepository
import com.example.personaltrainer.data.NotificationRemoteRepository
import com.example.personaltrainer.data.ProfileRepository
import com.example.personaltrainer.data.WorkoutRemoteRepository


private enum class HomeTab(val label: String, val icon: @Composable () -> Unit) {
    Today("Сегодня", { Icon(Icons.Default.Today, contentDescription = null) }),
    Catalog("Каталог", { Icon(Icons.Default.List, contentDescription = null) }),
    Workouts("Тренинг", { Icon(Icons.Default.FitnessCenter, contentDescription = null) }),
    Activity("Актив.", { Icon(Icons.Default.BarChart, contentDescription = null) }),
    Profile("Профиль", { Icon(Icons.Default.Person, contentDescription = null) }),
}

@Composable
fun HomeScreen(
    repo: Repository,
    settings: SettingsStore,
    userSessionStore: UserSessionStore,
    authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    exerciseRemoteRepository: ExerciseRemoteRepository,
    workoutRemoteRepository: WorkoutRemoteRepository,
    notificationPrefsStore: NotificationPrefsStore,
    onCreateWorkout: () -> Unit,
    onEditWorkout: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
    historyRemoteRepository: HistoryRemoteRepository,
    activityRemoteRepository: ActivityRemoteRepository,
    onOpenDetailedStats: () -> Unit,
    notificationRemoteRepository: NotificationRemoteRepository
) {
    var tab by rememberSaveable { mutableStateOf(HomeTab.Today) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                HomeTab.values().forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { t.icon() },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                HomeTab.Today -> TodayTab(
                    settings = settings,
                    onStartWorkout = onStartWorkout,
                    onOpenWorkouts = { tab = HomeTab.Workouts },
                    onOpenCatalog = { tab = HomeTab.Catalog },
                    notificationPrefsStore = notificationPrefsStore,
                    notificationRemoteRepository = notificationRemoteRepository,
                    workoutRemoteRepository = workoutRemoteRepository,
                    historyRemoteRepository = historyRemoteRepository,
                    repo = repo
                    )

                HomeTab.Catalog -> CatalogTab(
                    repo = repo,
                    exerciseRemoteRepository = exerciseRemoteRepository
                )

                HomeTab.Workouts -> WorkoutsTab(
                    repo = repo,
                    workoutRemoteRepository = workoutRemoteRepository,
                    onCreateWorkout = onCreateWorkout,
                    onEditWorkout = onEditWorkout,
                    onStartWorkout = onStartWorkout
                )

                HomeTab.Activity -> ActivityTab(
                    activityRemoteRepository = activityRemoteRepository
                )

                HomeTab.Profile -> ProfileTab(
                    userSessionStore = userSessionStore,
                    authRepository = authRepository,
                    profileRepository = profileRepository,
                    historyRemoteRepository = historyRemoteRepository,
                    repo = repo,
                    onOpenDetailedStats = onOpenDetailedStats
                )
            }
        }
    }
}
