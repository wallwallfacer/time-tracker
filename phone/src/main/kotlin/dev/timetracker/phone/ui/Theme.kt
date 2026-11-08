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
internal val LightTones = mapOf(
    0xFFE57373 to TagTones(Color(0xFFD9534F), Color(0xFFFFDAD6), Color(0xFF410002)),
    0xFFFFB74D to TagTones(Color(0xFFCC7A00), Color(0xFFFFDDB5), Color(0xFF2A1800)),
    0xFFFFF176 to TagTones(Color(0xFF9C8A00), Color(0xFFF3E98C), Color(0xFF201C00)),
    0xFF81C784 to TagTones(Color(0xFF43A047), Color(0xFFC3EFC0), Color(0xFF002106)),
    0xFF4DB6AC to TagTones(Color(0xFF00897B), Color(0xFFB4EFE6), Color(0xFF00201D)),
    0xFF64B5F6 to TagTones(Color(0xFF1E88E5), Color(0xFFD0E4FF), Color(0xFF001D35)),
    0xFF9575CD to TagTones(Color(0xFF7E57C2), Color(0xFFE9DDFF), Color(0xFF22005D)),
    0xFFF06292 to TagTones(Color(0xFFD81B60), Color(0xFFFFD9E2), Color(0xFF3E001D)),
    0xFFF277FE to TagTones(Color(0xFFD359DF), Color(0xFFF9DDFB), Color(0xFF371939)),
    0xFFE5BBE1 to TagTones(Color(0xFFA880A4), Color(0xFFFCDDF8), Color(0xFF381837)),
    0xFF9ED3F2 to TagTones(Color(0xFF5F92AF), Color(0xFFD0EDFE), Color(0xFF012A3D)),
    0xFFA3745B to TagTones(Color(0xFFA3745B), Color(0xFFFEE1D2), Color(0xFF401A02)),
    0xFFADA3FF to TagTones(Color(0xFF8C81DA), Color(0xFFE6E5FE), Color(0xFF251F46)),
    0xFFB5D6AA to TagTones(Color(0xFF76956C), Color(0xFFD8F0D0), Color(0xFF132E08)),
    0xFFFD80C4 to TagTones(Color(0xFFDA60A4), Color(0xFFFFDDED), Color(0xFF3E162C)),
    0xFFAD75FE to TagTones(Color(0xFFA76FF7), Color(0xFFEBE3FE), Color(0xFF2C1D43)),
    0xFFFA6004 to TagTones(Color(0xFFF05C04), Color(0xFFFEE1D5), Color(0xFF421806)),
    0xFFF8AAFF to TagTones(Color(0xFFBC72C3), Color(0xFFF9DDFB), Color(0xFF371939)),
    0xFF7E7AA8 to TagTones(Color(0xFF7E7AA8), Color(0xFFE6E5FE), Color(0xFF261F46)),
    0xFFF3909D to TagTones(Color(0xFFCF707D), Color(0xFFFFDFE1), Color(0xFF42151D)),
    0xFF3A74FF to TagTones(Color(0xFF3A74FF), Color(0xFFDDE8FE), Color(0xFF142548)),
    0xFF86F8CF to TagTones(Color(0xFF1B9E7A), Color(0xFFC9F3E2), Color(0xFF012F21)),
    0xFF088BBE to TagTones(Color(0xFF088BBE), Color(0xFFCFEDFF), Color(0xFF002A3D)),
    0xFFAC731A to TagTones(Color(0xFFAC731A), Color(0xFFFDE3C4), Color(0xFF372100)),
    0xFF6E8D35 to TagTones(Color(0xFF6E8D35), Color(0xFFDFEFCB), Color(0xFF1E2C01)),
    0xFFFE5166 to TagTones(Color(0xFFFA4D63), Color(0xFFFEDFDF), Color(0xFF421519)),
    0xFFE0C796 to TagTones(Color(0xFFA28A5B), Color(0xFFF8E6C3), Color(0xFF332401)),
    0xFF718FFF to TagTones(Color(0xFF6886F5), Color(0xFFDFE7FE), Color(0xFF192348)),
    0xFF6CF6FB to TagTones(Color(0xFF0A9CA0), Color(0xFFC2F3F4), Color(0xFF002D2F)),
    0xFF5B7EC8 to TagTones(Color(0xFF5B7EC8), Color(0xFFDDE8FE), Color(0xFF142548)),
    0xFFF19773 to TagTones(Color(0xFFCC7552), Color(0xFFFEE1D5), Color(0xFF411806)),
    0xFFA26F83 to TagTones(Color(0xFFA26F83), Color(0xFFFFDDE9), Color(0xFF3F1529)),
).mapKeys { it.key.toInt() }

internal val DarkTones = mapOf(
    0xFFE57373 to TagTones(Color(0xFFE57373), Color(0xFF6B2A27), Color(0xFFFFDAD6)),
    0xFFFFB74D to TagTones(Color(0xFFFFB74D), Color(0xFF5E4100), Color(0xFFFFDDB5)),
    0xFFFFF176 to TagTones(Color(0xFFFFF176), Color(0xFF4C4600), Color(0xFFF3E98C)),
    0xFF81C784 to TagTones(Color(0xFF81C784), Color(0xFF1F5223), Color(0xFFC3EFC0)),
    0xFF4DB6AC to TagTones(Color(0xFF4DB6AC), Color(0xFF00504A), Color(0xFFB4EFE6)),
    0xFF64B5F6 to TagTones(Color(0xFF64B5F6), Color(0xFF00497C), Color(0xFFD0E4FF)),
    0xFF9575CD to TagTones(Color(0xFF9575CD), Color(0xFF4F378A), Color(0xFFE9DDFF)),
    0xFFF06292 to TagTones(Color(0xFFF06292), Color(0xFF7A2949), Color(0xFFFFD9E2)),
    0xFFF277FE to TagTones(Color(0xFFF277FE), Color(0xFF512D55), Color(0xFFF6DFF7)),
    0xFFE5BBE1 to TagTones(Color(0xFFE5BBE1), Color(0xFF512F4F), Color(0xFFF8DFF5)),
    0xFF9ED3F2 to TagTones(Color(0xFF9ED3F2), Color(0xFF0A425C), Color(0xFFD0EDFE)),
    0xFFA3745B to TagTones(Color(0xFFA3745B), Color(0xFF5A3119), Color(0xFFFEE1D2)),
    0xFFADA3FF to TagTones(Color(0xFFADA3FF), Color(0xFF3B3564), Color(0xFFE6E5FE)),
    0xFFB5D6AA to TagTones(Color(0xFFB5D6AA), Color(0xFF2A4620), Color(0xFFDBEFD5)),
    0xFFFD80C4 to TagTones(Color(0xFFFD80C4), Color(0xFF5B2A44), Color(0xFFFEDEED)),
    0xFFAD75FE to TagTones(Color(0xFFAD75FE), Color(0xFF443261), Color(0xFFEBE3FE)),
    0xFFFA6004 to TagTones(Color(0xFFFA6004), Color(0xFF5F2D18), Color(0xFFFEE1D5)),
    0xFFF8AAFF to TagTones(Color(0xFFF8AAFF), Color(0xFF512D55), Color(0xFFF6DFF7)),
    0xFF7E7AA8 to TagTones(Color(0xFF7E7AA8), Color(0xFF3C375F), Color(0xFFE6E5FE)),
    0xFFF3909D to TagTones(Color(0xFFF3909D), Color(0xFF602932), Color(0xFFFFDFE1)),
    0xFF3A74FF to TagTones(Color(0xFF3A74FF), Color(0xFF273B67), Color(0xFFDDE8FE)),
    0xFF86F8CF to TagTones(Color(0xFF86F8CF), Color(0xFF024936), Color(0xFFCFF1E3)),
    0xFF088BBE to TagTones(Color(0xFF088BBE), Color(0xFF02435E), Color(0xFFCFEDFF)),
    0xFFAC731A to TagTones(Color(0xFFAC731A), Color(0xFF553501), Color(0xFFF9E4CB)),
    0xFF6E8D35 to TagTones(Color(0xFF6E8D35), Color(0xFF32440F), Color(0xFFE1EDD1)),
    0xFFFE5166 to TagTones(Color(0xFFFE5166), Color(0xFF60292D), Color(0xFFFEDFDF)),
    0xFFE0C796 to TagTones(Color(0xFFE0C796), Color(0xFF4E3906), Color(0xFFF5E6CA)),
    0xFF718FFF to TagTones(Color(0xFF718FFF), Color(0xFF2D3A67), Color(0xFFDFE7FE)),
    0xFF6CF6FB to TagTones(Color(0xFF6CF6FB), Color(0xFF034649), Color(0xFFCAF1F2)),
    0xFF5B7EC8 to TagTones(Color(0xFF5B7EC8), Color(0xFF273B67), Color(0xFFDDE8FE)),
    0xFFF19773 to TagTones(Color(0xFFF19773), Color(0xFF5F2D18), Color(0xFFFEE1D5)),
    0xFFA26F83 to TagTones(Color(0xFFA26F83), Color(0xFF592C3F), Color(0xFFFFDDE9)),
).mapKeys { it.key.toInt() }

@Composable
fun tagTones(color: Int): TagTones {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    return (if (dark) DarkTones else LightTones)[color]
        ?: Color(color).let { TagTones(it, lerp(it, scheme.surface, if (dark) 0.65f else 0.75f), scheme.onSurface) }
}
