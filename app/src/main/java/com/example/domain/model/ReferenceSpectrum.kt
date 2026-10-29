package com.example.domain.model

import java.util.UUID

/**
 * Model representing a reference spectrum in the spectral library.
 *
 * Essential scientific distinction:
 * Reference spectra serve as comparative baselines for spectral similarity evaluation.
 * A high similarity match does NOT equate to definitive chemical identification.
 */
data class ReferenceSpectrum(
    val id: String = UUID.randomUUID().toString(),
    val databaseIdentifier: String = id,
    val compoundName: String,
    val chemicalFormula: String? = null,
    val casNumber: String? = null,
    val materialClass: String = "General Organic",
    val description: String = "",
    val notes: String = description,
    val measurementMethod: String = "ATR-FTIR (Diamond Crystal)",
    val source: String = "Standard Reference Library",
    val sampleInformation: String = "Analytical reference grade",
    val xAxisUnit: String = "cm⁻¹",
    val yAxisType: SpectrumType = SpectrumType.Transmittance,
    val functionalGroups: List<String> = emptyList(),
    val spectralRegions: List<String> = emptyList(),
    val characteristicPeaks: List<Double> = emptyList(),
    val spectrum: Spectrum,
    val isDemo: Boolean = true,
    val isUserCustom: Boolean = false
) {
    // Backward compatibility helper for existing code referencing molecularFormula and category
    val molecularFormula: String
        get() = chemicalFormula ?: ""

    val category: String
        get() = materialClass
}
