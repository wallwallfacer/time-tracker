package dev.timetracker.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

/** Unit words for one language; [gap] goes between two amounts (Chinese and Japanese write none). */
private class DurationUnits(val h: String, val m: String, val s: String, val gap: String)

private val EN = DurationUnits("h", "m", "s", " ")
private val ZH_HANS = DurationUnits("小时", "分", "秒", "")
private val ZH_HANT = DurationUnits("小時", "分", "秒", "")
private val JA = DurationUnits("時間", "分", "秒", "")
private val KO = DurationUnits("시간", "분", "초", " ")
private val MS = DurationUnits("j", "m", "s", " ") // jam, minit, saat

/** Whether [locale] is Chinese written in Traditional characters: script Hant, or no script and region TW, HK or MO. */
fun isTraditionalChinese(locale: Locale): Boolean =
    locale.language == "zh" && (locale.script == "Hant" || (locale.script.isEmpty() && locale.country in setOf("TW", "HK", "MO")))

/** Languages without their own units fall back to English. */
private fun unitsFor(locale: Locale): DurationUnits = when (locale.language) {
    "zh" -> if (isTraditionalChinese(locale)) ZH_HANT else ZH_HANS
    "ja" -> JA
    "ko" -> KO
    "ms" -> MS
    else -> EN
}

/** `12s`, `35m`, `3h 10m` (`3小时10分` in Chinese); truncates, clamps negatives to zero seconds. */
fun formatDuration(ms: Long, locale: Locale = Locale.ENGLISH): String {
    val u = unitsFor(locale)
    val totalSeconds = maxOf(ms, 0L) / 1000
    val totalMinutes = totalSeconds / 60
    return when {
        totalSeconds < 60 -> "$totalSeconds${u.s}"
        totalMinutes < 60 -> "$totalMinutes${u.m}"
        else -> "${totalMinutes / 60}${u.h}${u.gap}${totalMinutes % 60}${u.m}"
    }
}

/** A live counter: [formatDuration] plus two-digit seconds once past a minute, e.g. `12m 08s`, `1h 5m 03s`. */
fun formatElapsed(ms: Long, locale: Locale = Locale.ENGLISH): String {
    val u = unitsFor(locale)
    val totalSeconds = maxOf(ms, 0L) / 1000
    if (totalSeconds < 60) return formatDuration(ms, locale)
    return "${formatDuration(ms, locale)}${u.gap}${(totalSeconds % 60).toString().padStart(2, '0')}${u.s}"
}

fun formatTime(epochMs: Long, zone: ZoneId): String =
    TIME_FORMAT.format(Instant.ofEpochMilli(epochMs).atZone(zone))
