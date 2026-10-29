package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Peak
import com.example.domain.model.PeakIntensity
import com.example.ui.theme.AmberLaser
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldMatch
import java.util.Locale

enum class PeakSortBy(val label: String) {
    WAVENUMBER_DESC("Wavenumber (4000 → 400)"),
    WAVENUMBER_ASC("Wavenumber (400 → 4000)"),
    PROMINENCE_DESC("Prominence (High → Low)"),
    SIGNAL_DESC("Signal (High → Low)"),
    FWHM_DESC("FWHM (Wide → Narrow)"),
    AREA_DESC("Area (Large → Small)")
}

enum class SpectralRegionFilter(val label: String, val minWn: Double, val maxWn: Double) {
    ALL("All Regions", 0.0, 10000.0),
    FINGERPRINT("Fingerprint (400–1500)", 400.0, 1500.0),
    FUNCTIONAL("Functional (1500–4000)", 1500.0, 4000.0),
    CH_STRETCH("C-H Region (2800–3100)", 2800.0, 3100.0),
    CARBONYL("Carbonyl C=O (1650–1850)", 1650.0, 1850.0)
}

@Composable
fun PeakListView(
    peaks: List<Peak>,
    signalUnit: String,
    selectedPeak: Peak? = null,
    onPeakSelected: (Peak) -> Unit,
    onAddManualPeak: ((Double) -> Unit)? = null,
    onDeletePeak: ((String) -> Unit)? = null,
    onUpdatePeakNote: ((peakId: String, note: String, assignment: String?) -> Unit)? = null,
    onSearchDictionary: ((Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var sortBy by remember { mutableStateOf(PeakSortBy.WAVENUMBER_DESC) }
    var selectedRegion by remember { mutableStateOf(SpectralRegionFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showSortMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var peakToEdit by remember { mutableStateOf<Peak?>(null) }

    // Filtering
    val filteredPeaks = peaks.filter { peak ->
        val inRegion = peak.wavenumber >= selectedRegion.minWn && peak.wavenumber <= selectedRegion.maxWn
        val matchesSearch = if (searchQuery.isBlank()) true else {
            val q = searchQuery.trim().lowercase(Locale.US)
            peak.wavenumber.toInt().toString().contains(q) ||
            "%.1f".format(Locale.US, peak.wavenumber).contains(q) ||
            (peak.matchedFunctionalGroup?.lowercase(Locale.US)?.contains(q) == true) ||
            (peak.notes?.lowercase(Locale.US)?.contains(q) == true)
        }
        inRegion && matchesSearch
    }

    // Sorting
    val sortedPeaks = when (sortBy) {
        PeakSortBy.WAVENUMBER_DESC -> filteredPeaks.sortedByDescending { it.wavenumber }
        PeakSortBy.WAVENUMBER_ASC -> filteredPeaks.sortedBy { it.wavenumber }
        PeakSortBy.PROMINENCE_DESC -> filteredPeaks.sortedByDescending { it.prominence }
        PeakSortBy.SIGNAL_DESC -> filteredPeaks.sortedByDescending { it.signalValue }
        PeakSortBy.FWHM_DESC -> filteredPeaks.sortedByDescending { it.fwhm }
        PeakSortBy.AREA_DESC -> filteredPeaks.sortedByDescending { it.area }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Controls Row: Search, Sort Dropdown, Add Manual Peak
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter peaks (e.g. 1715, C=O)", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("input_filter_peaks")
            )

            // Sort Menu Button
            Box {
                OutlinedButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.height(48.dp).testTag("button_sort_peaks")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sort", fontSize = 12.sp)
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    PeakSortBy.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option.label,
                                    fontWeight = if (sortBy == option) FontWeight.Bold else FontWeight.Normal,
                                    color = if (sortBy == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                sortBy = option
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            // Add Manual Peak Button
            if (onAddManualPeak != null) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.height(48.dp).testTag("button_add_manual_peak")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Add", fontSize = 12.sp)
                }
            }
        }

        // Region Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(SpectralRegionFilter.entries.toTypedArray()) { region ->
                FilterChip(
                    selected = selectedRegion == region,
                    onClick = { selectedRegion = region },
                    label = { Text(region.label, fontSize = 11.sp) }
                )
            }
        }

        // Selected Peak Highlight & Details Card
        selectedPeak?.let { sel ->
            SelectedPeakDetailCard(
                peak = sel,
                signalUnit = signalUnit,
                onEdit = { peakToEdit = sel },
                onDelete = if (onDeletePeak != null) { { onDeletePeak(sel.id) } } else null,
                onSearchDictionary = onSearchDictionary?.let { search -> { search(sel.wavenumber) } }
            )
        }

        if (peaks.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("No peaks detected in current spectrum", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Adjust prominence threshold above or tap '+ Add' to select a peak manually.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            return
        }

        // Table Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                Text("cm⁻¹", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(68.dp))
                Text("Signal", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(46.dp))
                Text("Prom", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
                Text("FWHM", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
                Text("Assignment", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
        }

        // Table Rows
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            sortedPeaks.forEach { peak ->
                val isSelected = selectedPeak?.id == peak.id
                PeakTableRowItem(
                    peak = peak,
                    signalUnit = signalUnit,
                    isSelected = isSelected,
                    onClick = { onPeakSelected(peak) }
                )
            }
        }
    }

    // Add Manual Peak Dialog
    if (showAddDialog && onAddManualPeak != null) {
        var inputWn by remember { mutableStateOf("") }
        var inputErr by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Peak Manually", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the wavenumber (cm⁻¹) where a peak is located. The scientific engine will numerically evaluate its prominence, FWHM, and area from surrounding data points.", fontSize = 12.sp)
                    OutlinedTextField(
                        value = inputWn,
                        onValueChange = {
                            inputWn = it
                            inputErr = null
                        },
                        label = { Text("Wavenumber (cm⁻¹)") },
                        placeholder = { Text("e.g. 1715.0") },
                        isError = inputErr != null,
                        supportingText = inputErr?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_manual_peak_wn")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = inputWn.trim().replace(',', '.').toDoubleOrNull()
                        if (parsed != null && parsed in 50.0..10000.0) {
                            onAddManualPeak(parsed)
                            showAddDialog = false
                        } else {
                            inputErr = "Please enter a valid wavenumber between 50 and 10000 cm⁻¹."
                        }
                    },
                    modifier = Modifier.testTag("button_confirm_add_peak")
                ) {
                    Text("Add Peak")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Peak Note / Assignment Dialog
    peakToEdit?.let { peak ->
        var noteText by remember { mutableStateOf(peak.notes ?: "") }
        var assignmentText by remember { mutableStateOf(peak.matchedFunctionalGroup ?: "") }

        AlertDialog(
            onDismissRequest = { peakToEdit = null },
            title = { Text("Peak Assignment & Notes (${"%.1f".format(Locale.US, peak.wavenumber)} cm⁻¹)", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = assignmentText,
                        onValueChange = { assignmentText = it },
                        label = { Text("IR Assignment / Functional Group") },
                        placeholder = { Text("e.g. C=O stretch (Ester)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Scientific Notes") },
                        placeholder = { Text("e.g. Strong sharp band, diagnostic for ketone") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdatePeakNote?.invoke(
                        peak.id,
                        noteText.trim(),
                        assignmentText.trim().ifEmpty { null }
                    )
                    peakToEdit = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { peakToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PeakTableRowItem(
    peak: Peak,
    signalUnit: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("peak_row_${peak.index}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AmberLaser.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(6.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, AmberLaser) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index & manual indicator
            Text(
                text = "${peak.index}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (peak.isManual) AmberLaser else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(22.dp)
            )

            // Wavenumber
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(68.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when (peak.intensity) {
                                PeakIntensity.VERY_STRONG -> Color(0xFFEF476F)
                                PeakIntensity.STRONG -> AmberLaser
                                PeakIntensity.MEDIUM -> EmeraldMatch
                                else -> CyanLaser
                            }
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "%.1f".format(Locale.US, peak.wavenumber),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Signal
            Text(
                text = "%.1f".format(Locale.US, peak.signalValue),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.width(46.dp)
            )

            // Prominence
            Text(
                text = "%.1f".format(Locale.US, peak.prominence),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(44.dp)
            )

            // FWHM
            Text(
                text = if (peak.fwhm > 0) "%.1f".format(Locale.US, peak.fwhm) else "—",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(44.dp)
            )

            // Assignment
            Box(modifier = Modifier.weight(1f)) {
                if (peak.matchedFunctionalGroup != null) {
                    Surface(
                        color = EmeraldMatch.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = peak.matchedFunctionalGroup,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (MaterialTheme.colorScheme.background.red < 0.5f) EmeraldMatch else Color(0xFF00796B),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                } else {
                    Text(
                        text = if (peak.isManual) "Manual" else "—",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedPeakDetailCard(
    peak: Peak,
    signalUnit: String,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?,
    onSearchDictionary: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Peak #${peak.index}: ${"%.1f".format(Locale.US, peak.wavenumber)} cm⁻¹",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (peak.isManual) {
                        Surface(
                            color = AmberLaser.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text("MANUAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AmberLaser, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onSearchDictionary != null) {
                        OutlinedButton(
                            onClick = onSearchDictionary,
                            modifier = Modifier.height(28.dp).testTag("button_peak_search_dictionary"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Search IR Dictionary", fontSize = 10.sp)
                        }
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Assignment", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Peak", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Metric pills grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricChip("Signal", "${"%.2f".format(Locale.US, peak.signalValue)} $signalUnit")
                MetricChip("Prominence", "Δ ${"%.2f".format(Locale.US, peak.prominence)}")
                MetricChip("FWHM", if (peak.fwhm > 0) "${"%.1f".format(Locale.US, peak.fwhm)} cm⁻¹" else "—")
                MetricChip("Area", if (peak.area > 0) "%.1f".format(Locale.US, peak.area) else "—")
                MetricChip("Rel. Int.", "${"%.0f".format(Locale.US, peak.relativeIntensity)}%")
            }

            if (peak.matchedFunctionalGroup != null || peak.notes != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Assignment: ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(peak.matchedFunctionalGroup ?: "Unassigned", fontSize = 11.sp, color = EmeraldMatch, fontWeight = FontWeight.Medium)
                    if (peak.notes != null) {
                        Text(" • Note: ${peak.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
