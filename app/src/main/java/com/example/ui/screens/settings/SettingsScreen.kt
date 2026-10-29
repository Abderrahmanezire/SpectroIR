package com.example.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.domain.spectroscopy.AIConfiguration
import com.example.domain.spectroscopy.AIConnectionTestResult
import com.example.domain.spectroscopy.StandardAIService
import com.example.ui.theme.AmberLaser
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldMatch
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val aiService = remember { StandardAIService() }
    val coroutineScope = rememberCoroutineScope()
    var isAiConfigured by remember { mutableStateOf(aiService.isConfigured()) }
    var apiKeyInput by remember { mutableStateOf(AIConfiguration.customApiKey ?: "") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<AIConnectionTestResult?>(null) }
    var keySaveFeedback by remember { mutableStateOf<String?>(null) }

    var axisDescending by remember { mutableStateOf(true) }
    var defaultTransmittance by remember { mutableStateOf(true) }
    var peakSensitivity by remember { mutableFloatStateOf(8.0f) }
    var autoMatchFunctionalGroups by remember { mutableStateOf(true) }
    var selectedRange by remember { mutableStateOf("Mid-IR (4000–400 cm⁻¹)") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Spectroscopy Preferences",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Instrument display parameters and chemical algorithm settings",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section: Graph & Axis Conventions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = CyanLaser, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Axis & Display Convention", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Descending Wavenumber Axis", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Standard IUPAC FTIR convention: 4000 cm⁻¹ left → 400 cm⁻¹ right", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = axisDescending,
                            onCheckedChange = { axisDescending = it },
                            modifier = Modifier.testTag("switch_axis_descending")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default Spectrum Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(if (defaultTransmittance) "% Transmittance (troughs dip down)" else "Absorbance (peaks rise up)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = defaultTransmittance,
                            onCheckedChange = { defaultTransmittance = it },
                            modifier = Modifier.testTag("switch_default_mode")
                        )
                    }
                }
            }
        }

        // Section: Wavenumber Spectral Ranges
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = EmeraldMatch, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Default Spectral Range", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    listOf(
                        "Mid-IR (4000–400 cm⁻¹)" to "Standard fundamental vibrations",
                        "Near-IR (12800–4000 cm⁻¹)" to "Overtones and combinations",
                        "Far-IR (400–10 cm⁻¹)" to "Lattice and skeletal modes"
                    ).forEach { (range, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRange == range,
                                onClick = { selectedRange = range }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(range, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Section: Peak Detection Sensitivity
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Peak Detection Sensitivity", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "Default prominence threshold: ${"%.1f".format(peakSensitivity)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = peakSensitivity,
                        onValueChange = { peakSensitivity = it },
                        valueRange = 2f..30f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-match Functional Groups", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Correlate detected peaks against IR reference dictionary", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = autoMatchFunctionalGroups,
                            onCheckedChange = { autoMatchFunctionalGroups = it }
                        )
                    }
                }
            }
        }

        // Section: AI API Configuration
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Interpretation Engine", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Surface(
                            color = if (isAiConfigured) EmeraldMatch.copy(alpha = 0.15f) else AmberLaser.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    if (isAiConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isAiConfigured) EmeraldMatch else AmberLaser,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isAiConfigured) "Configured" else "Key Required",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAiConfigured) EmeraldMatch else AmberLaser
                                )
                            }
                        }
                    }

                    Text(
                        text = "Gemini API analyzes locally measured peaks, functional group matches, and reference alignments to provide physical organic chemistry insights.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            keySaveFeedback = null
                        },
                        label = { Text("Gemini API Key (GEMINI_API_KEY)") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isKeyVisible) "Hide Key" else "Show Key",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_gemini_api_key")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                AIConfiguration.setKey(apiKeyInput.trim())
                                isAiConfigured = aiService.isConfigured()
                                keySaveFeedback = if (isAiConfigured) "API Key saved successfully." else "API Key cleared."
                                testResult = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("button_save_api_key")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Key", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isTestingConnection = true
                                    testResult = null
                                    keySaveFeedback = null
                                    // Apply current text input before test
                                    if (apiKeyInput.isNotBlank()) {
                                        AIConfiguration.setKey(apiKeyInput.trim())
                                    }
                                    val res = aiService.testConnection()
                                    testResult = res
                                    isAiConfigured = aiService.isConfigured()
                                    isTestingConnection = false
                                }
                            },
                            enabled = !isTestingConnection,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("button_test_connection")
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Connection", fontSize = 12.sp)
                            }
                        }
                    }

                    keySaveFeedback?.let { msg ->
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = if (isAiConfigured) EmeraldMatch else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    testResult?.let { res ->
                        Surface(
                            color = if (res.isSuccess) EmeraldMatch.copy(alpha = 0.12f) else AmberLaser.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (res.isSuccess) "Connection Successful" else "Connection Failed",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (res.isSuccess) EmeraldMatch else MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (res.isSuccess) {
                                        "Connected to generativelanguage.googleapis.com (gemini-3.5-flash) in ${res.durationMs}ms."
                                    } else {
                                        res.errorMessage ?: "Failed to connect to Gemini API."
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: About & Scientific Standards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("About SpectroIR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Version 1.0.0 (Foundation Stage)", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Designed for analytical chemists, spectroscopists, and materials researchers. Implements IUPAC convention wavenumber coordinates (cm⁻¹), non-destructive preprocessing transforms, and empirical vibrational correlation databases.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
