package com.entreprisekilde.app.data.model.auth

import com.entreprisekilde.app.data.model.users.User

sealed class LoginResult {
    data class Success(val user: User) : LoginResult()
    data class Error(val message: String) : LoginResult()
    object TooManyAttempts : LoginResult()
}