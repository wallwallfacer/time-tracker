package dev.timetracker.phone.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.timetracker.core.ClippedBlock
import dev.timetracker.core.Point
import dev.timetracker.core.Source
import dev.timetracker.core.Tag
import dev.timetracker.core.clipToDay
import dev.timetracker.core.deriveBlocks
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

    private fun shownDay(): LocalDate = pinned.value ?: dateAt(clock())
    fun previousDay() { pinned.value = shownDay().minusDays(1) }
    fun nextDay() { pinned.value = shownDay().plusDays(1) }
    fun today() { pinned.value = null }

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

    /** Re-inserts [p] under its original id, so a redelivered watch point cannot duplicate it. */
    fun undoDelete(p: Point) {
        viewModelScope.launch {
            repo.mark(p.source, p.at, p.id)
            p.tagId?.let { repo.setTag(p.id, it) }
            p.note?.let { repo.setNote(p.id, it) }
            publisher.publish(repo.newestAt())
        }
    }
}
