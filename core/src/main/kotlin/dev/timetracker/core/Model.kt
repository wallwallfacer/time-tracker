package dev.timetracker.core

enum class Source { WATCH, PHONE, MANUAL }

/** A recorded timestamp. A block runs from one point to the next. */
data class Point(
    val id: String,
    val at: Long,
    val source: Source,
    val tagId: String? = null,
    val note: String? = null,
    val createdAt: Long,
)

data class Tag(val id: String, val name: String, val color: Int, val lastUsedAt: Long)

/** The span from [start] to [end]; [end] is null for the current block, which ends at [endAt]. */
data class Block(val start: Point, val end: Point?, val endAt: Long) {
    val durationMs: Long get() = endAt - start.at
    val isCurrent: Boolean get() = end == null
}
