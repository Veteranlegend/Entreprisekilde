package com.entreprisekilde.app.ui.admin.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entreprisekilde.app.ui.components.AppBottomNavBar
import com.entreprisekilde.app.ui.components.BottomNavDestination
import kotlinx.coroutines.delay

/**
 * Small internal model used to describe which profile fields should be shown.
 *
 * Instead of hardcoding every visible field directly in the LazyColumn,
 * we build a small list of field descriptors and render them in one place.
 * That keeps the UI easier to maintain and makes role-based visibility simpler.
 */
private data class ProfileFieldItem(
    val key: String,
    val label: String
)

/**
 * Profile screen for viewing/updating user information and changing password.
 *
 * Responsibilities:
 * - show editable general profile information
 * - allow toggling individual fields into edit mode
 * - allow password changes
 * - show temporary success/error messages
 * - provide logout action
 * - keep bottom navigation visible
 *
 * There is also a role-based behavior here:
 * - admins can edit more general fields
 * - non-admin users get a more limited set
 */
@Composable
fun ProfileScreen(
    email: String,
    firstName: String,
    lastName: String,
    username: String,
    phoneNumber: String,
    role: String,
    passwordSuccessMessage: String? = null,
    passwordErrorMessage: String? = null,
    isChangingPassword: Boolean = false,
    onChangePassword: (String, String, String) -> Unit = { _, _, _ -> },
    onSaveProfile: (String, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
    onClearPasswordMessages: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val isAdmin = role.equals("admin", ignoreCase = true)

    /**
     * Editable field state.
     *
     * These local states are initialized from the incoming values and updated
     * again whenever the source values change from above.
     */
    var editableFirstName by remember(firstName) { mutableStateOf(firstName) }
    var editableLastName by remember(lastName) { mutableStateOf(lastName) }
    var editableUsername by remember(username) { mutableStateOf(username) }
    var editableEmail by remember(email) { mutableStateOf(email) }
    var editablePhoneNumber by remember(phoneNumber) { mutableStateOf(phoneNumber) }

    /**
     * Tracks which general fields are currently in edit mode.
     *
     * We use a list of keys instead of separate booleans for each field,
     * which scales much better as fields are added or removed.
     */
    val editingFields = remember { mutableStateListOf<String>() }

    /**
     * Password form state.
     */
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    /**
     * Password visibility toggles for the three password inputs.
     */
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    /**
     * Temporary security/password status message state.
     *
     * This is shown inside the security card and automatically cleared later.
     */
    var visibleStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf(false) }

    /**
     * Temporary general profile save status message.
     */
    var profileStatusMessage by remember { mutableStateOf<String?>(null) }

    /**
     * Build the list of general profile fields dynamically.
     *
     * Admins can edit more fields, while non-admins have a smaller set.
     * This keeps the UI role-aware without duplicating large chunks of code.
     */
    val generalFields = buildList {
        if (isAdmin) add(ProfileFieldItem("firstName", "First name"))
        if (isAdmin) add(ProfileFieldItem("lastName", "Last name"))
        add(ProfileFieldItem("username", "Username"))
        if (isAdmin) add(ProfileFieldItem("email", "Email address"))
        if (isAdmin) add(ProfileFieldItem("phoneNumber", "Phone number"))
    }

    /**
     * If the profile data coming from above changes, refresh the local editable
     * state so the screen stays in sync with the true source of truth.
     *
     * We also clear edit mode because the incoming data may now represent a
     * freshly saved or newly loaded profile state.
     */
    LaunchedEffect(firstName, lastName, username, email, phoneNumber) {
        editableFirstName = firstName
        editableLastName = lastName
        editableUsername = username
        editableEmail = email
        editablePhoneNumber = phoneNumber
        editingFields.clear()
    }

    /**
     * React to password success/error messages from above.
     *
     * Success behavior:
     * - show success message
     * - clear all password inputs
     * - reset password visibility toggles
     * - hide message after a short delay
     *
     * Error behavior:
     * - show error message
     * - leave the inputs as-is so the user can correct them
     * - hide message after a short delay
     */
    LaunchedEffect(passwordSuccessMessage, passwordErrorMessage) {
        when {
            !passwordSuccessMessage.isNullOrBlank() -> {
                visibleStatusMessage = passwordSuccessMessage
                isSuccessStatus = true

                // Clear sensitive input after a successful password change.
                currentPassword = ""
                newPassword = ""
                confirmPassword = ""

                currentPasswordVisible = false
                newPasswordVisible = false
                confirmPasswordVisible = false

                delay(3500)
                onClearPasswordMessages()
                visibleStatusMessage = null
            }

            !passwordErrorMessage.isNullOrBlank() -> {
                visibleStatusMessage = passwordErrorMessage
                isSuccessStatus = false

                delay(3500)
                onClearPasswordMessages()
                visibleStatusMessage = null
            }

            else -> {
                visibleStatusMessage = null
            }
        }
    }

    /**
     * Auto-hide the profile save success message after a short delay.
     */
    LaunchedEffect(profileStatusMessage) {
        if (!profileStatusMessage.isNullOrBlank()) {
            delay(2500)
            profileStatusMessage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F3F3))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        /**
         * Custom header area.
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD9A06B))
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profile",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Profile",
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        /**
         * Main scrollable content area.
         *
         * Using LazyColumn keeps the layout flexible and scalable as more
         * settings/sections are added later.
         */
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 22.dp,
                bottom = 24.dp
            )
        ) {
            item {
                Text(
                    text = "General",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            /**
             * Render all general profile fields from the dynamic field list.
             */
            items(generalFields, key = { it.key }) { field ->
                when (field.key) {
                    "firstName" -> EditableProfileField(
                        label = field.label,
                        value = editableFirstName,
                        onValueChange = { editableFirstName = it },
                        isEditing = editingFields.contains(field.key),
                        onToggleEdit = { toggleEditField(editingFields, field.key) }
                    )

                    "lastName" -> EditableProfileField(
                        label = field.label,
                        value = editableLastName,
                        onValueChange = { editableLastName = it },
                        isEditing = editingFields.contains(field.key),
                        onToggleEdit = { toggleEditField(editingFields, field.key) }
                    )

                    "username" -> EditableProfileField(
                        label = field.label,
                        value = editableUsername,
                        onValueChange = { editableUsername = it },
                        isEditing = editingFields.contains(field.key),
                        onToggleEdit = { toggleEditField(editingFields, field.key) }
                    )

                    "email" -> EditableProfileField(
                        label = field.label,
                        value = editableEmail,
                        onValueChange = { editableEmail = it },
                        isEditing = editingFields.contains(field.key),
                        onToggleEdit = { toggleEditField(editingFields, field.key) }
                    )

                    "phoneNumber" -> EditableProfileField(
                        label = field.label,
                        value = editablePhoneNumber,
                        onValueChange = { editablePhoneNumber = it },
                        isEditing = editingFields.contains(field.key),
                        onToggleEdit = { toggleEditField(editingFields, field.key) }
                    )
                }
            }

            item {
                if (!profileStatusMessage.isNullOrBlank()) {
                    PasswordStatusBox(
                        message = profileStatusMessage!!,
                        isSuccess = true
                    )
                }
            }

            /**
             * Save profile button.
             *
             * We trim values before sending them upward so the parent layer gets
             * cleaner, normalized input.
             */
            item {
                Button(
                    onClick = {
                        onSaveProfile(
                            editableFirstName.trim(),
                            editableLastName.trim(),
                            editableUsername.trim(),
                            editableEmail.trim(),
                            editablePhoneNumber.trim()
                        )

                        // Once save is triggered, leave edit mode.
                        editingFields.clear()
                        profileStatusMessage = "Profile updated successfully."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1D5FB8),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Save Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            item {
                Text(
                    text = "Security",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            /**
             * Password change section.
             */
            item {
                SecurityCard {
                    PasswordInputField(
                        label = "Current password",
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        visible = currentPasswordVisible,
                        onToggleVisibility = { currentPasswordVisible = !currentPasswordVisible }
                    )

                    Spacer(modifier = Modifier.size(12.dp))

                    PasswordInputField(
                        label = "New password",
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        visible = newPasswordVisible,
                        onToggleVisibility = { newPasswordVisible = !newPasswordVisible }
                    )

                    Spacer(modifier = Modifier.size(12.dp))

                    PasswordInputField(
                        label = "Confirm new password",
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        visible = confirmPasswordVisible,
                        onToggleVisibility = { confirmPasswordVisible = !confirmPasswordVisible }
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    if (!visibleStatusMessage.isNullOrBlank()) {
                        PasswordStatusBox(
                            message = visibleStatusMessage!!,
                            isSuccess = isSuccessStatus
                        )

                        Spacer(modifier = Modifier.size(14.dp))
                    }

                    Button(
                        onClick = {
                            onChangePassword(
                                currentPassword,
                                newPassword,
                                confirmPassword
                            )
                        },
                        enabled = !isChangingPassword,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1D5FB8),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF8DAEDB),
                            disabledContentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (isChangingPassword) {
                                "Changing password..."
                            } else {
                                "Change Password"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }

            /**
             * Logout action at the bottom.
             *
             * Styled as a destructive/red action to visually communicate that it
             * ends the current session.
             */
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFC62828))
                        .clickable { onLogoutClick() }
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Log Out",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        AppBottomNavBar(
            selectedItem = BottomNavDestination.PROFILE,
            onHomeClick = onHomeClick,
            onMessagesClick = onMessagesClick,
            onNotificationsClick = onNotificationsClick,
            onProfileClick = onProfileClick
        )
    }
}

/**
 * Toggles whether a given general profile field is currently editable.
 *
 * If the field key is already in the list, remove it.
 * Otherwise, add it.
 */
private fun toggleEditField(
    editingFields: MutableList<String>,
    key: String
) {
    if (editingFields.contains(key)) {
        editingFields.remove(key)
    } else {
        editingFields.add(key)
    }
}

/**
 * Reusable editable text field for profile information.
 *
 * This field is read-only by default and becomes editable only when the user
 * taps the trailing edit icon.
 */
@Composable
private fun EditableProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isEditing: Boolean,
    onToggleEdit: () -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = !isEditing,
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        trailingIcon = {
            IconButton(onClick = onToggleEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit $label",

                    // Slightly stronger color when the field is actively editable.
                    tint = if (isEditing) Color(0xFF1D5FB8) else Color(0xFF8F99A7)
                )
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF0F0F0),
            unfocusedContainerColor = Color(0xFFF0F0F0),
            disabledContainerColor = Color(0xFFF0F0F0),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            disabledTextColor = Color.Black,
            focusedLabelColor = Color(0xFF8F99A7),
            unfocusedLabelColor = Color(0xFF8F99A7)
        ),
        label = {
            Text(
                text = label,
                color = Color(0xFF8F99A7)
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Small reusable card container for the security/password section.
 */
@Composable
private fun SecurityCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF7F9FC))
            .padding(16.dp),
        content = content
    )
}

/**
 * Password input field with a visibility toggle.
 *
 * Used for:
 * - current password
 * - new password
 * - confirm new password
 */
@Composable
private fun PasswordInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisibility: () -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle password visibility",
                    tint = Color(0xFF7F8A99)
                )
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            disabledTextColor = Color.Black,
            focusedLabelColor = Color(0xFF8F99A7),
            unfocusedLabelColor = Color(0xFF8F99A7)
        ),
        label = {
            Text(
                text = label,
                color = Color(0xFF8F99A7)
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Reusable status message box.
 *
 * Even though the name mentions "Password", it is also reused for the general
 * profile success message because the visual style works well for both cases.
 */
@Composable
private fun PasswordStatusBox(
    message: String,
    isSuccess: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSuccess) Color(0xFFE7F6EC) else Color(0xFFFDEBEC)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = message,
            color = if (isSuccess) Color(0xFF237A43) else Color(0xFFB3261E),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}