package com.obdhero.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Canvas & Surfaces (OLED Pitch Black & Gunmetal Carbon)
val CanvasBg = Color(0xFF050608)
val SurfaceCard = Color(0xFF0F1116)
val SurfaceCardElevated = Color(0xFF171A22)
val BorderSubtle = Color(0xFF261D21)

// Performance Red Accents
val RaceRed = Color(0xFFFF1E42)
val CrimsonRed = Color(0xFFE60026)
val CriticalCrimson = CrimsonRed
val RubyGlow = Color(0xFFFF4D6D)
val AlertAmber = Color(0xFFFFB703)
val DarkCharcoal = Color(0xFF1E2129)
val EmeraldSuccess = Color(0xFF00E676)
val NeonCyan = RaceRed
val ElectricTeal = RubyGlow
val CyanPulse = Color(0xFF00E5FF)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)
val AccentSilver = Color(0xFF94A3B8)

val MidnightColorScheme = darkColorScheme(
    primary = RaceRed,
    onPrimary = Color.White,
    secondary = RubyGlow,
    onSecondary = Color.White,
    tertiary = AlertAmber,
    background = CanvasBg,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardElevated,
    onSurfaceVariant = TextSecondary,
    error = RaceRed,
    onError = Color.White
)

