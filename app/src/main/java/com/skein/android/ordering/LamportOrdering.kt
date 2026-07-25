package com.skein.android.ordering

/** A Lamport timestamp paired with the sender identity for deterministic ordering. */
data class LogicalTimestamp(
    val counter: Long,
    val nodeId: String
) : Comparable<LogicalTimestamp> {
    init {
        require(counter >= 0) { "Lamport counter cannot be negative" }
        require(nodeId.isNotBlank()) { "Node ID cannot be blank" }
    }

    override fun compareTo(other: LogicalTimestamp): Int =
        compareValuesBy(this, other, LogicalTimestamp::counter, LogicalTimestamp::nodeId)
}

/** Thread-safe Lamport clock for one Skein node. */
class LamportClock(private val nodeId: String, initialCounter: Long = 0) {
    init {
        require(nodeId.isNotBlank()) { "Node ID cannot be blank" }
        require(initialCounter >= 0) { "Lamport counter cannot be negative" }
    }

    private var counter = initialCounter

    @Synchronized
    fun tick(): LogicalTimestamp {
        counter += 1
        return LogicalTimestamp(counter, nodeId)
    }

    @Synchronized
    fun observe(remote: LogicalTimestamp): LogicalTimestamp {
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
