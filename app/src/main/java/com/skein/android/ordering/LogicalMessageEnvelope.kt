package com.skein.android.ordering

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Optional envelope for public mesh message payloads. Legacy payloads remain plain UTF-8. */
data class LogicalMessageEnvelope(val timestamp: LogicalTimestamp, val content: ByteArray) {
    fun encode(): ByteArray {
        val node = timestamp.nodeId.toByteArray(Charsets.UTF_8)
        require(node.isNotEmpty() && node.size <= 255)
        return ByteBuffer.allocate(MAGIC.size + 8 + 1 + node.size + content.size)
            .order(ByteOrder.BIG_ENDIAN)
            .put(MAGIC)
            .putLong(timestamp.counter)
            .put(node.size.toByte())
            .put(node)
            .put(content)
            .array()
    }

    companion object {
        private val MAGIC = byteArrayOf(0x53, 0x4B, 0x4F, 0x31) // SKO1

        fun decode(data: ByteArray): LogicalMessageEnvelope? {
            if (data.size < MAGIC.size + 9 || !data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) return null
            return runCatching {
                val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
                val magic = ByteArray(MAGIC.size)
                buffer.get(magic)
                val counter = buffer.long
                val nodeSize = buffer.get().toUByte().toInt()
                require(counter in 0 until MAX_LAMPORT_COUNTER && nodeSize in 1..255 && buffer.remaining() >= nodeSize)
                val nodeBytes = ByteArray(nodeSize)
                buffer.get(nodeBytes)
                LogicalMessageEnvelope(
                    LogicalTimestamp(counter, String(nodeBytes, Charsets.UTF_8)),
                    ByteArray(buffer.remaining()).also(buffer::get)
                )
            }.getOrNull()
        }

        fun encodeIfValid(timestamp: LogicalTimestamp?, content: ByteArray): ByteArray =
            if (timestamp == null) content else LogicalMessageEnvelope(timestamp, content).encode()
    }
}

object LamportClockRegistry {
    private val clocks = mutableMapOf<String, LamportClock>()

    @Synchronized
    fun forNode(nodeId: String): LamportClock = clocks.getOrPut(nodeId) { LamportClock(nodeId) }
}
