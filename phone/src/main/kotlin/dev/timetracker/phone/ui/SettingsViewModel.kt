package dev.timetracker.phone.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.timetracker.core.Tag
import dev.timetracker.core.deriveBlocks
import dev.timetracker.core.toCsv
import dev.timetracker.phone.data.PointRepository
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: PointRepository,
    private val zone: ZoneId,
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

    fun deleteTag(tagId: String) {
        viewModelScope.launch { repo.deleteTag(tagId) }
    }

    suspend fun csv(): String =
        toCsv(deriveBlocks(repo.points.first(), clock()), repo.tags.first().associateBy { it.id }, zone)
}
