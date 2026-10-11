package com.vesper.flipper.ble

import com.flipperdevices.protobuf.Flipper
import org.junit.Assert.assertEquals
import org.junit.Test

class DesktopLockStatusTest {

    @Test
    fun `empty OK means the flipper is locked`() {
        assertEquals(
            DesktopLockStatus.Reading.Locked,
            DesktopLockStatus.interpret(
                commandStatus = Flipper.CommandStatus.OK,
                hasDesktopStatus = false,
                lockedFlag = false
            )
        )
    }

    @Test
    fun `empty ERROR means the flipper is unlocked`() {
        assertEquals(
            DesktopLockStatus.Reading.Unlocked,
            DesktopLockStatus.interpret(
                commandStatus = Flipper.CommandStatus.ERROR,
                hasDesktopStatus = false,
                lockedFlag = true
            )
        )
    }

    @Test
    fun `a desktop status body carries the flag itself`() {
        assertEquals(
            DesktopLockStatus.Reading.Unlocked,
            DesktopLockStatus.interpret(
                commandStatus = Flipper.CommandStatus.OK,
                hasDesktopStatus = true,
                lockedFlag = false
            )
        )
    }
}
