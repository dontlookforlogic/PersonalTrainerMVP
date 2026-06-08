package com.example.personaltrainer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.personaltrainer.PersonalTrainerApp
import com.example.personaltrainer.data.Repository
import com.example.personaltrainer.data.SettingsStore
import com.example.personaltrainer.ui.screens.*
import com.example.personaltrainer.data.UserSessionStore
import com.example.personaltrainer.data.NotificationPrefsStore
import com.example.personaltrainer.data.AuthRepository
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.personaltrainer.data.ProfileRepository
import com.example.personaltrainer.data.ExerciseRemoteRepository
import com.example.personaltrainer.data.WorkoutRemoteRepository
import com.example.personaltrainer.data.HistoryRemoteRepository
import com.example.personaltrainer.data.ActivityRemoteRepository
import com.example.personaltrainer.data.NotificationRemoteRepository
import com.example.personaltrainer.util.LogTest
import com.example.personaltrainer.util.FcmTokenSync
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object Routes {
    const val Home = "home"
    const val WorkoutEditor = "workoutEditor"
    const val WorkoutEditorWithId = "workoutEditor/{workoutId}"
    const val Perform = "perform/{workoutId}"
    const val DetailedStats = "detailedStats"
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as PersonalTrainerApp
    val repo = remember { Repository(app.db) }
    val settings = remember { SettingsStore(context) }
    val userSessionStore = remember { UserSessionStore(context) }
    val authRepository = remember { AuthRepository(userSessionStore) }
    val profileRepository = remember { ProfileRepository(userSessionStore) }
    val exerciseRemoteRepository = remember { ExerciseRemoteRepository(userSessionStore) }
    val workoutRemoteRepository = remember { WorkoutRemoteRepository(userSessionStore) }
    val historyRemoteRepository = remember { HistoryRemoteRepository(userSessionStore) }
    val activityRemoteRepository = remember { ActivityRemoteRepository(userSessionStore) }
    val notificationRemoteRepository = remember { NotificationRemoteRepository(userSessionStore) }
    val notificationPrefsStore = remember { NotificationPrefsStore(context) }
    val currentUser by userSessionStore.currentUser.collectAsState(initial = null)
    var checkingSession by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withTimeoutOrNull(5000) {
            authRepository.refreshCurrentUser()
        }

        checkingSession = false
    }

    if (checkingSession) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (currentUser == null) {
        AuthScreen(authRepository = authRepository)
        return
    }

    LaunchedEffect(currentUser) {
        LogTest.sessionChecked(currentUser != null)
        if (currentUser != null) {
            FcmTokenSync.syncToken(context, userSessionStore)
            LogTest.homeOpened()
        }
    }

    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.Home) {
        composable(Routes.Home) {
            HomeScreen(
                repo = repo,
                settings = settings,
                userSessionStore = userSessionStore,
                authRepository = authRepository,
                profileRepository = profileRepository,
                exerciseRemoteRepository = exerciseRemoteRepository,
                workoutRemoteRepository = workoutRemoteRepository,
                notificationPrefsStore = notificationPrefsStore,
                historyRemoteRepository = historyRemoteRepository,
                activityRemoteRepository = activityRemoteRepository,
                notificationRemoteRepository = notificationRemoteRepository,
                onCreateWorkout = { nav.navigate(Routes.WorkoutEditor) },
                onEditWorkout = { id -> nav.navigate("workoutEditor/$id") },
                onStartWorkout = { id -> nav.navigate("perform/$id") },
                onOpenDetailedStats = {
                    nav.navigate(Routes.DetailedStats)
                }
            )
        }

        composable(Routes.DetailedStats) {
            StatsTab(
                repo = repo,
                historyRemoteRepository = historyRemoteRepository,
                onBack = {
                    nav.popBackStack()
                }
            )
        }

        composable(Routes.WorkoutEditor) {
            WorkoutEditorScreen(
                repo = repo,
                exerciseRemoteRepository = exerciseRemoteRepository,
                workoutRemoteRepository = workoutRemoteRepository,
                workoutId = null,
                onDone = { nav.popBackStack() }
            )
        }
        composable(
            route = Routes.WorkoutEditorWithId,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { backStack ->
            val workoutId = backStack.arguments?.getLong("workoutId")
            WorkoutEditorScreen(
                repo = repo,
                exerciseRemoteRepository = exerciseRemoteRepository,
                workoutRemoteRepository = workoutRemoteRepository,
                workoutId = workoutId,
                onDone = { nav.popBackStack() }
            )
        }
        composable(
            route = Routes.Perform,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { backStack ->
            val workoutId = backStack.arguments?.getLong("workoutId") ?: return@composable
            PerformWorkoutScreen(
                repo = repo,
                settings = settings,
                workoutRemoteRepository = workoutRemoteRepository,
                exerciseRemoteRepository = exerciseRemoteRepository,
                historyRemoteRepository = historyRemoteRepository,
                workoutId = workoutId,
                onFinished = { nav.popBackStack() }
            )
        }
    }
}
