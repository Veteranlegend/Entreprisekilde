package com.entreprisekilde.app.data.repository.users

import com.entreprisekilde.app.data.model.auth.LoginResult
import com.entreprisekilde.app.data.model.users.User

/**
 * In-memory demo implementation of [UserRepository].
 *
 * This is used for:
 * - development without backend
 * - testing authentication flows
 * - quick UI iteration
 *
 * Important:
 * - No real persistence → everything resets when app restarts
 * - Passwords are stored in plain text (ONLY OK for demo)
 */
class DemoUsersRepository : UserRepository {

    /**
     * Simple incrementing ID generator for new users.
     */
    private var nextId = 7

    /**
     * Holds the currently logged-in user (if any).
     */
    private var currentLoggedInUser: User? = null

    /**
     * Observer for authentication state changes.
     *
     * This mimics something like FirebaseAuth listeners.
     */
    private var authObserver: ((String?) -> Unit)? = null

    /**
     * In-memory user list acting as our "database".
     */
    private val users = mutableListOf(
        User(
            id = "1",
            firstName = "Rasmus",
            lastName = "Jensen",
            email = "rasmus.jensen@entreprisekilde.dk",
            phoneNumber = "12341234",
            username = "rasmus",
            password = "1234",
            role = "employee"
        ),
        User(
            id = "2",
            firstName = "Tomas",
            lastName = "Larsen",
            email = "tomas.larsen@entreprisekilde.dk",
            phoneNumber = "22334455",
            username = "tomas",
            password = "1234",
            role = "admin"
        ),
        User(
            id = "3",
            firstName = "Peter",
            lastName = "Hansen",
            email = "peter.hansen@entreprisekilde.dk",
            phoneNumber = "33445566",
            username = "peter",
            password = "1234",
            role = "employee"
        ),
        User(
            id = "4",
            firstName = "John",
            lastName = "Miller",
            email = "john.miller@entreprisekilde.dk",
            phoneNumber = "44556677",
            username = "john",
            password = "1234",
            role = "employee"
        ),
        User(
            id = "5",
            firstName = "Ahmad",
            lastName = "El Haj",
            email = "ahmad.elhaj@entreprisekilde.dk",
            phoneNumber = "55667788",
            username = "ahmad",
            password = "1234",
            role = "employee"
        ),
        User(
            id = "6",
            firstName = "Lars",
            lastName = "Nielsen",
            email = "lars.nielsen@entreprisekilde.dk",
            phoneNumber = "66778899",
            username = "lars",
            password = "1234",
            role = "employee"
        )
    )

    /**
     * Deletes a user by ID.
     *
     * If the deleted user is currently logged in,
     * we also clear auth state and notify observers.
     */
    override suspend fun deleteUser(userId: String): Result<Unit> {
        val userToRemove = users.firstOrNull { it.id == userId }
            ?: return Result.failure(Exception("User not found."))

        users.remove(userToRemove)

        // If we just deleted the logged-in user → log them out
        if (currentLoggedInUser?.id == userId) {
            currentLoggedInUser = null
            authObserver?.invoke(null)
        }

        return Result.success(Unit)
    }

    /**
     * Returns all users (copy of the list to avoid external mutation).
     */
    override suspend fun getUsers(): List<User> {
        return users.toList()
    }

    /**
     * Handles login logic.
     *
     * Steps:
     * 1. Trim input (avoid issues with accidental spaces)
     * 2. Validate non-empty fields
     * 3. Find matching user
     * 4. Update auth state if successful
     */
    override suspend fun login(username: String, password: String): LoginResult {
        val cleanUsername = username.trim()
        val cleanPassword = password.trim()

        if (cleanUsername.isBlank() || cleanPassword.isBlank()) {
            return LoginResult.Error("Please enter both username and password.")
        }

        val matchedUser = users.find {
            it.username == cleanUsername && it.password == cleanPassword
        }

        return if (matchedUser != null) {
            currentLoggedInUser = matchedUser

            // Notify observers that a user is now logged in
            authObserver?.invoke(matchedUser.id)

            LoginResult.Success(matchedUser)
        } else {
            LoginResult.Error("Invalid username or password.")
        }
    }

    /**
     * Returns a user by ID, or null if not found.
     */
    override suspend fun getUserById(userId: String): User? {
        return users.firstOrNull { it.id == userId }
    }

    /**
     * Returns the ID of the currently authenticated user (if any).
     */
    override fun getCurrentAuthUserId(): String? {
        return currentLoggedInUser?.id
    }

    /**
     * Starts observing authentication state changes.
     *
     * Immediately emits current state so UI is always in sync.
     */
    override fun observeAuthState(onAuthUserChanged: (String?) -> Unit) {
        authObserver = onAuthUserChanged
        onAuthUserChanged(currentLoggedInUser?.id)
    }

    /**
     * Stops observing authentication state.
     */
    override fun stopObservingAuthState() {
        authObserver = null
    }

    /**
     * Logs out the current user and notifies observers.
     */
    override fun logout() {
        currentLoggedInUser = null
        authObserver?.invoke(null)
    }

    /**
     * Adds a new user to the in-memory list.
     *
     * Generates a new ID automatically.
     */
    override suspend fun addUser(
        firstName: String,
        lastName: String,
        email: String,
        phoneNumber: String,
        username: String,
        password: String,
        role: String
    ): Result<Unit> {
        val newUser = User(
            id = nextId.toString(),
            firstName = firstName,
            lastName = lastName,
            email = email,
            phoneNumber = phoneNumber,
            username = username,
            password = password,
            role = role
        )

        nextId++
        users.add(newUser)

        return Result.success(Unit)
    }

    /**
     * Updates an existing user.
     *
     * Replaces the entire user object if found.
     */
    override suspend fun updateUser(updatedUser: User): Result<Unit> {
        val index = users.indexOfFirst { it.id == updatedUser.id }

        return if (index != -1) {
            users[index] = updatedUser
            Result.success(Unit)
        } else {
            Result.failure(Exception("User not found"))
        }
    }

    /**
     * Changes the current user's password.
     *
     * NOTE:
     * This demo implementation does NOT actually update the stored password.
     * It only validates input and returns success.
     *
     * In a real implementation:
     * - verify current password matches
     * - update stored password securely
     */
    override suspend fun changeOwnPassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        return if (currentPassword.isBlank() || newPassword.isBlank()) {
            Result.failure(Exception("Password fields cannot be empty."))
        } else {
            Result.success(Unit)
        }
    }
}