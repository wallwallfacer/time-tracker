package dev.timetracker.core

/** Paths and keys used by the Wear OS Data Layer to sync points. */
object DataLayerContract {
    const val POINT_PATH_PREFIX = "/point/"
    const val LATEST_PATH = "/latest"
    const val KEY_ID = "id"
    const val KEY_AT = "at"
    const val KEY_SOURCE = "source"

    fun pointPath(id: String) = POINT_PATH_PREFIX + id
}
