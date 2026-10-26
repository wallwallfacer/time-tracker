package dev.timetracker.phone.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.timetracker.phone.R

/** Google Sans Flex with full roundness (ROND 100), bundled as static weights. */
val GoogleSansFlex = FontFamily(
    Font(R.font.google_sans_flex_400, FontWeight.Normal),
    Font(R.font.google_sans_flex_500, FontWeight.Medium),
    Font(R.font.google_sans_flex_600, FontWeight.SemiBold),
    Font(R.font.google_sans_flex_700, FontWeight.Bold),
)

// Fixed M3 tonal scheme from seed #4355B9; no dynamic color, so the rail and tag tones always match.
private val LightColors = lightColorScheme(
    primary = Color(0xFF4355B9), onPrimary = Color.White,
    primaryContainer = Color(0xFFDEE0FF), onPrimaryContainer = Color(0xFF00105C),
    secondaryContainer = Color(0xFFE1E0F9), onSecondaryContainer = Color(0xFF191A2C),
    background = Color(0xFFFBF8FF), onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF), onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC), onSurfaceVariant = Color(0xFF46464F),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF5F2FA),
    surfaceContainer = Color(0xFFEFEDF4), surfaceContainerHigh = Color(0xFFE9E7EF),
    surfaceContainerHighest = Color(0xFFE3E1E9),
    outline = Color(0xFF777680), outlineVariant = Color(0xFFC7C5D0),
    error = Color(0xFFBA1A1A), errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF303036), inverseOnSurface = Color(0xFFF2EFF7), inversePrimary = Color(0xFFBAC3FF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBAC3FF), onPrimary = Color(0xFF08218A),
    primaryContainer = Color(0xFF293CA0), onPrimaryContainer = Color(0xFFDEE0FF),
    secondaryContainer = Color(0xFF444559), onSecondaryContainer = Color(0xFFE1E0F9),
    background = Color(0xFF121318), onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF121318), onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F), onSurfaceVariant = Color(0xFFC7C5D0),
    surfaceContainerLowest = Color(0xFF0D0E13), surfaceContainerLow = Color(0xFF1A1B21),
    surfaceContainer = Color(0xFF1F1F25), surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A),
    outline = Color(0xFF91909A), outlineVariant = Color(0xFF46464F),
    error = Color(0xFFFFB4AB), errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE4E1E9), inverseOnSurface = Color(0xFF303036), inversePrimary = Color(0xFF4355B9),
)

private fun style(size: Int, weight: FontWeight, line: Int, tracking: Double = 0.0) =
    TextStyle(fontFamily = GoogleSansFlex, fontSize = size.sp, fontWeight = weight, lineHeight = line.sp, letterSpacing = tracking.em)

private val AppTypography = Typography(
    displayLarge = style(104, FontWeight.Medium, 104, -0.03),
    displayMedium = style(52, FontWeight.Medium, 56, -0.02),
    headlineMedium = style(32, FontWeight.SemiBold, 40, -0.01),
    titleLarge = style(22, FontWeight.SemiBold, 28, -0.01),
    titleMedium = style(16, FontWeight.SemiBold, 22),
    titleSmall = style(15, FontWeight.SemiBold, 20),
    bodyLarge = style(16, FontWeight.Normal, 24),
    bodyMedium = style(14, FontWeight.Normal, 20),
    bodySmall = style(12, FontWeight.Medium, 16),
    labelLarge = style(15, FontWeight.SemiBold, 20),
    labelMedium = style(13, FontWeight.SemiBold, 18),
    labelSmall = style(12, FontWeight.SemiBold, 16, 0.06),
)

/** Tabular figures, so times and durations line up in columns. */
val TextStyle.tabular: TextStyle get() = copy(fontFeatureSettings = "tnum")

@Composable
fun TimeTrackerTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = AppTypography, content = content)
}

/** How one tag paints: [rail] for the timeline segment and dots, [container]/[onContainer] for its cards and chips. */
@Immutable
data class TagTones(val rail: Color, val container: Color, val onContainer: Color)

// Keyed by the stored TagPalette color. Light rails sit near tone 50 (3:1 on surface); dark rails are the stored hex.
private val LightTones = mapOf(
    0xFFE57373 to TagTones(Color(0xFFD9534F), Color(0xFFFFDAD6), Color(0xFF410002)),
    0xFFFFB74D to TagTones(Color(0xFFD98200), Color(0xFFFFDDB5), Color(0xFF2A1800)),
    0xFFFFF176 to TagTones(Color(0xFFA89400), Color(0xFFF3E98C), Color(0xFF201C00)),
    0xFF81C784 to TagTones(Color(0xFF43A047), Color(0xFFC3EFC0), Color(0xFF002106)),
    0xFF4DB6AC to TagTones(Color(0xFF00897B), Color(0xFFB4EFE6), Color(0xFF00201D)),
    0xFF64B5F6 to TagTones(Color(0xFF1E88E5), Color(0xFFD0E4FF), Color(0xFF001D35)),
    0xFF9575CD to TagTones(Color(0xFF7E57C2), Color(0xFFE9DDFF), Color(0xFF22005D)),
    0xFFF06292 to TagTones(Color(0xFFD81B60), Color(0xFFFFD9E2), Color(0xFF3E001D)),
).mapKeys { it.key.toInt() }

private val DarkTones = mapOf(
    0xFFE57373 to TagTones(Color(0xFFE57373), Color(0xFF6B2A27), Color(0xFFFFDAD6)),
    0xFFFFB74D to TagTones(Color(0xFFFFB74D), Color(0xFF5E4100), Color(0xFFFFDDB5)),
    0xFFFFF176 to TagTones(Color(0xFFFFF176), Color(0xFF4C4600), Color(0xFFF3E98C)),
    0xFF81C784 to TagTones(Color(0xFF81C784), Color(0xFF1F5223), Color(0xFFC3EFC0)),
    0xFF4DB6AC to TagTones(Color(0xFF4DB6AC), Color(0xFF00504A), Color(0xFFB4EFE6)),
    0xFF64B5F6 to TagTones(Color(0xFF64B5F6), Color(0xFF00497C), Color(0xFFD0E4FF)),
    0xFF9575CD to TagTones(Color(0xFF9575CD), Color(0xFF4F378A), Color(0xFFE9DDFF)),
    0xFFF06292 to TagTones(Color(0xFFF06292), Color(0xFF7A2949), Color(0xFFFFD9E2)),
).mapKeys { it.key.toInt() }

@Composable
fun tagTones(color: Int): TagTones {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    return (if (dark) DarkTones else LightTones)[color]
        ?: Color(color).let { TagTones(it, lerp(it, scheme.surface, if (dark) 0.65f else 0.75f), scheme.onSurface) }
}
