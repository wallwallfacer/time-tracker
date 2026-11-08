package dev.timetracker.phone.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.timetracker.core.DayColumn
import dev.timetracker.core.ViewMode
import dev.timetracker.core.formatDuration
import dev.timetracker.core.periodStart
import dev.timetracker.phone.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private val MONTH_DAY = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
private val MONTH_NAME = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
private val COLUMN_DAY = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
private const val FIVE_MINUTES = 5 * 60_000L

/** Week or Month: the rhythm grid on top, the labeled line, then tags ranked by time (spec §4). */
@Composable
fun OverviewScreen(
    state: OverviewState,
    zone: ZoneId,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCurrent: () -> Unit,
    onMode: (ViewMode) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onFocus: (Focus) -> Unit,
    onSettings: () -> Unit,
) {
    val today = LocalDate.now(zone)
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PeriodHeader(periodTitle(state.mode, state.anchor, today), state.isCurrent, onPrev, onNext, onCurrent, onSettings)
            ModeSwitch(state.mode, onMode)
            if (state.isEmpty) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Text(
                        stringResource(R.string.empty_hint),
                        Modifier.align(Alignment.Center).padding(32.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                return@Column
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp)) {
                item { RhythmGrid(state, today, onOpenDay) }
                if (state.overview.elapsedMs == 0L) {
                    item {
                        Text(
                            stringResource(R.string.nothing_recorded),
                            Modifier.fillMaxWidth().padding(top = 24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    item {
                        Box(Modifier.padding(top = 12.dp, bottom = 10.dp)) {
                            val focus = state.focus
                            if (focus == null) LabeledLine(state, onFocus, onOpenDay) else FocusBar(state, focus, onFocus)
                        }
                    }
                    itemsIndexed(state.rows, key = { _, row -> row.tag.id }) { i, row ->
                        TagRow(row, state.rows[0].ms, i == 0, i == state.rows.lastIndex, state.focus == Focus.OnTag(row.tag.id)) {
                            onFocus(Focus.OnTag(row.tag.id))
                        }
                    }
                }
            }
        }
    }
}

/** `Oct 19 – 25`, `Oct 26 – Nov 1`; `October`, or `October 2025` outside the current year. */
internal fun periodTitle(mode: ViewMode, anchor: LocalDate, today: LocalDate): String {
    val start = periodStart(mode, anchor)
    return when (mode) {
        ViewMode.WEEK -> start.plusDays(6).let { end ->
            "${start.format(MONTH_DAY)} – ${if (end.month == start.month) end.dayOfMonth else end.format(MONTH_DAY)}"
        }
        else -> start.format(MONTH_NAME) + if (start.year != today.year) " ${start.year}" else ""
    }
}

@Composable
private fun RhythmGrid(state: OverviewState, today: LocalDate, onOpenDay: (LocalDate) -> Unit) {
    val month = state.mode == ViewMode.MONTH
    val gridHeight = if (month) 196.dp else 206.dp
    val headerHeight = if (month) 18.dp else 34.dp
    val scheme = MaterialTheme.colorScheme
    val rails = state.tagsById.mapValues { tagTones(it.value.color).rail }
    val label = MaterialTheme.typography.labelSmall.tabular
    Row {
        Box(Modifier.width(22.dp).padding(top = headerHeight).height(gridHeight)) {
            listOf(0, 6, 12, 18, 24).forEach { h ->
                Text(
                    h.toString().padStart(2, '0'),
                    Modifier.align(Alignment.TopEnd).offset(y = gridHeight * (h / 24f) - 8.dp),
                    style = label, color = scheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(if (month) 3.dp else 6.dp)) {
                state.overview.days.forEach { col ->
                    val future = col.date > today
                    Column(
                        Modifier.weight(1f)
                            .clickable(enabled = !future) { onOpenDay(col.date) }
                            .semantics { contentDescription = col.date.format(COLUMN_DAY) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ColumnHeader(col.date, month, headerHeight)
                        DayRail(col, state, rails, future, gridHeight, if (month) 4.dp else 10.dp)
                    }
                }
            }
            if (month) {
                val last = state.overview.days.last().date.dayOfMonth
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    state.overview.days.forEach { col ->
                        val d = col.date
                        // Mondays, plus the first and last day unless a Monday label sits right next to them.
                        val shown = d.dayOfWeek == DayOfWeek.MONDAY ||
                            (d.dayOfMonth == 1 && d.plusDays(1).dayOfWeek != DayOfWeek.MONDAY) ||
                            (d.dayOfMonth == last && d.minusDays(1).dayOfWeek != DayOfWeek.MONDAY)
                        // Month columns are ~7dp wide: let the label overflow its column instead of clipping "16" to "1".
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (shown) {
                                Text(d.dayOfMonth.toString(), Modifier.wrapContentWidth(unbounded = true), style = label, color = scheme.onSurfaceVariant, softWrap = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnHeader(date: LocalDate, month: Boolean, height: Dp) {
    val scheme = MaterialTheme.colorScheme
    val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    val initial = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH)
    Column(Modifier.height(height).padding(bottom = 4.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
        if (!month || date.dayOfWeek == DayOfWeek.MONDAY) {
            Text(
                initial, Modifier.wrapContentWidth(unbounded = true), style = MaterialTheme.typography.labelSmall,
                color = if (weekend) scheme.primary else scheme.onSurfaceVariant, softWrap = false,
            )
        }
        if (!month) Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium.tabular)
    }
}

/** One day positioned by clock time: 00:00 at the top, the day's real end (23 h / 25 h on DST days) at the bottom. */
@Composable
private fun DayRail(col: DayColumn, state: OverviewState, rails: Map<String, Color>, future: Boolean, height: Dp, radius: Dp) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(radius)
    val focus = state.focus
    Canvas(
        Modifier.fillMaxWidth().height(height).clip(shape)
            .then(if (future) Modifier.border(1.dp, scheme.outlineVariant, shape) else Modifier.background(scheme.surfaceContainer)),
    ) {
        if (future) return@Canvas
        val length = (col.end - col.start).toFloat()
        fun y(ms: Long) = (ms - col.start) / length * size.height
        for (c in col.blocks) {
            val tagId = c.block.start.tagId
            val dimmed = focus != null && focus != (if (tagId == null) Focus.Unlabeled else Focus.OnTag(tagId))
            val alpha = if (dimmed) 0.14f else 1f
            val top = y(c.visibleStart)
            val h = maxOf(y(c.visibleEnd) - top, 1f)
            val rail = tagId?.let { rails[it] }
            if (rail != null) {
                drawRect(rail.copy(alpha = alpha), Offset(0f, top), Size(size.width, h))
            } else {
                clipRect(0f, top, size.width, top + h) { stripes(0f, top, size.width, h, scheme.outline.copy(alpha = alpha)) }
            }
            drawLine(scheme.surface, Offset(0f, top), Offset(size.width, top), 1f) // separates consecutive blocks
        }
        for (k in 1..3) {
            val y = size.height * k / 4
            drawLine(scheme.surface.copy(alpha = 0.7f), Offset(0f, y), Offset(size.width, y), 1f)
        }
        col.nowAt?.let {
            val center = Offset(size.width / 2, y(it).coerceAtMost(size.height - 5.dp.toPx()))
            drawCircle(scheme.surface, 6.dp.toPx(), center)
            drawCircle(scheme.primary, 4.dp.toPx(), center)
        }
    }
}

@Composable
private fun LabeledLine(state: OverviewState, onFocus: (Focus) -> Unit, onOpenDay: (LocalDate) -> Unit) {
    val o = state.overview
    val unlabeled = o.totals[null] ?: 0L
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = unlabeled > 0) { onFocus(Focus.Unlabeled) }
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp).height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.labeled_percent, o.labeledPercent), style = MaterialTheme.typography.titleMedium.tabular)
            if (unlabeled > 0) {
                Text(
                    stringResource(R.string.unlabeled_amount, formatDuration(unlabeled)),
                    style = MaterialTheme.typography.bodySmall.tabular, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val target = o.labelTarget
        if (unlabeled > 0 && target != null) {
            FilledTonalButton(onClick = { onOpenDay(target) }) { Text(stringResource(R.string.label_action)) }
        }
    }
}

@Composable
private fun FocusBar(state: OverviewState, focus: Focus, onFocus: (Focus) -> Unit) {
    val name = when (focus) {
        Focus.Unlabeled -> stringResource(R.string.unlabeled)
        is Focus.OnTag -> state.tagsById[focus.tagId]?.name.orEmpty()
    }
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.showing_only, name), Modifier.weight(1f).padding(start = 4.dp),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { onFocus(focus) }) { Text(stringResource(R.string.clear)) }
    }
}

@Composable
private fun TagRow(row: TagTotal, max: Long, first: Boolean, last: Boolean, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tones = tagTones(row.tag.color)
    val shape = RoundedCornerShape(if (first) 18.dp else 6.dp, if (first) 18.dp else 6.dp, if (last) 18.dp else 6.dp, if (last) 18.dp else 6.dp)
    val content = if (selected) tones.onContainer else scheme.onSurface
    Column(
        Modifier.padding(bottom = 2.dp).fillMaxWidth().clip(shape)
            .background(if (selected) tones.container else scheme.surfaceContainerLow)
            .then(if (selected) Modifier.border(2.dp, tones.rail, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(tones.rail, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(row.tag.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = content)
            Text(formatDuration(row.ms), style = MaterialTheme.typography.titleSmall.tabular, color = content)
        }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(scheme.surfaceContainerHighest)) {
                Box(Modifier.fillMaxWidth(row.ms.toFloat() / max).height(6.dp).clip(CircleShape).background(tones.rail))
            }
            Text(
                deltaText(row.deltaMs), Modifier.width(80.dp), style = MaterialTheme.typography.bodySmall.tabular,
                color = if (selected) tones.onContainer else scheme.onSurfaceVariant, textAlign = TextAlign.End,
            )
        }
    }
}

/** `▴ 20m`, `▾ 1h 5m`, `same` under five minutes, nothing when there is no previous period to compare with. */
@Composable
private fun deltaText(deltaMs: Long?): String = when {
    deltaMs == null -> ""
    abs(deltaMs) < FIVE_MINUTES -> stringResource(R.string.delta_same)
    else -> "${if (deltaMs > 0) "▴" else "▾"} ${formatDuration(abs(deltaMs))}"
}
