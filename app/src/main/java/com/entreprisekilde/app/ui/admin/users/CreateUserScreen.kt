package com.entreprisekilde.app.ui.admin.users

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CreateUserScreen(
    onBack: () -> Unit = {},
    onCreateUser: (String, String, String, String, String, String, String) -> Unit = { _, _, _, _, _, _, _ -> },
    successMessage: String? = null,
    backendErrorMessage: String? = null,
    onClearMessages: () -> Unit = {},
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
    var localErrorMessage by remember { mutableStateOf("") }

    var selectedRole by remember { mutableStateOf("employee") }

    // 🔹 CLEAR old backend messages when entering screen
    LaunchedEffect(Unit) {
        onClearMessages()
    }

    // 🔹 RESET after success
    LaunchedEffect(successMessage) {
        if (!successMessage.isNullOrBlank()) {
            firstName = ""
            lastName = ""
            phoneNumber = ""
            email = ""
            username = ""
            password = ""
            selectedRole = "employee"
            localErrorMessage = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
            .statusBarsPadding()
    ) {

        // 🔹 HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0A673))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                onClearMessages()
                onBack()
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }

            Text(
                "Create User",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            FancyInputField("First Name", firstName) {
                firstName = it
                onClearMessages()
            }

            FancyInputField("Last Name", lastName) {
                lastName = it
                onClearMessages()
            }

            FancyInputField("Phone Number", phoneNumber) {
                phoneNumber = it
                onClearMessages()
            }

            FancyInputField("Email", email) {
                email = it
                onClearMessages()
            }

            FancyInputField("Username", username) {
                username = it
                onClearMessages()
            }

            FancyPasswordField(
                "Password",
                password,
                {
                    password = it
                    onClearMessages()
                },
                passwordVisible
            ) {
                passwordVisible = !passwordVisible
            }

            // 🔹 ROLE
            Text("Role", fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                OutlinedButton(
                    onClick = { selectedRole = "admin" },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selectedRole == "admin") Color(0xFF7FA8D6) else Color.Transparent
                    )
                ) {
                    Text("Admin")
                }

                OutlinedButton(
                    onClick = { selectedRole = "employee" },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selectedRole == "employee") Color(0xFF7FA8D6) else Color.Transparent
                    )
                ) {
                    Text("Employee")
                }
            }

            // 🔴 ERRORS
            if (localErrorMessage.isNotBlank()) {
                Text(localErrorMessage, color = Color.Red)
            }

            if (!backendErrorMessage.isNullOrBlank()) {
                Text(backendErrorMessage, color = Color.Red)
            }

            if (!successMessage.isNullOrBlank()) {
                Text(successMessage, color = Color(0xFF2E7D32))
            }

            // 🔵 BUTTON
            Button(
                onClick = {

                    if (
                        firstName.isBlank() ||
                        lastName.isBlank() ||
                        phoneNumber.isBlank() ||
                        email.isBlank() ||
                        username.isBlank() ||
                        password.isBlank()
                    ) {
                        localErrorMessage = "Fill all fields"
                        return@Button
                    }

                    localErrorMessage = ""

                    onCreateUser(
                        firstName.trim(),
                        lastName.trim(),
                        email.trim(),
                        phoneNumber.trim(),
                        username.trim(),
                        password.trim(),
                        selectedRole
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Create User")
            }
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
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun FancyPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    toggle: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = toggle) {
                Icon(
                    if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}