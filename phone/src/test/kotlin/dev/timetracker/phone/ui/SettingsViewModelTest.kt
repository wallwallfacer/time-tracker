package dev.timetracker.phone.ui

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.timetracker.core.Source
import dev.timetracker.phone.data.AppDatabase
import dev.timetracker.phone.data.PointRepository
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsViewModelTest {
    private val now = 1_000L

    private lateinit var db: AppDatabase
    private lateinit var repo: PointRepository
    private lateinit var vm: SettingsViewModel

    @Before fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = PointRepository(db) { now }
        vm = SettingsViewModel(repo, ZoneOffset.UTC) { now }
    }

    @After fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test fun `csv contains header and one row per block`() = runTest {
        val work = repo.createTag("Work")!!
        repo.mark(Source.PHONE, 100, "a"); repo.mark(Source.PHONE, 200, "b")
        repo.setTag("a", work.id)
        val lines = vm.csv().trimEnd('\n').lines()
        assertEquals("start_iso,end_iso,duration_sec,tag,note,source", lines[0])
        assertEquals(3, lines.size)
        assertTrue(lines[1].contains("Work"))
    }

    @Test fun `rename rejects duplicate`() = runTest {
        val work = repo.createTag("Work")!!
        val play = repo.createTag("Play")!!
        assertFalse(vm.rename(play.id, "work"))
        assertTrue(vm.rename(work.id, "Job"))
        assertEquals(setOf("Job", "Play"), repo.tags.first().map { it.name }.toSet())
    }

    @Test fun `rename rejects blank name`() = runTest {
        val work = repo.createTag("Work")!!
        assertFalse(vm.rename(work.id, "   "))
        assertEquals("Work", repo.tags.first().single().name)
    }

    @Test fun `deleteTag clears blocks`() = runTest {
        val work = repo.createTag("Work")!!
        repo.mark(Source.PHONE, 100, "a"); repo.mark(Source.PHONE, 200, "b")
        repo.setTag("a", work.id); repo.setTag("b", work.id)
        assertEquals(2, vm.deleteCount(work.id))
        vm.deleteTag(work.id)
        advanceUntilIdle()
        // Room runs the delete on its own executor, so wait for the flows rather than the test scheduler.
        repo.tags.first { it.isEmpty() }
        assertTrue(repo.points.first { p -> p.all { it.tagId == null } }.isNotEmpty())
        assertNull(repo.points.first().first { it.id == "a" }.tagId)
    }
}
