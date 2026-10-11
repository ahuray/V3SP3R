package com.vesper.flipper.domain.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression tests for [extractSubGhzKey].
 *
 * The parser must validate the entire `Key:` value, not just a leading
 * hex-looking prefix, so a line like "Key: AB CD ZZ" is rejected instead of
 * silently truncated to "ABCD".
 */
class SubGhzKeyExtractionTest {

    @Test
    fun `space-separated bytes normalize to contiguous uppercase hex`() {
        assertEquals(
            "0000000000ABCDEF",
            extractSubGhzKey("Key: 00 00 00 00 00 AB CD EF")
        )
    }

    @Test
    fun `lowercase hex is uppercased`() {
        assertEquals("ABCD", extractSubGhzKey("Key: ab cd"))
    }

    @Test
    fun `trailing non-hex bytes are rejected, not truncated`() {
        assertNull(extractSubGhzKey("Key: AB CD ZZ"))
    }

    @Test
    fun `tab-separated bytes are handled, not cut off`() {
        assertEquals("ABCD", extractSubGhzKey("Key:\tAB\tCD"))
    }

    @Test
    fun `odd-length hex is rejected`() {
        assertNull(extractSubGhzKey("Key: ABC"))
    }

    @Test
    fun `missing Key line returns null`() {
        assertNull(extractSubGhzKey("Protocol: Princeton\nFrequency: 433920000"))
    }

    @Test
    fun `blank value returns null`() {
        assertNull(extractSubGhzKey("Key: "))
    }

    @Test
    fun `key value is read only to end of its line`() {
        // A following line must not bleed into the key value.
        assertEquals(
            "ABCD",
            extractSubGhzKey("Key: AB CD\nProtocol: Princeton")
        )
    }
}
