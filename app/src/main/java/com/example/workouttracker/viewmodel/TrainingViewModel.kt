package com.example.workouttracker.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.workouttracker.ui.training.ExerciseCatalogItem
import com.example.workouttracker.ui.training.ExerciseEntry
import com.example.workouttracker.ui.training.TrainingSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class TrainingViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("training_prefs", Context.MODE_PRIVATE)

    private val _sessions = MutableStateFlow<List<TrainingSession>>(emptyList())
    val sessions: StateFlow<List<TrainingSession>> = _sessions

    // Каталог упражнений
    private val _exerciseCatalog = MutableStateFlow<List<ExerciseCatalogItem>>(emptyList())
    val exerciseCatalog: StateFlow<List<ExerciseCatalogItem>> = _exerciseCatalog

    init {
        loadSessions()
        loadExerciseCatalog()
        if (_exerciseCatalog.value.isEmpty()) seedDefaultCatalog()
    }

    // -------- Тренировки --------
    private fun loadSessions() {
        val jsonString = prefs.getString("sessions", null) ?: return
        val jsonArray = JSONArray(jsonString)
        val list = mutableListOf<TrainingSession>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val exercises = mutableListOf<ExerciseEntry>()
            val exArray = obj.getJSONArray("exercises")
            for (j in 0 until exArray.length()) {
                val ex = exArray.getJSONObject(j)
                exercises.add(
                    ExerciseEntry(
                        name = ex.getString("name"),
                        sets = ex.getInt("sets"),
                        reps = ex.getInt("reps"),
                        weight = ex.getDouble("weight").toFloat()
                    )
                )
            }
            list.add(
                TrainingSession(
                    id = UUID.fromString(obj.getString("id")),
                    date = obj.getString("date"),
                    exercises = exercises
                )
            )
        }
        _sessions.value = list
    }

    private fun saveSessions() {
        val jsonArray = JSONArray()
        _sessions.value.forEach { session ->
            val obj = JSONObject().apply {
                put("id", session.id.toString())
                put("date", session.date)
                val exArray = JSONArray()
                session.exercises.forEach { ex ->
                    exArray.put(
                        JSONObject().apply {
                            put("name", ex.name)
                            put("sets", ex.sets)
                            put("reps", ex.reps)
                            put("weight", ex.weight)
                        }
                    )
                }
                put("exercises", exArray)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("sessions", jsonArray.toString()).apply()
    }

    fun addSession(session: TrainingSession) {
        _sessions.value = _sessions.value + session
        saveSessions()
    }

    fun removeSession(id: String) {
        _sessions.value = _sessions.value.filterNot { it.id.toString() == id }
        saveSessions()
    }

    fun updateSession(updated: TrainingSession) {
        _sessions.value = _sessions.value.map { if (it.id == updated.id) updated else it }
        saveSessions()
    }

    // -------- Каталог упражнений --------
    private fun loadExerciseCatalog() {
        val json = prefs.getString("exercise_catalog", null) ?: return
        val arr = JSONArray(json)
        val items = mutableListOf<ExerciseCatalogItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            items.add(
                ExerciseCatalogItem(
                    id = UUID.fromString(o.getString("id")),
                    name = o.getString("name"),
                    photoUri = if (o.has("photoUri") && !o.isNull("photoUri")) o.getString("photoUri") else null
                )
            )
        }
        _exerciseCatalog.value = items
    }

    private fun saveExerciseCatalog() {
        val arr = JSONArray()
        _exerciseCatalog.value.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id.toString())
                    put("name", item.name)
                    put("photoUri", item.photoUri)
                }
            )
        }
        prefs.edit().putString("exercise_catalog", arr.toString()).apply()
    }

    private fun seedDefaultCatalog() {
        _exerciseCatalog.value = listOf(
            ExerciseCatalogItem(name = "Жим штанги лежа"),
            ExerciseCatalogItem(name = "Присед"),
            ExerciseCatalogItem(name = "Становая тяга"),
            ExerciseCatalogItem(name = "Тяга верхнего блока"),
            ExerciseCatalogItem(name = "Подтягивания"),
            ExerciseCatalogItem(name = "Отжимания"),
            ExerciseCatalogItem(name = "Жим гантелей"),
            ExerciseCatalogItem(name = "Выпады")
        )
        saveExerciseCatalog()
    }

    fun addCatalogItem(name: String, photoUri: String?) {
        _exerciseCatalog.value = _exerciseCatalog.value + ExerciseCatalogItem(name = name, photoUri = photoUri)
        saveExerciseCatalog()
    }

    fun updateCatalogItem(updated: ExerciseCatalogItem) {
        _exerciseCatalog.value = _exerciseCatalog.value.map { if (it.id == updated.id) updated else it }
        saveExerciseCatalog()
    }

    fun removeCatalogItem(id: UUID) {
        _exerciseCatalog.value = _exerciseCatalog.value.filterNot { it.id == id }
        saveExerciseCatalog()
    }
}
