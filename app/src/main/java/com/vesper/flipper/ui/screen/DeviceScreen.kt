package com.vesper.flipper.ui.screen

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import com.vesper.flipper.R
import com.vesper.flipper.ble.CliCapabilityLevel
import com.vesper.flipper.ble.CliCapabilityStatus
import com.vesper.flipper.ble.CommandPipelineAutotuneStatus
import com.vesper.flipper.ble.ConnectionCheckLevel
import com.vesper.flipper.ble.ConnectionDiagnosticsReport
import com.vesper.flipper.ble.ConnectionState
import com.vesper.flipper.ble.FirmwareCompatibilityProfile
import com.vesper.flipper.ble.FirmwareTransportMode
import com.vesper.flipper.ble.FlipperDevice
import com.vesper.flipper.ble.FlipperScreenImage
import com.vesper.flipper.ble.FlipperScreenOrientation
import com.vesper.flipper.ble.PassportSnapshot
import com.vesper.flipper.domain.model.DolphinMood
import com.vesper.flipper.domain.model.FlipperRemoteButton
import com.vesper.flipper.glasses.BridgeState
import com.vesper.flipper.ui.theme.*
import com.vesper.flipper.ui.viewmodel.DeviceViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    viewModel: DeviceViewModel = hiltViewModel(),
    onNavigateToFiles: () -> Unit = {},
    onNavigateToOracle: () -> Unit = {},
    onNavigateToArsenal: () -> Unit = {},
    onNavigateToPayloadLab: () -> Unit = {},
    onNavigateToStageRemote: () -> Unit = {},
    onNavigateToOps: () -> Unit = {},
    onNavigateToAudit: () -> Unit = {}
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val storageInfo by viewModel.storageInfo.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val cliCapabilityStatus by viewModel.cliCapabilityStatus.collectAsState()
    val connectionDiagnostics by viewModel.connectionDiagnostics.collectAsState()
    val isRunningDiagnostics by viewModel.isRunningDiagnostics.collectAsState()
    val autotuneStatus by viewModel.autotuneStatus.collectAsState()
    val firmwareCompatibility by viewModel.firmwareCompatibility.collectAsState()
    val isSendingRemoteInput by viewModel.isSendingRemoteInput.collectAsState()
    val remoteInputStatus by viewModel.remoteInputStatus.collectAsState()
    val screenFrame by viewModel.screenFrame.collectAsState()
    val screenStreamStatus by viewModel.screenStreamStatus.collectAsState()
    val passport by viewModel.passport.collectAsState()
    val passportBusy by viewModel.passportBusy.collectAsState()
    val passportStatus by viewModel.passportStatus.collectAsState()
    val mountedAccessoryIds by viewModel.mountedAccessoryIds.collectAsState()
    var devicePage by rememberSaveable { mutableStateOf(0) }
    val connected = connectionState is ConnectionState.Connected
    var liveScreen by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(connected, liveScreen) {
        if (connected && liveScreen) viewModel.startScreenMirror()
        else viewModel.stopScreenMirror()
    }
    LaunchedEffect(connected) {
        if (connected) viewModel.refreshPassport()
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.stopScreenMirror() }
    }
    val glassesEnabled by viewModel.glassesEnabled.collectAsState()
    val glassesBridgeState by viewModel.glassesBridgeState.collectAsState()
    val glassesBridgeUrl by viewModel.glassesBridgeUrl.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Device", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = onNavigateToArsenal) {
                        Icon(
                            Icons.Default.Sensors,
                            contentDescription = "Signal Arsenal"
                        )
                    }
                    IconButton(onClick = onNavigateToOracle) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = "Spectral Oracle"
                        )
                    }
                    IconButton(onClick = onNavigateToPayloadLab) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = "Payload Lab"
                        )
                    }
                    IconButton(onClick = onNavigateToStageRemote) {
                        Icon(
                            Icons.Default.SettingsRemote,
                            contentDescription = "Stage Remote"
                        )
                    }
                    if (connectionState is ConnectionState.Connected) {
                        IconButton(onClick = onNavigateToFiles) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = "File Browser",
                                tint = VesperOrange
                            )
                        }
                        IconButton(
                            onClick = { viewModel.refreshDeviceInfo() },
                            enabled = !isRefreshing
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = VesperOrange,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        TabRow(selectedTabIndex = devicePage) {
            Tab(
                selected = devicePage == 0,
                onClick = { devicePage = 0 },
                text = { Text("Flipper") }
            )
            Tab(
                selected = devicePage == 1,
                onClick = { devicePage = 1 },
                text = { Text("Accessories") }
            )
        }
        if (devicePage == 1) {
            AccessoriesPage(
                connected = connectionState is ConnectionState.Connected,
                mountedIds = mountedAccessoryIds,
                onMountedChange = viewModel::setAccessoryMounted,
                modifier = Modifier.weight(1f)
            )
        } else if (connectionState is ConnectionState.Connected) {
            RemoteControlCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                isSendingRemoteInput = isSendingRemoteInput,
                statusText = remoteInputStatus,
                screenFrame = screenFrame,
                screenStatus = screenStreamStatus,
                liveScreen = liveScreen,
                onLiveScreenChange = { liveScreen = it },
                onRetryScreen = { viewModel.startScreenMirror() },
                onDisconnect = { viewModel.disconnect() },
                onKeyDown = { button -> viewModel.remoteKeyDown(button) },
                onKeyUp = { gestureId -> viewModel.remoteKeyUp(gestureId) },
                onNavigateToFiles = onNavigateToFiles,
                onNavigateToOracle = onNavigateToOracle,
                onNavigateToArsenal = onNavigateToArsenal,
                onNavigateToPayloadLab = onNavigateToPayloadLab,
                onNavigateToStageRemote = onNavigateToStageRemote,
                onNavigateToOps = onNavigateToOps,
                onNavigateToAudit = onNavigateToAudit,
                deviceName = connectedDevice?.name ?: deviceInfo?.name.orEmpty(),
                passport = passport,
                passportBusy = passportBusy,
                passportStatus = passportStatus,
                onApplyPassport = { mood, level, animation, updateState, updateAnimation ->
                    viewModel.applyPassport(mood, level, animation, updateState, updateAnimation)
                }
            )
        } else if (connectionState !is ConnectionState.Connected) LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Status Card
            item {
                ConnectionStatusCard(
                    connectionState = connectionState,
                    connectedDevice = connectedDevice,
                    onDisconnect = { viewModel.disconnect() }
                )
            }

            item {
                ScanSection(
                    isScanning = connectionState is ConnectionState.Scanning,
                    onStartScan = { viewModel.startScan() },
                    onStopScan = { viewModel.stopScan() },
                    onConnectUsb = { viewModel.connectUsb() }
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Discovered Devices",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (discoveredDevices.isEmpty()) {
                            "None yet. Scan, then tap a Flipper here."
                        } else {
                            "Tap a Flipper to connect. The screen and the kit are below."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (discoveredDevices.isEmpty()) {
                item {
                    Text(
                        "No Flipper in range.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(discoveredDevices) { device ->
                    DeviceListItem(
                        device = device,
                        isConnecting = connectionState is ConnectionState.Connecting &&
                            (connectionState as ConnectionState.Connecting).deviceName == device.name,
                        onClick = { viewModel.connectToDevice(device) }
                    )
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    FlipperKit(
                        compact = false,
                        scrollable = false,
                        onNavigateToFiles = onNavigateToFiles,
                        onNavigateToOracle = onNavigateToOracle,
                        onNavigateToArsenal = onNavigateToArsenal,
                        onNavigateToPayloadLab = onNavigateToPayloadLab,
                        onNavigateToStageRemote = onNavigateToStageRemote,
                        onNavigateToOps = onNavigateToOps,
                        onNavigateToAudit = onNavigateToAudit,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Smart Glasses Bridge (only when enabled in Settings)
            if (glassesEnabled) {
                item {
                    GlassesBridgeStatusCard(
                        bridgeState = glassesBridgeState,
                        bridgeUrl = glassesBridgeUrl,
                        onConnect = { viewModel.connectGlassesBridge() },
                        onDisconnect = { viewModel.disconnectGlassesBridge() }
                    )
                }
            }

            item {
                CommandAutomationStatusCard(
                    connectionState = connectionState,
                    cliStatus = cliCapabilityStatus,
                    diagnostics = connectionDiagnostics,
                    autotuneStatus = autotuneStatus,
                    firmwareCompatibility = firmwareCompatibility,
                    isRunningDiagnostics = isRunningDiagnostics,
                    onRunDiagnostics = { viewModel.runConnectionDiagnostics() }
                )
            }

            // Device Info Card (when connected)
            if (connectionState is ConnectionState.Connected && deviceInfo != null) {
                item {
                    DeviceInfoCard(
                        deviceInfo = deviceInfo!!,
                        storageInfo = storageInfo
                    )
                }
            }

            // Advanced Operations (from Ops Center)
            if (connectionState is ConnectionState.Connected) {
                item {
                    AdvancedOperationsSection(viewModel = viewModel, connectionState = connectionState, isSendingRemoteInput = isSendingRemoteInput, remoteInputStatus = remoteInputStatus)
                }
            }

        }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: ConnectionState,
    connectedDevice: FlipperDevice?,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                is ConnectionState.Connected -> VesperAccent.copy(alpha = 0.1f)
                is ConnectionState.Connecting -> RiskMedium.copy(alpha = 0.1f)
                is ConnectionState.Error -> RiskHigh.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (connectionState) {
                            is ConnectionState.Connected -> VesperAccent
                            is ConnectionState.Connecting -> RiskMedium
                            is ConnectionState.Error -> RiskHigh
                            else -> MaterialTheme.colorScheme.secondary
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (connectionState) {
                        is ConnectionState.Connected -> Icons.Default.BluetoothConnected
                        is ConnectionState.Connecting -> Icons.Default.BluetoothSearching
                        is ConnectionState.Scanning -> Icons.Default.BluetoothSearching
                        is ConnectionState.Error -> Icons.Default.BluetoothDisabled
                        else -> Icons.Default.Bluetooth
                    },
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Status Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (connectionState) {
                        is ConnectionState.Connected -> "Connected"
                        is ConnectionState.Connecting -> "Connecting..."
                        is ConnectionState.Scanning -> "Scanning..."
                        is ConnectionState.Error -> "Error"
                        else -> "Disconnected"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (connectionState) {
                        is ConnectionState.Connected -> connectedDevice?.name ?: connectionState.deviceName
                        is ConnectionState.Connecting -> connectionState.deviceName
                        is ConnectionState.Error -> connectionState.message
                        else -> "No device connected"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Disconnect Button
            if (connectionState is ConnectionState.Connected) {
                IconButton(onClick = onDisconnect) {
                    Icon(
                        Icons.Default.LinkOff,
                        contentDescription = "Disconnect",
                        tint = RiskHigh
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassesBridgeStatusCard(
    bridgeState: BridgeState,
    bridgeUrl: String,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val isConnected = bridgeState is BridgeState.Connected
    val isConnecting = bridgeState is BridgeState.Connecting || bridgeState is BridgeState.Reconnecting
    val isError = bridgeState is BridgeState.Error

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isConnected -> VesperAccent.copy(alpha = 0.1f)
                isConnecting -> RiskMedium.copy(alpha = 0.1f)
                isError -> RiskHigh.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isConnected -> VesperAccent
                                isConnecting -> RiskMedium
                                isError -> RiskHigh
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isConnected -> Icons.Default.Visibility
                            isConnecting -> Icons.Default.Sync
                            isError -> Icons.Default.ErrorOutline
                            else -> Icons.Default.VisibilityOff
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Smart Glasses Bridge",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (bridgeState) {
                            is BridgeState.Connected -> "Connected"
                            is BridgeState.Connecting -> "Connecting..."
                            is BridgeState.Reconnecting ->
                                "Reconnecting (${bridgeState.attempt}/${bridgeState.maxAttempts})..."
                            is BridgeState.Error -> bridgeState.message
                            is BridgeState.Disconnected -> "Not connected"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isConnected -> VesperAccent
                            isError -> RiskHigh
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                if (isConnected) {
                    IconButton(onClick = onDisconnect) {
                        Icon(
                            Icons.Default.LinkOff,
                            contentDescription = "Disconnect Bridge",
                            tint = RiskHigh
                        )
                    }
                }
            }

            // Bridge URL
            if (bridgeUrl.isNotBlank()) {
                Text(
                    text = bridgeUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Connect / Retry button
            if (!isConnected && !isConnecting) {
                FilledTonalButton(
                    onClick = onConnect,
                    enabled = bridgeUrl.isNotBlank()
                ) {
                    Icon(Icons.Default.Cable, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isError) "Retry Connection" else "Connect to Bridge")
                }
            }

            // Connecting spinner
            if (isConnecting) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = RiskMedium,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Establishing WebSocket connection...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // No URL configured hint
            if (bridgeUrl.isBlank()) {
                Text(
                    text = "Set your bridge server URL in Settings to connect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(
    deviceInfo: com.vesper.flipper.domain.model.DeviceInfo,
    storageInfo: com.vesper.flipper.domain.model.StorageInfo?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Device Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Battery
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (deviceInfo.isCharging) Icons.Default.BatteryChargingFull
                        else Icons.Default.Battery5Bar,
                        contentDescription = null,
                        tint = when {
                            deviceInfo.batteryLevel > 50 -> VesperAccent
                            deviceInfo.batteryLevel > 20 -> RiskMedium
                            else -> RiskHigh
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Battery")
                }
                Text(
                    "${deviceInfo.batteryLevel}%${if (deviceInfo.isCharging) " (Charging)" else ""}",
                    fontWeight = FontWeight.Medium
                )
            }

            Divider()

            // Firmware
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Firmware")
                }
                Text(deviceInfo.firmwareVersion, fontWeight = FontWeight.Medium)
            }

            // Storage
            storageInfo?.let { storage ->
                Divider()

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Internal Storage")
                        }
                        Text(
                            "${formatBytes(storage.internalFree)} / ${formatBytes(storage.internalTotal)}",
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Storage progress bar
                    val usedPercent = if (storage.internalTotal > 0L) {
                        (1f - (storage.internalFree.toFloat() / storage.internalTotal.toFloat()))
                            .coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    LinearProgressIndicator(
                        progress = usedPercent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when {
                            usedPercent < 0.7f -> VesperAccent
                            usedPercent < 0.9f -> RiskMedium
                            else -> RiskHigh
                        },
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }

                if (storage.hasSdCard && storage.externalTotal != null) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SdCard, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SD Card")
                            }
                            Text(
                                "${formatBytes(storage.externalFree ?: 0)} / ${formatBytes(storage.externalTotal)}",
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val sdUsedPercent = 1f - ((storage.externalFree ?: 0).toFloat() / storage.externalTotal.toFloat())
                        LinearProgressIndicator(
                            progress = sdUsedPercent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = when {
                                sdUsedPercent < 0.7f -> VesperAccent
                                sdUsedPercent < 0.9f -> RiskMedium
                                else -> RiskHigh
                            },
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandAutomationStatusCard(
    connectionState: ConnectionState,
    cliStatus: CliCapabilityStatus,
    diagnostics: ConnectionDiagnosticsReport,
    autotuneStatus: CommandPipelineAutotuneStatus,
    firmwareCompatibility: FirmwareCompatibilityProfile,
    isRunningDiagnostics: Boolean,
    onRunDiagnostics: () -> Unit
) {
    val (title, detail, tint) = when {
        connectionState !is ConnectionState.Connected -> Triple(
            "Automation Channel",
            "Connect a Flipper to check CLI/RPC automation readiness.",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        cliStatus.level == CliCapabilityLevel.READY && cliStatus.supportsCli -> Triple(
            "Automation Ready",
            cliStatus.firmwareHint?.let { "CLI/RPC responsive ($it)." }
                ?: "CLI/RPC responsive and ready for autonomous command workflows.",
            VesperAccent
        )
        cliStatus.level == CliCapabilityLevel.READY && cliStatus.supportsRpc -> Triple(
            "RPC Ready (CLI Limited)",
            if (cliStatus.details.isNotBlank()) {
                "RPC automation is responsive. ${cliStatus.details}"
            } else {
                "RPC automation is responsive, but direct CLI commands are unavailable on this connection."
            },
            RiskMedium
        )
        cliStatus.level == CliCapabilityLevel.READY -> Triple(
            "Automation Ready",
            "Transport is responsive.",
            VesperAccent
        )
        cliStatus.level == CliCapabilityLevel.UNAVAILABLE -> Triple(
            "Automation Unavailable",
            cliStatus.details,
            RiskHigh
        )
        else -> Triple(
            "Probing Automation Channel",
            "Checking CLI/RPC transport capabilities...",
            RiskMedium
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = tint
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FilledTonalButton(
                onClick = onRunDiagnostics,
                enabled = !isRunningDiagnostics
            ) {
                if (isRunningDiagnostics) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Running link diagnostics...")
                } else {
                    Icon(Icons.Default.BugReport, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run Link Diagnostics")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider()
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Compatibility Layer",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildString {
                    append("Firmware: ")
                    append(firmwareCompatibility.label)
                    append(" | Mode: ")
                    append(
                        when (firmwareCompatibility.transportMode) {
                            FirmwareTransportMode.CLI_AND_RPC -> "CLI + RPC"
                            FirmwareTransportMode.CLI_ONLY -> "CLI-only"
                            FirmwareTransportMode.RPC_ONLY -> "RPC-only"
                            FirmwareTransportMode.PROBING -> "Probing…"
                            FirmwareTransportMode.UNAVAILABLE -> "Unavailable"
                        }
                    )
                    append(" | Confidence: ")
                    append((firmwareCompatibility.confidence * 100f).toInt())
                    append('%')
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = firmwareCompatibility.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            Divider()
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Connection Autotuner",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildString {
                    append("Profile: ")
                    append(autotuneStatus.profileLabel)
                    append(" | Success: ")
                    append((autotuneStatus.successRate * 100f).toInt())
                    append("% | Avg latency: ")
                    append(autotuneStatus.averageLatencyMs)
                    append("ms")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Retries cli=${autotuneStatus.cliRetryAttempts} rpc=${autotuneStatus.rpcRetryAttempts} write=${autotuneStatus.fileWriteRetryAttempts} | busy=${autotuneStatus.busySignals} disconnect=${autotuneStatus.disconnectSignals}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (diagnostics.checks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${diagnostics.summary} (${diagnostics.durationMs} ms)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                diagnostics.checks.forEach { check ->
                    val (icon, checkTint) = when (check.level) {
                        ConnectionCheckLevel.PASS -> Icons.Default.CheckCircle to VesperAccent
                        ConnectionCheckLevel.WARN -> Icons.Default.Warning to RiskMedium
                        ConnectionCheckLevel.FAIL -> Icons.Default.Error to RiskHigh
                        ConnectionCheckLevel.SKIPPED -> Icons.Default.RemoveCircleOutline to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = checkTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${check.name}: ${check.detail}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteControlCard(
    isSendingRemoteInput: Boolean,
    statusText: String?,
    screenFrame: FlipperScreenImage?,
    screenStatus: String?,
    liveScreen: Boolean,
    onLiveScreenChange: (Boolean) -> Unit,
    onRetryScreen: () -> Unit,
    onDisconnect: () -> Unit,
    onKeyDown: (FlipperRemoteButton) -> Long?,
    onKeyUp: (Long) -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToOracle: () -> Unit,
    onNavigateToArsenal: () -> Unit,
    onNavigateToPayloadLab: () -> Unit,
    onNavigateToStageRemote: () -> Unit,
    onNavigateToOps: () -> Unit,
    onNavigateToAudit: () -> Unit,
    deviceName: String,
    passport: PassportSnapshot,
    passportBusy: Boolean,
    passportStatus: String?,
    onApplyPassport: (DolphinMood, Int, String?, Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // The picture slot stays the same height when Live is toggled, so the
        // switch under it does not move and the page does not jump.
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            FlipperScreenMirror(
                frame = screenFrame,
                status = screenStatus,
                paused = !liveScreen,
                onRetry = onRetryScreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (liveScreen) "Live screen" else "Live screen paused",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Switch(
                checked = liveScreen,
                onCheckedChange = onLiveScreenChange
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Gamepad,
                    contentDescription = null,
                    tint = VesperOrange
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Remote Buttons",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDisconnect) {
                    Text("Disconnect")
                }
            }
            DPadRow {
                RemoteHoldButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    label = "Up",
                    button = FlipperRemoteButton.UP,
                    onKeyDown = onKeyDown,
                    onKeyUp = onKeyUp
                )
            }
            DPadRow {
                RemoteHoldButton(
                    icon = Icons.Default.KeyboardArrowLeft,
                    label = "Left",
                    button = FlipperRemoteButton.LEFT,
                    onKeyDown = onKeyDown,
                    onKeyUp = onKeyUp
                )
                Spacer(modifier = Modifier.width(10.dp))
                RemoteHoldButton(
                    icon = Icons.Default.Check,
                    label = "OK",
                    button = FlipperRemoteButton.OK,
                    onKeyDown = onKeyDown,
                    onKeyUp = onKeyUp
                )
                Spacer(modifier = Modifier.width(10.dp))
                RemoteHoldButton(
                    icon = Icons.Default.KeyboardArrowRight,
                    label = "Right",
                    button = FlipperRemoteButton.RIGHT,
                    onKeyDown = onKeyDown,
                    onKeyUp = onKeyUp
                )
            }
            DPadRow {
                RemoteHoldButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    label = "Down",
                    button = FlipperRemoteButton.DOWN,
                    onKeyDown = onKeyDown,
                    onKeyUp = onKeyUp
                )
            }

            DPadRow {
                RemoteHoldButton(
                    icon = Icons.Default.ArrowBack,
                    label = "Back",
                    button = FlipperRemoteButton.BACK,
                    wide = true,
                    onKeyDown = onKeyDown,
                    onKeyUp = onKeyUp
                )
            }

            if (isSendingRemoteInput) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = VesperOrange
                )
            }

            statusText?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        }
        FlipperKit(
            compact = true,
            onNavigateToFiles = onNavigateToFiles,
            onNavigateToOracle = onNavigateToOracle,
            onNavigateToArsenal = onNavigateToArsenal,
            onNavigateToPayloadLab = onNavigateToPayloadLab,
            onNavigateToStageRemote = onNavigateToStageRemote,
            onNavigateToOps = onNavigateToOps,
            onNavigateToAudit = onNavigateToAudit
        )
        PassportCard(
            deviceName = deviceName,
            passport = passport,
            busy = passportBusy,
            status = passportStatus,
            onApply = onApplyPassport
        )
        }
    }
}

@Composable
private fun DPadRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun RemoteHoldButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    button: FlipperRemoteButton,
    onKeyDown: (FlipperRemoteButton) -> Long?,
    onKeyUp: (Long) -> Unit,
    wide: Boolean = false
) {
    var pressed by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (pressed) VesperOrange else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .widthIn(min = if (wide) 160.dp else 86.dp)
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .heightIn(min = 48.dp)
            .pointerInput(button) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    down.consume()
                    pressed = true
                    val gestureId = onKeyDown(button)
                    try {
                        waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    } finally {
                        pressed = false
                        if (gestureId != null) onKeyUp(gestureId)
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (pressed) Color(0xFF1A1B26) else MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                label,
                color = if (pressed) Color(0xFF1A1B26) else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private data class KitTool(
    val title: String,
    val detail: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit
)

@Composable
private fun FlipperKit(
    compact: Boolean,
    scrollable: Boolean = true,
    dense: Boolean = false,
    onNavigateToFiles: () -> Unit,
    onNavigateToOracle: () -> Unit,
    onNavigateToArsenal: () -> Unit,
    onNavigateToPayloadLab: () -> Unit,
    onNavigateToStageRemote: () -> Unit,
    onNavigateToOps: () -> Unit,
    onNavigateToAudit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val defend = listOf(
        KitTool("Files", "What's stored on the Flipper", Icons.Default.Folder, RiskLow, onNavigateToFiles),
        KitTool("Audit", "What this session already did", Icons.Default.History, RiskLow, onNavigateToAudit),
        KitTool("Signals", "Radio the Flipper has saved", Icons.Default.Sensors, VesperCyan, onNavigateToArsenal),
        KitTool("Oracle", "A closer look at one signal", Icons.Default.Visibility, VesperCyan, onNavigateToOracle)
    )
    val attack = listOf(
        KitTool("Payloads", "Open the payload lab", Icons.Default.Code, VesperMagenta, onNavigateToPayloadLab),
        KitTool("Stage", "Demo files ready to send", Icons.Default.SettingsRemote, VesperMagenta, onNavigateToStageRemote),
        KitTool("Ops", "Link check and button macros", Icons.Default.BluetoothSearching, VesperAccent, onNavigateToOps)
    )
    if (compact) {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(defend + attack, key = { it.title }) { tool ->
                KitChip(tool)
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Kit",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (!dense) {
                Text(
                    text = "Tokyo Night. The tools already in this app, split into defend and attack.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            KitSection("Defend", defend, dense)
            if (dense) {
                Text(
                    text = "Attack",
                    style = MaterialTheme.typography.labelLarge,
                    color = VesperMagenta
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(attack, key = { "attack-${it.title}" }) { tool ->
                        KitChip(tool)
                    }
                }
            } else {
                KitSection("Attack", attack, dense)
            }
        }
    }
}

@Composable
private fun KitSection(title: String, tools: List<KitTool>, dense: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(if (dense) 6.dp else 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = if (title == "Attack") VesperMagenta else RiskLow
        )
        tools.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { tool ->
                    KitCard(tool, Modifier.weight(1f), dense)
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun KitCard(tool: KitTool, modifier: Modifier = Modifier, dense: Boolean = false) {
    Surface(
        onClick = tool.onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.heightIn(min = if (dense) 44.dp else 72.dp)
    ) {
        if (dense) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(tool.icon, contentDescription = null, tint = tool.tint, modifier = Modifier.size(18.dp))
                Text(tool.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        } else {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(tool.icon, contentDescription = null, tint = tool.tint)
                Text(tool.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    tool.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun KitChip(tool: KitTool) {
    Surface(
        onClick = tool.onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(tool.icon, contentDescription = null, tint = tool.tint, modifier = Modifier.size(20.dp))
            Text(tool.title, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun FlipperScreenMirror(
    frame: FlipperScreenImage?,
    status: String?,
    paused: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(frame?.id) { frame?.toLcdBitmap() }
    val aspect = if (bitmap != null && bitmap.height > 0) {
        bitmap.width.toFloat() / bitmap.height.toFloat()
    } else {
        2f
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0C1210))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Flipper screen",
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFD5E0C8)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = status ?: "Waiting for the Flipper screen",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF1C2418),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when {
                    paused && bitmap != null -> "Paused"
                    bitmap != null -> "Live Flipper screen"
                    status.isNullOrBlank() -> "Flipper screen"
                    else -> status
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (bitmap != null || status == "Live") VesperAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (bitmap == null && status?.startsWith("Screen mirror unavailable") == true) {
                TextButton(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

private fun FlipperScreenImage.toLcdBitmap(): androidx.compose.ui.graphics.ImageBitmap {
    val (destWidth, destHeight) = when (orientation) {
        FlipperScreenOrientation.VERTICAL,
        FlipperScreenOrientation.VERTICAL_FLIP -> height to width
        else -> width to height
    }
    val argb = IntArray(destWidth * destHeight)
    val on = 0xFF1C2418.toInt()
    val off = 0xFFD5E0C8.toInt()
    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = if (pixels[y * width + x]) on else off
            val (dx, dy) = when (orientation) {
                FlipperScreenOrientation.HORIZONTAL -> x to y
                FlipperScreenOrientation.HORIZONTAL_FLIP -> (width - 1 - x) to (height - 1 - y)
                FlipperScreenOrientation.VERTICAL -> (height - 1 - y) to x
                FlipperScreenOrientation.VERTICAL_FLIP -> y to (width - 1 - x)
            }
            argb[dy * destWidth + dx] = color
        }
    }
    val bitmap = Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(argb, 0, destWidth, 0, 0, destWidth, destHeight)
    return bitmap.asImageBitmap()
}

@Composable
private fun ScanSection(
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectUsb: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = if (isScanning) onStopScan else onStartScan,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isScanning) RiskHigh else VesperOrange
            )
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF1A1B26),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop Scanning")
            } else {
                Icon(Icons.Default.BluetoothSearching, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan for Flipper")
            }
        }

        OutlinedButton(
            onClick = onConnectUsb,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Usb, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Connect via USB")
        }
    }
}

@Composable
private fun DeviceListItem(
    device: FlipperDevice,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VesperOrange),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_flipper),
                    contentDescription = "Flipper",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (device.isConfirmedFlipper) "Flipper signature detected" else "Unverified Flipper",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (device.isConfirmedFlipper) VesperAccent else RiskMedium
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = VesperOrange,
                    strokeWidth = 2.dp
                )
            } else {
                // Signal strength indicator
                val signalBars = when {
                    device.rssi >= -50 -> 4
                    device.rssi >= -60 -> 3
                    device.rssi >= -70 -> 2
                    else -> 1
                }
                Icon(
                    when (signalBars) {
                        4 -> Icons.Default.SignalCellular4Bar
                        else -> Icons.Default.SignalCellular4Bar
                    },
                    contentDescription = "Signal: $signalBars bars",
                    tint = VesperAccent.copy(alpha = signalBars / 4f)
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${"%.1f".format(bytes.toFloat() / (1024 * 1024 * 1024))} GB"
    }
}

// ═══════════════════════════════════════════════════════════
// ADVANCED OPERATIONS (merged from Ops Center)
// ═══════════════════════════════════════════════════════════

private data class AdvMacroStep(val button: FlipperRemoteButton, val delayMs: Long, val longPress: Boolean)

@Composable
private fun AdvancedOperationsSection(
    viewModel: DeviceViewModel,
    connectionState: ConnectionState,
    isSendingRemoteInput: Boolean,
    remoteInputStatus: String?
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Runbook state
    var runbookStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var runbookInFlight by remember { mutableStateOf(false) }

    // Macro state
    val macroSteps = remember { mutableStateListOf<AdvMacroStep>() }
    var macroName by rememberSaveable { mutableStateOf("macro_1") }
    var isRecording by rememberSaveable { mutableStateOf(false) }
    var longPressMode by rememberSaveable { mutableStateOf(false) }
    var lastRecordedAtMs by remember { mutableLongStateOf(0L) }
    var replayStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var replayJob by remember { mutableStateOf<Job?>(null) }
    var isReplaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { replayJob?.cancel() } }

    fun sendAndRecord(button: FlipperRemoteButton) {
        if (isRecording) {
            val now = System.currentTimeMillis()
            val delta = if (lastRecordedAtMs == 0L) 0L else (now - lastRecordedAtMs).coerceAtMost(5_000L)
            macroSteps.add(AdvMacroStep(button, delta, longPressMode))
            lastRecordedAtMs = now
        }
        scope.launch { viewModel.sendRemoteButtonAwait(button, longPressMode) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Expandable Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Engineering, null, tint = VesperOrange)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Advanced Operations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Runbooks, macros & diagnostics", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Toggle")
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // ── RUNBOOKS ──
                    Text("Runbooks", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = VesperOrange)

                    val runbooks = listOf(
                        Triple("Link Health Sweep", "Refresh telemetry + full diagnostics") {
                            scope.launch {
                                runbookInFlight = true
                                runbookStatus = "Running health sweep..."
                                viewModel.refreshDeviceInfo(); delay(250); viewModel.runConnectionDiagnostics()
                                runbookStatus = "Health sweep complete."
                                runbookInFlight = false
                            }; Unit
                        },
                        Triple("Input Smoke Test", "Send Back > Up > Down > OK sequence") {
                            if (connectionState !is ConnectionState.Connected) { runbookStatus = "Connect first."; return@Triple }
                            scope.launch {
                                runbookInFlight = true; runbookStatus = "Smoke testing..."
                                listOf(FlipperRemoteButton.BACK, FlipperRemoteButton.UP, FlipperRemoteButton.DOWN, FlipperRemoteButton.OK).forEach {
                                    viewModel.sendRemoteButtonAwait(it, false); delay(220)
                                }
                                runbookStatus = "Smoke test complete."; runbookInFlight = false
                            }; Unit
                        },
                        Triple("Recover & Scan", "Disconnect and restart scanning") {
                            scope.launch {
                                runbookInFlight = true; runbookStatus = "Recovering..."
                                viewModel.disconnect(); delay(350); viewModel.startScan()
                                runbookStatus = "Recovery scan started."; runbookInFlight = false
                            }; Unit
                        }
                    )

                    runbooks.forEach { (title, desc, action) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            FilledTonalButton(onClick = { action() }, enabled = !runbookInFlight) {
                                Text(if (runbookInFlight) "..." else "Run")
                            }
                        }
                    }

                    runbookStatus?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }

                    Divider()

                    // ── MACRO RECORDER ──
                    Text("Macro Recorder", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = VesperOrange)

                    OutlinedTextField(
                        value = macroName, onValueChange = { macroName = it },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Macro Name") }
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Long Press Mode"); Switch(checked = longPressMode, onCheckedChange = { longPressMode = it })
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (isRecording) { isRecording = false; replayStatus = "Recorded ${macroSteps.size} steps." }
                            else { macroSteps.clear(); isRecording = true; lastRecordedAtMs = 0L; replayStatus = "Recording..." }
                        }) { Text(if (isRecording) "Stop" else "Record") }

                        OutlinedButton(onClick = { macroSteps.clear(); replayStatus = "Cleared." }) { Text("Clear") }

                        Button(onClick = {
                            if (isReplaying) { replayJob?.cancel(); isReplaying = false; replayStatus = "Cancelled." }
                            else if (macroSteps.isNotEmpty()) {
                                replayJob = scope.launch {
                                    isReplaying = true; replayStatus = "Replaying..."
                                    try {
                                        macroSteps.forEachIndexed { i, step ->
                                            if (!isActive) return@launch
                                            if (step.delayMs > 0L) delay(step.delayMs)
                                            replayStatus = "Step ${i + 1}/${macroSteps.size}: ${step.button.label}"
                                            val result = viewModel.sendRemoteButtonAwait(step.button, step.longPress)
                                            if (result.isFailure) {
                                                replayStatus = "Stopped at step ${i + 1}/${macroSteps.size}: " +
                                                    (result.exceptionOrNull()?.message ?: "send failed")
                                                return@launch
                                            }
                                            delay(160)
                                        }
                                        if (isActive) {
                                            replayStatus = "Done (${macroSteps.size} steps)."
                                        }
                                    } finally {
                                        isReplaying = false
                                    }
                                }
                            }
                        }) { Text(if (isReplaying) "Stop" else "Replay") }
                    }

                    Text("${macroSteps.size} steps recorded", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    replayStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }

                    // Mini D-pad for recording
                    if (isRecording) {
                        Divider()
                        Text("Tap buttons to record:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                OutlinedButton(onClick = { sendAndRecord(FlipperRemoteButton.UP) }, enabled = !isSendingRemoteInput) { Text("UP") }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                OutlinedButton(onClick = { sendAndRecord(FlipperRemoteButton.LEFT) }, enabled = !isSendingRemoteInput) { Text("LEFT") }
                                OutlinedButton(onClick = { sendAndRecord(FlipperRemoteButton.OK) }, enabled = !isSendingRemoteInput) { Text("OK") }
                                OutlinedButton(onClick = { sendAndRecord(FlipperRemoteButton.RIGHT) }, enabled = !isSendingRemoteInput) { Text("RIGHT") }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                OutlinedButton(onClick = { sendAndRecord(FlipperRemoteButton.BACK) }, enabled = !isSendingRemoteInput) { Text("BACK") }
                                OutlinedButton(onClick = { sendAndRecord(FlipperRemoteButton.DOWN) }, enabled = !isSendingRemoteInput) { Text("DOWN") }
                            }
                        }
                    }
                }
            }
        }
    }
}
