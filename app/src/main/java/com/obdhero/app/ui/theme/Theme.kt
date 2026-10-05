package com.obdhero.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val CanvasBg = Color(0xFF07090E)
val SurfaceCard = Color(0xFF111622)
val SurfaceCardElevated = Color(0xFF181F30)
val BorderSubtle = Color(0xFF222B3D)

// Neon & High-Visibility Accents
val NeonCyan = Color(0xFF00E5FF)
val ElectricTeal = Color(0xFF00B4D8)
val AlertAmber = Color(0xFFFFB703)
val CautionOrange = Color(0xFFFB8500)
val CriticalCrimson = Color(0xFFFF3366)
val EmeraldSuccess = Color(0xFF06D6A0)

val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

val MidnightColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    secondary = ElectricTeal,
    onSecondary = Color.Black,
    tertiary = AlertAmber,
    background = CanvasBg,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardElevated,
    onSurfaceVariant = TextSecondary,
    error = CriticalCrimson,
    onError = Color.White
)
