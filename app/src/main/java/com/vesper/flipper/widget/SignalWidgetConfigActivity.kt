package com.vesper.flipper.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vesper.flipper.ble.FlipperFileSystem
import com.vesper.flipper.ui.theme.VesperTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SignalWidgetConfigActivity : ComponentActivity() {

    @Inject lateinit var fileSystem: FlipperFileSystem

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set the result to CANCELED in case the user backs out
        setResult(RESULT_CANCELED)

        // Get the widget ID from the intent
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            VesperTheme(darkTheme = true) {
                WidgetConfigScreen(
                    loadSignals = { loadAvailableSignals() },
                    onSave = { signals ->
                        saveWidgetConfiguration(signals)
                        finishWithResult()
                    },
                    onCancel = { finish() }
                )
            }
        }
    }

    /**
     * Loads the real captures stored on the connected Flipper, grouped by
     * signal family. Returns only families that actually have captures so the
     * picker never advertises signals that cannot be triggered.
     */
    private suspend fun loadAvailableSignals(): List<SignalGroup> {
        data class SignalSource(
            val label: String,
            val type: String,
            val dir: String,
            val ext: String
        )

        val sources = listOf(
            SignalSource("Sub-GHz", "subghz", "/ext/subghz", ".sub"),
            SignalSource("Infrared", "ir", "/ext/infrared", ".ir"),
            SignalSource("NFC", "nfc", "/ext/nfc", ".nfc"),
            SignalSource("RFID", "rfid", "/ext/lfrfid", ".rfid")
        )

        return sources.mapNotNull { source ->
            val files = fileSystem.listDirectory(source.dir).getOrNull().orEmpty()
                .filter { !it.isDirectory && it.name.endsWith(source.ext, ignoreCase = true) }
                .map { file ->
                    WidgetSignal(
                        id = file.path,
                        name = file.name.substringBeforeLast('.'),
                        type = source.type
                    )
                }
            if (files.isEmpty()) null else SignalGroup(source.label, source.type, files)
        }
    }

    private fun saveWidgetConfiguration(signals: List<WidgetSignal?>) {
        val prefs = getSharedPreferences("widget_$appWidgetId", MODE_PRIVATE)
        prefs.edit().apply {
            signals.forEachIndexed { index, signal ->
                if (signal != null) {
                    putString("signal_${index}_id", signal.id)
                    putString("signal_${index}_name", signal.name)
                    putString("signal_${index}_type", signal.type)
                } else {
                    remove("signal_${index}_id")
                    remove("signal_${index}_name")
                    remove("signal_${index}_type")
                }
            }
            apply()
        }
    }

    private fun finishWithResult() {
        // Update the widget
        val appWidgetManager = AppWidgetManager.getInstance(this)
        SignalWidget.updateAppWidget(this, appWidgetManager, appWidgetId)

        // Return success
        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    loadSignals: suspend () -> List<SignalGroup>,
    onSave: (List<WidgetSignal?>) -> Unit,
    onCancel: () -> Unit
) {
    var selectedSignals by remember { mutableStateOf(listOf<WidgetSignal?>(null, null, null, null)) }
    var selectedSlot by remember { mutableStateOf<Int?>(null) }

    // Real captures from the connected Flipper; null while loading.
    val signalsState = produceState<List<SignalGroup>?>(initialValue = null) {
        value = runCatching { loadSignals() }.getOrDefault(emptyList())
    }
    val availableSignals = signalsState.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D0D),
                        Color(0xFF1A1A2E)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
        ) {
            // Header
            TopAppBar(
                title = {
                    Text(
                        "Configure Widget",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSave(selectedSignals) },
                        enabled = selectedSignals.any { it != null }
                    ) {
                        Text("Save", color = Color(0xFFFF6B00))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Signal Slots
            Text(
                text = "Tap a slot to assign a signal",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) { index ->
                    SignalSlot(
                        signal = selectedSignals[index],
                        isSelected = selectedSlot == index,
                        slotNumber = index + 1,
                        onClick = { selectedSlot = index },
                        onClear = {
                            selectedSignals = selectedSignals.toMutableList().apply {
                                set(index, null)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Divider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 8.dp))

            // Signal List
            if (selectedSlot != null) {
                Text(
                    text = "Select a signal for Slot ${selectedSlot!! + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                when {
                    availableSignals == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFF6B00))
                        }
                    }
                    availableSignals.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color(0xFF444444)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No captured signals found",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Connect your Flipper and capture Sub-GHz, IR, NFC, or RFID signals first.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableSignals.forEach { group ->
                                item {
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFFF6B00),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(group.signals) { signal ->
                                    SignalItem(
                                        signal = signal,
                                        isSelected = selectedSignals.contains(signal),
                                        onClick = {
                                            selectedSlot?.let { slot ->
                                                selectedSignals = selectedSignals.toMutableList().apply {
                                                    set(slot, signal)
                                                }
                                                selectedSlot = null
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF444444)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tap a slot above to configure",
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SignalSlot(
    signal: WidgetSignal?,
    isSelected: Boolean,
    slotNumber: Int,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2A2A4E) else Color(0xFF1E1E2E)
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF6B00))
        } else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (signal != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = signal.name.substringBefore(" "),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = signal.name.substringAfter(" "),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1
                    )
                }
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color(0xFF555555),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Slot $slotNumber",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF555555)
                    )
                }
            }
        }
    }
}

@Composable
fun SignalItem(
    signal: WidgetSignal,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2A3A2E) else Color(0xFF1E1E2E)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = signal.name,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFF00C853),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

data class SignalGroup(
    val name: String,
    val type: String,
    val signals: List<WidgetSignal>
)
