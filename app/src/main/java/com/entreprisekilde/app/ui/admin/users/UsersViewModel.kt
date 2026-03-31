package com.entreprisekilde.app.ui.admin.users

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entreprisekilde.app.data.model.auth.LoginResult
import com.entreprisekilde.app.data.model.users.User
import com.entreprisekilde.app.data.repository.users.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UsersViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    val users = mutableStateListOf<User>()

    var selectedUser by mutableStateOf<User?>(null)
        private set

    var loggedInUser by mutableStateOf<User?>(null)
        private set

    var isCheckingAuth by mutableStateOf(true)
        private set

    var loginErrorMessage by mutableStateOf<String?>(null)
        private set

    var loginInfoMessage by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isLocked by mutableStateOf(false)
        private set

    private var failedAttempts = 0
    private val maxAttempts = 5

    var createUserMessage by mutableStateOf<String?>(null)
        private set

    var createUserErrorMessage by mutableStateOf<String?>(null)
        private set

    var updateUserMessage by mutableStateOf<String?>(null)
        private set

    var updateUserErrorMessage by mutableStateOf<String?>(null)
        private set

    var deleteUserMessage by mutableStateOf<String?>(null)
        private set

    var deleteUserErrorMessage by mutableStateOf<String?>(null)
        private set

    var isDeletingUser by mutableStateOf(false)
        private set

    var changePasswordMessage by mutableStateOf<String?>(null)
        private set

    var changePasswordErrorMessage by mutableStateOf<String?>(null)
        private set

    var isChangingPassword by mutableStateOf(false)
        private set

    init {
        loadUsers()
    }

    fun startAuthObserver() {
        isCheckingAuth = true

        userRepository.observeAuthState { userId ->
            if (userId == null) {
                loggedInUser = null
                isCheckingAuth = false
            } else {
                viewModelScope.launch {
                    loggedInUser = userRepository.getUserById(userId)
                    isCheckingAuth = false
                }
            }
        }
    }

    fun deleteUser(userId: String, onSuccess: () -> Unit = {}) {
        if (isDeletingUser) return

        if (loggedInUser?.id == userId) {
            deleteUserErrorMessage = "You cannot delete your own account."
            return
        }

        viewModelScope.launch {
            isDeletingUser = true
            deleteUserMessage = null
            deleteUserErrorMessage = null

            val result = userRepository.deleteUser(userId)

            result
                .onSuccess {
                    refreshUsers()

                    if (selectedUser?.id == userId) {
                        selectedUser = null
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

    fun clearDeleteUserMessages() {
        deleteUserMessage = null
        deleteUserErrorMessage = null
    }

    fun login(username: String, password: String) {
        if (isLocked) return

        viewModelScope.launch {
            isLoading = true
            loginErrorMessage = null
            loginInfoMessage = null

            when (val result = userRepository.login(username, password)) {
                is LoginResult.Success -> {
                    loggedInUser = result.user
                    failedAttempts = 0
                    loginErrorMessage = null
                    loginInfoMessage = null
                }

                is LoginResult.Error -> {
                    failedAttempts++

                    if (failedAttempts >= maxAttempts) {
                        isLocked = true
                        loginErrorMessage = "Too many failed attempts. Try again later."

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

    fun changeOwnPassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        viewModelScope.launch {
            changePasswordMessage = null
            changePasswordErrorMessage = null

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

    fun clearChangePasswordMessages() {
        changePasswordMessage = null
        changePasswordErrorMessage = null
    }

    fun logout() {
        userRepository.logout()
        loggedInUser = null
        loginErrorMessage = null
        loginInfoMessage = null
        isLoading = false
        isLocked = false
        failedAttempts = 0
        clearChangePasswordMessages()
        clearDeleteUserMessages()
    }

    fun selectUser(user: User) {
        selectedUser = user
    }

    fun clearSelectedUser() {
        selectedUser = null
    }

    fun clearCreateUserMessages() {
        createUserMessage = null
        createUserErrorMessage = null
    }

    fun clearUpdateUserMessages() {
        updateUserMessage = null
        updateUserErrorMessage = null
    }

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

    fun updateUser(updatedUser: User) {
        viewModelScope.launch {
            clearUpdateUserMessages()

            val result = userRepository.updateUser(updatedUser)

            result
                .onSuccess {
                    refreshUsers()
                    selectedUser = updatedUser

                    if (loggedInUser?.id == updatedUser.id) {
                        loggedInUser = updatedUser
                    }

                    updateUserMessage = "User updated successfully."
                }
                .onFailure { exception ->
                    updateUserErrorMessage = exception.message ?: "Failed to update user."
                }
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            users.clear()
            users.addAll(userRepository.getUsers())
        }
    }

    private suspend fun refreshUsers() {
        users.clear()
        users.addAll(userRepository.getUsers())
    }

    override fun onCleared() {
        super.onCleared()
        userRepository.stopObservingAuthState()
    }
}