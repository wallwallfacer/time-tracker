package dev.timetracker.phone.ui.themes

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.timetracker.core.formatDuration
import dev.timetracker.core.formatElapsed
import dev.timetracker.core.formatTime
import dev.timetracker.phone.R
import dev.timetracker.phone.ui.AppTheme
import dev.timetracker.phone.ui.Radii
import dev.timetracker.phone.ui.RailWeekGrid
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

/** The few colours beyond the ColorScheme: the live apricot and the glass highlight. */
private object Dusk {
    val Apricot = Color(0xFFFFB98A)
    val Glint = Color.White.copy(alpha = 0.11f)
    val Sky = Color(0xFF14161F)
}

private val DuskColors = darkColorScheme(
    primary = Color(0xFFAAB4FF), onPrimary = Color(0xFF14163A),
    primaryContainer = Color(0xFF3A3F73), onPrimaryContainer = Color(0xFFE0E3FF),
    secondaryContainer = Color(0xFF2C3045), onSecondaryContainer = Color(0xFFEBE9F4),
    background = Dusk.Sky, onBackground = Color(0xFFEBE9F4),
    surface = Dusk.Sky, onSurface = Color(0xFFEBE9F4),
    surfaceVariant = Color(0xFF232634), onSurfaceVariant = Color(0xFFA2A3B9),
    surfaceContainerLowest = Color(0xFF1D202C), surfaceContainerLow = Color(0xFF1C1F2B),
    surfaceContainer = Color(0xFF232634), surfaceContainerHigh = Color(0xFF2A2E3F),
    surfaceContainerHighest = Color(0xFF323649),
    outline = Color(0xFF4A4E66), outlineVariant = Color(0xFF3A3E54),
    error = Color(0xFFFF9D93), errorContainer = Color(0xFF5A3A42), onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFEBE9F4), inverseOnSurface = Color(0xFF232634), inversePrimary = Color(0xFF4A52B0),
)

private fun wix(family: FontFamily, size: Int, weight: FontWeight, line: Int, tracking: Double = 0.0) =
    TextStyle(fontFamily = family, fontSize = size.sp, fontWeight = weight, lineHeight = line.sp, letterSpacing = tracking.em)

private val DuskTypography = Typography(
    displayLarge = wix(Fonts.WixMadeforDisplay, 96, FontWeight.Normal, 100, -0.03),
    displayMedium = wix(Fonts.WixMadeforDisplay, 52, FontWeight.Normal, 56, -0.02),
    headlineMedium = wix(Fonts.WixMadeforDisplay, 30, FontWeight.Normal, 38, -0.02),
    titleLarge = wix(Fonts.WixMadeforDisplay, 26, FontWeight.Normal, 32, -0.02),
    titleMedium = wix(Fonts.WixMadeforText, 16, FontWeight.SemiBold, 22),
    titleSmall = wix(Fonts.WixMadeforText, 15, FontWeight.SemiBold, 20),
    bodyLarge = wix(Fonts.WixMadeforText, 16, FontWeight.Normal, 24),
    bodyMedium = wix(Fonts.WixMadeforText, 14, FontWeight.Normal, 20),
    bodySmall = wix(Fonts.WixMadeforText, 12, FontWeight.Medium, 16),
    labelLarge = wix(Fonts.WixMadeforText, 15, FontWeight.SemiBold, 20),
    labelMedium = wix(Fonts.WixMadeforText, 13, FontWeight.SemiBold, 18),
    labelSmall = wix(Fonts.WixMadeforText, 12, FontWeight.SemiBold, 16, 0.06),
)

// ---- OKLCH tones ----

private class DuskLch(val l: Double, val c: Double, val h: Double)

private fun lin(v: Float) = if (v <= 0.04045f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
private fun gamma(v: Double) = if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1 / 2.4) - 0.055

private fun toLch(color: Int): DuskLch {
    val c = Color(color)
    val r = lin(c.red); val g = lin(c.green); val b = lin(c.blue)
    val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    val ol = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    return DuskLch(ol, hypot(a, bb), atan2(bb, a))
}

/** OKLCH to sRGB; chroma shrinks until the colour fits the gamut, so hue and lightness hold. */
private fun fromLch(l: Double, chroma: Double, h: Double): Color {
    var c = chroma
    while (true) {
        val a = c * cos(h); val b = c * sin(h)
        val l3 = (l + 0.3963377774 * a + 0.2158037573 * b).pow(3)
        val m3 = (l - 0.1055613458 * a - 0.0638541728 * b).pow(3)
        val s3 = (l - 0.0894841775 * a - 1.2914855480 * b).pow(3)
        val rgb = doubleArrayOf(
            4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3,
            -1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3,
            -0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3,
        )
        if (c <= 0.0 || rgb.all { it in -0.0005..1.0005 }) {
            val (r, g, bl) = rgb.map { gamma(it.coerceIn(0.0, 1.0)).toFloat() }
            return Color(r, g, bl)
        }
        c -= 0.005
    }
}

private fun mix(src: Double, target: Double, weight: Double) = src + (target - src) * weight

/** Luminous rail, deep container, pale text; each keeps part of the stored colour's lightness and chroma so hues stay apart. */
internal fun duskTones(color: Int): TagTones {
    val s = toLch(color)
    return TagTones(
        rail = fromLch(mix(s.l, 0.78, 0.3), mix(s.c, 0.12, 0.1), s.h),
        container = fromLch(mix(s.l, 0.32, 0.8), mix(s.c, 0.07, 0.8), s.h),
        onContainer = fromLch(0.90, 0.06, s.h),
    )
}

// ---- Slots ----

private val CardShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DuskRow(args: TimelineRowArgs) {
    val visible = args.row.block
    val ms = visible.visibleEnd - visible.visibleStart
    val current = visible.block.isCurrent
    val tones = args.row.tag?.let { tagTones(it.color) }
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val rail = if (current) Dusk.Apricot else tones?.rail
    val ink = if (tones == null) scheme.onSurfaceVariant else scheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .height(IntrinsicSize.Min)
            .combinedClickable(interaction, indication = null, onClick = args.onClick, onLongClick = args.onLongClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            formatTime(visible.visibleStart, args.zone),
            Modifier.width(42.dp).padding(top = 14.dp),
            style = MaterialTheme.typography.labelMedium.tabular,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
        val base = Modifier.weight(1f).fillMaxHeight()
        val card = if (tones == null) {
            base.clip(CardShape).border(1.dp, scheme.outlineVariant, CardShape)
        } else {
            base.shadow(8.dp, CardShape, clip = false).clip(CardShape).background(scheme.surfaceContainer)
        }
        Box(
            card
                .then(if (current) Modifier.border(1.5.dp, Dusk.Apricot, CardShape) else Modifier)
                .indication(interaction, ripple())
                .drawBehind {
                    if (tones != null) {
                        drawLine(Dusk.Glint, Offset(8.dp.toPx(), 0.5.dp.toPx()), Offset(size.width - 8.dp.toPx(), 0.5.dp.toPx()), 1.dp.toPx())
                    }
                    if (rail != null) {
                        drawRoundRect(rail, Offset(8.dp.toPx(), 12.dp.toPx()), Size(3.dp.toPx(), size.height - 24.dp.toPx()), CornerRadius(1.5.dp.toPx()))
                    }
                },
        ) {
            Column(Modifier.padding(start = 22.dp, end = 14.dp, top = 11.dp, bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        args.row.tag?.name ?: stringResource(R.string.unlabeled),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontStyle = if (tones == null) FontStyle.Italic else FontStyle.Normal,
                        color = ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!current) Text(formatDuration(ms), style = MaterialTheme.typography.bodyMedium.tabular, color = ink.copy(alpha = 0.8f))
                }
                if (current) LiveLine(formatElapsed(ms))
            }
        }
    }
}

@Composable
private fun LiveLine(elapsed: String) {
    val pulse by rememberInfiniteTransition(label = "dusk-live").animateFloat(
        1f, 0.45f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse",
    )
    Text(
        stringResource(R.string.in_progress, elapsed),
        Modifier.padding(top = 3.dp).alpha(pulse),
        style = MaterialTheme.typography.labelMedium.tabular,
        color = Dusk.Apricot,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DuskMarkButton(onMark: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(200.dp, 68.dp)
            .shadow(18.dp, CircleShape, ambientColor = scheme.primary, spotColor = scheme.primary) // the periwinkle glow
            .clip(CircleShape)
            .background(scheme.primary)
            .combinedClickable(onClick = onMark, role = Role.Button)
            .drawBehind {
                // Inner top highlight: a light edge that fades within the pill's upper curve.
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), 0f, 10.dp.toPx()),
                    cornerRadius = CornerRadius(size.height / 2),
                    style = Stroke(1.5.dp.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.mark), style = wix(Fonts.WixMadeforDisplay, 22, FontWeight.SemiBold, 28), color = scheme.onPrimary)
    }
}

val DuskTheme = AppTheme(
    id = "dusk",
    name = "Dusk",
    darkOnly = true,
    light = DuskColors,
    dark = DuskColors,
    typography = DuskTypography,
    shapes = Shapes(medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(16.dp), extraLarge = RoundedCornerShape(28.dp)),
    radii = Radii(card = 16.dp, chip = 20.dp, sheet = 28.dp),
    scrim = Color(0xCC05060A),
    tones = { color, _, _ -> duskTones(color) },
    timelineRow = { DuskRow(it) },
    markButton = { DuskMarkButton(it) },
    weekGrid = { RailWeekGrid(it.state, it.today, it.onOpenDay) },
)
