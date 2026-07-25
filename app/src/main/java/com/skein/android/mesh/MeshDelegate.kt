package com.skein.android.mesh

import com.skein.android.model.SkeinMessage

/**
 * Shared mesh delegate interface for transport-agnostic callbacks.
 */
interface MeshDelegate {
    fun didReceiveMessage(message: SkeinMessage)
    fun didUpdatePeerList(peers: List<String>)
    fun didReceiveChannelLeave(channel: String, fromPeer: String)
    fun didReceiveDeliveryAck(messageID: String, recipientPeerID: String)
    fun didReceiveReadReceipt(messageID: String, recipientPeerID: String)
    fun didReceiveVerifyChallenge(peerID: String, payload: ByteArray, timestampMs: Long) {}
    fun didReceiveVerifyResponse(peerID: String, payload: ByteArray, timestampMs: Long) {}
    fun decryptChannelMessage(encryptedContent: ByteArray, channel: String): String?
    fun getNickname(): String?
    fun isFavorite(peerID: String): Boolean
}
