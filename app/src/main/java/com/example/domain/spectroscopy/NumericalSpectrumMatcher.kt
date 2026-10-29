package com.example.domain.spectroscopy

import com.example.domain.model.Peak
import com.example.domain.model.ReferenceSpectrum
import com.example.domain.model.Spectrum
import com.example.domain.model.SpectrumPoint
import com.example.domain.model.SpectrumType
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Supported numerical spectral similarity algorithms.
 */
enum class SimilarityAlgorithm(val displayName: String, val shortName: String, val description: String) {
    HQI(
        "Hit Quality Index (HQI)",
        "HQI",
        "Squared cosine vector correlation; analytical spectroscopy standard benchmark (0–100%)."
    ),
    PEARSON(
        "Pearson Correlation (r)",
        "Pearson",
        "Measures linear covariance of absorption profiles; invariant to uniform vertical baseline offsets."
    ),
    COSINE(
        "Cosine Similarity",
        "Cosine",
        "Calculates the cosine of the angle between spectral vector intensity profiles in high-dimensional space."
    ),
    SPECTRAL_DISTANCE(
        "Normalized Spectral Distance",
        "NRMSE",
        "1 minus Normalized Root-Mean-Square Error; sensitive to absolute signal height discrepancies."
    ),
    NORMALIZED_CROSS_CORRELATION(
        "Cross-Correlation (Zero-Lag)",
        "NCC",
        "Normalized cross-correlation evaluated across the mutually shared wavenumber grid."
    )
}

enum class PeakMatchStatus(val label: String) {
    MATCHED("Matched"),
    SHIFTED("Slightly Shifted"),
    UNMATCHED_UNKNOWN("Present Only in Unknown"),
    UNMATCHED_REFERENCE("Missing from Unknown")
}

data class PeakComparisonMatch(
    val unknownPeakWn: Double?,
    val unknownPeakIntensity: Double?,
    val referencePeakWn: Double?,
    val differenceCm: Double?, // unknown - reference
    val status: PeakMatchStatus,
    val diagnosticNote: String = ""
)

data class DifferenceSpectrum(
    val points: List<SpectrumPoint>,
    val meanAbsoluteDifference: Double,
    val rmsDifference: Double,
    val maxDeviationWavenumber: Double,
    val maxDeviationValue: Double
)

data class NumericalComparisonResult(
    val unknownSpectrum: Spectrum,
    val reference: ReferenceSpectrum,
    val primaryAlgorithm: SimilarityAlgorithm,
    val similarityScore: Double, // 0.0 to 100.0%
    val allAlgorithmScores: Map<SimilarityAlgorithm, Double>,
    val commonRangeMinWn: Double,
    val commonRangeMaxWn: Double,
    val rangeOverlapRatio: Double, // 0.0 to 1.0
    val resampledGridPointsCount: Int,
    val gridSpacingCm: Double,
    val differenceSpectrum: DifferenceSpectrum,
    val peakMatches: List<PeakComparisonMatch>,
    val matchedPeaksCount: Int,
    val totalUnknownPeaksCount: Int,
    val totalReferencePeaksCount: Int,
    val scientificLimitations: List<String>,
    val scientificCaveat: String = "CRITICAL: A high spectral similarity score measures mathematical curve agreement only and must NEVER be interpreted as standalone proof of chemical identity. Environmental matrix, baseline tilt, polymorphs, concentration, and instrument geometries (e.g. ATR vs Transmission) affect band shapes."
)

/**
 * Structured bundle prepared for downstream scientific interpretation (e.g. future AI stage).
 */
data class StructuredAiComparisonEvidence(
    val unknownSpectrumId: String,
    val unknownSpectrumName: String,
    val referenceId: String,
    val referenceCompoundName: String,
    val referenceCas: String?,
    val referenceFormula: String?,
    val referenceMethod: String,
    val similarityScores: Map<String, Double>,
    val overlappingRange: String,
    val overlapPercentage: Double,
    val matchedPeaksSummary: List<String>,
    val missingReferencePeaks: List<Double>,
    val uniqueUnknownPeaks: List<Double>,
    val averagePeakShiftCm: Double,
    val differenceSpectrumRms: Double,
    val limitationsNoted: List<String>
)

interface NumericalSpectrumMatcher {
    /**
     * Resamples unknown and reference spectra onto a unified wavenumber grid and calculates
     * multiple rigorous numerical similarity metrics, difference spectrum, and peak cross-matching.
     */
    fun compareSpectra(
        unknown: Spectrum,
        reference: ReferenceSpectrum,
        primaryAlgorithm: SimilarityAlgorithm = SimilarityAlgorithm.HQI,
        peakToleranceCm: Double = 15.0,
        gridSpacingCm: Double = 2.0,
        normalizeForComparison: Boolean = false
    ): NumericalComparisonResult

    /**
     * Formats the comparison result into a clean, structured payload ready for future AI consumption.
     */
    fun extractStructuredAiEvidence(result: NumericalComparisonResult): StructuredAiComparisonEvidence
}

class StandardNumericalSpectrumMatcher : NumericalSpectrumMatcher {

    override fun compareSpectra(
        unknown: Spectrum,
        reference: ReferenceSpectrum,
        primaryAlgorithm: SimilarityAlgorithm,
        peakToleranceCm: Double,
        gridSpacingCm: Double,
        normalizeForComparison: Boolean
    ): NumericalComparisonResult {
        val refSpectrum = reference.spectrum

        val limitations = mutableListOf<String>()

        // 1. Determine common overlapping wavenumber range
        val uMin = unknown.minWavenumber
        val uMax = unknown.maxWavenumber
        val rMin = refSpectrum.minWavenumber
        val rMax = refSpectrum.maxWavenumber

        val commonMin = max(uMin, rMin)
        val commonMax = min(uMax, rMax)

        val uSpan = max(10.0, uMax - uMin)
        val rSpan = max(10.0, rMax - rMin)
        val commonSpan = max(0.0, commonMax - commonMin)
        val overlapRatio = (commonSpan / max(uSpan, rSpan)).coerceIn(0.0, 1.0)

        if (commonSpan < 100.0) {
            limitations.add("Very narrow spectral overlap (${commonSpan.toInt()} cm⁻¹). Comparison validity is significantly reduced.")
        } else if (overlapRatio < 0.75) {
            limitations.add("Incomplete spectral overlap (${(overlapRatio * 100).toInt()}%). Truncated to ${commonMin.toInt()}–${commonMax.toInt()} cm⁻¹.")
        }

        // Check spectrum type compatibility (Transmittance vs Absorbance)
        if (unknown.spectrumType != refSpectrum.spectrumType) {
            limitations.add("Spectrum type mismatch: Unknown is ${unknown.spectrumType.label}, Reference is ${refSpectrum.spectrumType.label}. Signal coordinates may diverge.")
        }

        // Check measurement method differences
        if (reference.measurementMethod.contains("ATR", ignoreCase = true) &&
            unknown.metadata["Method"]?.contains("Transmission", ignoreCase = true) == true
        ) {
            limitations.add("Measurement method difference: ATR band intensities scale with wavelength (longer wavelengths appear stronger than transmission).")
        }

        // 2. Interpolate both spectra onto an identical shared equidistant grid
        val gridPoints = mutableListOf<Double>()
        var currWn = commonMin
        while (currWn <= commonMax) {
            gridPoints.add(currWn)
            currWn += gridSpacingCm
        }
        if (gridPoints.isEmpty()) {
            gridPoints.add((commonMin + commonMax) / 2.0)
        }

        val uInterp = interpolateSpectrum(unknown.points, gridPoints)
        val rInterp = interpolateSpectrum(refSpectrum.points, gridPoints)

        val n = gridPoints.size

        // In analytical spectroscopy library searching (e.g. NIST / OMNIC standards), vector metrics
        // (HQI, Cosine, Distance) are evaluated on absorption profiles so the ~100% transmittance
        // non-absorbing background does not artificially dominate the dot product.
        val uAbsorption = DoubleArray(n) { i ->
            if (unknown.spectrumType == SpectrumType.Transmittance) {
                (100.0 - uInterp[i]).coerceAtLeast(0.0)
            } else {
                uInterp[i].coerceAtLeast(0.0)
            }
        }
        val rAbsorption = DoubleArray(n) { i ->
            if (refSpectrum.spectrumType == SpectrumType.Transmittance) {
                (100.0 - rInterp[i]).coerceAtLeast(0.0)
            } else {
                rInterp[i].coerceAtLeast(0.0)
            }
        }

        // Optional amplitude normalization (min-max [0, 1])
        val uProcessed: DoubleArray
        val rProcessed: DoubleArray

        if (normalizeForComparison) {
            uProcessed = normalizeVector(uAbsorption)
            rProcessed = normalizeVector(rAbsorption)
        } else {
            uProcessed = uAbsorption
            rProcessed = rAbsorption
        }

        // 3. Compute Numerical Metrics
        val scoresMap = mutableMapOf<SimilarityAlgorithm, Double>()

        // Vector dot products and norms
        var dot = 0.0
        var uSumSq = 0.0
        var rSumSq = 0.0
        var uSum = 0.0
        var rSum = 0.0
        var sumDiffSq = 0.0
        var sumAbsDiff = 0.0

        for (i in 0 until n) {
            val u = uProcessed[i]
            val r = rProcessed[i]
            dot += u * r
            uSumSq += u * u
            rSumSq += r * r
            uSum += u
            rSum += r
            val d = u - r
            sumDiffSq += d * d
            sumAbsDiff += abs(d)
        }

        val uMean = if (n > 0) uSum / n else 0.0
        val rMean = if (n > 0) rSum / n else 0.0

        // Cosine Similarity
        val uNorm = sqrt(uSumSq)
        val rNorm = sqrt(rSumSq)
        val cosine = if (uNorm > 1e-9 && rNorm > 1e-9) {
            (dot / (uNorm * rNorm)).coerceIn(-1.0, 1.0)
        } else 0.0
        scoresMap[SimilarityAlgorithm.COSINE] = (max(0.0, cosine) * 100.0)

        // HQI (Hit Quality Index) = (dot)^2 / (uNorm^2 * rNorm^2) * 100%
        val hqi = if (uSumSq > 1e-9 && rSumSq > 1e-9) {
            ((dot * dot) / (uSumSq * rSumSq)).coerceIn(0.0, 1.0) * 100.0
        } else 0.0
        scoresMap[SimilarityAlgorithm.HQI] = hqi

        // Pearson Correlation (r)
        var covar = 0.0
        var varU = 0.0
        var varR = 0.0
        for (i in 0 until n) {
            val du = uProcessed[i] - uMean
            val dr = rProcessed[i] - rMean
            covar += du * dr
            varU += du * du
            varR += dr * dr
        }
        val denom = sqrt(varU * varR)
        val pearsonR = if (denom > 1e-9) (covar / denom).coerceIn(-1.0, 1.0) else 0.0
        scoresMap[SimilarityAlgorithm.PEARSON] = (max(0.0, pearsonR) * 100.0)

        // Normalized Spectral Distance (1 - NRMSE)
        val rmse = if (n > 0) sqrt(sumDiffSq / n) else 0.0
        val rRange = max(1.0, (rProcessed.maxOrNull() ?: 100.0) - (rProcessed.minOrNull() ?: 0.0))
        val nrmse = (rmse / rRange).coerceIn(0.0, 1.0)
        val distanceScore = (1.0 - nrmse) * 100.0
        scoresMap[SimilarityAlgorithm.SPECTRAL_DISTANCE] = max(0.0, distanceScore)

        // Normalized Cross Correlation at zero-lag
        scoresMap[SimilarityAlgorithm.NORMALIZED_CROSS_CORRELATION] = (max(0.0, cosine) * 100.0)

        // Primary score based on selected algorithm
        val primaryScore = scoresMap[primaryAlgorithm] ?: hqi

        // 4. Construct Difference Spectrum D(w) = u(w) - r(w)
        val diffPoints = mutableListOf<SpectrumPoint>()
        var maxDevWn = commonMin
        var maxDevVal = 0.0

        for (i in 0 until n) {
            val wn = gridPoints[i]
            val diffVal = uInterp[i] - rInterp[i]
            diffPoints.add(SpectrumPoint(wavenumber = wn, signalValue = diffVal))
            if (abs(diffVal) > abs(maxDevVal)) {
                maxDevVal = diffVal
                maxDevWn = wn
            }
        }

        val differenceSpectrum = DifferenceSpectrum(
            points = diffPoints.sortedByDescending { it.wavenumber },
            meanAbsoluteDifference = if (n > 0) sumAbsDiff / n else 0.0,
            rmsDifference = rmse,
            maxDeviationWavenumber = maxDevWn,
            maxDeviationValue = maxDevVal
        )

        // 5. Peak Comparison (Unknown vs Reference)
        val unknownPeaks = unknown.detectedPeaks
        val referencePeaks = if (reference.characteristicPeaks.isNotEmpty()) {
            reference.characteristicPeaks
        } else {
            refSpectrum.detectedPeaks.map { it.wavenumber }
        }

        val peakMatches = mutableListOf<PeakComparisonMatch>()
        val matchedRefPeaks = mutableSetOf<Double>()

        for (up in unknownPeaks) {
            // Find closest reference peak
            val closestRef = referencePeaks.minByOrNull { abs(it - up.wavenumber) }
            if (closestRef != null) {
                val diff = up.wavenumber - closestRef
                val absDiff = abs(diff)

                if (absDiff <= peakToleranceCm) {
                    matchedRefPeaks.add(closestRef)
                    peakMatches.add(
                        PeakComparisonMatch(
                            unknownPeakWn = up.wavenumber,
                            unknownPeakIntensity = up.signalValue,
                            referencePeakWn = closestRef,
                            differenceCm = diff,
                            status = PeakMatchStatus.MATCHED,
                            diagnosticNote = "Strong positional match (Δ = ${"%.1f".format(Locale.US, diff)} cm⁻¹)"
                        )
                    )
                } else if (absDiff <= peakToleranceCm * 2.0) {
                    matchedRefPeaks.add(closestRef)
                    peakMatches.add(
                        PeakComparisonMatch(
                            unknownPeakWn = up.wavenumber,
                            unknownPeakIntensity = up.signalValue,
                            referencePeakWn = closestRef,
                            differenceCm = diff,
                            status = PeakMatchStatus.SHIFTED,
                            diagnosticNote = "Shifted band (Δ = ${"%.1f".format(Locale.US, diff)} cm⁻¹); potential hydrogen bonding or matrix effect"
                        )
                    )
                } else {
                    peakMatches.add(
                        PeakComparisonMatch(
                            unknownPeakWn = up.wavenumber,
                            unknownPeakIntensity = up.signalValue,
                            referencePeakWn = null,
                            differenceCm = null,
                            status = PeakMatchStatus.UNMATCHED_UNKNOWN,
                            diagnosticNote = "Distinct band in unknown not present in pure reference standard"
                        )
                    )
                }
            } else {
                peakMatches.add(
                    PeakComparisonMatch(
                        unknownPeakWn = up.wavenumber,
                        unknownPeakIntensity = up.signalValue,
                        referencePeakWn = null,
                        differenceCm = null,
                        status = PeakMatchStatus.UNMATCHED_UNKNOWN,
                        diagnosticNote = "Unique peak in unknown"
                    )
                )
            }
        }

        // Add reference peaks that are absent in the unknown sample
        for (rp in referencePeaks) {
            if (!matchedRefPeaks.contains(rp) && rp in commonMin..commonMax) {
                peakMatches.add(
                    PeakComparisonMatch(
                        unknownPeakWn = null,
                        unknownPeakIntensity = null,
                        referencePeakWn = rp,
                        differenceCm = null,
                        status = PeakMatchStatus.UNMATCHED_REFERENCE,
                        diagnosticNote = "Diagnostic reference peak absent in unknown test spectrum"
                    )
                )
            }
        }

        val matchedCount = peakMatches.count { it.status == PeakMatchStatus.MATCHED }

        return NumericalComparisonResult(
            unknownSpectrum = unknown,
            reference = reference,
            primaryAlgorithm = primaryAlgorithm,
            similarityScore = primaryScore,
            allAlgorithmScores = scoresMap,
            commonRangeMinWn = commonMin,
            commonRangeMaxWn = commonMax,
            rangeOverlapRatio = overlapRatio,
            resampledGridPointsCount = n,
            gridSpacingCm = gridSpacingCm,
            differenceSpectrum = differenceSpectrum,
            peakMatches = peakMatches,
            matchedPeaksCount = matchedCount,
            totalUnknownPeaksCount = unknownPeaks.size,
            totalReferencePeaksCount = referencePeaks.size,
            scientificLimitations = limitations
        )
    }

    override fun extractStructuredAiEvidence(result: NumericalComparisonResult): StructuredAiComparisonEvidence {
        val scoresStringMap = result.allAlgorithmScores.mapKeys { it.key.shortName }
        val matchedSummaries = result.peakMatches
            .filter { it.status == PeakMatchStatus.MATCHED || it.status == PeakMatchStatus.SHIFTED }
            .map { match ->
                val uWn = "%.1f".format(Locale.US, match.unknownPeakWn ?: 0.0)
                val rWn = "%.1f".format(Locale.US, match.referencePeakWn ?: 0.0)
                val dWn = "%.1f".format(Locale.US, match.differenceCm ?: 0.0)
                "Unknown: $uWn cm⁻¹ -> Ref: $rWn cm⁻¹ (Δ $dWn cm⁻¹) [${match.status.label}]"
            }

        val missingRef = result.peakMatches
            .filter { it.status == PeakMatchStatus.UNMATCHED_REFERENCE }
            .mapNotNull { it.referencePeakWn }

        val uniqueUnknown = result.peakMatches
            .filter { it.status == PeakMatchStatus.UNMATCHED_UNKNOWN }
            .mapNotNull { it.unknownPeakWn }

        val avgShift = result.peakMatches
            .mapNotNull { it.differenceCm }
            .map { abs(it) }
            .let { if (it.isNotEmpty()) it.average() else 0.0 }

        return StructuredAiComparisonEvidence(
            unknownSpectrumId = result.unknownSpectrum.id,
            unknownSpectrumName = result.unknownSpectrum.name,
            referenceId = result.reference.id,
            referenceCompoundName = result.reference.compoundName,
            referenceCas = result.reference.casNumber,
            referenceFormula = result.reference.chemicalFormula,
            referenceMethod = result.reference.measurementMethod,
            similarityScores = scoresStringMap,
            overlappingRange = "${result.commonRangeMinWn.toInt()} - ${result.commonRangeMaxWn.toInt()} cm⁻¹",
            overlapPercentage = result.rangeOverlapRatio * 100.0,
            matchedPeaksSummary = matchedSummaries,
            missingReferencePeaks = missingRef,
            uniqueUnknownPeaks = uniqueUnknown,
            averagePeakShiftCm = avgShift,
            differenceSpectrumRms = result.differenceSpectrum.rmsDifference,
            limitationsNoted = result.scientificLimitations
        )
    }

    private fun interpolateSpectrum(points: List<SpectrumPoint>, grid: List<Double>): DoubleArray {
        if (points.isEmpty()) return DoubleArray(grid.size) { 0.0 }
        if (points.size == 1) return DoubleArray(grid.size) { points[0].signalValue }

        // Sort ascending by wavenumber for binary search
        val sorted = points.sortedBy { it.wavenumber }
        val out = DoubleArray(grid.size)

        for (i in grid.indices) {
            val targetWn = grid[i]

            // Boundary clamping
            if (targetWn <= sorted.first().wavenumber) {
                out[i] = sorted.first().signalValue
                continue
            }
            if (targetWn >= sorted.last().wavenumber) {
                out[i] = sorted.last().signalValue
                continue
            }

            // Binary search for interval [p_low, p_high]
            var low = 0
            var high = sorted.size - 1
            while (low <= high) {
                val mid = (low + high) ushr 1
                if (sorted[mid].wavenumber < targetWn) {
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }

            val idx1 = low.coerceIn(1, sorted.size - 1)
            val idx0 = idx1 - 1

            val p0 = sorted[idx0]
            val p1 = sorted[idx1]

            val span = p1.wavenumber - p0.wavenumber
            val frac = if (span > 1e-9) ((targetWn - p0.wavenumber) / span).coerceIn(0.0, 1.0) else 0.0
            out[i] = p0.signalValue + frac * (p1.signalValue - p0.signalValue)
        }

        return out
    }

    private fun normalizeVector(arr: DoubleArray): DoubleArray {
        if (arr.isEmpty()) return arr
        val minVal = arr.minOrNull() ?: 0.0
        val maxVal = arr.maxOrNull() ?: 1.0
        val span = max(1e-6, maxVal - minVal)
        val norm = DoubleArray(arr.size)
        for (i in arr.indices) {
            norm[i] = (arr[i] - minVal) / span
        }
        return norm
    }
}
