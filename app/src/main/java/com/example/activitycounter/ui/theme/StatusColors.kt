package com.example.activitycounter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.activitycounter.model.ActivityStatus

data class StatusColors(val container: Color, val accent: Color, val content: Color)

@Composable
fun statusColors(status: ActivityStatus): StatusColors {
    val dark = isSystemInDarkTheme()
    return when (status) {
        ActivityStatus.COMPLETED ->
            if (dark) StatusColors(CompletedContainerDark, CompletedAccentDark, CompletedContentDark)
            else StatusColors(CompletedContainerLight, CompletedAccentLight, CompletedContentLight)
        ActivityStatus.LATE ->
            if (dark) StatusColors(LateContainerDark, LateAccentDark, LateContentDark)
            else StatusColors(LateContainerLight, LateAccentLight, LateContentLight)
        ActivityStatus.MISSED ->
            if (dark) StatusColors(MissedContainerDark, MissedAccentDark, MissedContentDark)
            else StatusColors(MissedContainerLight, MissedAccentLight, MissedContentLight)
        ActivityStatus.PENDING -> StatusColors(
            container = MaterialTheme.colorScheme.surfaceContainerHigh,
            accent = MaterialTheme.colorScheme.primary,
            content = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
