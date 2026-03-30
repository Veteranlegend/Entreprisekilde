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
import com.entreprisekilde.app.data.model.users.EmployeeUser

@Composable
fun UserDetailsScreen(
    user: EmployeeUser,
    onBack: () -> Unit = {},
    onSaveUser: (EmployeeUser) -> Unit = {},
    onHomeClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    var isEditing by remember(user.id) { mutableStateOf(false) }
    var isSaving by remember(user.id) { mutableStateOf(false) }
    var showSuccessDialog by remember(user.id) { mutableStateOf(false) }

    var email by remember(user.id) { mutableStateOf(user.email) }
    var firstName by remember(user.id) { mutableStateOf(user.firstName) }
    var lastName by remember(user.id) { mutableStateOf(user.lastName) }
    var phoneNumber by remember(user.id) { mutableStateOf(user.phoneNumber) }
    var username by remember(user.id) { mutableStateOf(user.username) }

    LaunchedEffect(user.id) {
        email = user.email
        firstName = user.firstName
        lastName = user.lastName
        phoneNumber = user.phoneNumber
        username = user.username
        isEditing = false
        isSaving = false
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0A673))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (!isSaving) onBack()
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

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            UserField("First name", firstName, { firstName = it }, !isEditing)
            UserField("Last name", lastName, { lastName = it }, !isEditing)
            UserField("Phone number", phoneNumber, { phoneNumber = it }, !isEditing)
            UserField("Email address", email, { email = it }, !isEditing)
            UserField("Username", username, { username = it }, !isEditing)

            UserField(
                label = "Password",
                value = "•".repeat(user.password.length.coerceAtLeast(6)),
                onValueChange = {},
                readOnly = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (isSaving) return@Button

                    if (isEditing) {
                        val updatedUser = user.copy(
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            phoneNumber = phoneNumber.trim(),
                            email = email.trim(),
                            username = username.trim()
                        )

                        isSaving = true
                        onSaveUser(updatedUser)
                        isEditing = false
                        isSaving = false
                        showSuccessDialog = true
                    } else {
                        isEditing = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
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
        }

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