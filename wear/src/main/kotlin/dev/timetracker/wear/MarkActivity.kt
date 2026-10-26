package dev.timetracker.wear

import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable
import dev.timetracker.core.LaunchDecision
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Process-wide scope for the Data Layer write; outlives any single activity instance. */
private object MarkScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO)

private class Shown(val now: Long, val prev: Long?, val recorded: Boolean)

/** Home-key double-press target: sends a point, vibrates, shows a 3 s confirmation, then removes itself. */
class MarkActivity : ComponentActivity() {
    private lateinit var store: LatestStore
    private lateinit var dataClient: DataClient
    private var shown by mutableStateOf<Shown?>(null)
    private var finishJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LatestStore(applicationContext)
        dataClient = Wearable.getDataClient(applicationContext)
        setContent { shown?.let { ConfirmScreen(it.now, it.prev, it.recorded) } }
        if (LaunchDecision.shouldRecord(savedInstanceState == null, fromNewIntent = false)) record()
        else finishAndRemoveTask() // recreation: nothing to show, don't leave an invisible activity on top
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (LaunchDecision.shouldRecord(savedInstanceStateIsNull = false, fromNewIntent = true)) record()
    }

    private fun record() {
        // Cancel the old finish timer synchronously so it cannot fire during this press's write.
        finishJob?.cancel()
        val store = store
        val dataClient = dataClient
        val appContext = applicationContext
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        // The write runs on a process-wide scope so rotation/finish never cancels it.
        // The lambda captures only the locals above, never the activity.
        val outcome = MarkScope.async {
            // A failing store read degrades to "no previous point"; it must never block the send.
            val prev = runCatching { store.get() } // read before updating
                .onFailure { Log.w(TAG, "Failed to read latest point", it) }
                .getOrNull()
            val sent = try {
                PointSender.send(dataClient, now, id)
                true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send point $id", e)
                false
            }
            if (sent) {
                runCatching { store.set(now) }
                    .onFailure { Log.w(TAG, "Failed to store latest point", it) }
            }
            runCatching { ComplicationUpdater.requestUpdate(appContext) }
                .onFailure { Log.w(TAG, "Failed to request complication update", it) }
            Shown(now, prev, sent)
        }
        lifecycleScope.launch {
            val result = outcome.await()
            vibrate(result.recorded)
            shown = result
            finishJob?.cancel()
            finishJob = launch {
                delay(3000)
                finishAndRemoveTask()
            }
        }
    }

    /** One short pulse when recorded; two 150 ms pulses when the write failed. */
    private fun vibrate(recorded: Boolean) {
        val effect = if (recorded) {
            VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
        } else {
            VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1)
        }
        getSystemService(VibratorManager::class.java).defaultVibrator.vibrate(effect)
    }

    private companion object {
        const val TAG = "MarkActivity"
    }
}
