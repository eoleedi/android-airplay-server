package io.github.jqssun.airplay.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.jqssun.airplay.ui.isTv

// Stable colors and a ten-foot type scale, independent of the TV's wallpaper.
private val TvColors = darkColorScheme(
    primary = Color(0xFFB9DEFF), onPrimary = Color(0xFF10344D),
    primaryContainer = Color(0xFF25465F), onPrimaryContainer = Color(0xFFE0F0FF),
    secondary = Color(0xFFA9D5C5), onSecondary = Color(0xFF14392D),
    secondaryContainer = Color(0xFF25483D), onSecondaryContainer = Color(0xFFD2F4E6),
    background = Color(0xFF101B29), onBackground = Color(0xFFF0F4FA),
    surface = Color(0xFF101B29), onSurface = Color(0xFFF0F4FA),
    surfaceVariant = Color(0xFF203247), onSurfaceVariant = Color(0xFFBDCCDD),
    surfaceContainer = Color(0xFF182739),
    surfaceContainerLow = Color(0xFF152233), surfaceContainerLowest = Color(0xFF101B29),
    surfaceContainerHigh = Color(0xFF203247), surfaceContainerHighest = Color(0xFF2B4056),
    outline = Color(0xFF8295AA), outlineVariant = Color(0xFF3D5269),
)

private val TvTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 40.sp, lineHeight = 48.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 32.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 28.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 26.sp),
    bodyLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodySmall = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
    labelMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    labelSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
)

@Composable
fun AirPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val tv = isTv()
    val colorScheme = when {
        tv -> TvColors
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(colorScheme = colorScheme, typography = if (tv) TvTypography else Typography(), content = content)
}
