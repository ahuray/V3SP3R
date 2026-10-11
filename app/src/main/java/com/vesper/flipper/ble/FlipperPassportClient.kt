package com.vesper.flipper.ble

import com.vesper.flipper.domain.model.DolphinManifest
import com.vesper.flipper.domain.model.DolphinMood
import com.vesper.flipper.domain.model.DolphinStateFile

data class PassportSnapshot(
    val mood: DolphinMood?,
    val level: Int?,
    val animations: List<String>,
    val activeAnimation: String?,
    val stateReady: Boolean,
    val manifestReady: Boolean,
    val note: String?
)

data class PassportApplyResult(
    val restartRequired: Boolean,
    val message: String
)

class FlipperPassportClient(
    private val fileSystem: FlipperFileSystem
) {
    suspend fun load(): PassportSnapshot {
        val state = fileSystem.readFileBytes(DolphinStateFile.PATH).getOrNull()
            ?.let(DolphinStateFile::parse)
        val manifestText = fileSystem.readFile(DolphinManifest.PATH).getOrNull()
        val listed = fileSystem.listDirectory("/ext/dolphin").getOrNull()
            .orEmpty()
            .filter { it.isDirectory }
            .map { it.name }
        val fromManifest = manifestText?.let(DolphinManifest::parse).orEmpty().map { it.name }
        val animations = (fromManifest + listed).distinct().sorted()
        val note = when {
            state == null && manifestText == null && animations.isEmpty() ->
                "Couldn't read the dolphin state. Mood and portrait stay as they are."
            state == null ->
                "Couldn't read the dolphin state, so mood and portrait can't be changed yet."
            else -> null
        }
        return PassportSnapshot(
            mood = state?.mood,
            level = state?.level,
            animations = animations,
            activeAnimation = manifestText?.let(DolphinManifest::activeName),
            stateReady = state != null,
            manifestReady = manifestText != null || animations.isNotEmpty(),
            note = note
        )
    }

    suspend fun apply(
        mood: DolphinMood,
        level: Int,
        animation: String?,
        updateState: Boolean,
        updateAnimation: Boolean
    ): Result<PassportApplyResult> {
        var restartRequired = false
        if (updateState) {
            val current = fileSystem.readFileBytes(DolphinStateFile.PATH).getOrElse { error ->
                return Result.failure(error)
            }
            val parsed = DolphinStateFile.parse(current)
                ?: return Result.failure(IllegalStateException("The dolphin state file is not a version this app can edit."))
            val changed = parsed.mood != mood || parsed.level != level ||
                (mood == DolphinMood.HAPPY && !parsed.happyMode) ||
                (mood != DolphinMood.HAPPY && parsed.happyMode)
            if (changed) {
                val updated = DolphinStateFile.withMoodAndLevel(current, mood, level)
                    ?: return Result.failure(IllegalStateException("Couldn't update the dolphin state."))
                fileSystem.writeFileBytes(DolphinStateFile.PATH, updated).getOrElse { error ->
                    return Result.failure(error)
                }
                restartRequired = true
            }
        }

        var animationMessage: String? = null
        val manifestText = fileSystem.readFile(DolphinManifest.PATH).getOrNull()
        if (updateAnimation && (animation != null || manifestText != null)) {
            val base = manifestText ?: """
                Filetype: Flipper Animation Manifest
                Version: 1
            """.trimIndent() + "\n"
            val withName = if (animation != null) ensureListed(base, animation) else base
            val currentActive = DolphinManifest.activeName(withName)
            if (currentActive != animation) {
                val selected = DolphinManifest.select(withName, animation)
                fileSystem.writeFile(DolphinManifest.PATH, selected).getOrElse { error ->
                    return Result.failure(error)
                }
                animationMessage = if (animation == null) {
                    "Built-in animations are back in charge."
                } else {
                    "\"$animation\" plays most often on the next idle screen. Built-in animations can still appear."
                }
            }
        }

        val message = when {
            restartRequired && animationMessage != null ->
                "Saved. The Flipper is restarting so the new mood and portrait show. $animationMessage"
            restartRequired ->
                "Saved. The Flipper is restarting so the new mood and portrait show."
            animationMessage != null -> animationMessage
            else -> "Nothing to change."
        }
        return Result.success(PassportApplyResult(restartRequired, message))
    }

    private fun ensureListed(text: String, name: String): String {
        if (DolphinManifest.parse(text).any { it.name == name }) return text
        val block = """
            Name: $name
            Min butthurt: 0
            Max butthurt: 14
            Min level: 1
            Max level: 3
            Weight: 0
        """.trimIndent()
        return text.trimEnd() + "\n\n" + block + "\n"
    }
}
