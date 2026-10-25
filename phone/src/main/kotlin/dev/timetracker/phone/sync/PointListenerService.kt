package dev.timetracker.phone.sync

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dev.timetracker.core.DataLayerContract
import dev.timetracker.phone.data.AppDatabase
import dev.timetracker.phone.data.PointRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

/** Consumes watch `/point/<id>` items into Room, deletes them, then republishes `/latest`. */
class PointListenerService : WearableListenerService() {
    private val repo by lazy { PointRepository(AppDatabase.get(applicationContext)) }
    private val dataClient by lazy { Wearable.getDataClient(this) }

    override fun onDataChanged(events: DataEventBuffer) {
        runBlocking {
            var inserted = false
            for (event in events) {
                val path = event.dataItem.uri.path ?: continue
                if (event.type != DataEvent.TYPE_CHANGED || !path.startsWith(DataLayerContract.POINT_PATH_PREFIX)) continue
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val point = PointItemParser.parse(path, dataMap.keySet().associateWith { dataMap.get<Any>(it) })
                if (point == null) {
                    Log.w(TAG, "Skipping malformed point item $path")
                    continue
                }
                try {
                    // A duplicate id (false) still means the row exists, so the item is safe to delete.
                    if (repo.mark(point.source, point.at, point.id)) inserted = true
                    dataClient.deleteDataItems(event.dataItem.uri).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to consume point item $path; leaving it for retry", e)
                }
            }
            if (inserted) LatestPublisher(dataClient).publish(repo.newestAt())
        }
    }

    private companion object {
        const val TAG = "PointListenerService"
    }
}
