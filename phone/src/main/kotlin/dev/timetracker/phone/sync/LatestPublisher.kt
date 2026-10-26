package dev.timetracker.phone.sync

import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import dev.timetracker.core.DataLayerContract
import dev.timetracker.core.Source
import dev.timetracker.phone.data.PointRepository
import java.util.UUID
import kotlinx.coroutines.tasks.await

class LatestPublisher(private val dataClient: DataClient) {
    /** Publishes the newest point time to the watch; no-op when null. Best-effort: failures are logged. */
    suspend fun publish(newestAt: Long?) {
        if (newestAt == null) return
        try {
            val request = PutDataMapRequest.create(DataLayerContract.LATEST_PATH)
            request.dataMap.putLong(DataLayerContract.KEY_AT, newestAt)
            request.setUrgent()
            dataClient.putDataItem(request.asPutDataRequest()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to publish latest point", e)
        }
    }

    private companion object {
        const val TAG = "LatestPublisher"
    }
}

suspend fun PointRepository.markAndPublish(
    source: Source,
    publisher: LatestPublisher,
    at: Long = clock(),
    id: String = UUID.randomUUID().toString(),
): Boolean = mark(source, at, id).also { publisher.publish(newestAt()) }
