package com.vesper.flipper.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * FapHub - Flipper Application Hub + Public Resource Library
 *
 * Browse apps, remote libraries, evil portal scripts, BadUSB payloads,
 * Sub-GHz files, and more from curated public repositories.
 */

@Serializable
data class FapApp(
    val id: String,
    val name: String,
    val description: String,
    val category: FapCategory,
    val author: String,
    val version: String,
    val downloadUrl: String,
    val iconUrl: String? = null,
    val screenshots: List<String> = emptyList(),
    val size: Long = 0,
    val downloads: Int = 0,
    val rating: Float = 0f,
    val updatedAt: Long = 0,
    val sdkVersion: String = "0.0",
    val targetFirmware: String = "official release",
    val isInstalled: Boolean = false,
    val installedVersion: String? = null,
    /** Flipper application catalog alias. Install resolves a real .fap from this. */
    val catalogAlias: String = "",
    /** Folder under /ext/apps. Matches the catalog category name, including "Sub-GHz". */
    val installFolder: String = "Tools",
    val pack: FapPack = FapPack.DAILY,
    /** True when the app needs a board or wired module the stock Flipper does not have. */
    val needsModule: Boolean = false,
)

@Serializable
enum class FapPack(val displayName: String) {
    @SerialName("daily") DAILY("Daily"),
    @SerialName("radio") RADIO("Radio"),
    @SerialName("edge") EDGE("Edge"),
    @SerialName("play") PLAY("Play"),
}

@Serializable
enum class FapCategory(val displayName: String, val icon: String) {
    @SerialName("games") GAMES("Games", "🎮"),
    @SerialName("tools") TOOLS("Tools", "🔧"),
    @SerialName("nfc") NFC("NFC", "💳"),
    @SerialName("subghz") SUBGHZ("Sub-GHz", "📡"),
    @SerialName("infrared") INFRARED("Infrared", "🔴"),
    @SerialName("gpio") GPIO("GPIO", "⚡"),
    @SerialName("bluetooth") BLUETOOTH("Bluetooth", "🔵"),
    @SerialName("usb") USB("USB", "🔌"),
    @SerialName("media") MEDIA("Media", "🎵"),
    @SerialName("misc") MISC("Misc", "📦")
}

@Serializable
data class FapCatalog(
    val apps: List<FapApp>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val lastUpdated: Long
)

sealed class InstallStatus {
    object Idle : InstallStatus()
    data class Downloading(val progress: Float) : InstallStatus()
    object Installing : InstallStatus()
    object Success : InstallStatus()
    data class Error(val message: String) : InstallStatus()
}

// ═══════════════════════════════════════════════════════════
// PUBLIC RESOURCE LIBRARY
// ═══════════════════════════════════════════════════════════

/**
 * A public repository/collection of Flipper files
 */
data class FlipperResourceRepo(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val resourceType: FlipperResourceType,
    val repoUrl: String,
    val stars: Int = 0,
    val fileCount: Int = 0,
    val lastUpdated: String = "",
    val tags: List<String> = emptyList()
)

enum class FlipperResourceType(
    val displayName: String,
    val icon: String,
    val color: Long,
    val flipperDir: String
) {
    IR_REMOTE("IR Remotes", "🔴", 0xFFF44336, "/ext/infrared"),
    SUB_GHZ("Sub-GHz Files", "📡", 0xFFFF9800, "/ext/subghz"),
    BAD_USB("BadUSB Scripts", "💀", 0xFF9C27B0, "/ext/badusb"),
    NFC_FILES("NFC Files", "💳", 0xFF4CAF50, "/ext/nfc"),
    EVIL_PORTAL("Evil Portal", "🕸", 0xFFE91E63, "/ext/apps_data/evil_portal"),
    MUSIC("Music / RTTTL", "🎵", 0xFF2196F3, "/ext/music_player"),
    ANIMATIONS("Animations", "🐬", 0xFF00BCD4, "/ext/dolphin"),
    GPIO_TOOLS("GPIO / Hardware", "⚡", 0xFFFFEB3B, "/ext/gpio")
}

/**
 * A file entry within a public repository
 */
data class RepoFileEntry(
    val name: String,
    val path: String,
    val downloadUrl: String,
    val size: Long = 0,
    val type: String = "file"
)

/**
 * Curated list of popular Flipper resource repositories
 */
object FlipperResourceLibrary {

    val repositories = listOf(
        // IR Remote Libraries
        FlipperResourceRepo(
            id = "irdb",
            name = "Flipper-IRDB",
            description = "Massive IR remote database. TV, AC, projector, soundbar, fan, and more from thousands of brands.",
            author = "logickworkshop",
            resourceType = FlipperResourceType.IR_REMOTE,
            repoUrl = "https://github.com/logickworkshop/Flipper-IRDB",
            stars = 2200,
            fileCount = 5000,
            lastUpdated = "2025",
            tags = listOf("Samsung", "LG", "Sony", "Vizio", "TCL", "AC", "Projector")
        ),
        FlipperResourceRepo(
            id = "ir_universal",
            name = "Flipper Universal IR Remote",
            description = "Universal remote files organized by device type. Covers TVs, ACs, audio systems, fans, and LED strips.",
            author = "UberGuidoZ",
            resourceType = FlipperResourceType.IR_REMOTE,
            repoUrl = "https://github.com/UberGuidoZ/Flipper",
            stars = 13000,
            fileCount = 3000,
            lastUpdated = "2025",
            tags = listOf("Universal", "All Brands", "LED", "Fan", "Audio")
        ),

        // Sub-GHz Files
        FlipperResourceRepo(
            id = "subghz_bruteforce",
            name = "Sub-GHz Bruteforce Files",
            description = "Pre-generated bruteforce files for common Sub-GHz protocols. Garage doors, gates, and barrier openers.",
            author = "tobiabocchi",
            resourceType = FlipperResourceType.SUB_GHZ,
            repoUrl = "https://github.com/tobiabocchi/flipperzero-bruteforce",
            stars = 1800,
            fileCount = 200,
            lastUpdated = "2024",
            tags = listOf("Bruteforce", "Garage", "Gate", "CAME", "Nice", "Linear")
        ),
        FlipperResourceRepo(
            id = "subghz_collection",
            name = "Sub-GHz Signal Library",
            description = "Curated collection of Sub-GHz signals: Tesla charge port, fans, lights, and protocol examples.",
            author = "UberGuidoZ",
            resourceType = FlipperResourceType.SUB_GHZ,
            repoUrl = "https://github.com/UberGuidoZ/Flipper",
            stars = 13000,
            fileCount = 500,
            lastUpdated = "2025",
            tags = listOf("Tesla", "Protocol", "Weather", "TPMS", "Doorbells")
        ),

        // BadUSB Scripts
        FlipperResourceRepo(
            id = "badusb_scripts",
            name = "Flipper BadUSB Payloads",
            description = "DuckyScript payloads for recon, exfiltration, pranks, and system modification. Windows, Mac, Linux.",
            author = "I-Am-Jakoby",
            resourceType = FlipperResourceType.BAD_USB,
            repoUrl = "https://github.com/I-Am-Jakoby/Flipper-Zero-BadUSB",
            stars = 2500,
            fileCount = 80,
            lastUpdated = "2025",
            tags = listOf("Windows", "Mac", "Exfil", "Recon", "Prank")
        ),
        FlipperResourceRepo(
            id = "badusb_hak5",
            name = "Hak5-Inspired BadUSB",
            description = "BadUSB scripts inspired by Hak5 payloads. Credential harvesting, reverse shells, and system recon.",
            author = "UberGuidoZ",
            resourceType = FlipperResourceType.BAD_USB,
            repoUrl = "https://github.com/UberGuidoZ/Flipper",
            stars = 13000,
            fileCount = 120,
            lastUpdated = "2025",
            tags = listOf("Hak5", "ReverseShell", "Credentials", "WiFi")
        ),

        // Evil Portal
        FlipperResourceRepo(
            id = "evil_portal_html",
            name = "Evil Portal Templates",
            description = "Captive portal HTML templates. Google, Facebook, Twitter, WiFi login, and custom brand pages.",
            author = "bigbrodude6119",
            resourceType = FlipperResourceType.EVIL_PORTAL,
            repoUrl = "https://github.com/bigbrodude6119/flipper-zero-evil-portal",
            stars = 1200,
            fileCount = 30,
            lastUpdated = "2024",
            tags = listOf("Google", "Facebook", "WiFi", "Custom", "Social")
        ),
        FlipperResourceRepo(
            id = "evil_portal_custom",
            name = "Custom Portal Pages",
            description = "Community-contributed evil portal templates for various login portals and services.",
            author = "RogueMaster",
            resourceType = FlipperResourceType.EVIL_PORTAL,
            repoUrl = "https://github.com/RogueMaster/flipperzero-firmware-wPlugins",
            stars = 3500,
            fileCount = 25,
            lastUpdated = "2025",
            tags = listOf("Netflix", "Amazon", "Hotel", "Airport", "Coffee")
        ),

        // NFC Files
        FlipperResourceRepo(
            id = "nfc_amiibo",
            name = "Amiibo NFC Collection",
            description = "Complete Amiibo NFC data files for Nintendo Switch and 3DS compatible figures.",
            author = "Gioman101",
            resourceType = FlipperResourceType.NFC_FILES,
            repoUrl = "https://github.com/Gioman101/FlipperAmiworker",
            stars = 500,
            fileCount = 600,
            lastUpdated = "2024",
            tags = listOf("Amiibo", "Nintendo", "Switch", "3DS")
        ),

        // Music / RTTTL
        FlipperResourceRepo(
            id = "music_rtttl",
            name = "RTTTL Music Collection",
            description = "Ringtone melodies in RTTTL format. Game themes, movie scores, classic tunes, and memes.",
            author = "UberGuidoZ",
            resourceType = FlipperResourceType.MUSIC,
            repoUrl = "https://github.com/UberGuidoZ/Flipper",
            stars = 13000,
            fileCount = 200,
            lastUpdated = "2025",
            tags = listOf("Mario", "Zelda", "Rickroll", "Star Wars", "Nokia")
        ),

        // Animations
        FlipperResourceRepo(
            id = "dolphin_animations",
            name = "Custom Dolphin Animations",
            description = "Custom Flipper dolphin animations. Anime, movies, games, memes, and custom artwork.",
            author = "Talking-Sasquach",
            resourceType = FlipperResourceType.ANIMATIONS,
            repoUrl = "https://github.com/Talking-Sasquach/Flipper-Zero-Animations",
            stars = 300,
            fileCount = 100,
            lastUpdated = "2024",
            tags = listOf("Anime", "Games", "Memes", "Custom", "Dolphin")
        ),

        // GPIO / Hardware
        FlipperResourceRepo(
            id = "gpio_marauder",
            name = "WiFi Marauder Companion",
            description = "ESP32 Marauder flasher and companion files. WiFi scanning, deauth, beacon spam, and PMKID capture.",
            author = "justcallmekoko",
            resourceType = FlipperResourceType.GPIO_TOOLS,
            repoUrl = "https://github.com/justcallmekoko/ESP32Marauder",
            stars = 5000,
            fileCount = 50,
            lastUpdated = "2025",
            tags = listOf("ESP32", "WiFi", "Deauth", "PMKID", "Wardriving")
        )
    )

    fun getByType(type: FlipperResourceType): List<FlipperResourceRepo> {
        return repositories.filter { it.resourceType == type }
    }

    fun search(query: String): List<FlipperResourceRepo> {
        val lq = query.lowercase()
        return repositories.filter {
            it.name.lowercase().contains(lq) ||
            it.description.lowercase().contains(lq) ||
            it.tags.any { t -> t.lowercase().contains(lq) } ||
            it.author.lowercase().contains(lq)
        }
    }
}

/**
 * On-device pack for a stock Flipper Zero.
 *
 * Every entry is a real Flipper application-catalog alias. Install resolves the
 * compiled FAP for the connected firmware's SDK. Nothing here needs a Wi-Fi
 * board, GPS, or GPIO module.
 */
object FapHubCatalog {

    val featuredApps: List<FapApp> = buildList {
        // Daily driver
        add(packApp("quac", "Quac!", "Quick-action remote for the IR, Sub-GHz, NFC, RFID, and iButton files you already saved.", FapCategory.TOOLS, "Tools", "Roberto De Feo", "0.10.0", FapPack.DAILY))
        add(packApp("totp", "Authenticator", "TOTP and HOTP codes on the Flipper.", FapCategory.TOOLS, "Tools", "Alexander Kopachov (@akopachov)", "5.190", FapPack.DAILY))
        add(packApp("nfc_maker", "NFC Maker", "Build NFC files for links, contacts, Wi-Fi, and text, then emulate them from the NFC app.", FapCategory.NFC, "NFC", "@Willy-JL", "2.1", FapPack.DAILY))
        add(packApp("subghz_remote_ofw", "Sub-GHz Remote", "Five-slot remote for .sub files you recorded.", FapCategory.SUBGHZ, "Sub-GHz", "@gid9798 and @xMasterX", "1.7", FapPack.DAILY))
        add(packApp("barcode_app", "Barcode", "Show barcodes on the Flipper screen.", FapCategory.TOOLS, "Tools", "@Kingal1337", "1.4", FapPack.DAILY))
        add(packApp("qrcode", "QR Code", "Show QR codes on the Flipper screen.", FapCategory.TOOLS, "Tools", "Bob Matcuk", "2.1", FapPack.DAILY))
        add(packApp("mass_storage", "Mass Storage", "Expose a disk image on the SD card as a USB drive.", FapCategory.USB, "USB", "@nminaylov @kevinwallace", "1.5", FapPack.DAILY))
        add(packApp("bt_trigger", "BT Trigger", "Use the Flipper as a Bluetooth camera remote for your phone.", FapCategory.BLUETOOTH, "Bluetooth", "@Nem0oo", "1.5", FapPack.DAILY))
        add(packApp("clock", "Clock", "Simple clock face.", FapCategory.TOOLS, "Tools", "@kowalski7cc", "1.2", FapPack.DAILY))
        add(packApp("analog_clock", "Analog Clock", "Analog clock on the Flipper screen.", FapCategory.TOOLS, "Tools", "@scrolltex", "1.4", FapPack.DAILY))
        add(packApp("dice_app", "Dice", "Coin and polyhedral dice.", FapCategory.TOOLS, "Tools", "Flipper catalog", "1.3", FapPack.DAILY))
        add(packApp("flipp_pomodoro", "Pomodoro", "Pomodoro timer.", FapCategory.TOOLS, "Tools", "@Th3Un1q3", "1.5", FapPack.DAILY))
        add(packApp("clock_o_dial", "Clock'o'Dial", "Workday clock that warns you before overtime.", FapCategory.TOOLS, "Tools", "Ilia Petrov-Komotskii", "1.0", FapPack.DAILY))
        add(packApp("lightmeter", "Lightmeter", "Photography light meter using the built-in light sensor.", FapCategory.TOOLS, "Tools", "Oleksii Kutuzov", "1.3", FapPack.DAILY))
        add(packApp("animation_switcher", "Animation Switcher", "Swap the desktop dolphin animation from the SD card.", FapCategory.TOOLS, "Tools", "@lsalik2", "1.1", FapPack.DAILY))
        add(packApp("music_player", "Music Player", "Play RTTTL tunes on the speaker.", FapCategory.MEDIA, "Media", "@DrZlo13", "1.4", FapPack.DAILY))
        add(packApp("wav_player", "WAV Player", "Play WAV files on the speaker.", FapCategory.MEDIA, "Media", "@DrZlo13", "1.4", FapPack.DAILY))

        // Radio bench — built-in CC1101 and IR only
        add(packApp("protoview", "ProtoView", "Draw unknown Sub-GHz pulses, edit them, and replay a capture.", FapCategory.SUBGHZ, "Sub-GHz", "@antirez", "1.3", FapPack.RADIO))
        add(packApp("spectrum_analyzer", "Spectrum Analyzer", "Live spectrogram of the bands the internal radio can hear.", FapCategory.SUBGHZ, "Sub-GHz", "@jolcese, @xMasterX", "1.4", FapPack.RADIO))
        add(packApp("weather_station", "Weather Station", "Receive outdoor weather sensors on Sub-GHz.", FapCategory.SUBGHZ, "Sub-GHz", "@Skorpionm", "2.0", FapPack.RADIO))
        add(packApp("pocsag_pager", "POCSAG Pager", "Capture POCSAG 1200 pager bursts on the internal radio.", FapCategory.SUBGHZ, "Sub-GHz", "@xMasterX, @Shmuma", "1.4", FapPack.RADIO))
        add(packApp("ir_scope", "IR Scope", "Watch incoming infrared signals.", FapCategory.INFRARED, "Infrared", "@kallanreed", "1.4", FapPack.RADIO))
        add(packApp("longwave_clock", "Longwave Clock", "Demo DCF77 and MSF time signals on the bare Flipper. Live decode needs a small longwave receiver on GPIO.", FapCategory.GPIO, "GPIO", "@m7i-org", "0.1", FapPack.RADIO))

        // Sharp tools that still run on the bare device
        add(packApp("nfc_magic", "NFC Magic", "Write NFC tags whose sector 0 can be changed.", FapCategory.NFC, "NFC", "@AloneLiberty", "1.15", FapPack.EDGE))
        add(packApp("picopass", "PicoPass", "Talk to PicoPass / iClass tags with the built-in NFC radio.", FapCategory.NFC, "NFC", "@bettse", "1.22", FapPack.EDGE))
        add(packApp("metroflip", "Metroflip", "Read a transit card you are holding.", FapCategory.NFC, "NFC", "luu176", "2.0", FapPack.EDGE))
        add(packApp("weebo", "Weebo", "NTAG215 parser, writer, and emulator.", FapCategory.NFC, "NFC", "bettse", "0.5", FapPack.EDGE))
        add(packApp("mouse_jiggler", "Mouse Jiggler", "USB mouse jiggler for a computer you plug the Flipper into.", FapCategory.USB, "USB", "David Berdik", "1.1", FapPack.EDGE))
        add(packApp("findmy", "FindMy Flipper", "BLE Find My beacon so you can locate this Flipper.", FapCategory.BLUETOOTH, "Bluetooth", "@MatthewKuKanich", "3.0", FapPack.EDGE))
        add(packApp("cyborg_detector", "Cyborg Detector", "Hold an NFC field so compatible implant LEDs light up.", FapCategory.NFC, "NFC", "@RocketGod-git", "1.1", FapPack.EDGE))

        // Screen and speaker
        add(packApp("doom", "DOOM", "DOOM on the Flipper screen.", FapCategory.GAMES, "Games", "@p4nic4ttack, @xMasterX", "1.5", FapPack.PLAY))
        add(packApp("tetris", "Tetris", "Tetris.", FapCategory.GAMES, "Games", "@jeffplang, @xMasterX", "1.5", FapPack.PLAY))
        add(packApp("snake20", "Snake", "Snake.", FapCategory.GAMES, "Games", "@Willzvul", "2.4", FapPack.PLAY))
        add(packApp("flappy_bird", "Flappy Bird", "Flappy Bird.", FapCategory.GAMES, "Games", "@DroomOne", "1.3", FapPack.PLAY))
        add(packApp("asteroids", "Asteroids", "Asteroids.", FapCategory.GAMES, "Games", "@SimplyMinimal", "1.2", FapPack.PLAY))
        add(packApp("zombiez", "Zombiez", "Defend the wall.", FapCategory.GAMES, "Games", "@DevMilanIan", "1.3", FapPack.PLAY))
        add(packApp("video_player", "Video Player", "Play a video on the Flipper screen.", FapCategory.MEDIA, "Media", "LTVA", "0.4", FapPack.PLAY))
        add(packApp("image_viewer", "Image Viewer", "Show pictures on the Flipper screen.", FapCategory.MEDIA, "Media", "@polioan", "0.1", FapPack.PLAY))
        add(packApp("usb_midi", "USB-MIDI", "Turn the Flipper into a USB MIDI controller.", FapCategory.USB, "USB", "@Kribesk", "1.0", FapPack.PLAY))
        add(packApp("morse_trainer", "Morse Trainer", "Learn Morse from zero, one level at a time.", FapCategory.GAMES, "Games", "Baris Mert", "1.0", FapPack.PLAY))
        add(packApp("brainfuck", "Brainfuck", "Brainfuck interpreter on the Flipper.", FapCategory.TOOLS, "Tools", "@nymda", "1.4", FapPack.PLAY))
    }

    val allApps: List<FapApp> get() = featuredApps

    /** Apps that run with only the Flipper Zero. */
    val bareApps: List<FapApp> get() = featuredApps.filter { !it.needsModule }

    fun appsInPack(pack: FapPack): List<FapApp> = featuredApps.filter { it.pack == pack }

    fun getAppsByCategory(category: FapCategory): List<FapApp> = featuredApps.filter { it.category == category }

    fun installPath(app: FapApp): String {
        val fileName = app.catalogAlias.ifBlank { app.id }
        return "/ext/apps/${app.installFolder}/$fileName.fap"
    }

    fun searchApps(query: String): List<FapApp> {
        val lq = query.lowercase().trim()
        if (lq.isEmpty()) return featuredApps
        return featuredApps.filter {
            it.name.lowercase().contains(lq) ||
                it.description.lowercase().contains(lq) ||
                it.author.lowercase().contains(lq) ||
                it.id.lowercase().contains(lq) ||
                it.catalogAlias.lowercase().contains(lq) ||
                it.pack.displayName.lowercase().contains(lq) ||
                it.pack.name.lowercase().contains(lq)
        }
    }

    private fun packApp(
        alias: String,
        name: String,
        description: String,
        category: FapCategory,
        installFolder: String,
        author: String,
        version: String,
        pack: FapPack,
    ): FapApp = FapApp(
        id = alias,
        catalogAlias = alias,
        name = name,
        description = description,
        category = category,
        author = author,
        version = version,
        downloadUrl = "https://catalog.flipperzero.one/api/v0/0/application?limit=500",
        installFolder = installFolder,
        pack = pack,
        needsModule = false,
        targetFirmware = "official release SDK",
    )
}
