package dev.timetracker.phone.ui

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    private fun of(tag: String) = AppLanguage.of(Locale.forLanguageTag(tag))

    @Test fun `chinese matches by script, inferring it from the region`() {
        assertEquals(AppLanguage.CHINESE_SIMPLIFIED, of("zh-Hans"))
        assertEquals(AppLanguage.CHINESE_SIMPLIFIED, of("zh-CN"))
        assertEquals(AppLanguage.CHINESE_SIMPLIFIED, of("zh-SG"))
        assertEquals(AppLanguage.CHINESE_TRADITIONAL, of("zh-Hant"))
        assertEquals(AppLanguage.CHINESE_TRADITIONAL, of("zh-TW"))
        assertEquals(AppLanguage.CHINESE_TRADITIONAL, of("zh-HK"))
        assertEquals(AppLanguage.CHINESE_TRADITIONAL, of("zh-Hant-HK"))
    }

    @Test fun `other languages match by language, any region`() {
        assertEquals(AppLanguage.ENGLISH, of("en-GB"))
        assertEquals(AppLanguage.JAPANESE, of("ja-JP"))
        assertEquals(AppLanguage.KOREAN, of("ko"))
        assertEquals(AppLanguage.MALAY, of("ms-MY"))
        assertEquals(null, of("fr"))
    }

    @Test fun `every tag round-trips`() {
        AppLanguage.entries.forEach { assertEquals(it, of(it.tag)) }
    }
}
