package com.skein.android.ordering

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogicalMessageEnvelopeTest {
    @Test
    fun `envelope round trips logical metadata and content`() {
        val original = LogicalMessageEnvelope(LogicalTimestamp(42, "node-a"), "hello".toByteArray())

        val decoded = LogicalMessageEnvelope.decode(original.encode())

        requireNotNull(decoded)
        assertEquals(original.timestamp, decoded.timestamp)
        assertArrayEquals(original.content, decoded.content)
    }

    @Test
    fun `legacy payload is not interpreted as an envelope`() {
        assertNull(LogicalMessageEnvelope.decode("legacy".toByteArray()))
    }

    @Test
    fun `negative counter is rejected`() {
        val encoded = java.nio.ByteBuffer.allocate(4 + 8 + 1 + 4)
            .put(byteArrayOf(0x53, 0x4B, 0x4F, 0x31))
            .putLong(-1)
            .put(4)
            .put("node".toByteArray())
            .array()

        assertNull(LogicalMessageEnvelope.decode(encoded))
    }
}
