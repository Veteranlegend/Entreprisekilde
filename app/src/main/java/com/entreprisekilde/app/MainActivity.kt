package com.entreprisekilde.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.entreprisekilde.app.ui.admin.dashboard.AdminDashboardScreen
import com.entreprisekilde.app.ui.admin.management.EmployeeScreen
import com.entreprisekilde.app.ui.admin.management.ManagementScreen
import com.entreprisekilde.app.ui.admin.management.TasksScreen
import com.entreprisekilde.app.ui.admin.profile.ProfileScreen
import com.entreprisekilde.app.ui.auth.login.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginClick = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            AdminDashboardScreen(
                onNavigateToManagement = {
                    navController.navigate("management")
                },
                onNavigateToEmployees = {
                    navController.navigate("employees")
                },
                onNavigateToTasks = {
                    navController.navigate("tasks")
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                }
            )
        }

        composable("management") {
            ManagementScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("employees") {
            EmployeeScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("tasks") {
            TasksScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("profile") {
            ProfileScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}