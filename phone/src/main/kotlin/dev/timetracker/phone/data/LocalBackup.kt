package dev.timetracker.phone.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps one export CSV per day in Download/TimeTracker/, the newest [KEEP_DAYS] days. Shared storage outlives
 * an uninstall, so these files can be restored from Settings after a reinstall.
 *
 * A reinstalled app does not own the old install's files: it cannot see, overwrite or delete them, so they stay
 * until the user removes them, and today's file may get a " (1)" suffix.
 */
object LocalBackup {
    const val KEEP_DAYS = 3
    private const val PREFIX = "time-tracker-backup-"
    private val NAME = Regex("""^${Regex.escape(PREFIX)}(\d{4}-\d{2}-\d{2})(?: \(\d+\))?\.csv$""")
    private val RELATIVE_PATH = Environment.DIRECTORY_DOWNLOADS + "/TimeTracker/"
    private const val TAG = "LocalBackup"

    /** Outlives the activity or service that asks, so a backup started on the way out is not cancelled. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()

    /** Writes today's backup in the background; failures are logged, never shown. */
    fun schedule(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return // MediaStore.Downloads needs Android 10
        val app = context.applicationContext
        scope.launch {
            runCatching { lock.withLock { write(app) } }.onFailure { Log.w(TAG, "Local backup failed", it) }
        }
    }

    fun fileName(date: LocalDate) = "$PREFIX$date.csv"

    /** The names to delete so only the [keep] newest dates remain; names that are not backups are never picked. */
    fun toPrune(names: Collection<String>, keep: Int): List<String> {
        val dated = names.mapNotNull { name -> NAME.find(name)?.let { name to LocalDate.parse(it.groupValues[1]) } }
        val kept = dated.map { it.second }.distinct().sortedDescending().take(keep).toSet()
        return dated.filter { it.second !in kept }.map { it.first }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun write(context: Context) {
        val repo = PointRepository(AppDatabase.get(context))
        // A fresh install has nothing worth saving, and must not crowd out the real backups.
        if (repo.points.first().isEmpty()) return
        val zone = ZoneId.systemDefault()
        val csv = repo.csv(zone)
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val own = ownFiles(context) // name -> uri; only files this install created are visible
        val name = fileName(LocalDate.now(zone))
        val uri = own[name] ?: resolver.insert(collection, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
        }) ?: error("MediaStore refused to create $name")
        resolver.openOutputStream(uri, "wt")?.use { it.write(csv.toByteArray()) } ?: error("Cannot open $uri")
        val now = ownFiles(context)
        for (old in toPrune(now.keys, KEEP_DAYS)) resolver.delete(now.getValue(old), null, null)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun ownFiles(context: Context): Map<String, Uri> {
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
        val files = HashMap<String, Uri>()
        context.contentResolver.query(collection, projection, selection, arrayOf(RELATIVE_PATH), null)?.use { c ->
            while (c.moveToNext()) files[c.getString(1)] = ContentUris.withAppendedId(collection, c.getLong(0))
        }
        return files
    }
}
