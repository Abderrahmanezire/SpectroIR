package com.example.domain.model

import java.util.UUID

/**
 * Classification of spectral regions across the mid-infrared range (400–4000 cm⁻¹).
 */
enum class SpectralRegion(
    val label: String,
    val minWavenumber: Double,
    val maxWavenumber: Double,
    val description: String
) {
    X_H_STRETCHING(
        "X-H Stretching",
        2500.0,
        4000.0,
        "High frequency region characteristic of stretching vibrations of light hydrogen atoms (O-H, N-H, C-H, S-H)."
    ),
    TRIPLE_BOND(
        "Triple Bond",
        2000.0,
        2500.0,
        "Uncluttered diagnostic region characteristic of triple bonds (C≡C, C≡N) and cumulated double bonds (N=C=O, N=C=S, C=C=O)."
    ),
    DOUBLE_BOND(
        "Double Bond (Carbonyl & C=C)",
        1500.0,
        2000.0,
        "Crucial diagnostic region for carbonyls C=O (1650–1850 cm⁻¹), alkenes C=C, aromatics, and amides."
    ),
    FINGERPRINT(
        "Fingerprint Region",
        400.0,
        1500.0,
        "Complex region of single bond stretches (C-O, C-C, C-N, C-X) and bending vibrations specific to exact molecular identity."
    );

    companion object {
        fun fromWavenumber(wavenumber: Double): SpectralRegion {
            return when {
                wavenumber >= 2500.0 -> X_H_STRETCHING
                wavenumber >= 2000.0 -> TRIPLE_BOND
                wavenumber >= 1500.0 -> DOUBLE_BOND
                else -> FINGERPRINT
            }
        }
    }
}

/**
 * Fundamental vibrational motion types observed in infrared spectroscopy.
 */
enum class VibrationType(val label: String, val code: String) {
    STRETCH_ASYM("Asymmetric Stretch", "ν_as"),
    STRETCH_SYM("Symmetric Stretch", "ν_s"),
    STRETCH("Stretching", "ν"),
    BEND_IN_PLANE("In-Plane Bending (Scissoring/Rocking)", "δ"),
    BEND_OUT_OF_PLANE("Out-of-Plane Bending (Wagging/Twisting)", "γ"),
    DEFORMATION("Deformation / Skeletal Breathing", "δ_skel"),
    RING_BREATHING("Aromatic Ring Skeletal Breathing", "ν_Ar"),
    FERMI_RESONANCE("Fermi Resonance Doublet", "FR");

    companion object {
        fun fromLabel(raw: String): VibrationType {
            val lower = raw.lowercase()
            return when {
                "asym" in lower -> STRETCH_ASYM
                "sym" in lower -> STRETCH_SYM
                "out" in lower || "oop" in lower || "wag" in lower || "twist" in lower -> BEND_OUT_OF_PLANE
                "bend" in lower || "sciss" in lower || "rock" in lower -> BEND_IN_PLANE
                "ring" in lower || "breath" in lower || "skelet" in lower -> RING_BREATHING
                "fermi" in lower -> FERMI_RESONANCE
                "deform" in lower -> DEFORMATION
                else -> STRETCH
            }
        }
    }
}

/**
 * Comprehensive scientific bond assignment in IR/FTIR spectroscopy.
 *
 * NOTE: Scientific spectroscopy rule: A single wavenumber does not uniquely identify a compound.
 * Band assignments represent typical ranges with physical/chemical environment dependencies
 * (e.g. hydrogen bonding, conjugation, ring strain, resonance, inductive effects).
 */
data class BondAssignment(
    val id: String = UUID.randomUUID().toString(),
    val bond: String,                          // e.g. "C=O", "O-H", "N-H", "C-H", "C-O", "Si-O", "P-O", "S=O"
    val functionalGroup: String,               // e.g. "Aliphatic Ketone", "Ester", "Primary Alcohol"
    val vibrationType: VibrationType,          // Vibration classification
    val vibrationalMode: String,               // Specific mode: e.g. "C=O stretching", "O-H stretching (polymeric)"
    val minWavenumber: Double,                 // Low bound (cm⁻¹)
    val maxWavenumber: Double,                 // High bound (cm⁻¹)
    val typicalWavenumber: Double,             // Median / typical diagnostic peak position (cm⁻¹)
    val spectralRegion: SpectralRegion,        // Classification region
    val intensity: PeakIntensity,              // Strong, Medium, Weak, Variable
    val bandShape: String,                     // "Broad", "Sharp", "Doublet", "Multiplet"
    val possibleCompoundClasses: List<String>, // e.g. ["Ketones", "Aldehydes", "Esters"]
    val supportingPeaks: List<SupportingPeakRef> = emptyList(), // Associated peaks needed for confirmation
    val notes: String,                         // Molecular environment nuances, conjugation shift, H-bonding
    val visualDiagramAscii: String? = null,    // Structural diagram
    val isCustom: Boolean = false,             // User-created entry flag
    val userNotes: String? = null              // User custom annotations
) {
    fun containsWavenumber(wavenumber: Double, tolerance: Double = 0.0): Boolean {
        return wavenumber >= (minWavenumber - tolerance) && wavenumber <= (maxWavenumber + tolerance)
    }
}

/**
 * Associated or supporting peaks required to corroborate a functional group assignment.
 * For instance:
 * - Carbonyl ester (1735–1750 cm⁻¹) requires supporting C-O stretch (1150–1250 cm⁻¹).
 * - Aldehyde (1720–1740 cm⁻¹) requires supporting C-H Fermi doublet (~2720 and ~2820 cm⁻¹).
 * - Primary amine (3300–3500 cm⁻¹) requires supporting N-H bend (~1580–1650 cm⁻¹).
 */
data class SupportingPeakRef(
    val description: String,
    val expectedRange: String,
    val minWavenumber: Double,
    val maxWavenumber: Double,
    val diagnosticRole: String // "Required for confirmation", "Characteristic", "Secondary indicator"
)
