package com.vesper.flipper.ble

import com.flipperdevices.protobuf.Flipper
import com.flipperdevices.protobuf.screen.Gui
import com.google.protobuf.ByteString
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RpcFrameRouterTest {

    @Test
    fun `screen frames are peeled out of a command response`() {
        val pixels = ByteArray(FlipperScreenDecoder.FRAME_BYTES) { 0x01 }
        val screen = Flipper.Main.newBuilder()
            .setCommandId(0)
            .setCommandStatus(Flipper.CommandStatus.OK)
            .setGuiScreenFrame(
                Gui.ScreenFrame.newBuilder()
                    .setData(ByteString.copyFrom(pixels))
                    .setOrientation(Gui.ScreenOrientation.HORIZONTAL)
                    .build()
            )
            .build()
        val response = Flipper.Main.newBuilder()
            .setCommandId(7)
            .setCommandStatus(Flipper.CommandStatus.OK)
            .build()
        val buffer = RpcFrameRouter.encode(screen) + RpcFrameRouter.encode(response)

        val peeled = RpcFrameRouter.peel(buffer)

        assertEquals(1, peeled.screenFrames.size)
        assertEquals(pixels.size, peeled.screenFrames.single().data.size())
        assertEquals(0, peeled.remainder.size)
        assertEquals(0, peeled.passthrough.size)
        val command = Flipper.Main.parseFrom(frameBody(peeled.commandFrames))
        assertEquals(7, command.commandId)
        assertTrue(!command.hasGuiScreenFrame())
    }

    @Test
    fun `noise in front of a screen frame does not hide the button reply`() {
        val screen = Flipper.Main.newBuilder()
            .setCommandStatus(Flipper.CommandStatus.OK)
            .setGuiScreenFrame(
                Gui.ScreenFrame.newBuilder()
                    .setData(ByteString.copyFrom(ByteArray(FlipperScreenDecoder.FRAME_BYTES)))
                    .build()
            )
            .build()
        val response = Flipper.Main.newBuilder()
            .setCommandId(4)
            .setCommandStatus(Flipper.CommandStatus.OK)
            .build()
        val encodedResponse = RpcFrameRouter.encode(response)
        val buffer = byteArrayOf(0) + RpcFrameRouter.encode(screen) + encodedResponse

        val peeled = RpcFrameRouter.peel(buffer)

        assertEquals(1, peeled.screenFrames.size)
        assertArrayEquals(byteArrayOf(0), peeled.passthrough)
        assertArrayEquals(encodedResponse, peeled.commandFrames)
    }

    @Test
    fun `an incomplete screen frame stays in the remainder`() {
        val screen = Flipper.Main.newBuilder()
            .setCommandStatus(Flipper.CommandStatus.OK)
            .setGuiScreenFrame(
                Gui.ScreenFrame.newBuilder()
                    .setData(ByteString.copyFrom(ByteArray(FlipperScreenDecoder.FRAME_BYTES)))
                    .build()
            )
            .build()
        val encoded = RpcFrameRouter.encode(screen)
        val partial = encoded.copyOf(12)

        val peeled = RpcFrameRouter.peel(partial)

        assertTrue(peeled.screenFrames.isEmpty())
        assertArrayEquals(partial, peeled.remainder)
        assertEquals(0, peeled.passthrough.size)
    }

    private fun frameBody(encoded: ByteArray): ByteArray {
        var result = 0
        var shift = 0
        var offset = 0
        while (true) {
            val b = encoded[offset].toInt() and 0xFF
            result = result or ((b and 0x7F) shl shift)
            offset++
            if (b and 0x80 == 0) break
            shift += 7
        }
        return encoded.copyOfRange(offset, offset + result)
    }
}
