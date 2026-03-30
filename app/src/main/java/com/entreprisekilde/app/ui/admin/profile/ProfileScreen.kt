package com.entreprisekilde.app.ui.admin.profile
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
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

private data class ProfileFieldItem(
    val key: String,
    val label: String
)

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

    var editableFirstName by remember(firstName) { mutableStateOf(firstName) }
    var editableLastName by remember(lastName) { mutableStateOf(lastName) }
    var editableUsername by remember(username) { mutableStateOf(username) }
    var editableEmail by remember(email) { mutableStateOf(email) }
    var editablePhoneNumber by remember(phoneNumber) { mutableStateOf(phoneNumber) }

    val editingFields = remember { mutableStateListOf<String>() }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var visibleStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf(false) }

    var profileStatusMessage by remember { mutableStateOf<String?>(null) }

    val generalFields = buildList {
        if (isAdmin) add(ProfileFieldItem("firstName", "First name"))
        if (isAdmin) add(ProfileFieldItem("lastName", "Last name"))
        add(ProfileFieldItem("username", "Username"))
        if (isAdmin) add(ProfileFieldItem("email", "Email address"))
        if (isAdmin) add(ProfileFieldItem("phoneNumber", "Phone number"))
    }

    LaunchedEffect(firstName, lastName, username, email, phoneNumber) {
        editableFirstName = firstName
        editableLastName = lastName
        editableUsername = username
        editableEmail = email
        editablePhoneNumber = phoneNumber
        editingFields.clear()
    }

    LaunchedEffect(passwordSuccessMessage, passwordErrorMessage) {
        when {
            !passwordSuccessMessage.isNullOrBlank() -> {
                visibleStatusMessage = passwordSuccessMessage
                isSuccessStatus = true
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

    LaunchedEffect(profileStatusMessage) {
        if (!profileStatusMessage.isNullOrBlank()) {
            delay(2500)
            profileStatusMessage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5EEE8),
                        Color(0xFFF9F7F4),
                        Color(0xFFFFFFFF)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFFDFDFD))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFD9A06B))
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Profile",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "General",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                items(generalFields, key = { it.key }) { field ->
                    when (field.key) {
                        "firstName" -> EditableProfileField(
                            label = field.label,
                            value = editableFirstName,
                            onValueChange = { editableFirstName = it },
                            isEditing = editingFields.contains(field.key),
                            onToggleEdit = {
                                toggleEditField(editingFields, field.key)
                            }
                        )

                        "lastName" -> EditableProfileField(
                            label = field.label,
                            value = editableLastName,
                            onValueChange = { editableLastName = it },
                            isEditing = editingFields.contains(field.key),
                            onToggleEdit = {
                                toggleEditField(editingFields, field.key)
                            }
                        )

                        "username" -> EditableProfileField(
                            label = field.label,
                            value = editableUsername,
                            onValueChange = { editableUsername = it },
                            isEditing = editingFields.contains(field.key),
                            onToggleEdit = {
                                toggleEditField(editingFields, field.key)
                            }
                        )

                        "email" -> EditableProfileField(
                            label = field.label,
                            value = editableEmail,
                            onValueChange = { editableEmail = it },
                            isEditing = editingFields.contains(field.key),
                            onToggleEdit = {
                                toggleEditField(editingFields, field.key)
                            }
                        )

                        "phoneNumber" -> EditableProfileField(
                            label = field.label,
                            value = editablePhoneNumber,
                            onValueChange = { editablePhoneNumber = it },
                            isEditing = editingFields.contains(field.key),
                            onToggleEdit = {
                                toggleEditField(editingFields, field.key)
                            }
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
                            editingFields.clear()
                            profileStatusMessage = "Profile updated successfully."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1D5FB8),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Save Profile",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                item {
                    Text(
                        text = "Security",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

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
                            shape = RoundedCornerShape(14.dp),
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
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFC62828))
                            .clickable { onLogoutClick() }
                            .padding(vertical = 16.dp),
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
}

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
        shape = RoundedCornerShape(14.dp),
        trailingIcon = {
            IconButton(onClick = onToggleEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit $label",
                    tint = if (isEditing) Color(0xFF1D5FB8) else Color(0xFF8F99A7)
                )
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF7F7F8),
            unfocusedContainerColor = Color(0xFFF7F7F8),
            disabledContainerColor = Color(0xFFF7F7F8),
            focusedIndicatorColor = Color(0xFF1D5FB8),
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
        shape = RoundedCornerShape(14.dp),
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