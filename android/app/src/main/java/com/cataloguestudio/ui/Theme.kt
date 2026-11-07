package com.cataloguestudio.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Paper = Color(0xFFF8F7F3)
private val Ink = Color(0xFF182D27)
private val Forest = Color(0xFF214C3E)
private val Sage = Color(0xFFE7EDE5)
private val Muted = Color(0xFF69716A)
private val Line = Color(0xFFDBDFD6)

private val StudioColors =
    lightColorScheme(
        primary = Forest,
        onPrimary = Color.White,
        primaryContainer = Sage,
        onPrimaryContainer = Forest,
        secondary = Muted,
        onSecondary = Color.White,
        background = Paper,
        onBackground = Ink,
        surface = Paper,
        onSurface = Ink,
        surfaceVariant = Sage,
        onSurfaceVariant = Muted,
        outline = Muted,
        outlineVariant = Line,
        surfaceTint = Forest,
    )

@Composable
fun StudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = StudioColors, content = content)
}
