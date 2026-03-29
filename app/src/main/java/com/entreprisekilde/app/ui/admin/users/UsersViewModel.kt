package com.entreprisekilde.app.ui.admin.users

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entreprisekilde.app.data.model.users.EmployeeUser
import com.entreprisekilde.app.data.repository.users.UserRepository
import kotlinx.coroutines.launch

class UsersViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    val users = mutableStateListOf<EmployeeUser>()

    var selectedUser by mutableStateOf<EmployeeUser?>(null)
        private set

    var loggedInUser by mutableStateOf<EmployeeUser?>(null)
        private set

    var loginErrorMessage by mutableStateOf<String?>(null)
        private set

    var createUserMessage by mutableStateOf<String?>(null)
        private set

    var createUserErrorMessage by mutableStateOf<String?>(null)
        private set

    var updateUserMessage by mutableStateOf<String?>(null)
        private set

    var updateUserErrorMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadUsers()
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val matchedUser = userRepository.login(username, password)

            if (matchedUser != null) {
                loggedInUser = matchedUser
                loginErrorMessage = null
            } else {
                loginErrorMessage = "Invalid username or password"
            }
        }
    }

    fun logout() {
        loggedInUser = null
        loginErrorMessage = null
    }

    fun selectUser(user: EmployeeUser) {
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
        password: String
    ) {
        viewModelScope.launch {
            clearCreateUserMessages()

            val result = userRepository.addUser(
                firstName = firstName,
                lastName = lastName,
                email = email,
                phoneNumber = phoneNumber,
                username = username,
                password = password
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

    fun updateUser(updatedUser: EmployeeUser) {
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
}