package com.example.activitycounter.ui.activities

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.activitycounter.data.ActivityViewModel
import com.example.activitycounter.model.ActivityItem
import com.example.activitycounter.model.ActivityStatus
import com.example.activitycounter.model.PERIOD_OPTIONS_DAYS
import com.example.activitycounter.model.daysLeftInCycle
import com.example.activitycounter.model.periodLabel
import com.example.activitycounter.model.sortedByUrgency
import com.example.activitycounter.ui.components.EditDeleteMenu
import com.example.activitycounter.ui.components.EmptyState
import com.example.activitycounter.ui.theme.StatusColors
import com.example.activitycounter.ui.theme.statusColors
import kotlinx.coroutines.delay

private const val DEFAULT_PERIOD_DAYS = 7

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(viewModel: ActivityViewModel = viewModel()) {
    val activities by viewModel.activities.collectAsState()

    // Ticks every minute so colors roll over at period boundaries while the app is open
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(60_000)
            value = System.currentTimeMillis()
        }
    }

    val rows = activities?.sortedByUrgency(now)

    var showAddDialog by remember { mutableStateOf(false) }
    var activityToEdit by remember { mutableStateOf<ActivityItem?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Activities") },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        enabled = !rows.isNullOrEmpty()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset all")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New activity") }
            )
        }
    ) { padding ->
        when {
            rows == null -> Unit // still loading
            rows.isEmpty() -> EmptyState(
                icon = Icons.Default.CheckCircle,
                title = "No activities yet",
                message = "Tap “New activity” to add your first one.",
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "summary") {
                    SummaryLine(statuses = rows.map { it.second })
                }
                items(rows, key = { it.first.id }) { (activity, status) ->
                    ActivityCard(
                        activity = activity,
                        status = status,
                        daysLeft = activity.daysLeftInCycle(now),
                        onToggleCompleted = { viewModel.toggleCompleted(activity) },
                        onEdit = { activityToEdit = activity },
                        onDelete = { viewModel.deleteActivity(activity) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ActivityDialog(
            title = "New Activity",
            confirmLabel = "Add",
            initialName = "",
            initialPeriodDays = DEFAULT_PERIOD_DAYS,
            isNameTaken = { viewModel.isNameDuplicate(it) },
            onConfirm = { name, periodDays ->
                viewModel.addActivity(name, periodDays)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    activityToEdit?.let { activity ->
        ActivityDialog(
            title = "Edit Activity",
            confirmLabel = "Save",
            initialName = activity.name,
            initialPeriodDays = activity.periodDays,
            // Keeping its own name (in any letter case) is not a duplicate
            isNameTaken = {
                viewModel.isNameDuplicate(it) && !it.trim().equals(activity.name, ignoreCase = true)
            },
            onConfirm = { name, periodDays ->
                viewModel.editActivity(activity, name, periodDays)
                activityToEdit = null
            },
            onDismiss = { activityToEdit = null }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
            title = { Text("Reset all activities?") },
            text = {
                Text("Every activity goes back to neutral, as if it was just added. Your activities and their periods are kept.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAll()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SummaryLine(statuses: List<ActivityStatus>) {
    val done = statuses.count { it == ActivityStatus.COMPLETED }
    val overdue = statuses.count { it == ActivityStatus.LATE || it == ActivityStatus.MISSED }
    val text = buildString {
        append("$done of ${statuses.size} done")
        if (overdue > 0) append("  ·  $overdue overdue")
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

private fun statusText(status: ActivityStatus, daysLeft: Int): String = when (status) {
    ActivityStatus.COMPLETED -> "Done"
    ActivityStatus.PENDING -> if (daysLeft <= 1) "Due today" else "$daysLeft days left"
    ActivityStatus.LATE -> "Overdue"
    ActivityStatus.MISSED -> "Long overdue"
}

@Composable
fun ActivityCard(
    activity: ActivityItem,
    status: ActivityStatus,
    daysLeft: Int,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = statusColors(status)
    val containerColor by animateColorAsState(colors.container, label = "cardContainer")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = activity.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(periodLabel(activity.periodDays))
                            append("  ·  ")
                        }
                        withStyle(SpanStyle(color = colors.content, fontWeight = FontWeight.Medium)) {
                            append(statusText(status, daysLeft))
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.width(12.dp))
            CheckButton(
                checked = status == ActivityStatus.COMPLETED,
                colors = colors,
                onClick = onToggleCompleted
            )
            EditDeleteMenu(onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@Composable
private fun CheckButton(checked: Boolean, colors: StatusColors, onClick: () -> Unit) {
    OutlinedIconToggleButton(
        checked = checked,
        onCheckedChange = { onClick() },
        modifier = Modifier.size(44.dp),
        colors = IconButtonDefaults.outlinedIconToggleButtonColors(
            checkedContainerColor = colors.accent
        ),
        border = if (checked) null else BorderStroke(2.dp, colors.content.copy(alpha = 0.6f))
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = if (checked) "Undo completion" else "Mark completed",
            tint = if (checked) MaterialTheme.colorScheme.surface else colors.content.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun ActivityDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    initialPeriodDays: Int,
    isNameTaken: (String) -> Boolean,
    onConfirm: (name: String, periodDays: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var periodDays by remember { mutableIntStateOf(initialPeriodDays) }
    val isDuplicate = isNameTaken(name)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Reading") },
                    singleLine = true,
                    isError = isDuplicate,
                    supportingText = if (isDuplicate) {
                        { Text("This name is already taken") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                PeriodPicker(periodDays = periodDays, onPeriodSelected = { periodDays = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, periodDays) },
                enabled = name.isNotBlank() && !isDuplicate
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PeriodPicker(periodDays: Int, onPeriodSelected: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Repeat ${periodLabel(periodDays).lowercase()}",
            style = MaterialTheme.typography.titleSmall
        )
        PeriodChips(
            unitLabel = "Days",
            options = PERIOD_OPTIONS_DAYS.filter { it % 7 != 0 },
            selected = periodDays,
            chipText = { "$it" },
            onPeriodSelected = onPeriodSelected
        )
        PeriodChips(
            unitLabel = "Weeks",
            options = PERIOD_OPTIONS_DAYS.filter { it % 7 == 0 },
            selected = periodDays,
            chipText = { "${it / 7}" },
            onPeriodSelected = onPeriodSelected
        )
    }
}

// Equal-width slots so the Days and Weeks rows line up as a grid
private const val PERIOD_CHIP_SLOTS = 6

@Composable
private fun PeriodChips(
    unitLabel: String,
    options: List<Int>,
    selected: Int,
    chipText: (Int) -> String,
    onPeriodSelected: (Int) -> Unit
) {
    Column {
        Text(
            text = unitLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { days ->
                FilterChip(
                    selected = days == selected,
                    onClick = { onPeriodSelected(days) },
                    label = {
                        Text(
                            text = chipText(days),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            repeat(PERIOD_CHIP_SLOTS - options.size) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
