package dev.timetracker.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlocksTest {
    private fun p(id: String, at: Long, tag: String? = null, created: Long = at) =
        Point(id, at, Source.WATCH, tag, null, created)

    @Test fun `no points gives no blocks`() = assertEquals(emptyList<Block>(), deriveBlocks(emptyList(), 1000))
    @Test fun `one point gives one current block ending now`() {
        val b = deriveBlocks(listOf(p("a", 100)), 1000)
        assertEquals(1, b.size); assertTrue(b[0].isCurrent); assertEquals(900, b[0].durationMs)
    }
    @Test fun `blocks follow sorted order regardless of input order`() {
        val b = deriveBlocks(listOf(p("b", 200), p("a", 100)), 1000)
        assertEquals(listOf("a", "b"), b.map { it.start.id }); assertEquals(100, b[0].durationMs)
    }
    @Test fun `equal at gives zero length block ordered by createdAt then id`() {
        val b = deriveBlocks(listOf(p("y", 100, created = 5), p("x", 100, created = 5), p("z", 100, created = 1)), 1000)
        assertEquals(listOf("z", "x", "y"), b.map { it.start.id }); assertEquals(0, b[0].durationMs)
    }
    @Test fun `block label is its start point label`() {
        val b = deriveBlocks(listOf(p("a", 100, tag = "work"), p("b", 200)), 1000)
        assertEquals("work", b[0].start.tagId); assertNull(b[1].start.tagId)
    }
    @Test fun `late_point_splits_block_without_copying_label`() {
        val before = deriveBlocks(listOf(p("a", 100, tag = "work"), p("c", 300)), 1000)
        val after = deriveBlocks(listOf(p("a", 100, tag = "work"), p("c", 300), p("b", 200)), 1000)
        assertEquals("work", after[0].start.tagId); assertNull(after[1].start.tagId)
        assertEquals(before[0].start, after[0].start); assertEquals(100, after[0].durationMs)
    }
}
