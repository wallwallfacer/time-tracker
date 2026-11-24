package dev.timetracker.phone.ui.themes

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.timetracker.core.formatDuration
import dev.timetracker.core.formatElapsed
import dev.timetracker.core.formatTime
import dev.timetracker.phone.R
import dev.timetracker.phone.ui.appLocale
import dev.timetracker.phone.ui.AppTheme
import dev.timetracker.phone.ui.RailWeekGrid
import dev.timetracker.phone.ui.Radii
import dev.timetracker.phone.ui.TagTones
import dev.timetracker.phone.ui.TimelineRowArgs
import dev.timetracker.phone.ui.tabular
import dev.timetracker.phone.ui.tagTones
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/** Pharmacy register: paper ground, tear-off labels with a tag-coloured edge, one pharmacy green, mono times. */
private object Dose {
    val line = Color(0xFFC9C6B4)
    val lineDark = Color(0xFF34443A)
    val card = Color(0xFFFBFAF4)
    val cardDark = Color(0xFF16211A)
    val cardShape = RoundedCornerShape(10.dp)
    val markShape = RoundedCornerShape(14.dp)
}

private fun text(family: FontFamily, size: Int, weight: FontWeight, line: Int = size + 6, tracking: Double = 0.0) =
    TextStyle(fontFamily = family, fontSize = size.sp, fontWeight = weight, lineHeight = line.sp, letterSpacing = tracking.sp)

private val DoseTypography = Typography(
    displayLarge = text(Fonts.SchibstedGrotesk, 96, FontWeight.Bold, 100, -2.0),
    displayMedium = text(Fonts.SchibstedGrotesk, 52, FontWeight.Bold, 56, -1.0),
    headlineMedium = text(Fonts.SchibstedGrotesk, 30, FontWeight.Bold, 38),
    titleLarge = text(Fonts.SchibstedGrotesk, 22, FontWeight.Bold, 28),
    titleMedium = text(Fonts.SchibstedGrotesk, 16, FontWeight.Bold, 22),
    titleSmall = text(Fonts.InstrumentSans, 15, FontWeight.SemiBold, 20),
    bodyLarge = text(Fonts.InstrumentSans, 16, FontWeight.Normal, 24),
    bodyMedium = text(Fonts.InstrumentSans, 14, FontWeight.Normal, 20),
    bodySmall = text(Fonts.InstrumentSans, 12, FontWeight.Medium, 16),
    labelLarge = text(Fonts.InstrumentSans, 15, FontWeight.SemiBold, 20),
    labelMedium = text(Fonts.InstrumentSans, 13, FontWeight.SemiBold, 18),
    labelSmall = text(Fonts.InstrumentSans, 12, FontWeight.SemiBold, 16, 0.6),
)

private val DoseLight = lightColorScheme(
    primary = Color(0xFF1F7A4C), onPrimary = Color(0xFFF4FBF6),
    primaryContainer = Color(0xFFCDE6D6), onPrimaryContainer = Color(0xFF0C3A24),
    secondaryContainer = Color(0xFFE3DFCC), onSecondaryContainer = Color(0xFF14231A),
    background = Color(0xFFEFEBDD), onBackground = Color(0xFF14231A),
    surface = Color(0xFFEFEBDD), onSurface = Color(0xFF14231A),
    surfaceVariant = Color(0xFFE3DFCC), onSurfaceVariant = Color(0xFF59665C),
    surfaceContainerLowest = Dose.card, surfaceContainerLow = Color(0xFFF7F4E8),
    surfaceContainer = Color(0xFFE3DFCC), surfaceContainerHigh = Color(0xFFDDD9C5),
    surfaceContainerHighest = Color(0xFFD6D2BD),
    outline = Dose.line, outlineVariant = Color(0xFFDAD6C3),
    error = Color(0xFFA13A22), errorContainer = Color(0xFFF3D5CB), onErrorContainer = Color(0xFF4A1408),
    inverseSurface = Color(0xFF14231A), inverseOnSurface = Color(0xFFEFEBDD), inversePrimary = Color(0xFF5CC38C),
)

private val DoseDark = darkColorScheme(
    primary = Color(0xFF52C28A), onPrimary = Color(0xFF06241A),
    primaryContainer = Color(0xFF1D4A34), onPrimaryContainer = Color(0xFFC4EBD3),
    secondaryContainer = Color(0xFF263229), onSecondaryContainer = Color(0xFFDCE8DF),
    background = Color(0xFF0E1511), onBackground = Color(0xFFE3EBE4),
    surface = Color(0xFF0E1511), onSurface = Color(0xFFE3EBE4),
    surfaceVariant = Color(0xFF263229), onSurfaceVariant = Color(0xFF9DAFA2),
    surfaceContainerLowest = Dose.cardDark, surfaceContainerLow = Color(0xFF1A261F),
    surfaceContainer = Color(0xFF212E26), surfaceContainerHigh = Color(0xFF2A382F),
    surfaceContainerHighest = Color(0xFF334338),
    outline = Color(0xFF3F5045), outlineVariant = Dose.lineDark,
    error = Color(0xFFFFB4A2), errorContainer = Color(0xFF7A2A16), onErrorContainer = Color(0xFFFFDAD0),
    inverseSurface = Color(0xFFE3EBE4), inverseOnSurface = Color(0xFF0E1511), inversePrimary = Color(0xFF1F7A4C),
)

// ---- OKLCH tones ----

private class Lch(val l: Double, val c: Double, val h: Double)

private fun lin(v: Float) = if (v <= 0.04045f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
private fun gamma(v: Double) = if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1 / 2.4) - 0.055

private fun toLch(color: Color): Lch {
    val r = lin(color.red); val g = lin(color.green); val b = lin(color.blue)
    val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    val ol = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    val oa = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    val ob = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    return Lch(ol, hypot(oa, ob), atan2(ob, oa))
}

/** OKLCH to sRGB; chroma is lowered until the colour fits the gamut, so hue and lightness hold. */
private fun fromLch(l: Double, c: Double, h: Double): Color {
    var chroma = c
    while (true) {
        val a = chroma * cos(h); val b = chroma * sin(h)
        val l_ = (l + 0.3963377774 * a + 0.2158037573 * b).pow(3)
        val m_ = (l - 0.1055613458 * a - 0.0638541728 * b).pow(3)
        val s_ = (l - 0.0894841775 * a - 1.2914855480 * b).pow(3)
        val r = 4.0767416621 * l_ - 3.3077115913 * m_ + 0.2309699292 * s_
        val g = -1.2684380046 * l_ + 2.6097574011 * m_ - 0.3413193965 * s_
        val bl = -0.0041960863 * l_ - 0.7034186147 * m_ + 1.7076147010 * s_
        val inGamut = listOf(r, g, bl).all { it in -0.0005..1.0005 }
        if (inGamut || chroma < 0.002) {
            return Color(gamma(r.coerceIn(0.0, 1.0)).toFloat(), gamma(g.coerceIn(0.0, 1.0)).toFloat(), gamma(bl.coerceIn(0.0, 1.0)).toFloat())
        }
        chroma -= 0.004
    }
}

/** Keeps hue and part of the source lightness and chroma, so the 32 palette colours stay apart. */
private fun doseTones(color: Int, @Suppress("UNUSED_PARAMETER") scheme: ColorScheme, dark: Boolean): TagTones {
    val src = toLch(Color(color))
    fun mix(target: Double, own: Double, keep: Double) = target + (own - target) * keep
    return if (dark) TagTones(
        rail = fromLch(mix(0.74, src.l, 0.6), mix(0.12, src.c, 0.7), src.h),
        container = fromLch(mix(0.30, src.l, 0.1), mix(0.05, src.c, 0.15), src.h),
        onContainer = fromLch(0.92, 0.04, src.h),
    ) else TagTones(
        rail = fromLch(mix(0.72, src.l, 0.6), mix(0.11, src.c, 0.7), src.h),
        container = fromLch(mix(0.92, src.l, 0.1), mix(0.05, src.c, 0.15), src.h),
        onContainer = fromLch(0.30, 0.08, src.h),
    )
}

// ---- Slots ----

/** A label: time on the left, a card with a tag-coloured edge. Unlabeled is a dashed ghost, live gets a green outline. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DoseRow(args: TimelineRowArgs) {
    val row = args.row
    val visible = row.block
    val ms = visible.visibleEnd - visible.visibleStart
    val current = visible.block.isCurrent
    val tones = row.tag?.let { tagTones(it.color) }
    val scheme = MaterialTheme.colorScheme
    val pulse = if (current) {
        val a by rememberInfiniteTransition(label = "dose-live").animateFloat(1f, 0.75f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alpha")
        a
    } else 1f
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = args.onClick, onLongClick = args.onLongClick, role = Role.Button)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            formatTime(visible.visibleStart, args.zone),
            Modifier.width(42.dp),
            style = MonoStyle(13).copy(color = scheme.onSurfaceVariant),
            textAlign = TextAlign.End,
        )
        val base = Modifier.weight(1f).heightIn(min = 52.dp).alpha(pulse).clip(Dose.cardShape)
        val body = when {
            tones == null && !current -> base.drawBehind {
                val inset = 0.75.dp.toPx()
                drawRoundRect(
                    scheme.outline,
                    Offset(inset, inset), Size(size.width - 2 * inset, size.height - 2 * inset), CornerRadius(10.dp.toPx() - inset),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
            else -> base.background(scheme.surfaceContainerLowest)
                .border(if (current) 2.dp else 1.dp, if (current) scheme.primary else scheme.outline, Dose.cardShape)
                .drawBehind { drawRect(tones?.rail ?: scheme.primary, size = Size(5.dp.toPx(), size.height)) }
        }
        Column(body.padding(start = if (tones == null && !current) 14.dp else 19.dp, end = 14.dp, top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.tag?.name ?: stringResource(R.string.unlabeled),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (tones == null) scheme.onSurfaceVariant else scheme.onSurface,
                    fontWeight = if (tones == null) FontWeight.Medium else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!current) Text(formatDuration(ms, appLocale()), style = MonoStyle(13).copy(color = scheme.onSurfaceVariant))
            }
            if (current) {
                Text(stringResource(R.string.in_progress, formatElapsed(ms, appLocale())), style = MonoStyle(12).copy(color = scheme.primary))
            }
        }
    }
}

private fun MonoStyle(size: Int) = text(Fonts.DMMono, size, FontWeight.Normal, size + 5).tabular

/** Full-width register key: green with a top glint and a darker bottom edge. */
@Composable
private fun DoseMarkButton(onMark: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val width = (LocalConfiguration.current.screenWidthDp - 32).dp
    Row(
        Modifier
            .width(width)
            .heightIn(min = 64.dp)
            .clip(Dose.markShape)
            .clickable(role = Role.Button, onClick = onMark)
            .drawBehind {
                drawRect(scheme.primary)
                drawRect(Color.White.copy(alpha = 0.22f), size = Size(size.width, 1.dp.toPx()))
                drawRect(lerp(scheme.primary, Color.Black, 0.3f), Offset(0f, size.height - 3.dp.toPx()), Size(size.width, 3.dp.toPx()))
            }
            .padding(bottom = 3.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(26.dp).drawBehind {
                drawCircle(scheme.onPrimary.copy(alpha = 0.25f))
                drawCircle(scheme.onPrimary, 9.dp.toPx())
            },
        )
        Spacer(Modifier.size(12.dp))
        Text(stringResource(R.string.mark), style = text(Fonts.SchibstedGrotesk, 22, FontWeight.Bold, 28), color = scheme.onPrimary)
    }
}

val DoseTheme = AppTheme(
    id = "dose",
    name = "Dose",
    darkOnly = false,
    light = DoseLight,
    dark = DoseDark,
    typography = DoseTypography,
    shapes = Shapes(
        extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(10.dp),
        large = RoundedCornerShape(14.dp), extraLarge = RoundedCornerShape(14.dp),
    ),
    radii = Radii(card = 10.dp, chip = 8.dp, sheet = 14.dp),
    scrim = Color(0xCC0A110D),
    tones = ::doseTones,
    timelineRow = { DoseRow(it) },
    markButton = { DoseMarkButton(it) },
    weekGrid = { RailWeekGrid(it.state, it.today, it.onOpenDay) },
)
