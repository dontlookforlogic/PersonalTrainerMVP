package com.example.personaltrainer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class WorkoutWithExercises(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutId",
        entity = WorkoutExerciseEntity::class
    )
    val items: List<WorkoutExerciseEntity>
)

data class WorkoutSummary(
    val id: Long,
    val name: String,
    val daysMask: Int,
    val exerciseCount: Int
)

data class DayStat(
    val day: String,
    val count: Int,
    val totalDurationSec: Long
)

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise ORDER BY name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE isBuiltin = 1 ORDER BY muscles, name")
    fun observeBuiltin(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE isBuiltin = 0 ORDER BY muscles, name")
    fun observeCustom(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ex: ExerciseEntity): Long

    @Delete
    suspend fun delete(ex: ExerciseEntity)
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout ORDER BY name")
    fun observeAll(): Flow<List<WorkoutEntity>>

    @Query(
        """
        SELECT w.id AS id,
               w.name AS name,
               w.daysMask AS daysMask,
               COUNT(we.workoutId) AS exerciseCount
        FROM workout w
        LEFT JOIN workout_exercise we ON we.workoutId = w.id
        GROUP BY w.id
        ORDER BY w.name
        """
    )
    fun observeSummaries(): Flow<List<WorkoutSummary>>

    @Query(
        """
        SELECT w.id AS id,
               w.name AS name,
               w.daysMask AS daysMask,
               COUNT(we.workoutId) AS exerciseCount
        FROM workout w
        LEFT JOIN workout_exercise we ON we.workoutId = w.id
        WHERE (w.daysMask & :dayBit) != 0
        GROUP BY w.id
        ORDER BY w.name
        """
    )
    fun observeSummariesForDay(dayBit: Int): Flow<List<WorkoutSummary>>

    @Query("SELECT * FROM workout WHERE id = :id")
    suspend fun getById(id: Long): WorkoutEntity?

    @Query("SELECT name FROM workout WHERE id = :id LIMIT 1")
    suspend fun getWorkoutNameById(id: Long): String?

    @Insert
    suspend fun insert(workout: WorkoutEntity): Long

    @Update
    suspend fun update(workout: WorkoutEntity)

    @Delete
    suspend fun delete(workout: WorkoutEntity)

    @Query("DELETE FROM workout_exercise WHERE workoutId = :workoutId")
    suspend fun deleteExercisesForWorkout(workoutId: Long)

    @Insert
    suspend fun insertExercises(items: List<WorkoutExerciseEntity>)

    @Transaction
    @Query("SELECT * FROM workout WHERE id = :workoutId")
    suspend fun getWorkoutWithExercises(workoutId: Long): WorkoutWithExercises?
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY performedAtEpochMs DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(item: HistoryEntity): Long

    @Query("SELECT COUNT(*) FROM history WHERE performedAtEpochMs >= :fromEpochMs")
    suspend fun countSince(fromEpochMs: Long): Int

    @Query("SELECT COUNT(*) FROM history")
    suspend fun countAll(): Int

    @Query(
        """
        SELECT date(datetime(performedAtEpochMs/1000, 'unixepoch', 'localtime')) AS day,
               COUNT(*) AS count,
               SUM(durationSec) AS totalDurationSec
        FROM history
        WHERE performedAtEpochMs >= :fromEpochMs
        GROUP BY day
        ORDER BY day ASC
        """
    )
    suspend fun getDayStats(fromEpochMs: Long): List<DayStat>
}
