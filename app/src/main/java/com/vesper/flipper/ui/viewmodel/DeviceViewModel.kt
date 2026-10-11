package com.vesper.flipper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vesper.flipper.ble.BleServiceManager
import com.vesper.flipper.ble.CliCapabilityStatus
import com.vesper.flipper.ble.CommandPipelineAutotuneStatus
import com.vesper.flipper.ble.ConnectionCheckLevel
import com.vesper.flipper.ble.ConnectionCheckResult
import com.vesper.flipper.ble.ConnectionDiagnosticsReport
import com.vesper.flipper.ble.ConnectionState
import com.vesper.flipper.ble.FirmwareCompatibilityProfile
import com.flipperdevices.protobuf.screen.Gui
import com.vesper.flipper.ble.FlipperDevice
import com.vesper.flipper.ble.FlipperFileSystem
import com.vesper.flipper.ble.GuiRemoteInput
import com.vesper.flipper.ble.FlipperScreenImage
import com.vesper.flipper.data.SettingsStore
import com.vesper.flipper.domain.model.DeviceInfo
import com.vesper.flipper.domain.model.FlipperRemoteButton
import com.vesper.flipper.domain.model.StorageInfo
import com.vesper.flipper.glasses.BridgeState
import com.vesper.flipper.glasses.GlassesIntegration
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val fileSystem: FlipperFileSystem,
    private val bleServiceManager: BleServiceManager,
    private val glassesIntegration: GlassesIntegration
) : ViewModel() {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<FlipperDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<FlipperDevice>> = _discoveredDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<FlipperDevice?>(null)
    val connectedDevice: StateFlow<FlipperDevice?> = _connectedDevice.asStateFlow()

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    private val _storageInfo = MutableStateFlow<StorageInfo?>(null)
    val storageInfo: StateFlow<StorageInfo?> = _storageInfo.asStateFlow()
    private val _cliCapabilityStatus = MutableStateFlow(CliCapabilityStatus())
    val cliCapabilityStatus: StateFlow<CliCapabilityStatus> = _cliCapabilityStatus.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    private val _connectionDiagnostics = MutableStateFlow(ConnectionDiagnosticsReport.idle())
    val connectionDiagnostics: StateFlow<ConnectionDiagnosticsReport> = _connectionDiagnostics.asStateFlow()
    private val _isRunningDiagnostics = MutableStateFlow(false)
    val isRunningDiagnostics: StateFlow<Boolean> = _isRunningDiagnostics.asStateFlow()
    private val _autotuneStatus = MutableStateFlow(CommandPipelineAutotuneStatus())
    val autotuneStatus: StateFlow<CommandPipelineAutotuneStatus> = _autotuneStatus.asStateFlow()
    val firmwareCompatibility: StateFlow<FirmwareCompatibilityProfile> = fileSystem.firmwareCompatibility
    private val _isSendingRemoteInput = MutableStateFlow(false)
    val isSendingRemoteInput: StateFlow<Boolean> = _isSendingRemoteInput.asStateFlow()
    private val _remoteInputStatus = MutableStateFlow<String?>(null)
    val remoteInputStatus: StateFlow<String?> = _remoteInputStatus.asStateFlow()
    val screenFrame: StateFlow<FlipperScreenImage?> = fileSystem.screenFrame
    private val _screenStreamStatus = MutableStateFlow<String?>(null)
    val screenStreamStatus: StateFlow<String?> = _screenStreamStatus.asStateFlow()
    // Glasses bridge state
    val glassesBridgeState: StateFlow<BridgeState> = glassesIntegration.bridgeState

    private val _glassesEnabled = MutableStateFlow(false)
    val glassesEnabled: StateFlow<Boolean> = _glassesEnabled.asStateFlow()

    private val _glassesBridgeUrl = MutableStateFlow("")
    val glassesBridgeUrl: StateFlow<String> = _glassesBridgeUrl.asStateFlow()

    private val refreshMutex = Mutex()
    private val remoteInputMutex = Mutex()
    private var remoteGesture: RemoteGesture? = null
    private val extraBackTaps = AtomicInteger(0)
    private var screenMirrorJob: Job? = null
    private var screenMirrorGeneration = 0

    init {
        bleServiceManager.bindService()
        observeServiceState()
        viewModelScope.launch {
            fileSystem.autotuneStatus.collect { status ->
                _autotuneStatus.value = status
            }
        }
        // Observe glasses settings
        viewModelScope.launch {
            settingsStore.glassesEnabled.collect { enabled ->
                _glassesEnabled.value = enabled
            }
        }
        viewModelScope.launch {
            settingsStore.glassesBridgeUrl.collect { url ->
                _glassesBridgeUrl.value = url ?: ""
            }
        }
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            bleServiceManager.connectionState.collect { state ->
                _connectionState.value = state
                if (state is ConnectionState.Connected) {
                    refreshDeviceInfo()
                } else {
                    clearConnectionTransientState()
                }
            }
        }

        viewModelScope.launch {
            bleServiceManager.discoveredDevices.collect { devices ->
                _discoveredDevices.value = devices
            }
        }

        viewModelScope.launch {
            bleServiceManager.connectedDevice.collect { device ->
                _connectedDevice.value = device
                device?.let {
                    viewModelScope.launch {
                        settingsStore.setLastDevice(it.address, it.name)
                    }
                }
            }
        }

        viewModelScope.launch {
            bleServiceManager.cliCapabilityStatus.collect { status ->
                _cliCapabilityStatus.value = status
            }
        }
    }

    private fun clearConnectionTransientState() {
        _isRunningDiagnostics.value = false
        _isSendingRemoteInput.value = false
        _remoteInputStatus.value = null
        _screenStreamStatus.value = null
        screenMirrorGeneration++
        screenMirrorJob?.cancel()
        screenMirrorJob = null
        remoteGesture?.release?.complete(Unit)
        remoteGesture = null
        extraBackTaps.set(0)
        _connectionDiagnostics.value = ConnectionDiagnosticsReport.idle()
    }

    fun startScan() {
        bleServiceManager.startScanning()
    }

    fun stopScan() {
        bleServiceManager.stopScanning()
    }

    fun connectToDevice(device: FlipperDevice) {
        bleServiceManager.connect(device)
    }

    fun connectUsb() {
        bleServiceManager.connectUsb()
    }

    fun disconnect() {
        bleServiceManager.disconnect()
        _deviceInfo.value = null
        _storageInfo.value = null
        _cliCapabilityStatus.value = CliCapabilityStatus()
        _connectionDiagnostics.value = ConnectionDiagnosticsReport.idle()
        _isRunningDiagnostics.value = false
        _isSendingRemoteInput.value = false
        _remoteInputStatus.value = null
        _screenStreamStatus.value = null
        screenMirrorGeneration++
        screenMirrorJob?.cancel()
        screenMirrorJob = null
    }

    fun refreshDeviceInfo() {
        if (_connectionState.value !is ConnectionState.Connected) return
        if (_isRefreshing.value) return

        viewModelScope.launch {
            refreshMutex.withLock {
                _isRefreshing.value = true
                try {
                    // Cap the total time spent refreshing so a non-responsive Flipper
                    // does not monopolise the command pipeline for 30+ seconds
                    // (each RPC timeout + CLI fallback chain adds up).
                    withTimeoutOrNull(REFRESH_DEVICE_INFO_TIMEOUT_MS) {
                        fileSystem.getDeviceInfo().onSuccess { info ->
                            _deviceInfo.value = info
                        }
                        fileSystem.getStorageInfo().onSuccess { info ->
                            _storageInfo.value = info
                        }
                    }
                } finally {
                    _isRefreshing.value = false
                }
            }
        }
    }

    companion object {
        /** Total wall-clock budget for the on-connect device info refresh. */
        private const val REFRESH_DEVICE_INFO_TIMEOUT_MS = 6_000L
        /** Keep the remote pad from sitting on "Sending…" if the link never answers. */
        private const val REMOTE_INPUT_TIMEOUT_MS = 12_000L
        /** Matches Flipper's long-press timer: 2 × 150ms. */
        private const val REMOTE_LONG_PRESS_MS = 300L
        private const val REMOTE_REPEAT_MS = 150L
    }

    /**
     * @return a gesture id the pointer must pass back on release, or null when
     * the press was ignored because another key is still in flight.
     */
    fun remoteKeyDown(button: FlipperRemoteButton): Long? {
        if (_connectionState.value !is ConnectionState.Connected) {
            _remoteInputStatus.value = "Connect a Flipper first."
            return null
        }
        val active = remoteGesture
        if (active?.job?.isActive == true) {
            if (button == FlipperRemoteButton.BACK && active.button == FlipperRemoteButton.BACK) {
                // The lock screen needs three shorts inside 600ms. Dropping the
                // second tap, or waiting out a new probe, makes that impossible.
                extraBackTaps.updateAndGet { (it + 1).coerceAtMost(GuiRemoteInput.LOCK_SCREEN_UNLOCK_TAPS - 1) }
                return null
            }
            _remoteInputStatus.value = "Still sending ${active.button.label}…"
            return null
        }
        extraBackTaps.set(0)

        val release = CompletableDeferred<Unit>()
        val id = System.nanoTime()
        val job = viewModelScope.launch {
            remoteInputMutex.withLock {
                runRemoteGesture(button, release)
            }
        }
        remoteGesture = RemoteGesture(id, button, release, job)
        return id
    }

    fun remoteKeyUp(gestureId: Long) {
        val gesture = remoteGesture ?: return
        if (gesture.id != gestureId) return
        gesture.release.complete(Unit)
    }

    fun startScreenMirror() {
        if (_connectionState.value !is ConnectionState.Connected) return
        val generation = ++screenMirrorGeneration
        screenMirrorJob?.cancel()
        screenMirrorJob = viewModelScope.launch {
            if (generation != screenMirrorGeneration) return@launch
            _screenStreamStatus.value = "Opening the Flipper screen…"
            val frame = openScreenMirror(generation)
            if (generation != screenMirrorGeneration) return@launch
            _screenStreamStatus.value = if (frame == null) {
                "Screen mirror unavailable: the Flipper did not send a frame."
            } else {
                "Live"
            }
        }
    }

    private suspend fun openScreenMirror(generation: Int): FlipperScreenImage? {
        repeat(2) { attempt ->
            if (generation != screenMirrorGeneration) return null
            if (attempt > 0) {
                fileSystem.stopScreenStream()
                if (generation != screenMirrorGeneration) return null
            }
            val beforeId = fileSystem.screenFrame.value?.id
            val started = fileSystem.startScreenStream()
            if (generation != screenMirrorGeneration) return null
            if (started.isFailure) {
                if (attempt == 1) return null
                return@repeat
            }
            val frame = withTimeoutOrNull(2_500L) {
                fileSystem.screenFrame.first { it != null && it.id != beforeId }
            }
            if (frame != null) return frame
        }
        return null
    }

    fun stopScreenMirror() {
        val generation = ++screenMirrorGeneration
        screenMirrorJob?.cancel()
        screenMirrorJob = null
        viewModelScope.launch {
            if (generation != screenMirrorGeneration) return@launch
            if (_connectionState.value is ConnectionState.Connected) {
                fileSystem.stopScreenStream()
            }
            if (generation == screenMirrorGeneration) {
                _screenStreamStatus.value = null
            }
        }
    }

    private suspend fun runRemoteGesture(
        button: FlipperRemoteButton,
        release: CompletableDeferred<Unit>
    ) {
        _isSendingRemoteInput.value = true
        try {
            _remoteInputStatus.value = "Sending ${button.label}…"
            if (fileSystem.wakeDesktopBeforeRemoteKey(button)) {
                extraBackTaps.set(0)
                _remoteInputStatus.value = "Desktop unlocked. Back would lock it again, so this press only woke the Flipper."
                return
            }
            var backShortsSent = 0
            val result = fileSystem.playRemoteGesture(button) { send ->
                if (!send(Gui.InputType.PRESS)) return@playRemoteGesture
                // Hold time starts once the press is on the wire, not during the lock wait.
                val releasedQuickly = release.isCompleted ||
                    withTimeoutOrNull(REMOTE_LONG_PRESS_MS) { release.await() } != null
                if (releasedQuickly) {
                    if (button == FlipperRemoteButton.BACK) {
                        backShortsSent = sendBackBurst(send)
                    } else {
                        send(Gui.InputType.SHORT)
                    }
                } else if (send(Gui.InputType.LONG)) {
                    extraBackTaps.set(0)
                    _remoteInputStatus.value = "${button.label} held."
                    while (!release.isCompleted) {
                        send(Gui.InputType.REPEAT)
                        withTimeoutOrNull(REMOTE_REPEAT_MS) { release.await() }
                    }
                }
            }
            if (result.isSuccess &&
                button == FlipperRemoteButton.BACK &&
                backShortsSent in 1 until GuiRemoteInput.LOCK_SCREEN_UNLOCK_TAPS
            ) {
                // A tap can arrive while the release reply is still in flight.
                // Send it before this gesture ends. Stop at three so the short
                // that unlocks the lock screen is not followed by one that locks
                // the desktop.
                while (backShortsSent < GuiRemoteInput.LOCK_SCREEN_UNLOCK_TAPS && takeQueuedBackTap()) {
                    val followUp = fileSystem.playRemoteGesture(button) { send ->
                        if (!send(Gui.InputType.PRESS)) return@playRemoteGesture
                        send(Gui.InputType.SHORT)
                    }
                    backShortsSent++
                    if (followUp.isFailure) break
                }
            }
            _remoteInputStatus.value = result.fold(
                onSuccess = { it },
                onFailure = { error ->
                    val message = error.message ?: "no response"
                    if (message.startsWith("GUI input written")) {
                        message
                    } else {
                        "Remote input failed: $message"
                    }
                }
            )
        } finally {
            extraBackTaps.set(0)
            _isSendingRemoteInput.value = false
        }
    }

    /**
     * The first press is already on the wire. Each short is released before
     * this waits for another tap. Holding Back while waiting is the desktop's
     * power-off gesture. One tap still sends one short. Extra taps stay inside
     * the lock screen's 600ms window, and a fourth short is not sent.
     */
    private suspend fun sendBackBurst(send: suspend (Gui.InputType) -> Boolean): Int {
        var sent = 0
        var firstShortAtMs = 0L
        while (sent < GuiRemoteInput.LOCK_SCREEN_UNLOCK_TAPS) {
            if (sent > 0 && !send(Gui.InputType.PRESS)) return sent
            if (!send(Gui.InputType.SHORT)) return sent
            if (sent == 0) firstShortAtMs = System.currentTimeMillis()
            sent++
            if (!send(Gui.InputType.RELEASE)) return sent
            if (!takeQueuedBackTap()) {
                val waitMs = GuiRemoteInput.backBurstWaitMs(
                    sentShorts = sent,
                    elapsedSinceFirstShortMs = System.currentTimeMillis() - firstShortAtMs
                )
                if (waitMs == 0L) return sent
                val arrived = withTimeoutOrNull(waitMs) {
                    while (!takeQueuedBackTap()) delay(20)
                    true
                }
                if (arrived != true) return sent
            }
            delay(30)
        }
        return sent
    }

    private fun takeQueuedBackTap(): Boolean {
        while (true) {
            val queued = extraBackTaps.get()
            if (queued == 0) return false
            if (extraBackTaps.compareAndSet(queued, queued - 1)) return true
        }
    }

    private data class RemoteGesture(
        val id: Long,
        val button: FlipperRemoteButton,
        val release: CompletableDeferred<Unit>,
        val job: Job
    )

    fun runConnectionDiagnostics() {
        if (_isRunningDiagnostics.value) return

        viewModelScope.launch {
            _isRunningDiagnostics.value = true
            try {
                val report = bleServiceManager.awaitService()?.runConnectionDiagnostics() ?: run {
                    val now = System.currentTimeMillis()
                    ConnectionDiagnosticsReport(
                        startedAtMs = now,
                        completedAtMs = now,
                        checks = listOf(
                            ConnectionCheckResult(
                                name = "Device Service",
                                level = ConnectionCheckLevel.FAIL,
                                detail = "BLE service is unavailable."
                            )
                        )
                    )
                }
                _connectionDiagnostics.value = report
                _cliCapabilityStatus.value = bleServiceManager.cliCapabilityStatus.value
            } finally {
                _isRunningDiagnostics.value = false
            }
        }
    }

    fun sendRemoteButton(button: FlipperRemoteButton, longPress: Boolean = false) {
        if (_connectionState.value !is ConnectionState.Connected) {
            _remoteInputStatus.value = "Connect a Flipper first."
            return
        }

        viewModelScope.launch {
            if (!remoteInputMutex.tryLock()) {
                _remoteInputStatus.value = "Previous button input is still in flight..."
                return@launch
            }
            try {
                sendRemoteButtonLocked(button, longPress)
            } finally {
                remoteInputMutex.unlock()
            }
        }
    }

    suspend fun sendRemoteButtonAwait(button: FlipperRemoteButton, longPress: Boolean = false): Result<String> {
        if (_connectionState.value !is ConnectionState.Connected) {
            val error = IllegalStateException("Connect a Flipper first.")
            _remoteInputStatus.value = error.message
            return Result.failure(error)
        }

        return remoteInputMutex.withLock {
            sendRemoteButtonLocked(button, longPress)
        }
    }

    private suspend fun sendRemoteButtonLocked(
        button: FlipperRemoteButton,
        longPress: Boolean
    ): Result<String> {
        _isSendingRemoteInput.value = true
        val pressLabel = if (longPress) "hold ${button.label}" else button.label
        _remoteInputStatus.value = "Sending $pressLabel..."
        return try {
            val result = withTimeoutOrNull(REMOTE_INPUT_TIMEOUT_MS) {
                fileSystem.sendRemoteButton(button, longPress)
            } ?: Result.failure(
                IllegalStateException(
                    "Remote input timed out. Bluetooth did not answer. Reconnect the Flipper and try again."
                )
            )
            result.also {
                _remoteInputStatus.value = result.fold(
                    onSuccess = { details ->
                        details.ifBlank { "Sent ${button.label}." }
                    },
                    onFailure = {
                        "Remote input failed: ${it.message ?: "Unknown error"}"
                    }
                )
            }
        } finally {
            _isSendingRemoteInput.value = false
        }
    }

    // ==================== Glasses Bridge ====================

    fun connectGlassesBridge() {
        val url = _glassesBridgeUrl.value
        if (url.isBlank()) return
        glassesIntegration.connect(url)
    }

    fun disconnectGlassesBridge() {
        glassesIntegration.disconnect()
    }

}
