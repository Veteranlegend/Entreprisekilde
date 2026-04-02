package com.entreprisekilde.app.users

import com.entreprisekilde.app.data.model.auth.LoginResult
import com.entreprisekilde.app.data.model.users.User
import com.entreprisekilde.app.data.repository.users.UserRepository
import com.entreprisekilde.app.viewmodel.UsersViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UsersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsUsersIntoState() = runTest {
        // Arrange
        val fakeUsers = listOf(
            User(
                id = "1",
                firstName = "Ahmad",
                lastName = "Ali",
                email = "ahmad@test.com",
                phoneNumber = "12345678",
                username = "ahmad",
                password = "123456",
                role = "admin"
            ),
            User(
                id = "2",
                firstName = "Sara",
                lastName = "Hassan",
                email = "sara@test.com",
                phoneNumber = "87654321",
                username = "sara",
                password = "123456",
                role = "employee"
            )
        )

        val fakeRepository = object : UserRepository {
            override suspend fun getUsers() = fakeUsers
            override suspend fun login(username: String, password: String) = LoginResult.Error("Not needed")
            override suspend fun getUserById(userId: String) = null
            override fun getCurrentAuthUserId() = null
            override fun observeAuthState(onAuthUserChanged: (String?) -> Unit) {}
            override fun stopObservingAuthState() {}
            override fun logout() {}

            override suspend fun addUser(
                firstName: String,
                lastName: String,
                email: String,
                phoneNumber: String,
                username: String,
                password: String,
                role: String
            ) = Result.success(Unit)

            override suspend fun updateUser(updatedUser: User) = Result.success(Unit)
            override suspend fun deleteUser(userId: String) = Result.success(Unit)
            override suspend fun changeOwnPassword(currentPassword: String, newPassword: String) = Result.success(Unit)
        }

        // Act
        val viewModel = UsersViewModel(fakeRepository)
        advanceUntilIdle()

        // Assert
        assertEquals(2, viewModel.users.size)
        assertEquals("ahmad", viewModel.users[0].username)
        assertEquals("sara", viewModel.users[1].username)
    }

    @Test
    fun selectUser_andClearSelectedUser_updateSelectedUserState() {
        // Arrange
        val fakeRepository = object : UserRepository {
            override suspend fun getUsers() = emptyList<User>()
            override suspend fun login(username: String, password: String) = LoginResult.Error("Not needed")
            override suspend fun getUserById(userId: String) = null
            override fun getCurrentAuthUserId() = null
            override fun observeAuthState(onAuthUserChanged: (String?) -> Unit) {}
            override fun stopObservingAuthState() {}
            override fun logout() {}

            override suspend fun addUser(
                firstName: String,
                lastName: String,
                email: String,
                phoneNumber: String,
                username: String,
                password: String,
                role: String
            ) = Result.success(Unit)

            override suspend fun updateUser(updatedUser: User) = Result.success(Unit)
            override suspend fun deleteUser(userId: String) = Result.success(Unit)
            override suspend fun changeOwnPassword(currentPassword: String, newPassword: String) = Result.success(Unit)
        }

        val viewModel = UsersViewModel(fakeRepository)

        val user = User(
            id = "1",
            firstName = "Ahmad",
            lastName = "Ali",
            email = "ahmad@test.com",
            phoneNumber = "12345678",
            username = "ahmad",
            password = "123456",
            role = "admin"
        )

        // Act
        viewModel.selectUser(user)

        // Assert
        assertEquals("ahmad", viewModel.selectedUser?.username)

        // Act
        viewModel.clearSelectedUser()

        // Assert
        assertEquals(null, viewModel.selectedUser)
    }

    @Test
    fun addUser_addsUserAndSetsSuccessMessage() = runTest {
        // Arrange
        val fakeUsers = mutableListOf(
            User(
                id = "1",
                firstName = "Ahmad",
                lastName = "Ali",
                email = "ahmad@test.com",
                phoneNumber = "12345678",
                username = "ahmad",
                password = "123456",
                role = "admin"
            )
        )

        val fakeRepository = object : UserRepository {
            override suspend fun getUsers(): List<User> = fakeUsers.toList()

            override suspend fun login(username: String, password: String): LoginResult {
                return LoginResult.Error("Not needed")
            }

            override suspend fun getUserById(userId: String): User? = null

            override fun getCurrentAuthUserId(): String? = null

            override fun observeAuthState(onAuthUserChanged: (String?) -> Unit) {}

            override fun stopObservingAuthState() {}

            override fun logout() {}

            override suspend fun addUser(
                firstName: String,
                lastName: String,
                email: String,
                phoneNumber: String,
                username: String,
                password: String,
                role: String
            ): Result<Unit> {
                fakeUsers.add(
                    User(
                        id = "2",
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phoneNumber = phoneNumber,
                        username = username,
                        password = password,
                        role = role
                    )
                )
                return Result.success(Unit)
            }

            override suspend fun updateUser(updatedUser: User): Result<Unit> = Result.success(Unit)

            override suspend fun deleteUser(userId: String): Result<Unit> = Result.success(Unit)

            override suspend fun changeOwnPassword(
                currentPassword: String,
                newPassword: String
            ): Result<Unit> = Result.success(Unit)
        }

        val viewModel = UsersViewModel(fakeRepository)
        advanceUntilIdle()

        // Act
        viewModel.addUser(
            firstName = "Sara",
            lastName = "Hassan",
            email = "sara@test.com",
            phoneNumber = "87654321",
            username = "sara",
            password = "123456",
            role = "employee"
        )
        advanceUntilIdle()

        // Assert
        assertEquals(2, viewModel.users.size)
        assertEquals("sara", viewModel.users[1].username)
        assertEquals("User created successfully.", viewModel.createUserMessage)
    }

    @Test
    fun deleteUser_deletesUserClearsSelectionAndSetsSuccessMessage() = runTest {
        // Arrange
        val fakeUsers = mutableListOf(
            User(
                id = "1",
                firstName = "Ahmad",
                lastName = "Ali",
                email = "ahmad@test.com",
                phoneNumber = "12345678",
                username = "ahmad",
                password = "123456",
                role = "admin"
            ),
            User(
                id = "2",
                firstName = "Sara",
                lastName = "Hassan",
                email = "sara@test.com",
                phoneNumber = "87654321",
                username = "sara",
                password = "123456",
                role = "employee"
            )
        )

        val fakeRepository = object : UserRepository {
            override suspend fun getUsers(): List<User> = fakeUsers.toList()

            override suspend fun login(username: String, password: String): LoginResult {
                return LoginResult.Error("Not needed")
            }

            override suspend fun getUserById(userId: String): User? = null

            override fun getCurrentAuthUserId(): String? = null

            override fun observeAuthState(onAuthUserChanged: (String?) -> Unit) {}

            override fun stopObservingAuthState() {}

            override fun logout() {}

            override suspend fun addUser(
                firstName: String,
                lastName: String,
                email: String,
                phoneNumber: String,
                username: String,
                password: String,
                role: String
            ): Result<Unit> = Result.success(Unit)

            override suspend fun updateUser(updatedUser: User): Result<Unit> = Result.success(Unit)

            override suspend fun deleteUser(userId: String): Result<Unit> {
                fakeUsers.removeAll { it.id == userId }
                return Result.success(Unit)
            }

            override suspend fun changeOwnPassword(
                currentPassword: String,
                newPassword: String
            ): Result<Unit> = Result.success(Unit)
        }

        val viewModel = UsersViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.selectUser(viewModel.users.first { it.id == "2" })

        // Act
        viewModel.deleteUser("2")
        advanceUntilIdle()

        // Assert
        assertEquals(1, viewModel.users.size)
        assertTrue(viewModel.users.none { it.id == "2" })
        assertEquals(null, viewModel.selectedUser)
        assertEquals("User deleted successfully.", viewModel.deleteUserMessage)
    }

    @Test
    fun login_whenSuccessful_setsLoggedInUser() = runTest {
        // Arrange
        val loggedIn = User(
            id = "1",
            firstName = "Ahmad",
            lastName = "Ali",
            email = "ahmad@test.com",
            phoneNumber = "12345678",
            username = "ahmad",
            password = "123456",
            role = "admin"
        )

        val fakeRepository = object : UserRepository {
            override suspend fun getUsers(): List<User> = emptyList()

            override suspend fun login(username: String, password: String): LoginResult {
                return LoginResult.Success(loggedIn)
            }

            override suspend fun getUserById(userId: String): User? = null

            override fun getCurrentAuthUserId(): String? = null

            override fun observeAuthState(onAuthUserChanged: (String?) -> Unit) {}

            override fun stopObservingAuthState() {}

            override fun logout() {}

            override suspend fun addUser(
                firstName: String,
                lastName: String,
                email: String,
                phoneNumber: String,
                username: String,
                password: String,
                role: String
            ): Result<Unit> = Result.success(Unit)

            override suspend fun updateUser(updatedUser: User): Result<Unit> = Result.success(Unit)

            override suspend fun deleteUser(userId: String): Result<Unit> = Result.success(Unit)

            override suspend fun changeOwnPassword(
                currentPassword: String,
                newPassword: String
            ): Result<Unit> = Result.success(Unit)
        }

        val viewModel = UsersViewModel(fakeRepository)
        advanceUntilIdle()

        // Act
        viewModel.login("ahmad", "123456")
        advanceUntilIdle()

        // Assert
        assertEquals("ahmad", viewModel.loggedInUser?.username)
        assertEquals(null, viewModel.loginErrorMessage)
        assertEquals(false, viewModel.isLoading)
    }


    @Test
    fun login_whenCredentialsAreWrong_setsErrorMessages() = runTest {
        // Arrange
        val fakeRepository = object : UserRepository {
            override suspend fun getUsers(): List<User> = emptyList()

            override suspend fun login(username: String, password: String): LoginResult {
                return LoginResult.Error("Invalid username or password.")
            }

            override suspend fun getUserById(userId: String): User? = null

            override fun getCurrentAuthUserId(): String? = null

            override fun observeAuthState(onAuthUserChanged: (String?) -> Unit) {}

            override fun stopObservingAuthState() {}

            override fun logout() {}

            override suspend fun addUser(
                firstName: String,
                lastName: String,
                email: String,
                phoneNumber: String,
                username: String,
                password: String,
                role: String
            ): Result<Unit> = Result.success(Unit)

            override suspend fun updateUser(updatedUser: User): Result<Unit> = Result.success(Unit)

            override suspend fun deleteUser(userId: String): Result<Unit> = Result.success(Unit)

            override suspend fun changeOwnPassword(
                currentPassword: String,
                newPassword: String
            ): Result<Unit> = Result.success(Unit)
        }

        val viewModel = UsersViewModel(fakeRepository)
        advanceUntilIdle()

        // Act
        viewModel.login("wrongUser", "wrongPassword")
        advanceUntilIdle()

        // Assert
        assertEquals(null, viewModel.loggedInUser)
        assertEquals("Invalid username or password.", viewModel.loginErrorMessage)
        assertEquals("Please try again.", viewModel.loginInfoMessage)
        assertEquals(false, viewModel.isLoading)
    }

    @Test
    fun logout_clearsUserAndResetsState() = runTest {
        // Arrange
        val user = User(
            id = "1",
            firstName = "Ahmad",
            lastName = "Ali",
            email = "ahmad@test.com",
            phoneNumber = "12345678",
            username = "ahmad",
            password = "123456",
            role = "admin"
        )

        val fakeRepository = object : UserRepository {
            override suspend fun getUsers(): List<User> = emptyList()

            override suspend fun login(username: String, password: String): LoginResult {
                return LoginResult.Success(user)
            }

            override suspend fun getUserById(userId: String): User? = null

            override fun getCurrentAuthUserId(): String? = null

            override fun observeAuthState(onAuthUserChanged: (String?) -> Unit) {}

            override fun stopObservingAuthState() {}

            override fun logout() {}

            override suspend fun addUser(
                firstName: String,
                lastName: String,
                email: String,
                phoneNumber: String,
                username: String,
                password: String,
                role: String
            ): Result<Unit> = Result.success(Unit)

            override suspend fun updateUser(updatedUser: User): Result<Unit> = Result.success(Unit)

            override suspend fun deleteUser(userId: String): Result<Unit> = Result.success(Unit)

            override suspend fun changeOwnPassword(
                currentPassword: String,
                newPassword: String
            ): Result<Unit> = Result.success(Unit)
        }

        val viewModel = UsersViewModel(fakeRepository)
        advanceUntilIdle()

        // Simulate logged in state
        viewModel.login("ahmad", "123456")
        advanceUntilIdle()

        // Act
        viewModel.logout()

        // Assert
        assertEquals(null, viewModel.loggedInUser)
        assertEquals(null, viewModel.loginErrorMessage)
        assertEquals(null, viewModel.loginInfoMessage)
        assertEquals(false, viewModel.isLoading)
        assertEquals(false, viewModel.isLocked)
    }

}