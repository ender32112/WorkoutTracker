package com.example.workouttracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.workouttracker.data.exercise.ExerciseCatalogDao
import com.example.workouttracker.data.exercise.ExerciseEntity as LibraryExerciseEntity
import com.example.workouttracker.data.exercise.ExerciseTypeConverters
import com.example.workouttracker.data.exercise.ExerciseUserMetaEntity

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
        NutritionProfileEntity::class,
        FridgeItemEntity::class,
        ProductCacheEntity::class,
        MealPlanEntity::class,
        LibraryExerciseEntity::class,
        ExerciseUserMetaEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(ExerciseTypeConverters::class)
abstract class WorkoutTrackerDatabase : RoomDatabase() {
    abstract fun dao(): WorkoutTrackerDao
    abstract fun exerciseCatalogDao(): ExerciseCatalogDao

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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise_catalog ADD COLUMN sourceExerciseId TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_catalog_sourceExerciseId ON exercise_catalog(sourceExerciseId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exercises` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `nameEn` TEXT NOT NULL,
                        `nameRu` TEXT NOT NULL,
                        `gifUrl` TEXT NOT NULL,
                        `bodyPart` TEXT NOT NULL,
                        `targetMuscles` TEXT NOT NULL,
                        `secondaryMuscles` TEXT NOT NULL,
                        `equipment` TEXT NOT NULL,
                        `instructions` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercises_userId` ON `exercises` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercises_bodyPart` ON `exercises` (`bodyPart`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercises_equipment` ON `exercises` (`equipment`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exercise_user_meta` (
                        `userId` TEXT NOT NULL,
                        `exerciseId` TEXT NOT NULL,
                        `isFavorite` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`, `exerciseId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_user_meta_userId` ON `exercise_user_meta` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_user_meta_exerciseId` ON `exercise_user_meta` (`exerciseId`)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN localMediaUri TEXT")
                db.execSQL("ALTER TABLE exercises ADD COLUMN source TEXT NOT NULL DEFAULT 'GLOBAL'")
                db.execSQL("ALTER TABLE exercises ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercises_isCustom` ON `exercises` (`isCustom`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercises_source` ON `exercises` (`source`)")

                db.execSQL("ALTER TABLE exercise_user_meta ADD COLUMN lastUsedAt INTEGER")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workout_template_exercise_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `templateId` INTEGER NOT NULL,
                        `catalogExerciseId` TEXT NOT NULL,
                        `orderInTemplate` INTEGER NOT NULL,
                        `defaultSets` INTEGER NOT NULL,
                        `defaultReps` INTEGER NOT NULL,
                        `defaultWeight` REAL,
                        FOREIGN KEY(`templateId`) REFERENCES `workout_templates`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_template_exercise_new_templateId` ON `workout_template_exercise_new` (`templateId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_template_exercise_new_catalogExerciseId` ON `workout_template_exercise_new` (`catalogExerciseId`)")
                db.execSQL(
                    """
                    INSERT INTO workout_template_exercise_new (id, templateId, catalogExerciseId, orderInTemplate, defaultSets, defaultReps, defaultWeight)
                    SELECT wte.id,
                           wte.templateId,
                           COALESCE(ec.sourceExerciseId, 'custom:' || wt.userId || ':' || ec.id, 'legacy:' || wte.exerciseId),
                           wte.orderInTemplate,
                           wte.defaultSets,
                           wte.defaultReps,
                           wte.defaultWeight
                    FROM workout_template_exercise wte
                    LEFT JOIN workout_templates wt ON wt.id = wte.templateId
                    LEFT JOIN exercise_catalog ec ON ec.id = wte.exerciseId
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE workout_template_exercise")
                db.execSQL("ALTER TABLE workout_template_exercise_new RENAME TO workout_template_exercise")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_template_exercise_templateId` ON `workout_template_exercise` (`templateId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_template_exercise_catalogExerciseId` ON `workout_template_exercise` (`catalogExerciseId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workout_performed_exercise_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `catalogExerciseId` TEXT NOT NULL,
                        `exerciseNameSnapshot` TEXT NOT NULL,
                        FOREIGN KEY(`sessionId`) REFERENCES `workout_session_performed`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_performed_exercise_new_sessionId` ON `workout_performed_exercise_new` (`sessionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_performed_exercise_new_catalogExerciseId` ON `workout_performed_exercise_new` (`catalogExerciseId`)")
                db.execSQL(
                    """
                    INSERT INTO workout_performed_exercise_new (id, sessionId, catalogExerciseId, exerciseNameSnapshot)
                    SELECT wspe.id,
                           wspe.sessionId,
                           COALESCE(ec.sourceExerciseId, 'custom:' || wspf.userId || ':' || ec.id, 'legacy:' || wspe.exerciseId),
                           wspe.exerciseNameSnapshot
                    FROM workout_performed_exercise wspe
                    LEFT JOIN workout_session_performed wspf ON wspf.id = wspe.sessionId
                    LEFT JOIN exercise_catalog ec ON ec.id = wspe.exerciseId
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE workout_performed_exercise")
                db.execSQL("ALTER TABLE workout_performed_exercise_new RENAME TO workout_performed_exercise")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_performed_exercise_sessionId` ON `workout_performed_exercise` (`sessionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_performed_exercise_catalogExerciseId` ON `workout_performed_exercise` (`catalogExerciseId`)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN password TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE users ADD COLUMN firstName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE users ADD COLUMN lastName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE users ADD COLUMN age INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN gender TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE users ADD COLUMN avatarUri TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN height REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN weight REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN shoulders REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN waist REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN hips REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN chest REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN measurementDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE users ADD COLUMN goalName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE users ADD COLUMN goalDeadline TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE users ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `nutrition_profiles` (
                        `userId` TEXT NOT NULL,
                        `sex` TEXT,
                        `age` INTEGER,
                        `heightCm` INTEGER,
                        `weightKg` REAL,
                        `goal` TEXT,
                        `favoriteIngredientsJson` TEXT NOT NULL,
                        `dislikedIngredientsJson` TEXT NOT NULL,
                        `allergiesJson` TEXT NOT NULL,
                        `dietSettingsJson` TEXT,
                        `customCalories` INTEGER,
                        `customProtein` INTEGER,
                        `customFats` INTEGER,
                        `customCarbs` INTEGER,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`),
                        FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_nutrition_profiles_userId` ON `nutrition_profiles` (`userId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `meal_plans` (
                        `userId` TEXT NOT NULL,
                        `dateIso` TEXT NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`, `dateIso`),
                        FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_plans_userId` ON `meal_plans` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_plans_dateIso` ON `meal_plans` (`dateIso`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `article_purchases` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `articleId` TEXT NOT NULL,
                        `cost` INTEGER NOT NULL,
                        `purchasedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_article_purchases_userId` ON `article_purchases` (`userId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_article_purchases_userId_articleId` ON `article_purchases` (`userId`, `articleId`)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `article_purchases`")
            }
        }

        fun getInstance(context: Context): WorkoutTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutTrackerDatabase::class.java,
                    "workout_tracker.db"
                ).addMigrations(
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9
                )
                    .build().also { INSTANCE = it }
            }
        }
    }
}
