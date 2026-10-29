package com.example.ui.screens.digitize

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.domain.model.Spectrum
import com.example.domain.model.SpectrumPoint
import com.example.domain.model.SpectrumType
import com.example.domain.spectroscopy.CalibrationPoint
import com.example.domain.spectroscopy.DigitizationResult
import com.example.domain.spectroscopy.PdfPageInfo
import com.example.domain.spectroscopy.PreprocessingOptions
import com.example.domain.spectroscopy.StandardPdfSpectrumExtractor
import com.example.domain.spectroscopy.StandardSpectrumImageProcessor
import com.example.domain.spectroscopy.WavenumberAxisDirection
import com.example.domain.spectroscopy.YCalibration
import com.example.ui.components.SpectrumViewer
import com.example.ui.theme.AmberLaser
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldMatch
import com.example.ui.theme.SpectrumCanvasBg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class DigitizationStep {
    SELECT_SOURCE,
    PDF_PAGE_SELECTION,
    CROP_GRAPH_AREA,
    CALIBRATION_AND_TRACE,
    VALIDATION_AND_SEND
}

enum class EditToolMode {
    CALIBRATE,
    PREPROCESS,
    ERASER,
    PEN_REDRAW
}

@Composable
fun SpectrumDigitizerScreen(
    onSpectrumDigitized: (Spectrum) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val processor = remember { StandardSpectrumImageProcessor() }
    val pdfExtractor = remember { StandardPdfSpectrumExtractor() }

    var currentStep by remember { mutableStateOf(DigitizationStep.SELECT_SOURCE) }
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var croppedGraphBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // PDF Pages state
    var pdfPages by remember { mutableStateOf<List<PdfPageInfo>>(emptyList()) }
    var currentPdfUri by remember { mutableStateOf<Uri?>(null) }

    // Crop box fractions (0.0 to 1.0 of loadedBitmap)
    var cropLeftFrac by remember { mutableFloatStateOf(0.08f) }
    var cropTopFrac by remember { mutableFloatStateOf(0.08f) }
    var cropRightFrac by remember { mutableFloatStateOf(0.94f) }
    var cropBottomFrac by remember { mutableFloatStateOf(0.88f) }

    // Calibration settings & interactive markers (relative 0.0 to 1.0 inside cropped bitmap)
    var markerX1Frac by remember { mutableFloatStateOf(0.04f) }
    var markerX2Frac by remember { mutableFloatStateOf(0.96f) }
    var markerYTopFrac by remember { mutableFloatStateOf(0.04f) }
    var markerYBottomFrac by remember { mutableFloatStateOf(0.96f) }

    var axisDirection by remember { mutableStateOf(WavenumberAxisDirection.HIGH_TO_LOW) }
    var leftWnInput by remember { mutableStateOf("4000") }
    var rightWnInput by remember { mutableStateOf("400") }
    var topSignalInput by remember { mutableStateOf("100") }
    var bottomSignalInput by remember { mutableStateOf("0") }
    var spectrumType by remember { mutableStateOf(SpectrumType.Transmittance) }

    // Preprocessing options
    var enhanceContrast by remember { mutableStateOf(true) }
    var suppressGrid by remember { mutableStateOf(true) }
    var denoise by remember { mutableStateOf(true) }
    var curveThreshold by remember { mutableIntStateOf(140) }
    var showPreprocessedPreview by remember { mutableStateOf(false) }

    // Manual edits state
    var currentToolMode by remember { mutableStateOf(EditToolMode.CALIBRATE) }
    var eraseMaskBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val manualDrawnPoints = remember { mutableStateMapOf<Int, Int>() } // x -> y

    // Result
    var digitizationResult by remember { mutableStateOf<DigitizationResult?>(null) }

    // Camera setup
    var tempCameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraImageUri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openInputStream(tempCameraImageUri!!)
                    val bmp = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    if (bmp != null) {
                        loadedBitmap = bmp
                        val bounds = processor.detectGraphRegion(bmp)
                        cropLeftFrac = (bounds.rect.left.toFloat() / bmp.width).coerceIn(0f, 0.45f)
                        cropTopFrac = (bounds.rect.top.toFloat() / bmp.height).coerceIn(0f, 0.45f)
                        cropRightFrac = (bounds.rect.right.toFloat() / bmp.width).coerceIn(0.55f, 1f)
                        cropBottomFrac = (bounds.rect.bottom.toFloat() / bmp.height).coerceIn(0.55f, 1f)
                        currentStep = DigitizationStep.CROP_GRAPH_AREA
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load camera image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Gallery / File launcher
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { fileUri ->
            coroutineScope.launch(Dispatchers.IO) {
                isProcessing = true
                try {
                    val mime = context.contentResolver.getType(fileUri) ?: ""
                    if (mime.contains("pdf", ignoreCase = true) || fileUri.toString().endsWith(".pdf", ignoreCase = true)) {
                        currentPdfUri = fileUri
                        val pagesRes = pdfExtractor.inspectPdfPages(context, fileUri)
                        pagesRes.onSuccess { pages ->
                            pdfPages = pages
                            currentStep = DigitizationStep.PDF_PAGE_SELECTION
                        }.onFailure { err ->
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Could not inspect PDF: ${err.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        val stream = context.contentResolver.openInputStream(fileUri)
                        val bmp = BitmapFactory.decodeStream(stream)
                        stream?.close()
                        if (bmp != null) {
                            loadedBitmap = bmp
                            val bounds = processor.detectGraphRegion(bmp)
                            cropLeftFrac = (bounds.rect.left.toFloat() / bmp.width).coerceIn(0f, 0.45f)
                            cropTopFrac = (bounds.rect.top.toFloat() / bmp.height).coerceIn(0f, 0.45f)
                            cropRightFrac = (bounds.rect.right.toFloat() / bmp.width).coerceIn(0.55f, 1f)
                            cropBottomFrac = (bounds.rect.bottom.toFloat() / bmp.height).coerceIn(0.55f, 1f)
                            currentStep = DigitizationStep.CROP_GRAPH_AREA
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to open document: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Compact Top Navigation Bar
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (currentStep != DigitizationStep.SELECT_SOURCE) {
                            currentStep = when (currentStep) {
                                DigitizationStep.PDF_PAGE_SELECTION -> DigitizationStep.SELECT_SOURCE
                                DigitizationStep.CROP_GRAPH_AREA -> DigitizationStep.SELECT_SOURCE
                                DigitizationStep.CALIBRATION_AND_TRACE -> DigitizationStep.CROP_GRAPH_AREA
                                DigitizationStep.VALIDATION_AND_SEND -> DigitizationStep.CALIBRATION_AND_TRACE
                                else -> DigitizationStep.SELECT_SOURCE
                            }
                        } else {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Spectrum Digitizer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = when (currentStep) {
                            DigitizationStep.SELECT_SOURCE -> "1/4 • Source Selection"
                            DigitizationStep.PDF_PAGE_SELECTION -> "2/4 • PDF Page Selection"
                            DigitizationStep.CROP_GRAPH_AREA -> "2/4 • Align Plot Crop Frame"
                            DigitizationStep.CALIBRATION_AND_TRACE -> "3/4 • Calibrate & Trace Curve"
                            DigitizationStep.VALIDATION_AND_SEND -> "4/4 • Scientific Validation"
                        },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }

        // Compact Step Indicator Breadcrumb
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val stepIndex = when (currentStep) {
                DigitizationStep.SELECT_SOURCE -> 0
                DigitizationStep.PDF_PAGE_SELECTION, DigitizationStep.CROP_GRAPH_AREA -> 1
                DigitizationStep.CALIBRATION_AND_TRACE -> 2
                DigitizationStep.VALIDATION_AND_SEND -> 3
            }

            listOf("Source", "Crop", "Calibrate", "Validate").forEachIndexed { idx, label ->
                val isActive = idx == stepIndex
                val isDone = idx < stepIndex
                val color = if (isActive) CyanLaser else if (isDone) EmeraldMatch else MaterialTheme.colorScheme.surfaceVariant
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }

        // Main Workflow Screen Body
        when (currentStep) {
            DigitizationStep.SELECT_SOURCE -> {
                SelectSourceContent(
                    onTakePhoto = {
                        try {
                            val tempFile = File.createTempFile("camera_spectrum_", ".jpg", context.cacheDir)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                tempFile
                            )
                            tempCameraImageUri = uri
                            cameraLauncher.launch(uri)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Camera init error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onPickFile = {
                        filePicker.launch(arrayOf("image/*", "application/pdf"))
                    },
                    onLoadSamplePlot = {
                        coroutineScope.launch(Dispatchers.Default) {
                            isProcessing = true
                            val sampleBmp = processor.generateSampleFtIrPlotBitmap()
                            loadedBitmap = sampleBmp
                            val bounds = processor.detectGraphRegion(sampleBmp)
                            cropLeftFrac = (bounds.rect.left.toFloat() / sampleBmp.width).coerceIn(0f, 0.45f)
                            cropTopFrac = (bounds.rect.top.toFloat() / sampleBmp.height).coerceIn(0f, 0.45f)
                            cropRightFrac = (bounds.rect.right.toFloat() / sampleBmp.width).coerceIn(0.55f, 1f)
                            cropBottomFrac = (bounds.rect.bottom.toFloat() / sampleBmp.height).coerceIn(0.55f, 1f)
                            withContext(Dispatchers.Main) {
                                currentStep = DigitizationStep.CROP_GRAPH_AREA
                                isProcessing = false
                            }
                        }
                    }
                )
            }

            DigitizationStep.PDF_PAGE_SELECTION -> {
                PdfPageSelectionContent(
                    pages = pdfPages,
                    onSelectPage = { pageIdx ->
                        currentPdfUri?.let { uri ->
                            coroutineScope.launch(Dispatchers.IO) {
                                isProcessing = true
                                val highRes = pdfExtractor.renderPageHighRes(context, uri, pageIdx)
                                highRes.onSuccess { bmp ->
                                    loadedBitmap = bmp
                                    val bounds = processor.detectGraphRegion(bmp)
                                    cropLeftFrac = (bounds.rect.left.toFloat() / bmp.width).coerceIn(0f, 0.45f)
                                    cropTopFrac = (bounds.rect.top.toFloat() / bmp.height).coerceIn(0f, 0.45f)
                                    cropRightFrac = (bounds.rect.right.toFloat() / bmp.width).coerceIn(0.55f, 1f)
                                    cropBottomFrac = (bounds.rect.bottom.toFloat() / bmp.height).coerceIn(0.55f, 1f)
                                    currentStep = DigitizationStep.CROP_GRAPH_AREA
                                }.onFailure { err ->
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Render error: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                isProcessing = false
                            }
                        }
                    }
                )
            }

            DigitizationStep.CROP_GRAPH_AREA -> {
                loadedBitmap?.let { bmp ->
                    CropGraphAreaContent(
                        bitmap = bmp,
                        cropLeft = cropLeftFrac,
                        cropTop = cropTopFrac,
                        cropRight = cropRightFrac,
                        cropBottom = cropBottomFrac,
                        onCropChanged = { l, t, r, b ->
                            cropLeftFrac = l
                            cropTopFrac = t
                            cropRightFrac = r
                            cropBottomFrac = b
                        },
                        onAutoDetect = {
                            val bounds = processor.detectGraphRegion(bmp)
                            cropLeftFrac = (bounds.rect.left.toFloat() / bmp.width).coerceIn(0f, 0.45f)
                            cropTopFrac = (bounds.rect.top.toFloat() / bmp.height).coerceIn(0f, 0.45f)
                            cropRightFrac = (bounds.rect.right.toFloat() / bmp.width).coerceIn(0.55f, 1f)
                            cropBottomFrac = (bounds.rect.bottom.toFloat() / bmp.height).coerceIn(0.55f, 1f)
                        },
                        onConfirmCrop = {
                            val pxLeft = (cropLeftFrac * bmp.width).toInt().coerceIn(0, bmp.width - 2)
                            val pxTop = (cropTopFrac * bmp.height).toInt().coerceIn(0, bmp.height - 2)
                            val pxWidth = ((cropRightFrac - cropLeftFrac) * bmp.width).toInt().coerceAtLeast(60)
                            val pxHeight = ((cropBottomFrac - cropTopFrac) * bmp.height).toInt().coerceAtLeast(60)

                            val safeW = pxWidth.coerceAtMost(bmp.width - pxLeft)
                            val safeH = pxHeight.coerceAtMost(bmp.height - pxTop)

                            val cropped = Bitmap.createBitmap(bmp, pxLeft, pxTop, safeW, safeH)
                            croppedGraphBitmap = cropped

                            // Reset edit buffers for fresh cropped area
                            eraseMaskBitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
                            manualDrawnPoints.clear()
                            markerX1Frac = 0.04f
                            markerX2Frac = 0.96f
                            markerYTopFrac = 0.04f
                            markerYBottomFrac = 0.96f

                            currentStep = DigitizationStep.CALIBRATION_AND_TRACE
                        }
                    )
                }
            }

            DigitizationStep.CALIBRATION_AND_TRACE -> {
                croppedGraphBitmap?.let { graphBmp ->
                    CalibrationAndTraceContent(
                        graphBitmap = graphBmp,
                        processor = processor,
                        currentToolMode = currentToolMode,
                        onToolModeChanged = { currentToolMode = it },
                        markerX1 = markerX1Frac,
                        markerX2 = markerX2Frac,
                        markerYTop = markerYTopFrac,
                        markerYBottom = markerYBottomFrac,
                        onMarkersChanged = { x1, x2, yt, yb ->
                            markerX1Frac = x1
                            markerX2Frac = x2
                            markerYTopFrac = yt
                            markerYBottomFrac = yb
                        },
                        axisDirection = axisDirection,
                        onAxisDirectionChanged = { axisDirection = it },
                        leftWnInput = leftWnInput,
                        onLeftWnChanged = { leftWnInput = it },
                        rightWnInput = rightWnInput,
                        onRightWnChanged = { rightWnInput = it },
                        topSignalInput = topSignalInput,
                        onTopSignalChanged = { topSignalInput = it },
                        bottomSignalInput = bottomSignalInput,
                        onBottomSignalChanged = { bottomSignalInput = it },
                        spectrumType = spectrumType,
                        onSpectrumTypeChanged = { spectrumType = it },
                        enhanceContrast = enhanceContrast,
                        onEnhanceContrastChanged = { enhanceContrast = it },
                        suppressGrid = suppressGrid,
                        onSuppressGridChanged = { suppressGrid = it },
                        denoise = denoise,
                        onDenoiseChanged = { denoise = it },
                        curveThreshold = curveThreshold,
                        onCurveThresholdChanged = { curveThreshold = it },
                        showPreprocessedPreview = showPreprocessedPreview,
                        onShowPreprocessedPreviewChanged = { showPreprocessedPreview = it },
                        eraseMaskBitmap = eraseMaskBitmap,
                        manualDrawnPoints = manualDrawnPoints,
                        onClearManualEdits = {
                            eraseMaskBitmap?.eraseColor(android.graphics.Color.TRANSPARENT)
                            manualDrawnPoints.clear()
                        },
                        onRunDigitization = {
                            coroutineScope.launch(Dispatchers.Default) {
                                isProcessing = true
                                val leftWn = leftWnInput.toDoubleOrNull() ?: 4000.0
                                val rightWn = rightWnInput.toDoubleOrNull() ?: 400.0
                                val topSig = topSignalInput.toDoubleOrNull() ?: 100.0
                                val botSig = bottomSignalInput.toDoubleOrNull() ?: 0.0

                                val pxX1 = (markerX1Frac * graphBmp.width)
                                val pxX2 = (markerX2Frac * graphBmp.width)
                                val pxYTop = (markerYTopFrac * graphBmp.height)
                                val pxYBot = (markerYBottomFrac * graphBmp.height)

                                val xLeft = CalibrationPoint(pixelX = pxX1, wavenumber = leftWn)
                                val xRight = CalibrationPoint(pixelX = pxX2, wavenumber = rightWn)
                                val yCal = YCalibration(
                                    pixelYTop = pxYTop,
                                    signalTop = topSig,
                                    pixelYBottom = pxYBot,
                                    signalBottom = botSig
                                )

                                val prepOptions = PreprocessingOptions(
                                    enhanceContrast = enhanceContrast,
                                    denoise = denoise,
                                    suppressGrid = suppressGrid,
                                    threshold = curveThreshold
                                )

                                val res = processor.digitizeGraph(
                                    graphBitmap = graphBmp,
                                    xCalibrationLeft = xLeft,
                                    xCalibrationRight = xRight,
                                    yCalibration = yCal,
                                    spectrumType = spectrumType,
                                    options = prepOptions,
                                    eraseMask = eraseMaskBitmap,
                                    manualDrawnPoints = manualDrawnPoints.toMap(),
                                    spectrumName = "Reconstructed Spectrum"
                                )

                                withContext(Dispatchers.Main) {
                                    digitizationResult = res
                                    currentStep = DigitizationStep.VALIDATION_AND_SEND
                                    isProcessing = false
                                }
                            }
                        }
                    )
                }
            }

            DigitizationStep.VALIDATION_AND_SEND -> {
                digitizationResult?.let { res ->
                    ValidationAndSendContent(
                        result = res,
                        onSendToAnalyzer = {
                            onSpectrumDigitized(res.spectrum)
                        },
                        onAdjustCalibration = {
                            currentStep = DigitizationStep.CALIBRATION_AND_TRACE
                        },
                        onCropAgain = {
                            currentStep = DigitizationStep.CROP_GRAPH_AREA
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectSourceContent(
    onTakePhoto: () -> Unit,
    onPickFile: () -> Unit,
    onLoadSamplePlot: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = CyanLaser, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scientific Spectrum Digitization",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "Deterministic curve reconstruction directly from printed graphs, journal figures, lab PDFs, and camera photos. No generic AI hallucination.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        item {
            Text("Select Input Source:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.weight(1f).height(94.dp).testTag("card_source_camera"),
                    onClick = onTakePhoto,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CyanLaser, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Camera", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Capture plot", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f).height(94.dp).testTag("card_source_file"),
                    onClick = onPickFile,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AmberLaser, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Image / PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("PNG, JPG, PDF", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f).height(94.dp).testTag("card_source_sample"),
                    onClick = onLoadSamplePlot,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldMatch, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Test Sample", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("FTIR Polystyrene", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Scientific Workflow Guide", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = AmberLaser)
                    Text(
                        text = "1. Align crop box directly to graph outer frame\n2. Set calibration points A & B (supports standard 4000 → 400 and inverted)\n3. Erase legends or redraw broken lines with touch tools\n4. Inspect measurable confidence and send to analyzer",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfPageSelectionContent(
    pages: List<PdfPageInfo>,
    onSelectPage: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp)
    ) {
        item {
            Text("Select Page Containing Spectrum Plot:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        items(pages) { page ->
            Card(
                modifier = Modifier.fillMaxWidth().testTag("pdf_page_${page.pageIndex}"),
                onClick = { onSelectPage(page.pageIndex) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Image(
                        bitmap = page.previewBitmap.asImageBitmap(),
                        contentDescription = "Page ${page.pageIndex + 1}",
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Page ${page.pageIndex + 1} of ${page.totalPages}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        if (page.hasLikelySpectrum) {
                            Text(
                                text = "Plot Detected",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldMatch
                            )
                        }
                    }

                    Button(
                        onClick = { onSelectPage(page.pageIndex) },
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Text("Crop Page", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CropGraphAreaContent(
    bitmap: Bitmap,
    cropLeft: Float,
    cropTop: Float,
    cropRight: Float,
    cropBottom: Float,
    onCropChanged: (Float, Float, Float, Float) -> Unit,
    onAutoDetect: () -> Unit,
    onConfirmCrop: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Align Crop Frame to Graph Bounds", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedButton(
                    onClick = onAutoDetect,
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto Detect", fontSize = 10.sp)
                }
            }
        }

        // Image with Crop Overlay Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val boxW = maxWidth
                    val boxH = maxHeight

                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Original plot",
                        modifier = Modifier.fillMaxSize()
                    )

                    // Crop border box
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = boxW * cropLeft,
                                top = boxH * cropTop,
                                end = boxW * (1f - cropRight),
                                bottom = boxH * (1f - cropBottom)
                            )
                            .border(2.dp, CyanLaser, RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        // Compact Sliders and Adjustments
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Left: ${(cropLeft * 100).toInt()}%", fontSize = 10.sp)
                        Text("Right: ${(cropRight * 100).toInt()}%", fontSize = 10.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Slider(
                            value = cropLeft,
                            onValueChange = { onCropChanged(it, cropTop, cropRight, cropBottom) },
                            valueRange = 0f..0.45f,
                            modifier = Modifier.weight(1f)
                        )
                        Slider(
                            value = cropRight,
                            onValueChange = { onCropChanged(cropLeft, cropTop, it, cropBottom) },
                            valueRange = 0.55f..1f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Top: ${(cropTop * 100).toInt()}%", fontSize = 10.sp)
                        Text("Bottom: ${(cropBottom * 100).toInt()}%", fontSize = 10.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Slider(
                            value = cropTop,
                            onValueChange = { onCropChanged(cropLeft, it, cropRight, cropBottom) },
                            valueRange = 0f..0.45f,
                            modifier = Modifier.weight(1f)
                        )
                        Slider(
                            value = cropBottom,
                            onValueChange = { onCropChanged(cropLeft, cropTop, cropRight, it) },
                            valueRange = 0.55f..1f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onConfirmCrop,
                modifier = Modifier.fillMaxWidth().height(40.dp).testTag("button_confirm_crop")
            ) {
                Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirm Crop & Calibrate", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CalibrationAndTraceContent(
    graphBitmap: Bitmap,
    processor: StandardSpectrumImageProcessor,
    currentToolMode: EditToolMode,
    onToolModeChanged: (EditToolMode) -> Unit,
    markerX1: Float,
    markerX2: Float,
    markerYTop: Float,
    markerYBottom: Float,
    onMarkersChanged: (Float, Float, Float, Float) -> Unit,
    axisDirection: WavenumberAxisDirection,
    onAxisDirectionChanged: (WavenumberAxisDirection) -> Unit,
    leftWnInput: String,
    onLeftWnChanged: (String) -> Unit,
    rightWnInput: String,
    onRightWnChanged: (String) -> Unit,
    topSignalInput: String,
    onTopSignalChanged: (String) -> Unit,
    bottomSignalInput: String,
    onBottomSignalChanged: (String) -> Unit,
    spectrumType: SpectrumType,
    onSpectrumTypeChanged: (SpectrumType) -> Unit,
    enhanceContrast: Boolean,
    onEnhanceContrastChanged: (Boolean) -> Unit,
    suppressGrid: Boolean,
    onSuppressGridChanged: (Boolean) -> Unit,
    denoise: Boolean,
    onDenoiseChanged: (Boolean) -> Unit,
    curveThreshold: Int,
    onCurveThresholdChanged: (Int) -> Unit,
    showPreprocessedPreview: Boolean,
    onShowPreprocessedPreviewChanged: (Boolean) -> Unit,
    eraseMaskBitmap: Bitmap?,
    manualDrawnPoints: MutableMap<Int, Int>,
    onClearManualEdits: () -> Unit,
    onRunDigitization: () -> Unit
) {
    // Local preprocessed preview cache
    val preprocessedPreview = remember(enhanceContrast, suppressGrid, denoise, curveThreshold, showPreprocessedPreview) {
        if (showPreprocessedPreview) {
            processor.preprocessBitmap(
                graphBitmap,
                PreprocessingOptions(
                    enhanceContrast = enhanceContrast,
                    denoise = denoise,
                    suppressGrid = suppressGrid,
                    threshold = curveThreshold
                )
            )
        } else null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
    ) {
        // Mode Tabs: Calibrate / Preprocess / Eraser / Pen
        item {
            TabRow(
                selectedTabIndex = currentToolMode.ordinal,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = currentToolMode == EditToolMode.CALIBRATE,
                    onClick = { onToolModeChanged(EditToolMode.CALIBRATE) },
                    text = { Text("1. Calibrate", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = currentToolMode == EditToolMode.PREPROCESS,
                    onClick = { onToolModeChanged(EditToolMode.PREPROCESS) },
                    text = { Text("2. Filters", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = currentToolMode == EditToolMode.ERASER,
                    onClick = { onToolModeChanged(EditToolMode.ERASER) },
                    text = { Text("3. Erase", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = currentToolMode == EditToolMode.PEN_REDRAW,
                    onClick = { onToolModeChanged(EditToolMode.PEN_REDRAW) },
                    text = { Text("4. Redraw", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Interactive Canvas for calibration, erasing, redrawing
        item {
            Card(
                modifier = Modifier.fillMaxWidth().height(175.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(currentToolMode) {
                            detectDragGestures { change, _ ->
                                val pos = change.position
                                val fracX = (pos.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val fracY = (pos.y / size.height.toFloat()).coerceIn(0f, 1f)

                                when (currentToolMode) {
                                    EditToolMode.CALIBRATE -> {
                                        // Move nearest calibration marker
                                        val distToX1 = abs(fracX - markerX1)
                                        val distToX2 = abs(fracX - markerX2)
                                        if (distToX1 < distToX2) {
                                            onMarkersChanged(fracX.coerceIn(0f, markerX2 - 0.05f), markerX2, markerYTop, markerYBottom)
                                        } else {
                                            onMarkersChanged(markerX1, fracX.coerceIn(markerX1 + 0.05f, 1f), markerYTop, markerYBottom)
                                        }
                                    }
                                    EditToolMode.ERASER -> {
                                        eraseMaskBitmap?.let { mask ->
                                            val pxX = (fracX * mask.width).toInt().coerceIn(0, mask.width - 1)
                                            val pxY = (fracY * mask.height).toInt().coerceIn(0, mask.height - 1)
                                            // Erase circle radius 8 px
                                            val rad = 8
                                            for (dx in -rad..rad) {
                                                for (dy in -rad..rad) {
                                                    val ex = (pxX + dx).coerceIn(0, mask.width - 1)
                                                    val ey = (pxY + dy).coerceIn(0, mask.height - 1)
                                                    mask.setPixel(ex, ey, android.graphics.Color.WHITE)
                                                }
                                            }
                                        }
                                    }
                                    EditToolMode.PEN_REDRAW -> {
                                        val pxX = (fracX * graphBitmap.width).toInt().coerceIn(0, graphBitmap.width - 1)
                                        val pxY = (fracY * graphBitmap.height).toInt().coerceIn(0, graphBitmap.height - 1)
                                        manualDrawnPoints[pxX] = pxY
                                    }
                                    else -> Unit
                                }
                            }
                        }
                ) {
                    val boxW = maxWidth
                    val boxH = maxHeight

                    val displayBmp = if (showPreprocessedPreview && preprocessedPreview != null) {
                        preprocessedPreview
                    } else {
                        graphBitmap
                    }

                    Image(
                        bitmap = displayBmp.asImageBitmap(),
                        contentDescription = "Cropped plot",
                        modifier = Modifier.fillMaxSize()
                    )

                    // Draw Erase Mask overlay (semi-transparent red overlay on erased pixels)
                    eraseMaskBitmap?.let { mask ->
                        Image(
                            bitmap = mask.asImageBitmap(),
                            contentDescription = "Eraser mask",
                            modifier = Modifier.fillMaxSize(),
                            alpha = 0.5f
                        )
                    }

                    // Calibration Markers A & B Lines
                    if (currentToolMode == EditToolMode.CALIBRATE) {
                        // Marker A (X1)
                        Box(
                            modifier = Modifier
                                .offset(x = boxW * markerX1)
                                .width(2.dp)
                                .height(boxH)
                                .background(CyanLaser)
                        )
                        Surface(
                            color = CyanLaser,
                            shape = CircleShape,
                            modifier = Modifier
                                .offset(x = (boxW * markerX1) - 10.dp, y = 4.dp)
                                .size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("A", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        // Marker B (X2)
                        Box(
                            modifier = Modifier
                                .offset(x = boxW * markerX2)
                                .width(2.dp)
                                .height(boxH)
                                .background(AmberLaser)
                        )
                        Surface(
                            color = AmberLaser,
                            shape = CircleShape,
                            modifier = Modifier
                                .offset(x = (boxW * markerX2) - 10.dp, y = 4.dp)
                                .size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("B", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        // Y Top
                        Box(
                            modifier = Modifier
                                .offset(y = boxH * markerYTop)
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .background(EmeraldMatch)
                        )
                        // Y Bottom
                        Box(
                            modifier = Modifier
                                .offset(y = boxH * markerYBottom)
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .background(EmeraldMatch)
                        )
                    }

                    // Mode Hint Badge
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                    ) {
                        Text(
                            text = when (currentToolMode) {
                                EditToolMode.CALIBRATE -> "Drag Pins A & B to align with ticks"
                                EditToolMode.PREPROCESS -> "Inspect preprocessed curve preview"
                                EditToolMode.ERASER -> "Touch & drag to erase unwanted text"
                                EditToolMode.PEN_REDRAW -> "Touch & drag to draw missing curve"
                            },
                            fontSize = 9.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Contextual Controls depending on Tool Mode
        when (currentToolMode) {
            EditToolMode.CALIBRATE -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = axisDirection == WavenumberAxisDirection.HIGH_TO_LOW,
                                    onClick = {
                                        onAxisDirectionChanged(WavenumberAxisDirection.HIGH_TO_LOW)
                                        onLeftWnChanged("4000")
                                        onRightWnChanged("400")
                                    },
                                    label = { Text("4000 → 400 (Standard)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = axisDirection == WavenumberAxisDirection.LOW_TO_HIGH,
                                    onClick = {
                                        onAxisDirectionChanged(WavenumberAxisDirection.LOW_TO_HIGH)
                                        onLeftWnChanged("400")
                                        onRightWnChanged("4000")
                                    },
                                    label = { Text("400 → 4000 (Inverted)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = leftWnInput,
                                    onValueChange = onLeftWnChanged,
                                    label = { Text("Point A (cm⁻¹)", fontSize = 10.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).testTag("input_cal_left_wn")
                                )
                                OutlinedTextField(
                                    value = rightWnInput,
                                    onValueChange = onRightWnChanged,
                                    label = { Text("Point B (cm⁻¹)", fontSize = 10.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).testTag("input_cal_right_wn")
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = spectrumType == SpectrumType.Transmittance,
                                    onClick = {
                                        onSpectrumTypeChanged(SpectrumType.Transmittance)
                                        onTopSignalChanged("100")
                                        onBottomSignalChanged("0")
                                    },
                                    label = { Text("Transmittance (%T)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = spectrumType == SpectrumType.Absorbance,
                                    onClick = {
                                        onSpectrumTypeChanged(SpectrumType.Absorbance)
                                        onTopSignalChanged("1.0")
                                        onBottomSignalChanged("0.0")
                                    },
                                    label = { Text("Absorbance (A)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            EditToolMode.PREPROCESS -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Live Preprocessing Preview", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = showPreprocessedPreview,
                                    onCheckedChange = onShowPreprocessedPreviewChanged,
                                    modifier = Modifier.height(24.dp)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = enhanceContrast,
                                    onClick = { onEnhanceContrastChanged(!enhanceContrast) },
                                    label = { Text("Contrast Boost", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = suppressGrid,
                                    onClick = { onSuppressGridChanged(!suppressGrid) },
                                    label = { Text("Suppress Grid", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = denoise,
                                    onClick = { onDenoiseChanged(!denoise) },
                                    label = { Text("Denoise", fontSize = 10.sp) }
                                )
                            }

                            Text("Curve Darkness Threshold: $curveThreshold", fontSize = 10.sp)
                            Slider(
                                value = curveThreshold.toFloat(),
                                onValueChange = { onCurveThresholdChanged(it.toInt()) },
                                valueRange = 80f..220f
                            )
                        }
                    }
                }
            }

            EditToolMode.ERASER, EditToolMode.PEN_REDRAW -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (currentToolMode == EditToolMode.ERASER) "Erase annotations & labels" else "Pinned columns: ${manualDrawnPoints.size}",
                                fontSize = 11.sp
                            )
                            OutlinedButton(
                                onClick = onClearManualEdits,
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Edits", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Run Reconstruction Action Button
        item {
            Button(
                onClick = onRunDigitization,
                modifier = Modifier.fillMaxWidth().height(42.dp).testTag("button_run_digitize")
            ) {
                Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Trace Curve & Reconstruct Spectrum", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ValidationAndSendContent(
    result: DigitizationResult,
    onSendToAnalyzer: () -> Unit,
    onAdjustCalibration: () -> Unit,
    onCropAgain: () -> Unit
) {
    var comparisonViewMode by remember { mutableStateOf(0) } // 0: Side-by-Side, 1: Mask Overlay, 2: Reconstructed Chart

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
    ) {
        // Validation Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("card_digitization_summary"),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.confidenceScore >= 0.70) EmeraldMatch.copy(alpha = 0.12f) else AmberLaser.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Reconstruction Validation", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Surface(
                            color = if (result.confidenceScore >= 0.70) EmeraldMatch else AmberLaser,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Quality: ${(result.confidenceScore * 100).toInt()}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Reconstructed ${result.extractedPoints.size} numerical data points spanning " +
                                "${result.extractedPoints.lastOrNull()?.wavenumber?.toInt() ?: 400} to ${result.extractedPoints.firstOrNull()?.wavenumber?.toInt() ?: 4000} cm⁻¹.",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    // Quality metric pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        result.qualityMetrics.forEach { (k, v) ->
                            if (k != "Notice") {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text(
                                        text = "$k: $v",
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Warnings if any
        if (result.warnings.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AmberLaser.copy(alpha = 0.10f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AmberLaser, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Trace Warnings:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AmberLaser)
                        }
                        result.warnings.forEach { w ->
                            Text("• $w", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Comparison View Switcher
        item {
            TabRow(
                selectedTabIndex = comparisonViewMode,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = comparisonViewMode == 0,
                    onClick = { comparisonViewMode = 0 },
                    text = { Text("Side-by-Side", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = comparisonViewMode == 1,
                    onClick = { comparisonViewMode = 1 },
                    text = { Text("Traced Mask", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = comparisonViewMode == 2,
                    onClick = { comparisonViewMode = 2 },
                    text = { Text("Spectrum Chart", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Visual Verification Content based on tab
        item {
            when (comparisonViewMode) {
                0 -> {
                    // Side-by-Side: Left = Cropped Image, Right = Reconstructed Spectrum Chart
                    Row(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f).height(160.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Image(
                                    bitmap = result.croppedGraphBitmap.asImageBitmap(),
                                    contentDescription = "Cropped original",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f).height(160.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            SpectrumViewer(
                                spectrum = result.spectrum,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                1 -> {
                    // Overlay Mask
                    Card(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Image(
                                bitmap = result.croppedGraphBitmap.asImageBitmap(),
                                contentDescription = "Cropped graph",
                                modifier = Modifier.fillMaxSize()
                            )
                            result.extractedCurveMask?.let { mask ->
                                Image(
                                    bitmap = mask.asImageBitmap(),
                                    contentDescription = "Traced curve mask",
                                    modifier = Modifier.fillMaxSize(),
                                    alpha = 0.85f
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // Full Reconstructed Spectrum Chart
                    Card(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        SpectrumViewer(
                            spectrum = result.spectrum,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Scientific notice banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = CyanLaser, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Scientifically labeled: 'Digitized from image'. Approximation of plotted artwork.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Actions
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onCropAgain,
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Crop Again", fontSize = 10.sp)
                }

                OutlinedButton(
                    onClick = onAdjustCalibration,
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Re-calibrate", fontSize = 10.sp)
                }

                Button(
                    onClick = onSendToAnalyzer,
                    modifier = Modifier.weight(1.3f).height(38.dp).testTag("button_send_to_analyzer")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Send to Analyzer", fontSize = 11.sp)
                }
            }
        }
    }
}
