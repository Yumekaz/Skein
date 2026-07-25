package com.skein.android.mesh

import com.skein.android.model.RoutedPacket
import com.skein.android.protocol.SkeinPacket

/**
 * Transport abstraction used by MeshCore to send packets via a specific medium.
 */
interface MeshTransport {
    val id: String

    fun broadcastPacket(routed: RoutedPacket)

    fun sendPacketToPeer(peerID: String, packet: SkeinPacket): Boolean

    fun cancelTransfer(transferId: String): Boolean = false

    fun getDeviceAddressForPeer(peerID: String): String? = null

    fun getDeviceAddressToPeerMapping(): Map<String, String> = emptyMap()

    fun getTransportDebugInfo(): String = ""
}
