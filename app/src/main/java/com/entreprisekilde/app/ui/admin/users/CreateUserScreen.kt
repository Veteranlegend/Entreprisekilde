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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CreateUserScreen(
    onBack: () -> Unit = {},
    onCreateUser: (String, String, String, String, String, String) -> Unit = { _, _, _, _, _, _ -> },
    onHomeClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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
                onClick = onBack,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Create User",
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
                    imageVector = Icons.Outlined.GridView,
                    contentDescription = null,
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FancyInputField(
                label = "First Name",
                value = firstName,
                onValueChange = { firstName = it }
            )

            FancyInputField(
                label = "Last Name",
                value = lastName,
                onValueChange = { lastName = it }
            )

            FancyInputField(
                label = "Phone Number",
                value = phoneNumber,
                onValueChange = { phoneNumber = it }
            )

            FancyInputField(
                label = "Email",
                value = email,
                onValueChange = { email = it }
            )

            FancyInputField(
                label = "Username",
                value = username,
                onValueChange = { username = it }
            )

            FancyPasswordField(
                label = "Password",
                value = password,
                onValueChange = { password = it },
                passwordVisible = passwordVisible,
                onToggleVisibility = { passwordVisible = !passwordVisible }
            )

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFD32F2F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val cleanFirstName = firstName.trim()
                    val cleanLastName = lastName.trim()
                    val cleanPhoneNumber = phoneNumber.trim()
                    val cleanEmail = email.trim()
                    val cleanUsername = username.trim()
                    val cleanPassword = password.trim()

                    if (
                        cleanFirstName.isBlank() ||
                        cleanLastName.isBlank() ||
                        cleanPhoneNumber.isBlank() ||
                        cleanEmail.isBlank() ||
                        cleanUsername.isBlank() ||
                        cleanPassword.isBlank()
                    ) {
                        errorMessage = "Please fill in all fields."
                    } else {
                        errorMessage = ""

                        onCreateUser(
                            cleanFirstName,
                            cleanLastName,
                            cleanEmail,
                            cleanPhoneNumber,
                            cleanUsername,
                            cleanPassword
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7FA8D6),
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "Create User",
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CreateUserBottomNavItem(
                label = "Home",
                icon = Icons.Outlined.Home,
                color = Color.Black,
                onClick = onHomeClick
            )
            CreateUserBottomNavItem(
                label = "Message",
                icon = Icons.Outlined.ChatBubbleOutline,
                color = Color(0xFF9F98AA),
                onClick = onMessagesClick
            )
            CreateUserBottomNavItem(
                label = "Notification",
                icon = Icons.Outlined.Inventory2,
                color = Color(0xFF9F98AA),
                onClick = onNotificationsClick
            )
            CreateUserBottomNavItem(
                label = "Profile",
                icon = Icons.Outlined.PersonOutline,
                color = Color(0xFF9F98AA),
                onClick = onProfileClick
            )
        }
    }
}

@Composable
private fun FancyInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedIndicatorColor = Color(0xFFD0D5DD),
            unfocusedIndicatorColor = Color(0xFFD0D5DD),
            disabledIndicatorColor = Color(0xFFD0D5DD),
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        ),
        label = {
            Text(
                text = label,
                color = Color(0xFF4F4F4F)
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FancyPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (passwordVisible) {
                        Icons.Default.Visibility
                    } else {
                        Icons.Default.VisibilityOff
                    },
                    contentDescription = "Toggle password visibility",
                    tint = Color(0xFF666666)
                )
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedIndicatorColor = Color(0xFFD0D5DD),
            unfocusedIndicatorColor = Color(0xFFD0D5DD),
            disabledIndicatorColor = Color(0xFFD0D5DD),
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        ),
        label = {
            Text(
                text = label,
                color = Color(0xFF4F4F4F)
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CreateUserBottomNavItem(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit = {}
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