package com.example.domain.spectroscopy

import com.example.domain.model.Peak
import com.example.domain.model.PeakIntensity
import com.example.domain.model.Spectrum
import com.example.domain.model.SpectrumPoint
import com.example.domain.model.SpectrumType
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

interface PeakDetector {
    fun detectPeaks(
        spectrum: Spectrum,
        minProminence: Double = 5.0,
        minDistance: Double = 10.0, // Minimum wavenumber distance in cm⁻¹
        minHeight: Double = 0.0,     // Minimum absolute signal height
        searchWindow: Int = 3        // Local extremum neighborhood
    ): List<Peak>

    fun createManualPeak(
        spectrum: Spectrum,
        targetWn: Double
    ): Peak
}

class StandardPeakDetector : PeakDetector {

    override fun detectPeaks(
        spectrum: Spectrum,
        minProminence: Double,
        minDistance: Double,
        minHeight: Double,
        searchWindow: Int
    ): List<Peak> {
        val points = spectrum.points
        val halfWin = max(1, searchWindow)
        if (points.size < halfWin * 2 + 1) return emptyList()

        val isAbsorbance = spectrum.spectrumType == SpectrumType.Absorbance ||
                           spectrum.spectrumType == SpectrumType.Intensity
        val candidatePeaks = mutableListOf<Peak>()

        for (i in halfWin until points.size - halfWin) {
            val currPt = points[i]
            val currY = currPt.signalValue

            var isExtremum = true
            if (isAbsorbance) {
                // Peak is local maximum
                for (j in 1..halfWin) {
                    if (points[i - j].signalValue >= currY || points[i + j].signalValue > currY) {
                        isExtremum = false
                        break
                    }
                }
                if (!isExtremum || currY < minHeight) continue

                // Compute valleys and prominence
                val leftValleyIdx = findLeftValley(points, i, isAbsorbance)
                val rightValleyIdx = findRightValley(points, i, isAbsorbance)

                val leftBase = points[leftValleyIdx].signalValue
                val rightBase = points[rightValleyIdx].signalValue
                val baseLevel = max(leftBase, rightBase)
                val prominence = currY - baseLevel

                if (prominence >= minProminence) {
                    val fwhm = calculateFwhm(points, i, baseLevel, prominence, isAbsorbance)
                    val baseWidth = abs(points[rightValleyIdx].wavenumber - points[leftValleyIdx].wavenumber)
                    val area = calculatePeakArea(points, leftValleyIdx, rightValleyIdx, baseLevel, isAbsorbance)

                    candidatePeaks.add(
                        Peak(
                            id = UUID.randomUUID().toString(),
                            wavenumber = currPt.wavenumber,
                            signalValue = currY,
                            prominence = prominence,
                            width = baseWidth,
                            fwhm = fwhm,
                            area = area,
                            isManual = false
                        )
                    )
                }
            } else {
                // Transmittance: peak is transmission trough / dip (local minimum)
                for (j in 1..halfWin) {
                    if (points[i - j].signalValue <= currY || points[i + j].signalValue < currY) {
                        isExtremum = false
                        break
                    }
                }
                if (!isExtremum) continue

                val leftValleyIdx = findLeftValley(points, i, isAbsorbance)
                val rightValleyIdx = findRightValley(points, i, isAbsorbance)

                val leftBase = points[leftValleyIdx].signalValue
                val rightBase = points[rightValleyIdx].signalValue
                val baseLevel = min(leftBase, rightBase)
                val prominence = baseLevel - currY

                if (prominence >= minProminence) {
                    val fwhm = calculateFwhm(points, i, baseLevel, prominence, isAbsorbance)
                    val baseWidth = abs(points[rightValleyIdx].wavenumber - points[leftValleyIdx].wavenumber)
                    val area = calculatePeakArea(points, leftValleyIdx, rightValleyIdx, baseLevel, isAbsorbance)

                    candidatePeaks.add(
                        Peak(
                            id = UUID.randomUUID().toString(),
                            wavenumber = currPt.wavenumber,
                            signalValue = currY,
                            prominence = prominence,
                            width = baseWidth,
                            fwhm = fwhm,
                            area = area,
                            isManual = false
                        )
                    )
                }
            }
        }

        // Apply Minimum Distance filter (suppress neighboring peaks within minDistance, retaining higher prominence)
        val distanceFiltered = filterByMinDistance(candidatePeaks, minDistance)

        // Calculate relative intensities and assign qualitative intensity category
        val maxProminence = distanceFiltered.maxOfOrNull { it.prominence }?.takeIf { it > 0 } ?: 1.0
        val finalPeaks = distanceFiltered
            .sortedByDescending { it.wavenumber }
            .mapIndexed { idx, peak ->
                val relIntensity = (peak.prominence / maxProminence) * 100.0
                peak.copy(
                    index = idx + 1,
                    relativeIntensity = relIntensity,
                    intensity = categorizeIntensity(relIntensity)
                )
            }

        return finalPeaks
    }

    override fun createManualPeak(spectrum: Spectrum, targetWn: Double): Peak {
        val points = spectrum.points
        if (points.isEmpty()) {
            return Peak(
                wavenumber = targetWn,
                signalValue = 0.0,
                isManual = true
            )
        }

        val nearestIdx = points.indices.minByOrNull { abs(points[it].wavenumber - targetWn) } ?: 0
        val isAbsorbance = spectrum.spectrumType == SpectrumType.Absorbance ||
                           spectrum.spectrumType == SpectrumType.Intensity

        // Search in a local +/- 5 point window for true peak extremum
        val searchRadius = 5
        val startIdx = max(0, nearestIdx - searchRadius)
        val endIdx = min(points.lastIndex, nearestIdx + searchRadius)
        val peakIdx = if (isAbsorbance) {
            (startIdx..endIdx).maxByOrNull { points[it].signalValue } ?: nearestIdx
        } else {
            (startIdx..endIdx).minByOrNull { points[it].signalValue } ?: nearestIdx
        }

        val peakPt = points[peakIdx]
        val leftIdx = findLeftValley(points, peakIdx, isAbsorbance)
        val rightIdx = findRightValley(points, peakIdx, isAbsorbance)

        val baseLevel = if (isAbsorbance) {
            max(points[leftIdx].signalValue, points[rightIdx].signalValue)
        } else {
            min(points[leftIdx].signalValue, points[rightIdx].signalValue)
        }

        val prominence = if (isAbsorbance) {
            (peakPt.signalValue - baseLevel).coerceAtLeast(0.01)
        } else {
            (baseLevel - peakPt.signalValue).coerceAtLeast(0.01)
        }

        val fwhm = calculateFwhm(points, peakIdx, baseLevel, prominence, isAbsorbance)
        val baseWidth = abs(points[rightIdx].wavenumber - points[leftIdx].wavenumber)
        val area = calculatePeakArea(points, leftIdx, rightIdx, baseLevel, isAbsorbance)

        return Peak(
            id = UUID.randomUUID().toString(),
            index = 1,
            wavenumber = peakPt.wavenumber,
            signalValue = peakPt.signalValue,
            prominence = prominence,
            width = baseWidth,
            fwhm = fwhm,
            area = area,
            relativeIntensity = 100.0,
            intensity = PeakIntensity.MEDIUM,
            isManual = true,
            notes = "Manually selected peak"
        )
    }

    private fun findLeftValley(points: List<SpectrumPoint>, peakIdx: Int, isAbsorbance: Boolean): Int {
        var minIdx = peakIdx
        val maxLookback = min(30, peakIdx)
        for (k in 1..maxLookback) {
            val idx = peakIdx - k
            if (isAbsorbance) {
                if (points[idx].signalValue <= points[minIdx].signalValue) {
                    minIdx = idx
                } else if (k > 3 && points[idx].signalValue > points[minIdx].signalValue + 0.05) {
                    break // Valley reached, signal started rising again
                }
            } else {
                if (points[idx].signalValue >= points[minIdx].signalValue) {
                    minIdx = idx
                } else if (k > 3 && points[idx].signalValue < points[minIdx].signalValue - 0.05) {
                    break // Shoulder peak reached
                }
            }
        }
        return minIdx
    }

    private fun findRightValley(points: List<SpectrumPoint>, peakIdx: Int, isAbsorbance: Boolean): Int {
        var minIdx = peakIdx
        val maxLookahead = min(30, points.lastIndex - peakIdx)
        for (k in 1..maxLookahead) {
            val idx = peakIdx + k
            if (isAbsorbance) {
                if (points[idx].signalValue <= points[minIdx].signalValue) {
                    minIdx = idx
                } else if (k > 3 && points[idx].signalValue > points[minIdx].signalValue + 0.05) {
                    break
                }
            } else {
                if (points[idx].signalValue >= points[minIdx].signalValue) {
                    minIdx = idx
                } else if (k > 3 && points[idx].signalValue < points[minIdx].signalValue - 0.05) {
                    break
                }
            }
        }
        return minIdx
    }

    private fun calculateFwhm(
        points: List<SpectrumPoint>,
        peakIdx: Int,
        baseLevel: Double,
        prominence: Double,
        isAbsorbance: Boolean
    ): Double {
        if (points.size < 3 || prominence <= 1e-6) return 0.0

        val halfLevel = if (isAbsorbance) {
            baseLevel + prominence / 2.0
        } else {
            baseLevel - prominence / 2.0
        }

        // Left crossing
        var leftCrossingWn = points[max(0, peakIdx - 1)].wavenumber
        for (k in peakIdx downTo 1) {
            val y1 = points[k].signalValue
            val y0 = points[k - 1].signalValue
            val x1 = points[k].wavenumber
            val x0 = points[k - 1].wavenumber

            val crosses = if (isAbsorbance) {
                (y1 >= halfLevel && y0 <= halfLevel) || (y1 <= halfLevel && y0 >= halfLevel)
            } else {
                (y1 <= halfLevel && y0 >= halfLevel) || (y1 >= halfLevel && y0 <= halfLevel)
            }

            if (crosses && abs(y1 - y0) > 1e-9) {
                val t = (halfLevel - y0) / (y1 - y0)
                leftCrossingWn = x0 + t * (x1 - x0)
                break
            }
        }

        // Right crossing
        var rightCrossingWn = points[min(points.lastIndex, peakIdx + 1)].wavenumber
        for (k in peakIdx until points.lastIndex) {
            val y0 = points[k].signalValue
            val y1 = points[k + 1].signalValue
            val x0 = points[k].wavenumber
            val x1 = points[k + 1].wavenumber

            val crosses = if (isAbsorbance) {
                (y0 >= halfLevel && y1 <= halfLevel) || (y0 <= halfLevel && y1 >= halfLevel)
            } else {
                (y0 <= halfLevel && y1 >= halfLevel) || (y0 >= halfLevel && y1 <= halfLevel)
            }

            if (crosses && abs(y1 - y0) > 1e-9) {
                val t = (halfLevel - y0) / (y1 - y0)
                rightCrossingWn = x0 + t * (x1 - x0)
                break
            }
        }

        return abs(rightCrossingWn - leftCrossingWn)
    }

    private fun calculatePeakArea(
        points: List<SpectrumPoint>,
        leftIdx: Int,
        rightIdx: Int,
        baseLevel: Double,
        isAbsorbance: Boolean
    ): Double {
        val start = min(leftIdx, rightIdx)
        val end = max(leftIdx, rightIdx)
        if (end <= start) return 0.0

        var area = 0.0
        for (i in start until end) {
            val dx = abs(points[i + 1].wavenumber - points[i].wavenumber)
            val h1 = if (isAbsorbance) {
                (points[i].signalValue - baseLevel).coerceAtLeast(0.0)
            } else {
                (baseLevel - points[i].signalValue).coerceAtLeast(0.0)
            }
            val h2 = if (isAbsorbance) {
                (points[i + 1].signalValue - baseLevel).coerceAtLeast(0.0)
            } else {
                (baseLevel - points[i + 1].signalValue).coerceAtLeast(0.0)
            }
            area += 0.5 * (h1 + h2) * dx
        }
        return area
    }

    private fun filterByMinDistance(peaks: List<Peak>, minDistance: Double): List<Peak> {
        if (peaks.size <= 1 || minDistance <= 0.0) return peaks

        // Sort descending by prominence so stronger peaks suppress weaker neighbors
        val sortedByProminence = peaks.sortedByDescending { it.prominence }
        val kept = mutableListOf<Peak>()

        for (cand in sortedByProminence) {
            val isTooClose = kept.any { abs(it.wavenumber - cand.wavenumber) < minDistance }
            if (!isTooClose) {
                kept.add(cand)
            }
        }

        return kept
    }

    private fun categorizeIntensity(relativeIntensity: Double): PeakIntensity {
        return when {
            relativeIntensity >= 75.0 -> PeakIntensity.VERY_STRONG
            relativeIntensity >= 45.0 -> PeakIntensity.STRONG
            relativeIntensity >= 20.0 -> PeakIntensity.MEDIUM
            else -> PeakIntensity.WEAK
        }
    }
}
