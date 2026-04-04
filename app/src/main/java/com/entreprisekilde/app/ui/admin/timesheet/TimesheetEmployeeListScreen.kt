package com.entreprisekilde.app.ui.admin.timesheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entreprisekilde.app.ui.components.AppBottomNavBar
import com.entreprisekilde.app.ui.components.BottomNavDestination

@Composable
fun TimesheetEmployeeListScreen(
    employees: List<String>,
    unreadNotificationCount: Int = 0,
    onBack: () -> Unit = {},
    onEmployeeClick: (String) -> Unit = {},
    onHomeClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    // Keeps track of whatever the admin types into the search field.
    // remember {} makes sure this value survives recomposition while the screen is active.
    var searchText by remember { mutableStateOf("") }

    // Build the list that will actually be shown on screen.
    //
    // We:
    // 1. remove duplicates, so the same employee does not appear multiple times,
    // 2. sort alphabetically for a cleaner UX,
    // 3. filter based on the current search text.
    //
    // If the search field is empty, we simply show the full cleaned list.
    val filteredEmployees = employees
        .distinct()
        .sorted()
        .filter {
            searchText.isBlank() || it.contains(searchText, ignoreCase = true)
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
            // Adds safe-area padding so content does not sit under notches/system bars.
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Top header bar with back navigation, title, and timesheet icon.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0A673))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() },
                tint = Color.Black
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Timesheet",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            // Push the icon container to the far right.
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = "Timesheet",
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Main screen content area.
        // weight(1f) lets this section expand and take the remaining vertical space
        // between the header and bottom navigation bar.
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Employees",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Simple search field for filtering the employee list in real time.
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search"
                    )
                },
                placeholder = { Text("Search employee") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Shows the current number of matching employees after filtering.
            Text(
                text = "${filteredEmployees.size} Results",
                fontSize = 15.sp,
                color = Color(0xFF444444)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // LazyColumn is used here so the list remains efficient even if the
            // employee list grows large in the future.
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredEmployees) { employee ->
                    EmployeeTimesheetCard(
                        name = employee,
                        onClick = { onEmployeeClick(employee) }
                    )
                }
            }
        }

        // Shared bottom navigation used across the app.
        //
        // selectedItem is currently set to HOME, which likely matches the app's
        // navigation structure. If this screen should highlight a different tab
        // later, this is the place to change it.
        AppBottomNavBar(
            selectedItem = BottomNavDestination.HOME,
            unreadNotificationCount = unreadNotificationCount,
            onHomeClick = onHomeClick,
            onMessagesClick = onMessagesClick,
            onNotificationsClick = onNotificationsClick,
            onProfileClick = onProfileClick
        )
    }
}

@Composable
private fun EmployeeTimesheetCard(
    name: String,
    onClick: () -> Unit
) {
    // Reusable row-style card representing one employee entry.
    // Tapping the card opens that employee's timesheet flow/details.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFFE1E1E1), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decorative icon/avatar area for the employee item.
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFFB9DFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.SupervisorAccount,
                contentDescription = null,
                tint = Color(0xFF47A4EA)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // weight(1f) makes the name take up remaining space,
        // which keeps the arrow pinned neatly to the right.
        Text(
            text = name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF8A8A8A)
        )
    }
}