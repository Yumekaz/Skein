package com.skein.android.fec

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

/** Versioned opt-in FEC fragment payload. It is never decoded as a legacy [FragmentPayload]. */
data class FecFragmentPayload(
    val transferId: ByteArray,
    val blockIndex: Int,
    val shardIndex: Int,
    val dataShards: Int,
    val parityShards: Int,
    val originalLength: Int,
    val originalType: UByte,
    val shard: ByteArray
) {
    init {
        require(transferId.size == TRANSFER_ID_BYTES)
        require(blockIndex >= 0 && shardIndex >= 0)
        require(dataShards > 0 && parityShards > 0 && shardIndex < dataShards + parityShards)
        require(originalLength >= 0 && shard.isNotEmpty())
    }

    fun encode(): ByteArray {
        val crc = CRC32().apply { update(shard) }.value.toInt()
        return ByteBuffer.allocate(HEADER_SIZE + shard.size).order(ByteOrder.BIG_ENDIAN)
            .put(MAGIC).put(FecConfig.VERSION.toByte()).put(transferId)
            .putShort(blockIndex.toShort()).put(shardIndex.toByte())
            .put(dataShards.toByte()).put(parityShards.toByte()).putInt(originalLength)
            .put(originalType.toByte()).putInt(crc).put(shard).array()
    }

    companion object {
        private val MAGIC = byteArrayOf(0x53, 0x46, 0x45, 0x43) // SFEC
        const val TRANSFER_ID_BYTES = 8
        const val FINAL_BLOCK_FLAG = 0x8000
        const val BLOCK_INDEX_MASK = 0x7fff
        const val HEADER_SIZE = 4 + 1 + TRANSFER_ID_BYTES + 2 + 1 + 1 + 1 + 4 + 1 + 4

        fun decode(bytes: ByteArray): FecFragmentPayload? = runCatching {
            require(bytes.size > HEADER_SIZE)
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            val magic = ByteArray(4); buffer.get(magic); require(magic.contentEquals(MAGIC))
            require(buffer.get().toInt() == FecConfig.VERSION)
            val id = ByteArray(TRANSFER_ID_BYTES); buffer.get(id)
            val block = buffer.short.toInt() and 0xffff
            val shardIndex = buffer.get().toInt() and 0xff
            val data = buffer.get().toInt() and 0xff
            val parity = buffer.get().toInt() and 0xff
            val originalLength = buffer.int
            val type = buffer.get().toUByte()
            val checksum = buffer.int
            val shard = ByteArray(buffer.remaining()); buffer.get(shard)
            require(data == FecConfig.DATA_SHARDS && parity == FecConfig.PARITY_SHARDS)
            require(originalLength in 0..FecConfig.MAX_TRANSFER_BYTES)
            require(shardIndex < data + parity)
            require(CRC32().apply { update(shard) }.value.toInt() == checksum)
            FecFragmentPayload(id, block, shardIndex, data, parity, originalLength, type, shard)
        }.getOrNull()
    }

    /** Index inside a transfer. The high bit of [blockIndex] marks its last block. */
    val transferBlockIndex: Int get() = blockIndex and BLOCK_INDEX_MASK
    val isFinalBlock: Boolean get() = blockIndex and FINAL_BLOCK_FLAG != 0
}
