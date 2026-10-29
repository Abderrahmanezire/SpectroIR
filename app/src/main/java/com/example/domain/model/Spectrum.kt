package com.example.domain.model

import java.util.UUID

data class AnalysisResult(
    val title: String,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Spectrum(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sourceType: SpectrumSourceType = SpectrumSourceType.DEMO,
    val xUnit: String = WavenumberUnit.CM_MINUS_1.symbol,
    val yUnit: String = SignalUnit.TRANSMITTANCE_PERCENT.symbol,
    val spectrumType: SpectrumType = SpectrumType.Transmittance,
    val points: List<SpectrumPoint> = emptyList(),
    val rawPoints: List<SpectrumPoint> = points,
    val metadata: Map<String, String> = emptyMap(),
    val preprocessingHistory: List<PreprocessingStep> = emptyList(),
    val detectedPeaks: List<Peak> = emptyList(),
    val analysisResults: List<AnalysisResult> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val minWavenumber: Double by lazy {
        points.minOfOrNull { it.wavenumber } ?: 400.0
    }

    val maxWavenumber: Double by lazy {
        points.maxOfOrNull { it.wavenumber } ?: 4000.0
    }

    val minSignal: Double by lazy {
        points.minOfOrNull { it.signalValue } ?: 0.0
    }

    val maxSignal: Double by lazy {
        points.maxOfOrNull { it.signalValue } ?: 100.0
    }

    val pointCount: Int
        get() = points.size

    val isDemo: Boolean
        get() = sourceType == SpectrumSourceType.DEMO
}
