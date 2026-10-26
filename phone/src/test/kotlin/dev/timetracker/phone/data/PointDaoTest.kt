package dev.timetracker.phone.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.timetracker.core.Source
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
