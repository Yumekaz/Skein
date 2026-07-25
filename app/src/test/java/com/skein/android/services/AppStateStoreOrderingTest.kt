package com.skein.android.services

import com.skein.android.model.SkeinMessage
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppStateStoreOrderingTest {
    @Before
    fun reset() {
        AppStateStore.clear()
    }

    @Test
    fun `public messages are ordered by logical timestamp regardless of arrival`() {
        val late = message("late", 4, "node-b")
        val early = message("early", 2, "node-a")

        AppStateStore.addPublicMessage(late)
        AppStateStore.addPublicMessage(early)

        assertEquals(listOf("early", "late"), AppStateStore.publicMessages.value.map { it.content })
    }

    @Test
    fun `equal logical counters use node id tie break`() {
        AppStateStore.addPublicMessage(message("b", 5, "node-b"))
        AppStateStore.addPublicMessage(message("a", 5, "node-a"))

        assertEquals(listOf("a", "b"), AppStateStore.publicMessages.value.map { it.content })
    }

    @Test
    fun `legacy messages use timestamp and id fallback`() {
        AppStateStore.addPublicMessage(message("newer", null, null, 2_000))
        AppStateStore.addPublicMessage(message("older", null, null, 1_000))

        assertEquals(listOf("older", "newer"), AppStateStore.publicMessages.value.map { it.content })
    }

    @Test
    fun `duplicate delivery through multiple transport paths is ignored`() {
        val message = message("same", 1, "node-a")

        AppStateStore.addPublicMessage(message)
        AppStateStore.addPublicMessage(message)

        assertEquals(1, AppStateStore.publicMessages.value.size)
    }

    @Test
    fun `private and channel stores use the same logical ordering`() {
        val late = message("late", 2, "node-b")
        val early = message("early", 1, "node-a")

        AppStateStore.addPrivateMessage("peer", late)
        AppStateStore.addPrivateMessage("peer", early)
        AppStateStore.addChannelMessage("#mesh", late.copy(id = "late-channel"))
        AppStateStore.addChannelMessage("#mesh", early.copy(id = "early-channel"))

        assertEquals(listOf("early", "late"), AppStateStore.privateMessages.value["peer"]!!.map { it.content })
        assertEquals(listOf("early", "late"), AppStateStore.channelMessages.value["#mesh"]!!.map { it.content })
    }

    private fun message(content: String, counter: Long?, node: String?, timestamp: Long = 1_000): SkeinMessage =
        SkeinMessage(
            id = content,
            sender = node ?: "legacy",
            content = content,
            timestamp = Date(timestamp),
            senderPeerID = node,
            logicalCounter = counter,
            logicalNodeId = node
        )
}
