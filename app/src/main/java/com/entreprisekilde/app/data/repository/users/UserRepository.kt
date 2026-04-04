package com.entreprisekilde.app.data.repository.users

import com.entreprisekilde.app.data.model.auth.LoginResult
import com.entreprisekilde.app.data.model.users.User

/**
 * Contract for all user-related data operations in the app.
 *
 * This interface defines the user and authentication actions the rest of the app
 * can rely on, while hiding the actual implementation details.
 *
 * That means the app can work with:
 * - a demo/in-memory repository
 * - Firebase/Auth backend
 * - local database storage
 * - any future user source
 *
 * ...as long as it follows this contract.
 */
interface UserRepository {

    /**
     * Fetches all users.
     *
     * Typically used in admin flows, assignment screens, user lists, etc.
     */
    suspend fun getUsers(): List<User>

    /**
     * Attempts to log in a user with the provided credentials.
     *
     * Returns:
     * - [LoginResult.Success] if credentials are valid
     * - [LoginResult.Error] if login fails
     */
    suspend fun login(username: String, password: String): LoginResult

    /**
     * Returns a single user by ID, or null if no matching user exists.
     */
    suspend fun getUserById(userId: String): User?

    /**
     * Returns the currently authenticated user's ID, if someone is logged in.
     */
    fun getCurrentAuthUserId(): String?

    /**
     * Starts observing authentication state changes.
     *
     * The callback should be triggered whenever the logged-in user changes,
     * for example:
     * - user logs in
     * - user logs out
     * - auth session expires
     *
     * The value is the current user ID, or null if no user is authenticated.
     */
    fun observeAuthState(onAuthUserChanged: (String?) -> Unit)

    /**
     * Stops observing authentication state changes.
     *
     * Useful for cleanup to avoid leaks or duplicate listeners.
     */
    fun stopObservingAuthState()

    /**
     * Logs out the currently authenticated user.
     */
    fun logout()

    /**
     * Adds a new user.
     *
     * Returns [Result.success] on success or [Result.failure] if creation fails.
     */
    suspend fun addUser(
        firstName: String,
        lastName: String,
        email: String,
        phoneNumber: String,
        username: String,
        password: String,
        role: String
    ): Result<Unit>

    /**
     * Updates an existing user's data.
     */
    suspend fun updateUser(updatedUser: User): Result<Unit>

    /**
     * Deletes a user by ID.
     */
    suspend fun deleteUser(userId: String): Result<Unit>

    /**
     * Changes the currently logged-in user's password.
     *
     * A real implementation would usually:
     * - verify the current password
     * - validate the new password
     * - update it securely in the backend/auth provider
     */
    suspend fun changeOwnPassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit>
}