package com.vesper.flipper.ble

import com.flipperdevices.protobuf.screen.Gui
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuiRemoteInputTest {

    @Test
    fun `short press holds the key before the short event and releases after it`() {
        val sequence = GuiRemoteInput.complementarySequence(Gui.InputType.SHORT)

        assertEquals(
            listOf(Gui.InputType.PRESS, Gui.InputType.SHORT, Gui.InputType.RELEASE),
            sequence
        )
        assertTrue(GuiRemoteInput.requiresHeldKey(Gui.InputType.SHORT))
        assertFalse(sequence.first() == Gui.InputType.SHORT)
        assertEquals(Gui.InputType.SHORT, sequence[sequence.lastIndex - 1])
    }

    @Test
    fun `back uses the same short sequence as the other keys`() {
        val back = GuiRemoteInput.complementarySequence(Gui.InputType.SHORT)
        val ok = GuiRemoteInput.complementarySequence(Gui.InputType.SHORT)

        assertEquals(ok, back)
        assertEquals(Gui.InputType.RELEASE, back.last())
    }

    @Test
    fun `back is not sent after an unlock because it would lock the desktop again`() {
        assertTrue(GuiRemoteInput.backWouldRelock(Gui.InputKey.BACK, justUnlocked = true))
        assertFalse(GuiRemoteInput.backWouldRelock(Gui.InputKey.BACK, justUnlocked = false))
        assertFalse(GuiRemoteInput.backWouldRelock(Gui.InputKey.OK, justUnlocked = true))
        assertFalse(GuiRemoteInput.backWouldRelock(Gui.InputKey.LEFT, justUnlocked = true))
    }

    @Test
    fun `a second back during settle would lock the desktop that just appeared`() {
        val until = 1_500L
        assertTrue(GuiRemoteInput.backSettling(Gui.InputKey.BACK, nowMs = 1_499L, suppressUntilMs = until))
        assertFalse(GuiRemoteInput.backSettling(Gui.InputKey.BACK, nowMs = 1_500L, suppressUntilMs = until))
        assertFalse(GuiRemoteInput.backSettling(Gui.InputKey.OK, nowMs = 100L, suppressUntilMs = until))
    }

    @Test
    fun `three back taps fit the lock screen and a fourth would lock the desktop`() {
        assertEquals(1, GuiRemoteInput.backBurstSize(1))
        assertEquals(3, GuiRemoteInput.backBurstSize(3))
        assertEquals(3, GuiRemoteInput.backBurstSize(4))
        assertEquals(220L, GuiRemoteInput.backBurstWaitMs(sentShorts = 1, elapsedSinceFirstShortMs = 0L))
        assertEquals(0L, GuiRemoteInput.backBurstWaitMs(sentShorts = 3, elapsedSinceFirstShortMs = 0L))
        assertEquals(0L, GuiRemoteInput.backBurstWaitMs(sentShorts = 1, elapsedSinceFirstShortMs = 580L))
        assertEquals(
            listOf(Gui.InputType.SHORT, Gui.InputType.RELEASE),
            GuiRemoteInput.backBurstEvents(1)
        )
        assertEquals(
            listOf(
                Gui.InputType.SHORT,
                Gui.InputType.RELEASE,
                Gui.InputType.PRESS,
                Gui.InputType.SHORT,
                Gui.InputType.RELEASE,
                Gui.InputType.PRESS,
                Gui.InputType.SHORT,
                Gui.InputType.RELEASE
            ),
            GuiRemoteInput.backBurstEvents(4)
        )
        GuiRemoteInput.backBurstEvents(3).zipWithNext().forEach { (event, next) ->
            if (event == Gui.InputType.SHORT) {
                assertEquals(Gui.InputType.RELEASE, next)
            }
        }
    }

    @Test
    fun `long press emits long while the key is still held`() {
        val sequence = GuiRemoteInput.complementarySequence(Gui.InputType.LONG)

        assertEquals(
            listOf(Gui.InputType.PRESS, Gui.InputType.LONG, Gui.InputType.RELEASE),
            sequence
        )
        assertTrue(sequence.indexOf(Gui.InputType.LONG) < sequence.indexOf(Gui.InputType.RELEASE))
    }
}
