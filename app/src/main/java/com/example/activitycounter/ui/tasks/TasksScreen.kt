package com.example.activitycounter.ui.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.activitycounter.data.TasksViewModel
import com.example.activitycounter.model.TaskGroup
import com.example.activitycounter.model.TaskItem
import com.example.activitycounter.model.buildTaskBoard
import com.example.activitycounter.model.inDisplayOrder
import com.example.activitycounter.ui.components.EditDeleteMenu
import com.example.activitycounter.ui.components.EmptyState
import com.example.activitycounter.ui.components.checkedTextColor
import com.example.activitycounter.ui.components.greyCheckboxColors
import com.example.activitycounter.ui.theme.NewFolderIcon
import kotlinx.coroutines.launch

// The task dialog edits [task], or adds a new task, preselecting [groupId]
private data class TaskDialogRequest(val task: TaskItem? = null, val groupId: Int? = null)

// The group dialog renames [group], or creates a new group when it's null
private data class GroupDialogRequest(val group: TaskGroup? = null)

private fun taskCount(n: Int) = if (n == 1) "1 task" else "$n tasks"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: TasksViewModel = viewModel()) {
    val tasks by viewModel.tasks.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var taskDialog by remember { mutableStateOf<TaskDialogRequest?>(null) }
    var groupDialog by remember { mutableStateOf<GroupDialogRequest?>(null) }
    var groupToDelete by remember { mutableStateOf<TaskGroup?>(null) }
    var showCompleted by rememberSaveable { mutableStateOf(true) }

    fun deleteWithUndo(removed: List<TaskItem>, message: String) {
        viewModel.deleteTasks(removed)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo",
                // Long, since deleting has no confirmation step
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreTasks(removed)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Tasks") },
                actions = {
                    IconButton(onClick = { groupDialog = GroupDialogRequest() }) {
                        Icon(NewFolderIcon, contentDescription = "New group")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { taskDialog = TaskDialogRequest() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New task") }
            )
        }
    ) { padding ->
        val loadedTasks = tasks
        val loadedGroups = groups
        when {
            loadedTasks == null || loadedGroups == null -> Unit // still loading
            loadedTasks.isEmpty() && loadedGroups.isEmpty() -> EmptyState(
                icon = Icons.AutoMirrored.Filled.List,
                title = "No tasks yet",
                message = "Tap “New task” to add something you want to remember to do.",
                modifier = Modifier.padding(padding)
            )
            else -> {
                val board = buildTaskBoard(loadedTasks, loadedGroups)
                val groupNames = loadedGroups.associate { it.id to it.name }

                @Composable
                fun Task(task: TaskItem, groupLabel: String? = null, modifier: Modifier) = TaskCard(
                    task = task,
                    groupLabel = groupLabel,
                    onDoneChange = { viewModel.setDone(task, it) },
                    onEdit = { taskDialog = TaskDialogRequest(task = task) },
                    onDelete = { deleteWithUndo(listOf(task), "Task deleted") },
                    modifier = modifier
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "summary") {
                        Text(
                            text = when {
                                board.openCount > 0 -> "${taskCount(board.openCount)} to do"
                                board.completed.isNotEmpty() -> "All tasks done"
                                else -> "No tasks yet"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(board.ungrouped, key = { it.id }) { task ->
                        Task(task, modifier = Modifier.animateItem())
                    }
                    board.sections.forEach { section ->
                        val group = section.group
                        item(key = "group-${group.id}") {
                            GroupHeader(
                                group = group,
                                openCount = section.openTasks.size,
                                onToggle = { viewModel.setGroupCollapsed(group, !group.isCollapsed) },
                                onAddTask = { taskDialog = TaskDialogRequest(groupId = group.id) },
                                onRename = { groupDialog = GroupDialogRequest(group) },
                                onDelete = { groupToDelete = group },
                                modifier = Modifier.animateItem()
                            )
                        }
                        if (!group.isCollapsed) {
                            if (section.openTasks.isEmpty()) {
                                item(key = "group-empty-${group.id}") {
                                    Text(
                                        text = "No open tasks",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .animateItem()
                                            .padding(start = 36.dp, bottom = 4.dp)
                                    )
                                }
                            }
                            items(section.openTasks, key = { it.id }) { task ->
                                Task(task, modifier = Modifier.animateItem())
                            }
                        }
                    }
                    if (board.completed.isNotEmpty()) {
                        item(key = "completed-header") {
                            CompletedHeader(
                                count = board.completed.size,
                                expanded = showCompleted,
                                onToggle = { showCompleted = !showCompleted },
                                onClear = {
                                    deleteWithUndo(board.completed, "${taskCount(board.completed.size)} deleted")
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                        if (showCompleted) {
                            items(board.completed, key = { it.id }) { task ->
                                Task(
                                    task,
                                    groupLabel = task.groupId?.let(groupNames::get),
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    taskDialog?.let { request ->
        val editing = request.task
        TaskDialog(
            title = if (editing == null) "New Task" else "Edit Task",
            confirmLabel = if (editing == null) "Add" else "Save",
            initialText = editing?.title.orEmpty(),
            initialGroupId = editing?.groupId ?: request.groupId,
            groups = groups.orEmpty().inDisplayOrder(),
            isGroupNameTaken = { viewModel.isGroupNameTaken(it) },
            onCreateGroup = { name, onCreated -> viewModel.addGroup(name, onCreated) },
            onConfirm = { text, groupId ->
                if (editing == null) viewModel.addTask(text, groupId)
                else viewModel.editTask(editing, text, groupId)
                taskDialog = null
            },
            onDismiss = { taskDialog = null }
        )
    }

    groupDialog?.let { request ->
        val renaming = request.group
        GroupDialog(
            title = if (renaming == null) "New Group" else "Rename Group",
            confirmLabel = if (renaming == null) "Create" else "Save",
            initialName = renaming?.name.orEmpty(),
            isNameTaken = { viewModel.isGroupNameTaken(it, except = renaming) },
            onConfirm = { name ->
                if (renaming == null) viewModel.addGroup(name) else viewModel.renameGroup(renaming, name)
                groupDialog = null
            },
            onDismiss = { groupDialog = null }
        )
    }

    groupToDelete?.let { group ->
        val tasksInGroup = tasks.orEmpty().count { it.groupId == group.id }
        val close = { groupToDelete = null }
        AlertDialog(
            onDismissRequest = close,
            title = { Text("Delete “${group.name}”?") },
            text = {
                Text(
                    if (tasksInGroup == 0) "This group has no tasks."
                    else "It has ${taskCount(tasksInGroup)}. You can keep them without a group, or delete them too."
                )
            },
            confirmButton = {
                val deleteColors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                if (tasksInGroup == 0) {
                    TextButton(
                        onClick = { viewModel.deleteGroup(group, deleteItsTasks = false); close() },
                        colors = deleteColors
                    ) { Text("Delete") }
                } else {
                    Row {
                        TextButton(onClick = { viewModel.deleteGroup(group, deleteItsTasks = false); close() }) {
                            Text("Keep tasks")
                        }
                        TextButton(
                            onClick = { viewModel.deleteGroup(group, deleteItsTasks = true); close() },
                            colors = deleteColors
                        ) { Text("Delete all") }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = close) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskItem,
    groupLabel: String?,
    onDoneChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        if (task.isDone) MaterialTheme.colorScheme.surfaceContainerLow
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "taskContainer"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = onDoneChange,
                colors = greyCheckboxColors()
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.isDone) checkedTextColor() else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                )
                if (groupLabel != null) {
                    Text(
                        text = groupLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = checkedTextColor()
                    )
                }
            }
            EditDeleteMenu(onEdit = onEdit, onDelete = onDelete)
        }
    }
}

@Composable
private fun GroupHeader(
    group: TaskGroup,
    openCount: Int,
    onToggle: () -> Unit,
    onAddTask: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expanded = !group.isCollapsed
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$openCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        IconButton(onClick = onAddTask) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add task to ${group.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        EditDeleteMenu(onEdit = onRename, onDelete = onDelete, editLabel = "Rename", deleteLabel = "Delete group")
    }
}

@Composable
private fun CompletedHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Completed ($count)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClear) { Text("Clear") }
    }
}

@Composable
private fun TaskDialog(
    title: String,
    confirmLabel: String,
    initialText: String,
    initialGroupId: Int?,
    groups: List<TaskGroup>,
    isGroupNameTaken: (String) -> Boolean,
    onCreateGroup: (name: String, onCreated: (Int) -> Unit) -> Unit,
    onConfirm: (text: String, groupId: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    // A task whose group was deleted is treated as having no group
    var groupId by rememberSaveable { mutableStateOf(initialGroupId?.takeIf { id -> groups.any { it.id == id } }) }
    var showNewGroup by remember { mutableStateOf(false) }

    TextInputDialog(
        title = title,
        confirmLabel = confirmLabel,
        fieldLabel = "Task",
        placeholder = "e.g. Renew passport",
        initialText = initialText,
        onConfirm = { onConfirm(it, groupId) },
        onDismiss = onDismiss
    ) {
        Spacer(Modifier.height(12.dp))
        GroupPicker(
            groups = groups,
            selected = groupId,
            onSelect = { groupId = it },
            onNewGroup = { showNewGroup = true }
        )
    }

    if (showNewGroup) {
        GroupDialog(
            title = "New Group",
            confirmLabel = "Create",
            initialName = "",
            isNameTaken = isGroupNameTaken,
            onConfirm = { name ->
                // Select the new group once it exists
                onCreateGroup(name) { groupId = it }
                showNewGroup = false
            },
            onDismiss = { showNewGroup = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupPicker(
    groups: List<TaskGroup>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    onNewGroup: () -> Unit
) {
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
    )
    Column {
        Text("Group", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("No group") },
                colors = chipColors
            )
            groups.forEach { group ->
                FilterChip(
                    selected = selected == group.id,
                    onClick = { onSelect(group.id) },
                    label = { Text(group.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = chipColors
                )
            }
            AssistChip(
                onClick = onNewGroup,
                label = { Text("New group") },
                leadingIcon = {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                }
            )
        }
    }
}

@Composable
private fun GroupDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    isNameTaken: (String) -> Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    TextInputDialog(
        title = title,
        confirmLabel = confirmLabel,
        fieldLabel = "Group name",
        placeholder = "e.g. Work",
        initialText = initialName,
        errorFor = { if (isNameTaken(it)) "This group already exists" else null },
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

// A dialog with one text field; extra content goes below the field
@Composable
private fun TextInputDialog(
    title: String,
    confirmLabel: String,
    fieldLabel: String,
    placeholder: String,
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    errorFor: (String) -> String? = { null },
    extraContent: @Composable ColumnScope.() -> Unit = {}
) {
    var text by rememberSaveable { mutableStateOf(initialText) }
    val error = errorFor(text)
    val canSave = text.isNotBlank() && error == null
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.replace('\n', ' ') },
                    label = { Text(fieldLabel) },
                    placeholder = { Text(placeholder) },
                    maxLines = 4,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (canSave) onConfirm(text) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                extraContent()
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = canSave) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
