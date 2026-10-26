package dev.timetracker.phone.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao interface PointDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertIgnore(p: PointEntity): Long   // -1 when ignored
    @Query("DELETE FROM points WHERE id = :id") suspend fun delete(id: String)
    @Query("UPDATE points SET at = :at WHERE id = :id") suspend fun updateAt(id: String, at: Long)
    @Query("UPDATE points SET tagId = :tagId WHERE id = :id") suspend fun updateTag(id: String, tagId: String?)
    @Query("UPDATE points SET note = :note WHERE id = :id") suspend fun updateNote(id: String, note: String?)
    @Query("SELECT * FROM points") fun observeAll(): Flow<List<PointEntity>>
    @Query("SELECT MAX(at) FROM points") suspend fun newestAt(): Long?
    @Query("UPDATE points SET tagId = NULL WHERE tagId = :tagId") suspend fun clearTag(tagId: String): Int
    @Query("SELECT COUNT(*) FROM points WHERE tagId = :tagId") suspend fun countWithTag(tagId: String): Int
}

@Dao interface TagDao {
    @Query("SELECT * FROM tags ORDER BY lastUsedAt DESC") fun observeAll(): Flow<List<TagEntity>>
    @Insert suspend fun insert(t: TagEntity)
    @Query("UPDATE tags SET name = :name WHERE id = :id") suspend fun rename(id: String, name: String)
    @Query("UPDATE tags SET lastUsedAt = :at WHERE id = :id") suspend fun touch(id: String, at: Long)
    @Query("DELETE FROM tags WHERE id = :id") suspend fun delete(id: String)
    @Query("SELECT COUNT(*) FROM tags WHERE LOWER(name) = LOWER(:name)") suspend fun countByName(name: String): Int
}
