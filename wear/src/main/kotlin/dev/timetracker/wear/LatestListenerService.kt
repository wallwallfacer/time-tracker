package dev.timetracker.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import dev.timetracker.core.DataLayerContract
import kotlinx.coroutines.runBlocking

/** Stores the phone's `/latest` value so the next press can show "previous block". */
class LatestListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        runBlocking {
            for (event in events) {
                if (event.type != DataEvent.TYPE_CHANGED || event.dataItem.uri.path != DataLayerContract.LATEST_PATH) continue
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                if (!dataMap.containsKey(DataLayerContract.KEY_AT)) continue
                LatestStore(applicationContext).setIfNewer(dataMap.getLong(DataLayerContract.KEY_AT))
                ComplicationUpdater.requestUpdate(applicationContext)
            }
        }
    }
}
