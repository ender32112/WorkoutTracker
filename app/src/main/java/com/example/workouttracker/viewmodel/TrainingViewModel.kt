package com.example.workouttracker.viewmodel

import androidx.lifecycle.ViewModel
import com.example.workouttracker.ui.training.TrainingSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TrainingViewModel : ViewModel() {

    private val _sessions = MutableStateFlow<List<TrainingSession>>(emptyList())
    val sessions: StateFlow<List<TrainingSession>> = _sessions

    fun addSession(session: TrainingSession) {
        _sessions.value = _sessions.value + session
    }

    fun removeSession(id: String) {
        _sessions.value = _sessions.value.filterNot { it.id.toString() == id }
    }
}