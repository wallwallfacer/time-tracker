package dev.timetracker.phone.sync

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataItemAsset
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.Wearable
import dev.timetracker.core.DataLayerContract
import dev.timetracker.core.Source
import dev.timetracker.phone.data.AppDatabase
import dev.timetracker.phone.data.PointRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PointItemConsumerTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val dataClient = Wearable.getDataClient(context)
    private lateinit var db: AppDatabase
    private lateinit var repo: PointRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = PointRepository(db) { 1L }
    }

    @After fun tearDown() = db.close()

    private class FakeItem(private val path: String, private val bytes: ByteArray?) : DataItem {
        override fun getUri(): Uri = Uri.parse("wear://node$path")
        override fun getData(): ByteArray? = bytes
        override fun getAssets(): Map<String, DataItemAsset> = emptyMap()
        override fun setData(data: ByteArray?): DataItem = this
        override fun freeze(): DataItem = this
        override fun isDataValid(): Boolean = true
    }

    private fun pointItem(id: String, at: Long) = FakeItem(
        DataLayerContract.pointPath(id),
        DataMap().apply {
            putString(DataLayerContract.KEY_ID, id)
            putLong(DataLayerContract.KEY_AT, at)
            putString(DataLayerContract.KEY_SOURCE, Source.WATCH.name)
        }.toByteArray(),
    )

    @Test fun `valid item is inserted once and a duplicate is not`() = runTest {
        assertTrue(PointItemConsumer.consume(dataClient, repo, pointItem("u1", 42)))
        assertFalse(PointItemConsumer.consume(dataClient, repo, pointItem("u1", 42)))
        assertEquals(listOf(42L), repo.points.first().map { it.at })
    }

    @Test fun `unparseable item does not throw and inserts nothing`() = runTest {
        assertFalse(PointItemConsumer.consume(dataClient, repo, FakeItem("/point/bad", byteArrayOf(1, 2, 3))))
        assertTrue(repo.points.first().isEmpty())
    }

    @Test fun `other paths are ignored`() = runTest {
        assertFalse(PointItemConsumer.consume(dataClient, repo, FakeItem(DataLayerContract.LATEST_PATH, null)))
    }
}
