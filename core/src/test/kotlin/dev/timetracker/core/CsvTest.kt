package dev.timetracker.core

import org.junit.Assert.assertEquals
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
    private val breakfast = Tag("tg", "Breakfast", 0, 0)
    private val tags = mapOf("tg" to breakfast)
    private val header = "start_iso,end_iso,duration_sec,tag,note,source\n"

    @Test fun `header and one row`() {
        val csv = toCsv(listOf(Block(p("a", t("07:10")), p("b", t("07:45")), t("07:45"))), tags, zone)
        assertEquals(header + "2026-10-25T07:10:00+08:00,2026-10-25T07:45:00+08:00,2100,Breakfast,,WATCH\n", csv)
    }
    @Test fun `current block has empty end`() {
        val csv = toCsv(listOf(Block(p("a", t("07:10")), null, t("07:40"))), tags, zone)
        assertEquals(header + "2026-10-25T07:10:00+08:00,,1800,Breakfast,,WATCH\n", csv)
    }
    @Test fun `note with comma is quoted`() {
        val csv = toCsv(listOf(Block(p("a", t("07:10"), note = "a, b"), p("b", t("07:45")), t("07:45"))), tags, zone)
        assertEquals(header + "2026-10-25T07:10:00+08:00,2026-10-25T07:45:00+08:00,2100,Breakfast,\"a, b\",WATCH\n", csv)
    }
    @Test fun `note with quote is doubled`() {
        val csv = toCsv(listOf(Block(p("a", t("07:10"), note = "say \"hi\""), null, t("07:10"))), tags, zone)
        assertEquals(header + "2026-10-25T07:10:00+08:00,,0,Breakfast,\"say \"\"hi\"\"\",WATCH\n", csv)
    }
    @Test fun `unknown tag is empty`() {
        val csv = toCsv(listOf(Block(p("a", t("07:10"), tag = "gone"), null, t("07:10"))), tags, zone)
        assertEquals(header + "2026-10-25T07:10:00+08:00,,0,,,WATCH\n", csv)
    }
    @Test fun `csv_uses_real_start_and_end_for_midnight_block`() {
        val start = ZonedDateTime.of(2026, 10, 25, 23, 0, 0, 0, zone).toInstant().toEpochMilli()
        val end = ZonedDateTime.of(2026, 10, 26, 1, 0, 0, 0, zone).toInstant().toEpochMilli()
        val csv = toCsv(listOf(Block(p("a", start), p("b", end), end)), tags, zone)
        assertEquals(header + "2026-10-25T23:00:00+08:00,2026-10-26T01:00:00+08:00,7200,Breakfast,,WATCH\n", csv)
    }
}
