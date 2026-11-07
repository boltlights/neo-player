package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.MusicViewModel

@Composable
fun SmartVolumeSettingsScreen(
    viewModel: MusicViewModel,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onBack: () -> Unit
) {
    SmartVolumeScreen(
        onBgColor = onBgColor,
        viewModel = viewModel,
        accentColor = accentColor,
        surfaceColor = surfaceColor,
        onBack = onBack
    )
}
