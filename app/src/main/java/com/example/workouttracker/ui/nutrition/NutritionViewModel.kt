package com.example.workouttracker.viewmodel

import androidx.lifecycle.ViewModel
import com.example.workouttracker.ui.nutrition.NutritionEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class NutritionViewModel : ViewModel() {
    private val _entries = MutableStateFlow<List<NutritionEntry>>(emptyList())
    val entries: StateFlow<List<NutritionEntry>> = _entries
    fun addEntry(entry: NutritionEntry) {
        _entries.value = _entries.value + entry
    }
    fun removeEntry(id: UUID) {
        _entries.value = _entries.value.filterNot { it.id == id }
    }
}
