package com.entreprisekilde.app.ui.auth.login

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entreprisekilde.app.R
import kotlinx.coroutines.delay

private const val LOGIN_PREFS = "login_prefs"
private const val KEY_REMEMBER_ME = "remember_me"
private const val KEY_SAVED_USERNAME = "saved_username"

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit = { _, _ -> },
    errorMessage: String? = null
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(LOGIN_PREFS, Context.MODE_PRIVATE)
    }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    var visibleErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val savedRememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false)
        val savedUsername = prefs.getString(KEY_SAVED_USERNAME, "") ?: ""

        rememberMe = savedRememberMe
        if (savedRememberMe) {
            username = savedUsername
        }
    }

    LaunchedEffect(errorMessage) {
        visibleErrorMessage = errorMessage
        if (!errorMessage.isNullOrBlank()) {
            delay(3000)
            if (visibleErrorMessage == errorMessage) {
                visibleErrorMessage = null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.workshop_bg),
            contentDescription = "Background",
            modifier = Modifier
                .fillMaxSize()
                .blur(6.dp),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.08f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(78.dp))

            Image(
                painter = painterResource(id = R.drawable.logo_entreprisekilden),
                contentDescription = "Logo",
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(180.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(34.dp))

            Text(
                text = "Login",
                color = Color.Black,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Username",
                modifier = Modifier.width(290.dp),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LoginInputField(
                value = username,
                onValueChange = { username = it },
                placeholder = "Username"
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Password",
                modifier = Modifier.width(290.dp),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LoginInputField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                isPassword = true,
                passwordVisible = passwordVisible,
                onTogglePasswordVisibility = { passwordVisible = !passwordVisible }
            )

            Spacer(modifier = Modifier.height(14.dp))

            RememberMeRow(
                checked = rememberMe,
                onCheckedChange = { isChecked ->
                    rememberMe = isChecked
                    if (!isChecked) {
                        prefs.edit()
                            .putBoolean(KEY_REMEMBER_ME, false)
                            .remove(KEY_SAVED_USERNAME)
                            .apply()
                    }
                }
            )

            if (!visibleErrorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = visibleErrorMessage!!,
                    color = Color(0xFFFFD6D6),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(290.dp)
                )
            }

            Spacer(modifier = Modifier.height(34.dp))

            Button(
                onClick = {
                    val cleanUsername = username.trim()
                    val cleanPassword = password.trim()

                    if (rememberMe) {
                        prefs.edit()
                            .putBoolean(KEY_REMEMBER_ME, true)
                            .putString(KEY_SAVED_USERNAME, cleanUsername)
                            .apply()
                    } else {
                        prefs.edit()
                            .putBoolean(KEY_REMEMBER_ME, false)
                            .remove(KEY_SAVED_USERNAME)
                            .apply()
                    }

                    onLoginClick(cleanUsername, cleanPassword)
                },
                modifier = Modifier
                    .width(290.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0454AD),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Login",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RememberMeRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .width(290.dp)
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(20.dp)
                .background(
                    color = if (checked) Color(0xFF0454AD) else Color.White.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(4.dp)
                )
                .border(
                    width = 1.5.dp,
                    color = Color.Black,
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Checked",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "Remember me",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LoginInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .width(290.dp)
            .height(54.dp)
            .background(
                color = Color.White.copy(alpha = 0.88f),
                shape = shape
            )
            .border(
                width = 1.5.dp,
                color = Color.Black,
                shape = shape
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.Black,
                fontSize = 15.sp
            ),
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = if (isPassword) {
                KeyboardOptions(keyboardType = KeyboardType.Password)
            } else {
                KeyboardOptions.Default
            },
            modifier = Modifier.width(250.dp),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.width(258.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = Color(0xFF9E9E9E),
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }

                    if (isPassword && onTogglePasswordVisibility != null) {
                        IconButton(onClick = onTogglePasswordVisibility) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.Visibility
                                } else {
                                    Icons.Default.VisibilityOff
                                },
                                contentDescription = "Toggle password visibility",
                                tint = Color(0xFF9E9E9E)
                            )
                        }
                    }
                }
            }
        )
    }
}