package com.example.domain.model

import java.util.UUID

data class SpectrumPoint(
    val wavenumber: Double,
    val signalValue: Double
) : Comparable<SpectrumPoint> {
    override fun compareTo(other: SpectrumPoint): Int =
        wavenumber.compareTo(other.wavenumber)
}

enum class PeakIntensity(val label: String) {
    VERY_STRONG("Very Strong"),
    STRONG("Strong"),
    MEDIUM("Medium"),
    WEAK("Weak"),
    VARIABLE("Variable")
}

data class Peak(
    val id: String = UUID.randomUUID().toString(),
    val index: Int = 1,
    val wavenumber: Double,
    val signalValue: Double,
    val prominence: Double = 0.0,
    val width: Double = 0.0,
    val fwhm: Double = 0.0, // Full width at half maximum (cm⁻¹)
    val relativeIntensity: Double = 0.0, // % relative to base or strongest peak
    val area: Double = 0.0, // Integrated numerical area
    val intensity: PeakIntensity = PeakIntensity.MEDIUM,
    val isManual: Boolean = false,
    val matchedFunctionalGroup: String? = null,
    val confidence: Double = 0.0,
    val notes: String? = null
)

data class PreprocessingStep(
    val stepType: String,
    val description: String,
    val parameters: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)
