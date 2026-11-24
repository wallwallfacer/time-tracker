package dev.timetracker.phone.ui

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LanguageDialogTest {
    @get:Rule val rule = createComposeRule()

    @Test fun `lists system default and every language, marks the current one, and reports a pick`() {
        val picks = mutableListOf<AppLanguage?>()
        rule.setContent { LanguageDialog(AppLanguage.JAPANESE, onPick = { picks += it }, onDismiss = {}) }
        rule.onNodeWithText("日本語").assertIsSelected()
        listOf("System default", "English", "简体中文", "繁體中文", "한국어", "Bahasa Melayu").forEach { rule.onNodeWithText(it).assertExists() }
        rule.onNodeWithText("繁體中文").performClick()
        rule.onNodeWithText("System default").performClick()
        assertEquals(listOf(AppLanguage.CHINESE_TRADITIONAL, null), picks)
    }
}
