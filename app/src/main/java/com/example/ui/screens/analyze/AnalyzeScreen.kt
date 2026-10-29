package com.example.ui.screens.analyze

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import com.example.ui.components.ExportDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.demo.DemoSpectraProvider
import com.example.domain.model.Spectrum
import com.example.domain.model.SpectrumPoint
import com.example.domain.model.SpectrumType
import com.example.ui.components.PeakListView
import com.example.ui.components.SpectrumViewer
import com.example.ui.components.rememberSpectrumViewerState
import com.example.ui.theme.AmberLaser
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldMatch
import java.util.Locale

@Composable
fun AnalyzeScreen(
    viewModel: AnalyzeViewModel,
    onNavigateToDictionaryWithQuery: ((String) -> Unit)? = null,
    onNavigateToDigitize: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val viewerState = rememberSpectrumViewerState()
    var showExportDialog by remember { mutableStateOf(false) }

    // Sync baseline points and selected peak with viewer state
    LaunchedEffect(uiState.estimatedBaselinePoints) {
        viewerState.baselinePoints = uiState.estimatedBaselinePoints
    }
    LaunchedEffect(uiState.selectedPeak) {
        viewerState.selectedPeak = uiState.selectedPeak
    }
    LaunchedEffect(uiState.numericalComparisonResult, uiState.showDifferenceSpectrum) {
        viewerState.differenceSpectrumPoints = if (uiState.showDifferenceSpectrum) {
            uiState.numericalComparisonResult?.differenceSpectrum?.points
        } else {
            null
        }
        viewerState.showDifferenceSpectrum = uiState.showDifferenceSpectrum
    }
    LaunchedEffect(uiState.isComparingNormalized) {
        viewerState.normalizeComparison = uiState.isComparingNormalized
    }

    // SAF File Picker for real spectroscopy data files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val fileName = com.example.util.FileUtils.getFileNameFromUri(context, it)
                val stream = context.contentResolver.openInputStream(it)
                if (stream != null) {
                    viewModel.importFromFileStream(stream, fileName)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Column Mapping Dialog when automatic import requires user verification
    uiState.pendingImportAnalysis?.let { analysis ->
        com.example.ui.components.ColumnMappingDialog(
            analysisResult = analysis,
            onConfirm = { mapping ->
                viewModel.confirmColumnMapping(mapping)
            },
            onDismiss = {
                viewModel.dismissImportDialog()
            }
        )
    }

    // Import error notice dialog with recovery paths
    uiState.importError?.let { errNotice ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportError() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(errNotice.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(errNotice.message, fontSize = 12.sp)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Recovery:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(errNotice.recoveryHint, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (errNotice.canOpenMapping) {
                        Button(
                            onClick = {
                                viewModel.dismissImportError()
                                viewModel.openColumnMappingForCurrentTable()
                            }
                        ) {
                            Text("Map Columns")
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.dismissImportError()
                            filePickerLauncher.launch(arrayOf("*/*"))
                        }
                    ) {
                        Text("Choose Another File")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissImportError() }) {
                    Text("Dismiss")
                }
            }
        )
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header: Spectrum title, demo badge, actions
            AnalyzeTopHeader(
                spectrum = uiState.currentSpectrum,
                isSaved = uiState.isSavedInHistory,
                hasParsedTable = uiState.lastParsedTable != null,
                isProcessing = uiState.isProcessing,
                onSave = { viewModel.saveToHistory() },
                onImportClick = {
                    filePickerLauncher.launch(arrayOf("*/*"))
                },
                onDigitizeClick = onNavigateToDigitize,
                onReconfigureMapping = { viewModel.openColumnMappingForCurrentTable() },
                onExportReport = {
                    showExportDialog = true
                }
            )

            // Full Scientific Export Modal
            if (showExportDialog) {
                val reportData = remember(uiState) {
                    viewModel.getLaboratoryReportData()
                }
                ExportDialog(
                    data = reportData,
                    onDismissRequest = { showExportDialog = false }
                )
            }

            // Main Interactive Spectrum Viewer (Adaptive 215dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                SpectrumViewer(
                    spectrum = uiState.currentSpectrum,
                    referenceSpectrum = uiState.referenceOverlay,
                    viewerState = viewerState,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Compact TabRow for Workspace Tools
            TabRow(
                selectedTabIndex = uiState.activeTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnalyzeTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.activeTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                maxLines = 1,
                                fontWeight = if (uiState.activeTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Tab Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (uiState.activeTab) {
                    AnalyzeTab.PEAKS -> {
                        item {
                            PeaksTabContent(
                                spectrum = uiState.currentSpectrum,
                                prominence = uiState.peakProminenceThreshold,
                                minDistance = uiState.peakMinDistance,
                                minHeight = uiState.peakMinHeight,
                                selectedPeak = uiState.selectedPeak,
                                onProminenceChange = { viewModel.setPeakProminence(it) },
                                onMinDistanceChange = { viewModel.setPeakMinDistance(it) },
                                onMinHeightChange = { viewModel.setPeakMinHeight(it) },
                                onDetectPeaks = { viewModel.detectPeaks() },
                                onPeakSelected = { peak ->
                                    viewModel.selectPeak(peak)
                                    viewerState.activeCursorPoint = SpectrumPoint(peak.wavenumber, peak.signalValue)
                                },
                                onAddManualPeak = { wn -> viewModel.addManualPeak(wn) },
                                onDeletePeak = { id -> viewModel.deletePeak(id) },
                                onUpdatePeakNote = { id, note, assign -> viewModel.updatePeakNote(id, note, assign) },
                                onSearchDictionary = { wn ->
                                    onNavigateToDictionaryWithQuery?.invoke("${wn.toInt()}")
                                }
                            )
                        }
                    }

                    AnalyzeTab.PROCESSING -> {
                        item {
                            ProcessingTabContent(
                                spectrum = uiState.currentSpectrum,
                                hasBaselinePreview = uiState.estimatedBaselinePoints != null,
                                derivativeSpectrum = uiState.derivativeSpectrum,
                                isViewingDerivative = uiState.isViewingDerivative,
                                onPreviewBaselinePolynomial = { deg -> viewModel.previewBaselinePolynomial(deg) },
                                onApplyBaselinePolynomial = { deg -> viewModel.applyBaselinePolynomial(deg) },
                                onPreviewBaselineRubberband = { seg -> viewModel.previewBaselineRubberband(seg) },
                                onApplyBaselineRubberband = { seg -> viewModel.applyBaselineRubberband(seg) },
                                onPreviewBaselineALS = { l, p, iter -> viewModel.previewBaselineALS(l, p, iter) },
                                onApplyBaselineALS = { l, p, iter -> viewModel.applyBaselineALS(l, p, iter) },
                                onApplyLinearBaseline = { viewModel.applyLinearBaseline() },
                                onClearBaselinePreview = { viewModel.clearBaselinePreview() },
                                onSavitzkyGolay = { win, poly -> viewModel.applySavitzkyGolay(win, poly) },
                                onMovingAverage = { win -> viewModel.applyMovingAverage(win) },
                                onNormalizeMinMax = { min, max -> viewModel.applyMinMaxNormalization(min, max) },
                                onNormalizeVector = { viewModel.applyVectorNormalization() },
                                onNormalizePeak = { wn, intVal -> viewModel.applyPeakNormalization(wn, intVal) },
                                onCalculateDerivative = { ord -> viewModel.calculateDerivative(ord) },
                                onToggleViewDerivative = { viewModel.toggleViewDerivative() },
                                onConvertToAbsorbance = { viewModel.convertTransmittanceToAbsorbance() },
                                onConvertToTransmittance = { viewModel.convertAbsorbanceToTransmittance() },
                                onResetToOriginal = { viewModel.resetToOriginal() }
                            )
                        }
                    }

                    AnalyzeTab.AI_ANALYSIS -> {
                        item {
                            AiAnalysisTabContent(
                                uiState = uiState,
                                onRunAi = { viewModel.runAiInterpretation() },
                                onTestConnection = { viewModel.testAiConnection() },
                                onSetApiKey = { viewModel.setCustomApiKey(it) },
                                onUpdateUserContext = { name, type, expected, known, method, notes ->
                                    viewModel.updateUserContext(name, type, expected, known, method, notes)
                                },
                                onSelectAiLayer = { viewModel.setActiveAiLayerIndex(it) },
                                onAcknowledgePrivacy = { viewModel.setPrivacyAcknowledged(it) },
                                onClearAiResult = { viewModel.clearAiResult() }
                            )
                        }
                    }

                    AnalyzeTab.COMPARE -> {
                        item {
                            CompareTabContent(
                                currentSpectrum = uiState.currentSpectrum,
                                selectedReference = uiState.selectedReference,
                                referenceOverlay = uiState.referenceOverlay,
                                comparisonResult = uiState.numericalComparisonResult,
                                activeAlgorithm = uiState.activeSimilarityAlgorithm,
                                isNormalized = uiState.isComparingNormalized,
                                showDifference = uiState.showDifferenceSpectrum,
                                peakToleranceCm = uiState.peakToleranceCm,
                                referenceLibrary = uiState.referenceLibrary,
                                searchQuery = uiState.referenceSearchQuery,
                                selectedMaterialClass = uiState.selectedMaterialClassFilter,
                                selectedSpectralRegion = uiState.selectedSpectralRegionFilter,
                                onSelectReference = { viewModel.selectReferenceForComparison(it) },
                                onSetAlgorithm = { viewModel.setSimilarityAlgorithm(it) },
                                onToggleNormalization = { viewModel.toggleComparisonNormalization(it) },
                                onToggleDifference = { viewModel.toggleDifferenceSpectrum(it) },
                                onSetPeakTolerance = { viewModel.setPeakToleranceCm(it) },
                                onSearchQuery = { viewModel.setReferenceSearchQuery(it) },
                                onFilterMaterialClass = { viewModel.setReferenceMaterialClassFilter(it) },
                                onFilterSpectralRegion = { viewModel.setReferenceSpectralRegionFilter(it) },
                                onImportCustomReference = { stream, fn, name, cas, formula, matClass, method, notes ->
                                    viewModel.importCustomReference(stream, fn, name, cas, formula, matClass, method, notes)
                                },
                                onDeleteCustomReference = { viewModel.deleteCustomReference(it) },
                                onNavigateToDictionary = onNavigateToDictionaryWithQuery
                            )
                        }
                    }

                    AnalyzeTab.INFO -> {
                        item {
                            SpectrumInfoTabContent(
                                spectrum = uiState.currentSpectrum,
                                hasParsedTable = uiState.lastParsedTable != null,
                                onReconfigureMapping = { viewModel.openColumnMappingForCurrentTable() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyzeTopHeader(
    spectrum: Spectrum,
    isSaved: Boolean,
    hasParsedTable: Boolean,
    isProcessing: Boolean,
    onSave: () -> Unit,
    onImportClick: () -> Unit,
    onDigitizeClick: (() -> Unit)? = null,
    onReconfigureMapping: () -> Unit,
    onExportReport: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = spectrum.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (isProcessing) {
                        Spacer(modifier = Modifier.width(6.dp))
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    if (spectrum.isDemo) {
                        Surface(
                            color = AmberLaser.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier.padding(end = 4.dp)
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

                    Text(
                        text = "${spectrum.spectrumType.label} • ${spectrum.pointCount} pts",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasParsedTable) {
                    IconButton(
                        onClick = onReconfigureMapping,
                        modifier = Modifier.size(36.dp).testTag("action_map_columns")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Map Columns", tint = CyanLaser, modifier = Modifier.size(18.dp))
                    }
                }

                if (onDigitizeClick != null) {
                    IconButton(
                        onClick = onDigitizeClick,
                        modifier = Modifier.size(36.dp).testTag("action_digitize_camera")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Digitize from Photo", tint = CyanLaser, modifier = Modifier.size(18.dp))
                    }
                }

                IconButton(
                    onClick = onImportClick,
                    modifier = Modifier.size(36.dp).testTag("action_import_file")
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Import File", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onSave,
                    modifier = Modifier.size(36.dp).testTag("action_save_history")
                ) {
                    Icon(
                        if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save to History",
                        tint = if (isSaved) EmeraldMatch else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                FilledTonalButton(
                    onClick = onExportReport,
                    modifier = Modifier.height(32.dp).testTag("button_export_analysis").testTag("action_export_report"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PeaksTabContent(
    spectrum: Spectrum,
    prominence: Double,
    minDistance: Double,
    minHeight: Double,
    selectedPeak: com.example.domain.model.Peak?,
    onProminenceChange: (Double) -> Unit,
    onMinDistanceChange: (Double) -> Unit,
    onMinHeightChange: (Double) -> Unit,
    onDetectPeaks: () -> Unit,
    onPeakSelected: (com.example.domain.model.Peak) -> Unit,
    onAddManualPeak: (Double) -> Unit,
    onDeletePeak: (String) -> Unit,
    onUpdatePeakNote: (String, String, String?) -> Unit,
    onSearchDictionary: ((Double) -> Unit)? = null
) {
    var showAdvancedParams by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Min Prominence: ${"%.1f".format(Locale.US, prominence)}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    IconButton(
                        onClick = { showAdvancedParams = !showAdvancedParams },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (showAdvancedParams) Icons.Default.ExpandLess else Icons.Default.Tune,
                            contentDescription = "Detection Settings",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Button(
                    onClick = onDetectPeaks,
                    modifier = Modifier.height(34.dp).testTag("button_detect_peaks"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.FilterAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-Detect", fontSize = 12.sp)
                }
            }

            Slider(
                value = prominence.toFloat(),
                onValueChange = { onProminenceChange(it.toDouble()) },
                valueRange = 1f..35f,
                modifier = Modifier.fillMaxWidth().testTag("slider_peak_prominence")
            )

            // Collapsible Advanced Settings (Distance, Height)
            if (showAdvancedParams) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Min Distance: ${minDistance.toInt()} cm⁻¹", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Slider(
                    value = minDistance.toFloat(),
                    onValueChange = { onMinDistanceChange(it.toDouble()) },
                    valueRange = 4f..40f,
                    modifier = Modifier.fillMaxWidth().testTag("slider_min_distance")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Min Height: ${"%.1f".format(Locale.US, minHeight)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Slider(
                    value = minHeight.toFloat(),
                    onValueChange = { onMinHeightChange(it.toDouble()) },
                    valueRange = 0f..50f,
                    modifier = Modifier.fillMaxWidth().testTag("slider_min_height")
                )
            }
        }
    }

    PeakListView(
        peaks = spectrum.detectedPeaks,
        signalUnit = spectrum.yUnit,
        selectedPeak = selectedPeak,
        onPeakSelected = onPeakSelected,
        onAddManualPeak = onAddManualPeak,
        onDeletePeak = onDeletePeak,
        onUpdatePeakNote = onUpdatePeakNote,
        onSearchDictionary = onSearchDictionary,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
private fun ProcessingTabContent(
    spectrum: Spectrum,
    hasBaselinePreview: Boolean,
    derivativeSpectrum: Spectrum?,
    isViewingDerivative: Boolean,
    onPreviewBaselinePolynomial: (Int) -> Unit,
    onApplyBaselinePolynomial: (Int) -> Unit,
    onPreviewBaselineRubberband: (Int) -> Unit,
    onApplyBaselineRubberband: (Int) -> Unit,
    onPreviewBaselineALS: (lambda: Double, p: Double, iter: Int) -> Unit,
    onApplyBaselineALS: (lambda: Double, p: Double, iter: Int) -> Unit,
    onApplyLinearBaseline: () -> Unit,
    onClearBaselinePreview: () -> Unit,
    onSavitzkyGolay: (windowSize: Int, polyOrder: Int) -> Unit,
    onMovingAverage: (windowSize: Int) -> Unit,
    onNormalizeMinMax: (Double, Double) -> Unit,
    onNormalizeVector: () -> Unit,
    onNormalizePeak: (Double?, Double) -> Unit,
    onCalculateDerivative: (Int) -> Unit,
    onToggleViewDerivative: () -> Unit,
    onConvertToAbsorbance: () -> Unit,
    onConvertToTransmittance: () -> Unit,
    onResetToOriginal: () -> Unit
) {
    var selectedBaselineMethod by remember { mutableStateOf(BaselineMethod.ALS) }
    var polyDegree by remember { mutableIntStateOf(3) }
    var rubberbandSegments by remember { mutableIntStateOf(24) }
    var alsLambdaIndex by remember { mutableIntStateOf(2) }
    val alsLambdas = listOf(1000.0, 10000.0, 100000.0, 1000000.0)
    var sgWindowSize by remember { mutableIntStateOf(9) }
    var sgPolyOrder by remember { mutableIntStateOf(2) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Status bar & Reset Button if modifications were made
        if (spectrum.preprocessingHistory.isNotEmpty() || isViewingDerivative) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Processed Spectrum (${spectrum.preprocessingHistory.size} ops applied)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    TextButton(
                        onClick = onResetToOriginal,
                        modifier = Modifier.height(28.dp).testTag("button_reset_original"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Original", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // 1. Baseline Correction Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Baseline Correction", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (hasBaselinePreview) {
                        TextButton(
                            onClick = onClearBaselinePreview,
                            modifier = Modifier.height(26.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Clear Preview", fontSize = 11.sp)
                        }
                    }
                }

                // Method selector chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(BaselineMethod.entries.toTypedArray()) { method ->
                        FilterChip(
                            selected = selectedBaselineMethod == method,
                            onClick = { selectedBaselineMethod = method },
                            label = { Text(method.label, fontSize = 11.sp) }
                        )
                    }
                }

                when (selectedBaselineMethod) {
                    BaselineMethod.POLYNOMIAL -> {
                        Text("Polynomial Degree: $polyDegree", fontSize = 11.sp)
                        Slider(
                            value = polyDegree.toFloat(),
                            onValueChange = { polyDegree = it.toInt() },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onPreviewBaselinePolynomial(polyDegree) },
                                modifier = Modifier.weight(1f).height(36.dp).testTag("button_preview_poly_baseline")
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { onApplyBaselinePolynomial(polyDegree) },
                                modifier = Modifier.weight(1f).height(36.dp).testTag("button_apply_poly_baseline")
                            ) {
                                Text("Apply", fontSize = 11.sp)
                            }
                        }
                    }

                    BaselineMethod.RUBBERBAND -> {
                        Text("Envelope Segments: $rubberbandSegments", fontSize = 11.sp)
                        Slider(
                            value = rubberbandSegments.toFloat(),
                            onValueChange = { rubberbandSegments = it.toInt() },
                            valueRange = 10f..48f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onPreviewBaselineRubberband(rubberbandSegments) },
                                modifier = Modifier.weight(1f).height(36.dp).testTag("button_preview_rubberband")
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { onApplyBaselineRubberband(rubberbandSegments) },
                                modifier = Modifier.weight(1f).height(36.dp).testTag("button_apply_rubberband")
                            ) {
                                Text("Apply", fontSize = 11.sp)
                            }
                        }
                    }

                    BaselineMethod.ALS -> {
                        Text("Smoothness λ: 10^${alsLambdaIndex + 3} • Asymmetry p: 0.01", fontSize = 11.sp)
                        Slider(
                            value = alsLambdaIndex.toFloat(),
                            onValueChange = { alsLambdaIndex = it.toInt() },
                            valueRange = 0f..3f,
                            steps = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onPreviewBaselineALS(alsLambdas[alsLambdaIndex], 0.01, 10) },
                                modifier = Modifier.weight(1f).height(36.dp).testTag("button_preview_als")
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { onApplyBaselineALS(alsLambdas[alsLambdaIndex], 0.01, 10) },
                                modifier = Modifier.weight(1f).height(36.dp).testTag("button_apply_als")
                            ) {
                                Text("Apply", fontSize = 11.sp)
                            }
                        }
                    }

                    BaselineMethod.LINEAR -> {
                        Text("Two-point anchor slope correction (4000 to 400 cm⁻¹)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = onApplyLinearBaseline,
                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("button_apply_linear_baseline")
                        ) {
                            Text("Apply Linear Correction", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 2. Savitzky-Golay Smoothing Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Savitzky-Golay Smoothing", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                // Window size chips
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Window: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val winOptions = listOf(5, 7, 9, 11, 15, 21)
                        items(winOptions) { win ->
                            FilterChip(
                                selected = sgWindowSize == win,
                                onClick = { sgWindowSize = win },
                                label = { Text("$win", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Polynomial order chips
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Poly: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val polyOptions = listOf(2, 3, 4)
                        items(polyOptions) { poly ->
                            FilterChip(
                                selected = sgPolyOrder == poly,
                                onClick = { sgPolyOrder = poly },
                                label = { Text("Degree $poly", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Button(
                    onClick = { onSavitzkyGolay(sgWindowSize, sgPolyOrder) },
                    modifier = Modifier.fillMaxWidth().height(36.dp).testTag("button_smooth_sg")
                ) {
                    Text("Apply Smoothing", fontSize = 11.sp)
                }
            }
        }

        // 3. Normalization Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Normalization", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onNormalizeMinMax(0.0, 100.0) },
                        modifier = Modifier.weight(1f).height(34.dp).testTag("button_norm_minmax"),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("Min-Max", fontSize = 11.sp)
                    }

                    FilledTonalButton(
                        onClick = onNormalizeVector,
                        modifier = Modifier.weight(1f).height(34.dp).testTag("button_norm_vector"),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("Vector L2", fontSize = 11.sp)
                    }

                    FilledTonalButton(
                        onClick = { onNormalizePeak(null, 100.0) },
                        modifier = Modifier.weight(1f).height(34.dp).testTag("button_norm_peak"),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("Peak 100%", fontSize = 11.sp)
                    }
                }
            }
        }

        // 4. Derivatives Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Derivatives", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onCalculateDerivative(1) },
                        modifier = Modifier.weight(1f).height(34.dp).testTag("button_calc_deriv_1"),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("1st Deriv (dY/dν)", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { onCalculateDerivative(2) },
                        modifier = Modifier.weight(1f).height(34.dp).testTag("button_calc_deriv_2"),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("2nd Deriv (d²Y/dν²)", fontSize = 11.sp)
                    }
                }

                if (derivativeSpectrum != null) {
                    Surface(
                        color = CyanLaser.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Derivative: ${derivativeSpectrum.yUnit}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanLaser
                            )
                            Button(
                                onClick = onToggleViewDerivative,
                                modifier = Modifier.height(28.dp).testTag("button_toggle_view_derivative"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(if (isViewingDerivative) "Main Curve" else "View on Graph", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // 5. Unit Conversion Card (%T <-> Absorbance)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Scale: ${spectrum.spectrumType.label}", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                Button(
                    onClick = {
                        if (spectrum.spectrumType == SpectrumType.Transmittance) onConvertToAbsorbance() else onConvertToTransmittance()
                    },
                    modifier = Modifier.height(32.dp).testTag("button_convert_type"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Text(if (spectrum.spectrumType == SpectrumType.Transmittance) "To Absorbance" else "To %T", fontSize = 11.sp)
                }
            }
        }

        // Preprocessing History Trail
        if (spectrum.preprocessingHistory.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Applied History", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    spectrum.preprocessingHistory.forEachIndexed { i, step ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}. ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(step.stepType, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            if (step.description.isNotEmpty()) {
                                Text(" • ${step.description}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun SpectrumInfoTabContent(
    spectrum: Spectrum,
    hasParsedTable: Boolean,
    onReconfigureMapping: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Spectrum Parameters", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (hasParsedTable) {
                    FilledTonalButton(
                        onClick = onReconfigureMapping,
                        modifier = Modifier.height(28.dp).testTag("button_reconfigure_mapping"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Re-Map Columns", fontSize = 11.sp)
                    }
                }
            }
            HorizontalDivider()

            InfoRow("Name", spectrum.name)
            InfoRow("Source", spectrum.sourceType.label)
            InfoRow("Type", spectrum.spectrumType.label)
            InfoRow("Range", "${"%.1f".format(spectrum.minWavenumber)} - ${"%.1f".format(spectrum.maxWavenumber)} ${spectrum.xUnit}")
            InfoRow("Data Points", "${spectrum.pointCount}")
            if (spectrum.rawPoints.isNotEmpty() && spectrum.rawPoints.size != spectrum.pointCount) {
                InfoRow("Raw Points", "${spectrum.rawPoints.size}")
            }
            InfoRow("Signal Range", "${"%.2f".format(spectrum.minSignal)} to ${"%.2f".format(spectrum.maxSignal)} ${spectrum.yUnit}")

            if (spectrum.metadata.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("Metadata", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                spectrum.metadata.forEach { (k, v) ->
                    InfoRow(k, v)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
