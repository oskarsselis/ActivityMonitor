package com.example.activitycounter.ui.components

import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Finished checklist items and tasks fade into the background: greyed text and a grey
// checkbox instead of the accent color. Translucent, so it works on any note color.

@Composable
fun checkedTextColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

@Composable
fun checkedBoxColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

@Composable
fun greyCheckboxColors(): CheckboxColors = CheckboxDefaults.colors(
    checkedColor = checkedBoxColor(),
    checkmarkColor = MaterialTheme.colorScheme.surface
)
