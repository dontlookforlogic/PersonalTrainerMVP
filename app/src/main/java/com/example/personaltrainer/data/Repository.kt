package com.example.personaltrainer.data

import kotlinx.coroutines.flow.Flow
import com.example.personaltrainer.util.AppLog

class Repository(private val db: AppDatabase) {
    val exercises: Flow<List<ExerciseEntity>> = db.exerciseDao().observeAll()
    val builtinExercises: Flow<List<ExerciseEntity>> = db.exerciseDao().observeBuiltin()
    val customExercises: Flow<List<ExerciseEntity>> = db.exerciseDao().observeCustom()
    val workouts: Flow<List<WorkoutEntity>> = db.workoutDao().observeAll()

    val workoutSummaries: Flow<List<WorkoutSummary>> = db.workoutDao().observeSummaries()

    fun workoutSummariesForDay(dayBit: Int): Flow<List<WorkoutSummary>> =
        db.workoutDao().observeSummariesForDay(dayBit)

    val history: Flow<List<HistoryEntity>> = db.historyDao().observeAll()

    suspend fun createWorkout(name: String, daysMask: Int, items: List<WorkoutExerciseDraft>): Long {
        val workoutId = db.workoutDao().insert(WorkoutEntity(name = name, daysMask = daysMask))
        setWorkoutExercises(workoutId, items)
        AppLog.i("Workouts: создана тренировка id=$workoutId, name='${name.trim()}', exercises=${items.size}")
        return workoutId
    }



    suspend fun updateWorkout(workoutId: Long, name: String, daysMask: Int, items: List<WorkoutExerciseDraft>) {
        db.workoutDao().update(WorkoutEntity(id = workoutId, name = name, daysMask = daysMask))
        setWorkoutExercises(workoutId, items)
        AppLog.i("Workouts: обновлена тренировка id=$workoutId, name='${name.trim()}', exercises=${items.size}")
    }

    private suspend fun setWorkoutExercises(workoutId: Long, items: List<WorkoutExerciseDraft>) {
        db.workoutDao().deleteExercisesForWorkout(workoutId)
        db.workoutDao().insertExercises(
            items.mapIndexed { idx, it ->
                WorkoutExerciseEntity(
                    workoutId = workoutId,
                    orderIndex = idx,
                    exerciseId = it.exerciseId,
                    sets = it.sets,
                    reps = it.reps
                )
            }
        )
    }

    suspend fun deleteWorkout(workout: WorkoutEntity) = db.workoutDao().delete(workout)

    suspend fun deleteWorkoutById(id: Long) {
        val w = db.workoutDao().getById(id) ?: return
        db.workoutDao().delete(w)
    }

    suspend fun getWorkoutWithExercises(workoutId: Long): WorkoutWithExercises? =
        db.workoutDao().getWorkoutWithExercises(workoutId)

    suspend fun getWorkoutNameById(id: Long): String? =
        db.workoutDao().getWorkoutNameById(id)

    suspend fun getExercise(exerciseId: Long): ExerciseEntity? =
        db.exerciseDao().getById(exerciseId)

    suspend fun saveHistory(workoutId: Long, workoutName: String, performedAtEpochMs: Long, durationSec: Long) {
        db.historyDao().insert(
            HistoryEntity(
                workoutId = workoutId,
                workoutName = workoutName,
                performedAtEpochMs = performedAtEpochMs,
                durationSec = durationSec
            )
        )
        AppLog.i("History: сохранена тренировка workoutId=$workoutId, durationSec=$durationSec")
    }

    suspend fun countSince(fromEpochMs: Long): Int = db.historyDao().countSince(fromEpochMs)
    suspend fun countAll(): Int = db.historyDao().countAll()

    suspend fun getDayStats(fromEpochMs: Long): List<DayStat> =
        db.historyDao().getDayStats(fromEpochMs)

    suspend fun addCustomExercise(name: String, muscles: String, description: String): Long {
        val id = db.exerciseDao().insert(
            ExerciseEntity(
                id = 0,
                name = name.trim(),
                muscles = muscles.trim(),
                description = description.trim(),
                isBuiltin = false
            )
        )
        AppLog.i("Catalog: добавлено пользовательское упражнение id=$id, name='${name.trim()}'")
        return id
    }

    suspend fun deleteCustomExercise(id: Long) {
        val ex = db.exerciseDao().getById(id) ?: return
        if (ex.isBuiltin) return
        db.exerciseDao().delete(ex)
        AppLog.i("Catalog: удалено пользовательское упражнение id=$id")
    }

    suspend fun updateCustomExercise(id: Long, name: String, muscles: String, description: String) {
        val ex = db.exerciseDao().getById(id) ?: return
        if (ex.isBuiltin) return

        db.exerciseDao().insert(
            ex.copy(
                name = name.trim(),
                muscles = muscles.trim(),
                description = description.trim()
            )
        )
    }

}

data class WorkoutExerciseDraft(
    val exerciseId: Long,
    val sets: Int,
    val reps: Int
)
