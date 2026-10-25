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
import dev.timetracker.phone.sync.markAndPublish
import java.time.ZoneId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (LaunchDecision.shouldRecord(savedInstanceStateIsNull = false, fromNewIntent = true)) record()
    }

    private fun record() {
        val now = repo.clock()
        lifecycleScope.launch {
            val previous = repo.newestAt() // read before inserting
            repo.markAndPublish(Source.PHONE, publisher, at = now)
            vibrate()
            overlay = OverlayState(
                time = formatTime(now, ZoneId.systemDefault()),
                previousBlock = previous?.let { formatDuration(now - it) },
            )
            // Only the timer is cancelled on a repeat press, never an in-flight write.
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
