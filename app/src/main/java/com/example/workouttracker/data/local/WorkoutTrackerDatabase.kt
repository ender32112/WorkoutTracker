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
        ActiveWorkoutStateEntity::class,
        NutritionEntryEntity::class,
        FridgeItemEntity::class,
        ProductCacheEntity::class
    ],
    version = 5,
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `nutrition_entries` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `dateIso` TEXT NOT NULL,
                        `mealType` TEXT NOT NULL,
                        `dishJson` TEXT NOT NULL,
                        `portionWeight` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_nutrition_entries_userId` ON `nutrition_entries` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_nutrition_entries_dateIso` ON `nutrition_entries` (`dateIso`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `fridge_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `unitType` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `calories100` INTEGER NOT NULL,
                        `protein100` INTEGER NOT NULL,
                        `fats100` INTEGER NOT NULL,
                        `carbs100` INTEGER NOT NULL,
                        `barcode` TEXT,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fridge_items_userId` ON `fridge_items` (`userId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `product_cache` (
                        `barcode` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `calories100` INTEGER NOT NULL,
                        `protein100` INTEGER NOT NULL,
                        `fats100` INTEGER NOT NULL,
                        `carbs100` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        `cachedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`barcode`)
                    )
                    """.trimIndent()
                )
            }
        }


        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `fridge_items_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `unitType` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `calories100` REAL NOT NULL,
                        `protein100` REAL NOT NULL,
                        `fats100` REAL NOT NULL,
                        `carbs100` REAL NOT NULL,
                        `barcode` TEXT,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fridge_items_new_userId` ON `fridge_items_new` (`userId`)")
                db.execSQL(
                    """
                    INSERT INTO fridge_items_new (id, userId, name, unitType, amount, calories100, protein100, fats100, carbs100, barcode, updatedAt)
                    SELECT id, userId, name, unitType, amount,
                           CAST(calories100 AS REAL), CAST(protein100 AS REAL), CAST(fats100 AS REAL), CAST(carbs100 AS REAL),
                           barcode, updatedAt
                    FROM fridge_items
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE fridge_items")
                db.execSQL("ALTER TABLE fridge_items_new RENAME TO fridge_items")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fridge_items_userId` ON `fridge_items` (`userId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `product_cache_new` (
                        `barcode` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `calories100` REAL NOT NULL,
                        `protein100` REAL NOT NULL,
                        `fats100` REAL NOT NULL,
                        `carbs100` REAL NOT NULL,
                        `source` TEXT NOT NULL,
                        `cachedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`barcode`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO product_cache_new (barcode, name, calories100, protein100, fats100, carbs100, source, cachedAt)
                    SELECT barcode, name,
                           CAST(calories100 AS REAL), CAST(protein100 AS REAL), CAST(fats100 AS REAL), CAST(carbs100 AS REAL),
                           source, cachedAt
                    FROM product_cache
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE product_cache")
                db.execSQL("ALTER TABLE product_cache_new RENAME TO product_cache")
            }
        }

        fun getInstance(context: Context): WorkoutTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutTrackerDatabase::class.java,
                    "workout_tracker.db"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
