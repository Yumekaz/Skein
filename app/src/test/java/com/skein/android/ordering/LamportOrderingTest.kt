package com.skein.android.ordering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LamportOrderingTest {
    @Test
    fun `local events advance the clock`() {
        val clock = LamportClock("a")

        assertEquals(LogicalTimestamp(1, "a"), clock.tick())
        assertEquals(LogicalTimestamp(2, "a"), clock.tick())
    }

    @Test
    fun `receiving a remote event advances beyond it`() {
        val clock = LamportClock("local", initialCounter = 2)

        assertEquals(LogicalTimestamp(8, "local"), clock.observe(LogicalTimestamp(7, "remote")))
        assertEquals(LogicalTimestamp(9, "local"), clock.tick())
    }

    @Test
    fun `partition merge produces deterministic total order`() {
        val messages = listOf(
            LogicalTimestamp(4, "node-b"),
            LogicalTimestamp(2, "node-a"),
            LogicalTimestamp(3, "node-a"),
            LogicalTimestamp(1, "node-b")
        )

        val ordered = LamportMessageOrdering.sort(messages) { it }

        assertEquals(
            listOf(
                LogicalTimestamp(1, "node-b"),
                LogicalTimestamp(2, "node-a"),
                LogicalTimestamp(3, "node-a"),
                LogicalTimestamp(4, "node-b")
            ),
            ordered
        )
    }

    @Test
    fun `node id breaks equal counter ties`() {
        val left = LogicalTimestamp(10, "node-a")
        val right = LogicalTimestamp(10, "node-b")

        assertTrue(left < right)
    }
}
