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
