package com.example.domain.model

enum class WavenumberUnit(val symbol: String, val displayName: String) {
    CM_MINUS_1("cm⁻¹", "Wavenumber (cm⁻¹)"),
    MICROMETERS("µm", "Wavelength (µm)")
}

enum class SignalUnit(val symbol: String, val displayName: String) {
    TRANSMITTANCE_PERCENT("%T", "Transmittance (%T)"),
    ABSORBANCE("A.U.", "Absorbance (A)"),
    ARBITRARY_INTENSITY("Counts", "Arbitrary Intensity")
}

enum class SpectrumType(val label: String, val defaultUnit: SignalUnit) {
    Transmittance("Transmittance (%T)", SignalUnit.TRANSMITTANCE_PERCENT),
    Absorbance("Absorbance (A)", SignalUnit.ABSORBANCE),
    Intensity("Intensity", SignalUnit.ARBITRARY_INTENSITY),
    Unknown("Unknown", SignalUnit.ARBITRARY_INTENSITY);

    /**
     * In IR spectroscopy:
     * - Absorbance has positive absorption peaks (values go UP).
     * - Transmittance has absorption bands as transmission troughs (values dip DOWN).
     */
    val isAbsorptionPeakUpward: Boolean
        get() = this == Absorbance
}

enum class SpectrumSourceType(val label: String) {
    DEMO("Demo Spectrum"),
    FILE_IMPORT("Imported File"),
    CAMERA_CAPTURE("Camera Capture"),
    DIGITIZED("Digitized Plot"),
    SYNTHETIC("Synthetic Reference")
}
