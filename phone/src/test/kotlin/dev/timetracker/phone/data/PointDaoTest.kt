package dev.timetracker.phone.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.timetracker.core.Point
import dev.timetracker.core.Snapshot
import dev.timetracker.core.Source
import dev.timetracker.core.Tag
import dev.timetracker.core.fromCsv
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PointDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: PointRepository
    private var now = 1000L

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = PointRepository(db) { now }
    }

    @After fun tearDown() = db.close()

    @Test fun `insert_same_id_twice_keeps_one_row`() = runTest {
        assertTrue(repo.mark(Source.WATCH, at = 100, id = "x")); assertFalse(repo.mark(Source.WATCH, at = 100, id = "x"))
        assertEquals(1, repo.points.first().size)
    }

    @Test fun `createTag rejects case-insensitive duplicate`() = runTest {
        assertNotNull(repo.createTag("Work")); assertNull(repo.createTag("work"))
    }

    @Test fun `deleteTag clears points and returns count`() = runTest {
        val tag = repo.createTag("Work")!!
        repo.mark(Source.PHONE, at = 100, id = "a"); repo.mark(Source.PHONE, at = 200, id = "b"); repo.mark(Source.PHONE, at = 300, id = "c")
        repo.setTag("a", tag.id); repo.setTag("b", tag.id)
        assertEquals(2, repo.deleteTag(tag.id))
        val points = repo.points.first()
        assertEquals(3, points.size)
        assertTrue(points.all { it.tagId == null })
        assertTrue(repo.tags.first().isEmpty())
    }

    @Test fun `merge adds missing rows and keeps existing ones`() = runTest {
        val work = repo.createTag("Work")!!
        repo.mark(Source.PHONE, at = 100, id = "a"); repo.setNote("a", "kept")
        val snapshot = Snapshot(
            points = listOf(
                Point("a", 999, Source.WATCH, null, "from backup", 1), // already there: untouched
                Point("b", 200, Source.WATCH, "w2", null, 2), // tagged with the backup's "work"
                Point("c", 300, Source.MANUAL, "gy", "x", 3),
                Point("d", 400, Source.PHONE, "missing", null, 4), // tag not in the file: unlabeled
            ),
            tags = listOf(Tag("w2", "work", 1, 5), Tag("gy", "Gym", 0xFF81C784.toInt(), 6)),
        )
        assertEquals(MergeResult(points = 3, tags = 1), repo.merge(snapshot))
        val points = repo.points.first().associateBy { it.id }
        assertEquals(Point("a", 100, Source.PHONE, null, "kept", now), points["a"])
        assertEquals(work.id, points.getValue("b").tagId)
        assertEquals(Point("c", 300, Source.MANUAL, "gy", "x", 3), points["c"])
        assertNull(points.getValue("d").tagId)
        assertEquals(setOf(work, Tag("gy", "Gym", 0xFF81C784.toInt(), 6)), repo.tags.first().toSet())
        assertEquals(MergeResult(0, 0), repo.merge(snapshot)) // idempotent
    }

    @Test fun `csv round-trips into an empty database`() = runTest {
        val work = repo.createTag("Work")!!
        repo.createTag("Unused")
        repo.mark(Source.WATCH, at = 100, id = "a"); repo.mark(Source.PHONE, at = 250, id = "b")
        repo.setTag("a", work.id); repo.setNote("a", "n, \"q\"")
        val before = repo.points.first().toSet() to repo.tags.first().toSet()
        val csv = repo.csv(ZoneOffset.UTC)

        val other = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val fresh = PointRepository(other) { now }
            assertEquals(MergeResult(2, 2), fresh.merge(fromCsv(csv)!!))
            assertEquals(before, fresh.points.first().toSet() to fresh.tags.first().toSet())
        } finally {
            other.close()
        }
    }

    @Test fun `setTag touches lastUsedAt`() = runTest {
        val a = repo.createTag("A")!!
        now = 2000L
        val b = repo.createTag("B")!!
        assertEquals(listOf(b.id, a.id), repo.tags.first().map { it.id })
        repo.mark(Source.PHONE, at = 100, id = "p")
        now = 3000L
        repo.setTag("p", a.id)
        assertEquals(listOf(a.id, b.id), repo.tags.first().map { it.id })
        assertEquals(3000L, repo.tags.first().first().lastUsedAt)
    }
}
