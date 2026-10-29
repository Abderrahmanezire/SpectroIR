package com.example.ui.screens.history

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.SpectrumRepositoryImpl
import com.example.domain.model.Spectrum
import com.example.domain.repository.SpectrumRepository
import com.example.domain.spectroscopy.LaboratoryAnalysisReportData
import com.example.ui.components.ExportDialog
import com.example.ui.navigation.Screen
import com.example.ui.theme.AmberLaser
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldMatch
import com.example.util.ExportFormat
import com.example.util.ExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: SpectrumRepository = SpectrumRepositoryImpl(
        AppDatabase.getDatabase(application).spectrumDao()
    )
) : AndroidViewModel(application) {

    val historySpectra: StateFlow<List<Spectrum>> = repository.getHistorySpectra()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun renameItem(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                repository.renameSpectrum(id, trimmed)
            }
        }
    }

    fun duplicateItem(id: String, onDone: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val cloned = repository.duplicateSpectrum(id)
            if (cloned != null) {
                onDone?.invoke(cloned.name)
            }
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            repository.deleteSpectrum(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }
}

@Composable
fun HistoryScreen(
    onLoadSpectrum: (Spectrum) -> Unit,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val historyItems by viewModel.historySpectra.collectAsStateWithLifecycle()

    var itemToDelete by remember { mutableStateOf<Spectrum?>(null) }
    var itemToRename by remember { mutableStateOf<Spectrum?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var itemToExport by remember { mutableStateOf<Spectrum?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.US) }

    // Rename Dialog
    if (itemToRename != null) {
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            title = { Text("Rename Analysis", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a new identifier for this spectrum record:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("Analysis Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_rename_spectrum")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        itemToRename?.let { item ->
                            viewModel.renameItem(item.id, renameInput)
                        }
                        itemToRename = null
                    },
                    enabled = renameInput.isNotBlank(),
                    modifier = Modifier.testTag("button_confirm_rename")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Analysis Record") },
            text = { Text("Are you sure you want to remove '${itemToDelete?.name}' from local history? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDelete?.let { viewModel.deleteItem(it.id) }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("button_confirm_delete")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All History") },
            text = { Text("Are you sure you want to delete all saved spectra from local database storage?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAll()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("button_confirm_clear_all")
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Full Export Dialog
    itemToExport?.let { spec ->
        val exportData = remember(spec) {
            LaboratoryAnalysisReportData(
                spectrum = spec,
                detectedPeaks = spec.detectedPeaks,
                userSampleName = spec.metadata["user_sample_name"] ?: spec.name,
                userSampleType = spec.metadata["user_sample_type"] ?: "Organic Solid / Film",
                userExpectedComposition = spec.metadata["user_expected_composition"] ?: "",
                userKnownComponents = spec.metadata["user_known_components"] ?: "",
                userMeasurementMethod = spec.metadata["user_measurement_method"] ?: "ATR-FTIR",
                userExperimentalNotes = spec.metadata["user_notes"] ?: ""
            )
        }
        ExportDialog(
            data = exportData,
            onDismissRequest = { itemToExport = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Screen Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Saved History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${historyItems.size} analyses stored in offline Room database",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (historyItems.isNotEmpty()) {
                OutlinedButton(
                    onClick = { showClearAllDialog = true },
                    modifier = Modifier.testTag("button_clear_history")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (historyItems.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No saved analyses yet", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "When working with a spectrum in the Analysis workspace, tap 'Save' to persist it here with its complete preprocessing trail, detected peaks, and interpretation.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(Screen.ANALYZE) },
                            modifier = Modifier.testTag("button_history_empty_analyze")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Analyzer", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onNavigate(Screen.HOME) },
                            modifier = Modifier.testTag("button_history_empty_home")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Spectrum", fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(historyItems, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_item_${item.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            // Header: Name and Primary Reopen Action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onLoadSpectrum(item)
                                            onNavigate(Screen.ANALYZE)
                                        }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 1
                                        )
                                        if (item.isDemo) {
                                            Surface(
                                                color = AmberLaser.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(3.dp),
                                                modifier = Modifier.padding(start = 6.dp)
                                            ) {
                                                Text(
                                                    text = "DEMO",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AmberLaser,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${dateFormat.format(Date(item.createdAt))}  •  ${item.sourceType.label}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Quick Reopen Button
                                Button(
                                    onClick = {
                                        onLoadSpectrum(item)
                                        onNavigate(Screen.ANALYZE)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp).testTag("action_reopen_${item.id}")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = "Reopen",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reopen", fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Spectrum Summary metrics
                            Text(
                                text = "${item.pointCount} pts  •  ${item.spectrumType.label}  •  ${item.minWavenumber.toInt()}–${item.maxWavenumber.toInt()} cm⁻¹" +
                                        if (item.detectedPeaks.isNotEmpty()) "  •  ${item.detectedPeaks.size} peaks detected" else "",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace
                            )

                            if (item.preprocessingHistory.isNotEmpty()) {
                                Text(
                                    text = "Preprocessing: " + item.preprocessingHistory.joinToString(" → ") { it.stepType },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            // Action Toolbar: Rename, Duplicate, Export, Share, Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Rename
                                    IconButton(
                                        onClick = {
                                            itemToRename = item
                                            renameInput = item.name
                                        },
                                        modifier = Modifier.size(34.dp).testTag("action_rename_${item.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Rename Analysis",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Duplicate
                                    IconButton(
                                        onClick = {
                                            viewModel.duplicateItem(item.id) { cloneName ->
                                                Toast.makeText(context, "Duplicated as '$cloneName'", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(34.dp).testTag("action_duplicate_${item.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Duplicate Analysis",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Export Dialog
                                    IconButton(
                                        onClick = { itemToExport = item },
                                        modifier = Modifier.size(34.dp).testTag("action_export_${item.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = "Export Report",
                                            tint = CyanLaser,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Quick Share PDF
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val repData = LaboratoryAnalysisReportData(
                                                    spectrum = item,
                                                    detectedPeaks = item.detectedPeaks,
                                                    userSampleName = item.metadata["user_sample_name"] ?: item.name,
                                                    userSampleType = item.metadata["user_sample_type"] ?: "Organic Solid",
                                                    userMeasurementMethod = item.metadata["user_measurement_method"] ?: "ATR-FTIR"
                                                )
                                                val res = withContext(Dispatchers.IO) {
                                                    ExportManager.exportReport(
                                                        context = context,
                                                        data = repData,
                                                        format = ExportFormat.PDF
                                                    )
                                                }
                                                if (res.success && res.file != null) {
                                                    ExportManager.shareExportedFile(
                                                        context = context,
                                                        file = res.file,
                                                        format = ExportFormat.PDF,
                                                        sampleName = repData.effectiveSampleName
                                                    )
                                                } else {
                                                    Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(34.dp).testTag("action_quick_share_${item.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Share PDF",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Delete
                                IconButton(
                                    onClick = { itemToDelete = item },
                                    modifier = Modifier.size(34.dp).testTag("action_delete_${item.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Analysis",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
