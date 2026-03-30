package com.entreprisekilde.app.data.repository.users

import com.entreprisekilde.app.data.model.auth.LoginResult
import com.entreprisekilde.app.data.model.users.EmployeeUser

interface UserRepository {

    suspend fun getUsers(): List<EmployeeUser>

    suspend fun login(username: String, password: String): LoginResult

    suspend fun getUserById(userId: String): EmployeeUser?

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
        password: String
    ): Result<Unit>

    suspend fun updateUser(updatedUser: EmployeeUser): Result<Unit>

    suspend fun changeOwnPassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit>
}