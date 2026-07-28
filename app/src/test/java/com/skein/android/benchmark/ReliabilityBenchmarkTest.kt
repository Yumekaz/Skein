package com.skein.android.benchmark

import com.skein.android.fec.FecConfig
import com.skein.android.ordering.LamportMessageOrdering
import com.skein.android.ordering.LogicalTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Random

/**
 * Deterministic, headless evidence generator. Run with:
 * ./gradlew testDebugUnitTest --tests com.skein.android.benchmark.ReliabilityBenchmarkTest
 *
 * It deliberately has no Android or BLE dependency.  Results are written to
 * build/reports/reliability/ as CSV and JSON so the data can be graphed or
 * compared in CI without a physical radio.
 */
class ReliabilityBenchmarkTest {
    @Test
    fun `write deterministic ordering and FEC benchmark evidence`() {
        val output = File("build/reports/reliability").apply { mkdirs() }
        val ordering = orderingEvidence()
        val fec = listOf(0.30, 0.40).map { loss -> fecEvidence(loss, attempts = 400, seed = 20_260L + (loss * 100).toLong()) }

        File(output, "ordering.csv").writeText(
            "events,arrival_order_violations,lamport_order_violations\n" +
                "${ordering.events},${ordering.arrivalViolations},${ordering.lamportViolations}\n"
        )
        File(output, "fec.csv").writeText(
            buildString {
                appendLine("loss_rate,attempts,baseline_deliveries,fec_deliveries")
                fec.forEach { appendLine("${it.lossRate},${it.attempts},${it.baselineDeliveries},${it.fecDeliveries}") }
            }
        )
        File(output, "reliability.json").writeText(
            """{
              |  "ordering": {"events": ${ordering.events}, "arrivalOrderViolations": ${ordering.arrivalViolations}, "lamportOrderViolations": ${ordering.lamportViolations}},
              |  "fec": [${fec.joinToString(",") { "{\"lossRate\":${it.lossRate},\"attempts\":${it.attempts},\"baselineDeliveries\":${it.baselineDeliveries},\"fecDeliveries\":${it.fecDeliveries}}" }}]
              |}
            """.trimMargin()
        )

        assertTrue("shuffled delivery should expose ordering violations", ordering.arrivalViolations > 0)
        assertEquals("Lamport ordering must converge", 0, ordering.lamportViolations)
        fec.forEach { result ->
            assertTrue("FEC must improve delivery at ${result.lossRate}", result.fecDeliveries > result.baselineDeliveries)
        }
    }

    private fun orderingEvidence(): OrderingEvidence {
        val logical = buildList {
            repeat(100) { add(LogicalTimestamp((it + 1).toLong(), "partition-a")) }
            repeat(100) { add(LogicalTimestamp((it + 1).toLong(), "partition-b")) }
        }
        val arrival = logical.shuffled(Random(7))
        val reconciled = LamportMessageOrdering.sort(arrival) { it }
        return OrderingEvidence(
            events = logical.size,
            arrivalViolations = inversions(arrival),
            lamportViolations = inversions(reconciled)
        )
    }

    private fun fecEvidence(lossRate: Double, attempts: Int, seed: Long): FecEvidence {
        val random = Random(seed)
        val payload = ByteArray(1_800) { (it * 31).toByte() }
        val shardSize = (payload.size + FecConfig.DATA_SHARDS - 1) / FecConfig.DATA_SHARDS
        val data = List(FecConfig.DATA_SHARDS) { index ->
            ByteArray(shardSize).also { shard ->
                val offset = index * shardSize
                if (offset < payload.size) System.arraycopy(payload, offset, shard, 0, minOf(shardSize, payload.size - offset))
            }
        }
        val encoded = FecConfig.codec.encode(data)
        var baseline = 0
        var recovered = 0
        repeat(attempts) {
            val received = encoded.map { shard -> if (random.nextDouble() >= lossRate) shard.copyOf() else null }
            if (received.all { it != null }) baseline += 1
            val rebuilt = runCatching { FecConfig.codec.reconstruct(received.toTypedArray()) }.getOrNull()
            if (rebuilt != null) {
                val result = rebuilt.take(FecConfig.DATA_SHARDS).flatMap { it.asIterable() }.take(payload.size).toByteArray()
                if (result.contentEquals(payload)) recovered += 1
            }
        }
        return FecEvidence(lossRate, attempts, baseline, recovered)
    }

    private fun inversions(sequence: List<LogicalTimestamp>): Int =
        sequence.zipWithNext().count { (left, right) -> left > right }

    private fun <T> List<T>.shuffled(random: Random): List<T> = toMutableList().also {
        for (index in it.lastIndex downTo 1) {
            val target = random.nextInt(index + 1)
            val value = it[index]
            it[index] = it[target]
            it[target] = value
        }
    }

    private data class OrderingEvidence(val events: Int, val arrivalViolations: Int, val lamportViolations: Int)
    private data class FecEvidence(val lossRate: Double, val attempts: Int, val baselineDeliveries: Int, val fecDeliveries: Int)
}
