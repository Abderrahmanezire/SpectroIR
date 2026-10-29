package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.domain.model.SpectrumType
import com.example.domain.spectroscopy.ColumnMapping
import com.example.domain.spectroscopy.ImportAnalysisResult
import com.example.domain.spectroscopy.ImportXUnit
import com.example.domain.spectroscopy.ParsedTable
import com.example.ui.theme.AmberLaser
import com.example.ui.theme.CyanLaser

@Composable
fun ColumnMappingDialog(
    analysisResult: ImportAnalysisResult,
    onConfirm: (ColumnMapping) -> Unit,
    onDismiss: () -> Unit
) {
    val table = analysisResult.table
    var selectedXCol by remember { mutableIntStateOf(analysisResult.suggestedMapping.xColumnIndex) }
    var selectedYCol by remember { mutableIntStateOf(analysisResult.suggestedMapping.yColumnIndex) }
    var selectedXUnit by remember { mutableStateOf(analysisResult.suggestedMapping.xUnit) }
    var selectedType by remember { mutableStateOf(analysisResult.suggestedMapping.spectrumType) }
    var selectedDecimalSep by remember { mutableStateOf(analysisResult.suggestedMapping.decimalSeparator) }

    var xDropdownExpanded by remember { mutableStateOf(false) }
    var yDropdownExpanded by remember { mutableStateOf(false) }
    var xUnitDropdownExpanded by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var decDropdownExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Configure Spectrum Import",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = table.fileName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Ambiguity or guidance warning banner
                if (analysisResult.ambiguityReason != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AmberLaser.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AmberLaser, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = analysisResult.ambiguityReason,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Table Raw Preview (First 3-4 rows)
                Text("File Preview (First rows):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val hScroll = rememberScrollState()
                    Column(modifier = Modifier.padding(8.dp).horizontalScroll(hScroll)) {
                        // Header
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            table.headers.forEachIndexed { i, h ->
                                Text(
                                    text = "Col ${i + 1}: $h",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (i == selectedXCol) CyanLaser else if (i == selectedYCol) AmberLaser else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        table.rows.take(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                table.headers.indices.forEach { colIdx ->
                                    val cell = row.getOrNull(colIdx) ?: ""
                                    Text(
                                        text = cell.take(12),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // X Axis Column Selection
                Column {
                    Text("X Axis (Wavenumber / Wavelength):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { xDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("select_x_col")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Col ${selectedXCol + 1}: ${table.headers.getOrElse(selectedXCol) { "" }}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = xDropdownExpanded,
                            onDismissRequest = { xDropdownExpanded = false }
                        ) {
                            table.headers.forEachIndexed { idx, name ->
                                DropdownMenuItem(
                                    text = { Text("Col ${idx + 1}: $name") },
                                    onClick = {
                                        selectedXCol = idx
                                        xDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // X Unit Selection (with wavelength conversion support)
                Column {
                    Text("X Unit & Conversion:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { xUnitDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("select_x_unit")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedXUnit.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = xUnitDropdownExpanded,
                            onDismissRequest = { xUnitDropdownExpanded = false }
                        ) {
                            ImportXUnit.entries.forEach { unit ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(unit.displayName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            if (unit != ImportXUnit.WAVENUMBER_CM_1) {
                                                Text("Converts to cm⁻¹ via ν = 1/λ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedXUnit = unit
                                        xUnitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Y Axis Column Selection
                Column {
                    Text("Y Axis (Spectroscopic Signal):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { yDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("select_y_col")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Col ${selectedYCol + 1}: ${table.headers.getOrElse(selectedYCol) { "" }}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = yDropdownExpanded,
                            onDismissRequest = { yDropdownExpanded = false }
                        ) {
                            table.headers.forEachIndexed { idx, name ->
                                DropdownMenuItem(
                                    text = { Text("Col ${idx + 1}: $name") },
                                    onClick = {
                                        selectedYCol = idx
                                        yDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Spectrum Type Selection
                Column {
                    Text("Spectrum Type:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { typeDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("select_spec_type")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedType.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false }
                        ) {
                            SpectrumType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.label) },
                                    onClick = {
                                        selectedType = type
                                        typeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Decimal Separator
                Column {
                    Text("Decimal Separator:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { decDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedDecimalSep == '.') "Dot: 1234.56 (Standard)" else "Comma: 1234,56 (European)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = decDropdownExpanded,
                            onDismissRequest = { decDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Dot: 1234.56 (Standard)") },
                                onClick = {
                                    selectedDecimalSep = '.'
                                    decDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Comma: 1234,56 (European)") },
                                onClick = {
                                    selectedDecimalSep = ','
                                    decDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mapping = ColumnMapping(
                        xColumnIndex = selectedXCol,
                        yColumnIndex = selectedYCol,
                        xUnit = selectedXUnit,
                        spectrumType = selectedType,
                        decimalSeparator = selectedDecimalSep
                    )
                    onConfirm(mapping)
                },
                modifier = Modifier.testTag("button_confirm_mapping")
            ) {
                Text("Confirm & Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
