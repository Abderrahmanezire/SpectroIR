package com.example.ui.screens.dictionary

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.MenuBook
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.data.local.AppDatabase
import com.example.data.local.CustomBondAssignmentEntity
import com.example.domain.model.BondAssignment
import com.example.domain.model.PeakIntensity
import com.example.domain.model.SpectralRegion
import com.example.domain.model.VibrationType
import com.example.domain.spectroscopy.SpectroscopyDatabase
import com.example.ui.theme.AmberLaser
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

@Composable
fun DictionaryScreen(
    initialSearchQuery: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val customEntitiesFlow = remember { db.customBondAssignmentDao().getAllCustomAssignmentsFlow() }
    val customEntities by customEntitiesFlow.collectAsState(initial = emptyList())

    val customAssignments = remember(customEntities) {
        customEntities.map { it.toDomain() }
    }

    val allAssignments = remember(customAssignments) {
        SpectroscopyDatabase.STANDARD_ASSIGNMENTS + customAssignments
    }

    var searchQuery by remember { mutableStateOf(initialSearchQuery) }
    var selectedBondFilter by remember { mutableStateOf<String?>(null) }
    var selectedRegionFilter by remember { mutableStateOf<SpectralRegion?>(null) }
    var selectedVibrationFilter by remember { mutableStateOf<VibrationType?>(null) }
    var showCustomEntryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialSearchQuery) {
        if (initialSearchQuery.isNotBlank()) {
            searchQuery = initialSearchQuery
        }
    }

    val bondsList = remember(allAssignments) {
        listOf("All Bonds") + allAssignments.map { it.bond }.distinct().sorted()
    }

    val filteredAssignments = remember(searchQuery, selectedBondFilter, selectedRegionFilter, selectedVibrationFilter, allAssignments) {
        val queryNum = searchQuery.trim().toDoubleOrNull()
        allAssignments.filter { a ->
            val matchesBond = selectedBondFilter == null || selectedBondFilter == "All Bonds" || a.bond == selectedBondFilter
            val matchesRegion = selectedRegionFilter == null || a.spectralRegion == selectedRegionFilter
            val matchesVibration = selectedVibrationFilter == null || a.vibrationType == selectedVibrationFilter

            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else if (queryNum != null) {
                // Wavenumber search: returns compatible assignments where wavenumber is inside range +/- 25 cm⁻¹
                a.containsWavenumber(queryNum, tolerance = 25.0)
            } else {
                val q = searchQuery.trim().lowercase(Locale.US)
                a.bond.lowercase(Locale.US).contains(q) ||
                a.functionalGroup.lowercase(Locale.US).contains(q) ||
                a.vibrationalMode.lowercase(Locale.US).contains(q) ||
                a.notes.lowercase(Locale.US).contains(q) ||
                a.possibleCompoundClasses.any { it.lowercase(Locale.US).contains(q) } ||
                (a.userNotes?.lowercase(Locale.US)?.contains(q) == true)
            }

            matchesBond && matchesRegion && matchesVibration && matchesSearch
        }.sortedWith(
            if (queryNum != null) {
                compareBy { kotlin.math.abs(it.typicalWavenumber - queryNum) }
            } else {
                compareByDescending { it.typicalWavenumber }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCustomEntryDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_custom_assignment")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Custom Assignment")
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

            // Search Header (Supports both bond text and exact wavenumber cm⁻¹)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by wavenumber (e.g. 1715) or bond, group…") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
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
                    .testTag("input_dictionary_search"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Bar 1: Bonds
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(bondsList) { bond ->
                    val isSelected = (selectedBondFilter == null && bond == "All Bonds") || selectedBondFilter == bond
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedBondFilter = if (bond == "All Bonds") null else bond
                        },
                        label = { Text(bond, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Filter Bar 2: Spectral Regions
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedRegionFilter == null,
                        onClick = { selectedRegionFilter = null },
                        label = { Text("All Regions", fontSize = 11.sp) }
                    )
                }
                items(SpectralRegion.entries.toTypedArray()) { region ->
                    val isSelected = selectedRegionFilter == region
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedRegionFilter = if (isSelected) null else region
                        },
                        label = { Text(region.label, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Results count & active search summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val queryNum = searchQuery.trim().toDoubleOrNull()
                Text(
                    text = if (queryNum != null) {
                        "Wavenumber search: ${queryNum.toInt()} cm⁻¹ (${filteredAssignments.size} matches)"
                    } else {
                        "Showing ${filteredAssignments.size} scientific assignments (${customAssignments.size} custom)"
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (selectedBondFilter != null || selectedRegionFilter != null || searchQuery.isNotBlank()) {
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            selectedBondFilter = null
                            selectedRegionFilter = null
                            selectedVibrationFilter = null
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Reset Filters", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Assignments List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredAssignments, key = { it.id }) { assignment ->
                    BondAssignmentDetailCard(
                        assignment = assignment,
                        highlightWavenumber = searchQuery.trim().toDoubleOrNull(),
                        onDeleteCustom = if (assignment.isCustom) {
                            {
                                coroutineScope.launch(Dispatchers.IO) {
                                    db.customBondAssignmentDao().deleteById(assignment.id)
                                }
                                Toast.makeText(context, "Custom entry removed", Toast.LENGTH_SHORT).show()
                            }
                        } else null
                    )
                }
            }
        }
    }

    if (showCustomEntryDialog) {
        CustomAssignmentDialog(
            onDismiss = { showCustomEntryDialog = false },
            onSave = { newAssignment ->
                coroutineScope.launch(Dispatchers.IO) {
                    db.customBondAssignmentDao().insertOrUpdate(
                        CustomBondAssignmentEntity.fromDomain(newAssignment)
                    )
                }
                showCustomEntryDialog = false
                Toast.makeText(context, "Custom assignment added to knowledge base", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun BondAssignmentDetailCard(
    assignment: BondAssignment,
    highlightWavenumber: Double? = null,
    onDeleteCustom: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("assignment_card_${assignment.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Bond badge, functional group, range pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = CyanLaser.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = assignment.bond,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = assignment.functionalGroup,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = assignment.vibrationalMode,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (highlightWavenumber != null && assignment.containsWavenumber(highlightWavenumber)) {
                        EmeraldMatch.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${assignment.minWavenumber.toInt()}–${assignment.maxWavenumber.toInt()} cm⁻¹",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (highlightWavenumber != null && assignment.containsWavenumber(highlightWavenumber)) {
                            EmeraldMatch
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Metrics row: Typical cm⁻¹, Intensity, Shape
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Typical: ${assignment.typicalWavenumber.toInt()} cm⁻¹",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldMatch
                )
                Text(
                    text = "Intensity: ${assignment.intensity.label}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AmberLaser
                )
                Text(
                    text = "Shape: ${assignment.bandShape}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Visual ASCII Diagram
            if (assignment.visualDiagramAscii != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = assignment.visualDiagramAscii,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Possible compound classes
            if (assignment.possibleCompoundClasses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Possible Compound Classes: ${assignment.possibleCompoundClasses.joinToString(", ")}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Supporting partner peaks
            if (assignment.supportingPeaks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = CyanLaser.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Supporting / Companion Peaks:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanLaser
                        )
                        assignment.supportingPeaks.forEach { sp ->
                            Text(
                                text = "• ${sp.description} in ${sp.expectedRange} (${sp.diagnosticRole})",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Notes / Environmental dependencies
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Notes: ${assignment.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }

            if (assignment.isCustom && onDeleteCustom != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDeleteCustom) {
                        Text("Delete Custom Entry", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomAssignmentDialog(
    onDismiss: () -> Unit,
    onSave: (BondAssignment) -> Unit
) {
    var bond by remember { mutableStateOf("") }
    var functionalGroup by remember { mutableStateOf("") }
    var minWn by remember { mutableStateOf("") }
    var maxWn by remember { mutableStateOf("") }
    var typicalWn by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var compoundClasses by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom IR Assignment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = bond,
                    onValueChange = { bond = it },
                    label = { Text("Bond (e.g. C=O, B-H)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = functionalGroup,
                    onValueChange = { functionalGroup = it },
                    label = { Text("Functional Group / Mode") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = minWn,
                        onValueChange = { minWn = it },
                        label = { Text("Min cm⁻¹") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxWn,
                        onValueChange = { maxWn = it },
                        label = { Text("Max cm⁻¹") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = typicalWn,
                        onValueChange = { typicalWn = it },
                        label = { Text("Typical cm⁻¹") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = compoundClasses,
                    onValueChange = { compoundClasses = it },
                    label = { Text("Compound Classes (comma-separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Scientific Notes / Environment context") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minV = minWn.toDoubleOrNull() ?: 1000.0
                    val maxV = maxWn.toDoubleOrNull() ?: 1100.0
                    val typV = typicalWn.toDoubleOrNull() ?: ((minV + maxV) / 2.0)
                    val classes = compoundClasses.split(",").map { it.trim() }.filter { it.isNotBlank() }

                    val assignment = BondAssignment(
                        id = UUID.randomUUID().toString(),
                        bond = bond.ifBlank { "Custom" },
                        functionalGroup = functionalGroup.ifBlank { "Custom Vibration" },
                        vibrationType = VibrationType.STRETCH,
                        vibrationalMode = functionalGroup.ifBlank { "Custom Mode" },
                        minWavenumber = minV,
                        maxWavenumber = maxV,
                        typicalWavenumber = typV,
                        spectralRegion = SpectralRegion.fromWavenumber(typV),
                        intensity = PeakIntensity.MEDIUM,
                        bandShape = "Sharp",
                        possibleCompoundClasses = classes,
                        notes = notes.ifBlank { "User custom assignment." },
                        isCustom = true
                    )
                    onSave(assignment)
                },
                enabled = bond.isNotBlank() && functionalGroup.isNotBlank() && minWn.isNotBlank() && maxWn.isNotBlank()
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
