package com.example.domain.spectroscopy

import com.example.domain.model.BondAssignment
import com.example.domain.model.Peak
import com.example.domain.model.PeakIntensity
import com.example.domain.model.Spectrum
import kotlin.math.abs

/**
 * Result of multi-peak simultaneous evidence corroboration.
 * Evaluates candidate functional groups / compound classes based on whether
 * primary diagnostic peaks AND required supporting peaks are co-present in the query set.
 */
data class CorroboratedInterpretation(
    val assignment: BondAssignment,
    val matchedQueryPeakWavenumber: Double,
    val deltaCm: Double,
    val supportingEvidenceFound: List<EvidenceMatch>,
    val missingSupportingEvidence: List<String>,
    val corroborationScore: Double, // 0.0 to 1.0 based on partner peaks present
    val diagnosticSummary: String,
    val potentialAmbiguities: List<String>
)

data class EvidenceMatch(
    val role: String,
    val expectedRange: String,
    val foundPeakWavenumber: Double
)

data class MultiPeakAnalysisResult(
    val queryPeaksCount: Int,
    val topInterpretations: List<CorroboratedInterpretation>,
    val compatibleCompoundClasses: List<CompoundClassEvidence>,
    val potentialAmbiguities: List<String>,
    val spectralRegionCoverage: Map<String, Int>
)

data class CompoundClassEvidence(
    val compoundClass: String,
    val supportingBonds: List<String>,
    val matchedPeaks: List<Double>,
    val confidenceGrade: String // "High Evidence", "Probable", "Possible"
)

interface ManualInterpretationEngine {
    fun searchByWavenumber(
        wavenumber: Double,
        toleranceCm: Double = 35.0,
        customAssignments: List<BondAssignment> = emptyList()
    ): List<BondAssignment>

    fun analyzeSinglePeak(
        wavenumber: Double,
        intensity: PeakIntensity? = null,
        widthFwhm: Double? = null,
        sampleNotes: String? = null,
        customAssignments: List<BondAssignment> = emptyList()
    ): List<CorroboratedInterpretation>

    fun analyzeMultiplePeaks(
        peakWavenumbers: List<Double>,
        toleranceCm: Double = 40.0,
        customAssignments: List<BondAssignment> = emptyList()
    ): MultiPeakAnalysisResult
}

class StandardManualInterpretationEngine : ManualInterpretationEngine {

    override fun searchByWavenumber(
        wavenumber: Double,
        toleranceCm: Double,
        customAssignments: List<BondAssignment>
    ): List<BondAssignment> {
        val all = SpectroscopyDatabase.STANDARD_ASSIGNMENTS + customAssignments
        return all.filter { assignment ->
            assignment.containsWavenumber(wavenumber, toleranceCm)
        }.sortedBy { assignment ->
            abs(assignment.typicalWavenumber - wavenumber)
        }
    }

    override fun analyzeSinglePeak(
        wavenumber: Double,
        intensity: PeakIntensity?,
        widthFwhm: Double?,
        sampleNotes: String?,
        customAssignments: List<BondAssignment>
    ): List<CorroboratedInterpretation> {
        val matches = searchByWavenumber(wavenumber, 35.0, customAssignments)

        return matches.map { assignment ->
            val delta = abs(assignment.typicalWavenumber - wavenumber)
            val missing = assignment.supportingPeaks.map { "${it.description} (${it.expectedRange})" }
            val ambiguities = generateAmbiguitiesForWavenumber(wavenumber, assignment)

            CorroboratedInterpretation(
                assignment = assignment,
                matchedQueryPeakWavenumber = wavenumber,
                deltaCm = delta,
                supportingEvidenceFound = emptyList(),
                missingSupportingEvidence = missing,
                corroborationScore = 0.5,
                diagnosticSummary = "${assignment.functionalGroup} (${assignment.vibrationalMode}). " +
                        if (missing.isNotEmpty()) "Confirm by checking: ${missing.joinToString(", ")}." else "",
                potentialAmbiguities = ambiguities
            )
        }
    }

    override fun analyzeMultiplePeaks(
        peakWavenumbers: List<Double>,
        toleranceCm: Double,
        customAssignments: List<BondAssignment>
    ): MultiPeakAnalysisResult {
        if (peakWavenumbers.isEmpty()) {
            return MultiPeakAnalysisResult(
                queryPeaksCount = 0,
                topInterpretations = emptyList(),
                compatibleCompoundClasses = emptyList(),
                potentialAmbiguities = listOf("No peaks provided for analysis."),
                spectralRegionCoverage = emptyMap()
            )
        }

        val allAssignments = SpectroscopyDatabase.STANDARD_ASSIGNMENTS + customAssignments
        val interpretations = mutableListOf<CorroboratedInterpretation>()

        for (wn in peakWavenumbers) {
            val candidateAssignments = allAssignments.filter { it.containsWavenumber(wn, toleranceCm) }

            for (assignment in candidateAssignments) {
                val delta = abs(assignment.typicalWavenumber - wn)
                val foundEvidence = mutableListOf<EvidenceMatch>()
                val missingEvidence = mutableListOf<String>()

                for (sup in assignment.supportingPeaks) {
                    val matchingPeak = peakWavenumbers.firstOrNull { otherWn ->
                        otherWn != wn && otherWn >= (sup.minWavenumber - toleranceCm) && otherWn <= (sup.maxWavenumber + toleranceCm)
                    }
                    if (matchingPeak != null) {
                        foundEvidence.add(
                            EvidenceMatch(
                                role = sup.description,
                                expectedRange = sup.expectedRange,
                                foundPeakWavenumber = matchingPeak
                            )
                        )
                    } else {
                        missingEvidence.add("${sup.description} in ${sup.expectedRange}")
                    }
                }

                val totalSupporting = assignment.supportingPeaks.size
                val score = if (totalSupporting == 0) {
                    0.7
                } else {
                    0.4 + (0.6 * (foundEvidence.size.toDouble() / totalSupporting))
                }

                val ambiguities = generateAmbiguitiesForWavenumber(wn, assignment)

                interpretations.add(
                    CorroboratedInterpretation(
                        assignment = assignment,
                        matchedQueryPeakWavenumber = wn,
                        deltaCm = delta,
                        supportingEvidenceFound = foundEvidence,
                        missingSupportingEvidence = missingEvidence,
                        corroborationScore = score,
                        diagnosticSummary = buildDiagnosticSummary(assignment, foundEvidence, missingEvidence),
                        potentialAmbiguities = ambiguities
                    )
                )
            }
        }

        // Sort interpretations: prioritize high corroboration score, then closeness to typical wavenumber
        val sortedInterpretations = interpretations
            .distinctBy { it.assignment.id to it.matchedQueryPeakWavenumber.toInt() }
            .sortedWith(
                compareByDescending<CorroboratedInterpretation> { it.corroborationScore }
                    .thenBy { it.deltaCm }
            )

        // Group into compound class evidence
        val classMap = mutableMapOf<String, MutableList<Pair<String, Double>>>()
        sortedInterpretations.forEach { interp ->
            interp.assignment.possibleCompoundClasses.forEach { cls ->
                val list = classMap.getOrPut(cls) { mutableListOf() }
                list.add(Pair(interp.assignment.bond, interp.matchedQueryPeakWavenumber))
            }
        }

        val compoundClasses = classMap.map { (cls, pairs) ->
            val uniqueBonds = pairs.map { it.first }.distinct()
            val peaks = pairs.map { it.second }.distinct().sorted()
            val grade = when {
                uniqueBonds.size >= 3 -> "High Evidence"
                uniqueBonds.size == 2 -> "Probable"
                else -> "Possible"
            }
            CompoundClassEvidence(
                compoundClass = cls,
                supportingBonds = uniqueBonds,
                matchedPeaks = peaks,
                confidenceGrade = grade
            )
        }.sortedWith(
            compareByDescending<CompoundClassEvidence> { it.supportingBonds.size }
                .thenByDescending { it.matchedPeaks.size }
        )

        // General ambiguities across provided spectrum
        val globalAmbiguities = mutableListOf<String>()
        val hasCarbonyl = peakWavenumbers.any { it in 1650.0..1850.0 }
        val hasAlkeneOrArom = peakWavenumbers.any { it in 1450.0..1680.0 }
        val hasOhOrNh = peakWavenumbers.any { it in 3100.0..3600.0 }

        if (hasCarbonyl && !hasOhOrNh) {
            globalAmbiguities.add("Carbonyl present without O-H (rules out unesterified carboxylic acids). Distinguish ketone vs ester via C-O presence (1150–1250 cm⁻¹).")
        }
        if (hasCarbonyl && peakWavenumbers.any { it in 2500.0..3300.0 }) {
            globalAmbiguities.add("Broad 2500–3300 cm⁻¹ envelope strongly corroborates carboxylic acid dimer over simple alcohol.")
        }
        if (peakWavenumbers.any { it > 3000.0 && it < 3100.0 } && peakWavenumbers.any { it > 2840.0 && it < 2980.0 }) {
            globalAmbiguities.add("Both sp² C-H (>3000 cm⁻¹) and sp³ C-H (<3000 cm⁻¹) detected, indicating mixed aromatic/alkenyl and aliphatic substituents.")
        }

        // Spectral region coverage
        val coverage = mutableMapOf<String, Int>()
        peakWavenumbers.forEach { wn ->
            val reg = com.example.domain.model.SpectralRegion.fromWavenumber(wn)
            coverage[reg.label] = (coverage[reg.label] ?: 0) + 1
        }

        return MultiPeakAnalysisResult(
            queryPeaksCount = peakWavenumbers.size,
            topInterpretations = sortedInterpretations,
            compatibleCompoundClasses = compoundClasses,
            potentialAmbiguities = globalAmbiguities,
            spectralRegionCoverage = coverage
        )
    }

    private fun buildDiagnosticSummary(
        assignment: BondAssignment,
        found: List<EvidenceMatch>,
        missing: List<String>
    ): String {
        return buildString {
            append("${assignment.functionalGroup}: ")
            if (found.isNotEmpty()) {
                append("Corroborated by ${found.size} partner peak(s) [${found.joinToString("; ") { "${it.role} at ${it.foundPeakWavenumber.toInt()} cm⁻¹" }}]. ")
            }
            if (missing.isNotEmpty()) {
                append("Missing partner: ${missing.take(2).joinToString(", ")}. ")
            }
            append(assignment.notes)
        }
    }

    private fun generateAmbiguitiesForWavenumber(
        wavenumber: Double,
        assignment: BondAssignment
    ): List<String> {
        val ambiguities = mutableListOf<String>()
        when {
            wavenumber in 1700.0..1750.0 -> {
                ambiguities.add("1700–1750 cm⁻¹ is shared by ketones, aldehydes, esters, and carboxylic acids. Check for C-O (esters), Fermi doublet ~2720 cm⁻¹ (aldehydes), or broad O-H (acids).")
            }
            wavenumber in 3200.0..3550.0 -> {
                ambiguities.add("O-H vs N-H ambiguity: Alcohol O-H is typically broad and intense; N-H is sharper and weaker (doublet for 1° amine, singlet for 2° amine).")
            }
            wavenumber in 1600.0..1680.0 -> {
                ambiguities.add("Alkene C=C (~1650 cm⁻¹) overlaps with conjugated carbonyls, Amide I, and aromatic ring breathing (~1600 cm⁻¹).")
            }
            wavenumber in 1000.0..1300.0 -> {
                ambiguities.add("Fingerprint region contains multiple overlapping C-O, C-C, C-N, Si-O, and P-O modes. Single peak assignment here has lower confidence without partner bands.")
            }
        }
        return ambiguities
    }
}
