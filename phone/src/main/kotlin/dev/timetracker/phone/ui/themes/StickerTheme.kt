package dev.timetracker.phone.ui.themes

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.timetracker.core.ViewMode
import dev.timetracker.core.formatDuration
import dev.timetracker.core.formatElapsed
import dev.timetracker.core.formatTime
import dev.timetracker.phone.R
import dev.timetracker.phone.ui.appLocale
import dev.timetracker.phone.ui.AppTheme
import dev.timetracker.phone.ui.Focus
import dev.timetracker.phone.ui.Radii
import dev.timetracker.phone.ui.RailWeekGrid
import dev.timetracker.phone.ui.TagTones
import dev.timetracker.phone.ui.TimelineRowArgs
import dev.timetracker.phone.ui.WeekGridArgs
import dev.timetracker.phone.ui.dateFormat
import dev.timetracker.phone.ui.railSize
import dev.timetracker.phone.ui.stripes
import dev.timetracker.phone.ui.tabular
import dev.timetracker.phone.ui.tagTones
import java.time.format.DateTimeFormatter

/** The few colours the scheme has no role for. */
private object Sticker {
    val Paper = Color(0xFFFBF6EC)
    val Cream = Color(0xFFFFFDF8)
    val Pink = Color(0xFFFF3D8B)
    val Lime = Color(0xFFC9F23D)
    val Ink = Color(0xFF1B1613)
    val Muted = Color(0xFF6A5F55)
    val Track = Color(0xFFEFE7D6)
    val NightGround = Color(0xFF1A1612)
    val NightCard = Color(0xFF2A241D)
    val NightCardHigh = Color(0xFF362E25)
    val NightInk = Color(0xFFF4ECDD)
    val NightMuted = Color(0xFFB8AB9B)
}

private val StickerLight = lightColorScheme(
    primary = Sticker.Pink, onPrimary = Sticker.Cream,
    primaryContainer = Sticker.Lime, onPrimaryContainer = Sticker.Ink,
    secondaryContainer = Sticker.Lime, onSecondaryContainer = Sticker.Ink,
    background = Sticker.Paper, onBackground = Sticker.Ink,
    surface = Sticker.Paper, onSurface = Sticker.Ink,
    surfaceVariant = Sticker.Track, onSurfaceVariant = Sticker.Muted,
    surfaceContainerLowest = Sticker.Cream, surfaceContainerLow = Sticker.Cream,
    surfaceContainer = Sticker.Cream, surfaceContainerHigh = Sticker.Track, surfaceContainerHighest = Sticker.Track,
    outline = Sticker.Ink, outlineVariant = Color(0xFFD9CFBA),
    error = Color(0xFFC4202F), errorContainer = Color(0xFFFFD9D6), onErrorContainer = Color(0xFF410002),
    inverseSurface = Sticker.Ink, inverseOnSurface = Sticker.Cream, inversePrimary = Sticker.Pink,
)

private val StickerDark = darkColorScheme(
    primary = Sticker.Pink, onPrimary = Sticker.Cream,
    primaryContainer = Sticker.Lime, onPrimaryContainer = Sticker.Ink,
    secondaryContainer = Sticker.Lime, onSecondaryContainer = Sticker.Ink,
    background = Sticker.NightGround, onBackground = Sticker.NightInk,
    surface = Sticker.NightGround, onSurface = Sticker.NightInk,
    surfaceVariant = Sticker.NightCardHigh, onSurfaceVariant = Sticker.NightMuted,
    surfaceContainerLowest = Color(0xFF14110D), surfaceContainerLow = Sticker.NightCard,
    surfaceContainer = Sticker.NightCard, surfaceContainerHigh = Sticker.NightCardHigh, surfaceContainerHighest = Color(0xFF42392E),
    outline = Sticker.NightInk, outlineVariant = Color(0xFF4A4034),
    error = Color(0xFFFF8A8A), errorContainer = Color(0xFF7A1F27), onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Sticker.NightInk, inverseOnSurface = Sticker.Ink, inversePrimary = Sticker.Pink,
)

private fun face(family: FontFamily, size: Int, weight: Int, line: Int) =
    TextStyle(fontFamily = family, fontSize = size.sp, fontWeight = FontWeight(weight), lineHeight = line.sp)

private fun sofia(size: Int, weight: Int, line: Int) = face(Fonts.SofiaSans, size, weight, line)

// Bowlby is all caps and single weight: screen titles only, never labels.
private val StickerTypography = Typography(
    displayLarge = sofia(96, 800, 100),
    displayMedium = face(Fonts.BowlbyOneSC, 44, 400, 50),
    headlineMedium = face(Fonts.BowlbyOneSC, 28, 400, 34),
    titleLarge = face(Fonts.BowlbyOneSC, 22, 400, 28),
    titleMedium = sofia(17, 800, 22),
    titleSmall = sofia(15, 800, 20),
    bodyLarge = sofia(16, 500, 24),
    bodyMedium = sofia(14, 500, 20),
    bodySmall = sofia(12, 600, 16),
    labelLarge = sofia(15, 800, 20),
    labelMedium = sofia(13, 800, 18),
    labelSmall = sofia(12, 800, 16),
)

// ---- Tones: OKLCH, keeping hue and half of the source lightness and chroma so the 32 tags stay apart ----

private fun lin(v: Float) = if (v <= 0.04045f) v / 12.92f else Math.pow((v + 0.055) / 1.055, 2.4).toFloat()
private fun unlin(v: Float) = if (v <= 0.0031308f) v * 12.92f else (1.055 * Math.pow(v.toDouble(), 1 / 2.4) - 0.055).toFloat()

/** L, C and hue (radians) of an sRGB colour. */
private fun toOklch(c: Color): Triple<Float, Float, Float> {
    val r = lin(c.red); val g = lin(c.green); val b = lin(c.blue)
    val l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    val ll = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    return Triple(ll.toFloat(), Math.hypot(a, bb).toFloat(), Math.atan2(bb, a).toFloat())
}

/** The colour at (L, C, h); chroma is lowered until it fits sRGB, so lightness and hue hold. */
private fun fromOklch(ll: Float, chroma: Float, h: Float): Color {
    fun rgb(cc: Float): FloatArray {
        val a = cc * Math.cos(h.toDouble()); val b = cc * Math.sin(h.toDouble())
        val l = Math.pow(ll + 0.3963377774 * a + 0.2158037573 * b, 3.0)
        val m = Math.pow(ll - 0.1055613458 * a - 0.0638541728 * b, 3.0)
        val s = Math.pow(ll - 0.0894841775 * a - 1.2914855480 * b, 3.0)
        return floatArrayOf(
            (4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s).toFloat(),
            (-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s).toFloat(),
            (-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s).toFloat(),
        )
    }
    fun inGamut(v: FloatArray) = v.all { it in -0.0005f..1.0005f }
    var lo = 0f
    var hi = chroma
    if (!inGamut(rgb(chroma))) {
        repeat(20) { val mid = (lo + hi) / 2; if (inGamut(rgb(mid))) lo = mid else hi = mid }
        hi = lo
    }
    val v = rgb(hi)
    return Color(unlin(v[0].coerceIn(0f, 1f)), unlin(v[1].coerceIn(0f, 1f)), unlin(v[2].coerceIn(0f, 1f)))
}

/** Same bright stickers on paper and on the dark ground; only the page behind them changes. */
internal fun stickerTones(color: Int): TagTones {
    val (l0, c0, h) = toOklch(Color(color))
    // Rails keep more of the source so near neighbours (the two reds) stay >= 0.04 apart in OKLab.
    fun mix(target: Float, source: Float, keep: Float) = (1 - keep) * target + keep * source
    return TagTones(
        rail = fromOklch(mix(0.74f, l0, 0.7f), mix(0.16f, c0, 0.7f), h),
        container = fromOklch(mix(0.84f, l0, 0.4f), mix(0.15f, c0, 0.5f), h),
        onContainer = fromOklch(0.22f, 0.04f, h),
    )
}

// ---- Slots ----

private val CardShape = RoundedCornerShape(18.dp)

/** Ink on paper; plain black on the dark ground, where ink would vanish. */
@Composable
private fun shadowColor() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color.Black else Sticker.Ink

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StickerRow(args: TimelineRowArgs) {
    val visible = args.row.block
    val ms = visible.visibleEnd - visible.visibleStart
    val current = visible.block.isCurrent
    val labeled = args.row.tag != null
    val tones = args.row.tag?.let { tagTones(it.color) }
    val shadow = shadowColor()
    val tilt = if ((visible.visibleStart / 60_000L) % 2 == 0L) -0.6f else 0.5f
    val compressed = railSize(ms, current).compressed
    val interaction = remember { MutableInteractionSource() }

    val fill = if (current || tones == null) Sticker.Cream else tones.container
    val ink = when {
        current -> Sticker.Ink
        tones != null -> tones.onContainer
        else -> Sticker.Muted
    }
    val card = Modifier
        .graphicsLayer { rotationZ = tilt }
        .drawBehind {
            // Hard offset shadow: a solid rounded rect, no blur. Unlabeled blocks have none.
            if (labeled || current) {
                drawRoundRect(if (current) Sticker.Pink else shadow, Offset(3.dp.toPx(), 3.dp.toPx()), size, CornerRadius(18.dp.toPx()))
            }
        }
        .clip(CardShape)
        .background(fill)
        .let {
            when {
                current -> it.border(4.dp, Sticker.Pink, CardShape)
                labeled -> it.border(4.dp, Sticker.Cream, CardShape)
                else -> it.drawBehind {
                    val w = 3.dp.toPx()
                    drawRoundRect(
                        Sticker.Ink, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), CornerRadius(18.dp.toPx() - w / 2),
                        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))),
                    )
                }
            }
        }
        .indication(interaction, ripple())
        .padding(horizontal = 12.dp, vertical = 6.dp)

    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .combinedClickable(interaction, indication = null, onClick = args.onClick, onLongClick = args.onLongClick)
            .padding(start = 16.dp, end = 19.dp, top = 5.dp, bottom = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            formatTime(visible.visibleStart, args.zone), Modifier.width(42.dp),
            style = MaterialTheme.typography.labelMedium.tabular.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End,
        )
        Column(Modifier.weight(1f).then(card), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (current) {
                    LiveLabel(formatElapsed(ms, appLocale()), Modifier.weight(1f))
                } else {
                    Text(
                        args.row.tag?.name ?: stringResource(R.string.unlabeled),
                        Modifier.weight(1f),
                        style = sofia(17, if (labeled) 800 else 600, 22),
                        fontStyle = if (labeled) FontStyle.Normal else FontStyle.Italic,
                        color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(formatDuration(ms, appLocale()), style = sofia(20, 800, 24).tabular, color = ink)
                }
            }
            if (compressed) Text(stringResource(R.string.compressed), style = MaterialTheme.typography.bodySmall, color = ink)
        }
    }
}

@Composable
private fun LiveLabel(elapsed: String, modifier: Modifier) {
    val blink by rememberInfiniteTransition(label = "blink").animateFloat(
        1f, 0.25f, infiniteRepeatable(tween(500, easing = { if (it < 1f) 0f else 1f }), RepeatMode.Reverse), label = "blink",
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(9.dp).drawBehind { drawCircle(Sticker.Pink.copy(alpha = blink)) })
        Text(stringResource(R.string.in_progress, elapsed), style = sofia(17, 800, 22).tabular, color = Sticker.Pink)
    }
}

@Composable
private fun StickerMarkButton(onMark: () -> Unit) {
    val shadow = shadowColor()
    val pill = RoundedCornerShape(999.dp)
    Box(
        Modifier
            .size(220.dp, 76.dp)
            .drawBehind { drawRoundRect(shadow, Offset(4.dp.toPx(), 4.dp.toPx()), size, CornerRadius(size.height / 2)) }
            .clip(pill)
            .background(Sticker.Pink)
            .border(5.dp, Sticker.Cream, pill)
            .clickable(role = Role.Button, onClick = onMark),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.mark), style = face(Fonts.BowlbyOneSC, 28, 400, 32).copy(color = Sticker.Cream))
    }
}

@Composable
private fun StickerWeekGrid(args: WeekGridArgs) {
    val state = args.state
    if (state.mode != ViewMode.WEEK) {
        RailWeekGrid(state, args.today, args.onOpenDay)
        return
    }
    val scheme = MaterialTheme.colorScheme
    val tones = state.tagsById.mapValues { tagTones(it.value.color) }
    val shape = RoundedCornerShape(10.dp)
    val locale = appLocale()
    val columnDay = remember(locale) { dateFormat(locale, "EEEMMMd") }
    // English keeps the strip's original "Tue 24"; CLDR would give "24 Tue".
    val stripLabel = remember(locale) { if (locale.language == "en") DateTimeFormatter.ofPattern("EEE d", locale) else dateFormat(locale, "EEEd") }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        state.overview.days.forEach { col ->
            val future = col.date > args.today
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(enabled = !future) { args.onOpenDay(col.date) }
                    .semantics { contentDescription = col.date.format(columnDay) },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    col.date.format(stripLabel), Modifier.width(56.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (future) scheme.onSurfaceVariant else scheme.onSurface,
                )
                Canvas(
                    Modifier.weight(1f).height(20.dp).clip(shape)
                        .then(if (future) Modifier.border(1.5.dp, scheme.outlineVariant, shape) else Modifier.background(scheme.surfaceContainerHigh)),
                ) {
                    if (future) return@Canvas
                    val length = (col.end - col.start).toFloat()
                    fun x(ms: Long) = (ms - col.start) / length * size.width
                    for (c in col.blocks) {
                        val tagId = c.block.start.tagId
                        val focus = state.focus
                        val dimmed = focus != null && focus != (if (tagId == null) Focus.Unlabeled else Focus.OnTag(tagId))
                        val alpha = if (dimmed) 0.14f else 1f
                        val left = x(c.visibleStart)
                        val w = maxOf(x(c.visibleEnd) - left, 1f)
                        val rail = tagId?.let { tones[it]?.rail }
                        if (rail != null) {
                            drawRect(rail.copy(alpha = alpha), Offset(left, 0f), Size(w, size.height))
                        } else {
                            val box = Path().apply { addRoundRect(RoundRect(left, 0f, left + w, size.height, CornerRadius(0f))) }
                            clipPath(box) { stripes(left, 0f, w, size.height, scheme.onSurfaceVariant.copy(alpha = alpha)) }
                        }
                        drawRect(Sticker.Cream.copy(alpha = alpha), Offset(left, 0f), Size(w, size.height), style = Stroke(1.5.dp.toPx()))
                    }
                    col.nowAt?.let {
                        val center = Offset(x(it).coerceAtMost(size.width - 5.dp.toPx()), size.height / 2)
                        drawCircle(Sticker.Cream, 6.dp.toPx(), center)
                        drawCircle(Sticker.Pink, 4.dp.toPx(), center)
                    }
                }
            }
        }
    }
}

val StickerTheme = AppTheme(
    id = "sticker",
    name = "Sticker",
    darkOnly = false,
    light = StickerLight,
    dark = StickerDark,
    typography = StickerTypography,
    shapes = Shapes(
        extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp),
    ),
    radii = Radii(card = 18.dp, chip = 20.dp, sheet = 28.dp),
    scrim = Color(0xCC1B1613),
    tones = { color, _, _ -> stickerTones(color) },
    timelineRow = { StickerRow(it) },
    markButton = { StickerMarkButton(it) },
    weekGrid = { StickerWeekGrid(it) },
)
