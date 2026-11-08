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
        // 24 more, picked by script to stay as far apart as possible (OKLab) from the 8 above and each other in both
        // themes; most distinct first, so new tags spread out before similar shades are used.
        0xFFF277FE.toInt(), 0xFFE5BBE1.toInt(), 0xFF9ED3F2.toInt(), 0xFFA3745B.toInt(),
        0xFFADA3FF.toInt(), 0xFFB5D6AA.toInt(), 0xFFFD80C4.toInt(), 0xFFAD75FE.toInt(),
        0xFFFA6004.toInt(), 0xFFF8AAFF.toInt(), 0xFF7E7AA8.toInt(), 0xFFF3909D.toInt(),
        0xFF3A74FF.toInt(), 0xFF86F8CF.toInt(), 0xFF088BBE.toInt(), 0xFFAC731A.toInt(),
        0xFF6E8D35.toInt(), 0xFFFE5166.toInt(), 0xFFE0C796.toInt(), 0xFF718FFF.toInt(),
        0xFF6CF6FB.toInt(), 0xFF5B7EC8.toInt(), 0xFFF19773.toInt(), 0xFFA26F83.toInt(),
    )

    /** The palette ordered by hue, so the picker reads as a spectrum. */
    val byHue: List<Int> = colors.sortedBy { hue(it) }

    private fun hue(argb: Int): Float {
        val r = (argb shr 16 and 0xFF) / 255f; val g = (argb shr 8 and 0xFF) / 255f; val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b); val d = max - minOf(r, g, b)
        if (d == 0f) return 0f
        val h = when (max) { r -> ((g - b) / d).mod(6f); g -> (b - r) / d + 2; else -> (r - g) / d + 4 }
        return h * 60
    }

    /** The first palette color no tag uses yet; once all are taken, cycles by count. */
    fun next(used: Collection<Int>): Int = colors.firstOrNull { it !in used } ?: colors[used.size % colors.size]
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
        val color = TagPalette.next(tags.first().map { it.color })
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

    suspend fun setTagColor(id: String, color: Int) = tagDao.setColor(id, color)

    suspend fun countWithTag(tagId: String): Int = pointDao.countWithTag(tagId)

    suspend fun newestAt(): Long? = pointDao.newestAt()
}
