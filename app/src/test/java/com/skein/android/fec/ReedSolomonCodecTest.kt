package com.skein.android.fec

import java.util.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ReedSolomonCodecTest {
    private val codec = ReedSolomonCodec(4, 2)

    @Test fun `no loss preserves data`() {
        val data = data(4, 64)
        val recovered = codec.reconstruct(codec.encode(data).map { it.copyOf() }.toTypedArray())
        data.indices.forEach { assertArrayEquals(data[it], recovered[it]) }
    }

    @Test fun `recovers exactly parity count erasures`() {
        val data = data(4, 64)
        val shards = codec.encode(data).map { it.copyOf() }.toTypedArray<ByteArray?>()
        shards[1] = null; shards[5] = null
        val recovered = codec.reconstruct(shards)
        data.indices.forEach { assertArrayEquals(data[it], recovered[it]) }
    }

    @Test fun `fails beyond correction capacity`() {
        val shards = codec.encode(data(4, 8)).map { it.copyOf() }.toTypedArray<ByteArray?>()
        shards[0] = null; shards[1] = null; shards[4] = null
        assertThrows(IllegalArgumentException::class.java) { codec.reconstruct(shards) }
    }

    @Test fun `fec payload rejects corruption`() {
        val encoded = FecFragmentPayload(ByteArray(8) { 1 }, 0, 0, 8, 4, 20, 2u, ByteArray(16) { 5 }).encode()
        encoded[encoded.lastIndex] = 7
        assertNull(FecFragmentPayload.decode(encoded))
    }

    private fun data(count: Int, size: Int): List<ByteArray> {
        val random = Random(7)
        return List(count) { ByteArray(size).also { random.nextBytes(it) } }
    }
}
