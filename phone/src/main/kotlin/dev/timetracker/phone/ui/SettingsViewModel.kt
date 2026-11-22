package dev.timetracker.phone.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.timetracker.core.Tag
import dev.timetracker.core.fromCsv
import dev.timetracker.phone.data.MergeResult
import dev.timetracker.phone.data.PointRepository
import dev.timetracker.phone.sync.LatestPublisher
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: PointRepository,
    private val zone: ZoneId,
    private val publisher: LatestPublisher? = null,
    private val clock: () -> Long,
) : ViewModel() {
    val tags: StateFlow<List<Tag>> =
        repo.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun deleteCount(tagId: String): Int = repo.countWithTag(tagId)

    /** Returns false for a blank name or a case-insensitive clash with another tag. */
    suspend fun rename(tagId: String, name: String): Boolean {
        if (name.isBlank()) return false
        return repo.renameTag(tagId, name)
    }

    fun setColor(tagId: String, color: Int) {
        viewModelScope.launch { repo.setTagColor(tagId, color) }
    }

    fun deleteTag(tagId: String) {
        viewModelScope.launch { repo.deleteTag(tagId) }
    }

    suspend fun csv(): String = repo.csv(zone, clock())

    /** Merges an exported or backed-up CSV into the app; null when [csv] is not such a file. */
    suspend fun restore(csv: String): MergeResult? {
        val snapshot = fromCsv(csv) ?: return null
        return repo.merge(snapshot).also { if (it.points > 0) publisher?.publish(repo.newestAt()) }
    }
}
