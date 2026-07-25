package com.skein.android.services

import com.skein.android.model.SkeinMessage
import com.skein.android.model.DeliveryStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide in-memory state store that survives Activity recreation.
 * The foreground Mesh service updates this store; UI subscribes/hydrates from it.
 */
object AppStateStore {
    // Global de-dup set by message id to avoid duplicate keys in Compose lists
    private val seenMessageIds = mutableSetOf<String>()
    private val seenPublicMessageKeys = mutableSetOf<String>()
    private val peerIdsByTransport = mutableMapOf<String, Set<String>>()
    // Direct (single-hop) peer IDs per transport, used to gossip a unified neighbor set.
    private val directPeerIdsByTransport = mutableMapOf<String, Set<String>>()
    // Connected peer IDs (mesh ephemeral IDs)
    private val _peers = MutableStateFlow<List<String>>(emptyList())
    val peers: StateFlow<List<String>> = _peers.asStateFlow()

    // Public mesh timeline messages (non-channel)
    private val _publicMessages = MutableStateFlow<List<SkeinMessage>>(emptyList())
    val publicMessages: StateFlow<List<SkeinMessage>> = _publicMessages.asStateFlow()

    // Private messages by peerID
    private val _privateMessages = MutableStateFlow<Map<String, List<SkeinMessage>>>(emptyMap())
    val privateMessages: StateFlow<Map<String, List<SkeinMessage>>> = _privateMessages.asStateFlow()

    // Channel messages by channel name
    private val _channelMessages = MutableStateFlow<Map<String, List<SkeinMessage>>>(emptyMap())
    val channelMessages: StateFlow<Map<String, List<SkeinMessage>>> = _channelMessages.asStateFlow()

    fun setPeers(ids: List<String>) {
        synchronized(this) {
            _peers.value = ids.distinct()
        }
    }

    fun setTransportPeers(transportId: String, ids: List<String>) {
        synchronized(this) {
            peerIdsByTransport[transportId] = ids.toSet()
            publishTransportPeersLocked()
        }
    }

    fun clearTransportPeers(transportId: String) {
        synchronized(this) {
            peerIdsByTransport.remove(transportId)
            publishTransportPeersLocked()
        }
    }

    private fun publishTransportPeersLocked() {
        _peers.value = peerIdsByTransport.values
            .asSequence()
            .flatten()
            .distinct()
            .toList()
    }

    /**
     * Record the set of direct (single-hop) peers reachable over a given transport. Each transport
     * (BLE, Wi-Fi Aware, ...) only knows its own direct peers; [getDirectPeers] unions them so every
     * transport can gossip the same complete neighbor list under our shared node identity.
     */
    fun setTransportDirectPeers(transportId: String, ids: Collection<String>) {
        synchronized(this) {
            directPeerIdsByTransport[transportId] = ids.toSet()
        }
    }

    fun clearTransportDirectPeers(transportId: String) {
        synchronized(this) {
            directPeerIdsByTransport.remove(transportId)
        }
    }

    /** Union of direct peers across all transports. */
    fun getDirectPeers(): Set<String> {
        synchronized(this) {
            return directPeerIdsByTransport.values.flatten().toSet()
        }
    }

    fun addPublicMessage(msg: SkeinMessage) {
        synchronized(this) {
            val publicKey = publicMessageKey(msg)
            if (seenMessageIds.contains(msg.id) || seenPublicMessageKeys.contains(publicKey)) return
            seenMessageIds.add(msg.id)
            seenPublicMessageKeys.add(publicKey)
            _publicMessages.value = ordered(_publicMessages.value + msg)
        }
    }

    fun addPrivateMessage(peerID: String, msg: SkeinMessage) {
        synchronized(this) {
            if (seenMessageIds.contains(msg.id)) return
            seenMessageIds.add(msg.id)
            val map = _privateMessages.value.toMutableMap()
            map[peerID] = ordered((map[peerID] ?: emptyList()) + msg)
            _privateMessages.value = map
        }
    }

    private fun statusPriority(status: DeliveryStatus?): Int = when (status) {
        null -> 0
        is DeliveryStatus.Sending -> 1
        is DeliveryStatus.Sent -> 2
        is DeliveryStatus.PartiallyDelivered -> 3
        is DeliveryStatus.Delivered -> 4
        is DeliveryStatus.Read -> 5
        is DeliveryStatus.Failed -> 0
    }

    fun updatePrivateMessageStatus(messageID: String, status: DeliveryStatus) {
        synchronized(this) {
            val map = _privateMessages.value.toMutableMap()
            var changed = false
            map.keys.toList().forEach { peer ->
                val list = map[peer]?.toMutableList() ?: mutableListOf()
                val idx = list.indexOfFirst { it.id == messageID }
                if (idx >= 0) {
                    val current = list[idx].deliveryStatus
                    // Do not downgrade (e.g., Read -> Delivered)
                    if (statusPriority(status) >= statusPriority(current)) {
                        list[idx] = list[idx].copy(deliveryStatus = status)
                        map[peer] = list
                        changed = true
                    }
                }
            }
            if (changed) {
                _privateMessages.value = map
            }
        }
    }

    fun addChannelMessage(channel: String, msg: SkeinMessage) {
        synchronized(this) {
            if (seenMessageIds.contains(msg.id)) return
            seenMessageIds.add(msg.id)
            val map = _channelMessages.value.toMutableMap()
            map[channel] = ordered((map[channel] ?: emptyList()) + msg)
            _channelMessages.value = map
        }
    }

    // Clear all in-memory state (used for full app shutdown)
    fun clear() {
        synchronized(this) {
            seenMessageIds.clear()
            seenPublicMessageKeys.clear()
            peerIdsByTransport.clear()
            directPeerIdsByTransport.clear()
            _peers.value = emptyList()
            _publicMessages.value = emptyList()
            _privateMessages.value = emptyMap()
            _channelMessages.value = emptyMap()
        }
    }

    private fun publicMessageKey(msg: SkeinMessage): String {
        val sender = msg.senderPeerID ?: msg.sender
        return listOf(
            sender,
            msg.timestamp.time.toString(),
            msg.type.name,
            msg.channel ?: "",
            msg.content
        ).joinToString("\u001F")
    }

    /**
     * Orders messages deterministically when both sides carry Lamport metadata.
     * Legacy messages retain the existing timestamp/id fallback and are kept after
     * fully ordered messages so old peers remain visible without inventing causality.
     */
    private fun ordered(messages: List<SkeinMessage>): List<SkeinMessage> {
        return messages.sortedWith(Comparator { left, right ->
            val leftCounter = left.logicalCounter
            val rightCounter = right.logicalCounter
            val leftNode = left.logicalNodeId
            val rightNode = right.logicalNodeId
            if (leftCounter != null && rightCounter != null && !leftNode.isNullOrBlank() && !rightNode.isNullOrBlank()) {
                leftCounter.compareTo(rightCounter).takeIf { it != 0 }
                    ?: leftNode.compareTo(rightNode).takeIf { it != 0 }
                    ?: left.timestamp.compareTo(right.timestamp)
            } else {
                left.timestamp.compareTo(right.timestamp).takeIf { it != 0 }
                    ?: left.id.compareTo(right.id)
            }
        })
    }
}
