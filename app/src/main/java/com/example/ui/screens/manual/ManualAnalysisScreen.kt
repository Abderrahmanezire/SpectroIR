package com.example.ui.screens.manual

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.BondAssignment
import com.example.domain.model.PeakIntensity
import com.example.domain.model.Spectrum
import com.example.domain.spectroscopy.CompoundClassEvidence
import com.example.domain.spectroscopy.CorroboratedInterpretation
import com.example.domain.spectroscopy.MultiPeakAnalysisResult
import com.example.domain.spectroscopy.StandardManualInterpretationEngine
import com.example.ui.theme.AmberLaser
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldMatch
import java.util.Locale

@Composable
fun ManualAnalysisScreen(
    currentSpectrum: Spectrum? = null,
    onNavigateToDictionaryWithQuery: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val engine = remember { StandardManualInterpretationEngine() }

    // Multi-peak entry list
    val enteredPeaks = remember { mutableStateListOf<Double>() }
    var singleWnInput by remember { mutableStateOf("") }
    var selectedIntensity by remember { mutableStateOf(PeakIntensity.STRONG) }
    var sampleNotesInput by remember { mutableStateOf("") }

    // Quick presets
    val quickPresets = remember {
        listOf(
            "Acetone (Ketone)" to listOf(1715.0, 1365.0, 1220.0, 2960.0),
            "Ethanol (Alcohol)" to listOf(3350.0, 2970.0, 2880.0, 1050.0),
            "Ethyl Acetate (Ester)" to listOf(1740.0, 1240.0, 1050.0, 2980.0),
            "Benzoic Acid (Acid)" to listOf(3000.0, 1700.0, 1290.0, 1600.0, 710.0),
            "Benzonitrile (Nitrile)" to listOf(2230.0, 1600.0, 1490.0, 760.0, 690.0)
        )
    }

    // Auto-populate from loaded active spectrum if available and list empty
    LaunchedEffect(currentSpectrum) {
        if (enteredPeaks.isEmpty() && currentSpectrum != null && currentSpectrum.detectedPeaks.isNotEmpty()) {
            enteredPeaks.addAll(currentSpectrum.detectedPeaks.map { it.wavenumber })
        }
    }

    // Run multi-peak deterministic analysis whenever peaks list changes
    val analysisResult: MultiPeakAnalysisResult = remember(enteredPeaks.toList()) {
        engine.analyzeMultiplePeaks(enteredPeaks.toList())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Deterministic Knowledge System
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_manual_header"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Deterministic Manual Interpretation",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Spectroscopy rule engine • Multi-peak evidence corroboration",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Scientific rule: A single wavenumber does not uniquely identify a compound. Enter one or multiple bands to cross-corroborate functional groups, check required partner peaks, and uncover molecular ambiguities.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Input Section: Enter Wavenumber & Manage Peaks
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Add Experimental Peaks",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = singleWnInput,
                                onValueChange = { singleWnInput = it },
                                placeholder = { Text("e.g. 1715") },
                                label = { Text("Wavenumber (cm⁻¹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("input_manual_wavenumber")
                            )

                            Button(
                                onClick = {
                                    val wn = singleWnInput.trim().toDoubleOrNull()
                                    if (wn != null && wn in 400.0..4000.0 && !enteredPeaks.contains(wn)) {
                                        enteredPeaks.add(wn)
                                        singleWnInput = ""
                                    }
                                },
                                enabled = singleWnInput.toDoubleOrNull() != null,
                                modifier = Modifier.height(52.dp).testTag("button_add_manual_peak")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add")
                            }
                        }

                        // Presets row
                        Text(
                            text = "Load Example Diagnostic Sets:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(quickPresets) { (name, peaks) ->
                                OutlinedButton(
                                    onClick = {
                                        enteredPeaks.clear()
                                        enteredPeaks.addAll(peaks)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(name, fontSize = 10.sp)
                                }
                            }
                        }

                        // Currently active query peaks chips
                        if (enteredPeaks.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Active Peaks (${enteredPeaks.size}):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                OutlinedButton(
                                    onClick = { enteredPeaks.clear() },
                                    modifier = Modifier.height(26.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear All", fontSize = 10.sp)
                                }
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(enteredPeaks.sortedDescending()) { wn ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.testTag("chip_peak_${wn.toInt()}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                                        ) {
                                            Text(
                                                text = "${wn.toInt()} cm⁻¹",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            IconButton(
                                                onClick = { enteredPeaks.remove(wn) },
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove peak",
                                                    modifier = Modifier.size(14.dp)
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

            // Section: Corroborated Compound Classes (Multi-peak joint evidence)
            if (analysisResult.compatibleCompoundClasses.isNotEmpty()) {
                item {
                    Text(
                        text = "Corroborated Compound Classes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(analysisResult.compatibleCompoundClasses.take(6)) { compound ->
                    CompoundClassCard(compound = compound)
                }
            }

            // Section: Spectral Ambiguities & Environment Warnings
            if (analysisResult.potentialAmbiguities.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("card_ambiguities"),
                        colors = CardDefaults.cardColors(
                            containerColor = AmberLaser.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberLaser.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AmberLaser, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Potential Ambiguities & Environmental Context", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AmberLaser)
                            }
                            analysisResult.potentialAmbiguities.forEach { amb ->
                                Text("• $amb", fontSize = 11.sp, lineHeight = 15.sp)
                            }
                        }
                    }
                }
            }

            // Section: Detailed Bond Assignments with Supporting Evidence
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Compatible Bond Assignments (${analysisResult.topInterpretations.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sorted by Corroboration",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (analysisResult.topInterpretations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("No peaks entered yet.", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Add one or more wavenumbers above or load a preset.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(analysisResult.topInterpretations) { interp ->
                    CorroboratedInterpretationCard(
                        interpretation = interp,
                        onSearchDictionary = {
                            onNavigateToDictionaryWithQuery?.invoke("${interp.assignment.bond} ${interp.matchedQueryPeakWavenumber.toInt()}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompoundClassCard(compound: CompoundClassEvidence) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(compound.compoundClass, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = when (compound.confidenceGrade) {
                            "High Evidence" -> EmeraldMatch.copy(alpha = 0.2f)
                            "Probable" -> CyanLaser.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = compound.confidenceGrade,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (compound.confidenceGrade) {
                                "High Evidence" -> EmeraldMatch
                                "Probable" -> CyanLaser
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Corroborating Bonds: ${compound.supportingBonds.joinToString(", ")}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Matching Peaks: ${compound.matchedPeaks.joinToString(", ") { "${it.toInt()} cm⁻¹" }}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun CorroboratedInterpretationCard(
    interpretation: CorroboratedInterpretation,
    onSearchDictionary: () -> Unit
) {
    val a = interpretation.assignment

    Card(
        modifier = Modifier.fillMaxWidth().testTag("card_interp_${a.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Top Row: Bond, Peak, Region
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = CyanLaser.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = a.bond,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(a.functionalGroup, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(a.vibrationalMode, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    color = when {
                        interpretation.corroborationScore >= 0.85 -> EmeraldMatch.copy(alpha = 0.18f)
                        interpretation.corroborationScore >= 0.65 -> CyanLaser.copy(alpha = 0.18f)
                        else -> AmberLaser.copy(alpha = 0.18f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Query: ${interpretation.matchedQueryPeakWavenumber.toInt()} cm⁻¹",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            interpretation.corroborationScore >= 0.85 -> EmeraldMatch
                            interpretation.corroborationScore >= 0.65 -> CyanLaser
                            else -> AmberLaser
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Diagnostic Parameters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Typical: ${a.typicalWavenumber.toInt()} cm⁻¹ (${a.minWavenumber.toInt()}–${a.maxWavenumber.toInt()})",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Intensity: ${a.intensity.label} • ${a.bandShape}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Visual diagram (ASCII)
            if (a.visualDiagramAscii != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = a.visualDiagramAscii,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Supporting evidence check results
            if (interpretation.supportingEvidenceFound.isNotEmpty()) {
                Surface(
                    color = EmeraldMatch.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldMatch, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Corroborating peaks found in query spectrum:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldMatch)
                        }
                        interpretation.supportingEvidenceFound.forEach { ev ->
                            Text(
                                text = "• ${ev.role}: found at ${ev.foundPeakWavenumber.toInt()} cm⁻¹ (${ev.expectedRange})",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            if (interpretation.missingSupportingEvidence.isNotEmpty()) {
                Surface(
                    color = AmberLaser.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AmberLaser, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Missing companion peaks (verify before assigning):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberLaser)
                        }
                        interpretation.missingSupportingEvidence.forEach { miss ->
                            Text("• $miss", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Molecular Environment Note
            Text(
                text = "Notes: ${a.notes}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )

            // Ambiguities for this specific band
            if (interpretation.potentialAmbiguities.isNotEmpty()) {
                Text(
                    text = "Ambiguity: ${interpretation.potentialAmbiguities.first()}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.error,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
