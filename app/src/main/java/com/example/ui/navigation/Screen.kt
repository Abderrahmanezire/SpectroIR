package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Default.Home),
    ANALYZE("analyze", "Analyze", Icons.AutoMirrored.Filled.ShowChart),
    DIGITIZE("digitize", "Digitize", Icons.Default.CameraAlt),
    MANUAL_ANALYSIS("manual", "Manual", Icons.Default.Science),
    DICTIONARY("dictionary", "Dictionary", Icons.AutoMirrored.Filled.MenuBook),
    REFERENCE("reference", "Reference", Icons.Default.Science),
    HISTORY("history", "History", Icons.Default.History),
    SETTINGS("settings", "Settings", Icons.Default.Settings);
}
