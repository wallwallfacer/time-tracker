package dev.timetracker.phone.ui.themes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.timetracker.core.ViewMode
import dev.timetracker.core.formatElapsed
import dev.timetracker.core.formatTime
import dev.timetracker.phone.R
import dev.timetracker.phone.ui.DayHeaderArgs
import dev.timetracker.phone.ui.Focus
import dev.timetracker.phone.ui.OverviewState
import dev.timetracker.phone.ui.RailWeekGrid
import dev.timetracker.phone.ui.WeekGridArgs
import dev.timetracker.phone.ui.appLocale
import dev.timetracker.phone.ui.dateFormat
import dev.timetracker.phone.ui.tabular
import dev.timetracker.phone.ui.tagTones
import java.time.LocalDate

private val RING = 232.dp
private val RING_STROKE = 26.dp
private const val MIN_SWEEP = 3f

/** Draws one arc per span; 00:00 is at the top and time runs clockwise. */
private fun DrawScope.arc(startFrac: Float, endFrac: Float, color: Color, stroke: Float) {
    val sweep = ((endFrac - startFrac) * 360f).coerceAtLeast(MIN_SWEEP)
    drawArc(
        color, -90f + startFrac * 360f, sweep, useCenter = false,
        topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke),
        style = Stroke(stroke, cap = StrokeCap.Butt),
    )
}

/** The hero: a 24 h ring that fills as blocks are logged, with the live block's elapsed time in the middle. */
@Composable
internal fun DialRing(args: DayHeaderArgs) {
    val scheme = MaterialTheme.colorScheme
    val state = args.state
    val dayStart = state.day.atStartOfDay(args.zone).toInstant().toEpochMilli()
    val dayLen = (state.day.plusDays(1).atStartOfDay(args.zone).toInstant().toEpochMilli() - dayStart).toFloat()
    val colors = state.rows.map { r -> r.tag?.let { tagTones(it.color).rail } ?: scheme.outline }
    val live = state.rows.lastOrNull { it.block.block.isCurrent }
    val isToday = state.day == LocalDate.now(args.zone)
    // One-second ticker; the first value is the real clock so a frozen test clock still draws the hand.
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            value = System.currentTimeMillis()
        }
    }
    val track = scheme.surfaceContainerHigh
    val ink = scheme.onSurface
    Box(Modifier.fillMaxWidth().height(248.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(RING).testTag("dial-ring")) {
            val stroke = RING_STROKE.toPx()
            drawCircle(track, (size.width - stroke) / 2, style = Stroke(stroke))
            state.rows.forEachIndexed { i, r ->
                val b = r.block
                arc((b.visibleStart - dayStart) / dayLen, (b.visibleEnd - dayStart) / dayLen, colors[i], stroke)
            }
            if (isToday) {
                val frac = ((now - dayStart) / dayLen).coerceIn(0f, 1f)
                val a = Math.toRadians(-90.0 + frac * 360.0)
                val c = Offset(size.width / 2, size.height / 2)
                val mid = (size.width - stroke) / 2
                fun at(r: Float) = Offset(c.x + r * Math.cos(a).toFloat(), c.y + r * Math.sin(a).toFloat())
                // A short hand across the ring band, ringed in the surface colour so it reads on any arc.
                drawLine(scheme.surface, at(mid - stroke / 2 - 4.dp.toPx()), at(mid + stroke / 2 + 4.dp.toPx()), 6.dp.toPx(), StrokeCap.Round)
                drawLine(ink, at(mid - stroke / 2 - 2.dp.toPx()), at(mid + stroke / 2 + 2.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
            }
        }
        val labelR = (RING.value - 2 * RING_STROKE.value) / 2 - 8
        listOf(0 to (0f to -labelR), 6 to (labelR to 0f), 12 to (0f to labelR), 18 to (-labelR to 0f)).forEach { (h, p) ->
            Text(
                h.toString(), Modifier.offset(p.first.dp, p.second.dp),
                style = MaterialTheme.typography.labelSmall.tabular, color = scheme.onSurfaceVariant,
            )
        }
        if (live != null) {
            val b = live.block
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatElapsed(b.visibleEnd - b.visibleStart, appLocale()),
                    style = MaterialTheme.typography.headlineMedium.tabular.copy(fontSize = 24.sp, lineHeight = 28.sp), color = ink, textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.since_time, formatTime(b.visibleStart, args.zone)),
                    style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Week: seven small rings per row, four columns. Month has too many days for rings, so it stays the Rail grid. */
@Composable
internal fun DialWeekGrid(args: WeekGridArgs) {
    val state = args.state
    if (state.mode == ViewMode.MONTH) {
        RailWeekGrid(state, args.today, args.onOpenDay)
        return
    }
    val rails = state.tagsById.mapValues { tagTones(it.value.color).rail }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        state.overview.days.chunked(4).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { col ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        MiniRing(col, state, rails, col.date > args.today) { args.onOpenDay(col.date) }
                    }
                }
                // Keep the last row's rings the same width as the full rows.
                repeat(4 - week.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MiniRing(
    col: dev.timetracker.core.DayColumn, state: OverviewState, rails: Map<String, Color>, future: Boolean, onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val focus = state.focus
    val size: Dp = 72.dp
    val locale = appLocale()
    val dayLabel = remember(locale) { dateFormat(locale, "EEEMMMd") }
    Column(
        Modifier.clickable(enabled = !future, onClick = onClick).semantics { contentDescription = col.date.format(dayLabel) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            col.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale),
            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
        )
        Canvas(Modifier.size(size)) {
            val stroke = 10.dp.toPx()
            if (future) {
                drawCircle(scheme.outlineVariant, (this.size.width - 1.dp.toPx()) / 2, style = Stroke(1.dp.toPx()))
                return@Canvas
            }
            drawCircle(scheme.surfaceContainerHigh, (this.size.width - stroke) / 2, style = Stroke(stroke))
            val length = (col.end - col.start).toFloat()
            for (c in col.blocks) {
                val tagId = c.block.start.tagId
                val dimmed = focus != null && focus != (if (tagId == null) Focus.Unlabeled else Focus.OnTag(tagId))
                val alpha = if (dimmed) 0.14f else 1f
                val color = (tagId?.let { rails[it] } ?: scheme.outline).copy(alpha = alpha)
                arc((c.visibleStart - col.start) / length, (c.visibleEnd - col.start) / length, color, stroke)
            }
            col.nowAt?.let {
                val a = Math.toRadians(-90.0 + (it - col.start) / length * 360.0)
                val r = (this.size.width - stroke) / 2
                val p = Offset(this.size.width / 2 + r * Math.cos(a).toFloat(), this.size.height / 2 + r * Math.sin(a).toFloat())
                drawCircle(scheme.surface, 4.5.dp.toPx(), p)
                drawCircle(scheme.primary, 3.dp.toPx(), p)
            }
        }
        Text(
            col.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelMedium.tabular, color = scheme.onSurface,
        )
    }
}
