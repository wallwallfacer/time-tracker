package dev.timetracker.phone.capture

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.Wearable
import dev.timetracker.core.LaunchDecision
import dev.timetracker.core.Source
import dev.timetracker.core.formatDuration
import dev.timetracker.core.formatTime
import dev.timetracker.phone.data.AppDatabase
import dev.timetracker.phone.data.PointRepository
import dev.timetracker.phone.sync.LatestPublisher
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Process-wide scope for capture DB work and publishing; outlives any single activity instance. */
private object CaptureScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Side-key target: records a point immediately, shows a 2 s confirmation, then removes itself. */
class MarkPointActivity : ComponentActivity() {
    private lateinit var repo: PointRepository
    private lateinit var publisher: LatestPublisher
    private var overlay by mutableStateOf<OverlayState?>(null)
    private var finishJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = PointRepository(AppDatabase.get(applicationContext))
        publisher = LatestPublisher(Wearable.getDataClient(applicationContext))
        setContent { ConfirmOverlay(overlay) }
        if (LaunchDecision.shouldRecord(savedInstanceState == null, fromNewIntent = false)) record()
        else finishAndRemoveTask() // recreation: nothing to show, don't leave an invisible activity on top
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (LaunchDecision.shouldRecord(savedInstanceStateIsNull = false, fromNewIntent = true)) record()
    }

    private fun record() {
        // Cancel the old finish timer synchronously so it cannot fire during this press's DB work.
        finishJob?.cancel()
        val repo = repo
        val publisher = publisher
        val now = repo.clock()
        // DB work and publish run on a process-wide scope so rotation/finish never cancels an in-flight insert.
        // The lambdas capture only the locals above, never the activity.
        val previous = CaptureScope.async {
            val previous = repo.newestAt() // read before inserting
            repo.mark(Source.PHONE, at = now)
            CaptureScope.launch { publisher.publish(repo.newestAt()) } // best-effort, never delays feedback
            previous
        }
        lifecycleScope.launch {
            val previousAt = previous.await()
            vibrate()
            overlay = OverlayState(
                time = formatTime(now, ZoneId.systemDefault()),
                previousBlock = previousAt?.let { formatDuration(now - it) },
            )
            finishJob?.cancel()
            finishJob = launch {
                delay(2000)
                finishAndRemoveTask()
            }
        }
    }

    private fun vibrate() {
        val effect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(effect)
        }
    }
}
