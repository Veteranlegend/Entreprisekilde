package com.entreprisekilde.app.data.repository.users

import com.entreprisekilde.app.data.model.users.EmployeeUser

class DemoUsersRepository : UserRepository {

    private var nextId = 7

    private val users = mutableListOf(
        EmployeeUser(
            id = "1",
            firstName = "Rasmus",
            lastName = "Jensen",
            email = "rasmus.jensen@entreprisekilde.dk",
            phoneNumber = "12341234",
            username = "rasmus",
            password = "1234",
            role = "employee"
        ),
        EmployeeUser(
            id = "2",
            firstName = "Tomas",
            lastName = "Larsen",
            email = "tomas.larsen@entreprisekilde.dk",
            phoneNumber = "22334455",
            username = "tomas",
            password = "1234",
            role = "admin"
        ),
        EmployeeUser(
            id = "3",
            firstName = "Peter",
            lastName = "Hansen",
            email = "peter.hansen@entreprisekilde.dk",
            phoneNumber = "33445566",
            username = "peter",
            password = "1234",
            role = "employee"
        ),
        EmployeeUser(
            id = "4",
            firstName = "John",
            lastName = "Miller",
            email = "john.miller@entreprisekilde.dk",
            phoneNumber = "44556677",
            username = "john",
            password = "1234",
            role = "employee"
        ),
        EmployeeUser(
            id = "5",
            firstName = "Ahmad",
            lastName = "El Haj",
            email = "ahmad.elhaj@entreprisekilde.dk",
            phoneNumber = "55667788",
            username = "ahmad",
            password = "1234",
            role = "employee"
        ),
        EmployeeUser(
            id = "6",
            firstName = "Lars",
            lastName = "Nielsen",
            email = "lars.nielsen@entreprisekilde.dk",
            phoneNumber = "66778899",
            username = "lars",
            password = "1234",
            role = "employee"
        )
    )

    override suspend fun getUsers(): List<EmployeeUser> {
        return users.toList()
    }

    override suspend fun login(username: String, password: String): EmployeeUser? {
        return users.find {
            it.username == username && it.password == password
        }
    }

    override suspend fun addUser(
        firstName: String,
        lastName: String,
        email: String,
        phoneNumber: String,
        username: String,
        password: String
    ): Result<Unit> {
        val newUser = EmployeeUser(
            id = nextId.toString(),
            firstName = firstName,
            lastName = lastName,
            email = email,
            phoneNumber = phoneNumber,
            username = username,
            password = password,
            role = "employee"
        )

        nextId++
        users.add(newUser)

        return Result.success(Unit)
    }

    override suspend fun updateUser(updatedUser: EmployeeUser): Result<Unit> {
        val index = users.indexOfFirst { it.id == updatedUser.id }

        return if (index != -1) {
            users[index] = updatedUser
            Result.success(Unit)
        } else {
            Result.failure(Exception("User not found"))
        }
    }
}