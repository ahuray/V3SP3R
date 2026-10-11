package com.vesper.flipper.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vesper.flipper.domain.model.AccessoryOrigin
import com.vesper.flipper.domain.model.FlipperAccessories
import com.vesper.flipper.domain.model.FlipperAccessory

@Composable
fun AccessoriesPage(
    connected: Boolean,
    mountedIds: Set<String>,
    onMountedChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showUnofficial by rememberSaveable { mutableStateOf(false) }
    val marked = mountedIds.mapNotNull(FlipperAccessories::byId)
        .count { it.origin == AccessoryOrigin.OFFICIAL || showUnofficial }
    val status = when {
        !connected -> "Connect the Flipper to show mounted boards."
        marked == 0 -> "No boards marked."
        else -> "Marked boards are described under their switch."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Mark a board only after it is on the header. Nothing is listed until you do that, and a marked board is shown only while the Flipper is connected.",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            status,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text("Official", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        FlipperAccessories.official.forEach { accessory ->
            AccessorySwitchRow(
                accessory = accessory,
                mounted = accessory.id in mountedIds,
                connected = connected,
                onMountedChange = onMountedChange
            )
        }

        Divider()
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Unofficial boards",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Switch(checked = showUnofficial, onCheckedChange = { showUnofficial = it })
        }
        if (showUnofficial) {
            FlipperAccessories.unofficial.forEach { accessory ->
                AccessorySwitchRow(
                    accessory = accessory,
                    mounted = accessory.id in mountedIds,
                    connected = connected,
                    onMountedChange = onMountedChange
                )
            }
        }
    }
}

@Composable
private fun AccessorySwitchRow(
    accessory: FlipperAccessory,
    mounted: Boolean,
    connected: Boolean,
    onMountedChange: (String, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(accessory.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (accessory.origin == AccessoryOrigin.OFFICIAL) "Official" else "Unofficial",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = mounted,
                onCheckedChange = { onMountedChange(accessory.id, it) }
            )
        }
        if (mounted && connected) {
            Text(
                "Marked on the header. This app cannot see the board electrically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                accessory.mount,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
