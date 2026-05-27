package com.example.workouttracker.core.di

import android.content.Context
import com.example.workouttracker.data.exercise.ExerciseCatalogDao
import com.example.workouttracker.data.local.WorkoutTrackerDao
import com.example.workouttracker.data.local.WorkoutTrackerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): WorkoutTrackerDatabase = WorkoutTrackerDatabase.getInstance(context)

    @Provides
    fun provideWorkoutTrackerDao(
        database: WorkoutTrackerDatabase
    ): WorkoutTrackerDao = database.dao()

    @Provides
    fun provideExerciseCatalogDao(
        database: WorkoutTrackerDatabase
    ): ExerciseCatalogDao = database.exerciseCatalogDao()
}
