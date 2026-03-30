package com.entreprisekilde.app.ui.navigation.employee

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun EmployeeAppFlow() {
    val currentScreen = remember { mutableStateOf<EmployeeScreen>(EmployeeScreen.Dashboard) }

    when (currentScreen.value) {
        EmployeeScreen.Dashboard -> {
            Text("Employee Flow (to be implemented)")
        }
    }
}