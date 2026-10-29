package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.LabelOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.Peak
import com.example.domain.model.Spectrum
import com.example.domain.model.SpectrumPoint
import com.example.domain.model.SpectrumType
import com.example.ui.theme.AmberLaser
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldMatch
import com.example.ui.theme.SpectrumCanvasBg
import com.example.ui.theme.SpectrumCursorColor
import com.example.ui.theme.SpectrumGridColor
import com.example.ui.theme.SpectrumGridMajorColor
import com.example.ui.theme.SpectrumPeakMarkerColor
import com.example.ui.theme.SpectrumRefCurveColor
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun SpectrumViewer(
    spectrum: Spectrum,
    modifier: Modifier = Modifier,
    referenceSpectrum: Spectrum? = null,
    viewerState: SpectrumViewerState = rememberSpectrumViewerState(),
    onAddManualPeak: ((Double) -> Unit)? = null
) {
    if (viewerState.isFullscreen) {
        Dialog(
            onDismissRequest = { viewerState.isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = SpectrumCanvasBg
            ) {
                SpectrumViewerContent(
                    spectrum = spectrum,
                    referenceSpectrum = referenceSpectrum,
                    viewerState = viewerState,
                    modifier = Modifier.fillMaxSize(),
                    showFullscreenClose = true,
                    onAddManualPeak = onAddManualPeak
                )
            }
        }
    } else {
        SpectrumViewerContent(
            spectrum = spectrum,
            referenceSpectrum = referenceSpectrum,
            viewerState = viewerState,
            modifier = modifier,
            onAddManualPeak = onAddManualPeak
        )
    }
}

@Composable
private fun SpectrumViewerContent(
    spectrum: Spectrum,
    referenceSpectrum: Spectrum?,
    viewerState: SpectrumViewerState,
    modifier: Modifier = Modifier,
    showFullscreenClose: Boolean = false,
    onAddManualPeak: ((Double) -> Unit)? = null
) {
    val points = spectrum.points
    val dataMinWn = spectrum.minWavenumber
    val dataMaxWn = spectrum.maxWavenumber
    val minWn = if (dataMinWn >= 350.0 && dataMaxWn <= 4200.0) 400.0 else (kotlin.math.floor(dataMinWn / 100.0) * 100.0).coerceAtLeast(0.0)
    val maxWn = if (dataMinWn >= 350.0 && dataMaxWn <= 4200.0) 4000.0 else (kotlin.math.ceil(dataMaxWn / 100.0) * 100.0)

    val dataMinSig = spectrum.minSignal
    val dataMaxSig = spectrum.maxSignal

    val minSig = if (spectrum.spectrumType == SpectrumType.Absorbance) {
        minOf(0.0, dataMinSig)
    } else {
        minOf(0.0, dataMinSig - 2.0)
    }

    val maxSig = if (spectrum.spectrumType == SpectrumType.Absorbance) {
        (dataMaxSig * 1.15).coerceAtLeast(1.0)
    } else {
        maxOf(105.0, dataMaxSig + 2.0)
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .testTag("spectrum_viewer_card"),
        colors = CardDefaults.cardColors(containerColor = SpectrumCanvasBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Toolbar: Coordinate readout and control buttons
            ViewerHeader(
                spectrum = spectrum,
                referenceSpectrum = referenceSpectrum,
                viewerState = viewerState,
                showFullscreenClose = showFullscreenClose,
                onAddManualPeak = onAddManualPeak
            )

            // Main Interactive Canvas
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SpectrumCanvasBg)
            ) {
                val canvasWidth = constraints.maxWidth.toFloat()
                val canvasHeight = constraints.maxHeight.toFloat()

                val padLeft = 60f
                val padRight = 24f
                val padTop = 32f
                val padBottom = 48f

                val plotWidth = (canvasWidth - padLeft - padRight).coerceAtLeast(10f)
                val plotHeight = (canvasHeight - padTop - padBottom).coerceAtLeast(10f)

                // Coordinate mapping functions
                fun wnToX(wn: Double): Float {
                    val norm = if (viewerState.isDescendingAxis) {
                        (maxWn - wn) / (maxWn - minWn)
                    } else {
                        (wn - minWn) / (maxWn - minWn)
                    }
                    val scaledX = (norm.toFloat() * plotWidth * viewerState.zoomLevel) + viewerState.panOffsetX
                    return padLeft + scaledX
                }

                fun sigToY(sig: Double): Float {
                    val norm = (sig - minSig) / (maxSig - minSig)
                    val rawY = (1.0f - norm.toFloat()) * plotHeight
                    return padTop + (rawY * viewerState.zoomLevel) + viewerState.panOffsetY
                }

                fun xToWn(x: Float): Double {
                    val relativeX = (x - padLeft - viewerState.panOffsetX) / (plotWidth * viewerState.zoomLevel)
                    val clampedNorm = relativeX.coerceIn(0f, 1f).toDouble()
                    return if (viewerState.isDescendingAxis) {
                        maxWn - clampedNorm * (maxWn - minWn)
                    } else {
                        minWn + clampedNorm * (maxWn - minWn)
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points, viewerState.isDescendingAxis) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                if (zoom != 1f) {
                                    viewerState.zoomLevel = (viewerState.zoomLevel * zoom).coerceIn(1f, 15f)
                                }
                                if (viewerState.zoomLevel > 1f) {
                                    viewerState.panOffsetX += pan.x
                                    val maxPan = plotWidth * (viewerState.zoomLevel - 1f)
                                    viewerState.panOffsetX = viewerState.panOffsetX.coerceIn(-maxPan, 0f)
                                } else {
                                    viewerState.panOffsetX = 0f
                                    if (points.isNotEmpty()) {
                                        val dragWn = xToWn(centroid.x)
                                        val nearest = points.minByOrNull { abs(it.wavenumber - dragWn) }
                                        viewerState.activeCursorPoint = nearest
                                    }
                                }
                            }
                        }
                        .pointerInput(points, viewerState.isDescendingAxis) {
                            detectTapGestures { offset ->
                                if (points.isNotEmpty()) {
                                    val tappedWn = xToWn(offset.x)
                                    val nearest = points.minByOrNull { abs(it.wavenumber - tappedWn) }
                                    viewerState.activeCursorPoint = nearest

                                    val wnTolerance = (maxWn - minWn) * 0.04
                                    val tappedPeak = spectrum.detectedPeaks.minByOrNull { abs(it.wavenumber - tappedWn) }
                                    if (tappedPeak != null && abs(tappedPeak.wavenumber - tappedWn) <= wnTolerance) {
                                        viewerState.selectedPeak = tappedPeak
                                    }
                                }
                            }
                        }
                ) {
                    val nativePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(160, 160, 180, 205)
                        textSize = 28f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.MONOSPACE
                    }

                    val titlePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(220, 0, 229, 255)
                        textSize = 30f
                        isAntiAlias = true
                        isFakeBoldText = true
                    }

                    // 1. Draw Grid Lines
                    if (viewerState.showGrid) {
                        val wnSpan = (maxWn - minWn).coerceAtLeast(10.0)
                        val tickStep = when {
                            wnSpan > 8000.0 -> 2000.0
                            wnSpan > 4000.0 -> 1000.0
                            wnSpan > 1500.0 -> 500.0
                            wnSpan > 500.0 -> 200.0
                            else -> 50.0
                        }

                        var tick = (kotlin.math.ceil(minWn / tickStep) * tickStep)
                        while (tick <= maxWn) {
                            val x = wnToX(tick)
                            if (x in padLeft..(padLeft + plotWidth)) {
                                drawLine(
                                    color = if (tick % (tickStep * 2) == 0.0) SpectrumGridMajorColor else SpectrumGridColor,
                                    start = Offset(x, padTop),
                                    end = Offset(x, padTop + plotHeight),
                                    strokeWidth = if (tick % (tickStep * 2) == 0.0) 1.5f else 1.0f
                                )
                                // Draw X tick label
                                val label = "${tick.toInt()}"
                                val labelWidth = nativePaint.measureText(label)
                                drawContext.canvas.nativeCanvas.drawText(
                                    label,
                                    x - labelWidth / 2f,
                                    padTop + plotHeight + 32f,
                                    nativePaint
                                )
                            }
                            tick += tickStep
                        }

                        // Signal horizontal grid ticks
                        val ySpan = (maxSig - minSig).coerceAtLeast(0.1)
                        val ySteps = if (spectrum.spectrumType == SpectrumType.Absorbance) {
                            val step = if (ySpan > 5.0) 1.0 else if (ySpan > 2.0) 0.5 else 0.2
                            generateSequence(minSig) { it + step }.takeWhile { it <= maxSig }.toList()
                        } else {
                            listOf(0.0, 20.0, 40.0, 60.0, 80.0, 100.0).filter { it in minSig..maxSig }
                        }

                        for (yVal in ySteps) {
                            val y = sigToY(yVal)
                            if (y in padTop..(padTop + plotHeight)) {
                                drawLine(
                                    color = SpectrumGridColor,
                                    start = Offset(padLeft, y),
                                    end = Offset(padLeft + plotWidth, y),
                                    strokeWidth = 1.0f
                                )
                                val label = if (spectrum.spectrumType == SpectrumType.Absorbance) {
                                    "%.1f".format(Locale.US, yVal)
                                } else {
                                    "${yVal.toInt()}%"
                                }
                                drawContext.canvas.nativeCanvas.drawText(
                                    label,
                                    8f,
                                    y + 10f,
                                    nativePaint
                                )
                            }
                        }
                    }

                    // 2. Draw Plot Bounding Box
                    drawRect(
                        color = SpectrumGridMajorColor,
                        topLeft = Offset(padLeft, padTop),
                        size = Size(plotWidth, plotHeight),
                        style = Stroke(width = 1.5f)
                    )

                    // 3. Draw Axis Labels
                    val xLabel = "Wavenumber (cm⁻¹)"
                    val xLabelWidth = nativePaint.measureText(xLabel)
                    drawContext.canvas.nativeCanvas.drawText(
                        xLabel,
                        padLeft + (plotWidth - xLabelWidth) / 2f,
                        padTop + plotHeight + 44f,
                        titlePaint
                    )

                    // Draw Y axis label near top left
                    val yLabel = if (spectrum.spectrumType == SpectrumType.Absorbance) "Absorbance (A)" else "Transmittance (%T)"
                    drawContext.canvas.nativeCanvas.drawText(
                        yLabel,
                        padLeft + 12f,
                        padTop + 24f,
                        titlePaint
                    )

                    // Normalization and difference baseline coordinates
                    val uMin = if (points.isNotEmpty()) points.minOf { it.signalValue } else 0.0
                    val uMax = if (points.isNotEmpty()) points.maxOf { it.signalValue } else 100.0
                    val uSpan = (uMax - uMin).coerceAtLeast(1e-4)

                    val refPointsList = referenceSpectrum?.points ?: emptyList()
                    val rMin = if (refPointsList.isNotEmpty()) refPointsList.minOf { it.signalValue } else 0.0
                    val rMax = if (refPointsList.isNotEmpty()) refPointsList.maxOf { it.signalValue } else 100.0
                    val rSpan = (rMax - rMin).coerceAtLeast(1e-4)

                    fun getUnknownY(rawSig: Double): Float {
                        return if (viewerState.normalizeComparison && referenceSpectrum != null) {
                            val norm01 = (rawSig - uMin) / uSpan
                            val standardSig = minSig + norm01 * (maxSig - minSig)
                            sigToY(standardSig)
                        } else {
                            sigToY(rawSig)
                        }
                    }

                    fun getReferenceY(rawSig: Double): Float {
                        return if (viewerState.normalizeComparison) {
                            val norm01 = (rawSig - rMin) / rSpan
                            val standardSig = minSig + norm01 * (maxSig - minSig)
                            sigToY(standardSig)
                        } else {
                            sigToY(rawSig)
                        }
                    }

                    val diffZeroSig = if (spectrum.spectrumType == SpectrumType.Absorbance) 0.0 else (minSig + maxSig) / 2.0
                    fun getDiffY(diffVal: Double): Float {
                        return sigToY(diffZeroSig + diffVal)
                    }

                    // 4. Draw Reference Spectrum (if overlay enabled and visible)
                    if (viewerState.showReferenceSpectrum) {
                        referenceSpectrum?.let { ref ->
                            val refPoints = ref.points
                            if (refPoints.size > 1) {
                                val refPath = Path()
                                var first = true
                                for (pt in refPoints) {
                                    val x = wnToX(pt.wavenumber)
                                    val y = getReferenceY(pt.signalValue).coerceIn(padTop, padTop + plotHeight)
                                    if (first) {
                                        refPath.moveTo(x, y)
                                        first = false
                                    } else {
                                        refPath.lineTo(x, y)
                                    }
                                }
                                drawPath(
                                    path = refPath,
                                    color = SpectrumRefCurveColor.copy(alpha = 0.85f),
                                    style = Stroke(width = 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }
                    }

                    // 4b. Draw Difference Spectrum (if enabled & available)
                    val diffPts = viewerState.differenceSpectrumPoints
                    if (viewerState.showDifferenceSpectrum && diffPts != null && diffPts.size > 1) {
                        val zeroY = sigToY(diffZeroSig).coerceIn(padTop, padTop + plotHeight)
                        drawLine(
                            color = Color(0xFFE040FB).copy(alpha = 0.5f),
                            start = Offset(padLeft, zeroY),
                            end = Offset(padLeft + plotWidth, zeroY),
                            strokeWidth = 1.0f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "Diff Baseline (Δ = 0)",
                            padLeft + 8f,
                            zeroY - 4f,
                            nativePaint
                        )
                        val diffPath = Path()
                        var first = true
                        for (pt in diffPts) {
                            val x = wnToX(pt.wavenumber)
                            val y = getDiffY(pt.signalValue).coerceIn(padTop, padTop + plotHeight)
                            if (first) {
                                diffPath.moveTo(x, y)
                                first = false
                            } else {
                                diffPath.lineTo(x, y)
                            }
                        }
                        drawPath(
                            path = diffPath,
                            color = Color(0xFFE040FB),
                            style = Stroke(width = 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    // 5. Draw Primary Spectrum Curve
                    if (viewerState.showUnknownSpectrum && points.size > 1) {
                        val curvePath = Path()
                        var first = true
                        for (pt in points) {
                            val x = wnToX(pt.wavenumber)
                            val y = getUnknownY(pt.signalValue).coerceIn(padTop, padTop + plotHeight)
                            if (first) {
                                curvePath.moveTo(x, y)
                                first = false
                            } else {
                                curvePath.lineTo(x, y)
                            }
                        }
                        drawPath(
                            path = curvePath,
                            color = CyanLaser,
                            style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    // 5b. Draw Estimated Baseline Curve (if enabled & available)
                    val basePts = viewerState.baselinePoints
                    if (viewerState.showBaseline && basePts != null && basePts.size > 1) {
                        val basePath = Path()
                        var first = true
                        for (pt in basePts) {
                            val x = wnToX(pt.wavenumber)
                            val y = sigToY(pt.signalValue).coerceIn(padTop, padTop + plotHeight)
                            if (first) {
                                basePath.moveTo(x, y)
                                first = false
                            } else {
                                basePath.lineTo(x, y)
                            }
                        }
                        drawPath(
                            path = basePath,
                            color = AmberLaser,
                            style = Stroke(
                                width = 2.0f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            )
                        )
                    }

                    // 6. Draw Peak Markers
                    if (viewerState.showPeakLabels && spectrum.detectedPeaks.isNotEmpty()) {
                        val peakTagPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(230, 6, 214, 160)
                            textSize = 22f
                            isAntiAlias = true
                            isFakeBoldText = true
                        }

                        for (peak in spectrum.detectedPeaks) {
                            val px = wnToX(peak.wavenumber)
                            val py = sigToY(peak.signalValue).coerceIn(padTop, padTop + plotHeight)

                            if (px in padLeft..(padLeft + plotWidth)) {
                                val isSelected = viewerState.selectedPeak?.id == peak.id

                                // Draw peak point marker
                                drawCircle(
                                    color = if (isSelected) AmberLaser else EmeraldMatch,
                                    radius = if (isSelected) 6.0f else 4.5f,
                                    center = Offset(px, py)
                                )

                                // Vertical marker flag
                                val flagY = if (spectrum.spectrumType == SpectrumType.Absorbance) {
                                    py - 12f
                                } else {
                                    py + 18f
                                }

                                drawLine(
                                    color = if (isSelected) AmberLaser else EmeraldMatch.copy(alpha = 0.5f),
                                    start = Offset(px, py),
                                    end = Offset(px, flagY),
                                    strokeWidth = if (isSelected) 2.0f else 1.2f
                                )

                                val peakTxt = "${peak.wavenumber.toInt()}"
                                val txtW = peakTagPaint.measureText(peakTxt)
                                drawContext.canvas.nativeCanvas.drawText(
                                    peakTxt,
                                    px - txtW / 2f,
                                    if (spectrum.spectrumType == SpectrumType.Absorbance) flagY - 4f else flagY + 16f,
                                    peakTagPaint
                                )
                            }
                        }
                    }

                    // 6b. Highlight Selected Peak & Draw FWHM Span
                    viewerState.selectedPeak?.let { sel ->
                        val sx = wnToX(sel.wavenumber)
                        val sy = sigToY(sel.signalValue).coerceIn(padTop, padTop + plotHeight)

                        if (sx in padLeft..(padLeft + plotWidth)) {
                            // Pulsing outer halo
                            drawCircle(
                                color = AmberLaser.copy(alpha = 0.35f),
                                radius = 13f,
                                center = Offset(sx, sy)
                            )
                            drawCircle(
                                color = AmberLaser,
                                radius = 6.5f,
                                center = Offset(sx, sy)
                            )

                            // If FWHM is calculated, draw the FWHM horizontal span bar
                            if (sel.fwhm > 0.0) {
                                val halfWnLeft = sel.wavenumber - sel.fwhm / 2.0
                                val halfWnRight = sel.wavenumber + sel.fwhm / 2.0
                                val hxLeft = wnToX(halfWnLeft).coerceIn(padLeft, padLeft + plotWidth)
                                val hxRight = wnToX(halfWnRight).coerceIn(padLeft, padLeft + plotWidth)

                                val halfY = if (spectrum.spectrumType == SpectrumType.Absorbance) {
                                    sigToY(sel.signalValue - sel.prominence / 2.0).coerceIn(padTop, padTop + plotHeight)
                                } else {
                                    sigToY(sel.signalValue + sel.prominence / 2.0).coerceIn(padTop, padTop + plotHeight)
                                }

                                drawLine(
                                    color = AmberLaser,
                                    start = Offset(hxLeft, halfY),
                                    end = Offset(hxRight, halfY),
                                    strokeWidth = 2.2f
                                )
                                drawCircle(color = AmberLaser, radius = 3.5f, center = Offset(hxLeft, halfY))
                                drawCircle(color = AmberLaser, radius = 3.5f, center = Offset(hxRight, halfY))
                            }
                        }
                    }

                    // 7. Draw Active Cursor & Crosshair
                    viewerState.activeCursorPoint?.let { cursor ->
                        val cx = wnToX(cursor.wavenumber)
                        val cy = sigToY(cursor.signalValue).coerceIn(padTop, padTop + plotHeight)

                        if (cx in padLeft..(padLeft + plotWidth)) {
                            // Vertical crosshair
                            drawLine(
                                color = SpectrumCursorColor.copy(alpha = 0.75f),
                                start = Offset(cx, padTop),
                                end = Offset(cx, padTop + plotHeight),
                                strokeWidth = 1.2f
                            )
                            // Horizontal crosshair
                            drawLine(
                                color = SpectrumCursorColor.copy(alpha = 0.45f),
                                start = Offset(padLeft, cy),
                                end = Offset(padLeft + plotWidth, cy),
                                strokeWidth = 1.0f
                            )
                            // Highlight circle
                            drawCircle(
                                color = SpectrumCursorColor,
                                radius = 6f,
                                center = Offset(cx, cy),
                                style = Stroke(width = 2.5f)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.5f,
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }

                if (points.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Waves, contentDescription = null, tint = CyanLaser, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Spectral Data Loaded", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text("Import a CSV/TSV file or select a reference compound to view", color = Color(0xFFA0B4CD), fontSize = 11.sp)
                    }
                }
            }

            // Bottom Axis Orientation & Controls Bar
            ViewerFooter(spectrum = spectrum, minWn = minWn, maxWn = maxWn, viewerState = viewerState)
        }
    }
}

@Composable
private fun ViewerHeader(
    spectrum: Spectrum,
    referenceSpectrum: Spectrum? = null,
    viewerState: SpectrumViewerState,
    showFullscreenClose: Boolean,
    onAddManualPeak: ((Double) -> Unit)? = null
) {
    Surface(
        color = Color(0xFF0F1B2B),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Coordinate Pill Readout & Curve Toggles
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val cursor = viewerState.activeCursorPoint
                val sel = viewerState.selectedPeak

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF172942))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (sel != null) {
                        Text(
                            text = "Peak #${sel.index}: ${"%.1f".format(Locale.US, sel.wavenumber)} cm⁻¹ • Prom: ${"%.1f".format(Locale.US, sel.prominence)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberLaser
                        )
                    } else if (cursor != null) {
                        val matchingPeak = spectrum.detectedPeaks.find { abs(it.wavenumber - cursor.wavenumber) < 15.0 }
                        val fgTag = matchingPeak?.matchedFunctionalGroup?.let { " • $it" } ?: ""
                        val refNear = referenceSpectrum?.points?.minByOrNull { abs(it.wavenumber - cursor.wavenumber) }
                        val refText = if (viewerState.showReferenceSpectrum && refNear != null && abs(refNear.wavenumber - cursor.wavenumber) < 15.0) {
                            " | Ref: ${"%.1f".format(Locale.US, refNear.signalValue)}"
                        } else ""

                        Text(
                            text = "${"%.1f".format(Locale.US, cursor.wavenumber)} cm⁻¹ | Unk: ${"%.1f".format(Locale.US, cursor.signalValue)}$refText$fgTag",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanLaser
                        )
                    } else {
                        Text(
                            text = "Tap curve to inspect",
                            fontSize = 11.sp,
                            color = Color(0xFF90A4AE)
                        )
                    }
                }

                // Comparison Legends & Toggles (when reference or difference curve is present)
                if (referenceSpectrum != null) {
                    Surface(
                        color = if (viewerState.showUnknownSpectrum) CyanLaser.copy(alpha = 0.25f) else Color(0xFF172942),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewerState.showUnknownSpectrum = !viewerState.showUnknownSpectrum }
                    ) {
                        Text(
                            text = "Unknown",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewerState.showUnknownSpectrum) CyanLaser else Color(0xFF607D8B),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        color = if (viewerState.showReferenceSpectrum) Color(0xFFFFB74D).copy(alpha = 0.25f) else Color(0xFF172942),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewerState.showReferenceSpectrum = !viewerState.showReferenceSpectrum }
                    ) {
                        Text(
                            text = "Reference",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewerState.showReferenceSpectrum) Color(0xFFFFB74D) else Color(0xFF607D8B),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                        )
                    }

                    if (viewerState.differenceSpectrumPoints != null) {
                        Surface(
                            color = if (viewerState.showDifferenceSpectrum) Color(0xFFE040FB).copy(alpha = 0.25f) else Color(0xFF172942),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { viewerState.showDifferenceSpectrum = !viewerState.showDifferenceSpectrum }
                        ) {
                            Text(
                                text = "Diff (Δ)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (viewerState.showDifferenceSpectrum) Color(0xFFE040FB) else Color(0xFF607D8B),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Surface(
                        color = if (viewerState.normalizeComparison) EmeraldMatch.copy(alpha = 0.25f) else Color(0xFF172942),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewerState.toggleNormalizeComparison() }
                    ) {
                        Text(
                            text = if (viewerState.normalizeComparison) "Norm: ON" else "Norm: OFF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewerState.normalizeComparison) EmeraldMatch else Color(0xFF607D8B),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Control Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Zoom In
                IconButton(
                    onClick = { viewerState.zoomIn() },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFFB0BEC5))
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
                }

                // Zoom Out
                IconButton(
                    onClick = { viewerState.zoomOut() },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFFB0BEC5))
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
                }

                // Reset Zoom
                IconButton(
                    onClick = { viewerState.resetZoom() },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFFB0BEC5))
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset Zoom", modifier = Modifier.size(20.dp))
                }

                // Fullscreen toggle
                IconButton(
                    onClick = { viewerState.toggleFullscreen() },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = CyanLaser)
                ) {
                    if (showFullscreenClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close Fullscreen", modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerFooter(
    spectrum: Spectrum,
    minWn: Double,
    maxWn: Double,
    viewerState: SpectrumViewerState
) {
    Surface(
        color = Color(0xFF0F1B2B),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Axis Orientation Indicator / Toggle Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E2F46))
                    .clickable { viewerState.toggleAxisDirection() }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "Reverse Axis",
                    tint = CyanLaser,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val minLabel = "${minWn.toInt()}"
                val maxLabel = "${maxWn.toInt()}"
                Text(
                    text = if (viewerState.isDescendingAxis) "$maxLabel → $minLabel cm⁻¹" else "$minLabel → $maxLabel cm⁻¹",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFCFD8DC)
                )
            }

            // Grid and Peak Label Toggles
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewerState.toggleGrid() },
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (viewerState.showGrid) CyanLaser else Color(0xFF607D8B)
                    )
                ) {
                    Icon(Icons.Default.GridOn, contentDescription = "Toggle Grid", modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = { viewerState.togglePeakLabels() },
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (viewerState.showPeakLabels) EmeraldMatch else Color(0xFF607D8B)
                    )
                ) {
                    Icon(
                        if (viewerState.showPeakLabels) Icons.AutoMirrored.Filled.Label else Icons.AutoMirrored.Filled.LabelOff,
                        contentDescription = "Toggle Peak Labels",
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (viewerState.zoomLevel > 1.05f) {
                    Text(
                        text = "Zoom: ${"%.1f".format(Locale.US, viewerState.zoomLevel)}x",
                        fontSize = 11.sp,
                        color = AmberLaser,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}
