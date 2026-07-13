package app.uamo.ynotes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAccent,
    secondary = SecondaryAccent,
    tertiary = TertiaryAccent,
    background = AmoledBlack,
    surface = AmoledBlack, // Base surface is black, we'll use GlassSurface for cards
    surfaceVariant = GlassSurface,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryAccent,
    secondary = SecondaryAccent,
    tertiary = TertiaryAccent
    // We force AMOLED dark mode anyway for this app as requested
)

@Composable
fun YNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context).copy(
                    background = AmoledBlack,
                    surface = AmoledBlack,
                    surfaceVariant = GlassSurface,
                    onSurface = TextPrimary,
                    onSurfaceVariant = TextSecondary,
                    outline = GlassBorder
                )
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
