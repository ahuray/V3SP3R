package com.vesper.flipper.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vesper.flipper.ble.PassportSnapshot
import com.vesper.flipper.domain.model.DolphinMood

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassportCard(
    deviceName: String,
    passport: PassportSnapshot,
    busy: Boolean,
    status: String?,
    onApply: (
        mood: DolphinMood,
        level: Int,
        animation: String?,
        updateState: Boolean,
        updateAnimation: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var mood by rememberSaveable { mutableStateOf(DolphinMood.HAPPY.name) }
    var level by rememberSaveable { mutableStateOf(1) }
    var animation by rememberSaveable { mutableStateOf("") }
    var edited by rememberSaveable { mutableStateOf(false) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var confirmRestart by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(passport.mood, passport.level, passport.activeAnimation) {
        if (!edited) {
            passport.mood?.let { mood = it.name }
            passport.level?.let { level = it }
            animation = passport.activeAnimation.orEmpty()
        }
    }
    val selectedMood = DolphinMood.entries.firstOrNull { it.name == mood } ?: DolphinMood.HAPPY
    val selectedAnimation = animation.ifBlank { null }
    val animationChanged = selectedAnimation != passport.activeAnimation
    val stateChanged = passport.stateReady &&
        (selectedMood != passport.mood || level != passport.level)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Passport", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = if (deviceName.isBlank()) "Name is on the Flipper" else "Name: $deviceName",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "The name is stored in the Flipper's hardware ID. This firmware has no safe rename.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Mood", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DolphinMood.entries.forEach { choice ->
                    FilterChip(
                        selected = selectedMood == choice,
                        onClick = {
                            edited = true
                            mood = choice.name
                        },
                        enabled = passport.stateReady && !busy,
                        label = { Text(choice.label) }
                    )
                }
            }
            Text("Portrait", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3).forEach { choice ->
                    FilterChip(
                        selected = level == choice,
                        onClick = {
                            edited = true
                            level = choice
                        },
                        enabled = passport.stateReady && !busy,
                        label = { Text("Level $choice") }
                    )
                }
            }
            Text("Screensaver", style = MaterialTheme.typography.labelLarge)
            ExposedDropdownMenuBox(
                expanded = menuOpen,
                onExpandedChange = { if (!busy) menuOpen = it }
            ) {
                OutlinedTextField(
                    value = selectedAnimation ?: "Built-in mix",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    label = { Text("Idle animation") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuOpen) }
                )
                ExposedDropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Built-in mix") },
                        onClick = {
                            edited = true
                            animation = ""
                            menuOpen = false
                        }
                    )
                    passport.animations.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                        onClick = {
                            edited = true
                            animation = name
                            menuOpen = false
                        }
                        )
                    }
                }
            }
            Text(
                text = "Custom animations live in /ext/dolphin. A chosen one plays most often. Built-in ones can still appear.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    if (stateChanged) confirmRestart = true
                    else if (animationChanged) {
                        onApply(selectedMood, level, selectedAnimation, false, true)
                    }
                },
                enabled = !busy && (stateChanged || animationChanged),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (busy) "Saving…" else "Apply on Flipper")
            }
            Text(
                text = status ?: "Mood and portrait need a restart. The animation changes on the next idle screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.heightIn(min = 48.dp)
            )
        }
    }

    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text("Restart the Flipper?") },
            text = {
                Text("Mood and portrait are applied from memory at boot. The Flipper will disconnect and come back.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestart = false
                    onApply(selectedMood, level, selectedAnimation, true, animationChanged)
                }) { Text("Restart") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestart = false }) { Text("Cancel") }
            }
        )
    }
}
