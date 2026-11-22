package dev.timetracker.core

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The first six columns read well in a spreadsheet; the rest carry what a restore needs to rebuild every
 * point and tag (ids, creation times, tag order). Tag colors are left out: they are a phone setting, so a
 * restore keeps the phone's colors and gives new tags palette colors.
 */
private val COLUMNS = listOf(
    "start_iso", "end_iso", "duration_sec", "tag", "note", "source",
    "point_id", "created_at_ms", "tag_id", "tag_last_used_ms",
)

/** A tag as an export holds it: everything but its color. */
data class SavedTag(val id: String, val name: String, val lastUsedAt: Long)

/** Everything an export holds: enough to restore the app's data. */
data class Snapshot(val points: List<Point>, val tags: List<SavedTag>)

/**
 * Exports [blocks] (unclipped, in input order) as CSV, one row per block; the current block has an empty
 * `end_iso`. Tags no block uses follow as rows with only the tag columns filled, so a restore keeps them.
 */
fun toCsv(blocks: List<Block>, tags: Map<String, Tag>, zone: ZoneId): String {
    fun iso(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    fun tagColumns(tag: Tag?) =
        if (tag == null) listOf("", "") else listOf(tag.id, tag.lastUsedAt.toString())
    val rows = blocks.map { b ->
        val tag = b.start.tagId?.let { tags[it] }
        listOf(
            iso(b.start.at),
            b.end?.let { iso(it.at) } ?: "",
            (b.durationMs / 1000).toString(),
            tag?.name ?: "",
            b.start.note ?: "",
            b.start.source.name,
            b.start.id,
            b.start.createdAt.toString(),
        ) + tagColumns(tag)
    }
    val used = blocks.mapNotNullTo(HashSet()) { it.start.tagId }
    val unusedTags = tags.values.filter { it.id !in used }.map { tag -> listOf("", "", "", tag.name, "", "", "", "") + tagColumns(tag) }
    return buildString {
        COLUMNS.joinTo(this, ",").append('\n')
        for (row in rows + unusedTags) row.joinTo(this, ",") { csvField(it) }.append('\n')
    }
}

/**
 * Reads a [toCsv] export back; null when the text is not one (an older export without ids included) or is malformed.
 * Columns it does not know, such as the `tag_color` of earlier exports, are ignored.
 */
fun fromCsv(text: String): Snapshot? {
    val records = parseCsv(text)
    val header = records.firstOrNull() ?: return null
    val index = COLUMNS.associateWith { header.indexOf(it) }
    if (index.values.any { it < 0 }) return null
    return try {
        val points = mutableListOf<Point>()
        val tags = LinkedHashMap<String, SavedTag>()
        for (record in records.drop(1)) {
            if (record.size == 1 && record[0].isEmpty()) continue // a blank line
            fun col(name: String) = record.getOrElse(index.getValue(name)) { "" }
            val tagId = col("tag_id").ifEmpty { null }
            if (tagId != null && tagId !in tags) {
                tags[tagId] = SavedTag(tagId, col("tag"), col("tag_last_used_ms").toLong())
            }
            val start = col("start_iso").ifEmpty { null } ?: continue // a tag-only row
            points += Point(
                id = col("point_id").ifEmpty { return null },
                at = OffsetDateTime.parse(start).toInstant().toEpochMilli(),
                source = Source.valueOf(col("source")),
                tagId = tagId,
                note = col("note").ifEmpty { null },
                createdAt = col("created_at_ms").toLong(),
            )
        }
        Snapshot(points, tags.values.toList())
    } catch (e: RuntimeException) {
        null // a bad number, time or source
    }
}

private fun csvField(s: String): String =
    if (s.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

/** RFC 4180 records: quoted fields may hold commas, doubled quotes and line breaks. */
private fun parseCsv(text: String): List<List<String>> {
    val records = mutableListOf<List<String>>()
    var record = mutableListOf<String>()
    val field = StringBuilder()
    var quoted = false
    var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            quoted && c == '"' && text.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
            quoted && c == '"' -> quoted = false
            quoted -> field.append(c)
            c == '"' -> quoted = true
            c == ',' -> { record.add(field.toString()); field.clear() }
            c == '\n' || c == '\r' -> {
                if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                record.add(field.toString()); field.clear()
                records.add(record); record = mutableListOf()
            }
            else -> field.append(c)
        }
        i++
    }
    if (field.isNotEmpty() || record.isNotEmpty()) { record.add(field.toString()); records.add(record) }
    return records
}
