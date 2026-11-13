package com.example.activitycounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.activitycounter.data.NotesViewModel
import com.example.activitycounter.ui.activities.ActivitiesScreen
import com.example.activitycounter.ui.notes.NoteEditorScreen
import com.example.activitycounter.ui.notes.NotesScreen
import com.example.activitycounter.ui.tasks.TasksScreen
import com.example.activitycounter.ui.theme.ActivityCounterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ActivityCounterTheme {
                App()
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    ACTIVITIES("Activities", Icons.Default.CheckCircle),
    NOTES("Notes", Icons.Default.Create),
    TASKS("Tasks", Icons.AutoMirrored.Filled.List)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun App(notesViewModel: NotesViewModel = viewModel()) {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.ACTIVITIES) }
    // Keeps each tab's scroll position and expanded sections while another tab is shown
    val tabStateHolder = rememberSaveableStateHolder()

    Box {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == selectedTab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            },
            // Each tab's own Scaffold handles the status bar
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
            ) {
                tabStateHolder.SaveableStateProvider(selectedTab.name) {
                    when (selectedTab) {
                        Tab.ACTIVITIES -> ActivitiesScreen()
                        Tab.NOTES -> NotesScreen(notesViewModel)
                        Tab.TASKS -> TasksScreen()
                    }
                }
            }
        }

        // Shown over the tabs so the notes list keeps its scroll position
        notesViewModel.draft?.let { draft ->
            NoteEditorScreen(
                draft = draft,
                onChange = notesViewModel::updateDraft,
                onClose = notesViewModel::closeEditor,
                onDelete = notesViewModel::deleteDraft,
                onAppBackgrounded = notesViewModel::saveDraft
            )
        }
    }
}
