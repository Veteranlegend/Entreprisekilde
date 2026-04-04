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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.CircularProgressIndicator
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

/**
 * SharedPreferences file used to persist lightweight login UI state.
 *
 * Right now this is only used for "Remember me" + saved username.
 */
private const val LOGIN_PREFS = "login_prefs"

/**
 * Preference key for whether the user enabled "Remember me".
 */
private const val KEY_REMEMBER_ME = "remember_me"

/**
 * Preference key for the saved username value.
 */
private const val KEY_SAVED_USERNAME = "saved_username"

/**
 * Main login screen for the app.
 *
 * Responsibilities:
 * - show the login form
 * - restore remembered username if enabled
 * - let the user toggle password visibility
 * - show loading / locked states
 * - surface error or info messages
 *
 * This composable is UI-only. Actual login logic is passed in through [onLoginClick].
 */
@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit = { _, _ -> },
    errorMessage: String? = null,
    infoMessage: String? = null,
    isLoading: Boolean = false,
    isLocked: Boolean = false
) {
    val context = LocalContext.current

    // We memoize SharedPreferences so we do not request it repeatedly
    // on every recomposition.
    val prefs = remember {
        context.getSharedPreferences(LOGIN_PREFS, Context.MODE_PRIVATE)
    }

    // Screen-local UI state
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    // This is the message actually shown in the small message box at the bottom.
    // We keep it separate from the incoming error/info params so we can control
    // timing and auto-dismiss behavior locally.
    var visibleMessage by remember { mutableStateOf<String?>(null) }

    // Controls whether the message box should use error styling or info styling.
    var isErrorStyle by remember { mutableStateOf(false) }

    /**
     * On first composition, restore any remembered login state.
     *
     * Current behavior:
     * - If "Remember me" was enabled, restore the saved username
     * - Password is intentionally not stored
     */
    LaunchedEffect(Unit) {
        val savedRememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false)
        val savedUsername = prefs.getString(KEY_SAVED_USERNAME, "") ?: ""

        rememberMe = savedRememberMe
        if (savedRememberMe) {
            username = savedUsername
        }
    }

    /**
     * React whenever the external message state changes.
     *
     * Priority:
     * 1. errorMessage
     * 2. infoMessage
     * 3. no message
     *
     * Also:
     * - Messages auto-hide after 4 seconds
     * - Locked state keeps the message visible instead of dismissing it
     *   (useful for rate-limited / temporary lockout scenarios)
     */
    LaunchedEffect(errorMessage, infoMessage, isLocked) {
        when {
            !errorMessage.isNullOrBlank() -> {
                visibleMessage = errorMessage
                isErrorStyle = true
            }
            !infoMessage.isNullOrBlank() -> {
                visibleMessage = infoMessage
                isErrorStyle = false
            }
            else -> {
                visibleMessage = null
            }
        }

        if (!visibleMessage.isNullOrBlank() && !isLocked) {
            delay(4000)
            visibleMessage = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        /**
         * Background image with a slight blur to push attention toward
         * the login form while still keeping some visual character.
         */
        Image(
            painter = painterResource(id = R.drawable.workshop_bg),
            contentDescription = "Background",
            modifier = Modifier
                .fillMaxSize()
                .blur(6.dp),
            contentScale = ContentScale.Crop
        )

        /**
         * Light dark overlay on top of the image.
         *
         * This improves readability without completely hiding the background.
         */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.08f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()

                // Makes the whole screen scrollable in case of smaller screens
                // or when the keyboard is open.
                .verticalScroll(rememberScrollState())

                // Respect system bars + keyboard insets.
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(78.dp))

            /**
             * App logo near the top.
             */
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

            /**
             * "Remember me" row.
             *
             * If the user turns it off, we immediately remove any saved username
             * from SharedPreferences so the next app launch starts clean.
             */
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

            Spacer(modifier = Modifier.height(34.dp))

            Button(
                onClick = {
                    val cleanUsername = username.trim()
                    val cleanPassword = password.trim()

                    // Save or clear "remember me" state before triggering login.
                    // We only persist the username, never the password.
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

                // Button is disabled while loading or when login is temporarily locked.
                enabled = !isLoading && !isLocked,
                modifier = Modifier
                    .width(290.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0454AD),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF6C91BC),
                    disabledContentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = if (isLocked) "Try again later" else "Login",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Extra bottom space so the content breathes a bit and avoids
            // feeling cramped against the bottom edge / system bars.
            Spacer(modifier = Modifier.height(100.dp))
        }

        /**
         * Floating message box shown near the bottom of the screen.
         *
         * Red-ish style = error
         * Blue-ish style = info
         */
        if (!visibleMessage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 54.dp, vertical = 74.dp)
                    .background(
                        color = if (isErrorStyle) Color(0xCCD32F2F) else Color(0xCC1565C0),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = visibleMessage!!,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Reusable "Remember me" row.
 *
 * The whole row is clickable, not just the checkbox, which makes it easier
 * and nicer to use on mobile.
 */
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
                .size(20.dp)
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
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
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

/**
 * Reusable login input field used for both username and password.
 *
 * Why this exists:
 * - keeps the screen composable cleaner
 * - ensures both fields share the same look and spacing
 * - supports optional password visibility toggle
 */
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

            // Hide password characters unless the user explicitly reveals them.
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },

            // Switch keyboard type for password fields.
            keyboardOptions = if (isPassword) {
                KeyboardOptions(keyboardType = KeyboardType.Password)
            } else {
                KeyboardOptions.Default
            },

            // Slightly narrower than the parent box so text + trailing icon
            // have enough room to coexist comfortably.
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
                        // Manual placeholder because BasicTextField does not
                        // provide one like TextField does.
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