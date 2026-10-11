package com.vesper.flipper.domain

import com.vesper.flipper.domain.model.CommandAction
import com.vesper.flipper.domain.model.FapHubCatalog
import com.vesper.flipper.domain.model.FileEntry
import com.vesper.flipper.domain.model.StageBeatAction
import com.vesper.flipper.domain.model.StagePhase
import com.vesper.flipper.domain.model.StageRemoteCatalog
import com.vesper.flipper.domain.model.StageTier
import com.vesper.flipper.domain.model.CommandResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StageRemoteCatalogTest {

    @Test
    fun `beat ids are unique and every beat has one action`() {
        val beats = StageRemoteCatalog.beats
        assertEquals(beats.map { it.id }.distinct().size, beats.size)
        assertTrue(beats.all { it.station.isNotBlank() && it.title.isNotBlank() })
        assertEquals(
            beats.map { it.action.commandAction }.distinct().sorted(),
            listOf(
                CommandAction.BADUSB_EXECUTE,
                CommandAction.BLE_SPAM,
                CommandAction.IR_TRANSMIT,
                CommandAction.LAUNCH_APP,
                CommandAction.SUBGHZ_TRANSMIT
            ).sorted()
        )
    }

    @Test
    fun `safe beats are the three D1 demo scripts`() {
        val safe = StageRemoteCatalog.beats.filter { it.tier == StageTier.SAFE }
        assertEquals(
            listOf(
                "demo_calc_windows.txt",
                "demo_notepad_hello_windows.txt",
                "demo_rickroll_windows.txt"
            ),
            safe.map { (it.action as StageBeatAction.BadUsbExecute).fileName }
        )
        safe.forEach { beat ->
            assertFalse(beat.requiresLiveConfirmation())
            assertEquals("D1", beat.station)
            assertEquals(CommandAction.BADUSB_EXECUTE, beat.action.commandAction)
            val command = beat.executeCommand()
            assertEquals(CommandAction.BADUSB_EXECUTE, command.action)
            assertEquals("/ext/badusb/${beat.subtitle}", command.args.path)
            assertTrue(command.justification.isNotBlank())
        }
    }

    @Test
    fun `live badusb beats target ext badusb and always confirm`() {
        val expected = listOf(
            "live_wifi_key_reveal_windows.txt",
            "live_wifi_key_reveal_elevated_windows.txt",
            "live_credential_prompt_windows.txt",
            "live_speech_windows.txt",
            "live_hackertyper_windows.txt"
        )
        val liveScripts = StageRemoteCatalog.beats.filter {
            it.tier == StageTier.LIVE && it.action is StageBeatAction.BadUsbExecute
        }
        assertEquals(expected, liveScripts.map { (it.action as StageBeatAction.BadUsbExecute).fileName })
        liveScripts.forEach { beat ->
            assertTrue(beat.requiresLiveConfirmation())
            assertEquals("D1", beat.station)
            assertEquals("/ext/badusb/${beat.subtitle}", beat.executeCommand().args.path)
        }
    }

    @Test
    fun `hardware beats map to one executor action and ext paths`() {
        val mouse = StageRemoteCatalog.beatById("haunted_mouse")!!
        assertEquals(StageTier.LIVE, mouse.tier)
        assertTrue(mouse.requiresLiveConfirmation())
        assertEquals("Flipper must be USB-cabled to the target", mouse.subtitle)
        val mouseCommand = mouse.executeCommand()
        assertEquals(CommandAction.LAUNCH_APP, mouseCommand.action)
        assertEquals("mouse_jiggler", mouseCommand.args.appName)

        val ir = StageRemoteCatalog.beatById("soundbar_ir")!!
        assertEquals(CommandAction.IR_TRANSMIT, ir.action.commandAction)
        assertEquals("/ext/infrared", (ir.action as StageBeatAction.IrTransmit).directory)
        assertThrows(IllegalArgumentException::class.java) { ir.executeCommand() }
        assertThrows(IllegalArgumentException::class.java) {
            ir.executeCommand("/ext/subghz/nope.ir")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ir.executeCommand("/ext/infrared/../badusb/x.ir")
        }
        assertEquals(
            "/ext/infrared/tv/soundbar.ir",
            ir.executeCommand("/ext/infrared/tv/soundbar.ir").args.path
        )

        val sub = StageRemoteCatalog.beatById("subghz_replay")!!
        assertEquals("D2", sub.station)
        assertEquals(CommandAction.SUBGHZ_TRANSMIT, sub.action.commandAction)
        assertEquals("/ext/subghz", (sub.action as StageBeatAction.SubGhzTransmit).directory)
        assertEquals("/ext/subghz/gate.sub", sub.executeCommand("/ext/subghz/gate.sub").args.path)

        val ble = StageRemoteCatalog.beatById("ble_room_buzz")!!
        assertEquals("D8", ble.station)
        assertEquals(CommandAction.BLE_SPAM, ble.executeCommand().action)
        assertNull(ble.executeCommand().args.appArgs)
        assertTrue(ble.subtitle.contains("may not exist"))

        val wifi = StageRemoteCatalog.beatById("wifi_station")!!
        assertEquals("D5", wifi.station)
        assertEquals(CommandAction.LAUNCH_APP, wifi.executeCommand().action)
        assertEquals("esp32_wifi_marauder", wifi.executeCommand().args.appName)
        assertTrue(wifi.subtitle.contains("on-device"))
    }

    @Test
    fun `only live tier requires the stage confirmation`() {
        assertTrue(StageRemoteCatalog.beats.filter { it.tier == StageTier.LIVE }.all { it.requiresLiveConfirmation() })
        assertTrue(StageRemoteCatalog.beats.filter { it.tier == StageTier.SAFE }.none { it.requiresLiveConfirmation() })
    }

    @Test
    fun `bundled assets map to ext paths and tiers`() {
        val tree = mapOf(
            "stage" to listOf("safe", "live"),
            "stage/safe" to listOf("demo_calc_windows.txt"),
            "stage/live" to listOf("live_speech_windows.txt", "evilportal"),
            "stage/live/evilportal" to listOf("index.html")
        )
        val paths = StageRemoteCatalog.discoverStageAssetPaths { tree[it].orEmpty() }
        assertEquals(
            listOf(
                "stage/safe/demo_calc_windows.txt",
                "stage/live/live_speech_windows.txt",
                "stage/live/evilportal/index.html"
            ),
            paths
        )
        val payloads = paths.map { StageRemoteCatalog.stagePayloadFromAsset(it) }
        assertEquals(StageTier.SAFE, payloads[0].tier)
        assertEquals("/ext/badusb/demo_calc_windows.txt", payloads[0].devicePath)
        assertFalse(payloads[0].requiresLiveConfirmation())
        assertEquals(StageTier.LIVE, payloads[1].tier)
        assertEquals("/ext/badusb/live_speech_windows.txt", payloads[1].devicePath)
        assertEquals(StageTier.LIVE, payloads[2].tier)
        assertEquals("/ext/apps_data/evil_portal/index.html", payloads[2].devicePath)
        assertTrue(payloads[2].requiresLiveConfirmation())

        assertTrue(StageRemoteCatalog.discoverStageAssetPaths { emptyList() }.isEmpty())
        assertEquals(
            StageRemoteCatalog.EMPTY_ASSETS_MESSAGE,
            "build with synced stage assets"
        )
    }

    @Test
    fun `on device indicator matches listed badusb and portal names`() {
        val script = StageRemoteCatalog.stagePayloadFromAsset("stage/safe/demo_calc_windows.txt")
        val portal = StageRemoteCatalog.stagePayloadFromAsset("stage/live/evilportal/index.html")
        assertTrue(StageRemoteCatalog.isPayloadOnDevice(script, setOf("demo_calc_windows.txt"), emptySet()))
        assertFalse(StageRemoteCatalog.isPayloadOnDevice(script, emptySet(), setOf("demo_calc_windows.txt")))
        assertTrue(StageRemoteCatalog.isPayloadOnDevice(portal, emptySet(), setOf("index.html")))
        assertFalse(StageRemoteCatalog.isPayloadOnDevice(portal, setOf("index.html"), emptySet()))
    }

    @Test
    fun `directory listing keeps signal files and child folders`() {
        val entries = listOf(
            FileEntry(name = "a.ir", path = "/ext/infrared/a.ir", isDirectory = false),
            FileEntry(name = "notes.txt", path = "/ext/infrared/notes.txt", isDirectory = false),
            FileEntry(name = "tv", path = "/ext/infrared/tv", isDirectory = true)
        )
        assertEquals(listOf("/ext/infrared/a.ir"), StageRemoteCatalog.filePathsWithExtension(entries, ".ir"))
        assertEquals(listOf("/ext/infrared/tv"), StageRemoteCatalog.childDirectories(entries))
        assertTrue(StageRemoteCatalog.isTransmittableFile("/ext/infrared/tv/a.IR", "/ext/infrared", ".ir"))
        assertFalse(StageRemoteCatalog.isTransmittableFile("/ext/infrared/a.sub", "/ext/infrared", ".ir"))
    }

    @Test
    fun `stage apps resolve to catalog install paths under ext apps`() {
        assertEquals(
            StageRemoteCatalog.stageApps.map { it.alias }.distinct().size,
            StageRemoteCatalog.stageApps.size
        )
        val expected = listOf(
            "mouse_jiggler",
            "bettermouse",
            "usb_hid_autofire",
            "esp32_wifi_marauder",
            "findmy",
            "xremote",
            "sub_analyzer",
            "radio_scanner",
            "flip_wifi"
        )
        assertEquals(expected, StageRemoteCatalog.stageApps.map { it.alias })
        StageRemoteCatalog.stageApps.forEach { spec ->
            assertTrue(spec.friendlyName.isNotBlank())
            val app = StageRemoteCatalog.resolveStageApp(spec.alias)
            assertTrue(app != null)
            val path = FapHubCatalog.installPath(app!!)
            assertTrue(path.startsWith("/ext/apps/"))
            assertTrue(path.endsWith("/${spec.alias}.fap"))
            assertTrue(StageRemoteCatalog.isStageAppInstalled(spec.alias, setOf(spec.alias)))
            assertTrue(StageRemoteCatalog.isStageAppInstalled(spec.alias, setOf("${spec.alias}.fap")))
            assertFalse(StageRemoteCatalog.isStageAppInstalled(spec.alias, emptySet()))
        }
    }

    @Test
    fun `command result feedback keeps executor error text`() {
        val ok = StageRemoteCatalog.feedbackFor(
            CommandResult(
                success = true,
                action = CommandAction.BADUSB_EXECUTE,
                data = com.vesper.flipper.domain.model.CommandResultData(message = "Executing BadUSB script: /ext/badusb/x.txt")
            )
        )
        assertEquals(StagePhase.OK, ok.phase)
        assertTrue(ok.text.contains("/ext/badusb/x.txt"))

        val failed = StageRemoteCatalog.feedbackFor(
            CommandResult(
                success = false,
                action = CommandAction.BLE_SPAM,
                error = "BLE_SPAM: unknown command"
            )
        )
        assertEquals(StagePhase.ERROR, failed.phase)
        assertEquals("BLE_SPAM: unknown command", failed.text)
    }
}
