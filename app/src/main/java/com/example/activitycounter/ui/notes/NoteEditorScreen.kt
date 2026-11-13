package com.example.activitycounter.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.activitycounter.model.ChecklistItem
import com.example.activitycounter.model.NoteDraft
import com.example.activitycounter.ui.components.checkedTextColor
import com.example.activitycounter.ui.components.greyCheckboxColors
import com.example.activitycounter.ui.theme.NOTE_COLORS
import com.example.activitycounter.ui.theme.PushPinIcon
import com.example.activitycounter.ui.theme.noteColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    draft: NoteDraft,
    onChange: ((NoteDraft) -> NoteDraft) -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onAppBackgrounded: () -> Unit
) {
    BackHandler(onBack = onClose)
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { onAppBackgrounded() }

    var showDeleteDialog by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val textFocus = remember { FocusRequester() }
    // Checklist item that should take focus once it's on screen
    var focusItemId by remember { mutableStateOf<Long?>(null) }

    // Start typing straight away in a new note
    LaunchedEffect(Unit) {
        if (draft.isBlank) titleFocus.requestFocus()
    }

    val background by animateColorAsState(
        noteColor(draft.colorIndex, default = MaterialTheme.colorScheme.surface),
        label = "noteBackground"
    )

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconToggleButton(
                        checked = draft.isPinned,
                        onCheckedChange = { pinned -> onChange { it.copy(isPinned = pinned) } }
                    ) {
                        Icon(
                            imageVector = PushPinIcon,
                            contentDescription = if (draft.isPinned) "Unpin" else "Pin",
                            tint = if (draft.isPinned) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete note")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = background,
                    scrolledContainerColor = background
                )
            )
        },
        bottomBar = {
            ColorPicker(
                selected = draft.colorIndex,
                onSelect = { index -> onChange { it.copy(colorIndex = index) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            PlainTextField(
                value = draft.title,
                onValueChange = { title -> onChange { it.copy(title = title.replace('\n', ' ')) } },
                placeholder = "Title",
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = {
                    if (!draft.isChecklist) {
                        textFocus.requestFocus()
                    } else if (draft.items.isNotEmpty()) {
                        focusItemId = draft.items.first().id
                    }
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus)
            )

            NoteTypeSelector(
                isChecklist = draft.isChecklist,
                onSelect = { checklist -> onChange { it.withChecklist(checklist) } }
            )

            if (draft.isChecklist) {
                ChecklistEditor(
                    items = draft.items,
                    onUpdate = { transform -> onChange { it.copy(items = transform(it.items)) } },
                    focusItemId = focusItemId,
                    onFocusItem = { focusItemId = it }
                )
            } else {
                PlainTextField(
                    value = draft.text,
                    onValueChange = { text -> onChange { it.copy(text = text) } },
                    placeholder = "Note",
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp)
                        .focusRequester(textFocus)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this note?") },
            text = { Text("This can’t be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteTypeSelector(isChecklist: Boolean, onSelect: (Boolean) -> Unit) {
    val options = listOf(false to "Text", true to "Checklist")
    SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(vertical = 12.dp)) {
        options.forEachIndexed { index, (checklist, label) ->
            SegmentedButton(
                selected = isChecklist == checklist,
                onClick = { onSelect(checklist) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(inactiveContainerColor = Color.Transparent)
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun ChecklistEditor(
    items: List<ChecklistItem>,
    // Changes are applied to the latest list, so quick successive edits aren't lost
    onUpdate: ((List<ChecklistItem>) -> List<ChecklistItem>) -> Unit,
    focusItemId: Long?,
    onFocusItem: (Long?) -> Unit
) {
    fun addItem(afterId: Long?) {
        val newItem = ChecklistItem("")
        onUpdate { list ->
            val index = list.indexOfFirst { it.id == afterId }
            list.toMutableList().apply { if (index >= 0) add(index + 1, newItem) else add(newItem) }
        }
        onFocusItem(newItem.id)
    }

    Column {
        items.forEach { item ->
            key(item.id) {
                ChecklistRow(
                    item = item,
                    requestFocus = item.id == focusItemId,
                    onFocused = { onFocusItem(null) },
                    onTextChange = { text ->
                        onUpdate { list -> list.map { if (it.id == item.id) it.copy(text = text) else it } }
                    },
                    onCheckedChange = { checked ->
                        onUpdate { list -> list.map { if (it.id == item.id) it.copy(checked = checked) else it } }
                    },
                    onNext = { addItem(afterId = item.id) },
                    onRemove = { onUpdate { list -> list.filterNot { it.id == item.id } } }
                )
            }
        }
        TextButton(onClick = { addItem(afterId = null) }) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add item")
        }
    }
}

@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    requestFocus: Boolean,
    onFocused: () -> Unit,
    onTextChange: (String) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onNext: () -> Unit,
    onRemove: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            onFocused()
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = item.checked, onCheckedChange = onCheckedChange, colors = greyCheckboxColors())
        PlainTextField(
            value = item.text,
            onValueChange = { onTextChange(it.replace('\n', ' ')) },
            placeholder = "List item",
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (item.checked) TextDecoration.LineThrough else null
            ),
            textColor = if (item.checked) checkedTextColor() else MaterialTheme.colorScheme.onSurface,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { onNext() }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove item",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Borderless text field with a placeholder, so the note reads like a page
@Composable
private fun PlainTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle.copy(color = textColor),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NOTE_COLORS.forEachIndexed { index, option ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(noteColor(index, default = MaterialTheme.colorScheme.surface))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(index) })
                    .semantics { contentDescription = option.name },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
