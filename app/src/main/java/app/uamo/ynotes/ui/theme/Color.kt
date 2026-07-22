package app.uamo.ynotes.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// 2026 Core Palette
val PrimaryAccent = Color(0xFF8B5CF6) // Deep Purple / Indigo
val SecondaryAccent = Color(0xFF06B6D4) // Cyan / Aqua
val TertiaryAccent = Color(0xFFEC4899) // Vibrant Pink

// Glassmorphism & AMOLED
val AmoledBlack = Color(0xFF000000)
val GlassSurface = Color(0x15FFFFFF) // Very subtle translucent white
val GlassSurfaceHighlight = Color(0x25FFFFFF) 
val GlassBorder = Color(0x33FFFFFF)
val TextPrimary = Color(0xFFF3F4F6)
val TextSecondary = Color(0xFF9CA3AF)

// Aurora Gradients for elements
val AuroraPrimary = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)))
val AuroraSecondary = Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)))

// Google Theme Colors
val GoogleLightBackground = Color(0xFFF8F9FA)
val GoogleLightSurface = Color(0xFFFFFFFF)
val GoogleDarkBackground = Color(0xFF202124)
val GoogleDarkSurface = Color(0xFF202124)
val GooglePrimary = Color(0xFF1A73E8)
val GoogleBorderLight = Color(0xFFE0E0E0)
val GoogleBorderDark = Color(0xFF5F6368)

// Samsung Theme Colors
val SamsungLightBackground = Color(0xFFF2F2F7)
val SamsungLightSurface = Color(0xFFFFFFFF)
val SamsungDarkBackground = Color(0xFF000000)
val SamsungDarkSurface = Color(0xFF1C1C1E)
val SamsungPrimary = Color(0xFFFF9F0A) // Or a nice soft blue/orange

