package com.entreprisekilde.app.data.repository.users

import com.entreprisekilde.app.data.model.auth.LoginResult
import com.entreprisekilde.app.data.model.users.User
import com.entreprisekilde.app.notifications.FcmTokenManager
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
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Firebase-backed implementation of [UserRepository].
 *
 * This repository handles:
 * - login/logout
 * - auth state observation
 * - reading and updating users in Firestore
 * - creating new auth users
 * - changing the current user's password
 * - syncing/removing FCM tokens during auth transitions
 *
 * Firebase Auth is used for authentication, while Firestore stores the
 * app-specific user profile data.
 */
class FirebaseUsersRepository : UserRepository {

    /**
     * Firebase Authentication instance used for sign-in/sign-out and password work.
     */
    private val auth = FirebaseAuth.getInstance()

    /**
     * Firestore instance used for user profile data.
     */
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Active auth state listener reference.
     *
     * We keep this so we can remove the listener later and avoid leaking it.
     */
    private var authStateListener: AuthStateListener? = null

    /**
     * Logs a user in using either:
     * - email + password, or
     * - username + password
     *
     * Behavior:
     * - If the input contains "@", we treat it as an email login.
     * - Otherwise, we first look up the user by username in Firestore,
     *   then sign in using that user's email.
     *
     * After successful login:
     * - we fetch the latest user data from Firestore
     * - we sync the current FCM token to the logged-in user
     */
    override suspend fun login(username: String, password: String): LoginResult {
        return try {
            val cleanInput = username.trim()
            val cleanPassword = password.trim()

            if (cleanInput.isBlank() || cleanPassword.isBlank()) {
                return LoginResult.Error("Please enter both username and password.")
            }

            // Email login path
            if (cleanInput.contains("@")) {
                val emailInput = cleanInput.lowercase()

                auth.signInWithEmailAndPassword(emailInput, cleanPassword).await()

                val firebaseUid = auth.currentUser?.uid
                    ?: return LoginResult.Error("Login failed. Please try again.")

                // Pull the freshest profile data directly from the server.
                val userDoc = firestore.collection("users")
                    .document(firebaseUid)
                    .get(Source.SERVER)
                    .await()

                val authEmail = auth.currentUser?.email.orEmpty()

                // If the Firestore profile document does not exist yet, we still
                // return a minimal fallback user so login can succeed gracefully.
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

                FcmTokenManager.syncCurrentTokenToLoggedInUser()

                LoginResult.Success(user)
            } else {
                // Username login path:
                // find the matching user profile first, then sign in with email.
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

                // Prefer fresh Firestore data when available.
                val latestUser = if (latestUserDoc.exists()) {
                    documentToUser(latestUserDoc)
                } else {
                    matchedUser
                }

                FcmTokenManager.syncCurrentTokenToLoggedInUser()

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

    /**
     * Deletes a user profile document from Firestore.
     *
     * Important:
     * This currently deletes only the Firestore user document,
     * not the Firebase Auth account itself.
     */
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

    /**
     * Fetches a single user profile from Firestore by ID.
     *
     * We request from [Source.SERVER] so we get the latest server state
     * instead of potentially stale cached data.
     */
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

    /**
     * Returns the currently authenticated Firebase user's UID, if available.
     */
    override fun getCurrentAuthUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Starts observing Firebase auth state changes.
     *
     * This is useful for app startup flows, login/logout handling,
     * or navigation decisions based on whether a user is authenticated.
     *
     * We stop any previous listener first so only one listener stays active.
     */
    override fun observeAuthState(onAuthUserChanged: (String?) -> Unit) {
        stopObservingAuthState()

        authStateListener = AuthStateListener { firebaseAuth ->
            onAuthUserChanged(firebaseAuth.currentUser?.uid)
        }

        authStateListener?.let { listener ->
            auth.addAuthStateListener(listener)
        }
    }

    /**
     * Stops observing auth state changes.
     */
    override fun stopObservingAuthState() {
        authStateListener?.let { listener ->
            auth.removeAuthStateListener(listener)
        }
        authStateListener = null
    }

    /**
     * Logs the current user out.
     *
     * Before signing out, we try to remove the current FCM token from the
     * user's Firestore profile so this device stops receiving notifications
     * intended for that logged-out account.
     *
     * This runs on an IO coroutine because it may do network work.
     */
    override fun logout() {
        val currentUserId = auth.currentUser?.uid

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!currentUserId.isNullOrBlank()) {
                    FcmTokenManager.removeCurrentTokenFromUser(currentUserId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Always sign out even if token cleanup fails.
                auth.signOut()
            }
        }
    }

    /**
     * Fetches all users from Firestore.
     *
     * Again, we use [Source.SERVER] to prefer the latest data from the backend.
     */
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

    /**
     * Creates a new user account.
     *
     * This method performs several steps:
     * 1. Clean and validate the input
     * 2. Check that username and email are unique
     * 3. Create a secondary Firebase app/auth instance
     * 4. Create the Firebase Auth account there
     * 5. Save the app-specific user profile in Firestore
     *
     * Why the secondary app?
     * Creating a user with the main FirebaseAuth instance would switch the
     * currently logged-in session to the new user. Using a secondary app avoids
     * accidentally logging out the admin/current user who is creating the account.
     */
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

            // Extra safety check directly against Firebase Auth sign-in methods.
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

            // Clean up the secondary app if anything fails mid-process.
            secondaryApp?.delete()

            Result.failure(Exception(e.message ?: "Failed to create user."))
        }
    }

    /**
     * Updates an existing user's Firestore profile.
     *
     * Notes:
     * - We check that the username is not already taken by another user.
     * - We only write profile fields here, not the Firebase Auth password.
     * - [SetOptions.merge] avoids wiping fields that are not included in this map.
     */
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
                .set(userData, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception(e.message ?: "Failed to update user."))
        }
    }

    /**
     * Changes the currently logged-in user's password.
     *
     * Firebase requires re-authentication before sensitive operations like
     * password updates, so we:
     * 1. verify the current password using email credentials
     * 2. call updatePassword with the new password
     */
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

    /**
     * Converts a Firestore document into a [User] model.
     *
     * We defensively trim strings and provide safe defaults so malformed or
     * incomplete documents do not break the app.
     *
     * Also worth noting:
     * - Password is intentionally returned as an empty string because we should
     *   never read/store raw passwords from Firestore for app use.
     * - If the stored "id" field is missing, we fall back to the document ID.
     */
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