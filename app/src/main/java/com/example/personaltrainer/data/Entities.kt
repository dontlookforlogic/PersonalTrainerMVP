package com.example.personaltrainer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscles: String,
    val description: String,
    val isBuiltin: Boolean = false
)

@Entity(tableName = "workout")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val daysMask: Int
)

@Entity(tableName = "workout_exercise", primaryKeys = ["workoutId", "orderIndex"])
data class WorkoutExerciseEntity(
    val workoutId: Long,
    val orderIndex: Int,
    val exerciseId: Long,
    val sets: Int,
    val reps: Int
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val workoutName: String,
    val performedAtEpochMs: Long,
    val durationSec: Long
)
