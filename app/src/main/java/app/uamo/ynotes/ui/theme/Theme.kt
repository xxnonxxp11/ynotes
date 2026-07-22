package app.uamo.ynotes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
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

enum class AppThemeType {
    AMOLED,
    GOOGLE,
    SAMSUNG
}

val LocalAppTheme = compositionLocalOf { AppThemeType.AMOLED }

private val GoogleLightColorScheme = lightColorScheme(
    primary = GooglePrimary,
    background = GoogleLightBackground,
    surface = GoogleLightSurface,
    surfaceVariant = GoogleLightSurface,
    onSurface = Color.Black,
    onSurfaceVariant = Color.DarkGray,
    outline = GoogleBorderLight
)

private val GoogleDarkColorScheme = darkColorScheme(
    primary = GooglePrimary,
    background = GoogleDarkBackground,
    surface = GoogleDarkSurface,
    surfaceVariant = GoogleDarkSurface,
    onSurface = Color.White,
    onSurfaceVariant = Color.LightGray,
    outline = GoogleBorderDark
)

private val SamsungLightColorScheme = lightColorScheme(
    primary = SamsungPrimary,
    background = SamsungLightBackground,
    surface = SamsungLightSurface,
    surfaceVariant = SamsungLightSurface,
    onSurface = Color.Black,
    onSurfaceVariant = Color.DarkGray,
    outline = Color.Transparent
)

private val SamsungDarkColorScheme = darkColorScheme(
    primary = SamsungPrimary,
    background = SamsungDarkBackground,
    surface = SamsungDarkSurface,
    surfaceVariant = SamsungDarkSurface,
    onSurface = Color.White,
    onSurfaceVariant = Color.LightGray,
    outline = Color.Transparent
)

@Composable
fun YNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeType: AppThemeType = AppThemeType.AMOLED,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeType) {
        AppThemeType.GOOGLE -> if (darkTheme) GoogleDarkColorScheme else GoogleLightColorScheme
        AppThemeType.SAMSUNG -> if (darkTheme) SamsungDarkColorScheme else SamsungLightColorScheme
        AppThemeType.AMOLED -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
            } else if (darkTheme) {
                DarkColorScheme
            } else {
                LightColorScheme
            }
        }
    }

    CompositionLocalProvider(LocalAppTheme provides themeType) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
