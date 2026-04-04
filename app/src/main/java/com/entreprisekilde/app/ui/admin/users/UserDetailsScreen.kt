package com.entreprisekilde.app.ui.admin.users

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entreprisekilde.app.data.model.users.User

@Composable
fun UserDetailsScreen(
    user: User,
    isCurrentLoggedInUser: Boolean,
    isDeleting: Boolean = false,
    deleteErrorMessage: String? = null,
    onBack: () -> Unit = {},
    onSaveUser: (User) -> Unit = {},
    onDeleteUser: (String) -> Unit = {},
    onClearDeleteMessage: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    // UI state for edit/save/delete flow.
    //
    // Using remember(user.id) means these values reset whenever a different user
    // is opened on this screen, which prevents old screen state from leaking
    // into the next user's details.
    var isEditing by remember(user.id) { mutableStateOf(false) }
    var isSaving by remember(user.id) { mutableStateOf(false) }
    var showSuccessDialog by remember(user.id) { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember(user.id) { mutableStateOf(false) }

    // Local editable copies of the user fields.
    // We edit these in the UI first, then build a new User object when saving.
    var email by remember(user.id) { mutableStateOf(user.email) }
    var firstName by remember(user.id) { mutableStateOf(user.firstName) }
    var lastName by remember(user.id) { mutableStateOf(user.lastName) }
    var phoneNumber by remember(user.id) { mutableStateOf(user.phoneNumber) }
    var username by remember(user.id) { mutableStateOf(user.username) }

    // Whenever the selected user changes, sync all local state with the new user.
    // This is a good safeguard in case the same screen instance is reused.
    LaunchedEffect(user.id) {
        email = user.email
        firstName = user.firstName
        lastName = user.lastName
        phoneNumber = user.phoneNumber
        username = user.username
        isEditing = false
        isSaving = false
        showDeleteConfirmDialog = false

        // Clear any stale delete message when entering a new user record.
        onClearDeleteMessage()
    }

    // Simple success dialog shown after a save action completes locally.
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
            },
            title = {
                Text(
                    text = "Success",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("User updated successfully.")
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        showSuccessDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    // Confirmation dialog before deleting the user.
    // The wording changes slightly if the admin is looking at their own account.
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                // Prevent the dialog from being dismissed while a delete operation
                // is actively running. This helps avoid weird UI states.
                if (!isDeleting) {
                    showDeleteConfirmDialog = false
                }
            },
            title = {
                Text(
                    text = "Delete user",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isCurrentLoggedInUser) {
                        "Are you sure you want to delete this account?"
                    } else {
                        "Are you sure you want to delete this user?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteUser(user.id)
                    },
                    enabled = !isDeleting
                ) {
                    Text(
                        text = if (isDeleting) "Deleting..." else "Delete",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                    },
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
            // Keeps content below the system status bar.
            .statusBarsPadding()
    ) {
        // Top app bar / header area.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0A673))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    // Block back navigation while save/delete is in progress.
                    if (!isSaving && !isDeleting) onBack()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "User",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Badge,
                    contentDescription = null,
                    tint = Color(0xFF666666)
                )
            }
        }

        // Main scrollable content area for the user fields and actions.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // These fields become editable only when isEditing = true.
            UserField("First name", firstName, { firstName = it }, !isEditing)
            UserField("Last name", lastName, { lastName = it }, !isEditing)
            UserField("Phone number", phoneNumber, { phoneNumber = it }, !isEditing)
            UserField("Email address", email, { email = it }, !isEditing)
            UserField("Username", username, { username = it }, !isEditing)

            // Password is intentionally masked and always read-only here.
            // This screen appears to be for viewing/editing profile details,
            // not for changing credentials directly.
            UserField(
                label = "Password",
                value = "•".repeat(user.password.length.coerceAtLeast(6)),
                onValueChange = {},
                readOnly = true
            )

            // Surface delete errors from a higher layer (likely ViewModel/repository).
            if (!deleteErrorMessage.isNullOrBlank()) {
                Text(
                    text = deleteErrorMessage,
                    color = Color.Red,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (isSaving || isDeleting) return@Button

                    if (isEditing) {
                        // Create an updated copy of the existing user with trimmed input.
                        // Trimming helps avoid accidental spaces from user input.
                        val updatedUser = user.copy(
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            phoneNumber = phoneNumber.trim(),
                            email = email.trim(),
                            username = username.trim()
                        )

                        // Note:
                        // isSaving is toggled on and off immediately here, which means
                        // this currently behaves like a synchronous save from the UI's
                        // point of view. If onSaveUser later becomes asynchronous, this
                        // logic may need to be driven by external state instead.
                        isSaving = true
                        onSaveUser(updatedUser)
                        isEditing = false
                        isSaving = false
                        showSuccessDialog = true
                    } else {
                        // First click switches the screen into edit mode.
                        isEditing = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    // Blue when entering edit mode, green when confirming save.
                    containerColor = if (isEditing) Color(0xFF6FBF73) else Color(0xFF7FA8D6),
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = when {
                        isSaving -> "Saving..."
                        isEditing -> "Save Changes"
                        else -> "Edit User"
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    if (!isDeleting && !isSaving) {
                        showDeleteConfirmDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD9534F),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isDeleting) "Deleting..." else "Delete User",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Local bottom navigation row for this screen.
        // This is custom-built here instead of using a shared bottom nav component.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BottomItem("Home", Icons.Outlined.Home, Color.Black, onHomeClick)
            BottomItem("Message", Icons.Outlined.ChatBubbleOutline, Color.Gray, onMessagesClick)
            BottomItem("Notification", Icons.Outlined.Inventory2, Color.Gray, onNotificationsClick)
            BottomItem("Profile", Icons.Outlined.PersonOutline, Color.Gray, onProfileClick)
        }
    }
}

@Composable
private fun UserField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean
) {
    // Shared field styling for all user info rows.
    // Editable fields get a light blue background so the user can clearly see
    // when the screen is in edit mode.
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = if (readOnly) Color.White else Color(0xFFEFF7FF),
            unfocusedContainerColor = if (readOnly) Color.White else Color(0xFFEFF7FF),
            disabledContainerColor = Color.White,
            focusedIndicatorColor = if (readOnly) Color.Gray else Color(0xFF49A7EE),
            unfocusedIndicatorColor = Color.Gray,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedLabelColor = Color.Black,
            unfocusedLabelColor = Color.DarkGray
        ),
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BottomItem(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    // Small reusable bottom-nav item made of an icon + label.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = color
        )
    }
}