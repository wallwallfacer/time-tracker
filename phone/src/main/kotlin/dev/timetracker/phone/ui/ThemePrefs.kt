package dev.timetracker.phone.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.themeStore: DataStore<Preferences> by preferencesDataStore("theme")

/** The chosen theme id and light/dark mode; missing or unreadable values mean Rail and the system setting. */
class ThemePrefs(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.themeStore)

    val themeId: Flow<String> = store.data.map { it[THEME] ?: RailTheme.id }
    val mode: Flow<ThemeMode> = store.data.map { p -> ThemeMode.entries.firstOrNull { it.name == p[MODE] } ?: ThemeMode.SYSTEM }

    suspend fun setTheme(id: String) { store.edit { it[THEME] = id } }
    suspend fun setMode(mode: ThemeMode) { store.edit { it[MODE] = mode.name } }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val MODE = stringPreferencesKey("mode")
    }
}

data class ThemeChoice(val theme: AppTheme, val mode: ThemeMode)

/** [initial] is read before the first frame so a non-default theme does not flash Rail at startup. */
class ThemeViewModel(private val prefs: ThemePrefs, initial: ThemeChoice) : ViewModel() {
    val choice: StateFlow<ThemeChoice> =
        combine(prefs.themeId, prefs.mode) { id, mode -> ThemeChoice(AppThemes.byId(id), mode) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    fun setTheme(id: String) { viewModelScope.launch { prefs.setTheme(id) } }
    fun setMode(mode: ThemeMode) { viewModelScope.launch { prefs.setMode(mode) } }
}
