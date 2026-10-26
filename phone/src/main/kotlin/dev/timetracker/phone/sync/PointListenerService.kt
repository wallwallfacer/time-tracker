package dev.timetracker.phone.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dev.timetracker.phone.data.AppDatabase
import dev.timetracker.phone.data.PointRepository
import kotlinx.coroutines.runBlocking

/** Consumes watch `/point/<id>` items into Room, deletes them, then republishes `/latest`. */
class PointListenerService : WearableListenerService() {
    private val repo by lazy { PointRepository(AppDatabase.get(applicationContext)) }
    private val dataClient by lazy { Wearable.getDataClient(this) }

    override fun onDataChanged(events: DataEventBuffer) {
        runBlocking {
            var inserted = false
            for (event in events) {
                if (event.type != DataEvent.TYPE_CHANGED) continue
                if (PointItemConsumer.consume(dataClient, repo, event.dataItem)) inserted = true
            }
            if (inserted) LatestPublisher(dataClient).publish(repo.newestAt())
        }
    }
}
