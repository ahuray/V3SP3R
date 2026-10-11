package com.vesper.flipper.domain.model

/**
 * Presenter beats, bundled payload paths, and stage-app aliases for Stage Remote.
 * Pure data: no Android, no device I/O. The screen turns these into [ExecuteCommand]s.
 */
enum class StageTier {
    SAFE,
    LIVE
}

enum class StagePhase {
    RUNNING,
    OK,
    ERROR
}

data class StageFeedback(
    val phase: StagePhase,
    val text: String
)

/**
 * One Flipper action. A beat holds exactly one of these.
 */
sealed class StageBeatAction {
    abstract val commandAction: CommandAction

    data class BadUsbExecute(val fileName: String) : StageBeatAction() {
        override val commandAction: CommandAction = CommandAction.BADUSB_EXECUTE
        val devicePath: String get() = "${StageRemoteCatalog.BADUSB_DIR}/$fileName"
    }

    data class LaunchApp(val appName: String) : StageBeatAction() {
        override val commandAction: CommandAction = CommandAction.LAUNCH_APP
    }

    data class IrTransmit(
        val directory: String = StageRemoteCatalog.INFRARED_DIR
    ) : StageBeatAction() {
        override val commandAction: CommandAction = CommandAction.IR_TRANSMIT
    }

    data class SubGhzTransmit(
        val directory: String = StageRemoteCatalog.SUBGHZ_DIR
    ) : StageBeatAction() {
        override val commandAction: CommandAction = CommandAction.SUBGHZ_TRANSMIT
    }

    data class BleSpam(val appArgs: String = "") : StageBeatAction() {
        override val commandAction: CommandAction = CommandAction.BLE_SPAM
    }
}

data class StageBeat(
    val id: String,
    val title: String,
    val subtitle: String,
    val station: String,
    val tier: StageTier,
    val action: StageBeatAction
) {
    /** LIVE beats always confirm in the stage UI before [CommandExecutor] sees them. */
    fun requiresLiveConfirmation(): Boolean = tier == StageTier.LIVE

    fun summaryTarget(): String = when (val spec = action) {
        is StageBeatAction.BadUsbExecute -> spec.devicePath
        is StageBeatAction.LaunchApp -> spec.appName
        is StageBeatAction.IrTransmit -> spec.directory
        is StageBeatAction.SubGhzTransmit -> spec.directory
        is StageBeatAction.BleSpam -> "ble_spam"
    }

    fun executeCommand(selectedPath: String? = null): ExecuteCommand {
        val args = when (val spec = action) {
            is StageBeatAction.BadUsbExecute -> CommandArgs(path = spec.devicePath)
            is StageBeatAction.LaunchApp -> CommandArgs(appName = spec.appName)
            is StageBeatAction.IrTransmit -> {
                val path = selectedPath
                    ?: throw IllegalArgumentException("Choose an .ir file under ${spec.directory}")
                if (!StageRemoteCatalog.isTransmittableFile(path, spec.directory, ".ir")) {
                    throw IllegalArgumentException("IR file must be under ${spec.directory}")
                }
                CommandArgs(path = path)
            }
            is StageBeatAction.SubGhzTransmit -> {
                val path = selectedPath
                    ?: throw IllegalArgumentException("Choose a .sub file under ${spec.directory}")
                if (!StageRemoteCatalog.isTransmittableFile(path, spec.directory, ".sub")) {
                    throw IllegalArgumentException("Sub-GHz file must be under ${spec.directory}")
                }
                CommandArgs(path = path)
            }
            is StageBeatAction.BleSpam -> CommandArgs(
                appArgs = spec.appArgs.takeIf { it.isNotBlank() }
            )
        }
        return ExecuteCommand(
            action = action.commandAction,
            args = args,
            justification = "Stage remote ${tier.name.lowercase()} beat: $title",
            expectedEffect = title
        )
    }
}

data class StagePayload(
    val assetPath: String,
    val name: String,
    val tier: StageTier,
    val devicePath: String
) {
    fun requiresLiveConfirmation(): Boolean = tier == StageTier.LIVE
}

data class StageAppSpec(
    val alias: String,
    val friendlyName: String
)

object StageRemoteCatalog {
    const val BADUSB_DIR = "/ext/badusb"
    const val INFRARED_DIR = "/ext/infrared"
    const val SUBGHZ_DIR = "/ext/subghz"
    const val EVIL_PORTAL_DIR = "/ext/apps_data/evil_portal"
    const val EMPTY_ASSETS_MESSAGE = "build with synced stage assets"

    val beats: List<StageBeat> = listOf(
        badUsb("demo_calc_windows", "Calculator", "demo_calc_windows.txt", StageTier.SAFE),
        badUsb("demo_notepad_hello_windows", "Notepad hello", "demo_notepad_hello_windows.txt", StageTier.SAFE),
        badUsb("demo_rickroll_windows", "Rickroll", "demo_rickroll_windows.txt", StageTier.SAFE),
        badUsb("live_wifi_key_reveal_windows", "WiFi key reveal", "live_wifi_key_reveal_windows.txt", StageTier.LIVE),
        badUsb(
            "live_wifi_key_reveal_elevated_windows",
            "WiFi key reveal (elevated)",
            "live_wifi_key_reveal_elevated_windows.txt",
            StageTier.LIVE
        ),
        badUsb("live_credential_prompt_windows", "Credential prompt", "live_credential_prompt_windows.txt", StageTier.LIVE),
        badUsb("live_speech_windows", "Speech", "live_speech_windows.txt", StageTier.LIVE),
        badUsb("live_hackertyper_windows", "Hacker typer", "live_hackertyper_windows.txt", StageTier.LIVE),
        StageBeat(
            id = "haunted_mouse",
            title = "Haunted mouse",
            subtitle = "Flipper must be USB-cabled to the target",
            station = "USB",
            tier = StageTier.LIVE,
            action = StageBeatAction.LaunchApp(appName = "mouse_jiggler")
        ),
        StageBeat(
            id = "soundbar_ir",
            title = "Soundbar hijack (IR)",
            subtitle = "Pick a .ir file on the Flipper",
            station = "IR",
            tier = StageTier.LIVE,
            action = StageBeatAction.IrTransmit()
        ),
        StageBeat(
            id = "subghz_replay",
            title = "Sub-GHz replay (D2)",
            subtitle = "Pick a .sub file on the Flipper",
            station = "D2",
            tier = StageTier.LIVE,
            action = StageBeatAction.SubGhzTransmit()
        ),
        StageBeat(
            id = "ble_room_buzz",
            title = "BLE room buzz (D8)",
            subtitle = "Firmware plugin, may not exist on all builds",
            station = "D8",
            tier = StageTier.LIVE,
            action = StageBeatAction.BleSpam()
        ),
        StageBeat(
            id = "wifi_station",
            title = "Start WiFi station (D5)",
            subtitle = "Devboard beats still run on-device per runbook",
            station = "D5",
            tier = StageTier.LIVE,
            action = StageBeatAction.LaunchApp(appName = "esp32_wifi_marauder")
        )
    )

    val stageApps: List<StageAppSpec> = listOf(
        StageAppSpec("mouse_jiggler", "Mouse Jiggler"),
        StageAppSpec("bettermouse", "Better Mouse"),
        StageAppSpec("usb_hid_autofire", "USB HID Autofire"),
        StageAppSpec("esp32_wifi_marauder", "WiFi Marauder"),
        StageAppSpec("findmy", "FindMy"),
        StageAppSpec("xremote", "Cross Remote"),
        StageAppSpec("sub_analyzer", "Sub Analyzer"),
        StageAppSpec("radio_scanner", "Radio Scanner"),
        StageAppSpec("flip_wifi", "FlipWiFi")
    )

    fun beatById(id: String): StageBeat? = beats.firstOrNull { it.id == id }

    fun resolveStageApp(alias: String): FapApp? =
        FapHubCatalog.allApps.firstOrNull { it.id == alias || it.catalogAlias == alias }

    fun listDirectoryCommand(path: String): ExecuteCommand = ExecuteCommand(
        action = CommandAction.LIST_DIRECTORY,
        args = CommandArgs(path = path),
        justification = "Stage remote listing $path",
        expectedEffect = "List $path"
    )

    fun writeFileCommand(path: String, content: String): ExecuteCommand = ExecuteCommand(
        action = CommandAction.WRITE_FILE,
        args = CommandArgs(path = path, content = content),
        justification = "Stage remote push to $path",
        expectedEffect = "Write $path"
    )

    fun createDirectoryCommand(path: String): ExecuteCommand = ExecuteCommand(
        action = CommandAction.CREATE_DIRECTORY,
        args = CommandArgs(path = path),
        justification = "Stage remote create $path",
        expectedEffect = "Create $path"
    )

    /**
     * Walk `assets/stage/safe`, `assets/stage/live`, and `assets/stage/live/evilportal`.
     * [listChildren] returns immediate child names, or an empty list when the path is missing.
     */
    fun discoverStageAssetPaths(listChildren: (String) -> List<String>): List<String> {
        val root = listChildren("stage")
        if (root.isEmpty()) return emptyList()
        val found = mutableListOf<String>()
        if ("safe" in root) {
            listChildren("stage/safe").forEach { name ->
                if (name.hasFileExtension()) found += "stage/safe/$name"
            }
        }
        if ("live" in root) {
            listChildren("stage/live").forEach { name ->
                if (name == "evilportal") {
                    listChildren("stage/live/evilportal").forEach { portalFile ->
                        if (portalFile.hasFileExtension()) found += "stage/live/evilportal/$portalFile"
                    }
                } else if (name.hasFileExtension()) {
                    found += "stage/live/$name"
                }
            }
        }
        return found
    }

    fun stagePayloadFromAsset(assetPath: String): StagePayload {
        val normalized = assetPath.removePrefix("/")
        val tier = when {
            normalized.startsWith("stage/safe/") -> StageTier.SAFE
            normalized.startsWith("stage/live/") -> StageTier.LIVE
            else -> throw IllegalArgumentException("Not a stage asset: $assetPath")
        }
        val name = normalized.substringAfterLast('/')
        if (!name.hasFileExtension()) {
            throw IllegalArgumentException("Not a payload file: $assetPath")
        }
        val parent = normalized.substringBeforeLast('/')
        val devicePath = if (parent == "stage/live/evilportal") {
            "$EVIL_PORTAL_DIR/$name"
        } else {
            "$BADUSB_DIR/$name"
        }
        return StagePayload(
            assetPath = normalized,
            name = name,
            tier = tier,
            devicePath = devicePath
        )
    }

    fun isPayloadOnDevice(
        payload: StagePayload,
        badUsbNames: Set<String>,
        evilPortalNames: Set<String>
    ): Boolean {
        val fileName = payload.devicePath.substringAfterLast('/')
        return if (payload.devicePath.startsWith("$EVIL_PORTAL_DIR/")) {
            fileName in evilPortalNames
        } else {
            fileName in badUsbNames
        }
    }

    /**
     * Same names [com.vesper.flipper.ui.viewmodel.FapHubViewModel] stores after scanning `/ext/apps`:
     * the `.fap` stem, the catalog id, and the catalog alias.
     */
    fun isStageAppInstalled(alias: String, installedNames: Set<String>): Boolean {
        val normalized = installedNames.map { it.removeSuffix(".fap") }.toSet()
        if (alias in normalized) return true
        val app = resolveStageApp(alias) ?: return false
        val installedFile = FapHubCatalog.installPath(app).substringAfterLast('/').removeSuffix(".fap")
        return installedFile in normalized || app.id in normalized || app.catalogAlias in normalized
    }

    fun filePathsWithExtension(entries: List<FileEntry>, extension: String): List<String> =
        entries.filter { !it.isDirectory && it.name.endsWith(extension, ignoreCase = true) }
            .map { it.path }
            .filter { it.isNotBlank() }
            .sorted()

    fun childDirectories(entries: List<FileEntry>): List<String> =
        entries.filter { it.isDirectory && it.path.isNotBlank() }
            .map { it.path }
            .sorted()

    fun isTransmittableFile(path: String, directory: String, extension: String): Boolean {
        val root = directory.trimEnd('/')
        if (!path.startsWith("$root/")) return false
        if (path.contains("..")) return false
        val relative = path.removePrefix("$root/")
        if (relative.isBlank()) return false
        return path.endsWith(extension, ignoreCase = true)
    }

    fun feedbackFor(result: CommandResult): StageFeedback {
        if (result.success) {
            val text = result.data?.message?.takeIf { it.isNotBlank() }
                ?: result.data?.content?.lineSequence()?.firstOrNull { it.isNotBlank() }?.take(180)
                ?: result.data?.bytesWritten?.let { "Wrote $it bytes" }
                ?: "OK"
            return StageFeedback(StagePhase.OK, text)
        }
        val error = result.error?.takeIf { it.isNotBlank() } ?: "Command failed"
        return StageFeedback(StagePhase.ERROR, error)
    }

    private fun badUsb(id: String, title: String, fileName: String, tier: StageTier): StageBeat = StageBeat(
        id = id,
        title = title,
        subtitle = fileName,
        station = "D1",
        tier = tier,
        action = StageBeatAction.BadUsbExecute(fileName)
    )
}

private fun String.hasFileExtension(): Boolean =
    isNotBlank() && !contains('/') && !contains('\\') && substringAfterLast('.', "").isNotEmpty()
