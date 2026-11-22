package dev.timetracker.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class CsvTest {
    private val zone = ZoneId.of("Asia/Singapore")
    private fun t(hhmm: String): Long {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        return ZonedDateTime.of(2026, 10, 25, h, m, 0, 0, zone).toInstant().toEpochMilli()
    }
    private fun p(id: String, at: Long, tag: String? = "tg", note: String? = null) =
        Point(id, at, Source.WATCH, tag, note, at)
    private val breakfast = Tag("tg", "Breakfast", 0xFF64B5F6.toInt(), 7)
    private val tags = mapOf("tg" to breakfast)
    private val header = "start_iso,end_iso,duration_sec,tag,note,source,point_id,created_at_ms,tag_id,tag_last_used_ms\n"
    private fun ids(id: String, at: Long) = "$id,$at,tg,7"

    @Test fun `header and one row`() {
        val csv = toCsv(listOf(Block(p("a", t("07:10")), p("b", t("07:45")), t("07:45"))), tags, zone)
        assertEquals(header + "2026-10-25T07:10:00+08:00,2026-10-25T07:45:00+08:00,2100,Breakfast,,WATCH,${ids("a", t("07:10"))}\n", csv)
    }
    @Test fun `current block has empty end`() {
        val csv = toCsv(listOf(Block(p("a", t("07:10")), null, t("07:40"))), tags, zone)
        assertEquals(header + "2026-10-25T07:10:00+08:00,,1800,Breakfast,,WATCH,${ids("a", t("07:10"))}\n", csv)
    }
    @Test fun `note with comma is quoted`() {
        val csv = toCsv(listOf(Block(p("a", t("07:10"), note = "a, b"), p("b", t("07:45")), t("07:45"))), tags, zone)
        assertEquals(header + "2026-10-25T07:10:00+08:00,2026-10-25T07:45:00+08:00,2100,Breakfast,\"a, b\",WATCH,${ids("a", t("07:10"))}\n", csv)
    }
    @Test fun `note with quote is doubled`() {
        val csv = toCsv(listOf(Block(p("a", t("07:10"), note = "say \"hi\""), null, t("07:10"))), tags, zone)
        assertEquals(header + "2026-10-25T07:10:00+08:00,,0,Breakfast,\"say \"\"hi\"\"\",WATCH,${ids("a", t("07:10"))}\n", csv)
    }
    @Test fun `unknown tag is empty`() {
        val csv = toCsv(listOf(Block(p("a", t("07:10"), tag = "gone"), null, t("07:10"))), emptyMap(), zone)
        assertEquals(header + "2026-10-25T07:10:00+08:00,,0,,,WATCH,a,${t("07:10")},,\n", csv)
    }
    @Test fun `unused tag gets a tag-only row`() {
        val gym = Tag("gy", "Gym", 0xFF81C784.toInt(), 3)
        val csv = toCsv(listOf(Block(p("a", t("07:10")), null, t("07:10"))), tags + ("gy" to gym), zone)
        assertEquals(",,,Gym,,,,,gy,3", csv.trimEnd('\n').lines().last())
    }
    @Test fun `csv_uses_real_start_and_end_for_midnight_block`() {
        val start = ZonedDateTime.of(2026, 10, 25, 23, 0, 0, 0, zone).toInstant().toEpochMilli()
        val end = ZonedDateTime.of(2026, 10, 26, 1, 0, 0, 0, zone).toInstant().toEpochMilli()
        val csv = toCsv(listOf(Block(p("a", start), p("b", end), end)), tags, zone)
        assertEquals(header + "2026-10-25T23:00:00+08:00,2026-10-26T01:00:00+08:00,7200,Breakfast,,WATCH,${ids("a", start)}\n", csv)
    }

    @Test fun `fromCsv restores every point and tag exactly`() {
        val gym = Tag("gy", "Gym, \"daily\"", 0xFF81C784.toInt(), 3)
        val points = listOf(
            Point("a", t("07:10") + 459, Source.WATCH, "tg", "a, \"b\"\nc", 11),
            Point("b", t("07:45"), Source.MANUAL, null, null, 22),
            Point("c", t("09:00") + 1, Source.PHONE, "tg", null, 33), // current block
        )
        val all = mapOf("tg" to breakfast, "gy" to gym)
        val back = fromCsv(toCsv(deriveBlocks(points, t("10:00")), all, zone))!!
        assertEquals(points, back.points)
        assertEquals(setOf(SavedTag("tg", "Breakfast", 7), SavedTag("gy", "Gym, \"daily\"", 3)), back.tags.toSet())
    }
    @Test fun `fromCsv reads CRLF line endings`() {
        val csv = toCsv(listOf(Block(p("a", t("07:10")), null, t("07:10"))), tags, zone).replace("\n", "\r\n")
        assertEquals(listOf(p("a", t("07:10"))), fromCsv(csv)!!.points)
    }
    @Test fun `fromCsv reads an earlier export with tag_color and ignores it`() {
        val old = "start_iso,end_iso,duration_sec,tag,note,source,point_id,created_at_ms,tag_id,tag_color,tag_last_used_ms\n" +
            "2026-10-25T07:10:00+08:00,,0,Breakfast,,WATCH,a,${t("07:10")},tg,#FF64B5F6,7\n"
        val back = fromCsv(old)!!
        assertEquals(listOf(p("a", t("07:10"))), back.points)
        assertEquals(listOf(SavedTag("tg", "Breakfast", 7)), back.tags)
    }
    @Test fun `fromCsv rejects an export without ids`() {
        assertNull(fromCsv("start_iso,end_iso,duration_sec,tag,note,source\n2026-10-25T07:10:00+08:00,,0,,,WATCH\n"))
    }
    @Test fun `fromCsv rejects malformed rows and other files`() {
        assertNull(fromCsv(header + "not-a-time,,0,,,WATCH,a,1,,\n"))
        assertNull(fromCsv(header + "2026-10-25T07:10:00+08:00,,0,,,NOPE,a,1,,\n"))
        assertNull(fromCsv(""))
        assertNull(fromCsv("hello world"))
    }
}
