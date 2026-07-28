package com.skein.android.fec

object FecConfig {
    const val VERSION = 1
    const val DATA_SHARDS = 8
    const val PARITY_SHARDS = 4
    /** Kept below the BLE MTU after packet, FEC-header, and padding overhead. */
    const val MAX_SHARD_BYTES = 400
    const val MAX_BLOCK_BYTES = DATA_SHARDS * MAX_SHARD_BYTES
    const val MAX_TRANSFER_BYTES = 1_048_576
    const val MAX_BLOCKS_PER_TRANSFER =
        (MAX_TRANSFER_BYTES + MAX_BLOCK_BYTES - 1) / MAX_BLOCK_BYTES
    const val CAPABILITY_BIT = 0x01
    val codec = ReedSolomonCodec(DATA_SHARDS, PARITY_SHARDS)
}
