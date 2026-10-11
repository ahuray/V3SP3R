package com.vesper.flipper.ble

import com.flipperdevices.protobuf.screen.Gui

/**
 * Flipper's GUI drops [Gui.InputType.SHORT], [Gui.InputType.LONG], and
 * [Gui.InputType.REPEAT] unless that key is already held. A physical tap is
 * published as press, then short, then release — short comes before release,
 * while the held bit is still set. Back only navigates on short, so a lone
 * short (or a release that overtakes it) does nothing on the device.
 */
object GuiRemoteInput {
    fun complementarySequence(inputType: Gui.InputType): List<Gui.InputType> {
        return when (inputType) {
            Gui.InputType.SHORT -> listOf(
                Gui.InputType.PRESS,
                Gui.InputType.SHORT,
                Gui.InputType.RELEASE
            )
            Gui.InputType.LONG -> listOf(
                Gui.InputType.PRESS,
                Gui.InputType.LONG,
                Gui.InputType.RELEASE
            )
            Gui.InputType.REPEAT -> listOf(
                Gui.InputType.PRESS,
                Gui.InputType.REPEAT,
                Gui.InputType.RELEASE
            )
            Gui.InputType.PRESS -> listOf(Gui.InputType.PRESS)
            Gui.InputType.RELEASE -> listOf(Gui.InputType.RELEASE)
            else -> listOf(Gui.InputType.PRESS, inputType, Gui.InputType.RELEASE)
        }
    }

    fun requiresHeldKey(inputType: Gui.InputType): Boolean {
        return when (inputType) {
            Gui.InputType.SHORT,
            Gui.InputType.LONG,
            Gui.InputType.REPEAT -> true
            else -> false
        }
    }

    /**
     * The lock screen wakes on an RPC desktop unlock. A Back short on the
     * desktop that just appeared locks it again, so that press must stop
     * after the unlock.
     */
    fun backWouldRelock(key: Gui.InputKey, justUnlocked: Boolean): Boolean {
        return justUnlocked && key == Gui.InputKey.BACK
    }

    /**
     * Unlock is asynchronous. A second Back in this window lands on the
     * desktop that just appeared and locks it, so the tap looks like it failed.
     */
    fun backSettling(key: Gui.InputKey, nowMs: Long, suppressUntilMs: Long): Boolean {
        return key == Gui.InputKey.BACK && nowMs < suppressUntilMs
    }

    /**
     * The lock screen unlocks on [LOCK_SCREEN_UNLOCK_TAPS] Back shorts inside
     * [LOCK_SCREEN_UNLOCK_WINDOW_MS]. A fourth short is delivered to the desktop
     * that just appeared and locks it again.
     */
    const val LOCK_SCREEN_UNLOCK_TAPS = 3
    const val LOCK_SCREEN_UNLOCK_WINDOW_MS = 600L

    fun backBurstSize(taps: Int): Int = taps.coerceIn(1, LOCK_SCREEN_UNLOCK_TAPS)

    /**
     * Events after the first press, which is already on the wire.
     * Release follows each short immediately. Waiting for another tap while
     * Back is still held is a long press, and the desktop opens the power-off
     * dialog instead of stepping back.
     */
    fun backBurstEvents(shorts: Int): List<Gui.InputType> {
        val count = shorts.coerceIn(1, LOCK_SCREEN_UNLOCK_TAPS)
        return buildList {
            repeat(count) { index ->
                if (index > 0) add(Gui.InputType.PRESS)
                add(Gui.InputType.SHORT)
                add(Gui.InputType.RELEASE)
            }
        }
    }

    /**
     * How long to wait for another Back tap after that short has been released.
     * Past the firmware window the lock screen would forget the earlier taps.
     */
    fun backBurstWaitMs(sentShorts: Int, elapsedSinceFirstShortMs: Long): Long {
        if (sentShorts >= LOCK_SCREEN_UNLOCK_TAPS) return 0L
        val remaining = LOCK_SCREEN_UNLOCK_WINDOW_MS - elapsedSinceFirstShortMs
        if (remaining <= 40L) return 0L
        return minOf(220L, remaining - 40L)
    }
}
