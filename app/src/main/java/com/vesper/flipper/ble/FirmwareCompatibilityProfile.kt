package com.vesper.flipper.ble

enum class FirmwareFamily {
    UNKNOWN,
    OFFICIAL,
    MOMENTUM,
    UNLEASHED,
    ROGUEMASTER,
    XTREME
}

enum class FirmwareTransportMode {
    UNAVAILABLE,
    PROBING,
    CLI_ONLY,
    RPC_ONLY,
    CLI_AND_RPC
}

enum class FirmwareCommandRoute {
    DIRECT_CLI,
    RPC_APP_BRIDGE,
    UNSUPPORTED
}

data class FirmwareCompatibilityProfile(
    val family: FirmwareFamily = FirmwareFamily.UNKNOWN,
    val label: String = "Unknown",
    val transportMode: FirmwareTransportMode = FirmwareTransportMode.UNAVAILABLE,
    val supportsCli: Boolean = false,
    val supportsRpc: Boolean = false,
    val supportsRpcAppBridge: Boolean = false,
    val confidence: Float = 0f,
    val notes: String = "No firmware compatibility data yet."
)

data class FirmwareCommandCompatibility(
    val supported: Boolean,
    val route: FirmwareCommandRoute,
    val message: String
)

/**
 * Names a custom firmware from a version banner or device-info blob.
 * A generic "Flipper" / "firmware" string is not enough to call it stock.
 */
object FirmwareIdentity {
    fun detect(text: String?): FirmwareFamily? {
        val hint = text?.lowercase().orEmpty()
        if (hint.isBlank()) return null
        return when {
            hint.contains("momentum") || hint.contains("mntm") -> FirmwareFamily.MOMENTUM
            hint.contains("unleashed") || hint.contains("unlshd") -> FirmwareFamily.UNLEASHED
            hint.contains("roguemaster") || hint.contains("rogue master") -> FirmwareFamily.ROGUEMASTER
            hint.contains("xtreme") -> FirmwareFamily.XTREME
            else -> null
        }
    }

    fun looksOfficial(text: String?): Boolean {
        val hint = text?.lowercase()?.trim().orEmpty()
        if (hint.isBlank() || detect(text) != null) return false
        if (hint.contains("official") || hint.contains("ofw")) return true
        return STOCK_VERSION.containsMatchIn(hint)
    }

    private val STOCK_VERSION = Regex("""\b\d+\.\d+\.\d+(?:-rc)?\b""")
}

object FirmwareCompatibilityLayer {

    fun assessCliCommand(
        profile: FirmwareCompatibilityProfile,
        command: String,
        hasRpcMapping: Boolean
    ): FirmwareCommandCompatibility {
        val normalized = command.trim().lowercase()
        if (normalized.isBlank()) {
            return FirmwareCommandCompatibility(
                supported = false,
                route = FirmwareCommandRoute.UNSUPPORTED,
                message = "Command is empty."
            )
        }

        // During PROBING, allow commands through even though supportsCli/supportsRpc
        // haven't been confirmed yet — the probe hasn't completed.
        if (profile.transportMode == FirmwareTransportMode.PROBING) {
            return FirmwareCommandCompatibility(
                supported = true,
                route = if (hasRpcMapping) FirmwareCommandRoute.RPC_APP_BRIDGE
                         else FirmwareCommandRoute.DIRECT_CLI,
                message = "Transport probing in progress — attempting command."
            )
        }

        if (!profile.supportsCli && !profile.supportsRpc) {
            return FirmwareCommandCompatibility(
                supported = false,
                route = FirmwareCommandRoute.UNSUPPORTED,
                message = "Automation transport is unavailable for this connection."
            )
        }

        return when (profile.transportMode) {
            FirmwareTransportMode.CLI_AND_RPC -> {
                if (hasRpcMapping && prefersRpcBridge(normalized)) {
                    FirmwareCommandCompatibility(
                        supported = true,
                        route = FirmwareCommandRoute.RPC_APP_BRIDGE,
                        message = "Using RPC app bridge for best compatibility on ${profile.label}."
                    )
                } else {
                    FirmwareCommandCompatibility(
                        supported = true,
                        route = FirmwareCommandRoute.DIRECT_CLI,
                        message = "Using direct CLI on ${profile.label}."
                    )
                }
            }

            FirmwareTransportMode.CLI_ONLY -> FirmwareCommandCompatibility(
                supported = true,
                route = FirmwareCommandRoute.DIRECT_CLI,
                message = "Using CLI-only transport."
            )

            FirmwareTransportMode.RPC_ONLY -> {
                if (hasRpcMapping) {
                    FirmwareCommandCompatibility(
                        supported = true,
                        route = FirmwareCommandRoute.RPC_APP_BRIDGE,
                        message = "CLI is unavailable on this transport, routing via RPC app bridge."
                    )
                } else {
                    FirmwareCommandCompatibility(
                        supported = false,
                        route = FirmwareCommandRoute.UNSUPPORTED,
                        message = "CLI is unavailable on this transport and this command has no RPC mapping."
                    )
                }
            }

            FirmwareTransportMode.PROBING -> FirmwareCommandCompatibility(
                supported = true,
                route = if (hasRpcMapping) FirmwareCommandRoute.RPC_APP_BRIDGE
                         else FirmwareCommandRoute.DIRECT_CLI,
                message = "Transport probing in progress — attempting command."
            )

            FirmwareTransportMode.UNAVAILABLE -> FirmwareCommandCompatibility(
                supported = false,
                route = FirmwareCommandRoute.UNSUPPORTED,
                message = "Command transport is unavailable."
            )
        }
    }

    private fun prefersRpcBridge(command: String): Boolean {
        return command.startsWith("badusb ") ||
                command.startsWith("subghz tx ") ||
                command.startsWith("subghz tx_from_file ") ||
                command.startsWith("ir tx ") ||
                command.startsWith("infrared tx ") ||
                command == "ble_spam" ||
                command == "blespam" ||
                command == "ble spam" ||
                command.startsWith("ble_spam ") ||
                command.startsWith("blespam ") ||
                command.startsWith("ble spam ") ||
                command.startsWith("nfc emulate ") ||
                command.startsWith("nfc emu ") ||
                command.startsWith("rfid emulate ") ||
                command.startsWith("ibutton emulate ")
    }
}
