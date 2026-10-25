package dev.timetracker.phone.sync

import dev.timetracker.core.DataLayerContract
import dev.timetracker.core.Source

data class IncomingPoint(val id: String, val at: Long, val source: Source)

object PointItemParser {
    /** Returns null when the path is not a point path or `id`/`at` are missing or invalid. */
    fun parse(path: String, data: Map<String, Any?>): IncomingPoint? {
        if (!path.startsWith(DataLayerContract.POINT_PATH_PREFIX)) return null
        val id = (data[DataLayerContract.KEY_ID] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val at = (data[DataLayerContract.KEY_AT] as? Number)?.toLong() ?: return null
        val source = Source.entries.firstOrNull { it.name == data[DataLayerContract.KEY_SOURCE] } ?: Source.WATCH
        return IncomingPoint(id, at, source)
    }
}
