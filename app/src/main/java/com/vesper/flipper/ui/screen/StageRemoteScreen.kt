@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.vesper.flipper.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vesper.flipper.domain.model.InstallStatus
import com.vesper.flipper.domain.model.StageBeat
import com.vesper.flipper.domain.model.StageFeedback
import com.vesper.flipper.domain.model.StagePhase
import com.vesper.flipper.domain.model.StageRemoteCatalog
import com.vesper.flipper.domain.model.StageTier
import com.vesper.flipper.ui.components.ApprovalDialog
import com.vesper.flipper.ui.theme.RiskHigh
import com.vesper.flipper.ui.theme.RiskLow
import com.vesper.flipper.ui.theme.VesperOrange
import com.vesper.flipper.ui.viewmodel.FapHubViewModel
import com.vesper.flipper.ui.viewmodel.StageConfirmRequest
import com.vesper.flipper.ui.viewmodel.StageFileChoice
import com.vesper.flipper.ui.viewmodel.StagePayloadRow
import com.vesper.flipper.ui.viewmodel.StageRemoteViewModel

@Composable
fun StageRemoteScreen(
    onNavigateBack: () -> Unit,
    viewModel: StageRemoteViewModel = hiltViewModel(),
    fapHubViewModel: FapHubViewModel = hiltViewModel()
) {
    val beatFeedback by viewModel.beatFeedback.collectAsState()
    val payloads by viewModel.payloads.collectAsState()
    val assetsScanned by viewModel.assetsScanned.collectAsState()
    val filePicker by viewModel.filePicker.collectAsState()
    val confirmRequest by viewModel.confirmRequest.collectAsState()
    val pendingApproval by viewModel.pendingApproval.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val banner by viewModel.banner.collectAsState()

    val installedApps by fapHubViewModel.installedApps.collectAsState()
    val installStatus by fapHubViewModel.installStatus.collectAsState()
    val packProgress by fapHubViewModel.packProgress.collectAsState()
    val installError by fapHubViewModel.error.collectAsState()
    val installing = packProgress != null
    val controlsLocked = busy || installing

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(banner) {
        banner?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearBanner()
        }
    }
    LaunchedEffect(installError) {
        installError?.let {
            snackbarHostState.showSnackbar(it)
            fapHubViewModel.clearError()
        }
    }

    val groupedBeats = remember { StageRemoteCatalog.beats.groupBy { it.station } }

    pendingApproval?.let { approval ->
        ApprovalDialog(
            approval = approval,
            onApprove = { viewModel.approveExecutor() },
            onReject = { viewModel.rejectExecutor() }
        )
    }
    if (pendingApproval == null) {
        confirmRequest?.let { request ->
            StageConfirmDialog(
                request = request,
                onConfirm = { viewModel.confirmPending() },
                onDismiss = { viewModel.dismissConfirm() }
            )
        }
        filePicker?.let { choice ->
            val beat = StageRemoteCatalog.beatById(choice.beatId)
            if (beat != null && confirmRequest == null) {
                StageFilePickerDialog(
                    choice = choice,
                    beat = beat,
                    onPick = { viewModel.onFilePicked(it) },
                    onDismiss = { viewModel.dismissFilePicker() }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Stage Remote", fontWeight = FontWeight.Bold)
                        Text(
                            "Trigger beats while the Flipper stays cabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.refresh()
                            fapHubViewModel.refresh()
                        },
                        enabled = !controlsLocked
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = VesperOrange,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(
                    title = "Beats",
                    subtitle = "SAFE is show material. LIVE always asks before it runs."
                )
            }
            for ((station, stationBeats) in groupedBeats) {
                item(key = "station-$station") {
                    Text(
                        text = station,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = VesperOrange
                    )
                }
                items(stationBeats, key = { it.id }) { beat ->
                    BeatCard(
                        beat = beat,
                        feedback = beatFeedback[beat.id],
                        enabled = !controlsLocked,
                        onClick = { viewModel.onBeatClicked(beat.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Push payloads",
                    subtitle = "Bundled stage scripts, written through the Flipper."
                )
            }
            if (!assetsScanned || payloads.isNotEmpty()) {
                item {
                    PayloadToolbar(
                        payloads = payloads,
                        assetsScanned = assetsScanned,
                        enabled = !controlsLocked,
                        onPushTier = { viewModel.pushTier(it) }
                    )
                }
            }
            if (assetsScanned && payloads.isEmpty()) {
                item {
                    EmptyPayloadsCard()
                }
            } else {
                items(payloads, key = { it.payload.assetPath }) { row ->
                    PayloadCard(
                        row = row,
                        enabled = !controlsLocked,
                        onPush = { viewModel.pushPayload(row.payload.assetPath) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Stage apps",
                    subtitle = "Installed from the Flipper catalog, same path as FapHub."
                )
            }
            item {
                packProgress?.let { progress ->
                    Text(
                        text = progress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = {
                        val apps = StageRemoteCatalog.stageApps.mapNotNull {
                            StageRemoteCatalog.resolveStageApp(it.alias)
                        }
                        fapHubViewModel.installApps(apps)
                    },
                    enabled = !controlsLocked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (installing) "Installing…" else "Install all")
                }
            }
            items(StageRemoteCatalog.stageApps, key = { it.alias }) { spec ->
                val app = StageRemoteCatalog.resolveStageApp(spec.alias)
                val status = installStatus[spec.alias] ?: app?.let { installStatus[it.id] }
                StageAppCard(
                    friendlyName = spec.friendlyName,
                    alias = spec.alias,
                    installed = StageRemoteCatalog.isStageAppInstalled(spec.alias, installedApps),
                    missingFromCatalog = app == null,
                    status = status,
                    enabled = !controlsLocked && app != null,
                    onInstall = { app?.let { fapHubViewModel.installApp(it) } }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BeatCard(
    beat: StageBeat,
    feedback: StageFeedback?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val live = beat.tier == StageTier.LIVE
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (live) {
                RiskHigh.copy(alpha = 0.10f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (live) BorderStroke(1.dp, RiskHigh) else null
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = beat.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TierChip(tier = beat.tier)
            }
            Text(
                text = beat.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = beat.summaryTarget(),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = if (live) RiskHigh else VesperOrange,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            feedback?.let { FeedbackLine(it) }
        }
    }
}

@Composable
private fun PayloadToolbar(
    payloads: List<StagePayloadRow>,
    assetsScanned: Boolean,
    enabled: Boolean,
    onPushTier: (StageTier) -> Unit
) {
    if (!assetsScanned) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = VesperOrange)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reading stage assets…", style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    if (payloads.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { onPushTier(StageTier.SAFE) },
            enabled = enabled && payloads.any { it.payload.tier == StageTier.SAFE }
        ) { Text("Push all safe") }
        OutlinedButton(
            onClick = { onPushTier(StageTier.LIVE) },
            enabled = enabled && payloads.any { it.payload.tier == StageTier.LIVE }
        ) { Text("Push all live", color = RiskHigh) }
    }
}

@Composable
private fun EmptyPayloadsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = StageRemoteCatalog.EMPTY_ASSETS_MESSAGE,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PayloadCard(
    row: StagePayloadRow,
    enabled: Boolean,
    onPush: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (row.payload.tier == StageTier.LIVE) BorderStroke(1.dp, RiskHigh) else null
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.payload.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        row.payload.devicePath,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(onClick = onPush, enabled = enabled) { Text("Push") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TierChip(tier = row.payload.tier)
                PresenceChip(onDevice = row.onDevice)
            }
            row.feedback?.let { FeedbackLine(it) }
        }
    }
}

@Composable
private fun StageAppCard(
    friendlyName: String,
    alias: String,
    installed: Boolean,
    missingFromCatalog: Boolean,
    status: InstallStatus?,
    enabled: Boolean,
    onInstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(friendlyName, fontWeight = FontWeight.SemiBold)
                    Text(
                        alias,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onInstall, enabled = enabled) { Text("Install") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PresenceChip(onDevice = installed, onLabel = "Installed", offLabel = "Not installed")
                if (missingFromCatalog) {
                    StatusChip("Not in catalog", RiskHigh)
                }
            }
            when (status) {
                is InstallStatus.Downloading -> FeedbackLine(StageFeedback(StagePhase.RUNNING, "Downloading"))
                is InstallStatus.Installing -> FeedbackLine(StageFeedback(StagePhase.RUNNING, "Installing"))
                is InstallStatus.Success -> FeedbackLine(StageFeedback(StagePhase.OK, "Installed"))
                is InstallStatus.Error -> FeedbackLine(StageFeedback(StagePhase.ERROR, status.message))
                else -> Unit
            }
        }
    }
}

@Composable
private fun TierChip(tier: StageTier) {
    val live = tier == StageTier.LIVE
    StatusChip(
        label = if (live) "LIVE" else "SAFE",
        color = if (live) RiskHigh else RiskLow
    )
}

@Composable
private fun PresenceChip(
    onDevice: Boolean,
    onLabel: String = "On device",
    offLabel: String = "Not on device"
) {
    StatusChip(
        label = if (onDevice) onLabel else offLabel,
        color = if (onDevice) RiskLow else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun StatusChip(label: String, color: androidx.compose.ui.graphics.Color) {
    Surface(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(6.dp)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FeedbackLine(feedback: StageFeedback) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (feedback.phase == StagePhase.RUNNING) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = VesperOrange)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = feedback.text,
            style = MaterialTheme.typography.bodySmall,
            color = when (feedback.phase) {
                StagePhase.RUNNING -> MaterialTheme.colorScheme.onSurfaceVariant
                StagePhase.OK -> RiskLow
                StagePhase.ERROR -> RiskHigh
            }
        )
    }
}

@Composable
private fun StageFilePickerDialog(
    choice: StageFileChoice,
    beat: StageBeat,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(beat.title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    choice.directory,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                choice.files.forEach { path ->
                    TextButton(onClick = { onPick(path) }, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(path.substringAfterLast('/'))
                            Text(
                                path,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun StageConfirmDialog(
    request: StageConfirmRequest,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val title: String
    val body: String
    val confirmLabel: String
    when (request) {
        is StageConfirmRequest.Beat -> {
            val beat = StageRemoteCatalog.beatById(request.beatId)
            title = "Run live beat?"
            body = buildString {
                append(beat?.title ?: request.beatId)
                beat?.subtitle?.let {
                    append('\n')
                    append(it)
                }
                request.selectedPath?.let {
                    append("\n\n")
                    append(it)
                }
            }
            confirmLabel = "Run"
        }
        is StageConfirmRequest.Payloads -> {
            title = "Push live payload?"
            body = request.assetPaths.joinToString("\n") { it.substringAfterLast('/') }
            confirmLabel = "Push"
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = RiskHigh) },
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = RiskHigh, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
