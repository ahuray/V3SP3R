package com.vesper.flipper.domain

import com.vesper.flipper.domain.model.DolphinManifest
import com.vesper.flipper.domain.model.DolphinMood
import com.vesper.flipper.domain.model.DolphinStateFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FlipperPassportTest {

    @Test
    fun `dolphin state round trip keeps mood level and checksum`() {
        val original = sampleState(butthurt = 2, xp = 10, flags = 0)
        val parsed = DolphinStateFile.parse(original)
        assertNotNull(parsed)
        assertEquals(DolphinMood.HAPPY, parsed!!.mood)
        assertEquals(1, parsed.level)

        val updated = DolphinStateFile.withMoodAndLevel(original, DolphinMood.ANGRY, 3)
        val again = DolphinStateFile.parse(updated!!)
        assertEquals(DolphinMood.ANGRY, again!!.mood)
        assertEquals(3, again.level)
        assertEquals(false, again.happyMode)
    }

    @Test
    fun `happy mood sets the happy-mode flag`() {
        val updated = DolphinStateFile.withMoodAndLevel(
            sampleState(butthurt = 12, xp = 400, flags = 0),
            DolphinMood.HAPPY,
            2
        )
        val parsed = DolphinStateFile.parse(updated!!)
        assertEquals(DolphinMood.HAPPY, parsed!!.mood)
        assertEquals(2, parsed.level)
        assertEquals(true, parsed.happyMode)
    }

    @Test
    fun `bad checksum is rejected`() {
        val bytes = sampleState(butthurt = 0, xp = 0, flags = 0)
        bytes[HEADER_CHECKSUM] = (bytes[HEADER_CHECKSUM].toInt() xor 0xFF).toByte()
        assertNull(DolphinStateFile.parse(bytes))
    }

    @Test
    fun `selecting an animation prefers it and zeros the others`() {
        val manifest = """
            Filetype: Flipper Animation Manifest
            Version: 1

            Name: Waves
            Min butthurt: 0
            Max butthurt: 5
            Min level: 1
            Max level: 1
            Weight: 3

            Name: Sleep
            Min butthurt: 0
            Max butthurt: 10
            Min level: 1
            Max level: 3
            Weight: 3
        """.trimIndent() + "\n"

        val selected = DolphinManifest.select(manifest, "Sleep")
        assertEquals("Sleep", DolphinManifest.activeName(selected))
        assertEquals(listOf(0, 8), DolphinManifest.parse(selected).map { it.weight })

        val builtIn = DolphinManifest.select(selected, null)
        assertNull(DolphinManifest.activeName(builtIn))
    }

    private fun sampleState(butthurt: Int, xp: Int, flags: Int): ByteArray {
        val payload = ByteBuffer.allocate(DolphinStateFile.PAYLOAD_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        payload.putInt(8, flags)
        payload.putInt(12, xp)
        payload.putInt(16, butthurt)
        val payloadBytes = payload.array()
        var sum = 0
        for (byte in payloadBytes) sum = (sum + (byte.toInt() and 0xFF)) and 0xFF
        val file = ByteArray(DolphinStateFile.HEADER_SIZE + payloadBytes.size)
        file[0] = DolphinStateFile.MAGIC.toByte()
        file[1] = DolphinStateFile.VERSION.toByte()
        file[HEADER_CHECKSUM] = sum.toByte()
        payloadBytes.copyInto(file, DolphinStateFile.HEADER_SIZE)
        return file
    }

    private companion object {
        const val HEADER_CHECKSUM = 2
    }
}
