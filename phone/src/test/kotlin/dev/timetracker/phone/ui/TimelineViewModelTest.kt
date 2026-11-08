package dev.timetracker.phone.ui

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.wearable.Wearable
import dev.timetracker.core.Source
import dev.timetracker.core.ViewMode
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

    @Test fun `deletePoint removes point and undo restores it`() = runTest {
        markAt("a", dayStart + 9 * hour); markAt("b", dayStart + 10 * hour); markAt("c", dayStart + 11 * hour)
        val tag = repo.createTag("Work")!!
        repo.setTag("b", tag.id); repo.setNote("b", "standup")
        val removed = vm.state.first { it.rows.size == 3 && it.rows[1].tag != null }.rows[1].block.block.start

        vm.deletePoint(removed)
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

        vm.previous()
        val yesterday = vm.state.first { it.day == today.minusDays(1) }
        assertEquals(listOf("x"), yesterday.rows.map { it.block.block.start.id })
        assertEquals(dayStart - hour, yesterday.rows.single().block.visibleStart)
        assertEquals(dayStart, yesterday.rows.single().block.visibleEnd)
        assertFalse(yesterday.isEmpty)

        vm.current(); vm.next()
        val tomorrow = vm.state.first { it.day == today.plusDays(1) }
        assertTrue(tomorrow.rows.isEmpty())

        vm.current()
        assertEquals(4, vm.state.first { it.day == today && it.rows.size == 4 }.rows.size)
    }

    @Test fun `day follows today when not pinned`() = runTest {
        var clockNow = dayStart + 23 * hour + 59 * 60_000
        val followVm = TimelineViewModel(repo, LatestPublisher(Wearable.getDataClient(ApplicationProvider.getApplicationContext<android.content.Context>())), zone) { clockNow }
        assertEquals(today, followVm.state.first { it.day == today }.day)
        clockNow += 2 * 60_000 // 00:01 the next day
        assertEquals(today.plusDays(1), followVm.state.first { it.day == today.plusDays(1) }.day)

        followVm.previous() // pins a concrete date relative to the resolved day
        clockNow += 24 * hour
        assertEquals(today, followVm.state.first { it.day == today }.day)
        followVm.current()
        assertEquals(today.plusDays(2), followVm.state.first { it.day == today.plusDays(2) }.day)
    }

    // ---- Week / Month ----  (today is Sun Oct 25 2026, so this week is Oct 19 – 25)
    private val oct = { d: Int -> LocalDate.of(2026, 10, d) }
    private fun msOn(d: Int, h: Int = 0) = oct(d).atStartOfDay(zone).toInstant().toEpochMilli() + h * hour
    private suspend fun tagged(id: String, at: Long, tagId: String?) { markAt(id, at); if (tagId != null) repo.setTag(id, tagId) }
    private suspend fun overviewWhere(test: (OverviewState) -> Boolean) = vm.overview.first { it != null && test(it) }!!

    @Test fun `week next steps seven days`() = runTest {
        vm.setMode(ViewMode.WEEK)
        vm.next()
        assertEquals(today.plusDays(7), overviewWhere { it.anchor == today.plusDays(7) }.anchor)
    }

    @Test fun `mode switch keeps anchor`() = runTest {
        repeat(3) { vm.previous() }
        vm.setMode(ViewMode.MONTH)
        val month = overviewWhere { it.mode == ViewMode.MONTH }
        assertEquals(oct(22), month.anchor)
        vm.setMode(ViewMode.DAY)
        assertEquals(oct(22), vm.state.first { it.day == oct(22) }.day)
        assertEquals(null, vm.overview.first { it == null })
    }

    @Test fun `following today stays following`() = runTest {
        vm.setMode(ViewMode.WEEK)
        val week = overviewWhere { it.mode == ViewMode.WEEK }
        assertTrue(week.isCurrent)
        assertEquals(today, week.anchor)
    }

    @Test fun `open day pins and switches to day`() = runTest {
        vm.setMode(ViewMode.WEEK)
        vm.openDay(oct(20))
        assertEquals(ViewMode.DAY, vm.mode.first { it == ViewMode.DAY })
        assertEquals(oct(20), vm.state.first { it.day == oct(20) }.day)
    }

    @Test fun `rows ranked with deltas`() = runTest {
        val work = repo.createTag("Work")!!; val sleep = repo.createTag("Sleep")!!
        tagged("s1", msOn(12), sleep.id); tagged("w1", msOn(12, 8), work.id); tagged("s2", msOn(12, 10), sleep.id)
        tagged("w2", msOn(19), work.id); tagged("s3", msOn(19, 6), sleep.id)
        vm.setMode(ViewMode.WEEK)
        val rows = overviewWhere { it.rows.size == 2 }.rows
        assertEquals(listOf("Sleep", "Work"), rows.map { it.tag.name })
        assertEquals(listOf(150 * hour, 6 * hour), rows.map { it.ms })
        assertEquals(listOf(-4 * hour, 4 * hour), rows.map { it.deltaMs })
    }

    @Test fun `focus toggles`() = runTest {
        val work = repo.createTag("Work")!!
        tagged("w", msOn(19), work.id)
        vm.setMode(ViewMode.WEEK)
        vm.toggleFocus(Focus.OnTag(work.id))
        assertEquals(Focus.OnTag(work.id), overviewWhere { it.focus != null }.focus)
        vm.toggleFocus(Focus.OnTag(work.id))
        overviewWhere { it.focus == null }
    }

    @Test fun `focus clears on mode switch`() = runTest {
        val work = repo.createTag("Work")!!
        tagged("w", msOn(19), work.id)
        vm.setMode(ViewMode.WEEK)
        vm.toggleFocus(Focus.OnTag(work.id))
        overviewWhere { it.focus != null }
        vm.setMode(ViewMode.MONTH)
        assertEquals(null, overviewWhere { it.mode == ViewMode.MONTH }.focus)
    }

    @Test fun `focus clears when focused tag absent after stepping`() = runTest {
        val work = repo.createTag("Work")!!; val sleep = repo.createTag("Sleep")!!
        tagged("s1", msOn(12), sleep.id); tagged("u", msOn(19, 8), null); tagged("w", msOn(19, 9), work.id); tagged("s2", msOn(19, 10), sleep.id)
        vm.setMode(ViewMode.WEEK)
        vm.toggleFocus(Focus.OnTag(work.id))
        overviewWhere { it.focus != null }
        vm.previous()
        assertEquals(null, overviewWhere { it.anchor == today.minusDays(7) }.focus)

        vm.current()
        vm.toggleFocus(Focus.Unlabeled)
        overviewWhere { it.focus == Focus.Unlabeled && it.isCurrent }
        vm.previous() // Oct 12 – 18 is all Sleep: 100 % labeled
        assertEquals(null, overviewWhere { it.anchor == today.minusDays(7) }.focus)
    }
}
