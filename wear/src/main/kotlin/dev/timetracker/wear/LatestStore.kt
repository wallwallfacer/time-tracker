package dev.timetracker.wear

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "latest")

/** The only value the watch persists: the newest point time it knows of, to compute "previous block". */
class LatestStore(context: Context) {
    private val store = context.applicationContext.dataStore

    suspend fun get(): Long? = store.data.first()[KEY]

    /** Writes only when [at] is newer than the stored value (or nothing is stored). */
    suspend fun setIfNewer(at: Long) {
        store.edit { prefs ->
            val current = prefs[KEY]
            if (current == null || at > current) prefs[KEY] = at
        }
    }

    private companion object {
        val KEY = longPreferencesKey("latest_known_at")
    }
}
