package dev.timetracker.phone.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import dev.timetracker.core.DataLayerContract
import dev.timetracker.phone.data.AppDatabase
import dev.timetracker.phone.data.PointRepository
import kotlinx.coroutines.tasks.await

/** Moves watch `/point/<id>` DataItems into Room. Shared by [PointListenerService] and the foreground [sweep]. */
object PointItemConsumer {
    /**
     * Inserts [item] (idempotent) and deletes it once the row exists. Any failure, parsing included,
     * leaves the item in place for the next [sweep]. Returns true when a new row was inserted.
     */
    suspend fun consume(dataClient: DataClient, repo: PointRepository, item: DataItem): Boolean {
        val path = item.uri.path ?: return false
        if (!path.startsWith(DataLayerContract.POINT_PATH_PREFIX)) return false
        var inserted = false
        try {
            val dataMap = DataMapItem.fromDataItem(item).dataMap
            val point = PointItemParser.parse(path, dataMap.keySet().associateWith { dataMap.get<Any>(it) })
            if (point == null) {
                Log.w(TAG, "Skipping malformed point item $path")
                return false
            }
            // A duplicate id (false) still means the row exists, so the item is safe to delete.
            inserted = repo.mark(point.source, point.at, point.id)
            dataClient.deleteDataItems(item.uri).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to consume point item $path; leaving it for the next sweep", e)
        }
        return inserted
    }

    /** Consumes every leftover point item (failed inserts, items that arrived during a reinstall). */
    suspend fun sweep(context: Context) {
        val dataClient = Wearable.getDataClient(context)
        val repo = PointRepository(AppDatabase.get(context))
        val items = dataClient.getDataItems(Uri.parse("wear://*" + DataLayerContract.POINT_PATH_PREFIX), DataClient.FILTER_PREFIX).await()
        var inserted = false
        try {
            for (item in items) if (consume(dataClient, repo, item)) inserted = true
        } finally {
            items.release()
        }
        if (inserted) LatestPublisher(dataClient).publish(repo.newestAt())
    }

    private const val TAG = "PointItemConsumer"
}
