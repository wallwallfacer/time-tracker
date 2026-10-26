package dev.timetracker.phone.data

import dev.timetracker.core.Point
import dev.timetracker.core.Source
import dev.timetracker.core.Tag
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object TagPalette {
    val colors: List<Int> = listOf(
        0xFFE57373.toInt(), 0xFFFFB74D.toInt(), 0xFFFFF176.toInt(), 0xFF81C784.toInt(),
        0xFF4DB6AC.toInt(), 0xFF64B5F6.toInt(), 0xFF9575CD.toInt(), 0xFFF06292.toInt(),
    )

    fun next(existing: Int): Int = colors[existing % colors.size]
}

class PointRepository(private val db: AppDatabase, val clock: () -> Long = System::currentTimeMillis) {
    private val pointDao = db.pointDao()
    private val tagDao = db.tagDao()

    val points: Flow<List<Point>> = pointDao.observeAll().map { it.map(PointEntity::toPoint) }
    val tags: Flow<List<Tag>> = tagDao.observeAll().map { it.map(TagEntity::toTag) }

    /** Returns false when a point with [id] already exists (idempotent insert). */
    suspend fun mark(source: Source, at: Long = clock(), id: String = UUID.randomUUID().toString()): Boolean =
        pointDao.insertIgnore(Point(id, at, source, createdAt = clock()).toEntity()) != -1L

    /** Re-inserts [p] exactly as it was (id, time, source, tag, note) in one write; false if its id already exists. */
    suspend fun restore(p: Point): Boolean = pointDao.insertIgnore(p.toEntity()) != -1L

    suspend fun deletePoint(id: String) = pointDao.delete(id)
    suspend fun movePoint(id: String, at: Long) = pointDao.updateAt(id, at)

    suspend fun insertManual(at: Long): Point =
        Point(UUID.randomUUID().toString(), at, Source.MANUAL, createdAt = clock()).also { pointDao.insertIgnore(it.toEntity()) }

    suspend fun setTag(pointId: String, tagId: String?) {
        pointDao.updateTag(pointId, tagId)
        if (tagId != null) tagDao.touch(tagId, clock())
    }

    suspend fun setNote(pointId: String, note: String?) = pointDao.updateNote(pointId, note)

    /** Returns null when the name is already taken (case-insensitive). */
    suspend fun createTag(name: String): Tag? {
        val trimmed = name.trim()
        if (tagDao.countByName(trimmed) > 0) return null
        val color = TagPalette.next(tags.first().size)
        return Tag(UUID.randomUUID().toString(), trimmed, color, clock()).also { tagDao.insert(it.toEntity()) }
    }

    /** Returns false on a case-insensitive clash with a different tag. */
    suspend fun renameTag(id: String, name: String): Boolean {
        val trimmed = name.trim()
        val clash = tags.first().any { it.id != id && it.name.equals(trimmed, ignoreCase = true) }
        if (clash) return false
        tagDao.rename(id, trimmed)
        return true
    }

    /** Returns the number of points whose tag was cleared. */
    suspend fun deleteTag(id: String): Int = pointDao.clearTag(id).also { tagDao.delete(id) }

    suspend fun countWithTag(tagId: String): Int = pointDao.countWithTag(tagId)

    suspend fun newestAt(): Long? = pointDao.newestAt()
}
