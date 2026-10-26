package dev.timetracker.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchDecisionTest {
    @Test fun `fresh create records`() = assertTrue(LaunchDecision.shouldRecord(true, false))

    @Test fun `recreated activity does not record`() = assertFalse(LaunchDecision.shouldRecord(false, false))

    @Test fun `mark_on_new_intent_records_second_point`() = assertTrue(LaunchDecision.shouldRecord(false, true))
}
