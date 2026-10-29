package com.example.domain.model

data class FunctionalGroup(
    val id: String,
    val name: String,
    val structure: String,
    val category: String,
    val minWavenumber: Double,
    val maxWavenumber: Double,
    val typicalPeakWavenumber: Double,
    val intensity: PeakIntensity,
    val bandShape: String, // "Broad", "Sharp", "Doublet", "Multiple"
    val vibrationalMode: String, // "O-H stretch", "C=O stretch", etc.
    val diagnosticTips: String
)
