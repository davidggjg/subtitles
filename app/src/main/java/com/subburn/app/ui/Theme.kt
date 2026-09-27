package com.subburn.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Void = Color(0xFF05070E)
val Panel = Color(0xFF0C1120)
val PanelHigh = Color(0xFF141B2E)
val Neon = Color(0xFF38F2E6)
val Violet = Color(0xFFA56BFF)
val Amber = Color(0xFFFFC46B)
val Danger = Color(0xFFFF5D73)
/** צבע אטום למלבני הכתוביות — מלבן אחד עשוי להיחתך בין שני מקטעים. */
val CueBlock = Color(0xFF3B2B63)
val TextHigh = Color(0xFFE9F0FF)
val TextDim = Color(0xFF8A97B8)

private val scheme = darkColorScheme(
    primary = Neon,
    onPrimary = Void,
    secondary = Violet,
    onSecondary = Void,
    background = Void,
    onBackground = TextHigh,
    surface = Panel,
    onSurface = TextHigh,
    surfaceVariant = PanelHigh,
    onSurfaceVariant = TextDim,
    error = Danger
)

private val typography = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelSmall = TextStyle(fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun SubBurnTheme(content: @Composable () -> Unit) {
    // The app is dark-mode only by design; isSystemInDarkTheme is read so the
    // composable still recomposes on a theme switch.
    isSystemInDarkTheme()
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
