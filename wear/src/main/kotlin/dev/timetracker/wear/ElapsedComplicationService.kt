package dev.timetracker.wear

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import dev.timetracker.core.formatDuration

/** Shows the time since the latest known point; tapping it opens [MarkActivity], which records a point. */
class ElapsedComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null
        val latest = runCatching { LatestStore(applicationContext).get() }.getOrNull()
        val text = if (latest != null) formatDuration(System.currentTimeMillis() - latest) else "—"
        return build(text)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        if (type == ComplicationType.SHORT_TEXT) build("47m") else null

    private fun build(text: String): ComplicationData {
        val tap = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MarkActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return ShortTextComplicationData.Builder(
            PlainComplicationText.Builder(text).build(),
            PlainComplicationText.Builder(getString(R.string.complication_description)).build(),
        ).setTapAction(tap).build()
    }
}

/** Asks the system to re-query [ElapsedComplicationService] for every active complication. */
object ComplicationUpdater {
    fun requestUpdate(context: Context) {
        ComplicationDataSourceUpdateRequester.create(
            context,
            ComponentName(context, ElapsedComplicationService::class.java),
        ).requestUpdateAll()
    }
}
