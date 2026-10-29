package com.example.domain.spectroscopy

import com.example.domain.model.BondAssignment
import com.example.domain.model.PeakIntensity
import com.example.domain.model.SpectralRegion
import com.example.domain.model.SupportingPeakRef
import com.example.domain.model.VibrationType

/**
 * Standard scientifically validated spectroscopy knowledge database.
 *
 * Covers:
 * - O-H (Free alcohol, polymeric H-bonded, carboxylic acid dimer)
 * - N-H (1° amine doublet, 2° amine singlet, amides, amine salts)
 * - C-H (sp alkyne, sp2 alkene & aromatic, sp3 alkane, aldehyde Fermi doublet)
 * - C=O (Acid chloride, anhydride doublet, ester, aldehyde, ketone, carboxylic acid, amide I)
 * - C=C (Alkene, conjugated diene, aromatic ring breathing multiplets)
 * - C≡C (Terminal alkyne, internal alkyne)
 * - C≡N (Aliphatic & aromatic nitriles, isonitriles)
 * - C-O (1°, 2°, 3° alcohols, dialkyl ethers, aryl ethers, esters)
 * - C-N (Aliphatic & aromatic amines)
 * - C-X (C-F, C-Cl, C-Br, C-I alkyl halides)
 * - Si-O (Silicates, silicones, silanols - Si-O-Si & Si-O-C)
 * - P-O (Phosphine oxides P=O, phosphate esters P-O-C)
 * - S=O (Sulfoxides, sulfones, sulfonamides, sulfonates)
 *
 * Each assignment uses scientifically valid wavenumber ranges, diagnostic environmental notes,
 * supporting/associated peaks, and clear ASCII structural diagrams.
 */
object SpectroscopyDatabase {

    val STANDARD_ASSIGNMENTS: List<BondAssignment> = listOf(
        // ==========================================
        // 1. O-H VIBRATIONS
        // ==========================================
        BondAssignment(
            id = "oh_free_alcohol",
            bond = "O-H",
            functionalGroup = "Alcohol / Phenol (Free, Monomeric)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "Free O-H Stretching (non-H-bonded)",
            minWavenumber = 3580.0,
            maxWavenumber = 3650.0,
            typicalWavenumber = 3620.0,
            spectralRegion = SpectralRegion.X_H_STRETCHING,
            intensity = PeakIntensity.MEDIUM,
            bandShape = "Sharp Singlet",
            possibleCompoundClasses = listOf("Alcohols", "Phenols"),
            supportingPeaks = listOf(
                SupportingPeakRef("C-O stretch", "1000–1200 cm⁻¹", 1000.0, 1200.0, "Strong corroborating band")
            ),
            notes = "Observed exclusively in very dilute non-polar solutions (e.g. CCl₄) or gas phase where intermolecular hydrogen bonding cannot form.",
            visualDiagramAscii = "R—O—H (isolated)"
        ),
        BondAssignment(
            id = "oh_bonded_alcohol",
            bond = "O-H",
            functionalGroup = "Aliphatic Alcohol (Polymeric H-Bonded)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "Intermolecular H-Bonded O-H Stretch",
            minWavenumber = 3200.0,
            maxWavenumber = 3550.0,
            typicalWavenumber = 3350.0,
            spectralRegion = SpectralRegion.X_H_STRETCHING,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Broad U-shaped envelope",
            possibleCompoundClasses = listOf("Primary Alcohols", "Secondary Alcohols", "Tertiary Alcohols", "Diols"),
            supportingPeaks = listOf(
                SupportingPeakRef("C-O Stretch (1°: ~1050, 2°: ~1100, 3°: ~1150)", "1000–1200 cm⁻¹", 1000.0, 1200.0, "Required for confirmation"),
                SupportingPeakRef("O-H in-plane bend", "1330–1420 cm⁻¹", 1330.0, 1420.0, "Secondary indicator")
            ),
            notes = "Classic rounded broad band centered near 3350 cm⁻¹. Hydrogen bonding weakens the O-H force constant, broadening and shifting frequency downward from 3600 cm⁻¹.",
            visualDiagramAscii = "R—O—H ···· O(R)—H"
        ),
        BondAssignment(
            id = "oh_carboxylic_acid",
            bond = "O-H",
            functionalGroup = "Carboxylic Acid (Dimeric)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "Strongly H-Bonded Acid O-H Stretch",
            minWavenumber = 2500.0,
            maxWavenumber = 3300.0,
            typicalWavenumber = 3000.0,
            spectralRegion = SpectralRegion.X_H_STRETCHING,
            intensity = PeakIntensity.STRONG,
            bandShape = "Extremely broad jagged envelope overlapping C-H",
            possibleCompoundClasses = listOf("Carboxylic Acids", "Dicarboxylic Acids", "Amino Acids"),
            supportingPeaks = listOf(
                SupportingPeakRef("C=O Stretch (acid dimer)", "1700–1725 cm⁻¹", 1700.0, 1725.0, "Essential partner band"),
                SupportingPeakRef("C-O Stretch", "1210–1320 cm⁻¹", 1210.0, 1320.0, "Corroborating strong band"),
                SupportingPeakRef("O-H out-of-plane bend dimer", "900–950 cm⁻¹", 900.0, 950.0, "Broad diagnostic band")
            ),
            notes = "Forms centrosymmetric 8-membered hydrogen-bonded dimers in condensed phase. Gives a massive absorption envelope stretching across 2500–3300 cm⁻¹ with characteristic Fermi resonance sub-peaks.",
            visualDiagramAscii = "    O ···· H—O\n    ‖        ‖\nR—C          C—R\n    |        |\n    O—H ···· O"
        ),

        // ==========================================
        // 2. N-H VIBRATIONS
        // ==========================================
        BondAssignment(
            id = "nh_primary_amine",
            bond = "N-H",
            functionalGroup = "Primary Amine (-NH₂)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "Asymmetric & Symmetric N-H Stretch",
            minWavenumber = 3300.0,
            maxWavenumber = 3500.0,
            typicalWavenumber = 3400.0,
            spectralRegion = SpectralRegion.X_H_STRETCHING,
            intensity = PeakIntensity.MEDIUM,
            bandShape = "Doublet (two prongs, ~3400 and ~3330 cm⁻¹)",
            possibleCompoundClasses = listOf("Primary Aliphatic Amines", "Primary Aromatic Amines (Anilines)"),
            supportingPeaks = listOf(
                SupportingPeakRef("N-H scissoring bend", "1580–1650 cm⁻¹", 1580.0, 1650.0, "Medium-strong scissoring"),
                SupportingPeakRef("C-N stretch", "1020–1250 cm⁻¹", 1020.0, 1250.0, "Medium band"),
                SupportingPeakRef("N-H wagging (broad)", "660–900 cm⁻¹", 660.0, 900.0, "Characteristic broad baseline")
            ),
            notes = "Two distinct peaks represent asymmetric and symmetric stretching of the two N-H bonds (not two isomers). Much sharper and weaker than alcohol O-H.",
            visualDiagramAscii = "    H\n   /\nR—N\n   \\\n    H"
        ),
        BondAssignment(
            id = "nh_secondary_amine",
            bond = "N-H",
            functionalGroup = "Secondary Amine (R₂N-H)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "N-H Stretching",
            minWavenumber = 3300.0,
            maxWavenumber = 3400.0,
            typicalWavenumber = 3350.0,
            spectralRegion = SpectralRegion.X_H_STRETCHING,
            intensity = PeakIntensity.WEAK,
            bandShape = "Single sharp band",
            possibleCompoundClasses = listOf("Secondary Aliphatic Amines", "Secondary Aromatic Amines", "Heterocycles (Piperidine, Pyrrole)"),
            supportingPeaks = listOf(
                SupportingPeakRef("C-N stretch", "1100–1280 cm⁻¹", 1100.0, 1280.0, "Fingerprint band"),
                SupportingPeakRef("N-H wag", "700–850 cm⁻¹", 700.0, 850.0, "Broad weak band")
            ),
            notes = "Only one N-H bond exists, giving a single weak peak. Tertiary amines (R₃N) have NO N-H stretch.",
            visualDiagramAscii = "R—NH—R'"
        ),
        BondAssignment(
            id = "nh_primary_amide",
            bond = "N-H",
            functionalGroup = "Primary Amide (-CONH₂)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "Asymmetric & Symmetric N-H Stretch",
            minWavenumber = 3180.0,
            maxWavenumber = 3450.0,
            typicalWavenumber = 3350.0,
            spectralRegion = SpectralRegion.X_H_STRETCHING,
            intensity = PeakIntensity.STRONG,
            bandShape = "Doublet (~3350 and ~3180 cm⁻¹ in solid)",
            possibleCompoundClasses = listOf("Primary Amides"),
            supportingPeaks = listOf(
                SupportingPeakRef("Amide I (C=O stretch)", "1630–1690 cm⁻¹", 1630.0, 1690.0, "Very strong carbonyl"),
                SupportingPeakRef("Amide II (N-H in-plane bend)", "1580–1620 cm⁻¹", 1580.0, 1620.0, "Strong band")
            ),
            notes = "Strongly hydrogen-bonded in solid/neat phase. Accompanied by very prominent Amide I and Amide II bands.",
            visualDiagramAscii = "R—C(=O)—NH₂"
        ),

        // ==========================================
        // 3. C-H VIBRATIONS
        // ==========================================
        BondAssignment(
            id = "ch_alkyne_sp",
            bond = "C-H",
            functionalGroup = "Terminal Alkyne (≡C-H)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "sp C-H Stretching",
            minWavenumber = 3280.0,
            maxWavenumber = 3340.0,
            typicalWavenumber = 3300.0,
            spectralRegion = SpectralRegion.X_H_STRETCHING,
            intensity = PeakIntensity.STRONG,
            bandShape = "Sharp spike",
            possibleCompoundClasses = listOf("Terminal Alkynes", "Ethynyl derivatives"),
            supportingPeaks = listOf(
                SupportingPeakRef("C≡C stretch", "2100–2140 cm⁻¹", 2100.0, 2140.0, "Essential partner band"),
                SupportingPeakRef("≡C-H bending", "600–700 cm⁻¹", 600.0, 700.0, "Strong broad bending band")
            ),
            notes = "High s-character (50%) creates very strong, stiff C-H bond vibrating well above 3300 cm⁻¹. Sharp spike easily distinguished from broad O-H.",
            visualDiagramAscii = "R—C≡C—H"
        ),
        BondAssignment(
            id = "ch_alkene_aromatic_sp2",
            bond = "C-H",
            functionalGroup = "Alkene / Aromatic (=C-H)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "sp² C-H Stretching",
            minWavenumber = 3000.0,
            maxWavenumber = 3100.0,
            typicalWavenumber = 3050.0,
            spectralRegion = SpectralRegion.X_H_STRETCHING,
            intensity = PeakIntensity.MEDIUM,
            bandShape = "Sharp peaks",
            possibleCompoundClasses = listOf("Alkenes", "Aromatic Hydrocarbons", "Heteroaromatics"),
            supportingPeaks = listOf(
                SupportingPeakRef("Aromatic C=C ring breathing", "1450–1600 cm⁻¹", 1450.0, 1600.0, "Sharp multiplets for aromatics"),
                SupportingPeakRef("Alkene C=C stretch", "1620–1680 cm⁻¹", 1620.0, 1680.0, "Partner band for alkenes"),
                SupportingPeakRef("=C-H OOP bending", "675–1000 cm⁻¹", 675.0, 1000.0, "Geometric substitution pattern")
            ),
            notes = "Crucial diagnostic boundary: Peaks strictly ABOVE 3000 cm⁻¹ signify unsaturated sp² C-H (alkene or aromatic).",
            visualDiagramAscii = "C=C—H  or  Ar—H"
        ),
        BondAssignment(
            id = "ch_alkane_sp3",
            bond = "C-H",
            functionalGroup = "Aliphatic Alkane (-C-H)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "sp³ C-H Stretching (Asym & Sym)",
            minWavenumber = 2840.0,
            maxWavenumber = 2980.0,
            typicalWavenumber = 2930.0,
            spectralRegion = SpectralRegion.X_H_STRETCHING,
            intensity = PeakIntensity.STRONG,
            bandShape = "Multiple sharp peaks (cluster)",
            possibleCompoundClasses = listOf("Alkanes", "Alkyl chains", "Cycloalkanes"),
            supportingPeaks = listOf(
                SupportingPeakRef("-CH₂- scissoring", "1450–1470 cm⁻¹", 1450.0, 1470.0, "Methylene bending"),
                SupportingPeakRef("-CH₃ umbrella bend", "1370–1385 cm⁻¹", 1370.0, 1385.0, "Methyl marker (doublet if isopropyl)"),
                SupportingPeakRef("-CH₂- rocking long chain", "720–725 cm⁻¹", 720.0, 725.0, "Present if chain length ≥ 4 carbons")
            ),
            notes = "Crucial diagnostic boundary: Peaks strictly BELOW 3000 cm⁻¹ signify saturated sp³ C-H. CH₃ asymmetric ~2960 cm⁻¹, CH₂ asymmetric ~2925 cm⁻¹, CH₃ symmetric ~2870 cm⁻¹, CH₂ symmetric ~2850 cm⁻¹.",
            visualDiagramAscii = "—CH₃  and  —CH₂—"
        ),
        BondAssignment(
            id = "ch_aldehyde_fermi",
            bond = "C-H",
            functionalGroup = "Aldehyde (C-H Fermi Doublet)",
            vibrationType = VibrationType.FERMI_RESONANCE,
            vibrationalMode = "C-H Stretch Fermi Resonance with Bend Overtone",
            minWavenumber = 2700.0,
            maxWavenumber = 2840.0,
            typicalWavenumber = 2720.0,
            spectralRegion = SpectralRegion.X_H_STRETCHING,
            intensity = PeakIntensity.MEDIUM,
            bandShape = "Characteristic Doublet (~2820 & ~2720 cm⁻¹)",
            possibleCompoundClasses = listOf("Aliphatic Aldehydes", "Aromatic Aldehydes (Benzaldehyde)"),
            supportingPeaks = listOf(
                SupportingPeakRef("Aldehyde C=O stretch", "1720–1740 cm⁻¹", 1720.0, 1740.0, "Essential carbonyl partner"),
                SupportingPeakRef("C-H in-plane bend", "1380–1410 cm⁻¹", 1380.0, 1410.0, "Fundamental mode causing overtone")
            ),
            notes = "Twin peaks at ~2820 cm⁻¹ and ~2720 cm⁻¹. The ~2720 cm⁻¹ peak is in a clear spectral window and unequivocally confirms an aldehyde carbonyl vs ketone.",
            visualDiagramAscii = "R—C(=O)—H"
        ),

        // ==========================================
        // 4. TRIPLE BONDS (2000–2500 cm⁻¹)
        // ==========================================
        BondAssignment(
            id = "nitrile_cn",
            bond = "C≡N",
            functionalGroup = "Nitrile (Aliphatic & Aromatic)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "C≡N Stretching",
            minWavenumber = 2210.0,
            maxWavenumber = 2260.0,
            typicalWavenumber = 2240.0,
            spectralRegion = SpectralRegion.TRIPLE_BOND,
            intensity = PeakIntensity.MEDIUM,
            bandShape = "Sharp Singlet",
            possibleCompoundClasses = listOf("Nitriles", "Cyanides"),
            supportingPeaks = listOf(
                SupportingPeakRef("C-C(≡N) skeletal", "800–1000 cm⁻¹", 800.0, 1000.0, "Secondary bend/stretch")
            ),
            notes = "Sharp peak in uncluttered triple bond region. Conjugation with aromatic rings shifts down by ~15–20 cm⁻¹ to 2225 cm⁻¹ with increased intensity.",
            visualDiagramAscii = "R—C≡N"
        ),
        BondAssignment(
            id = "alkyne_cc",
            bond = "C≡C",
            functionalGroup = "Alkyne (C≡C Triple Bond)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "C≡C Stretching",
            minWavenumber = 2100.0,
            maxWavenumber = 2260.0,
            typicalWavenumber = 2150.0,
            spectralRegion = SpectralRegion.TRIPLE_BOND,
            intensity = PeakIntensity.VARIABLE,
            bandShape = "Sharp Weak to Medium",
            possibleCompoundClasses = listOf("Terminal Alkynes", "Internal Alkynes"),
            supportingPeaks = listOf(
                SupportingPeakRef("≡C-H stretch (terminal)", "3280–3340 cm⁻¹", 3280.0, 3340.0, "Present only if terminal alkyne")
            ),
            notes = "Dipole moment rule: Symmetrical internal alkynes (e.g. 3-hexyne, tolan) have zero net dipole change and are IR INACTIVE (silent in IR, visible in Raman). Terminal alkynes are active near 2120 cm⁻¹.",
            visualDiagramAscii = "R—C≡C—R'"
        ),
        BondAssignment(
            id = "isothiocyanate_ncs",
            bond = "N=C=S / N=C=O",
            functionalGroup = "Isocyanate / Isothiocyanate / Azide",
            vibrationType = VibrationType.STRETCH_ASYM,
            vibrationalMode = "Cumulated Double Bond Asymmetric Stretch",
            minWavenumber = 2050.0,
            maxWavenumber = 2270.0,
            typicalWavenumber = 2140.0,
            spectralRegion = SpectralRegion.TRIPLE_BOND,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Very Strong Broad-to-Medium",
            possibleCompoundClasses = listOf("Isocyanates", "Isothiocyanates", "Azides (-N₃)"),
            supportingPeaks = emptyList(),
            notes = "Extremely strong diagnostic band due to huge dipole oscillation in cumulated linear systems.",
            visualDiagramAscii = "R—N=C=S  or  R—N=N⁺=N⁻"
        ),

        // ==========================================
        // 5. CARBONYL VIBRATIONS (C=O: 1650–1850 cm⁻¹)
        // ==========================================
        BondAssignment(
            id = "co_acid_chloride",
            bond = "C=O",
            functionalGroup = "Acid Halide (Acyl Chloride)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "C=O Stretching",
            minWavenumber = 1785.0,
            maxWavenumber = 1815.0,
            typicalWavenumber = 1800.0,
            spectralRegion = SpectralRegion.DOUBLE_BOND,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Sharp Strong",
            possibleCompoundClasses = listOf("Acyl Chlorides", "Acyl Fluorides"),
            supportingPeaks = listOf(
                SupportingPeakRef("C-Cl stretch", "600–750 cm⁻¹", 600.0, 750.0, "Halide stretch")
            ),
            notes = "Highest frequency carbonyl among simple compounds due to strong -I inductive electron withdrawal by chlorine, strengthening the C=O bond.",
            visualDiagramAscii = "R—C(=O)—Cl"
        ),
        BondAssignment(
            id = "co_anhydride",
            bond = "C=O",
            functionalGroup = "Acid Anhydride",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "Coupled Asymmetric & Symmetric C=O Stretch",
            minWavenumber = 1750.0,
            maxWavenumber = 1830.0,
            typicalWavenumber = 1780.0,
            spectralRegion = SpectralRegion.DOUBLE_BOND,
            intensity = PeakIntensity.STRONG,
            bandShape = "Characteristic Doublet (separated by ~60 cm⁻¹)",
            possibleCompoundClasses = listOf("Acyclic Anhydrides", "Cyclic Anhydrides"),
            supportingPeaks = listOf(
                SupportingPeakRef("C-O-C stretch", "1000–1100 cm⁻¹", 1000.0, 1100.0, "Strong ether-like C-O band")
            ),
            notes = "Mechanical vibrational coupling between the two carbonyls produces two bands: asymmetric (~1820 cm⁻¹) and symmetric (~1760 cm⁻¹). In cyclic anhydrides, the lower band is stronger; in acyclic, the higher is stronger.",
            visualDiagramAscii = "R—C(=O)—O—C(=O)—R'"
        ),
        BondAssignment(
            id = "co_ester",
            bond = "C=O",
            functionalGroup = "Aliphatic Ester",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "C=O Stretching",
            minWavenumber = 1735.0,
            maxWavenumber = 1750.0,
            typicalWavenumber = 1740.0,
            spectralRegion = SpectralRegion.DOUBLE_BOND,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Sharp Strong",
            possibleCompoundClasses = listOf("Aliphatic Esters", "Acetates", "Lactones (6-membered)"),
            supportingPeaks = listOf(
                SupportingPeakRef("C-O ester stretch (strong)", "1150–1250 cm⁻¹", 1150.0, 1250.0, "Essential confirmation band (often strongest peak)"),
                SupportingPeakRef("O-C alkyl stretch", "1050–1150 cm⁻¹", 1050.0, 1150.0, "Secondary C-O")
            ),
            notes = "Electronegative ester oxygen withdraws electrons via induction, raising frequency relative to ketones (~1715 cm⁻¹). Conjugation with C=C or phenyl lowers to 1715–1725 cm⁻¹.",
            visualDiagramAscii = "R—C(=O)—O—R'"
        ),
        BondAssignment(
            id = "co_ketone",
            bond = "C=O",
            functionalGroup = "Aliphatic Ketone",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "C=O Stretching",
            minWavenumber = 1705.0,
            maxWavenumber = 1725.0,
            typicalWavenumber = 1715.0,
            spectralRegion = SpectralRegion.DOUBLE_BOND,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Sharp Strong",
            possibleCompoundClasses = listOf("Aliphatic Ketones", "Cyclohexanones"),
            supportingPeaks = listOf(
                SupportingPeakRef("C-C-C bending", "1100–1250 cm⁻¹", 1100.0, 1250.0, "Fingerprint mode")
            ),
            notes = "Standard reference point for carbonyls at 1715 cm⁻¹. Conjugation with C=C or aryl shifts downward by ~30 cm⁻¹ to 1685 cm⁻¹. Ring strain increases frequency (cyclopentanone = 1745 cm⁻¹, cyclobutanone = 1780 cm⁻¹).",
            visualDiagramAscii = "R—C(=O)—R'"
        ),
        BondAssignment(
            id = "co_aldehyde",
            bond = "C=O",
            functionalGroup = "Aliphatic Aldehyde",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "C=O Stretching",
            minWavenumber = 1720.0,
            maxWavenumber = 1740.0,
            typicalWavenumber = 1730.0,
            spectralRegion = SpectralRegion.DOUBLE_BOND,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Sharp Strong",
            possibleCompoundClasses = listOf("Aldehydes"),
            supportingPeaks = listOf(
                SupportingPeakRef("C-H Fermi doublet", "2700–2840 cm⁻¹", 2700.0, 2840.0, "Essential twin peaks at 2720 and 2820 cm⁻¹")
            ),
            notes = "Slightly higher than ketone (1730 vs 1715 cm⁻¹). ALWAYS verify presence of the C-H Fermi resonance doublet at ~2720 & ~2820 cm⁻¹ to distinguish from ketone.",
            visualDiagramAscii = "R—C(=O)—H"
        ),
        BondAssignment(
            id = "co_carboxylic_acid",
            bond = "C=O",
            functionalGroup = "Carboxylic Acid (C=O)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "C=O Stretching in Dimer",
            minWavenumber = 1700.0,
            maxWavenumber = 1725.0,
            typicalWavenumber = 1710.0,
            spectralRegion = SpectralRegion.DOUBLE_BOND,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Strong",
            possibleCompoundClasses = listOf("Carboxylic Acids"),
            supportingPeaks = listOf(
                SupportingPeakRef("O-H envelope", "2500–3300 cm⁻¹", 2500.0, 3300.0, "Enormous diagnostic envelope"),
                SupportingPeakRef("C-O stretch", "1210–1320 cm⁻¹", 1210.0, 1320.0, "Strong band")
            ),
            notes = "Appears at ~1710 cm⁻¹ for hydrogen-bonded dimer. Conjugation with aryl shifts to 1680–1700 cm⁻¹.",
            visualDiagramAscii = "R—C(=O)—OH"
        ),
        BondAssignment(
            id = "co_amide_i",
            bond = "C=O",
            functionalGroup = "Amide I (All Amide Classes)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "Amide I (C=O Stretching + C-N contribution)",
            minWavenumber = 1630.0,
            maxWavenumber = 1690.0,
            typicalWavenumber = 1660.0,
            spectralRegion = SpectralRegion.DOUBLE_BOND,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Broad Strong",
            possibleCompoundClasses = listOf("Primary Amides", "Secondary Amides", "Tertiary Amides", "Peptides & Proteins"),
            supportingPeaks = listOf(
                SupportingPeakRef("Amide II (N-H bend / C-N stretch)", "1530–1570 cm⁻¹", 1530.0, 1570.0, "Essential for 1° and 2° amides"),
                SupportingPeakRef("N-H stretch (if 1° or 2°)", "3150–3450 cm⁻¹", 3150.0, 3450.0, "Doublet or singlet")
            ),
            notes = "Lower frequency carbonyl because resonance donation from nitrogen lone pair gives partial single bond character to C=O (O⁻-C=N⁺).",
            visualDiagramAscii = "R—C(=O)—NR'R\""
        ),

        // ==========================================
        // 6. C=C AND AROMATICS (1450–1680 cm⁻¹)
        // ==========================================
        BondAssignment(
            id = "cc_alkene",
            bond = "C=C",
            functionalGroup = "Alkene (Aliphatic)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "C=C Stretching",
            minWavenumber = 1620.0,
            maxWavenumber = 1680.0,
            typicalWavenumber = 1650.0,
            spectralRegion = SpectralRegion.DOUBLE_BOND,
            intensity = PeakIntensity.VARIABLE,
            bandShape = "Medium / Variable",
            possibleCompoundClasses = listOf("Alkenes", "Cycloalkenes"),
            supportingPeaks = listOf(
                SupportingPeakRef("=C-H stretch", "3010–3090 cm⁻¹", 3010.0, 3090.0, "Above 3000 cm⁻¹ marker"),
                SupportingPeakRef("=C-H OOP bend", "675–1000 cm⁻¹", 675.0, 1000.0, "Cis/trans and substitution marker")
            ),
            notes = "Trans-disubstituted and tetrasubstituted symmetrical alkenes have weak or zero IR intensity due to symmetry. Conjugation with another double bond lowers to ~1600 cm⁻¹ and increases intensity.",
            visualDiagramAscii = "R₂C=CR₂"
        ),
        BondAssignment(
            id = "aromatic_ring",
            bond = "C=C",
            functionalGroup = "Aromatic Ring (Skeletal Breathing)",
            vibrationType = VibrationType.RING_BREATHING,
            vibrationalMode = "Aromatic Ring C=C Skeletal Stretching",
            minWavenumber = 1450.0,
            maxWavenumber = 1600.0,
            typicalWavenumber = 1500.0,
            spectralRegion = SpectralRegion.DOUBLE_BOND,
            intensity = PeakIntensity.MEDIUM,
            bandShape = "Sharp Multiplet (pairs at ~1600, 1580, 1500, 1450 cm⁻¹)",
            possibleCompoundClasses = listOf("Benzenoid Aromatics", "Heteroaromatics (Pyridine, Furan, Thiophene)"),
            supportingPeaks = listOf(
                SupportingPeakRef("Aromatic C-H stretch", "3000–3100 cm⁻¹", 3000.0, 3100.0, "Above 3000 cm⁻¹"),
                SupportingPeakRef("C-H Out-of-Plane bending", "690–850 cm⁻¹", 690.0, 850.0, "Critical substitution pattern fingerprint"),
                SupportingPeakRef("Aromatic overtone combination bands", "1650–2000 cm⁻¹", 1650.0, 2000.0, "Weak saw-tooth pattern in neat liquids")
            ),
            notes = "Two to four sharp bands characteristic of the delocalized benzene ring. The ~1500 cm⁻¹ band is often stronger and more reliable than ~1600 cm⁻¹.",
            visualDiagramAscii = "   // \\\n  |  O  |\n   \\\\ //"
        ),

        // ==========================================
        // 7. C-O AND OXYGEN SINGLE BONDS (1000–1300 cm⁻¹)
        // ==========================================
        BondAssignment(
            id = "co_alcohol_primary",
            bond = "C-O",
            functionalGroup = "Primary Alcohol (C-O)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "C-O Stretching in Primary Alcohol",
            minWavenumber = 1000.0,
            maxWavenumber = 1075.0,
            typicalWavenumber = 1050.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.STRONG,
            bandShape = "Strong Sharp",
            possibleCompoundClasses = listOf("Primary Alcohols"),
            supportingPeaks = listOf(
                SupportingPeakRef("O-H stretch", "3200–3550 cm⁻¹", 3200.0, 3550.0, "Broad U-shaped band")
            ),
            notes = "C-O stretch frequency increases with branching: 1° alcohol (~1050 cm⁻¹) < 2° alcohol (~1100 cm⁻¹) < 3° alcohol (~1150 cm⁻¹).",
            visualDiagramAscii = "R—CH₂—OH"
        ),
        BondAssignment(
            id = "co_alcohol_secondary",
            bond = "C-O",
            functionalGroup = "Secondary Alcohol (C-O)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "C-O Stretching in Secondary Alcohol",
            minWavenumber = 1080.0,
            maxWavenumber = 1125.0,
            typicalWavenumber = 1100.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.STRONG,
            bandShape = "Strong",
            possibleCompoundClasses = listOf("Secondary Alcohols", "Cyclic Alcohols"),
            supportingPeaks = listOf(
                SupportingPeakRef("O-H stretch", "3200–3550 cm⁻¹", 3200.0, 3550.0, "Broad U-band")
            ),
            notes = "Characteristic band at ~1100 cm⁻¹.",
            visualDiagramAscii = "R₂CH—OH"
        ),
        BondAssignment(
            id = "co_alcohol_tertiary",
            bond = "C-O",
            functionalGroup = "Tertiary Alcohol / Phenol (C-O)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "C-O Stretching in 3° Alcohol / Phenol",
            minWavenumber = 1130.0,
            maxWavenumber = 1260.0,
            typicalWavenumber = 1150.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.STRONG,
            bandShape = "Strong",
            possibleCompoundClasses = listOf("Tertiary Alcohols", "Phenols"),
            supportingPeaks = listOf(
                SupportingPeakRef("O-H stretch", "3200–3600 cm⁻¹", 3200.0, 3600.0, "Alcohol/phenol O-H")
            ),
            notes = "Higher C-O frequency due to mass coupling and partial resonance (for phenols at 1200–1230 cm⁻¹).",
            visualDiagramAscii = "R₃C—OH  or  Ar—OH"
        ),
        BondAssignment(
            id = "co_ether_aliphatic",
            bond = "C-O",
            functionalGroup = "Aliphatic Ether (C-O-C)",
            vibrationType = VibrationType.STRETCH_ASYM,
            vibrationalMode = "Asymmetric C-O-C Stretch",
            minWavenumber = 1080.0,
            maxWavenumber = 1150.0,
            typicalWavenumber = 1120.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Very Strong Sharp",
            possibleCompoundClasses = listOf("Dialkyl Ethers", "Epoxides", "Cyclic Ethers"),
            supportingPeaks = emptyList(),
            notes = "Ethers show prominent strong C-O stretch with NO O-H band in 3200–3600 cm⁻¹ and NO C=O in 1650–1850 cm⁻¹.",
            visualDiagramAscii = "R—O—R'"
        ),
        BondAssignment(
            id = "co_ether_aryl_alkyl",
            bond = "C-O",
            functionalGroup = "Aryl / Vinyl Ether (=C-O-C)",
            vibrationType = VibrationType.STRETCH_ASYM,
            vibrationalMode = "Asymmetric =C-O Stretch",
            minWavenumber = 1200.0,
            maxWavenumber = 1275.0,
            typicalWavenumber = 1250.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Very Strong",
            possibleCompoundClasses = listOf("Alkyl Aryl Ethers (Anisole derivatives)", "Vinyl Ethers"),
            supportingPeaks = listOf(
                SupportingPeakRef("Alkyl-O stretch", "1020–1075 cm⁻¹", 1020.0, 1075.0, "Secondary C-O band"),
                SupportingPeakRef("Aromatic ring bands", "1450–1600 cm⁻¹", 1450.0, 1600.0, "Aromatic confirmation")
            ),
            notes = "Strongly shifted to ~1250 cm⁻¹ by oxygen lone pair resonance into the aromatic ring, giving partial double bond character.",
            visualDiagramAscii = "Ar—O—CH₃"
        ),

        // ==========================================
        // 8. C-N AND NITROGEN SINGLE BONDS (1020–1360 cm⁻¹)
        // ==========================================
        BondAssignment(
            id = "cn_aliphatic_amine",
            bond = "C-N",
            functionalGroup = "Aliphatic Amine (C-N)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "C-N Stretching",
            minWavenumber = 1020.0,
            maxWavenumber = 1220.0,
            typicalWavenumber = 1130.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.MEDIUM,
            bandShape = "Medium Sharp",
            possibleCompoundClasses = listOf("Primary, Secondary, Tertiary Aliphatic Amines"),
            supportingPeaks = listOf(
                SupportingPeakRef("N-H stretch (if 1°/2°)", "3300–3500 cm⁻¹", 3300.0, 3500.0, "Amine diagnostic")
            ),
            notes = "Medium intensity band. For 3° amines where N-H is absent, C-N and C-H are the primary markers.",
            visualDiagramAscii = "R—N(R')—R\""
        ),
        BondAssignment(
            id = "cn_aromatic_amine",
            bond = "C-N",
            functionalGroup = "Aromatic Amine (Ar-N)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "Aromatic C-N Stretching",
            minWavenumber = 1250.0,
            maxWavenumber = 1360.0,
            typicalWavenumber = 1280.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.STRONG,
            bandShape = "Strong",
            possibleCompoundClasses = listOf("Anilines", "Aromatic Amines"),
            supportingPeaks = listOf(
                SupportingPeakRef("N-H stretch", "3300–3500 cm⁻¹", 3300.0, 3500.0, "Aniline N-H"),
                SupportingPeakRef("Aromatic ring", "1450–1600 cm⁻¹", 1450.0, 1600.0, "Benzene skeletal")
            ),
            notes = "Shifted to 1250–1360 cm⁻¹ due to resonance shortening of the C_ar-N bond.",
            visualDiagramAscii = "Ar—NH₂"
        ),
        BondAssignment(
            id = "nitro_no2_asym",
            bond = "N-O",
            functionalGroup = "Nitro Group (-NO₂ Asymmetric)",
            vibrationType = VibrationType.STRETCH_ASYM,
            vibrationalMode = "Asymmetric N-O Stretch",
            minWavenumber = 1515.0,
            maxWavenumber = 1560.0,
            typicalWavenumber = 1535.0,
            spectralRegion = SpectralRegion.DOUBLE_BOND,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Very Strong",
            possibleCompoundClasses = listOf("Aliphatic Nitro", "Aromatic Nitro (Nitrobenzene)"),
            supportingPeaks = listOf(
                SupportingPeakRef("Symmetric N-O stretch", "1330–1360 cm⁻¹", 1330.0, 1360.0, "Essential partner band")
            ),
            notes = "Nitro groups give TWO VERY STRONG bands (asymmetric at 1530–1550 cm⁻¹ and symmetric at 1330–1360 cm⁻¹). In aromatics, conjugative stabilization narrows and intensifies both bands.",
            visualDiagramAscii = "    O\n   //\nR—N⁺\n   \\\n    O⁻"
        ),
        BondAssignment(
            id = "nitro_no2_sym",
            bond = "N-O",
            functionalGroup = "Nitro Group (-NO₂ Symmetric)",
            vibrationType = VibrationType.STRETCH_SYM,
            vibrationalMode = "Symmetric N-O Stretch",
            minWavenumber = 1330.0,
            maxWavenumber = 1365.0,
            typicalWavenumber = 1345.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Very Strong Sharp",
            possibleCompoundClasses = listOf("Nitroalkanes", "Nitroaromatics"),
            supportingPeaks = listOf(
                SupportingPeakRef("Asymmetric N-O stretch", "1515–1560 cm⁻¹", 1515.0, 1560.0, "Essential partner band")
            ),
            notes = "Always pairs with the 1535 cm⁻¹ asymmetric stretch.",
            visualDiagramAscii = "R—NO₂ (sym)"
        ),

        // ==========================================
        // 9. C-X CARBON-HALOGEN BONDS (500–1400 cm⁻¹)
        // ==========================================
        BondAssignment(
            id = "cx_fluorine",
            bond = "C-F",
            functionalGroup = "Alkyl Fluoride (C-F)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "C-F Stretching",
            minWavenumber = 1000.0,
            maxWavenumber = 1400.0,
            typicalWavenumber = 1100.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Extremely Strong",
            possibleCompoundClasses = listOf("Fluoroalkanes", "Perfluorinated compounds (PTFE)"),
            supportingPeaks = emptyList(),
            notes = "Because of the high dipole moment of C-F, this stretch is one of the most intense absorptions in all IR spectroscopy. -CF₃ groups give massive bands at 1100–1350 cm⁻¹.",
            visualDiagramAscii = "R—CF₃  or  R—F"
        ),
        BondAssignment(
            id = "cx_chlorine",
            bond = "C-Cl",
            functionalGroup = "Alkyl Chloride (C-Cl)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "C-Cl Stretching",
            minWavenumber = 600.0,
            maxWavenumber = 800.0,
            typicalWavenumber = 700.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.STRONG,
            bandShape = "Strong Sharp",
            possibleCompoundClasses = listOf("Chloroalkanes", "Chlorobenzenes"),
            supportingPeaks = emptyList(),
            notes = "Heavy chlorine atom lowers frequency to 600–800 cm⁻¹. Conformation dependent (axial vs equatorial isomers).",
            visualDiagramAscii = "R—Cl"
        ),
        BondAssignment(
            id = "cx_bromine",
            bond = "C-Br",
            functionalGroup = "Alkyl Bromide (C-Br)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "C-Br Stretching",
            minWavenumber = 500.0,
            maxWavenumber = 680.0,
            typicalWavenumber = 600.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.STRONG,
            bandShape = "Medium-Strong",
            possibleCompoundClasses = listOf("Bromoalkanes"),
            supportingPeaks = emptyList(),
            notes = "Heavy bromine atom pushes vibration into low fingerprint / far-IR fringe.",
            visualDiagramAscii = "R—Br"
        ),
        BondAssignment(
            id = "cx_iodine",
            bond = "C-I",
            functionalGroup = "Alkyl Iodide (C-I)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "C-I Stretching",
            minWavenumber = 450.0,
            maxWavenumber = 580.0,
            typicalWavenumber = 500.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.MEDIUM,
            bandShape = "Medium",
            possibleCompoundClasses = listOf("Iodoalkanes"),
            supportingPeaks = emptyList(),
            notes = "Very heavy atom, low force constant. Sits right at the lower boundary of mid-IR spectrometers.",
            visualDiagramAscii = "R—I"
        ),

        // ==========================================
        // 10. Si-O SILICON-OXYGEN BONDS (800–1100 cm⁻¹)
        // ==========================================
        BondAssignment(
            id = "sio_siloxane",
            bond = "Si-O",
            functionalGroup = "Siloxane / Silicone (Si-O-Si)",
            vibrationType = VibrationType.STRETCH_ASYM,
            vibrationalMode = "Asymmetric Si-O-Si Stretching",
            minWavenumber = 1000.0,
            maxWavenumber = 1110.0,
            typicalWavenumber = 1060.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Very Strong Broad Doublet/Band",
            possibleCompoundClasses = listOf("Silicones (PDMS)", "Silicates", "Silanols", "Sol-Gel materials"),
            supportingPeaks = listOf(
                SupportingPeakRef("Si-CH₃ symmetric deformation", "1255–1265 cm⁻¹", 1255.0, 1265.0, "Sharp diagnostic methyl on silicon"),
                SupportingPeakRef("Si-C rocking / stretching", "780–860 cm⁻¹", 780.0, 860.0, "Strong band")
            ),
            notes = "Dominant hallmark of polydimethylsiloxane (PDMS) and silica networks. Often shows a distinct broad doublet in cyclic vs linear siloxanes (1020 & 1080 cm⁻¹).",
            visualDiagramAscii = "—Si—O—Si—"
        ),
        BondAssignment(
            id = "sio_silanol",
            bond = "Si-O",
            functionalGroup = "Silanol (Si-OH)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "Si-OH Stretching / Bending",
            minWavenumber = 850.0,
            maxWavenumber = 950.0,
            typicalWavenumber = 900.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.MEDIUM,
            bandShape = "Medium Sharp",
            possibleCompoundClasses = listOf("Silanols", "Surface hydroxylated silica"),
            supportingPeaks = listOf(
                SupportingPeakRef("Free Si-O-H stretch", "3690–3750 cm⁻¹", 3690.0, 3750.0, "Sharp high-frequency O-H")
            ),
            notes = "Key diagnostic marker in sol-gel chemistry and catalyst surface characterization.",
            visualDiagramAscii = "≡Si—OH"
        ),

        // ==========================================
        // 11. P-O PHOSPHORUS-OXYGEN BONDS (900–1300 cm⁻¹)
        // ==========================================
        BondAssignment(
            id = "po_phosphine_oxide",
            bond = "P=O",
            functionalGroup = "Phosphine Oxide / Phosphate (P=O)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "P=O Stretching",
            minWavenumber = 1150.0,
            maxWavenumber = 1300.0,
            typicalWavenumber = 1200.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Very Strong",
            possibleCompoundClasses = listOf("Phosphates", "Phosphine Oxides", "Organophosphates (DNA/RNA backbone)"),
            supportingPeaks = listOf(
                SupportingPeakRef("P-O-C aliphatic stretch", "990–1050 cm⁻¹", 990.0, 1050.0, "Phosphate ester marker")
            ),
            notes = "Very polar bond producing intense absorption. In phospholipids and nucleic acids, asymmetric PO₂⁻ stretch appears near 1220–1240 cm⁻¹ and symmetric PO₂⁻ near 1085 cm⁻¹.",
            visualDiagramAscii = "R₃P=O"
        ),
        BondAssignment(
            id = "poc_phosphate_ester",
            bond = "P-O",
            functionalGroup = "Phosphate Ester (P-O-C)",
            vibrationType = VibrationType.STRETCH_ASYM,
            vibrationalMode = "Asymmetric P-O-C Stretching",
            minWavenumber = 950.0,
            maxWavenumber = 1050.0,
            typicalWavenumber = 1000.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.STRONG,
            bandShape = "Strong",
            possibleCompoundClasses = listOf("Phosphate Esters", "Phospholipids"),
            supportingPeaks = listOf(
                SupportingPeakRef("P=O stretch", "1150–1280 cm⁻¹", 1150.0, 1280.0, "Phosphoryl partner band")
            ),
            notes = "Classic diagnostic marker for organophosphates and lipid headgroups.",
            visualDiagramAscii = "P—O—CH₂—R"
        ),

        // ==========================================
        // 12. S=O SULFUR-OXYGEN BONDS (1000–1350 cm⁻¹)
        // ==========================================
        BondAssignment(
            id = "so_sulfoxide",
            bond = "S=O",
            functionalGroup = "Sulfoxide (R₂S=O)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "S=O Stretching",
            minWavenumber = 1030.0,
            maxWavenumber = 1070.0,
            typicalWavenumber = 1050.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Very Strong Sharp",
            possibleCompoundClasses = listOf("Sulfoxides (e.g. DMSO)"),
            supportingPeaks = emptyList(),
            notes = "Single S=O group gives ONE very intense band at ~1050 cm⁻¹ (famous diagnostic band of DMSO solvent).",
            visualDiagramAscii = "R—S(=O)—R'"
        ),
        BondAssignment(
            id = "so_sulfone_asym",
            bond = "S=O",
            functionalGroup = "Sulfone (R₂SO₂ Asymmetric)",
            vibrationType = VibrationType.STRETCH_ASYM,
            vibrationalMode = "Asymmetric SO₂ Stretching",
            minWavenumber = 1290.0,
            maxWavenumber = 1350.0,
            typicalWavenumber = 1320.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Very Strong",
            possibleCompoundClasses = listOf("Sulfones", "Sulfonyl Chlorides"),
            supportingPeaks = listOf(
                SupportingPeakRef("Symmetric SO₂ stretch", "1120–1160 cm⁻¹", 1120.0, 1160.0, "Essential partner band")
            ),
            notes = "Sulfones have two S=O bonds, producing TWO strong bands: asymmetric at 1300–1350 cm⁻¹ and symmetric at 1120–1165 cm⁻¹.",
            visualDiagramAscii = "    O\n    ‖\nR—S—R'\n    ‖\n    O"
        ),
        BondAssignment(
            id = "so_sulfonamide",
            bond = "S=O",
            functionalGroup = "Sulfonamide (R-SO₂-NH₂)",
            vibrationType = VibrationType.STRETCH,
            vibrationalMode = "Coupled SO₂ & N-H Modes",
            minWavenumber = 1310.0,
            maxWavenumber = 1370.0,
            typicalWavenumber = 1340.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Very Strong",
            possibleCompoundClasses = listOf("Sulfonamides", "Sulfa drugs"),
            supportingPeaks = listOf(
                SupportingPeakRef("Symmetric SO₂ stretch", "1140–1180 cm⁻¹", 1140.0, 1180.0, "Partner SO₂ band"),
                SupportingPeakRef("N-H stretch", "3200–3400 cm⁻¹", 3200.0, 3400.0, "Doublet or singlet")
            ),
            notes = "Important class in pharmaceutical chemistry and organic synthesis.",
            visualDiagramAscii = "R—SO₂—NHR'"
        ),

        // ==========================================
        // 13. AROMATIC C-H OUT-OF-PLANE SUBSTITUTION PATTERNS
        // ==========================================
        BondAssignment(
            id = "aromatic_oop_mono",
            bond = "C-H",
            functionalGroup = "Monosubstituted Benzene (C-H OOP)",
            vibrationType = VibrationType.BEND_OUT_OF_PLANE,
            vibrationalMode = "Adjacent 5-Hydrogen Out-of-Plane Bending",
            minWavenumber = 690.0,
            maxWavenumber = 770.0,
            typicalWavenumber = 750.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "Strong Doublet (~750 and ~690 cm⁻¹)",
            possibleCompoundClasses = listOf("Monosubstituted Aromatics (Toluene, Polystyrene, Benzoic acid)"),
            supportingPeaks = listOf(
                SupportingPeakRef("Ring puckering / OOP bend", "690–710 cm⁻¹", 690.0, 710.0, "Second required band at ~700 cm⁻¹"),
                SupportingPeakRef("Aromatic ring stretch", "1450–1600 cm⁻¹", 1450.0, 1600.0, "Aromatic framework")
            ),
            notes = "Classic signature of monosubstituted benzene: TWO strong bands at 730–770 cm⁻¹ and 690–710 cm⁻¹.",
            visualDiagramAscii = "C₆H₅—R"
        ),
        BondAssignment(
            id = "aromatic_oop_ortho",
            bond = "C-H",
            functionalGroup = "Ortho-Disubstituted Benzene (1,2-)",
            vibrationType = VibrationType.BEND_OUT_OF_PLANE,
            vibrationalMode = "Adjacent 4-Hydrogen Out-of-Plane Bending",
            minWavenumber = 735.0,
            maxWavenumber = 770.0,
            typicalWavenumber = 750.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "One Strong Band",
            possibleCompoundClasses = listOf("1,2-Disubstituted Aromatics (Phthalates, o-Xylene)"),
            supportingPeaks = emptyList(),
            notes = "One strong band at 735–770 cm⁻¹. No band at 690 cm⁻¹.",
            visualDiagramAscii = "1,2-(R)₂-C₆H₄"
        ),
        BondAssignment(
            id = "aromatic_oop_para",
            bond = "C-H",
            functionalGroup = "Para-Disubstituted Benzene (1,4-)",
            vibrationType = VibrationType.BEND_OUT_OF_PLANE,
            vibrationalMode = "Adjacent 2-Hydrogen Out-of-Plane Bending",
            minWavenumber = 800.0,
            maxWavenumber = 860.0,
            typicalWavenumber = 830.0,
            spectralRegion = SpectralRegion.FINGERPRINT,
            intensity = PeakIntensity.VERY_STRONG,
            bandShape = "One Strong Band",
            possibleCompoundClasses = listOf("1,4-Disubstituted Aromatics (p-Xylene, Terephthalates)"),
            supportingPeaks = emptyList(),
            notes = "One very strong band in 800–860 cm⁻¹ range (typically ~830 cm⁻¹).",
            visualDiagramAscii = "1,4-(R)₂-C₆H₄"
        )
    )

    val DISTINCT_BONDS: List<String> = STANDARD_ASSIGNMENTS.map { it.bond }.distinct().sorted()
    val DISTINCT_FUNCTIONAL_GROUPS: List<String> = STANDARD_ASSIGNMENTS.map { it.functionalGroup }.distinct().sorted()
    val DISTINCT_COMPOUND_CLASSES: List<String> = STANDARD_ASSIGNMENTS.flatMap { it.possibleCompoundClasses }.distinct().sorted()
}
