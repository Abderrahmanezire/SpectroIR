package com.example.ui.screens.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.demo.DemoSpectraProvider
import com.example.domain.model.Spectrum
import com.example.ui.navigation.Screen
import com.example.ui.theme.AmberLaser
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldMatch

@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit,
    onLoadSpectrum: (Spectrum) -> Unit,
    onImportStream: (java.io.InputStream, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val demoSpectra = remember { DemoSpectraProvider.getAllDemoSpectra() }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val stream = context.contentResolver.openInputStream(it)
                if (stream != null) {
                    val fileName = com.example.util.FileUtils.getFileNameFromUri(context, it)
                    onImportStream(stream, fileName)
                    onNavigate(Screen.ANALYZE)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Hero & Scientific Identity
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_hero"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Waves,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SpectroIR",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "FTIR & IR Spectroscopy Analysis",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Precision mid-IR analysis, peak detection, baseline correction & spectral library matching.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(Screen.ANALYZE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hero_action_analyze"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Analyzer")
                        }

                        Button(
                            onClick = {
                                filePicker.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hero_action_import"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import File")
                        }
                    }
                }
            }
        }

        // Section: Spectral Tools & Knowledge Base
        item {
            Text(
                text = "Spectral Tools & Libraries",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionTile(
                        title = "IR Dictionary",
                        subtitle = "Functional group correlations",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        iconTint = EmeraldMatch,
                        modifier = Modifier.weight(1f),
                        testTag = "tile_dictionary",
                        onClick = { onNavigate(Screen.DICTIONARY) }
                    )

                    ActionTile(
                        title = "Manual Analysis",
                        subtitle = "Multi-peak rule correlation",
                        icon = Icons.Default.Science,
                        iconTint = AmberLaser,
                        modifier = Modifier.weight(1f),
                        testTag = "tile_manual_analysis",
                        onClick = { onNavigate(Screen.MANUAL_ANALYSIS) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionTile(
                        title = "Reference Spectra",
                        subtitle = "Standard compounds library",
                        icon = Icons.Default.Science,
                        iconTint = Color(0xFF9C27B0),
                        modifier = Modifier.weight(1f),
                        testTag = "tile_reference",
                        onClick = { onNavigate(Screen.REFERENCE) }
                    )

                    ActionTile(
                        title = "History",
                        subtitle = "Saved local spectra & runs",
                        icon = Icons.Default.History,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        testTag = "tile_history",
                        onClick = { onNavigate(Screen.HISTORY) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionTile(
                        title = "Digitize Spectrum",
                        subtitle = "From Camera photo or PDF",
                        icon = Icons.Default.CameraAlt,
                        iconTint = CyanLaser,
                        modifier = Modifier.weight(1f),
                        testTag = "tile_digitize",
                        onClick = { onNavigate(Screen.DIGITIZE) }
                    )

                    ActionTile(
                        title = "Settings",
                        subtitle = "Spectroscopy & API preferences",
                        icon = Icons.Default.Settings,
                        iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        testTag = "tile_settings",
                        onClick = { onNavigate(Screen.SETTINGS) }
                    )
                }
            }
        }

        // Section: Development & Testing Demo Spectra
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Demo Spectra for Testing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = AmberLaser.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "DEMO DATA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberLaser,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        items(demoSpectra) { demo ->
            DemoSpectrumCard(
                spectrum = demo,
                onLoad = {
                    onLoadSpectrum(demo)
                    onNavigate(Screen.ANALYZE)
                }
            )
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Surface(
                color = iconTint.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun DemoSpectrumCard(
    spectrum: Spectrum,
    onLoad: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLoad)
            .testTag("demo_card_${spectrum.id}"),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(spectrum.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = "${spectrum.pointCount} points  •  ${spectrum.detectedPeaks.size} major peaks  •  ${spectrum.spectrumType.label}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onLoad,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Analyze", fontSize = 12.sp)
            }
        }
    }
}
