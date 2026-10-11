package com.vesper.flipper.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vesper.flipper.domain.executor.CommandExecutor
import com.vesper.flipper.domain.model.CommandResult
import com.vesper.flipper.domain.model.ExecuteCommand
import com.vesper.flipper.domain.model.PendingApproval
import com.vesper.flipper.domain.model.StageBeatAction
import com.vesper.flipper.domain.model.StageFeedback
import com.vesper.flipper.domain.model.StagePayload
import com.vesper.flipper.domain.model.StagePhase
import com.vesper.flipper.domain.model.StageRemoteCatalog
import com.vesper.flipper.domain.model.StageTier
import com.vesper.flipper.domain.service.AuditService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

data class StagePayloadRow(
    val payload: StagePayload,
    val onDevice: Boolean = false,
    val feedback: StageFeedback? = null
)

data class StageFileChoice(
    val beatId: String,
    val directory: String,
    val files: List<String>
)

sealed class StageConfirmRequest {
    data class Beat(val beatId: String, val selectedPath: String?) : StageConfirmRequest()
    data class Payloads(val assetPaths: List<String>) : StageConfirmRequest()
}

/**
 * Stage Remote. Beats and file pushes go through [CommandExecutor], so RiskAssessor
 * still gates them. LIVE beats and LIVE payload pushes also confirm in this screen
 * first — that dialog is not skipped by auto-approve settings.
 */
@HiltViewModel
class StageRemoteViewModel @Inject constructor(
    private val commandExecutor: CommandExecutor,
    private val auditService: AuditService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val gate = Mutex()
    private var approvalWaiter: CompletableDeferred<CommandResult>? = null

    private val _beatFeedback = MutableStateFlow<Map<String, StageFeedback>>(emptyMap())
    val beatFeedback: StateFlow<Map<String, StageFeedback>> = _beatFeedback.asStateFlow()

    private val _payloads = MutableStateFlow<List<StagePayloadRow>>(emptyList())
    val payloads: StateFlow<List<StagePayloadRow>> = _payloads.asStateFlow()

    private val _assetsScanned = MutableStateFlow(false)
    val assetsScanned: StateFlow<Boolean> = _assetsScanned.asStateFlow()

    private val _filePicker = MutableStateFlow<StageFileChoice?>(null)
    val filePicker: StateFlow<StageFileChoice?> = _filePicker.asStateFlow()

    private val _confirmRequest = MutableStateFlow<StageConfirmRequest?>(null)
    val confirmRequest: StateFlow<StageConfirmRequest?> = _confirmRequest.asStateFlow()

    private val _pendingApproval = MutableStateFlow<PendingApproval?>(null)
    val pendingApproval: StateFlow<PendingApproval?> = _pendingApproval.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _banner = MutableStateFlow<String?>(null)
    val banner: StateFlow<String?> = _banner.asStateFlow()

    init {
        loadBundledPayloads()
    }

    fun clearBanner() {
        _banner.value = null
    }

    fun refresh() {
        if (blocked()) return
        viewModelScope.launch {
            gate.withLock {
                _busy.value = true
                try {
                    refreshDevicePresence()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _banner.value = e.message ?: "Could not reach the Flipper"
                } finally {
                    _busy.value = false
                }
            }
        }
    }

    fun onBeatClicked(beatId: String) {
        if (blocked()) return
        val beat = StageRemoteCatalog.beatById(beatId) ?: return
        when (val spec = beat.action) {
            is StageBeatAction.IrTransmit -> openFilePicker(beat.id, spec.directory, ".ir")
            is StageBeatAction.SubGhzTransmit -> openFilePicker(beat.id, spec.directory, ".sub")
            else -> armOrRunBeat(beat.id, selectedPath = null)
        }
    }

    fun onFilePicked(path: String) {
        val picker = _filePicker.value ?: return
        _filePicker.value = null
        armOrRunBeat(picker.beatId, path)
    }

    fun dismissFilePicker() {
        _filePicker.value = null
    }

    fun confirmPending() {
        val request = _confirmRequest.value ?: return
        _confirmRequest.value = null
        when (request) {
            is StageConfirmRequest.Beat -> launchBeat(request.beatId, request.selectedPath)
            is StageConfirmRequest.Payloads -> launchPush(request.assetPaths)
        }
    }

    fun dismissConfirm() {
        _confirmRequest.value = null
    }

    fun pushPayload(assetPath: String) {
        if (blocked()) return
        val row = _payloads.value.find { it.payload.assetPath == assetPath } ?: return
        if (row.payload.requiresLiveConfirmation()) {
            _confirmRequest.value = StageConfirmRequest.Payloads(listOf(assetPath))
        } else {
            launchPush(listOf(assetPath))
        }
    }

    fun pushTier(tier: StageTier) {
        if (blocked()) return
        val paths = _payloads.value.filter { it.payload.tier == tier }.map { it.payload.assetPath }
        if (paths.isEmpty()) return
        if (tier == StageTier.LIVE) {
            _confirmRequest.value = StageConfirmRequest.Payloads(paths)
        } else {
            launchPush(paths)
        }
    }

    fun approveExecutor() {
        val approval = _pendingApproval.value ?: return
        val waiter = approvalWaiter ?: return
        viewModelScope.launch {
            val result = try {
                commandExecutor.approve(approval.id, sessionId())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CommandResult(
                    success = false,
                    action = approval.command.action,
                    error = e.message ?: "Approval failed"
                )
            }
            if (!waiter.isCompleted) waiter.complete(result)
        }
    }

    fun rejectExecutor() {
        val approval = _pendingApproval.value ?: return
        val waiter = approvalWaiter ?: return
        viewModelScope.launch {
            val result = try {
                commandExecutor.reject(approval.id, sessionId())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CommandResult(
                    success = false,
                    action = approval.command.action,
                    error = e.message ?: "Reject failed"
                )
            }
            if (!waiter.isCompleted) waiter.complete(result)
        }
    }

    private fun loadBundledPayloads() {
        viewModelScope.launch {
            val rows = try {
                withContext(Dispatchers.IO) {
                    StageRemoteCatalog.discoverStageAssetPaths(::listAssetChildren)
                        .map { StagePayloadRow(StageRemoteCatalog.stagePayloadFromAsset(it)) }
                }
            } catch (e: Exception) {
                _banner.value = e.message ?: "Could not read stage assets"
                emptyList()
            }
            _payloads.value = rows
            _assetsScanned.value = true
            gate.withLock {
                _busy.value = true
                try {
                    refreshDevicePresence()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _banner.value = e.message ?: "Could not reach the Flipper"
                } finally {
                    _busy.value = false
                }
            }
        }
    }

    private fun listAssetChildren(path: String): List<String> {
        return try {
            context.assets.list(path)?.toList().orEmpty()
        } catch (_: IOException) {
            emptyList()
        }
    }

    private fun armOrRunBeat(beatId: String, selectedPath: String?) {
        val beat = StageRemoteCatalog.beatById(beatId) ?: return
        if (beat.requiresLiveConfirmation()) {
            _confirmRequest.value = StageConfirmRequest.Beat(beatId, selectedPath)
        } else {
            launchBeat(beatId, selectedPath)
        }
    }

    private fun openFilePicker(beatId: String, directory: String, extension: String) {
        viewModelScope.launch {
            gate.withLock {
                _busy.value = true
                try {
                    setBeat(beatId, StageFeedback(StagePhase.RUNNING, "Listing $directory"))
                    val listed = listMatchingFiles(directory, extension)
                    if (listed.error != null) {
                        setBeat(beatId, StageFeedback(StagePhase.ERROR, listed.error))
                        return@withLock
                    }
                    if (listed.files.isEmpty()) {
                        setBeat(beatId, StageFeedback(StagePhase.ERROR, "No $extension files in $directory"))
                        return@withLock
                    }
                    setBeat(beatId, StageFeedback(StagePhase.OK, "${listed.files.size} file(s) on device"))
                    _filePicker.value = StageFileChoice(beatId, directory, listed.files)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    setBeat(beatId, StageFeedback(StagePhase.ERROR, e.message ?: "Could not list $directory"))
                } finally {
                    _busy.value = false
                }
            }
        }
    }

    private fun launchBeat(beatId: String, selectedPath: String?) {
        val beat = StageRemoteCatalog.beatById(beatId) ?: return
        viewModelScope.launch {
            gate.withLock {
                _busy.value = true
                try {
                    setBeat(beatId, StageFeedback(StagePhase.RUNNING, "Running"))
                    val command = try {
                        beat.executeCommand(selectedPath)
                    } catch (e: IllegalArgumentException) {
                        setBeat(beatId, StageFeedback(StagePhase.ERROR, e.message ?: "Invalid beat"))
                        return@withLock
                    }
                    val result = runCommand(command) {
                        setBeat(beatId, StageFeedback(StagePhase.RUNNING, "Awaiting approval"))
                    }
                    setBeat(beatId, StageRemoteCatalog.feedbackFor(result))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    setBeat(beatId, StageFeedback(StagePhase.ERROR, e.message ?: "Command failed"))
                } finally {
                    _busy.value = false
                }
            }
        }
    }

    private fun launchPush(assetPaths: List<String>) {
        viewModelScope.launch {
            gate.withLock {
                _busy.value = true
                try {
                    for (assetPath in assetPaths) {
                        pushOne(assetPath)
                    }
                    refreshDevicePresence()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _banner.value = e.message ?: "Push failed"
                } finally {
                    _busy.value = false
                }
            }
        }
    }

    private suspend fun pushOne(assetPath: String) {
        val row = _payloads.value.find { it.payload.assetPath == assetPath } ?: return
        updatePayload(assetPath) { it.copy(feedback = StageFeedback(StagePhase.RUNNING, "Pushing")) }
        val content = try {
            readAsset(assetPath)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            updatePayload(assetPath) {
                it.copy(feedback = StageFeedback(StagePhase.ERROR, e.message ?: "Could not read asset"))
            }
            return
        }
        val written = writePayload(row, content).let { first ->
            if (first.success || !missingDirectory(first.error)) {
                first
            } else {
                val parent = row.payload.devicePath.substringBeforeLast('/')
                val created = runCommand(StageRemoteCatalog.createDirectoryCommand(parent)) {
                    updatePayload(assetPath) {
                        it.copy(feedback = StageFeedback(StagePhase.RUNNING, "Awaiting approval"))
                    }
                }
                if (!created.success && !alreadyExists(created.error)) {
                    created
                } else {
                    writePayload(row, content)
                }
            }
        }
        updatePayload(assetPath) { current ->
            current.copy(
                feedback = StageRemoteCatalog.feedbackFor(written),
                onDevice = current.onDevice || written.success
            )
        }
    }

    private suspend fun writePayload(row: StagePayloadRow, content: String): CommandResult {
        return runCommand(StageRemoteCatalog.writeFileCommand(row.payload.devicePath, content)) {
            updatePayload(row.payload.assetPath) {
                it.copy(feedback = StageFeedback(StagePhase.RUNNING, "Awaiting approval"))
            }
        }
    }

    private suspend fun refreshDevicePresence() {
        val badUsb = listNames(StageRemoteCatalog.BADUSB_DIR)
        val portal = listNames(StageRemoteCatalog.EVIL_PORTAL_DIR)
        _payloads.update { rows ->
            rows.map { row ->
                val listed = if (row.payload.devicePath.startsWith("${StageRemoteCatalog.EVIL_PORTAL_DIR}/")) {
                    portal
                } else {
                    badUsb
                }
                if (listed == null) {
                    row
                } else {
                    row.copy(
                        onDevice = StageRemoteCatalog.isPayloadOnDevice(
                            row.payload,
                            badUsb.orEmpty(),
                            portal.orEmpty()
                        )
                    )
                }
            }
        }
    }

    private suspend fun listNames(path: String): Set<String>? {
        val result = runCommand(StageRemoteCatalog.listDirectoryCommand(path))
        if (!result.success) return null
        return result.data?.entries.orEmpty().map { it.name }.toSet()
    }

    private suspend fun listMatchingFiles(directory: String, extension: String): ListedFiles {
        val root = runCommand(StageRemoteCatalog.listDirectoryCommand(directory))
        if (!root.success) {
            return ListedFiles(emptyList(), root.error ?: "Could not list $directory")
        }
        val entries = root.data?.entries.orEmpty()
        val files = StageRemoteCatalog.filePathsWithExtension(entries, extension).toMutableList()
        for (child in StageRemoteCatalog.childDirectories(entries)) {
            val nested = runCommand(StageRemoteCatalog.listDirectoryCommand(child))
            if (!nested.success) continue
            files += StageRemoteCatalog.filePathsWithExtension(nested.data?.entries.orEmpty(), extension)
        }
        return ListedFiles(files.distinct().sorted(), null)
    }

    private suspend fun readAsset(assetPath: String): String = withContext(Dispatchers.IO) {
        context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private suspend fun runCommand(
        command: ExecuteCommand,
        onAwaitingApproval: () -> Unit = {}
    ): CommandResult {
        val result = commandExecutor.execute(command, sessionId())
        val approvalId = result.pendingApprovalId
        if (!result.requiresConfirmation || approvalId.isNullOrBlank()) {
            return result
        }
        val approval = commandExecutor.getPendingApproval(approvalId)
            ?: return CommandResult(
                success = false,
                action = command.action,
                error = result.error ?: result.data?.message ?: "Approval expired"
            )
        onAwaitingApproval()
        val waiter = CompletableDeferred<CommandResult>()
        approvalWaiter = waiter
        _pendingApproval.value = approval
        return try {
            waiter.await()
        } finally {
            if (!waiter.isCompleted) {
                withContext(NonCancellable) {
                    runCatching { commandExecutor.reject(approval.id, sessionId()) }
                }
            }
            if (approvalWaiter === waiter) approvalWaiter = null
            if (_pendingApproval.value?.id == approval.id) _pendingApproval.value = null
        }
    }

    private fun blocked(): Boolean =
        _busy.value ||
            _confirmRequest.value != null ||
            _filePicker.value != null ||
            _pendingApproval.value != null

    private fun setBeat(id: String, feedback: StageFeedback) {
        _beatFeedback.update { it + (id to feedback) }
    }

    private fun updatePayload(assetPath: String, transform: (StagePayloadRow) -> StagePayloadRow) {
        _payloads.update { rows ->
            rows.map { if (it.payload.assetPath == assetPath) transform(it) else it }
        }
    }

    private fun sessionId(): String = auditService.getCurrentSessionId() ?: "stage-remote"

    private fun alreadyExists(error: String?): Boolean {
        val text = error?.lowercase().orEmpty()
        return text.contains("already") && text.contains("exist")
    }

    private fun missingDirectory(error: String?): Boolean {
        val text = error?.lowercase().orEmpty()
        return text.contains("not found") ||
            text.contains("not exist") ||
            text.contains("no such") ||
            text.contains("error_storage_not_exist")
    }

    private data class ListedFiles(val files: List<String>, val error: String?)
}
