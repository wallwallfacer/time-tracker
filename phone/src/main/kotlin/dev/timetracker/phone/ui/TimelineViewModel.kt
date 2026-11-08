package dev.timetracker.phone.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.timetracker.core.ClippedBlock
import dev.timetracker.core.Overview
import dev.timetracker.core.Point
import dev.timetracker.core.Source
import dev.timetracker.core.Tag
import dev.timetracker.core.ViewMode
import dev.timetracker.core.clipToDay
import dev.timetracker.core.deriveBlocks
import dev.timetracker.core.overview
import dev.timetracker.core.periodStart
import dev.timetracker.core.stepAnchor
import dev.timetracker.phone.data.PointRepository
import dev.timetracker.phone.sync.LatestPublisher
import dev.timetracker.phone.sync.markAndPublish
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimelineRow(val block: ClippedBlock, val tag: Tag?)

/** [isEmpty] is true when no point exists at all (not merely none on [day]). */
data class TimelineState(val day: LocalDate, val rows: List<TimelineRow>, val isEmpty: Boolean)

/** What the Week/Month grid isolates: one tag, or unlabeled time. */
sealed interface Focus {
    data class OnTag(val tagId: String) : Focus
    data object Unlabeled : Focus
}

/** One tag's time in the period; [deltaMs] vs the same span of the previous period, null when there is none. */
data class TagTotal(val tag: Tag, val ms: Long, val deltaMs: Long?)

/** The Week/Month screen. [rows] are ranked by time, ties by name; [isEmpty] is true when no point exists at all. */
data class OverviewState(
    val mode: ViewMode,
    val anchor: LocalDate,
    val isCurrent: Boolean,
    val overview: Overview,
    val rows: List<TagTotal>,
    val tagsById: Map<String, Tag>,
    val focus: Focus?,
    val isEmpty: Boolean,
)

/** The Week/Month state for [pinned] (null = follow today); [focus] comes back null when it has no time in the period. */
fun overviewState(
    points: List<Point>, tags: List<Tag>, mode: ViewMode, pinned: LocalDate?, focus: Focus?, zone: ZoneId, now: Long,
): OverviewState {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val anchor = pinned ?: today
    val o = overview(points, mode, anchor, zone, now)
    val tagsById = tags.associateBy { it.id }
    val rows = o.totals.mapNotNull { (id, ms) -> tagsById[id]?.let { TagTotal(it, ms, o.previous?.let { p -> ms - (p[id] ?: 0L) }) } }
        .sortedWith(compareByDescending<TagTotal> { it.ms }.thenBy { it.tag.name })
    val present = when (focus) {
        null -> true
        Focus.Unlabeled -> (o.totals[null] ?: 0L) > 0
        is Focus.OnTag -> rows.any { it.tag.id == focus.tagId }
    }
    val isCurrent = periodStart(mode, anchor) == periodStart(mode, today)
    return OverviewState(mode, anchor, isCurrent, o, rows, tagsById, focus.takeIf { present }, points.isEmpty())
}

class TimelineViewModel(
    private val repo: PointRepository,
    private val publisher: LatestPublisher,
    private val zone: ZoneId,
    private val clock: () -> Long,
) : ViewModel() {
    private fun dateAt(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    /** A pinned date, or null to follow today (so the timeline rolls over at midnight). */
    private val pinned = MutableStateFlow<LocalDate?>(null)
    private val ticker = flow { while (true) { emit(clock()); delay(1_000) } }
    private val _mode = MutableStateFlow(ViewMode.DAY)
    val mode: StateFlow<ViewMode> = _mode
    private val focus = MutableStateFlow<Focus?>(null)

    val state: StateFlow<TimelineState> =
        combine(repo.points, repo.tags, pinned, ticker) { points, tags, pinned, now ->
            val day = pinned ?: dateAt(now)
            val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val tagsById = tags.associateBy { it.id }
            val rows = clipToDay(deriveBlocks(points, now), start, end)
                .map { TimelineRow(it, tagsById[it.block.start.tagId]) }
            TimelineState(day, rows, points.isEmpty())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineState(dateAt(clock()), emptyList(), true))

    /** Null while the Day view is shown. Day, Week and Month share [pinned] as their anchor date. */
    val overview: StateFlow<OverviewState?> =
        combine(combine(repo.points, repo.tags, ticker, ::Triple), pinned, _mode, focus) { (points, tags, now), pinned, mode, focus ->
            if (mode == ViewMode.DAY) return@combine null
            overviewState(points, tags, mode, pinned, focus, zone, now).also {
                // A focus with no time in this period would have no row and no Clear control: drop it (spec §4.6).
                if (it.focus != focus) this.focus.value = null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun shownDay(): LocalDate = pinned.value ?: dateAt(clock())
    fun previous() { pinned.value = stepAnchor(_mode.value, shownDay(), -1) }
    fun next() { pinned.value = stepAnchor(_mode.value, shownDay(), 1) }
    fun current() { pinned.value = null }

    /** Keeps the anchor (pinned or following today) and clears any focus. */
    fun setMode(mode: ViewMode) { focus.value = null; _mode.value = mode }

    fun openDay(date: LocalDate) { pinned.value = date; setMode(ViewMode.DAY) }

    /** Tapping the focused row again clears it. */
    fun toggleFocus(f: Focus) { focus.value = if (focus.value == f) null else f }

    fun mark() {
        viewModelScope.launch { repo.markAndPublish(Source.PHONE, publisher) }
    }

    /** Deletes [point] asynchronously; the caller keeps [point] to offer undo. */
    fun deletePoint(point: Point) {
        viewModelScope.launch {
            repo.deletePoint(point.id)
            publisher.publish(repo.newestAt())
        }
    }

    /**
     * Re-inserts [p] under its original id, so a redelivered watch point cannot duplicate it. One write, so the
     * timeline never shows a half-restored point (tag back, note not yet).
     */
    fun undoDelete(p: Point) {
        viewModelScope.launch {
            repo.restore(p)
            publisher.publish(repo.newestAt())
        }
    }
}
