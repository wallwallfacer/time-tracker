package dev.timetracker.phone.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.timetracker.core.Block
import dev.timetracker.core.Point
import dev.timetracker.core.Tag
import dev.timetracker.core.midpoint
import dev.timetracker.core.moveBounds
import dev.timetracker.core.resolvePickedTime
import dev.timetracker.phone.data.PointRepository
import dev.timetracker.phone.sync.LatestPublisher
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LabelSheetViewModel(
    private val repo: PointRepository,
    private val publisher: LatestPublisher,
    private val clock: () -> Long,
) : ViewModel() {
    val tags: StateFlow<List<Tag>> =
        repo.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val points: StateFlow<List<Point>> =
        repo.points.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The label belongs to the block's start point. */
    fun applyTag(block: Block, tagId: String?) {
        viewModelScope.launch { repo.setTag(block.start.id, tagId) }
    }

    /** Returns false for a blank or duplicate name; otherwise creates the tag and applies it. */
    suspend fun createAndApply(block: Block, name: String): Boolean {
        if (name.isBlank()) return false
        val tag = repo.createTag(name) ?: return false
        repo.setTag(block.start.id, tag.id)
        return true
    }

    fun setNote(block: Block, note: String) {
        viewModelScope.launch { repo.setNote(block.start.id, note.trim().ifEmpty { null }) }
    }

    fun startBounds(block: Block, all: List<Point>): LongRange = moveBounds(all, block.start.id, clock())

    /** The end time is the next point; the current block has none. */
    fun endBounds(block: Block, all: List<Point>): LongRange? =
        block.end?.let { moveBounds(all, it.id, clock()) }

    // No replay: a rejection nobody is watching (sheet already closed) is dropped, never shown later.
    private val moveRejections = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Fires when a picked time fits between the neighbours on no nearby date; the point was left unchanged. */
    val moveRejected: SharedFlow<Unit> = moveRejections

    fun moveStart(block: Block, hour: Int, minute: Int, zone: ZoneId) =
        move(block.start, hour, minute, zone) { startBounds(block, it) }

    fun moveEnd(block: Block, hour: Int, minute: Int, zone: ZoneId) {
        block.end?.let { end -> move(end, hour, minute, zone) { endBounds(block, it)!! } }
    }

    /** Reads the latest points inside the coroutine so bounds are never stale; never clamps. */
    private fun move(point: Point, hour: Int, minute: Int, zone: ZoneId, bounds: (List<Point>) -> LongRange) {
        viewModelScope.launch {
            val at = resolvePickedTime(point.at, hour, minute, bounds(repo.points.first()), zone)
            if (at == null) {
                moveRejections.tryEmit(Unit)
                return@launch
            }
            repo.movePoint(point.id, at)
            publisher.publish(repo.newestAt())
        }
    }

    /** Returns the new, unlabeled point so the caller can open the time picker on it. */
    suspend fun insertMidpoint(block: Block): Point =
        repo.insertManual(midpoint(block)).also { publisher.publish(repo.newestAt()) }

    fun deleteStart(block: Block) {
        viewModelScope.launch {
            repo.deletePoint(block.start.id)
            publisher.publish(repo.newestAt())
        }
    }
}
