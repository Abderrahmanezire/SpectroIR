package com.example.ui.screens.analyze

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.spectroscopy.AIConfidenceEstimate
import com.example.domain.spectroscopy.AIConfiguration
import com.example.domain.spectroscopy.AIInterpretationResult
import com.example.domain.spectroscopy.AIMajorPeakAssignment
import com.example.domain.spectroscopy.AISpectrumSummary
import com.example.ui.theme.AmberLaser
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldMatch
import java.util.Locale

/**
 * AI-assisted FTIR spectroscopy interpretation interface.
 * Strictly adheres to the core scientific principle:
 * The AI is an interpreter and scientific assistant, NOT the measurement instrument.
 * Local numerical analysis is performed first, and data is presented in three distinct layers:
 * 1. Measured Data (Immutable experimental data)
 * 2. Database Assignments (Rule-based on-device correlations)
 * 3. AI Interpretation (Scientifically cautious physical organic chemistry synthesis)
 */
@Composable
fun AiAnalysisTabContent(
    uiState: AnalyzeUiState,
    onRunAi: () -> Unit,
    onTestConnection: () -> Unit,
    onSetApiKey: (String?) -> Unit,
    onUpdateUserContext: (sampleName: String?, sampleType: String?, expectedComp: String?, knownComp: String?, method: String?, notes: String?) -> Unit,
    onSelectAiLayer: (Int) -> Unit,
    onAcknowledgePrivacy: (Boolean) -> Unit,
    onClearAiResult: () -> Unit
) {
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(AIConfiguration.customApiKey ?: "") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var isContextExpanded by remember { mutableStateOf(false) }

    val isKeyConfigured = uiState.aiDiagnostics.isConfigured

    if (showApiKeyDialog) {
        ApiKeyConfigurationDialog(
            currentKey = apiKeyInput,
            isKeyVisible = isKeyVisible,
            onToggleVisibility = { isKeyVisible = !isKeyVisible },
            onKeyChange = { apiKeyInput = it },
            onSave = {
                onSetApiKey(apiKeyInput.trim())
                showApiKeyDialog = false
            },
            onClear = {
                onSetApiKey(null)
                apiKeyInput = ""
                showApiKeyDialog = false
            },
            onDismiss = { showApiKeyDialog = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---------------------------------------------------------
        // HEADER & API CONFIGURATION CARD
        // ---------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI Spectroscopy Assistant",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Interpreter & Reasoning Assistant (Gemini API)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = if (isKeyConfigured) EmeraldMatch.copy(alpha = 0.15f) else AmberLaser.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                if (isKeyConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isKeyConfigured) EmeraldMatch else AmberLaser,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isKeyConfigured) "Key Ready" else "Key Required",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isKeyConfigured) EmeraldMatch else AmberLaser
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            apiKeyInput = AIConfiguration.customApiKey ?: ""
                            showApiKeyDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("button_open_key_dialog")
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isKeyConfigured) "Configure API Key" else "Set Gemini API Key", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onTestConnection,
                        enabled = !uiState.isAiLoading && !uiState.isTestingConnection,
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("button_quick_test_connection")
                    ) {
                        if (uiState.isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testing…", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Connection", fontSize = 12.sp)
                        }
                    }
                }

                uiState.aiConnectionTestResult?.let { testRes ->
                    Surface(
                        color = if (testRes.isSuccess) EmeraldMatch.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (testRes.isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (testRes.isSuccess) EmeraldMatch else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (testRes.isSuccess)
                                    "Connected to Gemini API successfully (${testRes.durationMs}ms latency)"
                                else
                                    (testRes.errorMessage ?: "Connection failed"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // THREE DISTINCT LAYERS SELECTOR
        // "Measured data", "Database assignments", "AI interpretation"
        // ---------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Spectroscopy Analysis Layers",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "3 Distinct Evidence Layers",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TabRow(
                    selectedTabIndex = uiState.activeAiLayerIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = uiState.activeAiLayerIndex == 0,
                        onClick = { onSelectAiLayer(0) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("1. Measured Data", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        modifier = Modifier.testTag("tab_layer_measured_data")
                    )
                    Tab(
                        selected = uiState.activeAiLayerIndex == 1,
                        onClick = { onSelectAiLayer(1) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("2. Database Rules", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        modifier = Modifier.testTag("tab_layer_database_assignments")
                    )
                    Tab(
                        selected = uiState.activeAiLayerIndex == 2,
                        onClick = { onSelectAiLayer(2) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("3. AI Interpretation", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        modifier = Modifier.testTag("tab_layer_ai_interpretation")
                    )
                }
            }
        }

        // ---------------------------------------------------------
        // LAYER CONTENT SWITCHER
        // ---------------------------------------------------------
        when (uiState.activeAiLayerIndex) {
            0 -> {
                // LAYER 1: MEASURED DATA
                MeasuredDataLayerCard(uiState = uiState)
            }
            1 -> {
                // LAYER 2: DATABASE ASSIGNMENTS
                DatabaseAssignmentsLayerCard(uiState = uiState)
            }
            2 -> {
                // LAYER 3: AI INTERPRETATION
                AiInterpretationLayerSection(
                    uiState = uiState,
                    isContextExpanded = isContextExpanded,
                    onToggleContext = { isContextExpanded = !isContextExpanded },
                    onRunAi = onRunAi,
                    onUpdateUserContext = onUpdateUserContext,
                    onAcknowledgePrivacy = onAcknowledgePrivacy,
                    onClearAiResult = onClearAiResult,
                    onOpenKeyDialog = {
                        apiKeyInput = AIConfiguration.customApiKey ?: ""
                        showApiKeyDialog = true
                    },
                    onTestConnection = onTestConnection
                )
            }
        }
    }
}

// -------------------------------------------------------------
// LAYER 1: MEASURED DATA (IMMUTABLE EXPERIMENTAL EVIDENCE)
// -------------------------------------------------------------
@Composable
private fun MeasuredDataLayerCard(uiState: AnalyzeUiState) {
    val spectrum = uiState.currentSpectrum
    Card(
        modifier = Modifier.fillMaxWidth().testTag("card_layer_measured_data"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = CyanLaser, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Layer 1: Measured Experimental Spectrum", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Surface(
                    color = CyanLaser.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Immutable Raw Data",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanLaser,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "This layer contains purely experimental measurements obtained from instrument acquisition or digitized data. It is never overwritten or altered by AI interpretation.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )

            // Spectrum metadata overview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetadataBadge(label = "Type", value = spectrum.spectrumType.label)
                MetadataBadge(label = "Range", value = "${spectrum.minWavenumber.toInt()} – ${spectrum.maxWavenumber.toInt()} ${spectrum.xUnit}")
                MetadataBadge(label = "Points", value = "${spectrum.pointCount}")
                MetadataBadge(label = "Detected Peaks", value = "${spectrum.detectedPeaks.size}")
                if (spectrum.preprocessingHistory.isNotEmpty()) {
                    MetadataBadge(label = "Preprocessing", value = "${spectrum.preprocessingHistory.size} steps")
                }
            }

            if (spectrum.preprocessingHistory.isNotEmpty()) {
                Text("Applied Preprocessing Pipeline:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    spectrum.preprocessingHistory.forEach { step ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• ${step.stepType}: ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(step.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Measured detected peaks table
            Text(
                text = "Locally Measured Peaks (${spectrum.detectedPeaks.size} bands detected):",
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )

            if (spectrum.detectedPeaks.isEmpty()) {
                Text(
                    text = "No peaks detected yet. Adjust prominence threshold in the Peaks tab or run peak detection.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Wavenumber", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.1f))
                            Text("Signal (${spectrum.yUnit})", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1f))
                            Text("Prominence", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1f))
                            Text("FWHM", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.9f))
                            Text("Intensity", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.9f))
                        }

                        spectrum.detectedPeaks.take(12).forEach { peak ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "%.1f cm⁻¹".format(Locale.US, peak.wavenumber),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1.1f)
                                )
                                Text(
                                    text = "%.2f".format(Locale.US, peak.signalValue),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "%.2f".format(Locale.US, peak.prominence),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "%.1f cm⁻¹".format(Locale.US, peak.fwhm),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(0.9f)
                                )
                                Text(
                                    text = "%.1f%%".format(Locale.US, peak.relativeIntensity),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(0.9f)
                                )
                            }
                        }

                        if (spectrum.detectedPeaks.size > 12) {
                            Text(
                                text = "… and ${spectrum.detectedPeaks.size - 12} more measured peaks (see Peaks tab for complete listing)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// LAYER 2: DATABASE ASSIGNMENTS (DETERMINISTIC ON-DEVICE RULES)
// -------------------------------------------------------------
@Composable
private fun DatabaseAssignmentsLayerCard(uiState: AnalyzeUiState) {
    val spectrum = uiState.currentSpectrum
    val matchedPeaks = spectrum.detectedPeaks.filter { it.matchedFunctionalGroup != null }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("card_layer_database_rules"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = EmeraldMatch, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Layer 2: Database Reference Rules", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Surface(
                    color = EmeraldMatch.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Deterministic On-Device",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldMatch,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "This layer contains deterministic rule-based assignments from the local IR functional group correlation dictionary and numerical library comparison, evaluated without AI.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )

            // Local IR Dictionary matches
            Text(
                text = "Local IR Dictionary Assignments (${matchedPeaks.size}/${spectrum.detectedPeaks.size} peaks matched):",
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )

            if (matchedPeaks.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No peaks matched predefined functional group intervals. Adjust thresholds or add custom group definitions in Dictionary screen.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    matchedPeaks.take(6).forEach { peak ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "%.1f cm⁻¹".format(Locale.US, peak.wavenumber),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        peak.matchedFunctionalGroup ?: "",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "%.1f%%".format(Locale.US, peak.relativeIntensity),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Reference Library Overlay status
            Text("Reference Library Comparison Status:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)

            val comp = uiState.numericalComparisonResult
            val ref = uiState.selectedReference

            if (ref != null && comp != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                ref.compoundName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            ref.chemicalFormula?.let {
                                Text(it, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val hqi = comp.allAlgorithmScores[com.example.domain.spectroscopy.SimilarityAlgorithm.HQI] ?: comp.similarityScore
                            val pearson = comp.allAlgorithmScores[com.example.domain.spectroscopy.SimilarityAlgorithm.PEARSON] ?: 0.0
                            val cosine = comp.allAlgorithmScores[com.example.domain.spectroscopy.SimilarityAlgorithm.COSINE] ?: 0.0
                            MetadataBadge("HQI", "%.1f%%".format(Locale.US, hqi))
                            MetadataBadge("Pearson r", "%.3f".format(Locale.US, pearson))
                            MetadataBadge("Cosine", "%.3f".format(Locale.US, cosine))
                            MetadataBadge("Aligned Peaks", "${comp.matchedPeaksCount}/${comp.totalUnknownPeaksCount}")
                        }
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No reference standard selected. Open the Compare tab to select a library standard (e.g. Polystyrene, Acetone, Ethanol) and compute numerical similarity.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// LAYER 3: AI INTERPRETATION SECTION
// -------------------------------------------------------------
@Composable
private fun AiInterpretationLayerSection(
    uiState: AnalyzeUiState,
    isContextExpanded: Boolean,
    onToggleContext: () -> Unit,
    onRunAi: () -> Unit,
    onUpdateUserContext: (sampleName: String?, sampleType: String?, expectedComp: String?, knownComp: String?, method: String?, notes: String?) -> Unit,
    onAcknowledgePrivacy: (Boolean) -> Unit,
    onClearAiResult: () -> Unit,
    onOpenKeyDialog: () -> Unit,
    onTestConnection: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Core Principle & Privacy Disclosure Card
        PrivacyAndCorePrincipleCard(
            hasAcknowledgedPrivacy = uiState.hasAcknowledgedPrivacy,
            onAcknowledge = onAcknowledgePrivacy
        )

        // User Context Accordion (Supporting background)
        UserSampleContextCard(
            uiState = uiState,
            isExpanded = isContextExpanded,
            onToggle = onToggleContext,
            onUpdateContext = onUpdateUserContext
        )

        // Action Trigger Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onRunAi,
                        enabled = !uiState.isAiLoading && !uiState.isTestingConnection,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("button_run_ai_analysis")
                    ) {
                        if (uiState.isAiLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (uiState.aiWorkflowState) {
                                    AiWorkflowState.PREPARING_LOCAL_DATA -> "1. Preparing local measurements…"
                                    AiWorkflowState.SENDING_AI_REQUEST -> "2. Contacting Gemini API…"
                                    AiWorkflowState.WAITING_FOR_AI_RESPONSE -> "3. Waiting for response…"
                                    AiWorkflowState.AI_RESPONSE_RECEIVED, AiWorkflowState.INTERPRETING_RESPONSE -> "4. Parsing interpretation…"
                                    else -> "Analyzing with AI…"
                                },
                                fontSize = 12.sp
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze with AI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    if (uiState.aiResult != null && !uiState.isAiLoading) {
                        IconButton(
                            onClick = onClearAiResult,
                            modifier = Modifier.size(44.dp).testTag("button_clear_ai_result")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear AI Analysis", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // AI Workflow State indication when active
                if (uiState.isAiLoading) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Local peaks and dictionary rules packaged -> External Gemini API reasoning active",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Display AI Result or Error
        uiState.aiResult?.let { result ->
            if (result.isSuccess) {
                StructuredAiInterpretationDisplay(result = result)
            } else {
                AiErrorCard(
                    result = result,
                    onRetry = onRunAi,
                    onOpenKey = onOpenKeyDialog,
                    onTestConn = onTestConnection,
                    isTesting = uiState.isTestingConnection
                )
            }
        }
    }
}

// -------------------------------------------------------------
// PRIVACY & CORE SCIENTIFIC PRINCIPLE CARD
// -------------------------------------------------------------
@Composable
private fun PrivacyAndCorePrincipleCard(
    hasAcknowledgedPrivacy: Boolean,
    onAcknowledge: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Core Scientific Principle & Privacy Notice", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Text(
                text = "• The AI is an interpreter and scientific assistant — NOT the measurement instrument.\n" +
                        "• All peak measurements, baseline corrections, and dictionary matching are performed locally on-device.\n" +
                        "• AI analysis transmits structured summary data (peak wavenumbers, FWHM, dictionary matches, reference stats, and sample notes) to the external Google Gemini API.\n" +
                        "• Data is never uploaded automatically; transmission occurs only when you press 'Analyze with AI'.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAcknowledge(!hasAcknowledgedPrivacy) }
                    .padding(vertical = 2.dp)
            ) {
                Checkbox(
                    checked = hasAcknowledgedPrivacy,
                    onCheckedChange = { onAcknowledge(it) },
                    modifier = Modifier.size(24.dp).testTag("checkbox_privacy_acknowledged")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I acknowledge that structured spectrum data will be sent to the configured Gemini API service.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// -------------------------------------------------------------
// USER SAMPLE CONTEXT ACCORDION CARD
// -------------------------------------------------------------
@Composable
private fun UserSampleContextCard(
    uiState: AnalyzeUiState,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onUpdateContext: (sampleName: String?, sampleType: String?, expectedComp: String?, knownComp: String?, method: String?, notes: String?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "User Sample Context (Supporting Evidence)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            Text(
                text = "Context provided here is used as supporting background information by the AI assistant, NEVER as definitive proof.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    OutlinedTextField(
                        value = uiState.userSampleName,
                        onValueChange = { onUpdateContext(it, null, null, null, null, null) },
                        label = { Text("Sample Name / ID", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Unknown Polymer Film #12", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_user_sample_name")
                    )

                    // Sample Type selection chips & field
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Sample Type:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Neat Liquid", "Solid / Film", "Powder / Pellet", "Polymer / Composite", "Solution").forEach { type ->
                                val selected = uiState.userSampleType == type
                                Surface(
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable { onUpdateContext(null, type, null, null, null, null) }
                                ) {
                                    Text(
                                        text = type,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.userExpectedComposition,
                        onValueChange = { onUpdateContext(null, null, it, null, null, null) },
                        label = { Text("Expected Composition / Functional Class", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Suspected aliphatic ketone or ester", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_user_expected_comp")
                    )

                    OutlinedTextField(
                        value = uiState.userKnownComponents,
                        onValueChange = { onUpdateContext(null, null, null, it, null, null) },
                        label = { Text("Known Components / Solvents / Additives", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Trace water, acetone rinse residue", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_user_known_components")
                    )

                    // Measurement Method chips & field
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Measurement Method:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "ATR-FTIR (Diamond Crystal)",
                                "Transmission (KBr Pellet)",
                                "Specular Reflection",
                                "ATR-FTIR (Ge Crystal)"
                            ).forEach { method ->
                                val selected = uiState.userMeasurementMethod == method
                                Surface(
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable { onUpdateContext(null, null, null, null, method, null) }
                                ) {
                                    Text(
                                        text = method,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.userExperimentalNotes,
                        onValueChange = { onUpdateContext(null, null, null, null, null, it) },
                        label = { Text("Experimental Notes / Sample Prep", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Scraped sample, purge time 30s", fontSize = 11.sp) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("input_user_notes")
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STRUCTURED AI RESULT DISPLAY
// -------------------------------------------------------------
@Composable
private fun StructuredAiInterpretationDisplay(result: AIInterpretationResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // AI Cautious Language Banner
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Scientific Interpretation Assistant (Physical Organic Chemistry)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = EmeraldMatch.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "${result.durationMs}ms (${result.modelName ?: "Gemini"})",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldMatch,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 1. SPECTRUM SUMMARY
        result.spectrumSummary?.let { summary ->
            Card(
                modifier = Modifier.fillMaxWidth().testTag("card_ai_spectrum_summary"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("1. Spectrum Summary", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetadataBadge("Type", summary.spectrumType)
                        MetadataBadge("Range", summary.range)
                        MetadataBadge("Peaks Analyzed", "${summary.peakCount}")
                    }

                    if (summary.functionalGroupRegionSummary.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Functional Group Region (4000–1500 cm⁻¹):", fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                                Text(summary.functionalGroupRegionSummary, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (summary.fingerprintRegionSummary.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Fingerprint Region (1500–400 cm⁻¹):", fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                                Text(summary.fingerprintRegionSummary, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // 2. MAJOR PEAK ASSIGNMENTS
        if (result.majorPeakAssignments.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("card_ai_peak_assignments"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("2. Major Peak Assignments", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${result.majorPeakAssignments.size} assignments", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Text(
                        text = "Vibrational mode assignments correlating observed peak features with bond dipole changes:",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    result.majorPeakAssignments.forEach { assign ->
                        MajorPeakAssignmentCard(assignment = assign)
                    }
                }
            }
        }

        // 3. COMPLETE SPECTRUM INTERPRETATION
        Card(
            modifier = Modifier.fillMaxWidth().testTag("card_ai_complete_interpretation"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("3. Complete Spectrum Interpretation", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = result.completeSpectrumInterpretation,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 4. REFERENCE COMPARISON SUMMARY
        result.referenceComparisonSummary?.let { refComp ->
            Card(
                modifier = Modifier.fillMaxWidth().testTag("card_ai_reference_comparison"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("4. Reference Standard Comparison", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        text = refComp,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 5. UNCERTAINTIES & AMBIGUITIES
        if (result.uncertainties.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("card_ai_uncertainties"),
                colors = CardDefaults.cardColors(containerColor = AmberLaser.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AmberLaser, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("5. Scientific Uncertainties & Ambiguities", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AmberLaser)
                    }

                    Text(
                        text = "Identified ambiguities where vibrational evidence is overlapping, ambiguous, or lacks independent verification:",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    result.uncertainties.forEach { unc ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("• ", fontSize = 11.sp, color = AmberLaser, fontWeight = FontWeight.Bold)
                            Text(unc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }

        // 6. ADDITIONAL INFORMATION NEEDED
        if (result.additionalInformationNeeded.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("card_ai_additional_info"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("6. Additional Measurements Needed", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Text(
                        text = "Complementary experimental analyses that would resolve ambiguity or confirm structural assignments:",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    result.additionalInformationNeeded.forEach { addInfo ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("• ", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(addInfo, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }

        // 7. CONFIDENCE ESTIMATE CARD
        // MUST BE CLEARLY LABELED: "AI/database confidence estimate"
        // MUST NOT BE REPRESENTED AS EXPERIMENTALLY VALIDATED PROBABILITY
        result.confidenceEstimate?.let { conf ->
            ConfidenceEstimateCard(confidence = conf)
        }
    }
}

// -------------------------------------------------------------
// MAJOR PEAK ASSIGNMENT COMPONENT
// -------------------------------------------------------------
@Composable
private fun MajorPeakAssignmentCard(assignment: AIMajorPeakAssignment) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "%.1f cm⁻¹".format(Locale.US, assignment.wavenumber),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (assignment.observedFeature.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                assignment.observedFeature,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (assignment.possibleBond.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            assignment.possibleBond,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (assignment.functionalGroup.isNotBlank()) {
                Text(
                    text = "Functional Group: ${assignment.functionalGroup}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }

            if (assignment.vibrationalMode.isNotBlank()) {
                Text(
                    text = "Vibrational Mode: ${assignment.vibrationalMode}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (assignment.supportingEvidence.isNotBlank()) {
                Text(
                    text = "Supporting Evidence: ${assignment.supportingEvidence}",
                    fontSize = 10.sp,
                    color = EmeraldMatch,
                    lineHeight = 14.sp
                )
            }

            if (assignment.alternativeExplanations.isNotBlank()) {
                Text(
                    text = "Alternative Explanations: ${assignment.alternativeExplanations}",
                    fontSize = 10.sp,
                    color = AmberLaser,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// CONFIDENCE ESTIMATE CARD
// -------------------------------------------------------------
@Composable
private fun ConfidenceEstimateCard(confidence: AIConfidenceEstimate) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("card_ai_confidence_estimate"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = confidence.label, // "AI/database confidence estimate"
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    color = when (confidence.level.lowercase(Locale.US)) {
                        "high" -> EmeraldMatch.copy(alpha = 0.2f)
                        "moderate", "medium" -> CyanLaser.copy(alpha = 0.2f)
                        else -> AmberLaser.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = confidence.level,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (confidence.level.lowercase(Locale.US)) {
                            "high" -> EmeraldMatch
                            "moderate", "medium" -> CyanLaser
                            else -> AmberLaser
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "Notice: This estimate reflects automated heuristic library agreement and physical bond frequency rules. It is NOT an experimentally validated mathematical probability.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )

            if (confidence.basis.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Basis: ${confidence.basis}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// AI ERROR CARD
// -------------------------------------------------------------
@Composable
private fun AiErrorCard(
    result: AIInterpretationResult,
    onRetry: () -> Unit,
    onOpenKey: () -> Unit,
    onTestConn: () -> Unit,
    isTesting: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("card_ai_error"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Interpretation Unavailable", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                }

                result.httpStatusCode?.let { code ->
                    Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text("HTTP $code", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }
            }

            Text(
                text = result.errorMessage ?: result.summary,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                lineHeight = 15.sp
            )

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldMatch, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Local spectroscopy data is completely intact. Peak detection, measurements, and library comparisons remain 100% available offline.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onOpenKey,
                    modifier = Modifier.weight(1f).height(36.dp).testTag("button_error_configure_key")
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("API Key", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onTestConn,
                    enabled = !isTesting,
                    modifier = Modifier.weight(1f).height(36.dp).testTag("button_error_test_conn")
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test", fontSize = 11.sp)
                }

                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f).height(36.dp).testTag("button_error_retry_ai")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry", fontSize = 11.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// API KEY CONFIGURATION DIALOG
// -------------------------------------------------------------
@Composable
private fun ApiKeyConfigurationDialog(
    currentKey: String,
    isKeyVisible: Boolean,
    onToggleVisibility: () -> Unit,
    onKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configure Gemini API Key")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter your Google Gemini API key from Google AI Studio. It is stored securely on-device and used to interpret locally measured FTIR spectra.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = currentKey,
                    onValueChange = onKeyChange,
                    label = { Text("GEMINI_API_KEY") },
                    placeholder = { Text("AIzaSy…") },
                    singleLine = true,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = onToggleVisibility) {
                            Icon(
                                if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_input_api_key")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                modifier = Modifier.testTag("dialog_button_save_key")
            ) {
                Text("Save & Apply")
            }
        },
        dismissButton = {
            Row {
                if (AIConfiguration.customApiKey != null) {
                    TextButton(onClick = onClear) {
                        Text("Clear Key")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

// -------------------------------------------------------------
// HELPER BADGE
// -------------------------------------------------------------
@Composable
private fun MetadataBadge(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$label: ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
