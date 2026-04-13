package com.example.activitycounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.activitycounter.model.ActivityItem
import com.example.activitycounter.ui.theme.ActivityCounterTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.activitycounter.data.ActivityViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ActivityCounterTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Activity Counter") }
                        )
                    }
                ) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = viewModel()
) {
    // Collect database state as Compose state
    val activities by viewModel.activities.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var newActivityName by remember { mutableStateOf("") }

    val maxCount = activities.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add New")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(activities, key = { it.id }) { activity ->
                ActivityRow(
                    activity = activity,
                    progress = activity.count.toFloat() / maxCount.toFloat(),
                    onIncrement = { viewModel.updateCount(activity, activity.count + 1) },
                    onDecrement = {
                        if (activity.count > 0) viewModel.updateCount(activity, activity.count - 1)
                    },
                    onDelete = { viewModel.deleteActivity(activity) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        if (showDialog) {
            val isDuplicate = viewModel.isNameDuplicate(newActivityName)
            val isBlank = newActivityName.isBlank()

            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("New Activity") },
                text = {
                    Column {
                        TextField(
                            value = newActivityName,
                            onValueChange = { newActivityName = it },
                            placeholder = { Text("e.g. Reading") },
                            singleLine = true,
                            isError = isDuplicate // Highlights the field in red
                        )
                        if (isDuplicate) {
                            Text(
                                text = "This activity already exists",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.addActivity(newActivityName.trim())
                            newActivityName = ""
                            showDialog = false
                        },
                        enabled = !isBlank && !isDuplicate // Disable button if invalid
                    ) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        newActivityName = ""
                        showDialog = false
                    }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun ActivityRow(
    activity: ActivityItem,
    progress: Float,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = activity.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
            strokeCap = StrokeCap.Round
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onDecrement,
                modifier = Modifier.defaultMinSize(minWidth = 36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("-", style = MaterialTheme.typography.titleLarge)
            }

            Text(
                text = "${activity.count}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.widthIn(min = 20.dp)
            )

            IconButton(onClick = onIncrement, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}