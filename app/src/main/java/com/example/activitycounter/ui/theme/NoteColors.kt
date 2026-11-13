package com.example.activitycounter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

class NoteColor(val name: String, val light: Color, val dark: Color)

// Index 0 is "Default" (no color); notes store the index
val NOTE_COLORS = listOf(
    NoteColor("Default", Color.Unspecified, Color.Unspecified),
    NoteColor("Red", Color(0xFFFAAFA8), Color(0xFF77172E)),
    NoteColor("Orange", Color(0xFFF7C59F), Color(0xFF692B17)),
    NoteColor("Yellow", Color(0xFFFFF4B0), Color(0xFF7C4A03)),
    NoteColor("Green", Color(0xFFD4EFC1), Color(0xFF264D3B)),
    NoteColor("Teal", Color(0xFFB4DDD3), Color(0xFF0C625D)),
    NoteColor("Blue", Color(0xFFC9DEF0), Color(0xFF256377)),
    NoteColor("Purple", Color(0xFFDDCBEB), Color(0xFF472E5B))
)

// The note's color for the current theme, or [default] for "Default" / unknown indexes
@Composable
fun noteColor(index: Int, default: Color): Color {
    val dark = isSystemInDarkTheme()
    if (index !in 1 until NOTE_COLORS.size) return default
    return if (dark) NOTE_COLORS[index].dark else NOTE_COLORS[index].light
}
