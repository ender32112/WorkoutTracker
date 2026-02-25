package com.example.workouttracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        WeightEntryEntity::class,
        StepEntryEntity::class,
        ExerciseEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutTemplateExerciseEntity::class,
        WorkoutSessionPerformedEntity::class,
        WorkoutPerformedExerciseEntity::class,
        WorkoutSetPerformedEntity::class,
        ActiveWorkoutStateEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class WorkoutTrackerDatabase : RoomDatabase() {
    abstract fun dao(): WorkoutTrackerDao

    companion object {
        @Volatile private var INSTANCE: WorkoutTrackerDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workout_template_exercise` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `templateId` INTEGER NOT NULL,
                        `exerciseId` INTEGER NOT NULL,
                        `orderInTemplate` INTEGER NOT NULL,
                        `defaultSets` INTEGER NOT NULL,
                        `defaultReps` INTEGER NOT NULL,
                        `defaultWeight` REAL,
                        FOREIGN KEY(`templateId`) REFERENCES `workout_templates`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`exerciseId`) REFERENCES `exercise_catalog`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_template_exercise_templateId` ON `workout_template_exercise` (`templateId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_template_exercise_exerciseId` ON `workout_template_exercise` (`exerciseId`)")
            }
        }

        fun getInstance(context: Context): WorkoutTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutTrackerDatabase::class.java,
                    "workout_tracker.db"
                ).addMigrations(MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
