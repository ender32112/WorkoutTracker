package com.example.workouttracker.data.local

import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val dao: WorkoutTrackerDao
) {
    fun observeUser(userId: String): Flow<UserEntity?> = dao.observeUser(userId)

    suspend fun getUserById(userId: String): UserEntity? = dao.getUserById(userId)

    suspend fun getUserByEmail(email: String): UserEntity? = dao.getUserByEmail(email)

    suspend fun upsertUser(user: UserEntity) = dao.upsertUser(user)

    suspend fun getAllUsers(): List<UserEntity> = dao.getAllUsers()
}
