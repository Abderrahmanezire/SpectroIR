package com.example.data.demo

import com.example.domain.model.Peak
import com.example.domain.model.ReferenceSpectrum
import com.example.domain.model.SignalUnit
import com.example.domain.model.Spectrum
import com.example.domain.model.SpectrumPoint
import com.example.domain.model.SpectrumSourceType
import com.example.domain.model.SpectrumType
import com.example.domain.model.WavenumberUnit
import com.example.domain.spectroscopy.StandardFunctionalGroupMatcher
import com.example.domain.spectroscopy.StandardPeakDetector
import kotlin.math.exp
import kotlin.math.sin

object DemoSpectraProvider {

    private val peakDetector = StandardPeakDetector()
    private val matcher = StandardFunctionalGroupMatcher()

    fun getAllDemoSpectra(): List<Spectrum> {
        return listOf(
            createPolystyreneDemo(),
            createAcetoneDemo(),
            createEthanolDemo(),
            createAspirinDemo(),
            createTolueneDemo(),
            createEthylAcetateDemo(),
            createBenzoicAcidDemo()
        )
    }

    fun getAllReferenceSpectra(): List<ReferenceSpectrum> {
        val polystyrene = createPolystyreneDemo()
        val acetone = createAcetoneDemo()
        val ethanol = createEthanolDemo()
        val aspirin = createAspirinDemo()
        val toluene = createTolueneDemo()
        val ethylAcetate = createEthylAcetateDemo()
        val benzoicAcid = createBenzoicAcidDemo()

        return listOf(
            ReferenceSpectrum(
                id = "ref_polystyrene",
                databaseIdentifier = "NIST-IR-PS-001",
                compoundName = "Polystyrene (PS Film Standard)",
                chemicalFormula = "(C₈H₈)ₙ",
                casNumber = "9003-53-6",
                materialClass = "Polymer",
                description = "Primary IUPAC standard for FTIR wavelength and wavenumber accuracy calibration. Exhibiting sharp aromatic C-H stretching, ring breathing, and monosubstituted benzene out-of-plane bending.",
                notes = "Calibrated on 0.05 mm film standard. NIST SRM 1921b reference match.",
                measurementMethod = "Transmission (Thin Film)",
                source = "Standard Reference Library (NIST / IUPAC Equivalent)",
                sampleInformation = "Biaxially oriented film standard, thickness ~0.05 mm",
                functionalGroups = listOf("Aromatic Ring", "Aliphatic C-H", "Monosubstituted Benzene"),
                spectralRegions = listOf("Aromatic C-H (3100-3000 cm⁻¹)", "Ring Breathing (1600-1450 cm⁻¹)", "Fingerprint (1050-690 cm⁻¹)"),
                characteristicPeaks = listOf(3082.0, 3060.0, 3026.0, 2924.0, 2850.0, 1601.0, 1492.0, 1452.0, 1028.0, 906.0, 756.0, 698.0),
                spectrum = polystyrene,
                isDemo = true,
                isUserCustom = false
            ),
            ReferenceSpectrum(
                id = "ref_acetone",
                databaseIdentifier = "REF-SOLV-ACT-002",
                compoundName = "Acetone (Propan-2-one)",
                chemicalFormula = "C₃H₆O",
                casNumber = "67-64-1",
                materialClass = "Solvent / Ketone",
                description = "Prototypical aliphatic ketone featuring an intense C=O carbonyl absorption band at 1715 cm⁻¹ and symmetric/asymmetric C-H methyl deformation modes.",
                notes = "Analytical grade >99.9% purity. Measured via single-reflection diamond ATR crystal.",
                measurementMethod = "ATR-FTIR (Diamond Crystal, 45° incidence)",
                source = "Standard Reference Library (Open Academic)",
                sampleInformation = "Neat liquid, 22°C, 1 atm",
                functionalGroups = listOf("Carbonyl (C=O)", "Methyl (C-H)", "Aliphatic Ketone"),
                spectralRegions = listOf("Carbonyl (1800-1650 cm⁻¹)", "C-H Stretch (3000-2850 cm⁻¹)", "C-C-C Skeletal (1250-1200 cm⁻¹)"),
                characteristicPeaks = listOf(2965.0, 1715.0, 1420.0, 1365.0, 1222.0, 530.0),
                spectrum = acetone,
                isDemo = true,
                isUserCustom = false
            ),
            ReferenceSpectrum(
                id = "ref_ethanol",
                databaseIdentifier = "REF-ALC-ETH-003",
                compoundName = "Ethanol (Ethyl Alcohol)",
                chemicalFormula = "C₂H₆O",
                casNumber = "64-17-5",
                materialClass = "Solvent / Alcohol",
                description = "Classic primary aliphatic alcohol characterized by a broad hydrogen-bonded O-H stretching band at ~3350 cm⁻¹, aliphatic C-H stretching, and intense C-O single bond stretch at 1050 cm⁻¹.",
                notes = "Anhydrous 99.8%. Characteristic hydrogen bonded broad envelope.",
                measurementMethod = "ATR-FTIR (Diamond Crystal)",
                source = "Standard Reference Library (Open Academic)",
                sampleInformation = "Neat liquid film, 21°C",
                functionalGroups = listOf("Hydroxyl (O-H)", "Primary Alcohol (C-O)", "Aliphatic C-H"),
                spectralRegions = listOf("O-H Hydrogen Bonded (3600-3200 cm⁻¹)", "C-H Stretch (2980-2850 cm⁻¹)", "C-O Stretch (1100-1000 cm⁻¹)"),
                characteristicPeaks = listOf(3350.0, 2975.0, 2885.0, 1380.0, 1088.0, 1050.0, 880.0),
                spectrum = ethanol,
                isDemo = true,
                isUserCustom = false
            ),
            ReferenceSpectrum(
                id = "ref_aspirin",
                databaseIdentifier = "REF-PHARM-ASA-004",
                compoundName = "Aspirin (Acetylsalicylic Acid)",
                chemicalFormula = "C₉H₈O₄",
                casNumber = "50-78-2",
                materialClass = "Pharmaceutical / Ester & Acid",
                description = "Dual carbonyl compound featuring distinct ester C=O stretch at 1754 cm⁻¹ and conjugated carboxylic acid C=O stretch at 1685 cm⁻¹, with broad acid O-H and ortho-substituted aromatic bands.",
                notes = "USP Reference Standard grade powder, pressed into KBr pellet at 10 tons.",
                measurementMethod = "Transmission (KBr Pellet, 1.0 mg / 200 mg)",
                source = "Pharmacopeia Reference Standards Collection",
                sampleInformation = "Crystalline powder, 99.5% purity",
                functionalGroups = listOf("Ester Carbonyl (C=O)", "Carboxylic Acid (C=O & O-H)", "Aromatic Ring"),
                spectralRegions = listOf("Carbonyl Doublet (1760-1680 cm⁻¹)", "Acid O-H (3300-2500 cm⁻¹)", "Fingerprint (1350-1150 cm⁻¹)"),
                characteristicPeaks = listOf(1754.0, 1685.0, 1606.0, 1458.0, 1306.0, 1222.0, 1188.0, 918.0),
                spectrum = aspirin,
                isDemo = true,
                isUserCustom = false
            ),
            ReferenceSpectrum(
                id = "ref_toluene",
                databaseIdentifier = "REF-AROM-TOL-005",
                compoundName = "Toluene (Methylbenzene)",
                chemicalFormula = "C₇H₈",
                casNumber = "108-88-3",
                materialClass = "Aromatic Hydrocarbon",
                description = "Monosubstituted aromatic hydrocarbon exhibiting aromatic =C-H stretch at 3030 cm⁻¹, aliphatic methyl C-H stretches, aromatic quadrant ring stretching at 1605 and 1495 cm⁻¹, and classic 694/728 cm⁻¹ bending.",
                notes = "Spectroscopy grade >99.8%. Monosubstituted aromatic marker bands.",
                measurementMethod = "ATR-FTIR (Diamond Crystal)",
                source = "Standard Reference Library (Open Academic)",
                sampleInformation = "Neat liquid, 20°C",
                functionalGroups = listOf("Aromatic Ring", "Alkyl C-H", "Monosubstituted Benzene"),
                spectralRegions = listOf("Aromatic C-H (3100-3000 cm⁻¹)", "Ring Stretching (1605-1490 cm⁻¹)", "Out-of-Plane Bending (750-680 cm⁻¹)"),
                characteristicPeaks = listOf(3030.0, 2925.0, 1605.0, 1495.0, 1080.0, 1030.0, 728.0, 694.0),
                spectrum = toluene,
                isDemo = true,
                isUserCustom = false
            ),
            ReferenceSpectrum(
                id = "ref_ethyl_acetate",
                databaseIdentifier = "REF-EST-ETA-006",
                compoundName = "Ethyl Acetate",
                chemicalFormula = "C₄H₈O₂",
                casNumber = "141-78-6",
                materialClass = "Solvent / Ester",
                description = "Classic aliphatic ester characterized by intense ester C=O carbonyl stretch at 1738 cm⁻¹ and strong asymmetric C-O-C stretch at 1240 cm⁻¹.",
                notes = "HPLC grade 99.7%. Intense ester fingerprint doublet.",
                measurementMethod = "ATR-FTIR (Diamond Crystal)",
                source = "Standard Reference Library (Open Academic)",
                sampleInformation = "Neat liquid, 21°C",
                functionalGroups = listOf("Ester Carbonyl (C=O)", "Ester C-O", "Aliphatic C-H"),
                spectralRegions = listOf("Carbonyl (1750-1730 cm⁻¹)", "C-O-C Stretch (1260-1200 cm⁻¹)", "C-H Stretch (2990-2850 cm⁻¹)"),
                characteristicPeaks = listOf(2985.0, 1738.0, 1445.0, 1372.0, 1240.0, 1048.0),
                spectrum = ethylAcetate,
                isDemo = true,
                isUserCustom = false
            ),
            ReferenceSpectrum(
                id = "ref_benzoic_acid",
                databaseIdentifier = "REF-ACID-BZA-007",
                compoundName = "Benzoic Acid",
                chemicalFormula = "C₇H₆O₂",
                casNumber = "65-85-0",
                materialClass = "Aromatic Carboxylic Acid",
                description = "Aromatic carboxylic acid exhibiting an extremely broad hydrogen-bonded dimeric O-H envelope (2500–3300 cm⁻¹), conjugated C=O carbonyl at 1685 cm⁻¹, and ring stretches.",
                notes = "Analytical grade. Classic carboxylic acid hydrogen-bonded dimer.",
                measurementMethod = "Transmission (KBr Pellet)",
                source = "Standard Reference Library (Open Academic)",
                sampleInformation = "Crystalline solid pressed in KBr",
                functionalGroups = listOf("Carboxylic Acid (C=O & O-H)", "Aromatic Ring"),
                spectralRegions = listOf("Dimeric O-H (3300-2500 cm⁻¹)", "Conjugated Carbonyl (1700-1680 cm⁻¹)", "Ring Bending (750-700 cm⁻¹)"),
                characteristicPeaks = listOf(3070.0, 2800.0, 2650.0, 1685.0, 1600.0, 1425.0, 1290.0, 935.0, 708.0),
                spectrum = benzoicAcid,
                isDemo = true,
                isUserCustom = false
            )
        )
    }

    fun createPolystyreneDemo(): Spectrum {
        val bands = listOf(
            Band(3082.0, 25.0, 12.0),
            Band(3060.0, 48.0, 15.0),
            Band(3026.0, 42.0, 14.0),
            Band(2924.0, 58.0, 22.0),
            Band(2850.0, 46.0, 18.0),
            Band(1945.0, 15.0, 30.0),
            Band(1872.0, 12.0, 28.0),
            Band(1802.0, 14.0, 26.0),
            Band(1601.0, 68.0, 12.0),
            Band(1492.0, 64.0, 14.0),
            Band(1452.0, 60.0, 16.0),
            Band(1372.0, 22.0, 20.0),
            Band(1182.0, 28.0, 20.0),
            Band(1069.0, 32.0, 18.0),
            Band(1028.0, 62.0, 14.0),
            Band(906.0, 52.0, 16.0),
            Band(756.0, 72.0, 20.0),
            Band(698.0, 78.0, 18.0)
        )

        return synthesizeSpectrum(
            id = "demo_polystyrene",
            name = "Polystyrene (PS Film Standard)",
            bands = bands,
            baseline = 92.0,
            noiseAmp = 0.3
        )
    }

    fun createAcetoneDemo(): Spectrum {
        val bands = listOf(
            Band(2965.0, 38.0, 35.0),
            Band(1715.0, 85.0, 24.0),
            Band(1420.0, 42.0, 18.0),
            Band(1365.0, 65.0, 16.0),
            Band(1222.0, 70.0, 22.0),
            Band(530.0, 35.0, 28.0)
        )

        return synthesizeSpectrum(
            id = "demo_acetone",
            name = "Acetone (Demo)",
            bands = bands,
            baseline = 96.0,
            noiseAmp = 0.25
        )
    }

    fun createEthanolDemo(): Spectrum {
        val bands = listOf(
            Band(3350.0, 75.0, 180.0),
            Band(2975.0, 55.0, 28.0),
            Band(2885.0, 40.0, 24.0),
            Band(1380.0, 35.0, 22.0),
            Band(1088.0, 68.0, 20.0),
            Band(1050.0, 82.0, 18.0),
            Band(880.0, 45.0, 25.0)
        )

        return synthesizeSpectrum(
            id = "demo_ethanol",
            name = "Ethanol (Demo)",
            bands = bands,
            baseline = 95.0,
            noiseAmp = 0.4
        )
    }

    fun createAspirinDemo(): Spectrum {
        val bands = listOf(
            Band(3000.0, 35.0, 120.0),
            Band(1754.0, 78.0, 18.0),
            Band(1685.0, 72.0, 20.0),
            Band(1606.0, 58.0, 15.0),
            Band(1458.0, 48.0, 16.0),
            Band(1306.0, 54.0, 18.0),
            Band(1222.0, 68.0, 20.0),
            Band(1188.0, 65.0, 18.0),
            Band(918.0, 42.0, 20.0)
        )

        return synthesizeSpectrum(
            id = "demo_aspirin",
            name = "Aspirin (Demo)",
            bands = bands,
            baseline = 93.0,
            noiseAmp = 0.35
        )
    }

    fun createTolueneDemo(): Spectrum {
        val bands = listOf(
            Band(3030.0, 42.0, 18.0),
            Band(2925.0, 48.0, 22.0),
            Band(1605.0, 55.0, 14.0),
            Band(1495.0, 62.0, 14.0),
            Band(1080.0, 32.0, 18.0),
            Band(1030.0, 45.0, 16.0),
            Band(728.0, 75.0, 18.0),
            Band(694.0, 78.0, 16.0)
        )

        return synthesizeSpectrum(
            id = "demo_toluene",
            name = "Toluene (Demo)",
            bands = bands,
            baseline = 94.0,
            noiseAmp = 0.3
        )
    }

    fun createEthylAcetateDemo(): Spectrum {
        val bands = listOf(
            Band(2985.0, 45.0, 25.0),
            Band(1738.0, 84.0, 20.0),
            Band(1445.0, 36.0, 18.0),
            Band(1372.0, 58.0, 16.0),
            Band(1240.0, 80.0, 22.0),
            Band(1048.0, 64.0, 20.0)
        )

        return synthesizeSpectrum(
            id = "demo_ethyl_acetate",
            name = "Ethyl Acetate (Demo)",
            bands = bands,
            baseline = 95.0,
            noiseAmp = 0.25
        )
    }

    fun createBenzoicAcidDemo(): Spectrum {
        val bands = listOf(
            Band(3070.0, 32.0, 28.0),
            Band(2800.0, 45.0, 320.0),
            Band(2650.0, 42.0, 220.0),
            Band(2560.0, 38.0, 180.0),
            Band(1685.0, 82.0, 22.0),
            Band(1600.0, 46.0, 16.0),
            Band(1452.0, 52.0, 18.0),
            Band(1425.0, 50.0, 20.0),
            Band(1290.0, 65.0, 24.0),
            Band(935.0, 40.0, 35.0),
            Band(708.0, 76.0, 18.0)
        )

        return synthesizeSpectrum(
            id = "demo_benzoic_acid",
            name = "Benzoic Acid (Demo)",
            bands = bands,
            baseline = 93.0,
            noiseAmp = 0.35
        )
    }

    private data class Band(val center: Double, val depthPercent: Double, val width: Double)

    private fun synthesizeSpectrum(
        id: String,
        name: String,
        bands: List<Band>,
        baseline: Double = 95.0,
        noiseAmp: Double = 0.3
    ): Spectrum {
        val points = mutableListOf<SpectrumPoint>()
        var wn = 400.0
        while (wn <= 4000.0) {
            var dip = 0.0
            for (band in bands) {
                val dx = wn - band.center
                val lorentz = 1.0 / (1.0 + (dx / (band.width / 2.0)) * (dx / (band.width / 2.0)))
                val gauss = exp(-0.5 * (dx / (band.width / 2.355)) * (dx / (band.width / 2.355)))
                val profile = 0.6 * lorentz + 0.4 * gauss
                dip += band.depthPercent * profile
            }

            val pseudoNoise = (sin(wn * 1.7) * 0.5 + sin(wn * 3.3) * 0.3 + sin(wn * 0.1) * 0.2) * noiseAmp
            val signal = (baseline - dip + pseudoNoise).coerceIn(1.0, 100.0)
            points.add(SpectrumPoint(wn, signal))
            wn += 4.0
        }

        val baseSpectrum = Spectrum(
            id = id,
            name = name,
            sourceType = SpectrumSourceType.DEMO,
            xUnit = WavenumberUnit.CM_MINUS_1.symbol,
            yUnit = SignalUnit.TRANSMITTANCE_PERCENT.symbol,
            spectrumType = SpectrumType.Transmittance,
            points = points,
            metadata = mapOf(
                "Source" to "Demonstration Synthetic FTIR Profile",
                "Resolution" to "4.0 cm⁻¹",
                "Range" to "400.0 - 4000.0 cm⁻¹",
                "Note" to "Clearly labeled demo dataset for development & verification."
            ),
            createdAt = System.currentTimeMillis()
        )

        val rawPeaks = peakDetector.detectPeaks(baseSpectrum, minProminence = 10.0)
        val matchedPeaks = matcher.matchPeaks(rawPeaks)

        return baseSpectrum.copy(detectedPeaks = matchedPeaks)
    }
}
