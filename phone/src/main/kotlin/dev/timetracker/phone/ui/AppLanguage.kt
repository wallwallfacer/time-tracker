package dev.timetracker.phone.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dev.timetracker.core.isTraditionalChinese
import java.util.Locale

/** A language the app is translated into, named in itself so anyone can find their own. */
enum class AppLanguage(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"),
    CHINESE_SIMPLIFIED("zh-Hans", "简体中文"),
    CHINESE_TRADITIONAL("zh-Hant", "繁體中文"),
    JAPANESE("ja", "日本語"),
    KOREAN("ko", "한국어"),
    MALAY("ms", "Bahasa Melayu");

    companion object {
        /** The entry [locale] picks; Chinese by script, since Android 13 settings can store a region tag such as zh-TW. */
        fun of(locale: Locale): AppLanguage? = when (locale.language) {
            "zh" -> if (isTraditionalChinese(locale)) CHINESE_TRADITIONAL else CHINESE_SIMPLIFIED
            else -> entries.firstOrNull { it.tag == locale.language }
        }

        /** The language chosen in the app (or, on Android 13+, in system settings); null follows the system. */
        fun chosen(): AppLanguage? = AppCompatDelegate.getApplicationLocales()[0]?.let(::of)

        /** Applies [language] app-wide and recreates the activities; null goes back to the system language. */
        fun choose(language: AppLanguage?) = AppCompatDelegate.setApplicationLocales(
            if (language == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(language.tag),
        )
    }
}
