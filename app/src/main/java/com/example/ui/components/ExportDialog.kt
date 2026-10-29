package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.spectroscopy.LaboratoryAnalysisReportData
import com.example.domain.spectroscopy.LaboratoryReportService
import com.example.ui.theme.AmberLaser
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldMatch
import com.example.util.ExportFormat
import com.example.util.ExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ExportDialog(
    data: LaboratoryAnalysisReportData,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var showPeakAnnotations by remember { mutableStateOf(true) }
    var publicationWhite by remember { mutableStateOf(false) }

    var isExporting by remember { mutableStateOf(false) }
    var lastExportedFile by remember { mutableStateOf<File?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .testTag("dialog_export_analysis"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Export FTIR Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${data.effectiveSampleName} • ${data.spectrum.spectrumType.label}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp).testTag("button_export_close")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider()

                // Scientific Transparency Classification Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Science,
                            contentDescription = null,
                            tint = CyanLaser,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Laboratory Scientific Transparency Standards",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Reports distinctly separate: Measured data, Preprocessing trail, Deterministic IR rules, and AI-assisted hypotheses.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Format Selector Buttons
                Text("Select Laboratory Export Format", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormat.entries.forEach { format ->
                        val isSelected = format == selectedFormat
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedFormat = format
                                    lastExportedFile = null
                                    statusMessage = null
                                }
                                .testTag("chip_format_${format.name.lowercase()}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else null
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    when (format) {
                                        ExportFormat.PDF -> Icons.Default.Description
                                        ExportFormat.CSV -> Icons.Default.TableChart
                                        ExportFormat.JSON -> Icons.Default.Science
                                        ExportFormat.IMAGE -> Icons.Default.Image
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = format.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = ".${format.extension}",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Format Details & Options Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (selectedFormat) {
                            ExportFormat.PDF -> {
                                Text("PDF Report Structure", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = "• Page 1: Sample metadata, embedded vector spectrum plot (${data.spectrum.minWavenumber.toInt()}–${data.spectrum.maxWavenumber.toInt()} cm⁻¹), and complete preprocessing trail.",
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "• Page 2: Quantitative peak detection table (${data.detectedPeaks.size} peaks), IR correlation dictionary assignments, reference library comparisons, AI-assisted interpretation, and scientific uncertainty limitations with signature sign-off.",
                                    fontSize = 10.sp
                                )
                                if (data.aiResult != null && data.aiResult.isSuccess) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldMatch, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Includes AI-assisted hypothesis section (${data.aiResult.modelName})", fontSize = 10.sp, color = EmeraldMatch)
                                    }
                                }
                            }

                            ExportFormat.CSV -> {
                                Text("CSV Numerical Export", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = "• Columns: Wavenumber (cm-1), Original Signal, Processed Signal, Is Peak, Peak #, Prominence, FWHM, Area, Intensity, Functional Group Assignment, Analyst Notes.",
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "• Complete metadata header comments compliant with standard scientific spreadsheet and chromatography data systems.",
                                    fontSize = 10.sp
                                )
                            }

                            ExportFormat.JSON -> {
                                Text("JSON Analysis Architecture", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = "• Complete hierarchical representation including spectral properties, full preprocessing history, detected peaks with physical parameters, dictionary assignments, reference comparison matrix, and full AI interpretation payload.",
                                    fontSize = 10.sp
                                )
                            }

                            ExportFormat.IMAGE -> {
                                Text("High-Resolution Spectrum Graphic (1920×1080)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Peak Annotations (cm⁻¹ Callouts)", fontSize = 11.sp)
                                    Switch(
                                        checked = showPeakAnnotations,
                                        onCheckedChange = { showPeakAnnotations = it },
                                        modifier = Modifier.testTag("switch_peak_annotations")
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Publication White Theme (for papers)", fontSize = 11.sp)
                                    Switch(
                                        checked = publicationWhite,
                                        onCheckedChange = { publicationWhite = it },
                                        modifier = Modifier.testTag("switch_publication_white")
                                    )
                                }
                            }
                        }
                    }
                }

                // Status / Result message
                statusMessage?.let { msg ->
                    Surface(
                        color = if (msg.startsWith("Exported")) EmeraldMatch.copy(alpha = 0.15f) else AmberLaser.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (msg.startsWith("Exported")) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (msg.startsWith("Exported")) EmeraldMatch else AmberLaser,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (msg.startsWith("Exported")) EmeraldMatch else AmberLaser
                            )
                        }
                    }
                }

                if (isExporting) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Generating ${selectedFormat.label}...", fontSize = 12.sp)
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Share via Android System
                    Button(
                        onClick = {
                            scope.launch {
                                isExporting = true
                                val result = withContext(Dispatchers.IO) {
                                    ExportManager.exportReport(
                                        context = context,
                                        data = data,
                                        format = selectedFormat,
                                        showPeakAnnotations = showPeakAnnotations,
                                        publicationWhite = publicationWhite
                                    )
                                }
                                isExporting = false
                                if (result.success && result.file != null) {
                                    lastExportedFile = result.file
                                    statusMessage = result.message
                                    ExportManager.shareExportedFile(
                                        context = context,
                                        file = result.file,
                                        format = selectedFormat,
                                        sampleName = data.effectiveSampleName
                                    )
                                } else {
                                    statusMessage = result.message
                                }
                            }
                        },
                        enabled = !isExporting,
                        modifier = Modifier.weight(1f).testTag("button_export_share")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 12.sp)
                    }

                    // Save to Device / Cache
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isExporting = true
                                val result = withContext(Dispatchers.IO) {
                                    ExportManager.exportReport(
                                        context = context,
                                        data = data,
                                        format = selectedFormat,
                                        showPeakAnnotations = showPeakAnnotations,
                                        publicationWhite = publicationWhite
                                    )
                                }
                                isExporting = false
                                if (result.success && result.file != null) {
                                    lastExportedFile = result.file
                                    statusMessage = "Saved: ${result.file.name} (${result.file.length() / 1024} KB)"
                                    Toast.makeText(context, "Saved to ${result.file.name}", Toast.LENGTH_SHORT).show()
                                } else {
                                    statusMessage = result.message
                                }
                            }
                        },
                        enabled = !isExporting,
                        modifier = Modifier.weight(1f).testTag("button_export_save")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save File", fontSize = 12.sp)
                    }

                    // Copy text (for CSV / JSON)
                    if (selectedFormat == ExportFormat.CSV || selectedFormat == ExportFormat.JSON) {
                        OutlinedButton(
                            onClick = {
                                val content = if (selectedFormat == ExportFormat.CSV) {
                                    LaboratoryReportService.generateCsv(data)
                                } else {
                                    LaboratoryReportService.generateJson(data)
                                }
                                ExportManager.copyToClipboard(context, "${selectedFormat.name} Data", content)
                            },
                            modifier = Modifier.testTag("button_export_copy")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
