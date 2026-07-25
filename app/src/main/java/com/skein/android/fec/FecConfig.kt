package com.skein.android.fec

object FecConfig {
    const val VERSION = 1
    const val DATA_SHARDS = 8
    const val PARITY_SHARDS = 4
    const val MAX_BLOCK_BYTES = 8 * 469
    const val CAPABILITY_BIT = 0x01
    val codec = ReedSolomonCodec(DATA_SHARDS, PARITY_SHARDS)
}
