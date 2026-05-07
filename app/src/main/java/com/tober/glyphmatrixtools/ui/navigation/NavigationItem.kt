package com.tober.glyphmatrixtools.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavigationItem(
    val title: String,
    val icon: ImageVector
) {
    GlyphCanvas(
        title = "Glyph Canvas",
        icon = Icons.Filled.Brush
    ),

    ScreenWakeGlyphs(
        title = "Screen Wake Glyphs",
        icon = Icons.Filled.Visibility
    ),

    NotificationGlyphs(
        title = "Notification Glyphs",
        icon = Icons.Filled.Notifications
    ),

    CallGlyphs(
        title = "Call Glyphs",
        icon = Icons.Filled.Call
    ),

    Settings(
        title = "Settings",
        icon = Icons.Filled.Settings
    )
}
