package dev.timetracker.phone.ui

import android.icu.text.DateIntervalFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.text.FieldPosition
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The language the UI is shown in: the in-app choice, or the system's when there is none.
 * Read it from the configuration, not Locale.getDefault(), which below Android 13 does not follow the in-app choice.
 */
@Composable
@ReadOnlyComposable
fun appLocale(): Locale = LocalConfiguration.current.locales[0]

/** A formatter for a CLDR [skeleton] (`MMMd`, `EEEMMMd`), in [locale]'s own field order and words: `Nov 24`, `11月24日`. */
fun dateFormat(locale: Locale, skeleton: String): DateTimeFormatter =
    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)

/** The inclusive day range [from]..[to] for a CLDR [skeleton]: `Oct 19 – 25`, `Oct 26 – Nov 1`, `10月19日至25日`. */
fun dateRange(locale: Locale, skeleton: String, from: LocalDate, to: LocalDate): String {
    val utc = TimeZone.GMT_ZONE
    fun cal(d: LocalDate) = Calendar.getInstance(utc, ULocale.forLocale(locale)).apply {
        clear()
        set(d.year, d.monthValue - 1, d.dayOfMonth)
    }
    val format = DateIntervalFormat.getInstance(skeleton, locale).apply { timeZone = utc }
    return format.format(cal(from), cal(to), StringBuffer(), FieldPosition(0)).toString()
}
