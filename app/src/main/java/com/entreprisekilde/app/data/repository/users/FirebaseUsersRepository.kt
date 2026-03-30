package com.entreprisekilde.app.data.repository.users

import com.entreprisekilde.app.data.model.auth.LoginResult
import com.entreprisekilde.app.data.model.users.User
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseUsersRepository : UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var authStateListener: AuthStateListener? = null

    override suspend fun login(username: String, password: String): LoginResult {
        return try {
            val cleanInput = username.trim()
            val cleanPassword = password.trim()

            if (cleanInput.isBlank() || cleanPassword.isBlank()) {
                return LoginResult.Error("Please enter both username and password.")
            }

            if (cleanInput.contains("@")) {
                val emailInput = cleanInput.lowercase()

                auth.signInWithEmailAndPassword(emailInput, cleanPassword).await()

                val firebaseUid = auth.currentUser?.uid
                    ?: return LoginResult.Error("Login failed. Please try again.")

                val userDoc = firestore.collection("users")
                    .document(firebaseUid)
                    .get(Source.SERVER)
                    .await()

                val authEmail = auth.currentUser?.email.orEmpty()

                val user = if (userDoc.exists()) {
                    documentToUser(userDoc)
                } else {
                    User(
                        id = firebaseUid,
                        firstName = "",
                        lastName = "",
                        email = authEmail,
                        phoneNumber = "",
                        username = "",
                        password = "",
                        role = "employee"
                    )
                }

                LoginResult.Success(user)
            } else {
                val users = getUsers()

                val matchedUser = users.firstOrNull {
                    it.username.trim() == cleanInput
                } ?: return LoginResult.Error("Invalid username or password.")

                if (matchedUser.email.isBlank()) {
                    return LoginResult.Error("This account is missing an email.")
                }

                auth.signInWithEmailAndPassword(
                    matchedUser.email.trim().lowercase(),
                    cleanPassword
                ).await()

                val firebaseUid = auth.currentUser?.uid
                    ?: return LoginResult.Error("Login failed. Please try again.")

                val latestUserDoc = firestore.collection("users")
                    .document(firebaseUid)
                    .get(Source.SERVER)
                    .await()

                val latestUser = if (latestUserDoc.exists()) {
                    documentToUser(latestUserDoc)
                } else {
                    matchedUser
                }

                LoginResult.Success(latestUser)
            }
        } catch (e: FirebaseTooManyRequestsException) {
            e.printStackTrace()
            LoginResult.TooManyAttempts
        } catch (e: FirebaseAuthInvalidUserException) {
            e.printStackTrace()
            LoginResult.Error("Invalid username or password.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            e.printStackTrace()
            LoginResult.Error("Invalid username or password.")
        } catch (e: Exception) {
            e.printStackTrace()
            LoginResult.Error("Login failed. Please try again.")
        }
    }


    override suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to delete user."))
        }
    }
    override suspend fun getUserById(userId: String): User? {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .get(Source.SERVER)
                .await()

            if (doc.exists()) documentToUser(doc) else null
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

    override suspend fun getUsers(): List<User> {
        return try {
            val snapshot = firestore.collection("users")
                .get(Source.SERVER)
                .await()

            snapshot.documents.map { documentToUser(it) }
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
        password: String,
        role: String
    ): Result<Unit> {
        var secondaryApp: FirebaseApp? = null

        return try {
            val cleanFirstName = firstName.trim()
            val cleanLastName = lastName.trim()
            val cleanEmail = email.trim().lowercase()
            val cleanPhoneNumber = phoneNumber.trim()
            val cleanUsername = username.trim()
            val cleanPassword = password.trim()
            val cleanRole = role.trim().lowercase().ifBlank { "employee" }

            if (
                cleanFirstName.isBlank() ||
                cleanLastName.isBlank() ||
                cleanEmail.isBlank() ||
                cleanPhoneNumber.isBlank() ||
                cleanUsername.isBlank() ||
                cleanPassword.isBlank()
            ) {
                return Result.failure(Exception("Please fill in all fields."))
            }

            if (cleanPassword.length < 6) {
                return Result.failure(Exception("Password must be at least 6 characters."))
            }

            val existingUsers = getUsers()

            val usernameAlreadyExists = existingUsers.any { existingUser ->
                existingUser.username.trim() == cleanUsername
            }

            if (usernameAlreadyExists) {
                return Result.failure(Exception("Username already exists."))
            }

            val emailAlreadyExists = existingUsers.any { existingUser ->
                existingUser.email.trim().lowercase() == cleanEmail
            }

            if (emailAlreadyExists) {
                return Result.failure(Exception("Email already exists."))
            }

            val existingSignInMethods = auth.fetchSignInMethodsForEmail(cleanEmail).await()
            if (!existingSignInMethods.signInMethods.isNullOrEmpty()) {
                return Result.failure(Exception("Email already exists."))
            }

            val defaultOptions: FirebaseOptions = FirebaseApp.getInstance().options
            val secondaryAppName = "secondary-${UUID.randomUUID()}"
            secondaryApp = FirebaseApp.initializeApp(
                auth.app.applicationContext,
                defaultOptions,
                secondaryAppName
            ) ?: return Result.failure(Exception("Failed to initialize secondary Firebase app."))

            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

            val authResult = secondaryAuth
                .createUserWithEmailAndPassword(cleanEmail, cleanPassword)
                .await()

            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Failed to create auth user."))

            val user = User(
                id = uid,
                firstName = cleanFirstName,
                lastName = cleanLastName,
                email = cleanEmail,
                phoneNumber = cleanPhoneNumber,
                username = cleanUsername,
                password = "",
                role = cleanRole
            )

            firestore.collection("users")
                .document(uid)
                .set(user)
                .await()

            secondaryAuth.signOut()
            secondaryApp.delete()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            secondaryApp?.delete()
            Result.failure(Exception(e.message ?: "Failed to create user."))
        }
    }

    override suspend fun updateUser(updatedUser: User): Result<Unit> {
        return try {
            val cleanUsername = updatedUser.username.trim()
            val existingUsers = getUsers()

            val usernameTakenByAnotherUser = existingUsers.any { existingUser ->
                existingUser.id != updatedUser.id &&
                        existingUser.username.trim() == cleanUsername
            }

            if (usernameTakenByAnotherUser) {
                return Result.failure(Exception("Username already exists."))
            }

            val userData = mapOf(
                "id" to updatedUser.id,
                "firstName" to updatedUser.firstName.trim(),
                "lastName" to updatedUser.lastName.trim(),
                "email" to updatedUser.email.trim().lowercase(),
                "phoneNumber" to updatedUser.phoneNumber.trim(),
                "username" to cleanUsername,
                "role" to updatedUser.role.trim().lowercase()
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

    override suspend fun changeOwnPassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        return try {
            val cleanCurrentPassword = currentPassword.trim()
            val cleanNewPassword = newPassword.trim()

            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("No logged in user found."))

            val email = currentUser.email?.trim().orEmpty()
            if (email.isBlank()) {
                return Result.failure(Exception("Current account email is missing."))
            }

            if (cleanNewPassword.length < 6) {
                return Result.failure(Exception("New password must be at least 6 characters."))
            }

            val credential = EmailAuthProvider.getCredential(email, cleanCurrentPassword)

            currentUser.reauthenticate(credential).await()
            currentUser.updatePassword(cleanNewPassword).await()

            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            e.printStackTrace()
            Result.failure(Exception("Current password is incorrect."))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception(e.message ?: "Failed to change password."))
        }
    }

    private fun documentToUser(doc: DocumentSnapshot): User {
        return User(
            id = doc.get("id")?.toString()?.trim().orEmpty().ifBlank { doc.id },
            firstName = doc.get("firstName")?.toString()?.trim().orEmpty(),
            lastName = doc.get("lastName")?.toString()?.trim().orEmpty(),
            email = doc.get("email")?.toString()?.trim().orEmpty(),
            phoneNumber = doc.get("phoneNumber")?.toString()?.trim().orEmpty(),
            username = doc.get("username")?.toString()?.trim().orEmpty(),
            password = "",
            role = doc.get("role")?.toString()?.trim().orEmpty().ifBlank { "employee" }
        )
    }
}