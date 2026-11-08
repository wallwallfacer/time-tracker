package dev.timetracker.wear

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMapItem
import dev.timetracker.core.DataLayerContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private val Context.dataStore by preferencesDataStore(name = "latest")

/** The only value the watch persists: the newest point time it knows of, to compute "previous block". */
class LatestStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val latest: Flow<Long?> = store.data.map { it[KEY] }

    suspend fun get(): Long? = latest.first()

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

/**
 * The phone's `/latest` as already synced into this watch's Data Layer. Read directly because
 * [LatestListenerService] is not always woken to copy it into [LatestStore] (e.g. a sleeping app).
 */
suspend fun syncedLatest(dataClient: DataClient): Long? {
    val items = dataClient.getDataItems(Uri.parse("wear://*" + DataLayerContract.LATEST_PATH)).await()
    try {
        return items.mapNotNull { item ->
            DataMapItem.fromDataItem(item).dataMap.takeIf { it.containsKey(DataLayerContract.KEY_AT) }?.getLong(DataLayerContract.KEY_AT)
        }.maxOrNull()
    } finally {
        items.release()
    }
}
