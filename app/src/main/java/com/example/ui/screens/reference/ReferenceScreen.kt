package com.example.ui.screens.reference

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.domain.model.ReferenceSpectrum
import com.example.domain.model.Spectrum
import com.example.domain.model.SpectrumType
import com.example.domain.repository.ReferenceSpectrumRepositoryImpl
import com.example.domain.spectroscopy.StandardPeakDetector
import com.example.domain.spectroscopy.StandardSpectrumImporter
import com.example.ui.navigation.Screen
import com.example.ui.theme.AmberLaser
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun ReferenceScreen(
    onLoadSpectrum: (Spectrum) -> Unit,
    onCompareReference: ((ReferenceSpectrum) -> Unit)? = null,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val repository = remember { ReferenceSpectrumRepositoryImpl(db.customReferenceSpectrumDao()) }
    val importer = remember { StandardSpectrumImporter() }
    val peakDetector = remember { StandardPeakDetector() }

    val referencesFlow = remember { repository.getAllReferencesFlow() }
    val allReferences by referencesFlow.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedMaterialClass by remember { mutableStateOf<String?>(null) }
    var selectedSpectralRegion by remember { mutableStateOf<String?>(null) }

    // Custom import dialog state
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingFileUri by remember { mutableStateOf<Uri?>(null) }
    var pendingFileName by remember { mutableStateOf("") }
    var customNameInput by remember { mutableStateOf("") }
    var customCasInput by remember { mutableStateOf("") }
    var customFormulaInput by remember { mutableStateOf("") }
    var customClassInput by remember { mutableStateOf("Organic Chemical") }
    var customMethodInput by remember { mutableStateOf("ATR-FTIR (Diamond Crystal)") }
    var customNotesInput by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingFileUri = it
            pendingFileName = com.example.util.FileUtils.getFileNameFromUri(context, it)
            customNameInput = pendingFileName.substringBeforeLast(".")
            showImportDialog = true
        }
    }

    // Material classes list
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

    val spectralRegions = listOf(
        "All Regions",
        "Carbonyl (1800–1650 cm⁻¹)",
        "O-H / N-H (3600–3200 cm⁻¹)",
        "C-H Stretch (3100–2800 cm⁻¹)",
        "Fingerprint (<1500 cm⁻¹)"
    )

    // Filter reference spectra based on query, material class, and spectral region
    val filteredReferences = remember(allReferences, searchQuery, selectedMaterialClass, selectedSpectralRegion) {
        val q = searchQuery.trim().lowercase(Locale.US)
        allReferences.filter { ref ->
            val matchesClass = when (selectedMaterialClass) {
                null, "All Classes" -> true
                "Custom" -> ref.isUserCustom
                else -> ref.materialClass.contains(selectedMaterialClass!!, ignoreCase = true)
            }

            val matchesRegion = when (selectedSpectralRegion) {
                null, "All Regions" -> true
                "Carbonyl (1800–1650 cm⁻¹)" -> ref.characteristicPeaks.any { it in 1650.0..1820.0 }
                "O-H / N-H (3600–3200 cm⁻¹)" -> ref.characteristicPeaks.any { it in 3150.0..3650.0 }
                "C-H Stretch (3100–2800 cm⁻¹)" -> ref.characteristicPeaks.any { it in 2800.0..3150.0 }
                "Fingerprint (<1500 cm⁻¹)" -> ref.characteristicPeaks.any { it < 1500.0 }
                else -> true
            }

            val matchesSearch = if (q.isBlank()) {
                true
            } else {
                ref.compoundName.lowercase(Locale.US).contains(q) ||
                (ref.chemicalFormula?.lowercase(Locale.US)?.contains(q) == true) ||
                (ref.casNumber?.lowercase(Locale.US)?.contains(q) == true) ||
                ref.materialClass.lowercase(Locale.US).contains(q) ||
                ref.functionalGroups.any { it.lowercase(Locale.US).contains(q) } ||
                ref.spectralRegions.any { it.lowercase(Locale.US).contains(q) } ||
                ref.measurementMethod.lowercase(Locale.US).contains(q) ||
                ref.description.lowercase(Locale.US).contains(q)
            }

            matchesClass && matchesRegion && matchesSearch
        }
    }

    // Custom Reference Import Dialog
    if (showImportDialog && pendingFileUri != null) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = CyanLaser)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Custom Reference Spectrum", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("File: $pendingFileName", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = customNameInput,
                        onValueChange = { customNameInput = it },
                        label = { Text("Compound Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = customFormulaInput,
                            onValueChange = { customFormulaInput = it },
                            label = { Text("Formula (e.g. C8H8)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customCasInput,
                            onValueChange = { customCasInput = it },
                            label = { Text("CAS Number") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = customClassInput,
                        onValueChange = { customClassInput = it },
                        label = { Text("Material Class") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customMethodInput,
                        onValueChange = { customMethodInput = it },
                        label = { Text("Measurement Method") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customNotesInput,
                        onValueChange = { customNotesInput = it },
                        label = { Text("Sample Notes / Source") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingFileUri ?: return@Button
                        val name = customNameInput.trim().ifBlank { pendingFileName }
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val stream = context.contentResolver.openInputStream(uri)
                                if (stream != null) {
                                    val parseRes = importer.parseStream(stream, pendingFileName)
                                    parseRes.onSuccess { importedSpectrum ->
                                        val detectedPeaks = peakDetector.detectPeaks(importedSpectrum)
                                        val finalSpectrum = importedSpectrum.copy(
                                            name = "$name (Reference)",
                                            detectedPeaks = detectedPeaks
                                        )

                                        val newRef = ReferenceSpectrum(
                                            compoundName = name,
                                            chemicalFormula = customFormulaInput.trim().ifBlank { null },
                                            casNumber = customCasInput.trim().ifBlank { null },
                                            materialClass = customClassInput.trim().ifBlank { "General Organic" },
                                            measurementMethod = customMethodInput.trim().ifBlank { "ATR-FTIR" },
                                            source = "User Imported Standard ($pendingFileName)",
                                            sampleInformation = "Imported numerical file: $pendingFileName",
                                            notes = customNotesInput.trim(),
                                            characteristicPeaks = detectedPeaks.map { it.wavenumber },
                                            spectrum = finalSpectrum,
                                            isDemo = false,
                                            isUserCustom = true
                                        )

                                        repository.addCustomReference(newRef)

                                        withContext(Dispatchers.Main) {
                                            showImportDialog = false
                                            Toast.makeText(context, "Added '$name' to Reference Library", Toast.LENGTH_SHORT).show()
                                        }
                                    }.onFailure { err ->
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Parse error: ${err.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("Save to Library")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_import_reference")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import Reference Spectrum")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar: supports compound name, formula, CAS, functional group, material class, region
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, formula, CAS, functional group…") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = CyanLaser)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_reference_search"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Filter Bar 1: Material Classes
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(materialClasses) { matClass ->
                    val isSelected = (selectedMaterialClass == null && matClass == "All Classes") || selectedMaterialClass == matClass
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedMaterialClass = if (matClass == "All Classes") null else matClass
                        },
                        label = { Text(matClass, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Filter Bar 2: Spectral Regions
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(spectralRegions) { region ->
                    val isSelected = (selectedSpectralRegion == null && region == "All Regions") || selectedSpectralRegion == region
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedSpectralRegion = if (region == "All Regions") null else region
                        },
                        label = { Text(region, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Scientific distinction caveat banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Scientific principle: Spectral similarity reflects curve correlation, not proof of chemical identity. Always corroborate with physical and chromatographic data.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reference spectrum items list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 72.dp)
            ) {
                if (filteredReferences.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No reference spectra match your search criteria.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(filteredReferences, key = { it.id }) { ref ->
                    ReferenceCompoundCard(
                        reference = ref,
                        onCompare = onCompareReference?.let { cmp -> { cmp(ref) } },
                        onLoad = {
                            onLoadSpectrum(ref.spectrum)
                            onNavigate(Screen.ANALYZE)
                        },
                        onDelete = if (ref.isUserCustom) {
                            {
                                coroutineScope.launch {
                                    repository.deleteCustomReference(ref.id)
                                    Toast.makeText(context, "Deleted '${ref.compoundName}'", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferenceCompoundCard(
    reference: ReferenceSpectrum,
    onCompare: (() -> Unit)?,
    onLoad: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ref_card_${reference.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Header Row: Compound Name, Formula, Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reference.compoundName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        reference.chemicalFormula?.let { f ->
                            Surface(
                                color = CyanLaser.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = f,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanLaser,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        reference.casNumber?.let { cas ->
                            Text(
                                text = "CAS: $cas",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "• ${reference.materialClass}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    color = if (reference.isUserCustom) CyanLaser.copy(alpha = 0.15f) else EmeraldMatch.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (reference.isUserCustom) "USER CUSTOM" else "STANDARD REF",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (reference.isUserCustom) CyanLaser else EmeraldMatch,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Method and Source
            Text(
                text = "Method: ${reference.measurementMethod} • Source: ${reference.source}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Diagnostic peaks strip
            if (reference.characteristicPeaks.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Diagnostic Peaks: ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = reference.characteristicPeaks.take(6).joinToString(" • ") { "${it.toInt()}" } + " cm⁻¹",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Description / notes
            if (reference.notes.isNotBlank()) {
                Text(
                    text = reference.notes,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onCompare != null) {
                    Button(
                        onClick = onCompare,
                        modifier = Modifier.weight(1.2f).height(32.dp).testTag("button_compare_${reference.id}"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compare Unknown", fontSize = 11.sp)
                    }
                }

                OutlinedButton(
                    onClick = onLoad,
                    modifier = Modifier.weight(1f).height(32.dp).testTag("button_load_${reference.id}"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load Active", fontSize = 11.sp)
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("button_delete_${reference.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Custom Reference", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
