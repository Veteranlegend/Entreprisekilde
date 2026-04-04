package com.entreprisekilde.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.entreprisekilde.app.data.model.auth.LoginResult
import com.entreprisekilde.app.data.model.users.User
import com.entreprisekilde.app.data.repository.users.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for user-related state and actions across the app.
 *
 * This includes:
 * - loading and refreshing the users list
 * - login/logout handling
 * - observing auth state changes
 * - creating, updating, and deleting users
 * - changing the currently logged-in user's password
 *
 * The ViewModel acts as the bridge between UI state and the UserRepository.
 * UI screens observe the state exposed here and trigger these functions in response
 * to user actions.
 */
class UsersViewModel(
    application: Application,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

    companion object {
        // SharedPreferences file used to persist lightweight login-related data locally.
        private const val LOGIN_PREFS = "login_prefs"

        // Key used to store the currently logged-in user's id.
        private const val KEY_LOGGED_IN_USER_ID = "logged_in_user_id"
    }

    // Local preferences storage for persisting the logged-in user id between app launches.
    private val prefs = application.getSharedPreferences(LOGIN_PREFS, Context.MODE_PRIVATE)

    /**
     * Compose-aware list of all users.
     *
     * Using a state list means any UI observing this list will update automatically
     * when users are added, removed, or refreshed.
     */
    val users = mutableStateListOf<User>()

    /**
     * The user currently selected in the UI, for example from a user details or edit screen.
     */
    var selectedUser by mutableStateOf<User?>(null)
        private set

    /**
     * The user currently authenticated in the app.
     */
    var loggedInUser by mutableStateOf<User?>(null)
        private set

    /**
     * Used by the UI while we are still determining whether there is an authenticated user.
     */
    var isCheckingAuth by mutableStateOf(true)
        private set

    /**
     * Error message shown when login fails.
     */
    var loginErrorMessage by mutableStateOf<String?>(null)
        private set

    /**
     * Extra informational login message, typically used for softer guidance like "Please try again."
     */
    var loginInfoMessage by mutableStateOf<String?>(null)
        private set

    /**
     * General loading flag for login requests.
     */
    var isLoading by mutableStateOf(false)
        private set

    /**
     * Lock flag used to temporarily prevent repeated login attempts after too many failures.
     */
    var isLocked by mutableStateOf(false)
        private set

    // Tracks consecutive failed login attempts on the client side.
    private var failedAttempts = 0

    // Maximum number of failed attempts allowed before temporary lockout.
    private val maxAttempts = 5

    /**
     * Success and error messages for user creation.
     */
    var createUserMessage by mutableStateOf<String?>(null)
        private set

    var createUserErrorMessage by mutableStateOf<String?>(null)
        private set

    /**
     * Success and error messages for user updates.
     */
    var updateUserMessage by mutableStateOf<String?>(null)
        private set

    var updateUserErrorMessage by mutableStateOf<String?>(null)
        private set

    /**
     * Success and error messages for user deletion.
     */
    var deleteUserMessage by mutableStateOf<String?>(null)
        private set

    var deleteUserErrorMessage by mutableStateOf<String?>(null)
        private set

    /**
     * Prevents duplicate delete requests from being fired while a delete operation is already running.
     */
    var isDeletingUser by mutableStateOf(false)
        private set

    /**
     * Success and error messages for password changes.
     */
    var changePasswordMessage by mutableStateOf<String?>(null)
        private set

    var changePasswordErrorMessage by mutableStateOf<String?>(null)
        private set

    /**
     * Loading flag for the password-change flow.
     */
    var isChangingPassword by mutableStateOf(false)
        private set

    init {
        // Load the initial user list as soon as the ViewModel is created.
        loadUsers()
    }

    /**
     * Starts listening for authentication state changes from the repository.
     *
     * This is useful when auth state can change outside a single screen flow,
     * for example after app startup, logout, token expiration, or remote auth updates.
     */
    fun startAuthObserver() {
        isCheckingAuth = true

        userRepository.observeAuthState { userId ->
            if (userId == null) {
                // No authenticated user found.
                loggedInUser = null
                clearLoggedInUserIdFromPrefs()
                isCheckingAuth = false
            } else {
                viewModelScope.launch {
                    // Fetch the full user object based on the authenticated id.
                    val user = userRepository.getUserById(userId)
                    loggedInUser = user

                    if (user != null) {
                        saveLoggedInUserIdToPrefs(user.id)
                    } else {
                        // If the user id exists in auth but no matching user record is found,
                        // clear the locally persisted id to avoid stale state.
                        clearLoggedInUserIdFromPrefs()
                    }

                    isCheckingAuth = false
                }
            }
        }
    }

    /**
     * Deletes a user by id.
     *
     * Also clears related local state if the deleted user was selected in the UI
     * or was the currently logged-in user.
     */
    fun deleteUser(userId: String, onSuccess: () -> Unit = {}) {
        // Avoid firing multiple delete operations for the same or different users at once.
        if (isDeletingUser) return

        viewModelScope.launch {
            isDeletingUser = true
            deleteUserMessage = null
            deleteUserErrorMessage = null

            val result = userRepository.deleteUser(userId)

            result
                .onSuccess {
                    // Refresh the list so UI immediately reflects the deletion.
                    refreshUsers()

                    // Clear selected user if the deleted user was currently selected.
                    if (selectedUser?.id == userId) {
                        selectedUser = null
                    }

                    // If the deleted account was also the logged-in user, reset auth-related local state.
                    if (loggedInUser?.id == userId) {
                        loggedInUser = null
                        clearLoggedInUserIdFromPrefs()
                    }

                    deleteUserMessage = "User deleted successfully."
                    onSuccess()
                }
                .onFailure { exception ->
                    deleteUserErrorMessage = exception.message ?: "Failed to delete user."
                }

            isDeletingUser = false
        }
    }

    /**
     * Clears delete-user feedback messages from state.
     *
     * Handy when leaving/re-entering a screen or after showing a snackbar/dialog.
     */
    fun clearDeleteUserMessages() {
        deleteUserMessage = null
        deleteUserErrorMessage = null
    }

    /**
     * Attempts to log the user in.
     *
     * This handles:
     * - loading state
     * - login success
     * - error messaging
     * - temporary lockout after repeated failures
     */
    fun login(username: String, password: String) {
        // Stop early if the user is currently locked out from trying again.
        if (isLocked) return

        viewModelScope.launch {
            isLoading = true
            loginErrorMessage = null
            loginInfoMessage = null

            when (val result = userRepository.login(username, password)) {
                is LoginResult.Success -> {
                    loggedInUser = result.user
                    saveLoggedInUserIdToPrefs(result.user.id)

                    // Reset failed-attempt state after a successful login.
                    failedAttempts = 0
                    loginErrorMessage = null
                    loginInfoMessage = null
                }

                is LoginResult.Error -> {
                    failedAttempts++

                    if (failedAttempts >= maxAttempts) {
                        isLocked = true
                        loginErrorMessage = "Too many failed attempts. Try again later."

                        // Temporary cooldown before allowing login again.
                        delay(30_000)

                        failedAttempts = 0
                        isLocked = false
                        loginErrorMessage = null
                        loginInfoMessage = null
                    } else {
                        loginErrorMessage = result.message
                        loginInfoMessage = "Please try again."
                    }
                }

                is LoginResult.TooManyAttempts -> {
                    // Repository-level lockout handling ends up following the same UI flow here.
                    isLocked = true
                    loginErrorMessage = "Too many failed attempts. Try again later."

                    delay(30_000)

                    failedAttempts = 0
                    isLocked = false
                    loginErrorMessage = null
                    loginInfoMessage = null
                }
            }

            isLoading = false
        }
    }

    /**
     * Changes the password for the currently logged-in user.
     *
     * Performs basic validation before calling the repository so the user gets
     * immediate feedback for common form mistakes.
     */
    fun changeOwnPassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        viewModelScope.launch {
            changePasswordMessage = null
            changePasswordErrorMessage = null

            // Trim whitespace to avoid accidental input issues from copy/paste or leading/trailing spaces.
            val cleanCurrentPassword = currentPassword.trim()
            val cleanNewPassword = newPassword.trim()
            val cleanConfirmPassword = confirmPassword.trim()

            when {
                cleanCurrentPassword.isBlank() || cleanNewPassword.isBlank() || cleanConfirmPassword.isBlank() -> {
                    changePasswordErrorMessage = "Please fill in all password fields."
                    return@launch
                }

                cleanNewPassword.length < 6 -> {
                    changePasswordErrorMessage = "New password must be at least 6 characters."
                    return@launch
                }

                cleanNewPassword != cleanConfirmPassword -> {
                    changePasswordErrorMessage = "New passwords do not match."
                    return@launch
                }

                cleanCurrentPassword == cleanNewPassword -> {
                    changePasswordErrorMessage = "New password must be different from current password."
                    return@launch
                }
            }

            isChangingPassword = true

            val result = userRepository.changeOwnPassword(
                currentPassword = cleanCurrentPassword,
                newPassword = cleanNewPassword
            )

            result
                .onSuccess {
                    changePasswordMessage = "Password changed successfully."
                    changePasswordErrorMessage = null
                }
                .onFailure { exception ->
                    changePasswordErrorMessage = exception.message ?: "Failed to change password."
                    changePasswordMessage = null
                }

            isChangingPassword = false
        }
    }

    /**
     * Clears password-change feedback messages.
     */
    fun clearChangePasswordMessages() {
        changePasswordMessage = null
        changePasswordErrorMessage = null
    }

    /**
     * Logs the current user out and clears all related local UI/auth state.
     */
    fun logout() {
        userRepository.logout()
        loggedInUser = null
        clearLoggedInUserIdFromPrefs()

        // Reset all transient login state so the next session starts clean.
        loginErrorMessage = null
        loginInfoMessage = null
        isLoading = false
        isLocked = false
        failedAttempts = 0

        clearChangePasswordMessages()
        clearDeleteUserMessages()
    }

    /**
     * Stores the user the UI is currently focused on.
     */
    fun selectUser(user: User) {
        selectedUser = user
    }

    /**
     * Clears the current user selection.
     */
    fun clearSelectedUser() {
        selectedUser = null
    }

    /**
     * Clears user-creation feedback messages.
     */
    fun clearCreateUserMessages() {
        createUserMessage = null
        createUserErrorMessage = null
    }

    /**
     * Clears user-update feedback messages.
     */
    fun clearUpdateUserMessages() {
        updateUserMessage = null
        updateUserErrorMessage = null
    }

    /**
     * Creates a new user through the repository.
     *
     * On success, the list is refreshed so the new user appears immediately in the UI.
     */
    fun addUser(
        firstName: String,
        lastName: String,
        email: String,
        phoneNumber: String,
        username: String,
        password: String,
        role: String
    ) {
        viewModelScope.launch {
            clearCreateUserMessages()

            val result = userRepository.addUser(
                firstName = firstName,
                lastName = lastName,
                email = email,
                phoneNumber = phoneNumber,
                username = username,
                password = password,
                role = role
            )

            result
                .onSuccess {
                    refreshUsers()
                    createUserMessage = "User created successfully."
                }
                .onFailure { exception ->
                    createUserErrorMessage = exception.message ?: "Failed to create user."
                }
        }
    }

    /**
     * Updates an existing user.
     *
     * If the updated user is the currently selected one or the logged-in user,
     * local state is updated as well so the UI stays consistent.
     */
    fun updateUser(updatedUser: User) {
        viewModelScope.launch {
            clearUpdateUserMessages()

            val result = userRepository.updateUser(updatedUser)

            result
                .onSuccess {
                    refreshUsers()

                    // Keep the detail/edit screen in sync with the saved data.
                    selectedUser = updatedUser

                    // If the logged-in user updated their own profile, reflect that here too.
                    if (loggedInUser?.id == updatedUser.id) {
                        loggedInUser = updatedUser
                        saveLoggedInUserIdToPrefs(updatedUser.id)
                    }

                    updateUserMessage = "User updated successfully."
                }
                .onFailure { exception ->
                    updateUserErrorMessage = exception.message ?: "Failed to update user."
                }
        }
    }

    /**
     * Loads all users from the repository into the Compose state list.
     *
     * Called once during initialization.
     */
    private fun loadUsers() {
        viewModelScope.launch {
            users.clear()
            users.addAll(userRepository.getUsers())
        }
    }

    /**
     * Re-fetches the latest users list.
     *
     * Marked suspend because it is typically called from inside existing coroutines
     * after create/update/delete operations complete.
     */
    private suspend fun refreshUsers() {
        users.clear()
        users.addAll(userRepository.getUsers())
    }

    /**
     * Persists the logged-in user's id locally.
     *
     * This is lightweight local storage and not a replacement for real auth state,
     * but it can help restore app session context between launches.
     */
    private fun saveLoggedInUserIdToPrefs(userId: String) {
        prefs.edit()
            .putString(KEY_LOGGED_IN_USER_ID, userId)
            .apply()
    }

    /**
     * Removes any locally stored logged-in user id.
     */
    private fun clearLoggedInUserIdFromPrefs() {
        prefs.edit()
            .remove(KEY_LOGGED_IN_USER_ID)
            .apply()
    }

    /**
     * Called when the ViewModel is about to be destroyed.
     *
     * We stop observing auth state here to avoid leaking listeners or doing work
     * after the ViewModel is no longer in use.
     */
    override fun onCleared() {
        super.onCleared()
        userRepository.stopObservingAuthState()
    }
}