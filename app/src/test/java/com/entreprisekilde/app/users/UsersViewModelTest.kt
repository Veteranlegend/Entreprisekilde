package com.entreprisekilde.app.users

import android.app.Application
import android.content.SharedPreferences
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

    // Test dispatcher used to make coroutine-based ViewModel logic deterministic.
    // This gives the tests full control over when queued coroutines actually run.
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application

    @Before
    fun setUp() {
        // Route Main dispatcher work to the test dispatcher.
        Dispatchers.setMain(testDispatcher)

        // Use a lightweight fake Application so the ViewModel can still access
        // SharedPreferences without needing a full Android runtime environment.
        application = FakeTestApplication()
    }

    @After
    fun tearDown() {
        // Always reset Main to avoid affecting other tests.
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsUsersIntoState() = runTest {
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

        // Minimal fake repository for testing the ViewModel's initial user loading.
        // Only the behavior relevant to this test is implemented meaningfully.
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

        val viewModel = UsersViewModel(application, fakeRepository)
        advanceUntilIdle()

        // The ViewModel should expose the fetched users in the same order
        // returned by the repository.
        assertEquals(2, viewModel.users.size)
        assertEquals("ahmad", viewModel.users[0].username)
        assertEquals("sara", viewModel.users[1].username)
    }

    @Test
    fun selectUser_andClearSelectedUser_updateSelectedUserState() {
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

        val viewModel = UsersViewModel(application, fakeRepository)

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

        // Selecting a user should update the ViewModel's selectedUser state.
        viewModel.selectUser(user)
        assertEquals("ahmad", viewModel.selectedUser?.username)

        // Clearing selection should put the state back to null.
        viewModel.clearSelectedUser()
        assertEquals(null, viewModel.selectedUser)
    }

    @Test
    fun addUser_addsUserAndSetsSuccessMessage() = runTest {
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
                // Simulate persistence by appending the new user to our in-memory list.
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
            override suspend fun changeOwnPassword(currentPassword: String, newPassword: String): Result<Unit> =
                Result.success(Unit)
        }

        val viewModel = UsersViewModel(application, fakeRepository)
        advanceUntilIdle()

        // Act: add a brand new user.
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

        // Assert: the user list should grow and the success message should be set.
        assertEquals(2, viewModel.users.size)
        assertEquals("sara", viewModel.users[1].username)
        assertEquals("User created successfully.", viewModel.createUserMessage)
    }

    @Test
    fun deleteUser_deletesUserClearsSelectionAndSetsSuccessMessage() = runTest {
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
                // Simulate deletion by removing the matching user from memory.
                fakeUsers.removeAll { it.id == userId }
                return Result.success(Unit)
            }

            override suspend fun changeOwnPassword(currentPassword: String, newPassword: String): Result<Unit> =
                Result.success(Unit)
        }

        val viewModel = UsersViewModel(application, fakeRepository)
        advanceUntilIdle()

        // Select the user first so we can verify deletion also clears selection.
        viewModel.selectUser(viewModel.users.first { it.id == "2" })

        viewModel.deleteUser("2")
        advanceUntilIdle()

        // The deleted user should be gone, selection should be cleared,
        // and a success message should be exposed.
        assertEquals(1, viewModel.users.size)
        assertTrue(viewModel.users.none { it.id == "2" })
        assertEquals(null, viewModel.selectedUser)
        assertEquals("User deleted successfully.", viewModel.deleteUserMessage)
    }

    @Test
    fun login_whenSuccessful_setsLoggedInUser() = runTest {
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
            override suspend fun changeOwnPassword(currentPassword: String, newPassword: String): Result<Unit> =
                Result.success(Unit)
        }

        val viewModel = UsersViewModel(application, fakeRepository)
        advanceUntilIdle()

        viewModel.login("ahmad", "123456")
        advanceUntilIdle()

        // Successful login should populate the logged-in user and leave no error.
        assertEquals("ahmad", viewModel.loggedInUser?.username)
        assertEquals(null, viewModel.loginErrorMessage)
        assertEquals(false, viewModel.isLoading)
    }

    @Test
    fun login_whenCredentialsAreWrong_setsErrorMessages() = runTest {
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
            override suspend fun changeOwnPassword(currentPassword: String, newPassword: String): Result<Unit> =
                Result.success(Unit)
        }

        val viewModel = UsersViewModel(application, fakeRepository)
        advanceUntilIdle()

        viewModel.login("wrongUser", "wrongPassword")
        advanceUntilIdle()

        // Failed login should keep loggedInUser null and expose the expected messages.
        assertEquals(null, viewModel.loggedInUser)
        assertEquals("Invalid username or password.", viewModel.loginErrorMessage)
        assertEquals("Please try again.", viewModel.loginInfoMessage)
        assertEquals(false, viewModel.isLoading)
    }

    @Test
    fun logout_clearsUserAndResetsState() = runTest {
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
            override suspend fun changeOwnPassword(currentPassword: String, newPassword: String): Result<Unit> =
                Result.success(Unit)
        }

        val viewModel = UsersViewModel(application, fakeRepository)
        advanceUntilIdle()

        // First log in successfully.
        viewModel.login("ahmad", "123456")
        advanceUntilIdle()

        // Then log out and verify the auth-related UI state has been reset.
        viewModel.logout()

        assertEquals(null, viewModel.loggedInUser)
        assertEquals(null, viewModel.loginErrorMessage)
        assertEquals(null, viewModel.loginInfoMessage)
        assertEquals(false, viewModel.isLoading)
        assertEquals(false, viewModel.isLocked)
    }

    // Small fake Application that always returns the same in-memory SharedPreferences.
    // This avoids Android framework complexity while still supporting preference-based logic.
    private class FakeTestApplication : Application() {
        private val prefs = InMemorySharedPreferences()

        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
            return prefs
        }
    }

    // Very lightweight in-memory SharedPreferences implementation for tests.
    // Useful when the ViewModel or repository expects preferences to exist, but we
    // do not want to rely on device/emulator storage.
    private class InMemorySharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = data.toMutableMap()

        override fun getString(key: String?, defValue: String?): String? {
            return data[key] as? String ?: defValue
        }

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            @Suppress("UNCHECKED_CAST")
            return (data[key] as? MutableSet<String>) ?: defValues
        }

        override fun getInt(key: String?, defValue: Int): Int {
            return data[key] as? Int ?: defValue
        }

        override fun getLong(key: String?, defValue: Long): Long {
            return data[key] as? Long ?: defValue
        }

        override fun getFloat(key: String?, defValue: Float): Float {
            return data[key] as? Float ?: defValue
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return data[key] as? Boolean ?: defValue
        }

        override fun contains(key: String?): Boolean {
            return data.containsKey(key)
        }

        override fun edit(): SharedPreferences.Editor {
            return Editor(data)
        }

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class Editor(
            private val data: MutableMap<String, Any?>
        ) : SharedPreferences.Editor {

            private val pending = mutableMapOf<String, Any?>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                pending[key.orEmpty()] = value
                return this
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                pending[key.orEmpty()] = values
                return this
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                pending[key.orEmpty()] = value
                return this
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                pending[key.orEmpty()] = value
                return this
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                pending[key.orEmpty()] = value
                return this
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                pending[key.orEmpty()] = value
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                pending[key.orEmpty()] = null
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                clearAll = true
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                // clear() wipes existing values before pending changes are applied.
                if (clearAll) {
                    data.clear()
                }

                // Apply all staged edits to the in-memory map.
                pending.forEach { (key, value) ->
                    if (value == null) {
                        data.remove(key)
                    } else {
                        data[key] = value
                    }
                }
            }
        }
    }
}