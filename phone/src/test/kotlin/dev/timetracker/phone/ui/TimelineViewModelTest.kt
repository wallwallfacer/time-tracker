package dev.timetracker.phone.ui

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.wearable.Wearable
import dev.timetracker.core.Source
import dev.timetracker.phone.data.AppDatabase
import dev.timetracker.phone.data.PointRepository
import dev.timetracker.phone.sync.LatestPublisher
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimelineViewModelTest {
    private val zone: ZoneId = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 25)
    private val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
    private val hour = 3_600_000L
    private val now = dayStart + 12 * hour

    private lateinit var db: AppDatabase
    private lateinit var repo: PointRepository
    private lateinit var vm: TimelineViewModel

    @Before fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = PointRepository(db) { now }
        vm = TimelineViewModel(repo, LatestPublisher(Wearable.getDataClient(context)), zone) { now }
    }

    @After fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private suspend fun markAt(id: String, at: Long) = repo.mark(Source.PHONE, at, id)

    @Test fun `rows for today derive from points and clip to day`() = runTest {
        markAt("a", dayStart + 9 * hour); markAt("b", dayStart + 10 * hour); markAt("c", dayStart + 11 * hour)
        val state = vm.state.first { it.rows.size == 3 }
        assertEquals(today, state.day)
        assertFalse(state.isEmpty)
        assertEquals(listOf("a", "b", "c"), state.rows.map { it.block.block.start.id })
        assertEquals(listOf(false, false, true), state.rows.map { it.block.block.isCurrent })
        assertEquals(dayStart + 11 * hour, state.rows.last().block.visibleStart)
        assertEquals(now, state.rows.last().block.visibleEnd)
    }

    @Test fun `deletePoint returns point and undo restores it`() = runTest {
        markAt("a", dayStart + 9 * hour); markAt("b", dayStart + 10 * hour); markAt("c", dayStart + 11 * hour)
        val tag = repo.createTag("Work")!!
        repo.setTag("b", tag.id); repo.setNote("b", "standup")
        vm.state.first { it.rows.size == 3 && it.rows[1].tag != null }

        val removed = vm.deletePoint("b")
        assertEquals("b", removed.id)
        assertEquals(dayStart + 10 * hour, removed.at)
        assertEquals(tag.id, removed.tagId)
        assertEquals("standup", removed.note)
        assertEquals(listOf("a", "c"), vm.state.first { it.rows.size == 2 }.rows.map { it.block.block.start.id })

        vm.undoDelete(removed)
        val restored = vm.state.first { it.rows.size == 3 && it.rows[1].tag != null }
        assertEquals(listOf("a", "b", "c"), restored.rows.map { it.block.block.start.id })
        val point = repo.points.first().first { it.id == "b" }
        assertEquals(removed.at, point.at)
        assertEquals(tag.id, point.tagId)
        assertEquals("standup", point.note)
    }

    @Test fun `next and previous day move the window`() = runTest {
        markAt("x", dayStart - hour); markAt("a", dayStart + 9 * hour); markAt("b", dayStart + 10 * hour)
        markAt("c", dayStart + 11 * hour)
        // Today: the block opened at 23:00 yesterday spans midnight and is clipped to 00:00.
        val todayState = vm.state.first { it.rows.size == 4 }
        assertEquals(dayStart, todayState.rows.first().block.visibleStart)

        vm.previousDay()
        val yesterday = vm.state.first { it.day == today.minusDays(1) }
        assertEquals(listOf("x"), yesterday.rows.map { it.block.block.start.id })
        assertEquals(dayStart - hour, yesterday.rows.single().block.visibleStart)
        assertEquals(dayStart, yesterday.rows.single().block.visibleEnd)
        assertFalse(yesterday.isEmpty)

        vm.today(); vm.nextDay()
        val tomorrow = vm.state.first { it.day == today.plusDays(1) }
        assertTrue(tomorrow.rows.isEmpty())

        vm.today()
        assertEquals(4, vm.state.first { it.day == today && it.rows.size == 4 }.rows.size)
    }
}
