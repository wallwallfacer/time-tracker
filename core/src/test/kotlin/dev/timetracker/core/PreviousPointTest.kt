package dev.timetracker.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreviousPointTest {
    @Test fun `newest candidate wins`() = assertEquals(300L, previousPoint(1000, 100, 300, null))

    @Test fun `no candidates gives null`() = assertNull(previousPoint(1000, null, null))

    @Test fun `the press itself and later times are ignored`() = assertEquals(100L, previousPoint(1000, 100, 1000, 1500))
}
