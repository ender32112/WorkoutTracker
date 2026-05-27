package com.example.workouttracker.data.exercise

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ExerciseTypeConverters {
    private val gson = Gson()
    private val listType = object : TypeToken<List<String>>() {}.type

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching { gson.fromJson<List<String>>(value, listType) }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromExerciseSource(value: ExerciseSource): String = value.name

    @TypeConverter
    fun toExerciseSource(value: String?): ExerciseSource =
        runCatching { ExerciseSource.valueOf(value.orEmpty()) }.getOrDefault(ExerciseSource.GLOBAL)
}
