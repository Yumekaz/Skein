package com.skein.android.fec

import com.skein.android.model.IdentityAnnouncement
import java.util.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FecFoundationTest {
    @Test fun `codec recovers two missing shards`() {
        val codec = ReedSolomonCodec(4, 2)
        val random = Random(4)
        val data = List(4) { ByteArray(64).also(random::nextBytes) }
        val shards = codec.encode(data).map { it.copyOf() }.toTypedArray<ByteArray?>()
        shards[0] = null; shards[5] = null
        val recovered = codec.reconstruct(shards)
        data.indices.forEach { assertArrayEquals(data[it], recovered[it]) }
    }

    @Test fun `corrupted fec payload is rejected`() {
        val raw = FecFragmentPayload(ByteArray(8), 0, 0, 8, 4, 12, 2u, ByteArray(8) { 3 }).encode()
        raw[raw.lastIndex] = 4
        assertNull(FecFragmentPayload.decode(raw))
    }

    @Test fun `announcement advertises optional fec version`() {
        val original = IdentityAnnouncement("a", ByteArray(32), ByteArray(32), FecConfig.VERSION)
        assertEquals(FecConfig.VERSION, IdentityAnnouncement.decode(requireNotNull(original.encode()))?.fecVersion)
    }
}
