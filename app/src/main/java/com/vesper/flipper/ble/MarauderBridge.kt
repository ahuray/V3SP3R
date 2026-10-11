package com.vesper.flipper.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * ESP32 Marauder Bridge
 *
 * Connects to an ESP32 running Marauder firmware via BLE or serial.
 * Provides WiFi attack capabilities:
 * - WiFi scanning and SSID enumeration
 * - Deauthentication attacks
 * - Beacon spam
 * - Probe requests
 * - Evil Portal hosting
 * - Packet sniffing
 */
class MarauderBridge(
    private val context: Context
) {
    companion object {
        // ESP32 Marauder BLE service UUIDs
        val MARAUDER_SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val MARAUDER_TX_CHAR_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
        val MARAUDER_RX_CHAR_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9")

        // Bounded recovery for GATT writes that fail to start or lose their callback.
        private const val MAX_WRITE_ATTEMPTS = 3
        private const val WRITE_RETRY_DELAY_MS = 60L
        private const val WRITE_TIMEOUT_MS = 3_000L
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow(MarauderConnectionState.DISCONNECTED)
    val connectionState: StateFlow<MarauderConnectionState> = _connectionState.asStateFlow()

    private val _responses = Channel<String>(Channel.BUFFERED)
    val responses: Flow<String> = _responses.receiveAsFlow()

    private val _scanResults = MutableStateFlow<List<WiFiNetwork>>(emptyList())
    val scanResults: StateFlow<List<WiFiNetwork>> = _scanResults.asStateFlow()

    private val _attackStatus = MutableStateFlow<AttackStatus>(AttackStatus.Idle)
    val attackStatus: StateFlow<AttackStatus> = _attackStatus.asStateFlow()

    private val responseBuffer = StringBuilder()

    // Serializes GATT writes: Android allows only one outstanding
    // writeCharacteristic at a time, so queued commands wait for the previous
    // onCharacteristicWrite before the next is issued.
    private val writeLock = Any()
    private val writeQueue = ArrayDeque<ByteArray>()
    private var writeInProgress = false
    private var writeAttempts = 0

    // Schedules bounded retries and a watchdog timeout for GATT writes so a stack
    // that returns false or never fires onCharacteristicWrite cannot stall the queue.
    private val writeScheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "marauder-write").apply { isDaemon = true }
    }

    @Volatile
    private var writeTimeoutTask: ScheduledFuture<*>? = null

    // ==================== Connection ====================

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        _connectionState.value = MarauderConnectionState.CONNECTING
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        clearTransportState()
        _connectionState.value = MarauderConnectionState.DISCONNECTED
    }

    /**
     * Permanently dispose this bridge. Unlike [disconnect], which stays reusable
     * for reconnects, this also stops the write scheduler thread. Call from the
     * owner's teardown (e.g. ViewModel.onCleared / service onDestroy).
     */
    fun release() {
        disconnect()
        writeScheduler.shutdownNow()
    }

    private fun clearTransportState() {
        txCharacteristic = null
        rxCharacteristic = null
        cancelWriteTimeout()
        synchronized(writeLock) {
            writeQueue.clear()
            writeInProgress = false
            writeAttempts = 0
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = MarauderConnectionState.DISCOVERING
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    // Remote drop: release this GATT so a later connect() does not
                    // leak it, and clear transport/queue state.
                    gatt.close()
                    if (bluetoothGatt === gatt) {
                        bluetoothGatt = null
                        clearTransportState()
                    }
                    _connectionState.value = MarauderConnectionState.DISCONNECTED
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (characteristic.uuid != MARAUDER_TX_CHAR_UUID) return
            // Ignore completions from a previous connection's GATT: after a
            // disconnect/reconnect a late callback must not touch the new queue.
            if (bluetoothGatt !== gatt) return

            if (status != BluetoothGatt.GATT_SUCCESS) {
                // The write completed with an error, so nothing is in flight and a
                // bounded retry is safe (no overlap risk).
                onWriteStartFailed(gatt, "status $status")
                return
            }

            cancelWriteTimeout()
            val advanced: Boolean
            synchronized(writeLock) {
                // A write already timed out and the gate was released; this is a
                // stale/duplicate completion, so do not pop a second command.
                if (!writeInProgress) {
                    advanced = false
                } else {
                    if (writeQueue.isNotEmpty()) writeQueue.removeFirst()
                    writeInProgress = false
                    writeAttempts = 0
                    advanced = true
                }
            }
            if (advanced) pumpWriteQueue()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(MARAUDER_SERVICE_UUID)
                if (service != null) {
                    txCharacteristic = service.getCharacteristic(MARAUDER_TX_CHAR_UUID)
                    rxCharacteristic = service.getCharacteristic(MARAUDER_RX_CHAR_UUID)

                    // Enable notifications on RX characteristic
                    rxCharacteristic?.let { char ->
                        gatt.setCharacteristicNotification(char, true)
                        char.descriptors.forEach { desc ->
                            desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(desc)
                        }
                    }

                    _connectionState.value = MarauderConnectionState.CONNECTED
                } else {
                    _connectionState.value = MarauderConnectionState.ERROR
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == MARAUDER_RX_CHAR_UUID) {
                val response = String(value)
                processResponse(response)
            }
        }
    }

    private fun processResponse(response: String) {
        responseBuffer.append(response)

        // Check for complete lines
        while (responseBuffer.contains("\n")) {
            val lineEnd = responseBuffer.indexOf("\n")
            val line = responseBuffer.substring(0, lineEnd).trim()
            responseBuffer.delete(0, lineEnd + 1)

            if (line.isNotEmpty()) {
                _responses.trySend(line)
                parseMarauderOutput(line)
            }
        }
    }

    private fun parseMarauderOutput(line: String) {
        // Parse scan results
        if (line.startsWith("SSID:") || line.contains("Channel:")) {
            parseWiFiNetwork(line)?.let { network ->
                _scanResults.value = _scanResults.value + network
            }
        }

        // Parse attack status
        when {
            line.contains("Attack started") -> _attackStatus.value = AttackStatus.Running(line)
            line.contains("Attack stopped") -> _attackStatus.value = AttackStatus.Stopped
            line.contains("Error") -> _attackStatus.value = AttackStatus.Error(line)
        }
    }

    private fun parseWiFiNetwork(line: String): WiFiNetwork? {
        val ssidMatch = Regex("SSID:\\s*(.+?)(?:\\s+|$)").find(line)
        val bssidMatch = Regex("BSSID:\\s*([A-Fa-f0-9:]+)").find(line)
        val channelMatch = Regex("Channel:\\s*(\\d+)").find(line)
        val rssiMatch = Regex("RSSI:\\s*(-?\\d+)").find(line)
        val encMatch = Regex("Enc:\\s*(\\w+)").find(line)

        val ssid = ssidMatch?.groupValues?.get(1) ?: return null

        return WiFiNetwork(
            ssid = ssid,
            bssid = bssidMatch?.groupValues?.get(1) ?: "",
            channel = channelMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            rssi = rssiMatch?.groupValues?.get(1)?.toIntOrNull() ?: -100,
            encryption = encMatch?.groupValues?.get(1) ?: "Unknown"
        )
    }

    // ==================== Commands ====================

    private fun sendCommand(command: String) {
        val payload = "$command\n".toByteArray()
        synchronized(writeLock) {
            writeQueue.addLast(payload)
        }
        pumpWriteQueue()
    }

    @SuppressLint("MissingPermission")
    private fun pumpWriteQueue() {
        val gatt = bluetoothGatt ?: return
        val char = txCharacteristic ?: return
        val next: ByteArray
        synchronized(writeLock) {
            if (writeInProgress) return
            next = writeQueue.firstOrNull() ?: return
            writeInProgress = true
            writeAttempts++
        }
        char.value = next
        val started = gatt.writeCharacteristic(char)
        if (started) {
            // Guard against a lost onCharacteristicWrite: if no completion arrives
            // within the timeout, treat the link as dead and reset.
            scheduleWriteTimeout(gatt)
        } else {
            onWriteStartFailed(gatt, "write did not start")
        }
    }

    /**
     * The write never left the stack (or completed with an error), so nothing is
     * in flight. Retry the same command up to [MAX_WRITE_ATTEMPTS], then drop it
     * so the queue keeps progressing and report the failure once. Because no write
     * is outstanding, a retry cannot overlap another write.
     */
    private fun onWriteStartFailed(gatt: BluetoothGatt, reason: String) {
        if (bluetoothGatt !== gatt) return
        cancelWriteTimeout()
        val giveUp: Boolean
        synchronized(writeLock) {
            // Already handled (e.g. a real completion raced this path).
            if (!writeInProgress) return
            writeInProgress = false
            if (writeAttempts >= MAX_WRITE_ATTEMPTS) {
                if (writeQueue.isNotEmpty()) writeQueue.removeFirst()
                writeAttempts = 0
                giveUp = true
            } else {
                giveUp = false
            }
        }
        if (giveUp) {
            _attackStatus.value = AttackStatus.Error("Marauder command dropped after retries: $reason")
        }
        schedulePump()
    }

    /**
     * A started write never reported completion. The link is almost certainly
     * dead, and re-issuing would risk overlapping an in-flight write — a late
     * completion could then be matched to the wrong queue entry and drop a
     * command. Tear the connection down instead of retrying on the same GATT.
     */
    @SuppressLint("MissingPermission")
    private fun onWriteTimedOut(gatt: BluetoothGatt) {
        if (bluetoothGatt !== gatt) return
        val outstanding = synchronized(writeLock) { writeInProgress }
        if (!outstanding) return
        _attackStatus.value = AttackStatus.Error("Marauder write timed out — connection reset")
        disconnect()
    }

    private fun scheduleWriteTimeout(gatt: BluetoothGatt) {
        cancelWriteTimeout()
        writeTimeoutTask = runCatching {
            writeScheduler.schedule(
                { onWriteTimedOut(gatt) },
                WRITE_TIMEOUT_MS,
                TimeUnit.MILLISECONDS
            )
        }.getOrNull()
    }

    private fun cancelWriteTimeout() {
        writeTimeoutTask?.cancel(false)
        writeTimeoutTask = null
    }

    private fun schedulePump() {
        runCatching {
            writeScheduler.schedule({ pumpWriteQueue() }, WRITE_RETRY_DELAY_MS, TimeUnit.MILLISECONDS)
        }
    }

    // WiFi Scanning
    fun scanWiFi() {
        _scanResults.value = emptyList()
        sendCommand("scanap")
    }

    fun stopScan() {
        sendCommand("stopscan")
    }

    // Deauth Attack
    fun deauthAttack(targetBssid: String? = null) {
        _attackStatus.value = AttackStatus.Starting
        if (targetBssid != null) {
            sendCommand("attack -t deauth -c $targetBssid")
        } else {
            sendCommand("attack -t deauth")
        }
    }

    // Beacon Spam
    fun beaconSpam(ssids: List<String>) {
        _attackStatus.value = AttackStatus.Starting
        // Set SSIDs first
        ssids.forEachIndexed { index, ssid ->
            sendCommand("ssid -a -n \"$ssid\"")
        }
        sendCommand("attack -t beacon -l")
    }

    fun randomBeaconSpam(count: Int = 50) {
        _attackStatus.value = AttackStatus.Starting
        sendCommand("attack -t beacon -r -n $count")
    }

    // Rickroll Beacon
    fun rickrollBeacon() {
        _attackStatus.value = AttackStatus.Starting
        sendCommand("attack -t rickroll")
    }

    // Probe Request Flood
    fun probeFlood() {
        _attackStatus.value = AttackStatus.Starting
        sendCommand("attack -t probe")
    }

    // Evil Portal
    fun startEvilPortal(ssid: String) {
        sendCommand("evilportal -c setap -s \"$ssid\"")
        sendCommand("evilportal -c start")
    }

    fun stopEvilPortal() {
        sendCommand("evilportal -c stop")
    }

    // Packet Sniffer
    fun startSniffer(channel: Int = 1) {
        sendCommand("sniff -c $channel")
    }

    fun stopSniffer() {
        sendCommand("sniff -s")
    }

    // Stop all attacks
    fun stopAttack() {
        sendCommand("stopscan")
        _attackStatus.value = AttackStatus.Stopped
    }

    // Raw command
    fun sendRawCommand(command: String) {
        sendCommand(command)
    }

    // Get Marauder info
    fun getInfo() {
        sendCommand("info")
    }

    fun getHelp() {
        sendCommand("help")
    }
}

// ==================== Data Classes ====================

enum class MarauderConnectionState {
    DISCONNECTED,
    CONNECTING,
    DISCOVERING,
    CONNECTED,
    ERROR
}

data class WiFiNetwork(
    val ssid: String,
    val bssid: String,
    val channel: Int,
    val rssi: Int,
    val encryption: String,
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis()
)

sealed class AttackStatus {
    object Idle : AttackStatus()
    object Starting : AttackStatus()
    data class Running(val details: String) : AttackStatus()
    object Stopped : AttackStatus()
    data class Error(val message: String) : AttackStatus()
}

// ==================== Marauder Commands Reference ====================

/**
 * Marauder Command Reference
 *
 * Scanning:
 * - scanap: Scan for access points
 * - scansta: Scan for stations
 * - stopscan: Stop scanning
 *
 * Attacks:
 * - attack -t deauth: Deauthentication attack
 * - attack -t beacon: Beacon spam
 * - attack -t probe: Probe request flood
 * - attack -t rickroll: Rickroll beacon spam
 *
 * SSID Management:
 * - ssid -a -n "name": Add SSID
 * - ssid -r -i index: Remove SSID
 * - ssid -c: Clear SSIDs
 *
 * Evil Portal:
 * - evilportal -c start: Start portal
 * - evilportal -c stop: Stop portal
 * - evilportal -c setap -s "ssid": Set AP name
 *
 * Sniffing:
 * - sniff -c channel: Start sniffing
 * - sniff -s: Stop sniffing
 *
 * General:
 * - info: Device info
 * - help: Command help
 * - reboot: Reboot device
 */
object MarauderCommands {
    val SCAN_AP = "scanap"
    val SCAN_STATIONS = "scansta"
    val STOP_SCAN = "stopscan"

    val DEAUTH = "attack -t deauth"
    val BEACON_SPAM = "attack -t beacon"
    val PROBE_FLOOD = "attack -t probe"
    val RICKROLL = "attack -t rickroll"

    val INFO = "info"
    val HELP = "help"
    val REBOOT = "reboot"
}
