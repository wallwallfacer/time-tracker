package dev.timetracker.wear

import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import dev.timetracker.core.DataLayerContract
import dev.timetracker.core.Source
import kotlinx.coroutines.tasks.await

object PointSender {
    /** Writes an urgent `/point/<id>` DataItem for the phone to consume. Exceptions propagate to the caller. */
    suspend fun send(dataClient: DataClient, at: Long, id: String) {
        val request = PutDataMapRequest.create(DataLayerContract.pointPath(id))
        request.dataMap.putString(DataLayerContract.KEY_ID, id)
        request.dataMap.putLong(DataLayerContract.KEY_AT, at)
        request.dataMap.putString(DataLayerContract.KEY_SOURCE, Source.WATCH.name)
        request.setUrgent()
        dataClient.putDataItem(request.asPutDataRequest()).await()
    }
}
