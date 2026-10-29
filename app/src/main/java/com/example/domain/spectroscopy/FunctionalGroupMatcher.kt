package com.example.domain.spectroscopy

import com.example.domain.model.FunctionalGroup
import com.example.domain.model.Peak
import com.example.domain.model.PeakIntensity
import kotlin.math.abs

interface FunctionalGroupMatcher {
    fun getAllFunctionalGroups(): List<FunctionalGroup>
    fun findMatchesForWavenumber(wavenumber: Double): List<FunctionalGroup>
    fun matchPeaks(peaks: List<Peak>): List<Peak>
}

class StandardFunctionalGroupMatcher : FunctionalGroupMatcher {

    private val dictionary: List<FunctionalGroup> = listOf(
        // Alcohols & Phenols
        FunctionalGroup(
            id = "oh_alcohol_free",
            name = "Alcohol / Phenol (Free O-H)",
            structure = "R-OH",
            category = "Alcohols & Phenols",
            minWavenumber = 3590.0,
            maxWavenumber = 3650.0,
            typicalPeakWavenumber = 3620.0,
            intensity = PeakIntensity.MEDIUM,
            bandShape = "Sharp",
            vibrationalMode = "O-H Stretch (non-H-bonded)",
            diagnosticTips = "Observed only in dilute non-polar solution or gas phase."
        ),
        FunctionalGroup(
            id = "oh_alcohol_bonded",
            name = "Alcohol (H-Bonded O-H)",
            structure = "R-OH…OH-R",
            category = "Alcohols & Phenols",
            minWavenumber = 3200.0,
            maxWavenumber = 3550.0,
            typicalPeakWavenumber = 3350.0,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Very Broad",
            vibrationalMode = "O-H Stretch (polymeric H-bond)",
            diagnosticTips = "Classic broad U-shaped band. Paired with strong C-O stretch at 1000-1150 cm⁻¹."
        ),
        FunctionalGroup(
            id = "oh_carboxylic",
            name = "Carboxylic Acid (O-H)",
            structure = "R-COOH",
            category = "Carbonyl Compounds",
            minWavenumber = 2500.0,
            maxWavenumber = 3300.0,
            typicalPeakWavenumber = 3000.0,
            intensity = PeakIntensity.STRONG,
            bandShape = "Extremely Broad",
            vibrationalMode = "O-H Stretch (dimeric)",
            diagnosticTips = "Extremely broad band overlapping C-H stretch region, with C=O at ~1710 cm⁻¹."
        ),
        // Amines & Amides
        FunctionalGroup(
            id = "nh_primary_amine",
            name = "Primary Amine (N-H Doublet)",
            structure = "R-NH₂",
            category = "Amines & Nitrogen",
            minWavenumber = 3300.0,
            maxWavenumber = 3500.0,
            typicalPeakWavenumber = 3400.0,
            intensity = PeakIntensity.MEDIUM,
            bandShape = "Doublet",
            vibrationalMode = "Asymmetric & Symmetric N-H Stretch",
            diagnosticTips = "Distinct two-pronged fork appearance from symmetric and asymmetric stretching."
        ),
        FunctionalGroup(
            id = "nh_secondary_amine",
            name = "Secondary Amine (N-H Singlet)",
            structure = "R₂N-H",
            category = "Amines & Nitrogen",
            minWavenumber = 3300.0,
            maxWavenumber = 3400.0,
            typicalPeakWavenumber = 3350.0,
            intensity = PeakIntensity.WEAK,
            bandShape = "Sharp Singlet",
            vibrationalMode = "N-H Stretch",
            diagnosticTips = "Single peak, sharper and weaker than alcohol O-H stretch."
        ),
        // Hydrocarbons (C-H)
        FunctionalGroup(
            id = "ch_alkyne_sp",
            name = "Terminal Alkyne (≡C-H)",
            structure = "R-C≡C-H",
            category = "Hydrocarbons",
            minWavenumber = 3280.0,
            maxWavenumber = 3340.0,
            typicalPeakWavenumber = 3300.0,
            intensity = PeakIntensity.STRONG,
            bandShape = "Sharp",
            vibrationalMode = "sp C-H Stretch",
            diagnosticTips = "Characteristic sharp spike above 3300 cm⁻¹, paired with C≡C at 2100-2260 cm⁻¹."
        ),
        FunctionalGroup(
            id = "ch_alkene_aromatic_sp2",
            name = "Alkene / Aromatic (=C-H)",
            structure = "=C-H / Ar-H",
            category = "Hydrocarbons",
            minWavenumber = 3000.0,
            maxWavenumber = 3100.0,
            typicalPeakWavenumber = 3050.0,
            intensity = PeakIntensity.MEDIUM,
            bandShape = "Sharp",
            vibrationalMode = "sp² C-H Stretch",
            diagnosticTips = "Appears strictly to the left of the 3000 cm⁻¹ dividing line."
        ),
        FunctionalGroup(
            id = "ch_alkane_sp3",
            name = "Aliphatic Alkane (-C-H)",
            structure = "-CH₃, -CH₂-",
            category = "Hydrocarbons",
            minWavenumber = 2840.0,
            maxWavenumber = 2980.0,
            typicalPeakWavenumber = 2930.0,
            intensity = PeakIntensity.STRONG,
            bandShape = "Multiple sharp peaks",
            vibrationalMode = "sp³ C-H Stretch",
            diagnosticTips = "Appears strictly to the right of the 3000 cm⁻¹ dividing line (CH₃ at ~2960/2870, CH₂ at ~2925/2850)."
        ),
        FunctionalGroup(
            id = "ch_aldehyde_fermi",
            name = "Aldehyde (C-H Fermi Doublet)",
            structure = "R-CH=O",
            category = "Carbonyl Compounds",
            minWavenumber = 2700.0,
            maxWavenumber = 2840.0,
            typicalPeakWavenumber = 2720.0,
            intensity = PeakIntensity.MEDIUM,
            bandShape = "Doublet (2820 & 2720 cm⁻¹)",
            vibrationalMode = "C-H Fermi Resonance",
            diagnosticTips = "Twin peaks at ~2820 and ~2720 cm⁻¹ confirm aldehyde presence along with C=O."
        ),
        // Triple Bonds & Cumulated Double Bonds
        FunctionalGroup(
            id = "nitrile_cn",
            name = "Nitrile (C≡N)",
            structure = "R-C≡N",
            category = "Amines & Nitrogen",
            minWavenumber = 2210.0,
            maxWavenumber = 2260.0,
            typicalPeakWavenumber = 2240.0,
            intensity = PeakIntensity.MEDIUM,
            bandShape = "Sharp",
            vibrationalMode = "C≡N Stretch",
            diagnosticTips = "Sharp medium-intensity band in the triple-bond region."
        ),
        FunctionalGroup(
            id = "alkyne_cc",
            name = "Internal/Terminal Alkyne (C≡C)",
            structure = "R-C≡C-R'",
            category = "Hydrocarbons",
            minWavenumber = 2100.0,
            maxWavenumber = 2260.0,
            typicalPeakWavenumber = 2150.0,
            intensity = PeakIntensity.VARIABLE,
            bandShape = "Sharp",
            vibrationalMode = "C≡C Stretch",
            diagnosticTips = "Weak or inactive in symmetrically substituted alkynes due to dipole symmetry."
        ),
        // Carbonyl Compounds (C=O Region 1650-1850 cm⁻¹)
        FunctionalGroup(
            id = "co_anhydride",
            name = "Acid Anhydride (C=O Doublet)",
            structure = "(RCO)₂O",
            category = "Carbonyl Compounds",
            minWavenumber = 1750.0,
            maxWavenumber = 1830.0,
            typicalPeakWavenumber = 1780.0,
            intensity = PeakIntensity.STRONG,
            bandShape = "Doublet",
            vibrationalMode = "Asymmetric & Symmetric C=O Stretch",
            diagnosticTips = "Two strong carbonyl peaks separated by ~60 cm⁻¹."
        ),
        FunctionalGroup(
            id = "co_ester",
            name = "Ester (C=O)",
            structure = "R-COO-R'",
            category = "Carbonyl Compounds",
            minWavenumber = 1735.0,
            maxWavenumber = 1755.0,
            typicalPeakWavenumber = 1740.0,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Sharp Strong",
            vibrationalMode = "C=O Stretch",
            diagnosticTips = "High wavenumber carbonyl paired with strong C-O stretch at 1150-1250 cm⁻¹."
        ),
        FunctionalGroup(
            id = "co_ketone",
            name = "Aliphatic Ketone (C=O)",
            structure = "R-CO-R'",
            category = "Carbonyl Compounds",
            minWavenumber = 1705.0,
            maxWavenumber = 1725.0,
            typicalPeakWavenumber = 1715.0,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Sharp Strong",
            vibrationalMode = "C=O Stretch",
            diagnosticTips = "Standard diagnostic ketone peak at 1715 cm⁻¹ (shifts to 1685 cm⁻¹ if conjugated)."
        ),
        FunctionalGroup(
            id = "co_aldehyde",
            name = "Aldehyde (C=O)",
            structure = "R-CH=O",
            category = "Carbonyl Compounds",
            minWavenumber = 1720.0,
            maxWavenumber = 1740.0,
            typicalPeakWavenumber = 1730.0,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Sharp Strong",
            vibrationalMode = "C=O Stretch",
            diagnosticTips = "Accompanied by C-H Fermi doublet at ~2720 & 2820 cm⁻¹."
        ),
        FunctionalGroup(
            id = "co_carboxylic_acid",
            name = "Carboxylic Acid (C=O)",
            structure = "R-COOH",
            category = "Carbonyl Compounds",
            minWavenumber = 1700.0,
            maxWavenumber = 1725.0,
            typicalPeakWavenumber = 1710.0,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Strong",
            vibrationalMode = "C=O Stretch",
            diagnosticTips = "Accompanied by enormous 2500-3300 cm⁻¹ O-H envelope."
        ),
        FunctionalGroup(
            id = "co_amide_i",
            name = "Amide I (C=O)",
            structure = "R-CO-NHR'",
            category = "Carbonyl Compounds",
            minWavenumber = 1630.0,
            maxWavenumber = 1690.0,
            typicalPeakWavenumber = 1660.0,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Strong",
            vibrationalMode = "Amide I (C=O Stretch)",
            diagnosticTips = "Lower frequency carbonyl due to nitrogen resonance donation; paired with Amide II (1550 cm⁻¹)."
        ),
        // Alkenes & Aromatics
        FunctionalGroup(
            id = "cc_alkene",
            name = "Alkene (C=C)",
            structure = "R₂C=CR₂",
            category = "Hydrocarbons",
            minWavenumber = 1620.0,
            maxWavenumber = 1680.0,
            typicalPeakWavenumber = 1650.0,
            intensity = PeakIntensity.VARIABLE,
            bandShape = "Medium / Variable",
            vibrationalMode = "C=C Stretch",
            diagnosticTips = "Weaker if symmetrical; conjugated alkenes shift down to ~1600 cm⁻¹."
        ),
        FunctionalGroup(
            id = "aromatic_ring",
            name = "Aromatic Ring (C=C Ring Stretch)",
            structure = "Ar-Ring",
            category = "Aromatics",
            minWavenumber = 1450.0,
            maxWavenumber = 1600.0,
            typicalPeakWavenumber = 1500.0,
            intensity = PeakIntensity.MEDIUM,
            bandShape = "Sharp Multiplet (1600, 1500, 1450 cm⁻¹)",
            vibrationalMode = "Aromatic C=C Skeletal Breathing",
            diagnosticTips = "Characteristic 2 to 4 sharp bands; 1600 and 1500 cm⁻¹ are classic diagnostic markers."
        ),
        // Nitro
        FunctionalGroup(
            id = "nitro_no2",
            name = "Nitro Group (-NO₂)",
            structure = "R-NO₂",
            category = "Amines & Nitrogen",
            minWavenumber = 1515.0,
            maxWavenumber = 1560.0,
            typicalPeakWavenumber = 1535.0,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Doublet with 1350 cm⁻¹",
            vibrationalMode = "Asymmetric & Symmetric N-O Stretch",
            diagnosticTips = "Two prominent strong bands at 1530-1550 cm⁻¹ and 1330-1360 cm⁻¹."
        ),
        // C-O Stretch
        FunctionalGroup(
            id = "co_single_bond",
            name = "C-O Single Bond (Alcohols / Ethers / Esters)",
            structure = "C-O",
            category = "Oxygen Compounds",
            minWavenumber = 1000.0,
            maxWavenumber = 1300.0,
            typicalPeakWavenumber = 1100.0,
            intensity = PeakIntensity.STRONG,
            bandShape = "Strong",
            vibrationalMode = "C-O Stretch",
            diagnosticTips = "Strong band in fingerprint region (1050 for 1° alcohol, 1100 for 2°, 1150 for 3°, 1200 for esters)."
        ),
        // Aromatic Out of Plane Bending
        FunctionalGroup(
            id = "aromatic_oop_mono",
            name = "Monosubstituted Benzene (C-H OOP)",
            structure = "C₆H₅-R",
            category = "Aromatics",
            minWavenumber = 690.0,
            maxWavenumber = 770.0,
            typicalPeakWavenumber = 750.0,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Doublet (~750 and ~690 cm⁻¹)",
            vibrationalMode = "Out-of-Plane C-H Bending",
            diagnosticTips = "Classic signature of monosubstituted benzene ring (e.g. polystyrene, toluene)."
        )
    )

    override fun getAllFunctionalGroups(): List<FunctionalGroup> = dictionary

    override fun findMatchesForWavenumber(wavenumber: Double): List<FunctionalGroup> {
        return dictionary.filter { group ->
            wavenumber in group.minWavenumber..group.maxWavenumber
        }.sortedBy { group ->
            abs(group.typicalPeakWavenumber - wavenumber)
        }
    }

    override fun matchPeaks(peaks: List<Peak>): List<Peak> {
        return peaks.map { peak ->
            val matches = findMatchesForWavenumber(peak.wavenumber)
            if (matches.isNotEmpty()) {
                val best = matches.first()
                val delta = abs(best.typicalPeakWavenumber - peak.wavenumber)
                val span = best.maxWavenumber - best.minWavenumber
                val confidence = (1.0 - (delta / span)).coerceIn(0.4, 0.98)
                peak.copy(
                    matchedFunctionalGroup = best.name,
                    confidence = confidence
                )
            } else {
                peak
            }
        }
    }
}
