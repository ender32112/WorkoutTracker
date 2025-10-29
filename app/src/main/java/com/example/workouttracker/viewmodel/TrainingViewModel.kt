package com.example.workouttracker.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.workouttracker.ui.training.TrainingSession
import com.example.workouttracker.ui.training.ExerciseEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class TrainingViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("training_prefs", Context.MODE_PRIVATE)

    private val _sessions = MutableStateFlow<List<TrainingSession>>(emptyList())
    val sessions: StateFlow<List<TrainingSession>> = _sessions

    init {
        loadSessions()
    }

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
}