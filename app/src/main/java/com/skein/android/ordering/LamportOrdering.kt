package com.skein.android.ordering

const val MAX_LAMPORT_COUNTER: Long = Long.MAX_VALUE / 2

/** A Lamport timestamp paired with the sender identity for deterministic ordering. */
data class LogicalTimestamp(
    val counter: Long,
    val nodeId: String
) : Comparable<LogicalTimestamp> {
    init {
        require(counter in 0..MAX_LAMPORT_COUNTER) { "Lamport counter is outside the supported range" }
        require(nodeId.isNotBlank()) { "Node ID cannot be blank" }
    }

    override fun compareTo(other: LogicalTimestamp): Int =
        compareValuesBy(this, other, LogicalTimestamp::counter, LogicalTimestamp::nodeId)
}

/** Thread-safe Lamport clock for one Skein node. */
class LamportClock(private val nodeId: String, initialCounter: Long = 0) {
    init {
        require(nodeId.isNotBlank()) { "Node ID cannot be blank" }
        require(initialCounter in 0..MAX_LAMPORT_COUNTER) { "Lamport counter is outside the supported range" }
    }

    private var counter = initialCounter

    @Synchronized
    fun tick(): LogicalTimestamp {
        require(counter < MAX_LAMPORT_COUNTER) { "Lamport counter exhausted" }
        counter += 1
        return LogicalTimestamp(counter, nodeId)
    }

    @Synchronized
    fun observe(remote: LogicalTimestamp): LogicalTimestamp {
        require(remote.counter < MAX_LAMPORT_COUNTER) { "Remote Lamport counter is too large" }
        counter = maxOf(counter, remote.counter) + 1
        return LogicalTimestamp(counter, nodeId)
    }

    @Synchronized
    fun current(): LogicalTimestamp = LogicalTimestamp(counter, nodeId)

}

/** Stable ordering helper used after messages have been reconciled from multiple paths. */
object LamportMessageOrdering {
    fun <T> sort(messages: Iterable<T>, timestamp: (T) -> LogicalTimestamp): List<T> =
        messages.sortedWith(compareBy(timestamp))
}
