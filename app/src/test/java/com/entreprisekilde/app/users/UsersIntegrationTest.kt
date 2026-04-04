package com.entreprisekilde.app.users

import android.app.Application
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

/**
 * Integration tests for UsersViewModel + DemoUsersRepository.
 *
 * These tests simulate real app behavior by testing:
 * - ViewModel + Repository working together
 * - Coroutine execution
 * - State updates inside the ViewModel
 *
 * Unlike unit tests, this verifies the full flow instead of isolated pieces.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UsersIntegrationTest {

    /**
     * Test dispatcher used to control coroutine execution manually.
     * This allows us to pause and advance async operations deterministically.
     */
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var application: Application
    private lateinit var repository: DemoUsersRepository
    private lateinit var viewModel: UsersViewModel

    /**
     * Runs before each test.
     *
     * - Replaces Main dispatcher with test dispatcher
     * - Initializes repository and ViewModel
     */
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        application = Application()
        repository = DemoUsersRepository()
        viewModel = UsersViewModel(application, repository)
    }

    /**
     * Runs after each test.
     *
     * Important to reset dispatcher to avoid leaking test configuration.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Verifies that adding a user:
     * - Increases list size
     * - Stores correct user data
     * - Updates success message
     * - Does NOT produce an error
     */
    @Test
    fun addUser_shouldAppearInUsersList() = runTest {

        // Ensure all initial coroutines (like loading users) are completed
        advanceUntilIdle()

        val initialSize = viewModel.users.size

        // Perform action
        viewModel.addUser(
            firstName = "Test",
            lastName = "User",
            email = "test.user@entreprisekilde.dk",
            phoneNumber = "99998888",
            username = "testuser",
            password = "1234",
            role = "employee"
        )

        // Wait for coroutine execution to complete
        advanceUntilIdle()

        val createdUser = viewModel.users
            .firstOrNull { it.username == "testuser" }

        // Validate state changes
        assertEquals(initialSize + 1, viewModel.users.size)
        assertNotNull(createdUser)

        // Validate user data
        assertEquals("Test", createdUser?.firstName)
        assertEquals("User", createdUser?.lastName)
        assertEquals("employee", createdUser?.role)

        // Validate ViewModel messages
        assertEquals("User created successfully.", viewModel.createUserMessage)
        assertNull(viewModel.createUserErrorMessage)

        // Extra safety check
        assertTrue(viewModel.users.any { it.username == "testuser" })
    }

    /**
     * Verifies that updating a user:
     * - Modifies existing user (not creating a new one)
     * - Updates specific fields correctly
     * - Sets success message
     */
    @Test
    fun updateUser_shouldChangeUserInUsersList() = runTest {

        advanceUntilIdle()

        // Find an existing demo user
        val existingUser = viewModel.users
            .firstOrNull { it.username == "ahmad" }

        assertNotNull(existingUser)

        // Create updated version
        val updatedUser = existingUser!!.copy(
            firstName = "AhmadUpdated",
            phoneNumber = "11112222"
        )

        // Perform update
        viewModel.updateUser(updatedUser)

        advanceUntilIdle()

        val refreshedUser = viewModel.users
            .firstOrNull { it.id == updatedUser.id }

        // Validate updated values
        assertNotNull(refreshedUser)
        assertEquals("AhmadUpdated", refreshedUser?.firstName)
        assertEquals("11112222", refreshedUser?.phoneNumber)

        // Validate messages
        assertEquals("User updated successfully.", viewModel.updateUserMessage)
        assertNull(viewModel.updateUserErrorMessage)
    }

    /**
     * Verifies that deleting a user:
     * - Removes the user from the list
     * - Decreases total count
     * - Updates success message
     */
    @Test
    fun deleteUser_shouldRemoveUserFromUsersList() = runTest {

        advanceUntilIdle()

        // Find an existing demo user
        val existingUser = viewModel.users
            .firstOrNull { it.username == "lars" }

        assertNotNull(existingUser)

        val initialSize = viewModel.users.size

        // Perform delete
        viewModel.deleteUser(existingUser!!.id)

        advanceUntilIdle()

        val deletedUser = viewModel.users
            .firstOrNull { it.id == existingUser.id }

        // Validate removal
        assertEquals(initialSize - 1, viewModel.users.size)
        assertFalse(viewModel.users.any { it.id == existingUser.id })
        assertNull(deletedUser)

        // Validate messages
        assertEquals("User deleted successfully.", viewModel.deleteUserMessage)
        assertNull(viewModel.deleteUserErrorMessage)
    }
}