package dev.timetracker.phone.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.timetracker.core.Point
import dev.timetracker.core.Source
import dev.timetracker.core.Tag

@Entity(tableName = "points", indices = [Index("at"), Index("tagId")])
data class PointEntity(
    @PrimaryKey val id: String,
    val at: Long,
    val source: String,
    val tagId: String?,
    val note: String?,
    val createdAt: Long,
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Int,
    val lastUsedAt: Long,
)

fun PointEntity.toPoint() = Point(id, at, Source.valueOf(source), tagId, note, createdAt)
fun Point.toEntity() = PointEntity(id, at, source.name, tagId, note, createdAt)
fun TagEntity.toTag() = Tag(id, name, color, lastUsedAt)
fun Tag.toEntity() = TagEntity(id, name, color, lastUsedAt)
