package com.example.activitycounter.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.activitycounter.data.NotesViewModel
import com.example.activitycounter.model.ChecklistItem
import com.example.activitycounter.model.Note
import com.example.activitycounter.model.parseChecklist
import com.example.activitycounter.ui.components.EmptyState
import com.example.activitycounter.ui.components.checkedBoxColor
import com.example.activitycounter.ui.components.checkedTextColor
import com.example.activitycounter.ui.theme.PushPinIcon
import com.example.activitycounter.ui.theme.noteColor

private const val MAX_PREVIEW_ITEMS = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(viewModel: NotesViewModel) {
    val notes by viewModel.notes.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(title = { Text("Notes") }, scrollBehavior = scrollBehavior)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::newNote,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New note") }
            )
        }
    ) { padding ->
        val loaded = notes
        when {
            loaded == null -> Unit // still loading
            loaded.isEmpty() -> EmptyState(
                icon = Icons.Default.Create,
                title = "No notes yet",
                message = "Tap “New note” to write your first one.",
                modifier = Modifier.padding(padding)
            )
            else -> NotesGrid(
                notes = loaded,
                onOpen = viewModel::openNote,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun NotesGrid(notes: List<Note>, onOpen: (Note) -> Unit, modifier: Modifier = Modifier) {
    val (pinned, others) = notes.partition { it.isPinned }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp
    ) {
        if (pinned.isNotEmpty()) {
            item(key = "pinned-header", span = StaggeredGridItemSpan.FullLine) {
                SectionHeader("Pinned")
            }
            items(pinned, key = { it.id }) { note ->
                NoteCard(note, onClick = { onOpen(note) }, modifier = Modifier.animateItem())
            }
            if (others.isNotEmpty()) {
                item(key = "others-header", span = StaggeredGridItemSpan.FullLine) {
                    SectionHeader("Others")
                }
            }
        }
        items(others, key = { it.id }) { note ->
            NoteCard(note, onClick = { onOpen(note) }, modifier = Modifier.animateItem())
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteCard(note: Note, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = noteColor(note.colorIndex, default = MaterialTheme.colorScheme.surfaceContainerHigh),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (note.title.isNotEmpty() || note.isPinned) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (note.isPinned) {
                        Icon(
                            imageVector = PushPinIcon,
                            contentDescription = "Pinned",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (note.isChecklist) {
                ChecklistPreview(parseChecklist(note.content))
            } else if (note.content.isNotEmpty()) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ChecklistPreview(items: List<ChecklistItem>) {
    val shown = items.take(MAX_PREVIEW_ITEMS)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        shown.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                CheckGlyph(item.checked)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                    color = if (item.checked) checkedTextColor() else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (items.size > shown.size) {
            Text(
                text = "+${items.size - shown.size} more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Small non-interactive checkbox for previews
@Composable
private fun CheckGlyph(checked: Boolean) {
    val color = if (checked) checkedBoxColor() else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = Modifier
            .size(14.dp)
            .border(1.5.dp, color, shape)
            .then(if (checked) Modifier.background(color, shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.surface
            )
        }
    }
}
