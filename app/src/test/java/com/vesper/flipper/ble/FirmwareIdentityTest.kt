package com.vesper.flipper.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirmwareIdentityTest {

    @Test
    fun `unleashed banners are not labeled official`() {
        assertEquals(FirmwareFamily.UNLEASHED, FirmwareIdentity.detect("firmware_version=unlshd-093"))
        assertEquals(FirmwareFamily.UNLEASHED, FirmwareIdentity.detect("Unleashed"))
        assertFalse(FirmwareIdentity.looksOfficial("Flipper firmware"))
        assertFalse(FirmwareIdentity.looksOfficial("Flipper Umrleum"))
        assertNull(FirmwareIdentity.detect("Flipper firmware"))
    }

    @Test
    fun `stock version strings still count as official`() {
        assertTrue(FirmwareIdentity.looksOfficial("1.4.1"))
        assertTrue(FirmwareIdentity.looksOfficial("official"))
        assertFalse(FirmwareIdentity.looksOfficial("unlshd-093"))
    }
}

class SerialFlowControlTest {

    @Test
    fun `flow control prefers the byte-reversed uint32`() {
        // 1024 as big-endian, which is how firmware publishes REVERSE_BYTES_U32.
        assertEquals(1024, SerialFlowControl.remainingBytes(byteArrayOf(0x00, 0x00, 0x04, 0x00)))
    }

    @Test
    fun `flow control accepts a little-endian reading when big-endian is absurd`() {
        // 1024 little-endian. Big-endian of these bytes is 262144, past the serial buffer.
        assertEquals(1024, SerialFlowControl.remainingBytes(byteArrayOf(0x00, 0x04, 0x00, 0x00)))
    }
}
