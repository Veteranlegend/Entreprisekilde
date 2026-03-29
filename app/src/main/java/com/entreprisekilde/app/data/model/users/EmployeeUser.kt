package com.entreprisekilde.app.data.model.users

data class EmployeeUser(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val username: String = "",
    val password: String = "",
    val role: String = "employee"
) {
    val fullName: String
        get() = "$firstName $lastName"
}