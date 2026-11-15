package dev.timetracker.phone.ui

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemePrefsTest {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var file: File
    private lateinit var prefs: ThemePrefs

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        file = File.createTempFile("theme", ".preferences_pb").also { it.delete() }
        prefs = ThemePrefs(PreferenceDataStoreFactory.create(scope = scope) { file })
    }

    @After fun tearDown() {
        // Join so completion handlers run while Main is still set.
        runBlocking { scope.coroutineContext[Job]!!.cancelAndJoin() }
        file.delete()
        Dispatchers.resetMain()
    }

    @Test fun defaultsToRailAndSystem() = runTest {
        assertEquals("rail", prefs.themeId.first())
        assertEquals(ThemeMode.SYSTEM, prefs.mode.first())
    }

    @Test fun remembersTheChosenThemeAndMode() = runTest {
        prefs.setTheme("dial")
        prefs.setMode(ThemeMode.DARK)
        assertEquals("dial", prefs.themeId.first())
        assertEquals(ThemeMode.DARK, prefs.mode.first())
    }

    @Test fun anUnreadableStoredModeMeansSystem() = runTest {
        val other = File.createTempFile("theme-other", ".preferences_pb").also { it.delete() }
        val store = PreferenceDataStoreFactory.create(scope = scope) { other }
        store.edit { it[stringPreferencesKey("mode")] = "sepia" }
        assertEquals(ThemeMode.SYSTEM, ThemePrefs(store).mode.first())
    }

    @Test fun viewModelPublishesTheResolvedThemeAndPersistsChanges() = runTest {
        val vm = ThemeViewModel(prefs, ThemeChoice(RailTheme, ThemeMode.SYSTEM))
        assertEquals("rail", vm.choice.value.theme.id)
        vm.setMode(ThemeMode.LIGHT)
        vm.setTheme("not-a-theme")
        // The ViewModel writes and the state flow follows the store on other threads, so wait instead of reading at once.
        val seen = withContext(Dispatchers.Default) { withTimeout(5_000) { vm.choice.first { it.mode == ThemeMode.LIGHT } } }
        assertEquals("rail", seen.theme.id) // unknown ids resolve to Rail
        vm.viewModelScope.cancel() // an Eager state flow would otherwise outlive the test and trip later setMain calls
    }
}
