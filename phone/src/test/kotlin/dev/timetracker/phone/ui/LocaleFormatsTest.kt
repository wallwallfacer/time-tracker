package dev.timetracker.phone.ui

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import dev.timetracker.core.ViewMode
import dev.timetracker.phone.R
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocaleFormatsTest {
    private val today = LocalDate.of(2026, 11, 24)
    private fun title(mode: ViewMode, anchor: LocalDate, tag: String) = periodTitle(mode, anchor, today, Locale.forLanguageTag(tag))
    private fun string(id: Int) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    // ICU puts thin spaces (U+2009) around the en dash.
    @Test fun `week titles are localized ranges`() {
        assertEquals("Nov 23\u2009–\u200929", title(ViewMode.WEEK, today, "en"))
        assertEquals("Oct 26\u2009–\u2009Nov 1", title(ViewMode.WEEK, LocalDate.of(2026, 10, 28), "en"))
        assertEquals("11月23日至29日", title(ViewMode.WEEK, today, "zh-Hans"))
        assertEquals("11月23日～29日", title(ViewMode.WEEK, today, "ja"))
        assertEquals("11월 23일~29일", title(ViewMode.WEEK, today, "ko"))
        assertEquals("23–29 Nov", title(ViewMode.WEEK, today, "ms"))
    }

    @Test fun `month titles add the year outside the current one`() {
        assertEquals("November", title(ViewMode.MONTH, today, "en"))
        assertEquals("November 2025", title(ViewMode.MONTH, today.minusYears(1), "en"))
        assertEquals("2025年11月", title(ViewMode.MONTH, today.minusYears(1), "zh-Hant"))
        assertEquals("2025년 11월", title(ViewMode.MONTH, today.minusYears(1), "ko"))
    }

    @Test fun `day labels follow the language's own order`() {
        assertEquals("Tue, Nov 24", today.format(dateFormat(Locale.ENGLISH, "EEEMMMd")))
        assertEquals("11月24日周二", today.format(dateFormat(Locale.forLanguageTag("zh-Hans"), "EEEMMMd")))
        assertEquals("11月24日(火)", today.format(dateFormat(Locale.JAPANESE, "EEEMMMd")))
    }

    @Test fun `untranslated languages format like english`() {
        listOf("fr", "fa-IR", "th-TH").forEach { tag ->
            val locale = supportedOrEnglish(Locale.forLanguageTag(tag))
            assertEquals(Locale.ENGLISH, locale)
            assertEquals("Nov 23\u2009–\u200929", periodTitle(ViewMode.WEEK, today, today, locale))
        }
        assertEquals(Locale.forLanguageTag("en-GB"), supportedOrEnglish(Locale.forLanguageTag("en-GB")))
        assertEquals(Locale.forLanguageTag("zh-TW"), supportedOrEnglish(Locale.forLanguageTag("zh-TW")))
    }

    @Test fun `ranges stay gregorian whatever the locale's calendar`() {
        val range = dateRange(Locale.forLanguageTag("th-TH"), "yMMMd", today, today.plusDays(2))
        assert("2026" in range) { range }
    }

    @Test @Config(qualifiers = "zh-rHK") fun `hong kong chinese gets traditional strings`() = assertEquals("設定", string(R.string.settings))
    @Test @Config(qualifiers = "zh-rTW") fun `taiwan chinese gets traditional strings`() = assertEquals("設定", string(R.string.settings))
    @Test @Config(qualifiers = "zh-rCN") fun `mainland chinese gets simplified strings`() = assertEquals("设置", string(R.string.settings))
    @Test @Config(qualifiers = "zh-rSG") fun `singapore chinese gets simplified strings`() = assertEquals("设置", string(R.string.settings))
    @Test @Config(qualifiers = "ms-rMY") fun `malay strings`() = assertEquals("Tetapan", string(R.string.settings))
    @Test @Config(qualifiers = "fr") fun `untranslated languages fall back to english`() = assertEquals("Settings", string(R.string.settings))
}
