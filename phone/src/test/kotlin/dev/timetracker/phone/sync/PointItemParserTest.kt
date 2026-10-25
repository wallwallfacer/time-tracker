package dev.timetracker.phone.sync

import dev.timetracker.core.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PointItemParserTest {
    @Test fun `parses valid point`() = assertEquals(
        IncomingPoint("u1", 123L, Source.WATCH),
        PointItemParser.parse("/point/u1", mapOf("id" to "u1", "at" to 123L, "source" to "WATCH")),
    )

    @Test fun `rejects other path`() = assertNull(PointItemParser.parse("/latest", mapOf("at" to 1L)))

    @Test fun `rejects missing at`() =
        assertNull(PointItemParser.parse("/point/u1", mapOf("id" to "u1", "source" to "WATCH")))

    @Test fun `unknown source falls back to watch`() = assertEquals(
        IncomingPoint("u1", 123L, Source.WATCH),
        PointItemParser.parse("/point/u1", mapOf("id" to "u1", "at" to 123L, "source" to "BOGUS")),
    )

    @Test fun `accepts int at`() = assertEquals(
        IncomingPoint("u1", 7L, Source.PHONE),
        PointItemParser.parse("/point/u1", mapOf("id" to "u1", "at" to 7, "source" to "PHONE")),
    )
}
