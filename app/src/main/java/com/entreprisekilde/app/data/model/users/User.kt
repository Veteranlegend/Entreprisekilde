package com.entreprisekilde.app.data.model.users

/**
 * Represents a user in the system.
 *
 * This model is used across:
 * - authentication (login)
 * - Firestore storage (users collection)
 * - UI (displaying user information)
 *
 * Note: Firebase Authentication handles the actual password security.
 */
data class User(

    // Unique ID of the user (Firebase UID)
    val id: String = "",

    // User's first name
    val firstName: String = "",

    // User's last name
    val lastName: String = "",

    // User's email (used for authentication with FirebaseAuth)
    val email: String = "",

    // User's phone number
    val phoneNumber: String = "",

    // Username used for login (mapped to email internally)
    val username: String = "",

    /**
     * Password field (NOT used for real authentication).
     *
     * Firebase Authentication securely handles passwords,
     * so this field should NOT be relied on for security.
     *
     * It exits for:
     * - temporary storage
     */
    val password: String = "",

    /**
     * Role of the user (e.g. "admin" or "employee").
     *
     * Used to control:
     * - navigation flow (AdminAppFlow vs EmployeeAppFlow)
     * - permissions in the app
     */
    val role: String = "employee"
) {

    /**
     * Convenience property for displaying full name in UI.
     *
     * Automatically combines first and last name.
     * trim() ensures no extra spaces if one of them is missing.
     */
    val fullName: String
        get() = "$firstName $lastName".trim()
}