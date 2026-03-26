package com.entreprisekilde.app.ui.admin.management

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Task(
    val title: String,
    val city: String,
    val date: String,
    val person: String,
    var status: String
)

@Composable
fun TasksScreen(onBack: () -> Unit = {}) {

    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var containerHeightPx by remember { mutableStateOf(0) }

    val tasks = remember {
        mutableStateListOf(
            Task("Painting the Wall", "Roskilde", "08/03/2026", "John", "Pending"),
            Task("Installation", "Copenhagen", "26/03/2026", "Peter", "In-progress"),
            Task("Fix Sink Leak", "Valby", "25/02/2026", "John", "In-progress"),
            Task("Bathroom Renovation", "Lyngby", "14/02/2026", "John", "Complete"),
            Task("Roof Repair", "Aarhus", "12/04/2026", "Mike", "Pending"),
            Task("Kitchen Setup", "Odense", "01/05/2026", "Anna", "Complete"),
            Task("Painting Office", "Copenhagen", "10/06/2026", "John", "Pending"),
            Task("Floor Fix", "Roskilde", "15/06/2026", "Peter", "In-progress"),
            Task("Door Install", "Valby", "20/06/2026", "Mike", "Pending"),
            Task("Lighting Setup", "Lyngby", "22/06/2026", "Anna", "Complete")
        )
    }

    val filteredTasks = tasks.filter {
        searchQuery.isBlank() ||
                it.title.contains(searchQuery, true) ||
                it.city.contains(searchQuery, true) ||
                it.person.contains(searchQuery, true) ||
                it.date.contains(searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6DADA))
            .statusBarsPadding()
    ) {

        // HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0A673))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, null, modifier = Modifier.clickable { onBack() })
            Spacer(modifier = Modifier.width(12.dp))
            Text("All Tasks", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.GridView, null)
        }

        // SEARCH
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // LIST + TRUE SCROLLBAR
        Box(
            modifier = Modifier
                .weight(1f)
                .onSizeChanged { containerHeightPx = it.height }
        ) {

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredTasks) { task ->
                    TaskItem(task)
                }
            }

            // 🔥 REAL SCROLL CALCULATION
            val layoutInfo = listState.layoutInfo

            val totalItems = layoutInfo.totalItemsCount
            val firstVisible = listState.firstVisibleItemIndex
            val scrollOffset = listState.firstVisibleItemScrollOffset

            val estimatedItemSize = if (layoutInfo.visibleItemsInfo.isNotEmpty())
                layoutInfo.visibleItemsInfo.first().size else 1

            val totalScrollPx = totalItems * estimatedItemSize
            val currentScrollPx = (firstVisible * estimatedItemSize) + scrollOffset

            val progress = if (totalScrollPx == 0) 0f
            else currentScrollPx.toFloat() / totalScrollPx.toFloat()

            val indicatorHeight = 120f
            val maxOffset = containerHeightPx - indicatorHeight

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .width(3.dp)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color.Gray, RoundedCornerShape(10.dp))
                        .offset {
                            IntOffset(
                                x = 0,
                                y = (progress * maxOffset).toInt()
                            )
                        }
                )
            }
        }

        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF79A7D8))
        ) {
            Text("Create a Task", color = Color.Black)
        }
    }
}

@Composable
fun TaskItem(task: Task) {

    var expanded by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(task.status) }

    val statusColor = when (status) {
        "Pending" -> Color.LightGray
        "In-progress" -> Color(0xFFF2E2A8)
        "Complete" -> Color(0xFFC7EBC4)
        else -> Color.LightGray
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(task.title, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.weight(1f))

            Box {
                Box(
                    modifier = Modifier
                        .background(statusColor, RoundedCornerShape(50))
                        .clickable { expanded = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(status, fontSize = 12.sp)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("Pending", "In-progress", "Complete").forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = {
                                status = it
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Text(task.city)

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE9E9EB), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(task.date)

            Spacer(modifier = Modifier.width(10.dp))

            Divider(
                modifier = Modifier
                    .height(20.dp)
                    .width(1.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Icon(Icons.Outlined.AccountCircle, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(task.person)

            Spacer(modifier = Modifier.weight(1f))

            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Go")
        }
    }
}