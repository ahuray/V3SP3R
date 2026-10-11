package com.vesper.flipper.domain.model

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The dolphin passport the Flipper draws on its idle screen.
 *
 * Mood and portrait live in `/int/.dolphin.state` (saved-struct, little-endian).
 * Idle animations listed in `/ext/dolphin/manifest.txt` are the moving screensavers.
 * The name printed on the passport is the hardware name. Official firmware does not
 * give a file or RPC that can change it.
 */
enum class DolphinMood(val label: String, val butthurt: Int) {
    HAPPY("Happy", 0),
    OK("Ok", 7),
    ANGRY("Angry", 12);

    companion object {
        fun fromButthurt(butthurt: Int): DolphinMood = when {
            butthurt <= 4 -> HAPPY
            butthurt <= 9 -> OK
            else -> ANGRY
        }
    }
}

object DolphinStateFile {
    const val PATH = "/int/.dolphin.state"
    const val MAGIC = 0xD0
    const val VERSION = 0x01
    const val HEADER_SIZE = 8
    const val PAYLOAD_SIZE = 28
    const val HAPPY_MODE_FLAG = 1
    private const val FLAGS_OFFSET = 8
    private const val ICOUNTER_OFFSET = 12
    private const val BUTTHURT_OFFSET = 16
    private const val LEVEL_2_XP = 301
    private const val LEVEL_3_XP = 1801

    data class Snapshot(
        val mood: DolphinMood,
        val level: Int,
        val happyMode: Boolean
    )

    fun parse(bytes: ByteArray): Snapshot? {
        if (bytes.size < HEADER_SIZE + PAYLOAD_SIZE) return null
        if (bytes[0].toInt() and 0xFF != MAGIC) return null
        if (bytes[1].toInt() and 0xFF != VERSION) return null
        val payload = bytes.copyOfRange(HEADER_SIZE, HEADER_SIZE + PAYLOAD_SIZE)
        if ((bytes[2].toInt() and 0xFF) != checksum(payload)) return null
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val flags = buffer.getInt(FLAGS_OFFSET)
        val icounter = buffer.getInt(ICOUNTER_OFFSET).toLong() and 0xFFFF_FFFFL
        val butthurt = buffer.getInt(BUTTHURT_OFFSET)
        if (butthurt !in 0..14) return null
        return Snapshot(
            mood = DolphinMood.fromButthurt(butthurt),
            level = levelForXp(icounter),
            happyMode = flags and HAPPY_MODE_FLAG != 0
        )
    }

    fun withMoodAndLevel(bytes: ByteArray, mood: DolphinMood, level: Int): ByteArray? {
        if (parse(bytes) == null || level !in 1..3) return null
        val updated = bytes.copyOf()
        val payload = updated.copyOfRange(HEADER_SIZE, HEADER_SIZE + PAYLOAD_SIZE)
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        var flags = buffer.getInt(FLAGS_OFFSET)
        flags = if (mood == DolphinMood.HAPPY) {
            flags or HAPPY_MODE_FLAG
        } else {
            flags and HAPPY_MODE_FLAG.inv()
        }
        buffer.putInt(FLAGS_OFFSET, flags)
        buffer.putInt(ICOUNTER_OFFSET, xpForLevel(level))
        buffer.putInt(BUTTHURT_OFFSET, mood.butthurt)
        payload.copyInto(updated, HEADER_SIZE)
        updated[2] = checksum(payload).toByte()
        return updated
    }

    fun levelForXp(icounter: Long): Int = when {
        icounter <= 300L -> 1
        icounter <= 1800L -> 2
        else -> 3
    }

    private fun xpForLevel(level: Int): Int = when (level) {
        1 -> 0
        2 -> LEVEL_2_XP
        else -> LEVEL_3_XP
    }

    private fun checksum(payload: ByteArray): Int {
        var sum = 0
        for (byte in payload) {
            sum = (sum + (byte.toInt() and 0xFF)) and 0xFF
        }
        return sum
    }
}

data class DolphinAnimation(
    val name: String,
    val weight: Int
)

object DolphinManifest {
    const val PATH = "/ext/dolphin/manifest.txt"
    private const val PREFERRED_WEIGHT = 8

    fun parse(text: String): List<DolphinAnimation> {
        return blocks(text).map { DolphinAnimation(it.name, it.weight) }
    }

    /** The external animation currently preferred, if one has a positive weight. */
    fun activeName(text: String): String? {
        return parse(text).filter { it.weight > 0 }.maxByOrNull { it.weight }?.name
    }

    /**
     * Prefer [name] for every mood and portrait. Pass null to leave the built-in
     * animations in charge. Other SD animations stay listed with weight 0.
     */
    fun select(text: String, name: String?): String {
        val parsed = blocks(text)
        if (parsed.isEmpty()) return text
        val header = text.lineSequence().takeWhile { !it.startsWith("Name:") }.toList()
        val body = parsed.joinToString(separator = "\n\n") { block ->
            val chosen = name != null && block.name == name
            val fields = linkedMapOf(
                "Name" to block.name,
                "Min butthurt" to if (chosen) "0" else block.minButthurt,
                "Max butthurt" to if (chosen) "14" else block.maxButthurt,
                "Min level" to if (chosen) "1" else block.minLevel,
                "Max level" to if (chosen) "3" else block.maxLevel,
                "Weight" to if (chosen) PREFERRED_WEIGHT.toString() else "0"
            )
            fields.entries.joinToString("\n") { (key, value) -> "$key: $value" }
        }
        val head = header.joinToString("\n").trimEnd()
        return if (head.isEmpty()) "$body\n" else "$head\n\n$body\n"
    }

    private data class Block(
        val name: String,
        val minButthurt: String,
        val maxButthurt: String,
        val minLevel: String,
        val maxLevel: String,
        val weight: Int
    )

    private fun blocks(text: String): List<Block> {
        val found = mutableListOf<Block>()
        var name: String? = null
        var minButthurt = "0"
        var maxButthurt = "14"
        var minLevel = "1"
        var maxLevel = "3"
        var weight = 0
        fun flush() {
            val current = name ?: return
            found += Block(current, minButthurt, maxButthurt, minLevel, maxLevel, weight)
            name = null
            minButthurt = "0"
            maxButthurt = "14"
            minLevel = "1"
            maxLevel = "3"
            weight = 0
        }
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line.startsWith("Name:") -> {
                    flush()
                    name = line.substringAfter(":").trim()
                }
                line.startsWith("Min butthurt:") -> minButthurt = line.substringAfter(":").trim()
                line.startsWith("Max butthurt:") -> maxButthurt = line.substringAfter(":").trim()
                line.startsWith("Min level:") -> minLevel = line.substringAfter(":").trim()
                line.startsWith("Max level:") -> maxLevel = line.substringAfter(":").trim()
                line.startsWith("Weight:") -> weight = line.substringAfter(":").trim().toIntOrNull() ?: 0
            }
        }
        flush()
        return found
    }
}
