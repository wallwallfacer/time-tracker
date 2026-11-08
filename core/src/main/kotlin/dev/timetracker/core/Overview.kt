package dev.timetracker.core

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class ViewMode { DAY, WEEK, MONTH }

/** First day of the period containing [anchor]: the anchor itself, the Monday on or before it, or the 1st. */
fun periodStart(mode: ViewMode, anchor: LocalDate): LocalDate = when (mode) {
    ViewMode.DAY -> anchor
    ViewMode.WEEK -> anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    ViewMode.MONTH -> anchor.withDayOfMonth(1)
}

/** First day after the period containing [anchor] (exclusive end). */
fun periodEnd(mode: ViewMode, anchor: LocalDate): LocalDate = stepAnchor(mode, periodStart(mode, anchor), 1)

/** Moves [anchor] by [delta] days, weeks or months; java.time clamps the day of month (Mar 31 - 1 month = Feb 28). */
fun stepAnchor(mode: ViewMode, anchor: LocalDate, delta: Int): LocalDate = when (mode) {
    ViewMode.DAY -> anchor.plusDays(delta.toLong())
    ViewMode.WEEK -> anchor.plusWeeks(delta.toLong())
    ViewMode.MONTH -> anchor.plusMonths(delta.toLong())
}

/** One calendar day of a period, `[start, end)` in epoch ms (23 h or 25 h on DST days), with its visible blocks. */
data class DayColumn(val date: LocalDate, val start: Long, val end: Long, val blocks: List<ClippedBlock>) {
    /** Now, when it falls in this day; null on earlier days the still-open current block also covers. */
    val nowAt: Long? get() = blocks.lastOrNull()?.takeIf { it.block.isCurrent && it.visibleEnd == it.block.endAt }?.visibleEnd
}

/**
 * A week or month summary. [totals] and [previous] map tagId to ms, the `null` key holding unlabeled time.
 * [previous] covers the same elapsed span of the previous period, or is null when there is nothing to compare with.
 * [labelTarget] is the day the Label action opens, or null when nothing in the period is unlabeled.
 */
data class Overview(
    val days: List<DayColumn>,
    val totals: Map<String?, Long>,
    val previous: Map<String?, Long>?,
    val elapsedMs: Long,
    val labeledPercent: Int,
    val labelTarget: LocalDate?,
)

/** Summarizes the [mode] period (WEEK or MONTH) containing [anchor], counting only `[max(start, first point), min(end, now))`. */
fun overview(points: List<Point>, mode: ViewMode, anchor: LocalDate, zone: ZoneId, now: Long): Overview {
    fun ms(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()
    val startDate = periodStart(mode, anchor)
    val endDate = periodEnd(mode, anchor)
    val blocks = deriveBlocks(points, now)
    val days = generateSequence(startDate) { it.plusDays(1) }.takeWhile { it < endDate }
        .map { d -> ms(d).let { s -> ms(d.plusDays(1)).let { e -> DayColumn(d, s, e, clipToDay(blocks, s, e)) } } }
        .toList()
    val firstAt = points.minOfOrNull { it.at } ?: return Overview(days, emptyMap(), null, 0, 100, null)

    val periodStartMs = ms(startDate)
    val periodEndMs = ms(endDate)
    val spanStart = maxOf(periodStartMs, firstAt)
    val spanEnd = minOf(periodEndMs, now)
    val elapsed = maxOf(0L, spanEnd - spanStart)
    val totals = totalsIn(blocks, spanStart, spanEnd)

    val previous = if (elapsed == 0L) null else {
        val prevStartDate = stepAnchor(mode, startDate, -1)
        val prevEnd = when {
            spanEnd >= periodEndMs -> periodStartMs // a past period compares with the whole previous one
            mode == ViewMode.MONTH -> Instant.ofEpochMilli(spanEnd).atZone(zone).let {
                // Oct 31 14:44 has no Sep 31: compare with all of September
                if (it.dayOfMonth > prevStartDate.lengthOfMonth()) periodStartMs else it.minusMonths(1).toInstant().toEpochMilli()
            }
            else -> Instant.ofEpochMilli(spanEnd).atZone(zone).minusWeeks(1).toInstant().toEpochMilli()
        }
        if (prevEnd <= firstAt) null else totalsIn(blocks, maxOf(ms(prevStartDate), firstAt), prevEnd)
    }

    val unlabeled = totals[null] ?: 0L
    val percent = if (elapsed == 0L) 100 else ((elapsed - unlabeled) * 100 / elapsed).toInt()
    val current = blocks.last()
    val labelTarget = days.firstOrNull { d -> d.blocks.any { it.block.start.tagId == null && !it.block.isCurrent } }?.date
        ?: if (current.start.tagId == null && current.start.at < spanEnd && current.endAt > spanStart) {
            Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        } else null
    return Overview(days, totals, previous, elapsed, percent, labelTarget)
}

/** Time per tagId (null = unlabeled) that [blocks] spend inside `[from, to)`. */
private fun totalsIn(blocks: List<Block>, from: Long, to: Long): Map<String?, Long> =
    blocks.filter { it.start.at < to && it.endAt > from }
        .groupBy { it.start.tagId }
        .mapValues { (_, bs) -> bs.sumOf { minOf(it.endAt, to) - maxOf(it.start.at, from) } }
