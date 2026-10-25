package dev.timetracker.phone.ui

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.wearable.Wearable
import dev.timetracker.core.Block
import dev.timetracker.core.Source
import dev.timetracker.core.deriveBlocks
import dev.timetracker.core.midpoint
import dev.timetracker.phone.data.AppDatabase
import dev.timetracker.phone.data.PointRepository
import dev.timetracker.phone.sync.LatestPublisher
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
class LabelSheetViewModelTest {
    private val now = 1_000L

    private lateinit var db: AppDatabase
    private lateinit var repo: PointRepository
    private lateinit var vm: LabelSheetViewModel

    @Before fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = PointRepository(db) { now }
        vm = LabelSheetViewModel(repo, LatestPublisher(Wearable.getDataClient(context))) { now }
    }

    @After fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    /** Marks a@100, b@200, c@300 and returns the derived blocks (a, b, c-current). */
    private suspend fun threeBlocks(): List<Block> {
        repo.mark(Source.PHONE, 100, "a"); repo.mark(Source.PHONE, 200, "b"); repo.mark(Source.PHONE, 300, "c")
        return deriveBlocks(repo.points.first(), now)
    }

    @Test fun `applyTag sets start point tag`() = runTest {
        val block = threeBlocks()[1]
        val tag = repo.createTag("Work")!!
        vm.applyTag(block, tag.id)
        val b = repo.points.first { p -> p.first { it.id == "b" }.tagId == tag.id }.first { it.id == "b" }
        assertEquals(tag.id, b.tagId)
        assertNull(repo.points.first().first { it.id == "a" }.tagId)
    }

    @Test fun `createAndApply rejects duplicate name`() = runTest {
        val block = threeBlocks()[1]
        repo.createTag("Work")
        assertFalse(vm.createAndApply(block, "work"))
        assertEquals(1, repo.tags.first().size)
        assertNull(repo.points.first().first { it.id == "b" }.tagId)

        assertTrue(vm.createAndApply(block, "Play"))
        val play = repo.tags.first().first { it.name == "Play" }
        assertEquals(play.id, repo.points.first { p -> p.first { it.id == "b" }.tagId != null }.first { it.id == "b" }.tagId)
    }

    @Test fun `createAndApply rejects blank name`() = runTest {
        val block = threeBlocks()[1]
        assertFalse(vm.createAndApply(block, "   "))
        assertTrue(repo.tags.first().isEmpty())
        assertNull(repo.points.first().first { it.id == "b" }.tagId)
    }

    @Test fun `moveStart clamps to bounds`() = runTest {
        val block = threeBlocks()[1]
        vm.moveStart(block, 50)
        val moved = repo.points.first { p -> p.first { it.id == "b" }.at != 200L }.first { it.id == "b" }
        assertEquals(100L, moved.at)
    }

    @Test fun `insertMidpoint creates unlabeled point at midpoint`() = runTest {
        val first = threeBlocks()[0]
        val block = first.copy(start = first.start.copy(tagId = "t"))
        val created = vm.insertMidpoint(block)
        assertEquals(midpoint(block), created.at)
        assertEquals(150L, created.at)
        assertNull(created.tagId)
        assertEquals(Source.MANUAL, created.source)
        assertEquals(4, repo.points.first().size)
    }

    @Test fun `endBounds is null for current block`() = runTest {
        val blocks = threeBlocks()
        val all = repo.points.first()
        assertNull(vm.endBounds(blocks[2], all))
        assertEquals(200L..now, vm.endBounds(blocks[1], all))
        assertEquals(100L..300L, vm.startBounds(blocks[1], all))
    }
}
