package com.vesper.flipper.ble

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Flipper's BLE flow-control characteristic is a uint32 of free RX bytes.
 * Firmware publishes it byte-reversed (big-endian on the wire). Some stacks
 * have been seen the other way, so accept whichever reading fits a real buffer.
 */
object SerialFlowControl {
    const val MAX_REASONABLE_BYTES = 64 * 1024

    fun remainingBytes(value: ByteArray): Int? {
        if (value.size < 4) return null
        val head = value.copyOfRange(0, 4)
        val bigEndian = readInt(head, ByteOrder.BIG_ENDIAN)
        val littleEndian = readInt(head, ByteOrder.LITTLE_ENDIAN)
        return when {
            bigEndian in 0..MAX_REASONABLE_BYTES -> bigEndian
            littleEndian in 0..MAX_REASONABLE_BYTES -> littleEndian
            else -> bigEndian.coerceAtLeast(0)
        }
    }

    private fun readInt(bytes: ByteArray, order: ByteOrder): Int {
        return ByteBuffer.wrap(bytes).order(order).int
    }
}
