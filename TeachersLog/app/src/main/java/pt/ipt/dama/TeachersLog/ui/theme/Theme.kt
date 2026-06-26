package pt.ipt.dama.TeachersLog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt

/**
 * Global shape definitions for the application.
 */
private val ProjectShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * AppTheme provides the single entry point for all project-specific design tokens.
 * It encapsulates Material 3 roles and provides access to custom semantic colors.
 * Use this instead of MaterialTheme to ensure project-specific styles are applied.
 */
object AppTheme {
    val colors: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes

    /**
     * Standard circular shape.
     */
    val circle = CircleShape

    /**
     * Fully transparent color.
     */
    val transparent = Color.Transparent

    /**
     * Parses a hex color string into a Compose Color object.
     * @param hex The color string (e.g., "#FFFFFF").
     * @return The Compose Color object.
     */
    fun parseColor(hex: String): Color = Color(hex.toColorInt())

    /**
     * Determines the semantic color for a grade based on its percentage (0.0 to 100.0).
     */
    @Composable
    @ReadOnlyComposable
    fun getGradeColor(percentage: Double): Color = when {
        percentage >= 80.0 -> success
        percentage >= 50.0 -> warning
        else -> critical
    }

    /**
     * Calculates the 0-100 percentage of a grade based on its scale.
     */
    fun calculatePercentage(grade: Double, scale: String): Double {
        val max = when (scale) { "20" -> 20.0; "100" -> 100.0; else -> 5.0 }
        return (grade / max) * 100.0
    }

    // --- Semantic Project Colors ---
    
    val success: Color @Composable @ReadOnlyComposable get() = if (!isSystemInDarkTheme()) SuccessLight else SuccessDark
    val warning: Color @Composable @ReadOnlyComposable get() = if (!isSystemInDarkTheme()) WarningLight else WarningDark
    val critical: Color @Composable @ReadOnlyComposable get() = if (!isSystemInDarkTheme()) CriticalLight else CriticalDark
}

/**
 * Dark color scheme following Material 3.1 guidelines.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
)

/**
 * Light color scheme following Material 3.1 guidelines.
 */
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
)

/**
 * Composable theme wrapper for the application.
 *
 * @param darkTheme Boolean flag to toggle between themes.
 * @param content Composable tree inheriting this theme.
 */
@Composable
fun TeachersLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ProjectShapes,
        content = content
    )
}
