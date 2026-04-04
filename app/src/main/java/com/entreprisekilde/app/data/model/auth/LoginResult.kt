package com.entreprisekilde.app.data.model.auth

import com.entreprisekilde.app.data.model.users.User

/**
 * Represents the result of a login attempt.
 *
 * We use a sealed class here so that all possible login outcomes are defined in one place.
 * This makes it easier to handle login results safely (e.g. with when statements),
 * and ensures we don’t forget any cases.
 */
sealed class LoginResult {

    /**
     * Login was successful.
     *
     * Contains the authenticated user object returned from the repository.
     */
    data class Success(val user: User) : LoginResult()

    /**
     * Login failed due to an error (e.g. wrong credentials, network issue, etc.).
     *
     * The message should be user-friendly so it can be shown directly in the UI.
     */
    data class Error(val message: String) : LoginResult()

    /**
     * Login is temporarily blocked due to too many failed attempts.
     *
     * This is typically used for basic security (e.g. brute force prevention).
     * No additional data is needed here — the UI just reacts to this state.
     */
    object TooManyAttempts : LoginResult()
}