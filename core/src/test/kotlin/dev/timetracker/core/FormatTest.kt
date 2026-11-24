package dev.timetracker.core

import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun `durations`() {
        assertEquals("0s", formatDuration(0))
        assertEquals("12s", formatDuration(12_000))
        assertEquals("35m", formatDuration(35 * 60_000L))
        assertEquals("3h 10m", formatDuration(190 * 60_000L))
    }

    @Test fun `negative clamps to zero`() = assertEquals("0s", formatDuration(-5000))

    @Test fun `whole hours keep zero minutes`() = assertEquals("3h 0m", formatDuration(3 * 3_600_000L))

    @Test fun `truncates never rounds`() {
        assertEquals("59s", formatDuration(59_999))
        assertEquals("59m", formatDuration(3_599_999))
    }

    @Test fun `elapsed adds padded seconds past a minute`() {
        assertEquals("12s", formatElapsed(12_000))
        assertEquals("12m 08s", formatElapsed(728_999))
        assertEquals("1h 5m 03s", formatElapsed(3_903_000))
        assertEquals("0s", formatElapsed(-1))
    }

    private fun both(tag: String) = Locale.forLanguageTag(tag).let {
        listOf(formatDuration(42_000, it), formatDuration(35 * 60_000L, it), formatDuration(190 * 60_000L, it), formatElapsed(3_903_000, it))
    }

    @Test fun `units follow the language`() {
        assertEquals(listOf("42s", "35m", "3h 10m", "1h 5m 03s"), both("en"))
        assertEquals(listOf("42秒", "35分", "3小时10分", "1小时5分03秒"), both("zh-Hans"))
        assertEquals(listOf("42秒", "35分", "3小時10分", "1小時5分03秒"), both("zh-Hant"))
        assertEquals(listOf("42秒", "35分", "3時間10分", "1時間5分03秒"), both("ja"))
        assertEquals(listOf("42초", "35분", "3시간 10분", "1시간 5분 03초"), both("ko"))
        assertEquals(listOf("42s", "35m", "3j 10m", "1j 5m 03s"), both("ms"))
        assertEquals(both("en"), both("fr")) // no units of its own
    }

    @Test fun `chinese without a script follows the region`() {
        assertEquals("3小時10分", formatDuration(190 * 60_000L, Locale.forLanguageTag("zh-HK")))
        assertEquals("3小時10分", formatDuration(190 * 60_000L, Locale.forLanguageTag("zh-TW")))
        assertEquals("3小时10分", formatDuration(190 * 60_000L, Locale.forLanguageTag("zh-CN")))
        assertEquals("3小时10分", formatDuration(190 * 60_000L, Locale.forLanguageTag("zh-SG")))
        assertEquals("3小时10分", formatDuration(190 * 60_000L, Locale.forLanguageTag("zh")))
    }

    @Test fun `formatTime uses zone`() {
        // 2026-10-25T06:32:00Z is 14:32 in Singapore (UTC+8)
        assertEquals("14:32", formatTime(1_792_909_920_000L, ZoneId.of("Asia/Singapore")))
    }
}
