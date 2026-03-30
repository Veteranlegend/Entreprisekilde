package com.entreprisekilde.app.data.model.auth

import com.entreprisekilde.app.data.model.users.EmployeeUser

sealed class LoginResult {
    data class Success(val user: EmployeeUser) : LoginResult()
    data class Error(val message: String) : LoginResult()
    object TooManyAttempts : LoginResult()
}