package com.example.workouttracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        WeightEntryEntity::class,
        StepEntryEntity::class,
        ExerciseEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WorkoutTrackerDatabase : RoomDatabase() {
    abstract fun dao(): WorkoutTrackerDao

    companion object {
        @Volatile private var INSTANCE: WorkoutTrackerDatabase? = null

        fun getInstance(context: Context): WorkoutTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutTrackerDatabase::class.java,
                    "workout_tracker.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
