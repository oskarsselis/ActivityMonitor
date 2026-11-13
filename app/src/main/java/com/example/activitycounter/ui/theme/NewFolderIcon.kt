package com.example.activitycounter.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Material "create_new_folder" icon, drawn here for the same reason as PushPinIcon
val NewFolderIcon: ImageVector = ImageVector.Builder(
    name = "CreateNewFolder",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).addPath(
    pathData = addPathNodes(
        "M20,6h-8l-2,-2H4C2.89,4 2.01,4.89 2.01,6L2,18c0,1.11 0.89,2 2,2h16c1.11,0 2,-0.89 2,-2V8" +
            "C22,6.89 21.11,6 20,6zM19,14h-3v3h-2v-3h-3v-2h3V9h2v3h3V14z"
    ),
    fill = SolidColor(Color.Black)
).build()
