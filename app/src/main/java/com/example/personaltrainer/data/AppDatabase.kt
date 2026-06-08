package com.example.personaltrainer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.personaltrainer.util.AppLog

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        HistoryEntity::class
    ],
    version = 3
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context.applicationContext).also { db ->
                    INSTANCE = db
                    AppLog.i("Room: база создана/открыта (personal_trainer.db)")

                    CoroutineScope(Dispatchers.IO).launch {
                        runCatching {
                            val count = db.exerciseDao().countAll()
                            AppLog.i("Room: упражнений в БД = $count")

                            if (count == 0) {
                                db.exerciseDao().upsertAll(PredefinedExercises.items)
                                AppLog.i("Seed: добавлены встроенные упражнения = ${PredefinedExercises.items.size}")
                            } else {
                                AppLog.i("Seed: пропущено (встроенные упражнения уже есть)")
                            }
                        }.onFailure {
                            AppLog.e("Seed: ошибка при заполнении БД", it)
                        }
                    }
                }
            }
        }

        private fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "personal_trainer.db")
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
