package com.entreprisekilde.app.ui.admin.dashboard

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdminDashboardScreen(
    onNavigateToManagement: () -> Unit = {},
    onNavigateToEmployees: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val pageBackground = Color(0xFFE7DADA)
    val headerColor = Color(0xFFDFA676)
    val whitePanel = Color(0xFFF7F7F7)
    val bottomInactive = Color(0xFF9E9AA7)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 26.dp, vertical = 18.dp)
        ) {
            Text(
                text = "Admin",
                color = Color(0xFF6F666A),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(whitePanel)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "9:41 AM",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "▮▮▮ ⌁ ▱",
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerColor)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dashboard",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E2230)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GridView,
                            contentDescription = "Grid",
                            tint = Color(0xFF6A625F)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 28.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    DashboardButton(
                        text = "All Tasks",
                        icon = Icons.Outlined.TaskAlt,
                        onClick = onNavigateToTasks
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    DashboardButton(
                        text = "Create Task",
                        icon = Icons.Outlined.AddBox,
                        onClick = onNavigateToTasks
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    DashboardButton(
                        text = "Calender",
                        icon = Icons.Outlined.CalendarMonth,
                        onClick = onNavigateToManagement
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    DashboardButton(
                        text = "Messages",
                        icon = Icons.Outlined.ChatBubbleOutline,
                        onClick = onNavigateToManagement
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    DashboardButton(
                        text = "Timesheet",
                        icon = Icons.Outlined.Timer,
                        onClick = onNavigateToManagement
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    DashboardButton(
                        text = "Users",
                        icon = Icons.Outlined.Groups,
                        onClick = onNavigateToEmployees
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 26.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem("Home", Icons.Outlined.Home, true, Color.Black, onClick = {})
                    BottomNavItem("Message", Icons.Outlined.ChatBubbleOutline, false, bottomInactive, onClick = {})
                    BottomNavItem("Notification", Icons.Outlined.Inventory2, false, bottomInactive, onClick = {})
                    BottomNavItem("Profile", Icons.Outlined.PersonOutline, false, bottomInactive, onClick = onNavigateToProfile)
                }
            }
        }
    }
}

@Composable
private fun DashboardButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                color = Color(0xFF7FA7D8),
                shape = RoundedCornerShape(11.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(18.dp))

        Text(
            text = text,
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
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
            tint = color,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}