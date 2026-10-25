package dev.timetracker.core

/** A [block] together with the part of it that is visible inside one day window. */
data class ClippedBlock(val block: Block, val visibleStart: Long, val visibleEnd: Long)

/** Keeps blocks overlapping `[dayStart, dayEnd)` and reports each one's visible span within that window. */
fun clipToDay(blocks: List<Block>, dayStart: Long, dayEnd: Long): List<ClippedBlock> =
    blocks.filter { it.start.at < dayEnd && it.endAt > dayStart }
        .map { ClippedBlock(it, maxOf(it.start.at, dayStart), minOf(it.endAt, dayEnd)) }
