package com.skein.android.fec

import java.util.Random

/** Debug-build-only deterministic loss injector for FEC demonstrations. */
object FecDebugPacketLoss {
    @Volatile private var lossPercent: Int = 0
    @Volatile private var random = Random(0L)

    fun configure(percent: Int, seed: Long = 0L) {
        lossPercent = percent.coerceIn(0, 90)
        random = Random(seed)
    }

    fun shouldDrop(): Boolean = lossPercent > 0 && random.nextInt(100) < lossPercent
}
