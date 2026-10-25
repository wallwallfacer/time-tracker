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

    /**
     * Overwrites the stored value. The phone's `/latest` is authoritative (a deleted press must stop
     * counting), and a successful local send writes its own time so an offline mark shows at once.
     */
    suspend fun set(at: Long) {
        store.edit { it[KEY] = at }
    }

    private companion object {
        val KEY = longPreferencesKey("latest_known_at")
    }
}
