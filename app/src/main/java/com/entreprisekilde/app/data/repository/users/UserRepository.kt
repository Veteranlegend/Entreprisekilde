package com.entreprisekilde.app.data.repository.users

import com.entreprisekilde.app.data.model.auth.LoginResult
import com.entreprisekilde.app.data.model.users.User

interface UserRepository {

    suspend fun getUsers(): List<User>

    suspend fun login(username: String, password: String): LoginResult

    suspend fun getUserById(userId: String): User?

    fun getCurrentAuthUserId(): String?

    fun observeAuthState(onAuthUserChanged: (String?) -> Unit)

    fun stopObservingAuthState()

    fun logout()

    suspend fun addUser(
        firstName: String,
        lastName: String,
        email: String,
        phoneNumber: String,
        username: String,
        password: String,
        role: String
    ): Result<Unit>

    suspend fun updateUser(updatedUser: User): Result<Unit>
    suspend fun deleteUser(userId: String): Result<Unit>

    suspend fun changeOwnPassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit>
}