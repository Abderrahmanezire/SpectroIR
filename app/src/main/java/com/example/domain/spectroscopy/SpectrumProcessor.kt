package com.example.domain.spectroscopy

import com.example.domain.model.PreprocessingStep
import com.example.domain.model.SignalUnit
import com.example.domain.model.Spectrum
import com.example.domain.model.SpectrumPoint
import com.example.domain.model.SpectrumType
import java.util.Locale
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

data class BaselineCorrectionResult(
    val correctedSpectrum: Spectrum,
    val baselinePoints: List<SpectrumPoint>
)

interface SpectrumProcessor {
    fun baselineCorrectPolynomial(spectrum: Spectrum, degree: Int = 3): BaselineCorrectionResult
    fun baselineCorrectRubberband(spectrum: Spectrum, numSegments: Int = 24): BaselineCorrectionResult
    fun baselineCorrectALS(
        spectrum: Spectrum,
        lambda: Double = 100000.0,
        p: Double = 0.01,
        maxIterations: Int = 10
    ): BaselineCorrectionResult
    fun baselineCorrectLinear(spectrum: Spectrum): BaselineCorrectionResult

    fun smoothSavitzkyGolay(spectrum: Spectrum, windowSize: Int = 9, polyOrder: Int = 2): Spectrum
    fun smoothMovingAverage(spectrum: Spectrum, windowSize: Int = 5): Spectrum

    fun normalizeMinMax(spectrum: Spectrum, targetMin: Double = 0.0, targetMax: Double = 100.0): Spectrum
    fun normalizeVector(spectrum: Spectrum): Spectrum
    fun normalizePeak(spectrum: Spectrum, targetWn: Double? = null, targetIntensity: Double = 100.0): Spectrum

    fun calculateDerivative(
        spectrum: Spectrum,
        order: Int = 1,
        sgWindowSize: Int = 9,
        sgPolyOrder: Int = 2
    ): Spectrum

    fun convertTransmittanceToAbsorbance(spectrum: Spectrum): Spectrum
    fun convertAbsorbanceToTransmittance(spectrum: Spectrum): Spectrum
    fun resetToOriginal(spectrum: Spectrum): Spectrum
}

class StandardSpectrumProcessor : SpectrumProcessor {

    override fun baselineCorrectPolynomial(spectrum: Spectrum, degree: Int): BaselineCorrectionResult {
        val points = spectrum.points
        if (points.size < 5) return BaselineCorrectionResult(spectrum, points)

        val x = DoubleArray(points.size) { points[it].wavenumber }
        val y = DoubleArray(points.size) { points[it].signalValue }
        val isAbsorbance = spectrum.spectrumType == SpectrumType.Absorbance

        // For absorbance, baseline passes through lowest points (valleys).
        // Find segment minima as anchor points to fit polynomial through baseline rather than peaks.
        val numAnchors = (points.size / 15).coerceIn(degree + 2, 40)
        val segSize = points.size / numAnchors
        val anchorX = mutableListOf<Double>()
        val anchorY = mutableListOf<Double>()

        for (i in 0 until numAnchors) {
            val start = i * segSize
            val end = if (i == numAnchors - 1) points.size else (i + 1) * segSize
            var bestIdx = start
            var bestVal = y[start]
            for (k in start until end) {
                if (isAbsorbance) {
                    if (y[k] < bestVal) {
                        bestVal = y[k]
                        bestIdx = k
                    }
                } else {
                    if (y[k] > bestVal) {
                        bestVal = y[k]
                        bestIdx = k
                    }
                }
            }
            anchorX.add(x[bestIdx])
            anchorY.add(y[bestIdx])
        }

        val polyCoeffs = ScientificMath.fitPolynomial(
            anchorX.toDoubleArray(),
            anchorY.toDoubleArray(),
            degree.coerceIn(1, 5)
        )

        val xMin = anchorX.minOrNull() ?: x.first()
        val xMax = anchorX.maxOrNull() ?: x.last()
        val xSpan = if (xMax - xMin > 0) xMax - xMin else 1.0

        val baselinePoints = ArrayList<SpectrumPoint>(points.size)
        val correctedPoints = ArrayList<SpectrumPoint>(points.size)

        for (i in points.indices) {
            val baseVal = ScientificMath.evaluateNormalizedPolynomial(polyCoeffs, x[i], xMin, xSpan)
            baselinePoints.add(SpectrumPoint(x[i], baseVal))

            val corr = if (isAbsorbance) {
                (y[i] - baseVal).coerceAtLeast(0.0)
            } else {
                (y[i] - baseVal + 100.0).coerceIn(0.0, 100.0)
            }
            correctedPoints.add(SpectrumPoint(x[i], corr))
        }

        val step = PreprocessingStep(
            stepType = "Baseline Correction",
            description = "Polynomial baseline correction (degree = $degree) applied.",
            parameters = mapOf(
                "method" to "Polynomial",
                "degree" to degree.toString()
            )
        )

        val updated = spectrum.copy(
            points = correctedPoints,
            rawPoints = if (spectrum.rawPoints.isEmpty()) points else spectrum.rawPoints,
            preprocessingHistory = spectrum.preprocessingHistory + step
        )
        return BaselineCorrectionResult(updated, baselinePoints)
    }

    override fun baselineCorrectRubberband(spectrum: Spectrum, numSegments: Int): BaselineCorrectionResult {
        val points = spectrum.points
        if (points.size < 5) return BaselineCorrectionResult(spectrum, points)

        val x = DoubleArray(points.size) { points[it].wavenumber }
        val y = DoubleArray(points.size) { points[it].signalValue }
        val isAbsorbance = spectrum.spectrumType == SpectrumType.Absorbance

        // If transmittance, invert to compute lower convex envelope then re-invert
        val calcY = if (isAbsorbance) y else DoubleArray(points.size) { 100.0 - y[it] }
        val baseCalc = ScientificMath.computeRubberbandBaseline(x, calcY, numSegments.coerceIn(8, 64))

        val baselinePoints = ArrayList<SpectrumPoint>(points.size)
        val correctedPoints = ArrayList<SpectrumPoint>(points.size)

        for (i in points.indices) {
            val baseVal = if (isAbsorbance) baseCalc[i] else 100.0 - baseCalc[i]
            baselinePoints.add(SpectrumPoint(x[i], baseVal))

            val corr = if (isAbsorbance) {
                (y[i] - baseVal).coerceAtLeast(0.0)
            } else {
                (y[i] - baseVal + 100.0).coerceIn(0.0, 100.0)
            }
            correctedPoints.add(SpectrumPoint(x[i], corr))
        }

        val step = PreprocessingStep(
            stepType = "Baseline Correction",
            description = "Rubberband convex-hull baseline ($numSegments segments) applied.",
            parameters = mapOf(
                "method" to "Rubberband",
                "segments" to numSegments.toString()
            )
        )

        val updated = spectrum.copy(
            points = correctedPoints,
            rawPoints = if (spectrum.rawPoints.isEmpty()) points else spectrum.rawPoints,
            preprocessingHistory = spectrum.preprocessingHistory + step
        )
        return BaselineCorrectionResult(updated, baselinePoints)
    }

    override fun baselineCorrectALS(
        spectrum: Spectrum,
        lambda: Double,
        p: Double,
        maxIterations: Int
    ): BaselineCorrectionResult {
        val points = spectrum.points
        if (points.size < 5) return BaselineCorrectionResult(spectrum, points)

        val x = DoubleArray(points.size) { points[it].wavenumber }
        val y = DoubleArray(points.size) { points[it].signalValue }
        val isAbsorbance = spectrum.spectrumType == SpectrumType.Absorbance

        // For absorbance, peaks point up: p ~ 0.01
        // For transmittance, absorption dips point down: p ~ 0.99
        val effectiveP = if (isAbsorbance) p.coerceIn(0.0001, 0.2) else (1.0 - p.coerceIn(0.0001, 0.2))

        val baseCalc = ScientificMath.computeAsymmetricLeastSquaresBaseline(
            y = y,
            smoothnessLambda = lambda.coerceIn(100.0, 10_000_000.0),
            asymmetryP = effectiveP,
            maxIterations = maxIterations.coerceIn(3, 30)
        )

        val baselinePoints = ArrayList<SpectrumPoint>(points.size)
        val correctedPoints = ArrayList<SpectrumPoint>(points.size)

        for (i in points.indices) {
            val baseVal = baseCalc[i]
            baselinePoints.add(SpectrumPoint(x[i], baseVal))

            val corr = if (isAbsorbance) {
                (y[i] - baseVal).coerceAtLeast(0.0)
            } else {
                (y[i] - baseVal + 100.0).coerceIn(0.0, 100.0)
            }
            correctedPoints.add(SpectrumPoint(x[i], corr))
        }

        val step = PreprocessingStep(
            stepType = "Baseline Correction",
            description = "Asymmetric Least Squares (AsLS) baseline (λ = ${"%.0e".format(Locale.US, lambda)}, p = $p) applied.",
            parameters = mapOf(
                "method" to "Asymmetric Least Squares (ALS)",
                "lambda" to lambda.toString(),
                "p" to p.toString(),
                "iterations" to maxIterations.toString()
            )
        )

        val updated = spectrum.copy(
            points = correctedPoints,
            rawPoints = if (spectrum.rawPoints.isEmpty()) points else spectrum.rawPoints,
            preprocessingHistory = spectrum.preprocessingHistory + step
        )
        return BaselineCorrectionResult(updated, baselinePoints)
    }

    override fun baselineCorrectLinear(spectrum: Spectrum): BaselineCorrectionResult {
        val points = spectrum.points
        if (points.size < 2) return BaselineCorrectionResult(spectrum, points)

        val firstPt = points.first()
        val lastPt = points.last()
        val dx = lastPt.wavenumber - firstPt.wavenumber
        if (dx == 0.0) return BaselineCorrectionResult(spectrum, points)

        val slope = (lastPt.signalValue - firstPt.signalValue) / dx
        val intercept = firstPt.signalValue - slope * firstPt.wavenumber

        val baselinePoints = ArrayList<SpectrumPoint>(points.size)
        val correctedPoints = ArrayList<SpectrumPoint>(points.size)

        for (pt in points) {
            val baselineVal = slope * pt.wavenumber + intercept
            baselinePoints.add(SpectrumPoint(pt.wavenumber, baselineVal))

            val adjusted = if (spectrum.spectrumType == SpectrumType.Absorbance) {
                (pt.signalValue - baselineVal).coerceAtLeast(0.0)
            } else {
                (pt.signalValue + (100.0 - baselineVal)).coerceIn(0.0, 100.0)
            }
            correctedPoints.add(SpectrumPoint(pt.wavenumber, adjusted))
        }

        val step = PreprocessingStep(
            stepType = "Baseline Correction",
            description = "Two-point linear baseline slope correction applied.",
            parameters = mapOf(
                "method" to "Linear 2-Point",
                "slope" to "%.4e".format(Locale.US, slope)
            )
        )

        val updated = spectrum.copy(
            points = correctedPoints,
            rawPoints = if (spectrum.rawPoints.isEmpty()) points else spectrum.rawPoints,
            preprocessingHistory = spectrum.preprocessingHistory + step
        )
        return BaselineCorrectionResult(updated, baselinePoints)
    }

    override fun smoothSavitzkyGolay(spectrum: Spectrum, windowSize: Int, polyOrder: Int): Spectrum {
        val points = spectrum.points
        if (points.size < 5) return spectrum

        val safeWin = if (windowSize % 2 == 0) windowSize + 1 else windowSize
        val clampedWin = safeWin.coerceIn(3, (points.size - 1) or 1)
        val clampedPoly = polyOrder.coerceIn(1, clampedWin - 1)

        val y = DoubleArray(points.size) { points[it].signalValue }
        val smoothed = ScientificMath.applySavitzkyGolayFilter(
            y = y,
            windowSize = clampedWin,
            polyOrder = clampedPoly,
            derivativeOrder = 0
        )

        val smoothedPoints = points.mapIndexed { i, pt ->
            val clampedSignal = if (spectrum.spectrumType == SpectrumType.Transmittance) {
                smoothed[i].coerceIn(0.0, 100.0)
            } else {
                smoothed[i].coerceAtLeast(0.0)
            }
            SpectrumPoint(pt.wavenumber, clampedSignal)
        }

        val step = PreprocessingStep(
            stepType = "Smoothing",
            description = "Savitzky-Golay smoothing (window = $clampedWin, order = $clampedPoly) applied.",
            parameters = mapOf(
                "method" to "Savitzky-Golay",
                "windowSize" to clampedWin.toString(),
                "polyOrder" to clampedPoly.toString()
            )
        )

        return spectrum.copy(
            points = smoothedPoints,
            rawPoints = if (spectrum.rawPoints.isEmpty()) points else spectrum.rawPoints,
            preprocessingHistory = spectrum.preprocessingHistory + step
        )
    }

    override fun smoothMovingAverage(spectrum: Spectrum, windowSize: Int): Spectrum {
        val points = spectrum.points
        val half = windowSize / 2
        if (points.size < windowSize) return spectrum

        val smoothedPoints = points.mapIndexed { i, pt ->
            val start = (i - half).coerceAtLeast(0)
            val end = (i + half).coerceAtMost(points.lastIndex)
            val avg = (start..end).map { points[it].signalValue }.average()
            SpectrumPoint(pt.wavenumber, avg)
        }

        val step = PreprocessingStep(
            stepType = "Smoothing",
            description = "Moving average filter (window = $windowSize) applied.",
            parameters = mapOf(
                "method" to "Moving Average",
                "windowSize" to windowSize.toString()
            )
        )

        return spectrum.copy(
            points = smoothedPoints,
            rawPoints = if (spectrum.rawPoints.isEmpty()) points else spectrum.rawPoints,
            preprocessingHistory = spectrum.preprocessingHistory + step
        )
    }

    override fun normalizeMinMax(spectrum: Spectrum, targetMin: Double, targetMax: Double): Spectrum {
        val points = spectrum.points
        if (points.isEmpty()) return spectrum

        val minVal = points.minOf { it.signalValue }
        val maxVal = points.maxOf { it.signalValue }
        val range = maxVal - minVal

        if (range <= 1e-9) return spectrum

        val targetRange = targetMax - targetMin
        val normalizedPoints = points.map { pt ->
            val norm = targetMin + ((pt.signalValue - minVal) / range) * targetRange
            SpectrumPoint(pt.wavenumber, norm)
        }

        val step = PreprocessingStep(
            stepType = "Normalization",
            description = "Min-Max normalization scaled to [${"%.1f".format(Locale.US, targetMin)}, ${"%.1f".format(Locale.US, targetMax)}].",
            parameters = mapOf(
                "method" to "Min-Max",
                "targetMin" to targetMin.toString(),
                "targetMax" to targetMax.toString(),
                "originalMin" to "%.3f".format(Locale.US, minVal),
                "originalMax" to "%.3f".format(Locale.US, maxVal)
            )
        )

        return spectrum.copy(
            points = normalizedPoints,
            rawPoints = if (spectrum.rawPoints.isEmpty()) points else spectrum.rawPoints,
            preprocessingHistory = spectrum.preprocessingHistory + step
        )
    }

    override fun normalizeVector(spectrum: Spectrum): Spectrum {
        val points = spectrum.points
        if (points.isEmpty()) return spectrum

        val y = DoubleArray(points.size) { points[it].signalValue }
        val normL2 = ScientificMath.computeVectorNorm(y)
        if (normL2 <= 1e-9) return spectrum

        val normalizedPoints = points.mapIndexed { i, pt ->
            SpectrumPoint(pt.wavenumber, y[i] / normL2)
        }

        val step = PreprocessingStep(
            stepType = "Normalization",
            description = "Vector normalization (Euclidean L2 norm ||y||₂ = ${"%.2f".format(Locale.US, normL2)}) applied.",
            parameters = mapOf(
                "method" to "Vector L2 Normalization",
                "normL2" to "%.4f".format(Locale.US, normL2)
            )
        )

        return spectrum.copy(
            points = normalizedPoints,
            yUnit = "Normalized (L2)",
            rawPoints = if (spectrum.rawPoints.isEmpty()) points else spectrum.rawPoints,
            preprocessingHistory = spectrum.preprocessingHistory + step
        )
    }

    override fun normalizePeak(spectrum: Spectrum, targetWn: Double?, targetIntensity: Double): Spectrum {
        val points = spectrum.points
        if (points.isEmpty()) return spectrum

        val isAbsorbance = spectrum.spectrumType == SpectrumType.Absorbance
        val referencePt = if (targetWn != null) {
            points.minByOrNull { abs(it.wavenumber - targetWn) } ?: points.first()
        } else {
            if (isAbsorbance) points.maxByOrNull { it.signalValue } ?: points.first()
            else points.minByOrNull { it.signalValue } ?: points.first()
        }

        val refValue = referencePt.signalValue
        if (abs(refValue) < 1e-9) return spectrum

        val scale = targetIntensity / refValue
        val normalizedPoints = points.map { pt ->
            SpectrumPoint(pt.wavenumber, pt.signalValue * scale)
        }

        val step = PreprocessingStep(
            stepType = "Normalization",
            description = "Peak normalization at ${"%.1f".format(Locale.US, referencePt.wavenumber)} cm⁻¹ scaled to ${"%.1f".format(Locale.US, targetIntensity)}.",
            parameters = mapOf(
                "method" to "Peak Normalization",
                "targetWn" to "%.1f".format(Locale.US, referencePt.wavenumber),
                "targetIntensity" to targetIntensity.toString(),
                "scaleFactor" to "%.4f".format(Locale.US, scale)
            )
        )

        return spectrum.copy(
            points = normalizedPoints,
            rawPoints = if (spectrum.rawPoints.isEmpty()) points else spectrum.rawPoints,
            preprocessingHistory = spectrum.preprocessingHistory + step
        )
    }

    override fun calculateDerivative(
        spectrum: Spectrum,
        order: Int,
        sgWindowSize: Int,
        sgPolyOrder: Int
    ): Spectrum {
        val points = spectrum.points
        if (points.size < 5) return spectrum

        val clampedOrder = order.coerceIn(1, 2)
        val safeWin = if (sgWindowSize % 2 == 0) sgWindowSize + 1 else sgWindowSize
        val clampedWin = safeWin.coerceIn(3, (points.size - 1) or 1)
        val clampedPoly = sgPolyOrder.coerceIn(clampedOrder, clampedWin - 1)

        val y = DoubleArray(points.size) { points[it].signalValue }
        val avgDx = if (points.size > 1) {
            abs(points.last().wavenumber - points.first().wavenumber) / (points.size - 1)
        } else 1.0

        val deriv = ScientificMath.applySavitzkyGolayFilter(
            y = y,
            windowSize = clampedWin,
            polyOrder = clampedPoly,
            derivativeOrder = clampedOrder,
            averageDeltaX = avgDx
        )

        val derivPoints = points.mapIndexed { i, pt ->
            SpectrumPoint(pt.wavenumber, deriv[i])
        }

        val derivSymbol = if (clampedOrder == 1) "dY/dν" else "d²Y/dν²"
        val step = PreprocessingStep(
            stepType = "Derivative",
            description = "$clampedOrder${if (clampedOrder == 1) "st" else "nd"} derivative ($derivSymbol, SG win=$clampedWin, poly=$clampedPoly) computed.",
            parameters = mapOf(
                "order" to clampedOrder.toString(),
                "sgWindowSize" to clampedWin.toString(),
                "sgPolyOrder" to clampedPoly.toString()
            )
        )

        return spectrum.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${spectrum.name} ($derivSymbol)",
            spectrumType = SpectrumType.Intensity,
            yUnit = derivSymbol,
            points = derivPoints,
            rawPoints = if (spectrum.rawPoints.isEmpty()) points else spectrum.rawPoints,
            preprocessingHistory = spectrum.preprocessingHistory + step,
            detectedPeaks = emptyList() // Needs fresh peak detection for derivative extrema
        )
    }

    override fun convertTransmittanceToAbsorbance(spectrum: Spectrum): Spectrum {
        if (spectrum.spectrumType != SpectrumType.Transmittance) return spectrum

        val convertedPoints = spectrum.points.map { pt ->
            val clampedT = pt.signalValue.coerceIn(0.001, 100.0)
            val absVal = (2.0 - log10(clampedT)).coerceAtLeast(0.0)
            SpectrumPoint(pt.wavenumber, absVal)
        }

        val step = PreprocessingStep(
            stepType = "Conversion",
            description = "Converted %Transmittance to Absorbance (A = 2 - log₁₀(%T)).",
            parameters = mapOf("from" to "%T", "to" to "Absorbance")
        )

        return spectrum.copy(
            spectrumType = SpectrumType.Absorbance,
            yUnit = SignalUnit.ABSORBANCE.symbol,
            points = convertedPoints,
            rawPoints = if (spectrum.rawPoints.isEmpty()) spectrum.points else spectrum.rawPoints,
            preprocessingHistory = spectrum.preprocessingHistory + step
        )
    }

    override fun convertAbsorbanceToTransmittance(spectrum: Spectrum): Spectrum {
        if (spectrum.spectrumType != SpectrumType.Absorbance) return spectrum

        val convertedPoints = spectrum.points.map { pt ->
            val clampedA = pt.signalValue.coerceIn(0.0, 4.0)
            val tVal = 10.0.pow(2.0 - clampedA).coerceIn(0.0, 100.0)
            SpectrumPoint(pt.wavenumber, tVal)
        }

        val step = PreprocessingStep(
            stepType = "Conversion",
            description = "Converted Absorbance to %Transmittance (%T = 10^(2 - A)).",
            parameters = mapOf("from" to "Absorbance", "to" to "%T")
        )

        return spectrum.copy(
            spectrumType = SpectrumType.Transmittance,
            yUnit = SignalUnit.TRANSMITTANCE_PERCENT.symbol,
            points = convertedPoints,
            rawPoints = if (spectrum.rawPoints.isEmpty()) spectrum.points else spectrum.rawPoints,
            preprocessingHistory = spectrum.preprocessingHistory + step
        )
    }

    override fun resetToOriginal(spectrum: Spectrum): Spectrum {
        val orig = if (spectrum.rawPoints.isNotEmpty()) spectrum.rawPoints else spectrum.points
        return spectrum.copy(
            points = orig,
            rawPoints = orig,
            preprocessingHistory = emptyList(),
            detectedPeaks = emptyList()
        )
    }
}
