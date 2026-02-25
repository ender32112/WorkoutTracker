package com.example.workouttracker.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "weight_entries",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val weightKg: Float,
    val loggedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "step_entries",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class StepEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val dateIso: String,
    val steps: Long
)

@Entity(
    tableName = "exercise_catalog",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId"), Index("isBase"), Index("isFavorite")]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val name: String,
    val aliases: String? = null,
    val muscles: String,
    val equipment: String? = null,
    val isFavorite: Boolean = false,
    val photoUri: String? = null,
    val isBase: Boolean = false,
    val lastUsedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "workout_templates",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class WorkoutTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val title: String,
    val notes: String? = null
)

@Entity(
    tableName = "workout_template_exercise",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateId"), Index("exerciseId")]
)
data class WorkoutTemplateExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val exerciseId: Long,
    val orderInTemplate: Int,
    val defaultSets: Int = 3,
    val defaultReps: Int = 10,
    val defaultWeight: Float? = null
)

@Entity(
    tableName = "workout_session_performed",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class WorkoutSessionPerformedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val startedAt: Long,
    val finishedAt: Long,
    val notes: String? = null
)

@Entity(
    tableName = "workout_performed_exercise",
    foreignKeys = [ForeignKey(
        entity = WorkoutSessionPerformedEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class WorkoutPerformedExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseNameSnapshot: String
)

@Entity(
    tableName = "workout_set_performed",
    foreignKeys = [ForeignKey(
        entity = WorkoutPerformedExerciseEntity::class,
        parentColumns = ["id"],
        childColumns = ["performedExerciseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("performedExerciseId")]
)
data class WorkoutSetPerformedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val performedExerciseId: Long,
    val setOrder: Int,
    val weight: Float,
    val reps: Int
)

@Entity(
    tableName = "active_workout_state",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class ActiveWorkoutStateEntity(
    @PrimaryKey val userId: String,
    val startedAt: Long,
    val updatedAt: Long,
    val payloadJson: String
)
