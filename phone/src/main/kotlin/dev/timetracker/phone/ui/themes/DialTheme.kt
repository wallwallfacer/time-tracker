package dev.timetracker.phone.ui.themes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import dev.timetracker.core.formatDuration
import dev.timetracker.core.formatElapsed
import dev.timetracker.core.formatTime
import dev.timetracker.phone.R
import dev.timetracker.phone.ui.appLocale
import dev.timetracker.phone.ui.AppTheme
import dev.timetracker.phone.ui.Radii
import dev.timetracker.phone.ui.TagTones
import dev.timetracker.phone.ui.TimelineRowArgs
import dev.timetracker.phone.ui.tabular
import dev.timetracker.phone.ui.tagTones
import androidx.compose.material3.ColorScheme

/** The few Dial colours that are not Material roles. */
private object DialInk {
    val scrim = Color(0xCC1D1B18)
}

private val Light = lightColorScheme(
    primary = Color(0xFFEF5230), onPrimary = Color(0xFFFFFDF8),
    primaryContainer = Color(0xFFFBD9CE), onPrimaryContainer = Color(0xFF4A1100),
    secondaryContainer = Color(0xFFE6E1D3), onSecondaryContainer = Color(0xFF1D1B18),
    background = Color(0xFFF2EFE8), onBackground = Color(0xFF1D1B18),
    surface = Color(0xFFF2EFE8), onSurface = Color(0xFF1D1B18),
    surfaceVariant = Color(0xFFE6E1D3), onSurfaceVariant = Color(0xFF6A665C),
    surfaceContainerLowest = Color(0xFFFFFDF8), surfaceContainerLow = Color(0xFFFFFDF8),
    surfaceContainer = Color(0xFFEDE9DE), surfaceContainerHigh = Color(0xFFE6E1D3),
    surfaceContainerHighest = Color(0xFFDDD7C7),
    outline = Color(0xFFA8A290), outlineVariant = Color(0xFFD3CEBF),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFDF8),
    errorContainer = Color(0xFFF9DEDA), onErrorContainer = Color(0xFF410E0B),
    inverseSurface = Color(0xFF1D1B18), inverseOnSurface = Color(0xFFFFFDF8), inversePrimary = Color(0xFFEF5230),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFF6A47), onPrimary = Color(0xFF2B0A00),
    primaryContainer = Color(0xFF6B2412), onPrimaryContainer = Color(0xFFFFDBD1),
    secondaryContainer = Color(0xFF3A3730), onSecondaryContainer = Color(0xFFEFE9DC),
    background = Color(0xFF1B1A17), onBackground = Color(0xFFEFE9DC),
    surface = Color(0xFF1B1A17), onSurface = Color(0xFFEFE9DC),
    surfaceVariant = Color(0xFF322F2A), onSurfaceVariant = Color(0xFFB3AC9C),
    surfaceContainerLowest = Color(0xFF151411), surfaceContainerLow = Color(0xFF24221E),
    surfaceContainer = Color(0xFF292723), surfaceContainerHigh = Color(0xFF322F2A),
    surfaceContainerHighest = Color(0xFF3A3730),
    outline = Color(0xFF7A7566), outlineVariant = Color(0xFF3D3A33),
    error = Color(0xFFFFB4AB), onError = Color(0xFF410E0B),
    errorContainer = Color(0xFF7A1F18), onErrorContainer = Color(0xFFF9DEDA),
    inverseSurface = Color(0xFFEFE9DC), inverseOnSurface = Color(0xFF1B1A17), inversePrimary = Color(0xFFD9431F),
)

private fun text(size: Int, weight: FontWeight, line: Int, tracking: Double = 0.0) =
    TextStyle(fontFamily = Fonts.BricolageGrotesque, fontSize = size.sp, fontWeight = weight, lineHeight = line.sp, letterSpacing = tracking.em)

private val DialTypography = Typography(
    displayLarge = text(96, FontWeight.SemiBold, 100, -0.03),
    displayMedium = text(52, FontWeight.SemiBold, 56, -0.02),
    headlineMedium = text(38, FontWeight.SemiBold, 42, -0.03),
    titleLarge = text(22, FontWeight.SemiBold, 28, -0.01),
    titleMedium = text(16, FontWeight.SemiBold, 22),
    titleSmall = text(15, FontWeight.Medium, 20),
    bodyLarge = text(16, FontWeight.Normal, 24),
    bodyMedium = text(14, FontWeight.Normal, 20),
    bodySmall = text(12, FontWeight.Medium, 16),
    labelLarge = text(15, FontWeight.SemiBold, 20),
    labelMedium = text(13, FontWeight.SemiBold, 18),
    labelSmall = text(12, FontWeight.SemiBold, 16, 0.04),
)

// ---- OKLCH tones: take the theme's L and C, keep a share of the tag's own so the 32 colours stay apart ----

private fun lin(v: Float) = if (v <= 0.04045f) v / 12.92f else Math.pow((v + 0.055) / 1.055, 2.4).toFloat()
private fun gam(v: Float) = (if (v <= 0.0031308f) v * 12.92f else (1.055 * Math.pow(v.toDouble(), 1 / 2.4) - 0.055).toFloat())

private fun toOklch(c: Color): Triple<Float, Float, Float> {
    val r = lin(c.red); val g = lin(c.green); val b = lin(c.blue)
    val l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    val bigL = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    return Triple(bigL.toFloat(), Math.hypot(a, bb).toFloat(), Math.atan2(bb, a).toFloat())
}

/** Back to sRGB; an out-of-gamut chroma is reduced until the colour fits. */
private fun fromOklch(L: Float, C: Float, h: Float): Color {
    var chroma = C
    while (true) {
        val a = chroma * Math.cos(h.toDouble()); val b = chroma * Math.sin(h.toDouble())
        val l = Math.pow(L + 0.3963377774 * a + 0.2158037573 * b, 3.0)
        val m = Math.pow(L - 0.1055613458 * a - 0.0638541728 * b, 3.0)
        val s = Math.pow(L - 0.0894841775 * a - 1.2914855480 * b, 3.0)
        val r = (4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s).toFloat()
        val g = (-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s).toFloat()
        val bl = (-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s).toFloat()
        val eps = 0.0005f
        if ((r in -eps..1 + eps && g in -eps..1 + eps && bl in -eps..1 + eps) || chroma <= 0f) {
            return Color(gam(r.coerceIn(0f, 1f)), gam(g.coerceIn(0f, 1f)), gam(bl.coerceIn(0f, 1f)))
        }
        chroma -= 0.004f
    }
}

private fun mix(src: Float, target: Float, keep: Float) = target * (1 - keep) + src * keep

private fun dialTones(color: Int, @Suppress("UNUSED_PARAMETER") scheme: ColorScheme, dark: Boolean): TagTones {
    val (l, c, h) = toOklch(Color(color))
    fun tone(tl: Float, tc: Float, kl: Float, kc: Float) = fromOklch(mix(l, tl, kl), mix(c, tc, kc), h)
    return if (dark) {
        TagTones(rail = tone(0.74f, 0.15f, 0.6f, 0.65f), container = tone(0.32f, 0.06f, 0.25f, 0.25f), onContainer = tone(0.92f, 0.05f, 0.1f, 0.2f))
    } else {
        TagTones(rail = tone(0.70f, 0.16f, 0.6f, 0.65f), container = tone(0.93f, 0.05f, 0.25f, 0.25f), onContainer = tone(0.32f, 0.10f, 0.1f, 0.3f))
    }
}

// ---- Slots ----

private val Swatch = RoundedCornerShape(3.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialRow(args: TimelineRowArgs) {
    val scheme = MaterialTheme.colorScheme
    val block = args.row.block
    val ms = block.visibleEnd - block.visibleStart
    val live = block.block.isCurrent
    val tones = args.row.tag?.let { tagTones(it.color) }
    val ink = if (tones == null) scheme.onSurfaceVariant else scheme.onSurface
    val hairline = scheme.outlineVariant
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .combinedClickable(onClick = args.onClick, onLongClick = args.onLongClick)
            .drawBehind { drawLine(hairline, Offset(16.dp.toPx(), size.height - 0.5f), Offset(size.width - 16.dp.toPx(), size.height - 0.5f), 1.dp.toPx()) }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            formatTime(block.visibleStart, args.zone), Modifier.width(42.dp),
            style = MaterialTheme.typography.bodyMedium.tabular, color = scheme.onSurfaceVariant,
        )
        Box(Modifier.padding(end = 6.dp).size(8.dp, 18.dp).clip(Swatch).background(tones?.rail ?: scheme.outline))
        Column(Modifier.weight(1f).padding(start = 2.dp)) {
            Text(
                args.row.tag?.name ?: stringResource(R.string.unlabeled),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (live) FontWeight.Bold else null,
                color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (live) {
                Text(stringResource(R.string.in_progress, formatElapsed(ms, appLocale())), style = MaterialTheme.typography.labelMedium.tabular, color = scheme.primary)
            }
        }
        if (!live) Text(formatDuration(ms, appLocale()), style = MaterialTheme.typography.bodyMedium.tabular, color = ink, textAlign = TextAlign.End)
    }
}

@Composable
private fun DialMarkButton(onMark: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val gap = scheme.surface
    val coral = scheme.primary
    Box(
        Modifier
            .size(88.dp)
            // The 6dp gap and the 2dp ring are drawn outside the circle, so the layout stays 88dp.
            .drawBehind {
                drawCircle(gap, 50.dp.toPx())
                drawCircle(coral, 51.dp.toPx(), style = Stroke(2.dp.toPx()))
            }
            .clip(CircleShape)
            .background(scheme.inverseSurface)
            .clickable(role = Role.Button, onClick = onMark),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Canvas(Modifier.size(24.dp)) {
                drawCircle(scheme.inversePrimary, 9.dp.toPx(), style = Stroke(2.dp.toPx()))
                drawCircle(scheme.inversePrimary, 4.dp.toPx())
            }
            Text(stringResource(R.string.mark), style = MaterialTheme.typography.labelLarge, color = scheme.inverseOnSurface)
        }
    }
}

val DialTheme = AppTheme(
    id = "dial",
    name = "Dial",
    darkOnly = false,
    light = Light,
    dark = Dark,
    typography = DialTypography,
    shapes = Shapes(small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(12.dp), extraLarge = RoundedCornerShape(24.dp)),
    radii = Radii(card = 12.dp, chip = 10.dp, sheet = 24.dp),
    scrim = DialInk.scrim,
    tones = ::dialTones,
    timelineRow = { DialRow(it) },
    markButton = { DialMarkButton(it) },
    weekGrid = { DialWeekGrid(it) },
    dayHeader = { DialRing(it) },
)
