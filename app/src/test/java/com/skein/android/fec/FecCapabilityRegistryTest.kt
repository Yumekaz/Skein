package com.skein.android.fec

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FecCapabilityRegistryTest {
    @Before fun reset() = FecCapabilityRegistry.clear()

    @Test fun `same version enables FEC`() {
        FecCapabilityRegistry.update("skein-peer", FecConfig.VERSION)
        assertTrue(FecCapabilityRegistry.supports("skein-peer"))
    }

    @Test fun `legacy and mismatched versions use fallback`() {
        FecCapabilityRegistry.update("legacy", null)
        FecCapabilityRegistry.update("newer", FecConfig.VERSION + 1)
        assertFalse(FecCapabilityRegistry.supports("legacy"))
        assertFalse(FecCapabilityRegistry.supports("newer"))
    }

    @Test fun `capability downgrade disables FEC`() {
        FecCapabilityRegistry.update("peer", FecConfig.VERSION)
        FecCapabilityRegistry.update("peer", FecConfig.VERSION + 1)
        assertFalse(FecCapabilityRegistry.supports("peer"))
    }
}
