package com.vesper.flipper.domain

import com.vesper.flipper.domain.catalog.FlipperCatalogJson
import com.vesper.flipper.domain.model.FapHubCatalog
import com.vesper.flipper.domain.model.FapPack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FapHubCatalogTest {

    @Test
    fun `bare pack uses catalog aliases and official install folders`() {
        assertTrue(FapHubCatalog.bareApps.size >= 30)
        assertTrue(FapHubCatalog.featuredApps.all { !it.needsModule })
        assertEquals(FapHubCatalog.featuredApps.map { it.id }.distinct().size, FapHubCatalog.featuredApps.size)

        val quac = FapHubCatalog.searchApps("quac").single()
        assertEquals("quac", quac.catalogAlias)
        assertEquals("/ext/apps/Tools/quac.fap", FapHubCatalog.installPath(quac))

        val spectrum = FapHubCatalog.searchApps("spectrum").single()
        assertEquals("/ext/apps/Sub-GHz/spectrum_analyzer.fap", FapHubCatalog.installPath(spectrum))
        assertEquals(FapPack.RADIO, spectrum.pack)
    }

    @Test
    fun `odd tools install from the official catalog aliases`() {
        val longwave = FapHubCatalog.searchApps("longwave_clock").single()
        assertEquals(FapPack.RADIO, longwave.pack)
        assertEquals("/ext/apps/GPIO/longwave_clock.fap", FapHubCatalog.installPath(longwave))
        assertFalse(longwave.needsModule)

        assertEquals("/ext/apps/Tools/clock_o_dial.fap", FapHubCatalog.installPath(FapHubCatalog.searchApps("clock_o_dial").single()))
        assertEquals("/ext/apps/Games/morse_trainer.fap", FapHubCatalog.installPath(FapHubCatalog.searchApps("morse_trainer").single()))
        assertEquals("/ext/apps/Tools/brainfuck.fap", FapHubCatalog.installPath(FapHubCatalog.searchApps("brainfuck").single()))
    }

    @Test
    fun `edge pack is the sharp bare-device set`() {
        val edge = FapHubCatalog.appsInPack(FapPack.EDGE).map { it.id }.toSet()
        assertEquals(
            setOf("nfc_magic", "picopass", "metroflip", "weebo", "mouse_jiggler", "findmy", "cyborg_detector"),
            edge,
        )
        assertTrue(FapHubCatalog.searchApps("edge").containsAll(FapHubCatalog.appsInPack(FapPack.EDGE)))
    }

    @Test
    fun `pack does not advertise module attack apps`() {
        val ids = FapHubCatalog.featuredApps.map { it.id }
        listOf("wifi_marauder", "evil_portal", "ble_spam", "subghz_bruteforcer", "mousejacker", "sentry_safe", "gps")
            .forEach { blocked -> assertFalse(ids.contains(blocked)) }
    }
}

class FlipperCatalogJsonTest {

    @Test
    fun `parser reads alias version and current sdk`() {
        val apps = FlipperCatalogJson.parseApplications(
            """
            [{
              "alias": "quac",
              "current_version": {
                "_id": "version-1",
                "version": "0.10.0",
                "current_build": { "sdk": { "api": "88.2" } }
              }
            }]
            """.trimIndent()
        )
        assertEquals(1, apps.size)
        assertEquals("quac", apps[0].alias)
        assertEquals("version-1", apps[0].versionId)
        assertEquals("0.10.0", apps[0].version)
        assertEquals("88.2", apps[0].currentApi)
    }

    @Test
    fun `latest release sdk is preferred over the dev build`() {
        val latest = FlipperCatalogJson.parseLatestReleaseApi(
            """
            [
              {"api":"88.2","is_latest_release":false},
              {"api":"87.1","is_latest_release":true}
            ]
            """.trimIndent()
        )
        assertEquals("87.1", latest)
        assertEquals(
            listOf("87.1", "88.2"),
            FlipperCatalogJson.candidateApis("1.4.1", latest, "88.2"),
        )
        assertEquals("88.2", FlipperCatalogJson.sdkApiForFirmware("mntm-dev 1.5.0-rc"))
        assertEquals("87.1", FlipperCatalogJson.sdkApiForFirmware("Momentum mntm-012"))
        assertEquals(null, FlipperCatalogJson.sdkApiForFirmware("unknown"))
    }

    @Test
    fun `fap files are elf binaries`() {
        assertTrue(FlipperCatalogJson.isFapElf(byteArrayOf(0x7f, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte(), 1)))
        assertFalse(FlipperCatalogJson.isFapElf("<html>".toByteArray()))
        assertEquals(
            "https://catalog.flipperzero.one/api/v0/0/application/version/abc/build/f7/87.1",
            FlipperCatalogJson.buildUrl("abc", "87.1"),
        )
    }
}
