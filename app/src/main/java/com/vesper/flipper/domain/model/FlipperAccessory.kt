package com.vesper.flipper.domain.model

enum class AccessoryOrigin {
    OFFICIAL,
    UNOFFICIAL
}

data class FlipperAccessory(
    val id: String,
    val name: String,
    val origin: AccessoryOrigin,
    val mount: String
)

/**
 * Boards that dock on a Flipper. Nothing in this catalog is treated as present
 * until the person marks it mounted. Official names match the Flipper shop.
 * Unofficial boards stay out of the list until that section is opened.
 */
object FlipperAccessories {
    val official: List<FlipperAccessory> = listOf(
        FlipperAccessory(
            id = "wifi_devboard",
            name = "Wi-Fi Developer Board",
            origin = AccessoryOrigin.OFFICIAL,
            mount = "ESP32-S2 on the top GPIO header, with its own antenna. The Flipper has no Wi-Fi radio without a board like this."
        ),
        FlipperAccessory(
            id = "video_game_module",
            name = "Video Game Module",
            origin = AccessoryOrigin.OFFICIAL,
            mount = "Sits on the GPIO header. Adds a motion sensor, speaker, vibration, and extra buttons."
        ),
        FlipperAccessory(
            id = "prototyping_boards",
            name = "Prototyping Boards",
            origin = AccessoryOrigin.OFFICIAL,
            mount = "Blank boards for a circuit of your own. They seat on the GPIO header and do not identify themselves."
        )
    )

    val unofficial: List<FlipperAccessory> = listOf(
        FlipperAccessory(
            id = "5ghost",
            name = "5Ghost",
            origin = AccessoryOrigin.UNOFFICIAL,
            mount = "Community RTL8720DN board, 2.4 and 5 GHz, on the GPIO header. Not sold by Flipper."
        ),
        FlipperAccessory(
            id = "third_party_esp32",
            name = "Other ESP32 board",
            origin = AccessoryOrigin.UNOFFICIAL,
            mount = "An ESP32 on the GPIO header that is not the official Wi-Fi Developer Board."
        ),
        FlipperAccessory(
            id = "external_subghz_antenna",
            name = "External Sub-GHz antenna",
            origin = AccessoryOrigin.UNOFFICIAL,
            mount = "A community antenna on the Sub-GHz radio. The Flipper already has an internal antenna."
        )
    )

    fun byId(id: String): FlipperAccessory? = (official + unofficial).firstOrNull { it.id == id }
}
