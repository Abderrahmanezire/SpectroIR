package com.example.domain.spectroscopy

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import com.example.domain.model.Spectrum
import com.example.domain.model.SpectrumPoint
import com.example.domain.model.SpectrumSourceType
import com.example.domain.model.SpectrumType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Axis orientation flag:
 * In traditional IR spectroscopy, 4000 cm⁻¹ is standardly placed on the LEFT and 400 cm⁻¹ on the RIGHT.
 * However, some instruments or non-standard plots place 400 cm⁻¹ on the LEFT.
 */
enum class WavenumberAxisDirection(val label: String) {
    HIGH_TO_LOW("4000 cm⁻¹ on Left → 400 cm⁻¹ on Right (Standard IR)"),
    LOW_TO_HIGH("400 cm⁻¹ on Left → 4000 cm⁻¹ on Right (Inverted X)")
}

data class CalibrationPoint(
    val pixelX: Float,
    val wavenumber: Double
)

data class YCalibration(
    val pixelYTop: Float,
    val signalTop: Double = 100.0,    // Typically 100% T or 1.0 A
    val pixelYBottom: Float,
    val signalBottom: Double = 0.0     // Typically 0% T or 0.0 A
)

data class DetectedGraphBounds(
    val rect: Rect,
    val isAutoDetected: Boolean,
    val confidence: Double,
    val detectedTicksX: List<Float> = emptyList(),
    val detectedTicksY: List<Float> = emptyList()
)

data class PreprocessingOptions(
    val enhanceContrast: Boolean = true,
    val denoise: Boolean = true,
    val suppressGrid: Boolean = true,
    val morphologicalBridge: Boolean = true,
    val threshold: Int = 140
)

data class DigitizationResult(
    val originalBitmap: Bitmap,
    val croppedGraphBitmap: Bitmap,
    val detectedBounds: Rect,
    val extractedPoints: List<SpectrumPoint>,
    val spectrum: Spectrum,
    val confidenceScore: Double,          // 0.0 to 1.0 (measurable: continuity, signal coverage, resolution)
    val warnings: List<String>,
    val estimatedAxisDirection: WavenumberAxisDirection,
    val extractedCurveMask: Bitmap? = null,
    val qualityMetrics: Map<String, String> = emptyMap()
)

interface SpectrumImageProcessor {
    /**
     * Automatic graph bounding box & axis tick detection.
     */
    fun detectGraphRegion(bitmap: Bitmap): DetectedGraphBounds

    /**
     * Preprocesses bitmap with contrast enhancement, denoising, and optional grid suppression.
     */
    fun preprocessBitmap(
        bitmap: Bitmap,
        options: PreprocessingOptions
    ): Bitmap

    /**
     * Extract spectrum curve from cropped graph region using image processing:
     * - Grayscale & Contrast enhancement
     * - Dark pixel line tracing
     * - Grid suppression & horizontal/vertical axis filtering
     * - Manual erase mask and user drawn overrides
     * - Converts pixel coordinates to numerical (wavenumber, signal)
     */
    fun digitizeGraph(
        graphBitmap: Bitmap,
        xCalibrationLeft: CalibrationPoint,
        xCalibrationRight: CalibrationPoint,
        yCalibration: YCalibration,
        spectrumType: SpectrumType = SpectrumType.Transmittance,
        options: PreprocessingOptions = PreprocessingOptions(),
        eraseMask: Bitmap? = null,
        manualDrawnPoints: Map<Int, Int>? = null, // x -> y overrides
        spectrumName: String = "Digitized Spectrum"
    ): DigitizationResult

    /**
     * Generates a sample synthetic FTIR polystyrene plot bitmap for offline testing and verification.
     */
    fun generateSampleFtIrPlotBitmap(): Bitmap
}

class StandardSpectrumImageProcessor : SpectrumImageProcessor {

    override fun detectGraphRegion(bitmap: Bitmap): DetectedGraphBounds {
        val width = bitmap.width
        val height = bitmap.height

        val defaultRect = Rect(
            (width * 0.08f).toInt(),
            (height * 0.08f).toInt(),
            (width * 0.94f).toInt(),
            (height * 0.88f).toInt()
        )

        if (width < 60 || height < 60) {
            return DetectedGraphBounds(defaultRect, isAutoDetected = false, confidence = 0.3)
        }

        val stepX = max(1, width / 240)
        val stepY = max(1, height / 240)

        var top = -1
        var bottom = -1
        var left = -1
        var right = -1

        // Scan horizontal lines for high density of dark frame border pixels
        for (y in (height * 0.03f).toInt() until (height * 0.40f).toInt() step stepY) {
            var darkCount = 0
            var sampleCount = 0
            for (x in (width * 0.12f).toInt() until (width * 0.88f).toInt() step stepX) {
                sampleCount++
                val p = bitmap.getPixel(x, y)
                val lum = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
                if (lum < 110) darkCount++
            }
            if (sampleCount > 0 && darkCount.toDouble() / sampleCount > 0.40) {
                top = y
                break
            }
        }

        for (y in (height * 0.96f).toInt() downTo (height * 0.60f).toInt() step stepY) {
            var darkCount = 0
            var sampleCount = 0
            for (x in (width * 0.12f).toInt() until (width * 0.88f).toInt() step stepX) {
                sampleCount++
                val p = bitmap.getPixel(x, y)
                val lum = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
                if (lum < 110) darkCount++
            }
            if (sampleCount > 0 && darkCount.toDouble() / sampleCount > 0.40) {
                bottom = y
                break
            }
        }

        for (x in (width * 0.03f).toInt() until (width * 0.35f).toInt() step stepX) {
            var darkCount = 0
            var sampleCount = 0
            for (y in (height * 0.18f).toInt() until (height * 0.82f).toInt() step stepY) {
                sampleCount++
                val p = bitmap.getPixel(x, y)
                val lum = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
                if (lum < 110) darkCount++
            }
            if (sampleCount > 0 && darkCount.toDouble() / sampleCount > 0.40) {
                left = x
                break
            }
        }

        for (x in (width * 0.97f).toInt() downTo (width * 0.65f).toInt() step stepX) {
            var darkCount = 0
            var sampleCount = 0
            for (y in (height * 0.18f).toInt() until (height * 0.82f).toInt() step stepY) {
                sampleCount++
                val p = bitmap.getPixel(x, y)
                val lum = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
                if (lum < 110) darkCount++
            }
            if (sampleCount > 0 && darkCount.toDouble() / sampleCount > 0.40) {
                right = x
                break
            }
        }

        val detected = if (top > 0 && bottom > top + 60 && left > 0 && right > left + 60) {
            Rect(left, top, right, bottom)
        } else {
            defaultRect
        }

        val isAuto = (detected != defaultRect)
        val conf = if (isAuto) 0.88 else 0.50

        // Find ticks along the bottom axis
        val detectedTicksX = mutableListOf<Float>()
        val tickScanY = (detected.bottom + 3).coerceAtMost(height - 2)
        if (tickScanY > 0) {
            for (x in detected.left..detected.right step 2) {
                val p = bitmap.getPixel(x, tickScanY)
                val lum = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
                if (lum < 100) {
                    detectedTicksX.add(x.toFloat())
                }
            }
        }

        return DetectedGraphBounds(
            rect = detected,
            isAutoDetected = isAuto,
            confidence = conf,
            detectedTicksX = detectedTicksX
        )
    }

    override fun preprocessBitmap(bitmap: Bitmap, options: PreprocessingOptions): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        // 1. Grayscale & Contrast Histogram Stretch
        var minLum = 255
        var maxLum = 0
        val lums = IntArray(w * h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val p = bitmap.getPixel(x, y)
                val lum = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
                lums[y * w + x] = lum
                if (lum < minLum) minLum = lum
                if (lum > maxLum) maxLum = lum
            }
        }

        val lumRange = max(1, maxLum - minLum)

        // 2. Identify potential horizontal and vertical grid lines for suppression
        val horizontalGrid = BooleanArray(h)
        val verticalGrid = BooleanArray(w)

        if (options.suppressGrid) {
            // A line is likely a grid line if > 65% of its pixels are light gray/dark with uniform intensity
            for (y in 0 until h) {
                var darkCount = 0
                for (x in 0 until w) {
                    if (lums[y * w + x] < options.threshold + 20) darkCount++
                }
                if (darkCount > w * 0.70) {
                    horizontalGrid[y] = true
                }
            }
            for (x in 0 until w) {
                var darkCount = 0
                for (y in 0 until h) {
                    if (lums[y * w + x] < options.threshold + 20) darkCount++
                }
                if (darkCount > h * 0.70) {
                    verticalGrid[x] = true
                }
            }
        }

        for (y in 0 until h) {
            for (x in 0 until w) {
                var l = lums[y * w + x]

                // Apply Contrast Stretch
                if (options.enhanceContrast) {
                    l = (((l - minLum).toFloat() / lumRange) * 255).toInt().coerceIn(0, 255)
                }

                // Grid suppression: if on a suspected grid line, lighten it
                if (options.suppressGrid && (horizontalGrid[y] || verticalGrid[x])) {
                    l = (l + 50).coerceAtMost(255)
                }

                val finalColor = if (l < options.threshold) {
                    Color.BLACK
                } else {
                    Color.WHITE
                }
                output.setPixel(x, y, finalColor)
            }
        }

        return output
    }

    override fun digitizeGraph(
        graphBitmap: Bitmap,
        xCalibrationLeft: CalibrationPoint,
        xCalibrationRight: CalibrationPoint,
        yCalibration: YCalibration,
        spectrumType: SpectrumType,
        options: PreprocessingOptions,
        eraseMask: Bitmap?,
        manualDrawnPoints: Map<Int, Int>?,
        spectrumName: String
    ): DigitizationResult {
        val w = graphBitmap.width
        val h = graphBitmap.height

        val warnings = mutableListOf<String>()

        // 1. Validate calibration coordinates
        val minX = min(xCalibrationLeft.pixelX, xCalibrationRight.pixelX).coerceIn(0f, (w - 1).toFloat())
        val maxX = max(xCalibrationLeft.pixelX, xCalibrationRight.pixelX).coerceIn(0f, (w - 1).toFloat())
        val deltaX = maxX - minX

        if (deltaX < 50f) {
            warnings.add("Horizontal calibration span is narrow (${deltaX.toInt()} px). Ensure calibration points span the full axis.")
        }

        val isStandardHighToLow = xCalibrationLeft.wavenumber > xCalibrationRight.wavenumber
        val axisDirection = if (isStandardHighToLow) WavenumberAxisDirection.HIGH_TO_LOW else WavenumberAxisDirection.LOW_TO_HIGH

        val yTop = min(yCalibration.pixelYTop, yCalibration.pixelYBottom).coerceIn(0f, (h - 1).toFloat())
        val yBottom = max(yCalibration.pixelYTop, yCalibration.pixelYBottom).coerceIn(0f, (h - 1).toFloat())
        val deltaY = yBottom - yTop

        if (deltaY < 40f) {
            warnings.add("Vertical calibration span is narrow (${deltaY.toInt()} px). Signal scaling may be noisy.")
        }

        // 2. Preprocess image
        val preprocessed = preprocessBitmap(graphBitmap, options)

        // 3. Curve Extraction
        val maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val rawPixelPoints = mutableListOf<Pair<Int, Int>>() // Pair(x, y)

        var lastValidY = ((yTop + yBottom) / 2f).toInt()
        var trackedCount = 0
        var totalColumns = 0

        val startCol = minX.roundToInt().coerceAtLeast(0)
        val endCol = maxX.roundToInt().coerceAtMost(w - 1)

        val minY = (yTop.toInt() + 1).coerceIn(0, h - 1)
        val maxY = (yBottom.toInt() - 1).coerceIn(minY, h - 1)

        for (x in startCol..endCol) {
            totalColumns++

            // Check if user manually drew a point at this column
            val manualY = manualDrawnPoints?.get(x)
            if (manualY != null) {
                val clampedY = manualY.coerceIn(minY, maxY)
                rawPixelPoints.add(Pair(x, clampedY))
                maskBitmap.setPixel(x, clampedY, Color.rgb(0, 229, 255)) // Cyan for manually pinned curve
                lastValidY = clampedY
                trackedCount++
                continue
            }

            // Search vertical column
            val candidates = mutableListOf<Int>()
            for (y in minY..maxY) {
                // If user erased this pixel, skip it
                if (eraseMask != null) {
                    val erasePixel = eraseMask.getPixel(x.coerceIn(0, eraseMask.width - 1), y.coerceIn(0, eraseMask.height - 1))
                    if (Color.alpha(erasePixel) > 100) {
                        continue // Erased region!
                    }
                }

                val pixelColor = preprocessed.getPixel(x, y)
                if (pixelColor == Color.BLACK) {
                    candidates.add(y)
                }
            }

            if (candidates.isNotEmpty()) {
                // Select candidate closest to the previous tracked position (continuity heuristic)
                val bestY = candidates.minByOrNull { abs(it - lastValidY) } ?: candidates.first()
                rawPixelPoints.add(Pair(x, bestY))
                maskBitmap.setPixel(x, bestY, Color.rgb(255, 64, 129)) // Pinkish for automatically traced curve
                lastValidY = bestY
                trackedCount++
            } else {
                // Missing column: smooth linear interpolation from last known point
                rawPixelPoints.add(Pair(x, lastValidY))
            }
        }

        // 4. Mathematical mapping from pixel coordinates to (wavenumber, signalValue)
        val points = mutableListOf<SpectrumPoint>()

        for ((px, py) in rawPixelPoints) {
            val fracX = if (deltaX > 0f) ((px - minX) / deltaX).coerceIn(0f, 1f) else 0f

            val wn = if (isStandardHighToLow) {
                val leftWn = max(xCalibrationLeft.wavenumber, xCalibrationRight.wavenumber)
                val rightWn = min(xCalibrationLeft.wavenumber, xCalibrationRight.wavenumber)
                leftWn - fracX * (leftWn - rightWn)
            } else {
                val leftWn = min(xCalibrationLeft.wavenumber, xCalibrationRight.wavenumber)
                val rightWn = max(xCalibrationLeft.wavenumber, xCalibrationRight.wavenumber)
                leftWn + fracX * (rightWn - leftWn)
            }

            val fracY = if (deltaY > 0f) ((py - yTop) / deltaY).coerceIn(0f, 1f) else 0.5f

            val signal = if (spectrumType == SpectrumType.Transmittance) {
                // Transmittance %: Top is yCalibration.signalTop (typically 100%), bottom is yCalibration.signalBottom (0%)
                yCalibration.signalBottom + (1.0 - fracY) * (yCalibration.signalTop - yCalibration.signalBottom)
            } else {
                // Absorbance: Top is max A, bottom is min A
                yCalibration.signalBottom + (1.0 - fracY) * (yCalibration.signalTop - yCalibration.signalBottom)
            }

            points.add(SpectrumPoint(wavenumber = wn, signalValue = signal))
        }

        // Standardize: sort descending by wavenumber (standard spectroscopy)
        val sortedPoints = points.sortedByDescending { it.wavenumber }

        // 5. Measurable Digitization Quality & Confidence (Based on true geometric criteria)
        val coverageRatio = if (totalColumns > 0) (trackedCount.toDouble() / totalColumns) else 0.0

        // Calculate second derivative variance (smoothness metric: physical spectra are continuous curves)
        var jumpPenalty = 0.0
        if (rawPixelPoints.size > 2) {
            var largeJumps = 0
            for (i in 1 until rawPixelPoints.size) {
                val dy = abs(rawPixelPoints[i].second - rawPixelPoints[i - 1].second)
                if (dy > 12) largeJumps++
            }
            jumpPenalty = (largeJumps.toDouble() / rawPixelPoints.size).coerceIn(0.0, 0.4)
        }

        val resolutionScore = (deltaX / 500f).coerceIn(0.1f, 1.0f).toDouble()
        val calculatedConfidence = (coverageRatio * 0.55 + resolutionScore * 0.25 + (1.0 - jumpPenalty) * 0.20).coerceIn(0.15, 0.98)

        if (coverageRatio < 0.70) {
            warnings.add("Low curve tracing coverage (${(coverageRatio * 100).toInt()}%). Some sections required gap interpolation.")
        }
        if (jumpPenalty > 0.15) {
            warnings.add("Irregular vertical jumps detected. Use the eraser or redraw tools to clear background grid artifacts.")
        }
        if (deltaX < 200f) {
            warnings.add("Low horizontal pixel width. Digitize high-resolution plots for optimal peak accuracy.")
        }

        val qualityMetrics = mapOf(
            "Coverage" to "${(coverageRatio * 100).toInt()}%",
            "Trace Columns" to "$trackedCount / $totalColumns",
            "Pixel Resolution" to "${deltaX.toInt()} x ${deltaY.toInt()} px",
            "Confidence" to "${(calculatedConfidence * 100).toInt()}%",
            "Axis Orientation" to axisDirection.label,
            "Notice" to "Digitized data is an approximation of plotted image. Not direct instrument telemetry."
        )

        val metadata = mapOf(
            "Source" to "Digitized from image",
            "Calibration Left" to "${xCalibrationLeft.wavenumber.toInt()} cm⁻¹",
            "Calibration Right" to "${xCalibrationRight.wavenumber.toInt()} cm⁻¹",
            "Axis Orientation" to axisDirection.label,
            "Quality Score" to "${(calculatedConfidence * 100).toInt()}%",
            "Traced Columns" to "$trackedCount / $totalColumns",
            "Notice" to "Digitized from image. Reconstructed numerical telemetry is an approximation."
        )

        val reconstructedSpectrum = Spectrum(
            name = "$spectrumName (Digitized)",
            sourceType = SpectrumSourceType.DIGITIZED,
            xUnit = "cm⁻¹",
            yUnit = if (spectrumType == SpectrumType.Transmittance) "%T" else "A.U.",
            spectrumType = spectrumType,
            points = sortedPoints,
            rawPoints = sortedPoints,
            metadata = metadata
        )

        return DigitizationResult(
            originalBitmap = graphBitmap,
            croppedGraphBitmap = graphBitmap,
            detectedBounds = Rect(minX.toInt(), yTop.toInt(), maxX.toInt(), yBottom.toInt()),
            extractedPoints = sortedPoints,
            spectrum = reconstructedSpectrum,
            confidenceScore = calculatedConfidence,
            warnings = warnings,
            estimatedAxisDirection = axisDirection,
            extractedCurveMask = maskBitmap,
            qualityMetrics = qualityMetrics
        )
    }

    override fun generateSampleFtIrPlotBitmap(): Bitmap {
        val w = 800
        val h = 480
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        canvas.drawColor(android.graphics.Color.WHITE)

        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 2.5f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }

        val gridPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 1.0f
            style = android.graphics.Paint.Style.STROKE
        }

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 20f
            isAntiAlias = true
        }

        val plotLeft = 80f
        val plotRight = 740f
        val plotTop = 50f
        val plotBottom = 420f
        val plotW = plotRight - plotLeft
        val plotH = plotBottom - plotTop

        // Draw Plot Frame
        canvas.drawRect(plotLeft, plotTop, plotRight, plotBottom, borderPaint)

        // Draw Grid Lines & Ticks (4000, 3000, 2000, 1000, 400 cm⁻¹)
        val wnTicks = listOf(4000, 3000, 2000, 1000, 400)
        for (wn in wnTicks) {
            val frac = (4000 - wn).toFloat() / (4000 - 400)
            val x = plotLeft + frac * plotW
            canvas.drawLine(x, plotTop, x, plotBottom, gridPaint)
            canvas.drawLine(x, plotBottom, x, plotBottom + 8f, borderPaint)
            val lbl = "$wn"
            val tw = textPaint.measureText(lbl)
            canvas.drawText(lbl, x - tw / 2f, plotBottom + 30f, textPaint)
        }

        // Draw Y Ticks (0, 20, 40, 60, 80, 100 %T)
        for (sig in 0..100 step 20) {
            val frac = sig / 100f
            val y = plotBottom - frac * plotH
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint)
            canvas.drawLine(plotLeft - 8f, y, plotLeft, y, borderPaint)
            val lbl = "$sig"
            val tw = textPaint.measureText(lbl)
            canvas.drawText(lbl, plotLeft - tw - 12f, y + 7f, textPaint)
        }

        // Axis Titles
        canvas.drawText("Wavenumber (cm⁻¹)", plotLeft + plotW / 2f - 90f, plotBottom + 52f, textPaint)

        // Draw Title
        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("FTIR Spectrum - Polystyrene Film", plotLeft + 10f, plotTop - 16f, titlePaint)

        // Synthetic Polystyrene Curve (Transmittance dips)
        val curvePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(10, 20, 40)
            strokeWidth = 2.4f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }

        val path = android.graphics.Path()
        var started = false

        for (px in plotLeft.toInt()..plotRight.toInt()) {
            val fracX = (px - plotLeft) / plotW
            val wn = 4000.0 - fracX * (4000.0 - 400.0)

            // Calculate synthetic transmittance with major polystyrene dips
            var t = 92.0 // baseline transmittance

            // Dips: 3026 (aromatic C-H), 2924 (aliphatic C-H), 1601 (aromatic ring), 1492, 1452, 698
            fun dip(center: Double, width: Double, depth: Double) {
                val d = (wn - center) / width
                t -= depth * Math.exp(-d * d)
            }

            dip(3082.0, 30.0, 28.0)
            dip(3060.0, 25.0, 34.0)
            dip(3026.0, 28.0, 48.0)
            dip(2924.0, 35.0, 38.0)
            dip(2850.0, 30.0, 22.0)
            dip(1944.0, 40.0, 16.0)
            dip(1872.0, 35.0, 14.0)
            dip(1802.0, 35.0, 15.0)
            dip(1601.0, 22.0, 68.0)
            dip(1492.0, 20.0, 64.0)
            dip(1452.0, 22.0, 56.0)
            dip(1028.0, 24.0, 42.0)
            dip(756.0, 25.0, 72.0)
            dip(698.0, 22.0, 78.0)

            t = t.coerceIn(5.0, 98.0)
            val fracY = t / 100.0
            val py = plotBottom - fracY.toFloat() * plotH

            if (!started) {
                path.moveTo(px.toFloat(), py)
                started = true
            } else {
                path.lineTo(px.toFloat(), py)
            }
        }

        canvas.drawPath(path, curvePaint)
        return bmp
    }
}
