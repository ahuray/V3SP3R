package com.vesper.flipper.domain

import com.vesper.flipper.domain.model.AccessoryOrigin
import com.vesper.flipper.domain.model.FlipperAccessories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlipperAccessoryTest {

    @Test
    fun `nothing is mounted by default and 5Ghost is unofficial`() {
        assertTrue(FlipperAccessories.official.none { it.id == "5ghost" })
        assertEquals(AccessoryOrigin.UNOFFICIAL, FlipperAccessories.byId("5ghost")?.origin)
        assertTrue(FlipperAccessories.official.any { it.id == "wifi_devboard" })
        assertNull(FlipperAccessories.byId("missing"))
    }
}
