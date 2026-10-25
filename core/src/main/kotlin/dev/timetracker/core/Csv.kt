package dev.timetracker.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val CSV_HEADER = "start_iso,end_iso,duration_sec,tag,note,source"

/** Exports [blocks] (unclipped, in input order) as CSV; the current block has an empty `end_iso`. */
fun toCsv(blocks: List<Block>, tags: Map<String, Tag>, zone: ZoneId): String {
    fun iso(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    return buildString {
        append(CSV_HEADER).append('\n')
        for (b in blocks) {
            listOf(
                iso(b.start.at),
                b.end?.let { iso(it.at) } ?: "",
                (b.durationMs / 1000).toString(),
                b.start.tagId?.let { tags[it]?.name } ?: "",
                b.start.note ?: "",
                b.start.source.name,
            ).joinTo(this, ",") { csvField(it) }
            append('\n')
        }
    }
}

private fun csvField(s: String): String =
    if (s.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
