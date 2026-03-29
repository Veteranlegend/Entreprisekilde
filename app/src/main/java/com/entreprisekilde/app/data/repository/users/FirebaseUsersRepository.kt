package com.entreprisekilde.app.data.repository.users

import com.entreprisekilde.app.data.model.users.EmployeeUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseUsersRepository : UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var authStateListener: AuthStateListener? = null

    override suspend fun login(username: String, password: String): EmployeeUser? {
        return try {
            val cleanUsername = username.trim()
            val cleanPassword = password.trim()

            val querySnapshot = firestore.collection("users")
                .whereEqualTo("username", cleanUsername)
                .get()
                .await()

            val userDoc = querySnapshot.documents.firstOrNull() ?: return null
            val email = userDoc.getString("email") ?: return null

            auth.signInWithEmailAndPassword(email, cleanPassword).await()

            val firebaseUid = auth.currentUser?.uid ?: return null

            val user = EmployeeUser(
                id = firebaseUid,
                firstName = userDoc.getString("firstName") ?: "",
                lastName = userDoc.getString("lastName") ?: "",
                email = email,
                phoneNumber = userDoc.getString("phoneNumber") ?: "",
                username = userDoc.getString("username") ?: "",
                password = "",
                role = userDoc.getString("role") ?: "employee"
            )

            firestore.collection("users")
                .document(firebaseUid)
                .set(user)
                .await()

            user
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getUserById(userId: String): EmployeeUser? {
        return try {
            val directDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (directDoc.exists()) {
                return documentToEmployeeUser(directDoc)
            }

            val byIdQuery = firestore.collection("users")
                .whereEqualTo("id", userId)
                .get()
                .await()

            val byIdDoc = byIdQuery.documents.firstOrNull()
            if (byIdDoc != null) {
                val user = documentToEmployeeUser(byIdDoc)

                firestore.collection("users")
                    .document(userId)
                    .set(user.copy(id = userId))
                    .await()

                return user.copy(id = userId)
            }

            val currentEmail = auth.currentUser?.email
            if (!currentEmail.isNullOrBlank()) {
                val byEmailQuery = firestore.collection("users")
                    .whereEqualTo("email", currentEmail)
                    .get()
                    .await()

                val byEmailDoc = byEmailQuery.documents.firstOrNull()
                if (byEmailDoc != null) {
                    val user = documentToEmployeeUser(byEmailDoc).copy(id = userId)

                    firestore.collection("users")
                        .document(userId)
                        .set(user)
                        .await()

                    return user
                }
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun getCurrentAuthUserId(): String? {
        return auth.currentUser?.uid
    }

    override fun observeAuthState(onAuthUserChanged: (String?) -> Unit) {
        stopObservingAuthState()

        authStateListener = AuthStateListener { firebaseAuth ->
            onAuthUserChanged(firebaseAuth.currentUser?.uid)
        }

        authStateListener?.let { listener ->
            auth.addAuthStateListener(listener)
        }
    }

    override fun stopObservingAuthState() {
        authStateListener?.let { listener ->
            auth.removeAuthStateListener(listener)
        }
        authStateListener = null
    }

    override fun logout() {
        auth.signOut()
    }

    override suspend fun getUsers(): List<EmployeeUser> {
        return try {
            val snapshot = firestore.collection("users").get().await()
            snapshot.documents.map { documentToEmployeeUser(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
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
        return try {
            val cleanFirstName = firstName.trim()
            val cleanLastName = lastName.trim()
            val cleanEmail = email.trim()
            val cleanPhoneNumber = phoneNumber.trim()
            val cleanUsername = username.trim()
            val cleanPassword = password.trim()

            val existingUsername = firestore.collection("users")
                .whereEqualTo("username", cleanUsername)
                .get()
                .await()

            if (!existingUsername.isEmpty) {
                return Result.failure(Exception("Username already exists."))
            }

            val result = auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword).await()
            val uid = result.user?.uid
                ?: return Result.failure(Exception("Failed to create auth user."))

            val user = EmployeeUser(
                id = uid,
                firstName = cleanFirstName,
                lastName = cleanLastName,
                email = cleanEmail,
                phoneNumber = cleanPhoneNumber,
                username = cleanUsername,
                password = "",
                role = "employee"
            )

            firestore.collection("users")
                .document(uid)
                .set(user)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception(e.message ?: "Failed to create user."))
        }
    }

    override suspend fun updateUser(updatedUser: EmployeeUser): Result<Unit> {
        return try {
            val userData = mapOf(
                "id" to updatedUser.id,
                "firstName" to updatedUser.firstName.trim(),
                "lastName" to updatedUser.lastName.trim(),
                "email" to updatedUser.email.trim(),
                "phoneNumber" to updatedUser.phoneNumber.trim(),
                "username" to updatedUser.username.trim(),
                "role" to updatedUser.role
            )

            firestore.collection("users")
                .document(updatedUser.id)
                .set(userData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception(e.message ?: "Failed to update user."))
        }
    }

    private fun documentToEmployeeUser(doc: com.google.firebase.firestore.DocumentSnapshot): EmployeeUser {
        return EmployeeUser(
            id = doc.getString("id") ?: doc.id,
            firstName = doc.getString("firstName") ?: "",
            lastName = doc.getString("lastName") ?: "",
            email = doc.getString("email") ?: "",
            phoneNumber = doc.getString("phoneNumber") ?: "",
            username = doc.getString("username") ?: "",
            password = "",
            role = doc.getString("role") ?: "employee"
        )
    }
}