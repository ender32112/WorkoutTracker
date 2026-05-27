package com.example.workouttracker.data.exercise

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ExerciseSource {
    GLOBAL,
    CUSTOM
}

@Entity(
    tableName = "exercises",
    indices = [
        Index("userId"),
        Index("bodyPart"),
        Index("equipment"),
        Index("isCustom"),
        Index("source")
    ]
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val nameEn: String,
    val nameRu: String,
    val gifUrl: String,
    val localMediaUri: String? = null,
    val bodyPart: String,
    val targetMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val equipment: String,
    val instructions: List<String>,
    val source: ExerciseSource = ExerciseSource.GLOBAL,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "exercise_user_meta",
    primaryKeys = ["userId", "exerciseId"],
    indices = [
        Index("userId"),
        Index("exerciseId")
    ]
)
data class ExerciseUserMetaEntity(
    val userId: String,
    val exerciseId: String,
    val isFavorite: Boolean = false,
    val lastUsedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
