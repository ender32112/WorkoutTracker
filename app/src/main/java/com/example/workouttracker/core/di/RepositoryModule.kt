package com.example.workouttracker.core.di

import com.example.workouttracker.data.exercise.ExerciseCatalogDao
import com.example.workouttracker.data.exercise.ExerciseRepository
import com.example.workouttracker.data.exercise.ExerciseSeedLoader
import com.example.workouttracker.data.local.AnalyticsRepository
import com.example.workouttracker.data.local.ArticleRepository
import com.example.workouttracker.data.local.NutritionRepository
import com.example.workouttracker.data.local.UserRepository
import com.example.workouttracker.data.local.WorkoutTrackerDao
import com.example.workouttracker.data.local.ProductRepository
import com.example.workouttracker.data.local.WeightSyncRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideUserRepository(
        dao: WorkoutTrackerDao
    ): UserRepository = UserRepository(dao)

    @Provides
    @Singleton
    fun provideNutritionRepository(
        dao: WorkoutTrackerDao
    ): NutritionRepository = NutritionRepository(dao)

    @Provides
    @Singleton
    fun provideAnalyticsRepository(
        dao: WorkoutTrackerDao
    ): AnalyticsRepository = AnalyticsRepository(dao)

    @Provides
    @Singleton
    fun provideWeightSyncRepository(
        dao: WorkoutTrackerDao
    ): WeightSyncRepository = WeightSyncRepository(dao)

    @Provides
    @Singleton
    fun provideArticleRepository(
        dao: WorkoutTrackerDao
    ): ArticleRepository = ArticleRepository(dao)

    @Provides
    @Singleton
    fun provideProductRepository(
        dao: WorkoutTrackerDao
    ): ProductRepository = ProductRepository(dao)

    @Provides
    @Singleton
    fun provideExerciseRepository(
        dao: ExerciseCatalogDao,
        seedLoader: ExerciseSeedLoader
    ): ExerciseRepository = ExerciseRepository(dao, seedLoader)
}
