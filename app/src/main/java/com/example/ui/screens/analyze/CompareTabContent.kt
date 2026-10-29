package com.example.ui.screens.analyze

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.domain.model.ReferenceSpectrum
import com.example.domain.model.Spectrum
import com.example.domain.spectroscopy.NumericalComparisonResult
import com.example.domain.spectroscopy.PeakMatchStatus
import com.example.domain.spectroscopy.SimilarityAlgorithm
import com.example.ui.theme.AmberLaser
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldMatch
import java.io.InputStream
import java.util.Locale
import kotlin.math.abs

@Composable
fun CompareTabContent(
    currentSpectrum: Spectrum,
    selectedReference: ReferenceSpectrum?,
    referenceOverlay: Spectrum?,
    comparisonResult: NumericalComparisonResult?,
    activeAlgorithm: SimilarityAlgorithm,
    isNormalized: Boolean,
    showDifference: Boolean,
    peakToleranceCm: Double,
    referenceLibrary: List<ReferenceSpectrum>,
    searchQuery: String,
    selectedMaterialClass: String?,
    selectedSpectralRegion: String?,
    onSelectReference: (ReferenceSpectrum?) -> Unit,
    onSetAlgorithm: (SimilarityAlgorithm) -> Unit,
    onToggleNormalization: (Boolean) -> Unit,
    onToggleDifference: (Boolean) -> Unit,
    onSetPeakTolerance: (Double) -> Unit,
    onSearchQuery: (String) -> Unit,
    onFilterMaterialClass: (String?) -> Unit,
    onFilterSpectralRegion: (String?) -> Unit,
    onImportCustomReference: (InputStream, String, String, String?, String?, String, String, String) -> Unit,
    onDeleteCustomReference: (String) -> Unit,
    onNavigateToDictionary: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // State for Custom Reference Import Dialog
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingFileUri by remember { mutableStateOf<Uri?>(null) }
    var pendingFileName by remember { mutableStateOf("") }
    var customCompoundName by remember { mutableStateOf("") }
    var customCasNumber by remember { mutableStateOf("") }
    var customFormula by remember { mutableStateOf("") }
    var customMaterialClass by remember { mutableStateOf("Organic Chemical") }
    var customMethod by remember { mutableStateOf("ATR-FTIR (Diamond Crystal)") }
    var customNotes by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingFileUri = it
            pendingFileName = com.example.util.FileUtils.getFileNameFromUri(context, it)
            customCompoundName = pendingFileName.substringBeforeLast(".")
            showImportDialog = true
        }
    }

    var isAiEvidenceExpanded by remember { mutableStateOf(false) }
    var isPeakTableExpanded by remember { mutableStateOf(true) }
    var isLibraryBrowserExpanded by remember { mutableStateOf(selectedReference == null) }

    val materialClasses = listOf(
        "All Classes",
        "Polymer",
        "Solvent",
        "Alcohol",
        "Ketone",
        "Ester",
        "Aromatic",
        "Carboxylic Acid",
        "Pharmaceutical",
        "Custom"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // -------------------------------------------------------------
        // 1. COMPARISON HEADER & ACTIVE REFERENCE BAR
        // -------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.CompareArrows,
                    contentDescription = null,
                    tint = CyanLaser,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Reference Comparison",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Reference", fontSize = 11.sp)
                }

                if (selectedReference != null) {
                    TextButton(
                        onClick = { onSelectReference(null) },
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Clear", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. CRITICAL SCIENTIFIC DISTINCTION CALLOUT
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = AmberLaser.copy(alpha = 0.12f)
            ),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AmberLaser.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Scientific Caveat",
                    tint = AmberLaser,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "SPECTRAL SIMILARITY ≠ CHEMICAL IDENTIFICATION",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = AmberLaser,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Numerical similarity calculates curve correlation over observed bands only. It does NOT prove chemical identity. Factors such as sampling geometry (ATR vs transmission), baseline tilt, optical dispersion, concentration, polymorphs, and multicomponent mixtures cause spectral variation.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 3. ACTIVE COMPARISON PANEL (when a reference is selected)
        // -------------------------------------------------------------
        if (selectedReference != null && comparisonResult != null) {
            ActiveComparisonSummaryCard(
                result = comparisonResult,
                reference = selectedReference,
                activeAlgorithm = activeAlgorithm,
                isNormalized = isNormalized,
                showDifference = showDifference,
                peakToleranceCm = peakToleranceCm,
                onSetAlgorithm = onSetAlgorithm,
                onToggleNormalization = onToggleNormalization,
                onToggleDifference = onToggleDifference,
                onSetPeakTolerance = onSetPeakTolerance,
                onChangeReferenceClick = { isLibraryBrowserExpanded = !isLibraryBrowserExpanded }
            )

            // Difference Spectrum Details Card (if toggled)
            if (showDifference) {
                DifferenceSpectrumMetricsCard(
                    diff = comparisonResult.differenceSpectrum,
                    unknownType = currentSpectrum.spectrumType,
                    referenceType = selectedReference.yAxisType
                )
            }

            // Peak Comparison Section
            PeakComparisonSection(
                matches = comparisonResult.peakMatches,
                toleranceCm = peakToleranceCm,
                isExpanded = isPeakTableExpanded,
                onToggleExpanded = { isPeakTableExpanded = !isPeakTableExpanded },
                onNavigateToDictionary = onNavigateToDictionary
            )

            // Structured AI Evidence Export Card
            StructuredAiEvidenceCard(
                result = comparisonResult,
                isExpanded = isAiEvidenceExpanded,
                onToggleExpanded = { isAiEvidenceExpanded = !isAiEvidenceExpanded },
                onCopy = { text ->
                    clipboardManager.setText(AnnotatedString(text))
                    Toast.makeText(context, "AI Evidence payload copied to clipboard!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // -------------------------------------------------------------
        // 4. REFERENCE LIBRARY BROWSER
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isLibraryBrowserExpanded = !isLibraryBrowserExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = CyanLaser, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedReference != null) "Search Other Reference Standards" else "Select Reference Spectrum for Comparison",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Icon(
                        if (isLibraryBrowserExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (isLibraryBrowserExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        placeholder = { Text("Search by name, CAS, formula, functional group...", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQuery("") }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Material Class filter chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(materialClasses) { matClass ->
                            val isSelected = if (matClass == "All Classes") selectedMaterialClass == null else selectedMaterialClass == matClass
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onFilterMaterialClass(if (matClass == "All Classes") null else matClass)
                                },
                                label = { Text(matClass, fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanLaser.copy(alpha = 0.2f),
                                    selectedLabelColor = CyanLaser
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filtered References list
                    val filteredList = referenceLibrary.filter { ref ->
                        val query = searchQuery.trim().lowercase(Locale.US)
                        val matchesQuery = query.isEmpty() ||
                            ref.compoundName.lowercase(Locale.US).contains(query) ||
                            (ref.chemicalFormula?.lowercase(Locale.US)?.contains(query) == true) ||
                            (ref.casNumber?.lowercase(Locale.US)?.contains(query) == true) ||
                            ref.materialClass.lowercase(Locale.US).contains(query) ||
                            ref.functionalGroups.any { it.lowercase(Locale.US).contains(query) } ||
                            ref.spectralRegions.any { it.lowercase(Locale.US).contains(query) } ||
                            ref.characteristicPeaks.any { abs(it - (query.toDoubleOrNull() ?: -999.0)) < 20.0 }

                        val matchesClass = selectedMaterialClass == null ||
                            (selectedMaterialClass == "Custom" && ref.isUserCustom) ||
                            ref.materialClass.contains(selectedMaterialClass, ignoreCase = true)

                        matchesQuery && matchesClass
                    }

                    Text(
                        text = "Available Standards (${filteredList.size} of ${referenceLibrary.size}):",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (filteredList.isEmpty()) {
                        Text(
                            text = "No reference spectra matching '$searchQuery'. Try clearing filters or importing a custom file.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            filteredList.forEach { ref ->
                                ReferenceCardItem(
                                    reference = ref,
                                    isSelected = selectedReference?.id == ref.id,
                                    onSelect = {
                                        onSelectReference(if (selectedReference?.id == ref.id) null else ref)
                                    },
                                    onDeleteCustom = if (ref.isUserCustom) {
                                        { onDeleteCustomReference(ref.id) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // CUSTOM REFERENCE IMPORT DIALOG
    // -------------------------------------------------------------
    if (showImportDialog && pendingFileUri != null) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = CyanLaser)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Reference Spectrum", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("File: $pendingFileName", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = customCompoundName,
                        onValueChange = { customCompoundName = it },
                        label = { Text("Compound Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customFormula,
                            onValueChange = { customFormula = it },
                            label = { Text("Formula (e.g. C8H8)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customCasNumber,
                            onValueChange = { customCasNumber = it },
                            label = { Text("CAS Number") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customMaterialClass,
                            onValueChange = { customMaterialClass = it },
                            label = { Text("Material Class") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customMethod,
                            onValueChange = { customMethod = it },
                            label = { Text("Method (ATR/KBr)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = customNotes,
                        onValueChange = { customNotes = it },
                        label = { Text("Notes / Preparation") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingFileUri
                        if (uri != null && customCompoundName.isNotBlank()) {
                            try {
                                val stream = context.contentResolver.openInputStream(uri)
                                if (stream != null) {
                                    onImportCustomReference(
                                        stream,
                                        pendingFileName,
                                        customCompoundName.trim(),
                                        customCasNumber.trim().ifBlank { null },
                                        customFormula.trim().ifBlank { null },
                                        customMaterialClass.trim(),
                                        customMethod.trim(),
                                        customNotes.trim()
                                    )
                                    showImportDialog = false
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = customCompoundName.isNotBlank()
                ) {
                    Text("Add to Library")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// COMPONENT: Active Comparison Summary Card
// -------------------------------------------------------------
@Composable
private fun ActiveComparisonSummaryCard(
    result: NumericalComparisonResult,
    reference: ReferenceSpectrum,
    activeAlgorithm: SimilarityAlgorithm,
    isNormalized: Boolean,
    showDifference: Boolean,
    peakToleranceCm: Double,
    onSetAlgorithm: (SimilarityAlgorithm) -> Unit,
    onToggleNormalization: (Boolean) -> Unit,
    onToggleDifference: (Boolean) -> Unit,
    onSetPeakTolerance: (Double) -> Unit,
    onChangeReferenceClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header with Reference Identity & Switch button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = reference.compoundName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = if (reference.isUserCustom) CyanLaser.copy(alpha = 0.2f) else EmeraldMatch.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = if (reference.isUserCustom) "CUSTOM" else "STANDARD REF",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (reference.isUserCustom) CyanLaser else EmeraldMatch,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = "CAS: ${reference.casNumber ?: "N/A"} • ${reference.chemicalFormula ?: ""} • ${reference.measurementMethod}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = onChangeReferenceClick,
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text("Change", fontSize = 11.sp)
                }
            }

            HorizontalDivider()

            // Main Score Indicator (Clearly labeled "Spectral similarity", NOT certainty)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "SPECTRAL SIMILARITY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${"%.1f".format(Locale.US, result.similarityScore)}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = if (result.similarityScore > 85.0) EmeraldMatch else if (result.similarityScore > 65.0) AmberLaser else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Method: ${activeAlgorithm.displayName}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Resampling & Overlap summary badge
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Overlap: ${(result.rangeOverlapRatio * 100).toInt()}% (${result.commonRangeMinWn.toInt()}–${result.commonRangeMaxWn.toInt()} cm⁻¹)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Resampled grid: ${result.resampledGridPointsCount} pts @ ${result.gridSpacingCm} cm⁻¹",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Numerical Algorithm Selector Chips
            Text(
                text = "Numerical Similarity Metric:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 1.dp)
            ) {
                items(SimilarityAlgorithm.entries) { algo ->
                    val isSelected = activeAlgorithm == algo
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSetAlgorithm(algo) },
                        label = { Text(algo.shortName, fontSize = 10.sp) },
                        modifier = Modifier.height(28.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Algorithm method description
            Text(
                text = activeAlgorithm.description,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 13.sp
            )

            // Multi-Algorithm Matrix (Scannable comparison table)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("All Numerical Metrics Comparison:", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        result.allAlgorithmScores.forEach { (algo, score) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(algo.shortName, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${"%.1f".format(Locale.US, score)}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (algo == activeAlgorithm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Scientific Limitations / Incompatibility warnings
            if (result.scientificLimitations.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scientific Limitations & Range Warnings:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                        }
                        result.scientificLimitations.forEach { warn ->
                            Text("• $warn", fontSize = 10.sp, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }

            HorizontalDivider()

            // Overlay & Difference View Controls
            Text(
                text = "Overlay & Inspection Controls:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isNormalized,
                    onClick = { onToggleNormalization(!isNormalized) },
                    label = { Text("Normalize Amplitude", fontSize = 10.sp) },
                    modifier = Modifier.height(28.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldMatch.copy(alpha = 0.2f),
                        selectedLabelColor = EmeraldMatch
                    )
                )

                FilterChip(
                    selected = showDifference,
                    onClick = { onToggleDifference(!showDifference) },
                    label = { Text("Difference Spectrum (Δ)", fontSize = 10.sp) },
                    modifier = Modifier.height(28.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE040FB).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFFE040FB)
                    )
                )
            }
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: Difference Spectrum Metrics Card
// -------------------------------------------------------------
@Composable
private fun DifferenceSpectrumMetricsCard(
    diff: com.example.domain.spectroscopy.DifferenceSpectrum,
    unknownType: com.example.domain.model.SpectrumType,
    referenceType: com.example.domain.model.SpectrumType
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE040FB).copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE040FB).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFFE040FB), shape = RoundedCornerShape(2.dp)) {
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Difference Spectrum: D(ṽ) = Unknown - Reference",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFFE040FB)
                    )
                }
            }

            Text(
                text = "Evaluates residual spectral deviations. Flat difference near 0 confirms close matching of vibrational band positions and widths.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Mean Abs Dev", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${"%.2f".format(Locale.US, diff.meanAbsoluteDifference)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Column {
                    Text("RMS Residual", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${"%.2f".format(Locale.US, diff.rmsDifference)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Column {
                    Text("Max Deviation", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${"%.1f".format(Locale.US, diff.maxDeviationValue)} @ ${diff.maxDeviationWavenumber.toInt()} cm⁻¹", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: Peak Comparison Section
// -------------------------------------------------------------
@Composable
private fun PeakComparisonSection(
    matches: List<com.example.domain.spectroscopy.PeakComparisonMatch>,
    toleranceCm: Double,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onNavigateToDictionary: ((String) -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Peak Comparison & Cross-Matching",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val matchedCount = matches.count { it.status == PeakMatchStatus.MATCHED }
                    Surface(
                        color = EmeraldMatch.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "$matchedCount / ${matches.size} matched",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldMatch,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isExpanded) {
                Text(
                    text = "Positional comparison uses a ±${"%.1f".format(Locale.US, toleranceCm)} cm⁻¹ tolerance window rather than requiring exact mathematical equality.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                // Peak Comparison Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Unknown Peak", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.2f))
                    Text("Reference Peak", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.2f))
                    Text("Diff (Δṽ)", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    Text("Status", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.4f))
                }

                HorizontalDivider()

                // Peak rows
                matches.forEach { match ->
                    PeakComparisonRowItem(match = match, onNavigateToDictionary = onNavigateToDictionary)
                }
            }
        }
    }
}

@Composable
private fun PeakComparisonRowItem(
    match: com.example.domain.spectroscopy.PeakComparisonMatch,
    onNavigateToDictionary: ((String) -> Unit)?
) {
    val statusColor = when (match.status) {
        PeakMatchStatus.MATCHED -> EmeraldMatch
        PeakMatchStatus.SHIFTED -> AmberLaser
        PeakMatchStatus.UNMATCHED_UNKNOWN -> CyanLaser
        PeakMatchStatus.UNMATCHED_REFERENCE -> Color(0xFF90A4AE)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Unknown Peak
            val uText = match.unknownPeakWn?.let { "${"%.1f".format(Locale.US, it)} cm⁻¹" } ?: "—"
            Text(
                text = uText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (match.unknownPeakWn != null) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .weight(1.2f)
                    .clickable {
                        match.unknownPeakWn?.let { onNavigateToDictionary?.invoke("${it.toInt()}") }
                    }
            )

            // Reference Peak
            val rText = match.referencePeakWn?.let { "${"%.1f".format(Locale.US, it)} cm⁻¹" } ?: "—"
            Text(
                text = rText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1.2f)
            )

            // Difference
            val diffText = match.differenceCm?.let {
                val sign = if (it > 0) "+" else ""
                "$sign${"%.1f".format(Locale.US, it)} cm⁻¹"
            } ?: "—"
            Text(
                text = diffText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = if (match.differenceCm != null && abs(match.differenceCm) > 10.0) AmberLaser else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Status Badge
            Surface(
                color = statusColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier.weight(1.4f)
            ) {
                Text(
                    text = match.status.label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        // Diagnostic subtext note
        if (match.diagnosticNote.isNotBlank()) {
            Text(
                text = match.diagnosticNote,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp, top = 1.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: Structured AI Evidence Card (for future stage)
// -------------------------------------------------------------
@Composable
private fun StructuredAiEvidenceCard(
    result: NumericalComparisonResult,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onCopy: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = CyanLaser, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Structured Comparison Evidence (AI Stage Ready)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Structured evidence bundle prepared for downstream scientific interpretation. Captures numerical similarity, overlapping ranges, matched/unmatched peaks, and reference standard metadata.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                val jsonPayload = buildString {
                    appendLine("{")
                    appendLine("  \"unknown_spectrum\": \"${result.unknownSpectrum.name}\",")
                    appendLine("  \"reference_compound\": \"${result.reference.compoundName}\",")
                    appendLine("  \"reference_cas\": \"${result.reference.casNumber ?: "N/A"}\",")
                    appendLine("  \"reference_formula\": \"${result.reference.chemicalFormula ?: "N/A"}\",")
                    appendLine("  \"reference_method\": \"${result.reference.measurementMethod}\",")
                    appendLine("  \"similarity_scores\": {")
                    result.allAlgorithmScores.forEach { (algo, score) ->
                        appendLine("    \"${algo.name}\": ${"%.2f".format(Locale.US, score)},")
                    }
                    appendLine("  },")
                    appendLine("  \"resampled_range\": \"${result.commonRangeMinWn.toInt()}-${result.commonRangeMaxWn.toInt()} cm⁻¹\",")
                    appendLine("  \"overlap_ratio\": ${"%.2f".format(Locale.US, result.rangeOverlapRatio)},")
                    appendLine("  \"matched_peaks_count\": ${result.matchedPeaksCount},")
                    appendLine("  \"total_unknown_peaks\": ${result.totalUnknownPeaksCount},")
                    appendLine("  \"total_reference_peaks\": ${result.totalReferencePeaksCount},")
                    appendLine("  \"difference_rms\": ${"%.3f".format(Locale.US, result.differenceSpectrum.rmsDifference)}")
                    appendLine("}")
                }

                Surface(
                    color = Color(0xFF0F1B2B),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = jsonPayload,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = Color(0xFF80D8FF),
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = { onCopy(jsonPayload) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Structured Evidence JSON", fontSize = 11.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: Individual Reference Item Card
// -------------------------------------------------------------
@Composable
private fun ReferenceCardItem(
    reference: ReferenceSpectrum,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDeleteCustom: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = reference.compoundName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = if (reference.isUserCustom) CyanLaser.copy(alpha = 0.18f) else EmeraldMatch.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = if (reference.isUserCustom) "CUSTOM" else "STANDARD",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (reference.isUserCustom) CyanLaser else EmeraldMatch,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = "Formula: ${reference.chemicalFormula ?: "N/A"} • CAS: ${reference.casNumber ?: "N/A"} • Class: ${reference.materialClass}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDeleteCustom != null) {
                        IconButton(
                            onClick = onDeleteCustom,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }

                    FilledTonalButton(
                        onClick = onSelect,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(if (isSelected) "Active" else "Compare", fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Method: ${reference.measurementMethod} • Source: ${reference.source}",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (reference.characteristicPeaks.isNotEmpty()) {
                Text(
                    text = "Key Peaks: ${reference.characteristicPeaks.take(5).joinToString(", ") { "${it.toInt()}" }} cm⁻¹",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
