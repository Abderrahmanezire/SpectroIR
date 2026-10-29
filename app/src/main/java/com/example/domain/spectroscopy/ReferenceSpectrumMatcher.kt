package com.example.domain.spectroscopy

import com.example.domain.model.ReferenceSpectrum
import com.example.domain.model.Spectrum
import kotlin.math.abs
import kotlin.math.sqrt

data class MatchResult(
    val reference: ReferenceSpectrum,
    val score: Double, // 0.0 to 1.0 (1.0 = exact match)
    val matchedPeaksCount: Int,
    val matchedPeakPairs: List<Pair<Double, Double>> // (spectrumPeak, referencePeak)
)

interface ReferenceSpectrumMatcher {
    fun matchSpectrum(
        query: Spectrum,
        library: List<ReferenceSpectrum>,
        toleranceCm: Double = 15.0
    ): List<MatchResult>
}

class StandardReferenceSpectrumMatcher : ReferenceSpectrumMatcher {
    override fun matchSpectrum(
        query: Spectrum,
        library: List<ReferenceSpectrum>,
        toleranceCm: Double
    ): List<MatchResult> {
        val queryPeaks = query.detectedPeaks
        if (queryPeaks.isEmpty() || library.isEmpty()) return emptyList()

        val results = mutableListOf<MatchResult>()

        for (ref in library) {
            val refPeaks = ref.characteristicPeaks
            val matchedPairs = mutableListOf<Pair<Double, Double>>()

            for (qp in queryPeaks) {
                val closest = refPeaks.minByOrNull { abs(it - qp.wavenumber) }
                if (closest != null && abs(closest - qp.wavenumber) <= toleranceCm) {
                    matchedPairs.add(Pair(qp.wavenumber, closest))
                }
            }

            if (matchedPairs.isNotEmpty()) {
                val matchRatio = matchedPairs.size.toDouble() / refPeaks.size.coerceAtLeast(1)
                val avgDeviation = matchedPairs.map { abs(it.first - it.second) }.average()
                val score = (matchRatio * 0.8 + (1.0 - (avgDeviation / toleranceCm)).coerceIn(0.0, 1.0) * 0.2)
                    .coerceIn(0.0, 0.99)

                results.add(
                    MatchResult(
                        reference = ref,
                        score = score,
                        matchedPeaksCount = matchedPairs.size,
                        matchedPeakPairs = matchedPairs
                    )
                )
            }
        }

        return results.sortedByDescending { it.score }
    }
}

/**
 * Foundation for future camera capture & image/PDF spectrum digitization.
 */
interface SpectrumDigitizer {
    fun isSupportedFormat(mimeType: String): Boolean
    fun extractSpectrumFromImage(imageBytes: ByteArray, fileName: String): Result<Spectrum>
}

class StandardSpectrumDigitizer : SpectrumDigitizer {
    override fun isSupportedFormat(mimeType: String): Boolean {
        return mimeType in listOf("image/png", "image/jpeg", "image/webp", "application/pdf")
    }

    override fun extractSpectrumFromImage(imageBytes: ByteArray, fileName: String): Result<Spectrum> {
        // Foundation placeholder: image processing and grid OCR calibration scheduled for next phase
        return Result.failure(
            UnsupportedOperationException(
                "Image digitization engine will process bitmap curve extraction in the next stage. Use CSV/TXT file import for direct numerical spectrum loading."
            )
        )
    }
}
