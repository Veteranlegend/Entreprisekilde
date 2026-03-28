package com.entreprisekilde.app.ui.admin.users

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.entreprisekilde.app.data.repository.UserRepository

class UsersViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    val users: List<EmployeeUser>
        get() = userRepository.getUsers()

    var selectedUser by mutableStateOf<EmployeeUser?>(null)
        private set

    var loggedInUser by mutableStateOf<EmployeeUser?>(null)
        private set

    var loginErrorMessage by mutableStateOf<String?>(null)
        private set

    fun login(username: String, password: String): Boolean {
        val matchedUser = userRepository.login(username, password)

        return if (matchedUser != null) {
            loggedInUser = matchedUser
            loginErrorMessage = null
            true
        } else {
            loginErrorMessage = "Invalid username or password"
            false
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

    fun addUser(
        firstName: String,
        lastName: String,
        email: String,
        phoneNumber: String,
        username: String,
        password: String
    ) {
        userRepository.addUser(
            firstName,
            lastName,
            email,
            phoneNumber,
            username,
            password
        )
    }

    fun updateUser(updatedUser: EmployeeUser) {
        userRepository.updateUser(updatedUser)
        selectedUser = updatedUser

        if (loggedInUser?.id == updatedUser.id) {
            loggedInUser = updatedUser
        }
    }
}