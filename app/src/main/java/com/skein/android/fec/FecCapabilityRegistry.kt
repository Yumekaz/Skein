package com.skein.android.fec

import java.util.concurrent.ConcurrentHashMap

/** In-memory capability state learned only from verified signed announcements. */
object FecCapabilityRegistry {
    private val peerVersions = ConcurrentHashMap<String, Int>()
    fun update(peerId: String, version: Int?) { if (version == FecConfig.VERSION) peerVersions[peerId] = version else peerVersions.remove(peerId) }
    fun supports(peerId: String): Boolean = peerVersions[peerId] == FecConfig.VERSION
    fun remove(peerId: String) { peerVersions.remove(peerId) }
    fun clear() { peerVersions.clear() }
}
