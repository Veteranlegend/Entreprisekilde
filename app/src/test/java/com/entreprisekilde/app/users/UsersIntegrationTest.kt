package com.entreprisekilde.app.users

import com.entreprisekilde.app.data.repository.users.DemoUsersRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UsersIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: DemoUsersRepository
    private lateinit var viewModel: UsersViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = DemoUsersRepository()
        viewModel = UsersViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addUser_shouldAppearInUsersList() = runTest {
        advanceUntilIdle()

        val initialSize = viewModel.users.size

        viewModel.addUser(
            firstName = "Test",
            lastName = "User",
            email = "test.user@entreprisekilde.dk",
            phoneNumber = "99998888",
            username = "testuser",
            password = "1234",
            role = "employee"
        )
        advanceUntilIdle()

        val createdUser = viewModel.users.firstOrNull { it.username == "testuser" }

        assertEquals(initialSize + 1, viewModel.users.size)
        assertNotNull(createdUser)
        assertEquals("Test", createdUser?.firstName)
        assertEquals("User", createdUser?.lastName)
        assertEquals("employee", createdUser?.role)
        assertEquals("User created successfully.", viewModel.createUserMessage)
        assertNull(viewModel.createUserErrorMessage)
        assertTrue(viewModel.users.any { it.username == "testuser" })
    }

    @Test
    fun updateUser_shouldChangeUserInUsersList() = runTest {
        advanceUntilIdle()

        val existingUser = viewModel.users.firstOrNull { it.username == "ahmad" }
        assertNotNull(existingUser)

        val updatedUser = existingUser!!.copy(
            firstName = "AhmadUpdated",
            phoneNumber = "11112222"
        )

        viewModel.updateUser(updatedUser)
        advanceUntilIdle()

        val refreshedUser = viewModel.users.firstOrNull { it.id == updatedUser.id }

        assertNotNull(refreshedUser)
        assertEquals("AhmadUpdated", refreshedUser?.firstName)
        assertEquals("11112222", refreshedUser?.phoneNumber)
        assertEquals("User updated successfully.", viewModel.updateUserMessage)
        assertNull(viewModel.updateUserErrorMessage)
    }

    @Test
    fun deleteUser_shouldRemoveUserFromUsersList() = runTest {
        advanceUntilIdle()

        val existingUser = viewModel.users.firstOrNull { it.username == "lars" }
        assertNotNull(existingUser)

        val initialSize = viewModel.users.size

        viewModel.deleteUser(existingUser!!.id)
        advanceUntilIdle()

        val deletedUser = viewModel.users.firstOrNull { it.id == existingUser.id }

        assertEquals(initialSize - 1, viewModel.users.size)
        assertFalse(viewModel.users.any { it.id == existingUser.id })
        assertNull(deletedUser)
        assertEquals("User deleted successfully.", viewModel.deleteUserMessage)
        assertNull(viewModel.deleteUserErrorMessage)
    }
}