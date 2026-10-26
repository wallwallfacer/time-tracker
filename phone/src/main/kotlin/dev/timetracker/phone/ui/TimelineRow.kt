package dev.timetracker.phone.ui

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.timetracker.core.formatDuration
import dev.timetracker.core.formatElapsed
import dev.timetracker.core.formatTime
import dev.timetracker.phone.R
import java.time.ZoneId

/** A row's minimum height on the rail and whether its duration was cut to fit. */
data class RailSize(val height: Dp, val compressed: Boolean)

/** Height follows duration: 1.2dp per minute, at least 48dp (64dp for the live row) so it stays tappable, at most 140dp. */
fun railSize(visibleMs: Long, isCurrent: Boolean): RailSize {
    val natural = visibleMs / 50_000f // 1.2dp per minute is exactly 1dp per 50 s, so the cap boundary stays exact
    val floor = if (isCurrent) 64f else 48f
    return RailSize(natural.coerceIn(floor, 140f).dp, natural > 140f)
}

private val CardShape = RoundedCornerShape(18.dp)

/** One block on the rail: start time, a duration-sized rail segment under its point, and a tag card. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineRowItem(row: TimelineRow, zone: ZoneId, onClick: () -> Unit, onLongClick: () -> Unit) {
    val visible = row.block
    val ms = visible.visibleEnd - visible.visibleStart
    val current = visible.block.isCurrent
    val size = railSize(ms, current)
    val tones = row.tag?.let { tagTones(it.color) }
    val scheme = MaterialTheme.colorScheme
    // The whole row takes the tap; only the card shows the ripple.
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = size.height)
            .height(IntrinsicSize.Min)
            .combinedClickable(interaction, indication = null, onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            formatTime(visible.visibleStart, zone),
            Modifier.width(44.dp).padding(top = 11.dp),
            style = MaterialTheme.typography.labelMedium.tabular,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
        Rail(tones?.rail, size.compressed, current, Modifier.width(22.dp).fillMaxHeight())
        val card = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(bottom = 6.dp)
            .clip(CardShape)
            .indication(interaction, ripple())
        Column(
            when {
                tones == null -> card
                    .background(if (current) scheme.primary.copy(alpha = 0.07f) else Color.Transparent)
                    .dashedBorder(if (current) scheme.primary else scheme.outline)
                current -> card.background(tones.container).border(1.5.dp, scheme.primary, CardShape)
                else -> card.background(tones.container)
            }.padding(start = 16.dp, end = 14.dp, top = 11.dp, bottom = 10.dp),
        ) {
            val ink = tones?.onContainer ?: scheme.onSurfaceVariant
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.tag?.name ?: stringResource(R.string.unlabeled),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontStyle = if (tones == null) FontStyle.Italic else FontStyle.Normal,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!current) {
                    Text(formatDuration(ms), style = MaterialTheme.typography.bodyMedium.tabular, color = ink.copy(alpha = 0.9f))
                }
            }
            if (current) {
                LiveLine(formatElapsed(ms))
            } else if (size.compressed) {
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.compressed), style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.78f))
            } else if (tones == null && size.height >= 96.dp) {
                Spacer(Modifier.weight(1f))
                TapToLabel()
            }
        }
    }
}

@Composable
private fun LiveLine(elapsed: String) {
    val blink by rememberInfiniteTransition(label = "blink").animateFloat(
        1f, 0.25f, infiniteRepeatable(tween(500, easing = { if (it < 1f) 0f else 1f }), RepeatMode.Reverse), label = "blink",
    )
    val primary = MaterialTheme.colorScheme.primary
    Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(7.dp).drawBehind { drawCircle(primary.copy(alpha = blink)) })
        Text(stringResource(R.string.in_progress, elapsed), style = MaterialTheme.typography.labelMedium.tabular, color = primary)
    }
}

@Composable
private fun TapToLabel() {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.height(32.dp).clip(RoundedCornerShape(12.dp)).background(scheme.secondaryContainer).padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp), tint = scheme.onSecondaryContainer)
        Text(stringResource(R.string.tap_to_label), style = MaterialTheme.typography.labelMedium, color = scheme.onSecondaryContainer)
    }
}

/** The dashed outline that marks a block as "not labeled yet". */
private fun Modifier.dashedBorder(color: Color): Modifier = drawBehind {
    val inset = 0.75.dp.toPx()
    drawRoundRect(
        color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - 2 * inset, size.height - 2 * inset),
        cornerRadius = CornerRadius(18.dp.toPx() - inset),
        style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
    )
}

/**
 * The point (dot) and the segment under it. [tag] null draws a hatched, unfinished segment. The segment runs 12dp
 * past the row so it meets the next row's dot, which is drawn later and covers the join.
 */
@Composable
private fun Rail(tag: Color?, compressed: Boolean, current: Boolean, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val pulse = if (current) {
        val t = rememberInfiniteTransition(label = "live")
        val grow by t.animateFloat(0.92f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "grow")
        val ping by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "ping")
        grow to ping
    } else null
    Box(
        modifier.drawBehind {
            val cx = size.width / 2
            val w = 10.dp.toPx()
            val top = 30.dp.toPx()
            val bottom = if (current) size.height - 14.dp.toPx() else size.height + 12.dp.toPx()
            val radius = CornerRadius(w / 2)
            fun seg(from: Float, to: Float, color: Color) = drawRoundRect(color, Offset(cx - w / 2, from), Size(w, to - from), radius)

            when {
                tag == null -> hatched(cx - w / 2, top, w, bottom - top, scheme.outline)
                compressed -> {
                    val len = bottom - top
                    seg(top, top + len * 0.44f, tag)
                    seg(top + len * 0.56f, bottom, tag)
                    squiggle(cx, top + len / 2, scheme.onSurfaceVariant)
                }
                else -> seg(top, bottom, tag)
            }
            pulse?.let { (grow, ping) ->
                val tipTop = bottom - (bottom - top) * 0.7f * grow
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color.Transparent, scheme.primary), tipTop, bottom),
                    Offset(cx - w / 2, tipTop), Size(w, bottom - tipTop), radius,
                )
                drawCircle(scheme.primary.copy(alpha = 0.5f * (1 - ping)), 8.dp.toPx() + 12.dp.toPx() * ping, Offset(cx, bottom))
                drawCircle(scheme.primary, 8.dp.toPx(), Offset(cx, bottom))
            }
            // The point: an ink dot with a surface ring, drawn last so it sits on top of the previous segment.
            val dot = Offset(cx, 21.dp.toPx())
            drawCircle(scheme.surface, 10.dp.toPx(), dot)
            drawCircle(scheme.onSurface, 7.dp.toPx(), dot)
        },
    )
}

private fun DrawScope.hatched(left: Float, top: Float, w: Float, h: Float, color: Color) {
    val r = CornerRadius(w / 2)
    val outline = Path().apply { addRoundRect(RoundRect(left, top, left + w, top + h, r)) }
    clipPath(outline) {
        val step = 5.dp.toPx()
        var y = top - w
        while (y < top + h) {
            drawLine(color, Offset(left, y + w), Offset(left + w, y), strokeWidth = 2.dp.toPx())
            y += step
        }
    }
    drawRoundRect(color, Offset(left + 0.75.dp.toPx(), top + 0.75.dp.toPx()), Size(w - 1.5.dp.toPx(), h - 1.5.dp.toPx()), r, style = Stroke(1.5.dp.toPx()))
}

/** Broken-axis mark: two short waves across the gap in a compressed segment. */
private fun DrawScope.squiggle(cx: Float, cy: Float, color: Color) {
    val half = 8.dp.toPx()
    val amp = 3.5.dp.toPx()
    for (dy in listOf(-5.5.dp.toPx(), 5.5.dp.toPx())) {
        val y = cy + dy
        val p = Path().apply {
            moveTo(cx - half, y)
            quadraticTo(cx - half / 2, y - amp, cx, y)
            quadraticTo(cx + half / 2, y + amp, cx + half, y)
        }
        drawPath(p, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}
