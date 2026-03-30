package com.entreprisekilde.app.data.repository.users

import com.entreprisekilde.app.data.model.auth.LoginResult
import com.entreprisekilde.app.data.model.users.User

class DemoUsersRepository : UserRepository {

    private var nextId = 7
    private var currentLoggedInUser: User? = null
    private var authObserver: ((String?) -> Unit)? = null

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

    override suspend fun getUsers(): List<User> {
        return users.toList()
    }

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
            authObserver?.invoke(matchedUser.id)
            LoginResult.Success(matchedUser)
        } else {
            LoginResult.Error("Invalid username or password.")
        }
    }

    override suspend fun getUserById(userId: String): User? {
        return users.firstOrNull { it.id == userId }
    }

    override fun getCurrentAuthUserId(): String? {
        return currentLoggedInUser?.id
    }

    override fun observeAuthState(onAuthUserChanged: (String?) -> Unit) {
        authObserver = onAuthUserChanged
        onAuthUserChanged(currentLoggedInUser?.id)
    }

    override fun stopObservingAuthState() {
        authObserver = null
    }

    override fun logout() {
        currentLoggedInUser = null
        authObserver?.invoke(null)
    }

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

    override suspend fun updateUser(updatedUser: User): Result<Unit> {
        val index = users.indexOfFirst { it.id == updatedUser.id }

        return if (index != -1) {
            users[index] = updatedUser
            Result.success(Unit)
        } else {
            Result.failure(Exception("User not found"))
        }
    }

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