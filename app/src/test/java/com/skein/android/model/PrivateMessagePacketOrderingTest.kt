package com.skein.android.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PrivateMessagePacketOrderingTest {
    @Test
    fun `legacy private packet remains compatible`() {
        val original = PrivateMessagePacket("id", "hello")

        assertEquals(original, PrivateMessagePacket.decode(requireNotNull(original.encode())))
    }

    @Test
    fun `logical metadata round trips in private packet`() {
        val original = PrivateMessagePacket("id", "hello", 12, "node-a")

        assertEquals(original, PrivateMessagePacket.decode(requireNotNull(original.encode())))
    }
}
