package com.example.ui.screens.analyze

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.demo.DemoSpectraProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.SpectrumRepositoryImpl
import com.example.domain.model.Peak
import com.example.domain.model.Spectrum
import com.example.domain.model.SpectrumPoint
import com.example.domain.model.SpectrumSourceType
import com.example.domain.model.SpectrumType
import com.example.domain.repository.SpectrumRepository
import com.example.domain.spectroscopy.AIInterpretationResult
import com.example.domain.spectroscopy.AIService
import com.example.domain.spectroscopy.ColumnMapping
import com.example.domain.spectroscopy.FunctionalGroupMatcher
import com.example.domain.spectroscopy.ImportAnalysisResult
import com.example.domain.spectroscopy.ParsedTable
import com.example.domain.spectroscopy.PeakDetector
import com.example.domain.spectroscopy.ReportGenerator
import com.example.domain.spectroscopy.SpectrumImporter
import com.example.domain.spectroscopy.SpectrumProcessor
import com.example.domain.spectroscopy.StandardAIService
import com.example.domain.spectroscopy.StandardFunctionalGroupMatcher
import com.example.domain.spectroscopy.StandardPeakDetector
import com.example.domain.spectroscopy.StandardReportGenerator
import com.example.domain.spectroscopy.StandardSpectrumImporter
import com.example.domain.spectroscopy.StandardSpectrumProcessor
import com.example.domain.model.ReferenceSpectrum
import com.example.domain.repository.ReferenceSpectrumRepository
import com.example.domain.repository.ReferenceSpectrumRepositoryImpl
import com.example.domain.spectroscopy.NumericalComparisonResult
import com.example.domain.spectroscopy.NumericalSpectrumMatcher
import com.example.domain.spectroscopy.SimilarityAlgorithm
import com.example.domain.spectroscopy.StandardNumericalSpectrumMatcher
import com.example.domain.spectroscopy.StructuredAiComparisonEvidence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale

data class ImportErrorNotice(
    val title: String,
    val message: String,
    val recoveryHint: String,
    val canOpenMapping: Boolean = false
)

enum class BaselineMethod(val label: String) {
    ALS("ALS"),
    POLYNOMIAL("Polynomial"),
    RUBBERBAND("Rubberband"),
    LINEAR("Linear")
}

enum class AiWorkflowState {
    IDLE,
    PREPARING_LOCAL_DATA,
    SENDING_AI_REQUEST,
    WAITING_FOR_AI_RESPONSE,
    AI_RESPONSE_RECEIVED,
    INTERPRETING_RESPONSE,
    COMPLETE,
    AI_REQUEST_FAILED
}

data class AnalyzeUiState(
    val currentSpectrum: Spectrum,
    val referenceOverlay: Spectrum? = null,
    val isProcessing: Boolean = false,
    val peakProminenceThreshold: Double = 8.0,
    val peakMinDistance: Double = 12.0,
    val peakMinHeight: Double = 0.0,
    val peakSearchWindow: Int = 3,
    val activeTab: AnalyzeTab = AnalyzeTab.PEAKS,
    val statusMessage: String? = null,
    val importError: ImportErrorNotice? = null,
    val aiResult: AIInterpretationResult? = null,
    val isAiLoading: Boolean = false,
    val aiWorkflowState: AiWorkflowState = AiWorkflowState.IDLE,
    val aiConnectionTestResult: com.example.domain.spectroscopy.AIConnectionTestResult? = null,
    val isTestingConnection: Boolean = false,
    val aiDiagnostics: com.example.domain.spectroscopy.AIDiagnosticsInfo = com.example.domain.spectroscopy.AIDiagnosticsInfo(),
    val isSavedInHistory: Boolean = false,
    val pendingImportAnalysis: ImportAnalysisResult? = null,
    val lastParsedTable: ParsedTable? = null,
    val estimatedBaselinePoints: List<SpectrumPoint>? = null,
    val showEstimatedBaseline: Boolean = true,
    val previewCorrectedSpectrum: Spectrum? = null,
    val isComparingBaselineBeforeAfter: Boolean = false,
    val selectedPeak: Peak? = null,
    val derivativeSpectrum: Spectrum? = null,
    val isViewingDerivative: Boolean = false,
    val selectedReference: ReferenceSpectrum? = null,
    val numericalComparisonResult: NumericalComparisonResult? = null,
    val activeSimilarityAlgorithm: SimilarityAlgorithm = SimilarityAlgorithm.HQI,
    val isComparingNormalized: Boolean = false,
    val showDifferenceSpectrum: Boolean = false,
    val referenceLibrary: List<ReferenceSpectrum> = emptyList(),
    val referenceSearchQuery: String = "",
    val selectedMaterialClassFilter: String? = null,
    val selectedSpectralRegionFilter: String? = null,
    val peakToleranceCm: Double = 15.0,
    val userSampleName: String = "",
    val userSampleType: String = "Organic Solid / Film",
    val userExpectedComposition: String = "",
    val userKnownComponents: String = "",
    val userMeasurementMethod: String = "ATR-FTIR (Diamond Crystal)",
    val userExperimentalNotes: String = "",
    val activeAiLayerIndex: Int = 2,
    val hasAcknowledgedPrivacy: Boolean = false
)

enum class AnalyzeTab(val label: String) {
    PEAKS("Peaks"),
    PROCESSING("Processing"),
    AI_ANALYSIS("AI Analysis"),
    COMPARE("Compare"),
    INFO("Info")
}

class AnalyzeViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: SpectrumRepository = SpectrumRepositoryImpl(
        AppDatabase.getDatabase(application).spectrumDao()
    ),
    private val processor: SpectrumProcessor = StandardSpectrumProcessor(),
    private val peakDetector: PeakDetector = StandardPeakDetector(),
    private val fgMatcher: FunctionalGroupMatcher = StandardFunctionalGroupMatcher(),
    private val importer: SpectrumImporter = StandardSpectrumImporter(),
    private val aiService: AIService = StandardAIService(),
    private val reportGen: ReportGenerator = StandardReportGenerator(),
    private val referenceRepo: ReferenceSpectrumRepository = ReferenceSpectrumRepositoryImpl(
        AppDatabase.getDatabase(application).customReferenceSpectrumDao()
    ),
    private val numericalMatcher: NumericalSpectrumMatcher = StandardNumericalSpectrumMatcher()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        AnalyzeUiState(
            currentSpectrum = DemoSpectraProvider.createPolystyreneDemo()
        )
    )
    val uiState: StateFlow<AnalyzeUiState> = _uiState.asStateFlow()

    init {
        detectPeaks()
        // Continuously observe reference library from local Room DB + standards
        viewModelScope.launch {
            referenceRepo.getAllReferencesFlow().collect { refs ->
                _uiState.update { it.copy(referenceLibrary = refs) }
                // Re-evaluate active comparison if a reference is already selected
                val currSelected = _uiState.value.selectedReference
                if (currSelected != null) {
                    val updatedRef = refs.find { it.id == currSelected.id } ?: currSelected
                    recalculateComparison(updatedRef)
                }
            }
        }
        // Restore last analyzed spectrum from history if available so app restart preserves state
        viewModelScope.launch {
            repository.getHistorySpectra().collect { spectra ->
                if (spectra.isNotEmpty() && _uiState.value.currentSpectrum.isDemo) {
                    val mostRecent = spectra.first()
                    loadSpectrum(mostRecent)
                }
            }
        }
    }

    fun selectTab(tab: AnalyzeTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun loadSpectrum(spectrum: Spectrum) {
        val savedName = spectrum.metadata["user_sample_name"] ?: spectrum.name
        val savedType = spectrum.metadata["user_sample_type"] ?: "Organic Solid / Film"
        val savedExpected = spectrum.metadata["user_expected_composition"] ?: ""
        val savedKnown = spectrum.metadata["user_known_components"] ?: ""
        val savedMethod = spectrum.metadata["user_measurement_method"] ?: "ATR-FTIR (Diamond Crystal)"
        val savedNotes = spectrum.metadata["user_notes"] ?: ""

        _uiState.update {
            it.copy(
                currentSpectrum = spectrum,
                statusMessage = "Loaded spectrum '${spectrum.name}'",
                aiResult = null,
                isSavedInHistory = true,
                estimatedBaselinePoints = null,
                selectedPeak = null,
                derivativeSpectrum = null,
                isViewingDerivative = false,
                userSampleName = savedName,
                userSampleType = savedType,
                userExpectedComposition = savedExpected,
                userKnownComponents = savedKnown,
                userMeasurementMethod = savedMethod,
                userExperimentalNotes = savedNotes
            )
        }
        detectPeaks()
    }

    fun selectReferenceForComparison(reference: ReferenceSpectrum?) {
        if (reference == null) {
            _uiState.update {
                it.copy(
                    selectedReference = null,
                    referenceOverlay = null,
                    numericalComparisonResult = null,
                    showDifferenceSpectrum = false
                )
            }
            return
        }

        recalculateComparison(reference)
    }

    fun recalculateComparison(reference: ReferenceSpectrum? = _uiState.value.selectedReference) {
        val targetRef = reference ?: return
        val curr = _uiState.value
        val result = numericalMatcher.compareSpectra(
            unknown = curr.currentSpectrum,
            reference = targetRef,
            primaryAlgorithm = curr.activeSimilarityAlgorithm,
            peakToleranceCm = curr.peakToleranceCm,
            normalizeForComparison = curr.isComparingNormalized
        )

        _uiState.update {
            it.copy(
                selectedReference = targetRef,
                referenceOverlay = targetRef.spectrum,
                numericalComparisonResult = result
            )
        }
    }

    fun loadReferenceOverlay(refSpectrum: Spectrum?) {
        if (refSpectrum == null) {
            selectReferenceForComparison(null)
            return
        }
        val foundRef = _uiState.value.referenceLibrary.find { it.spectrum.id == refSpectrum.id || it.id == refSpectrum.id }
        if (foundRef != null) {
            selectReferenceForComparison(foundRef)
        } else {
            _uiState.update { it.copy(referenceOverlay = refSpectrum) }
        }
    }

    fun setSimilarityAlgorithm(algorithm: SimilarityAlgorithm) {
        _uiState.update { it.copy(activeSimilarityAlgorithm = algorithm) }
        val ref = _uiState.value.selectedReference
        if (ref != null) recalculateComparison(ref)
    }

    fun toggleComparisonNormalization(normalize: Boolean) {
        _uiState.update { it.copy(isComparingNormalized = normalize) }
        val ref = _uiState.value.selectedReference
        if (ref != null) recalculateComparison(ref)
    }

    fun toggleDifferenceSpectrum(show: Boolean) {
        _uiState.update { it.copy(showDifferenceSpectrum = show) }
    }

    fun setPeakToleranceCm(tol: Double) {
        _uiState.update { it.copy(peakToleranceCm = tol) }
        val ref = _uiState.value.selectedReference
        if (ref != null) recalculateComparison(ref)
    }

    fun setReferenceSearchQuery(query: String) {
        _uiState.update { it.copy(referenceSearchQuery = query) }
    }

    fun setReferenceMaterialClassFilter(filter: String?) {
        _uiState.update { it.copy(selectedMaterialClassFilter = filter) }
    }

    fun setReferenceSpectralRegionFilter(filter: String?) {
        _uiState.update { it.copy(selectedSpectralRegionFilter = filter) }
    }

    fun deleteCustomReference(id: String) {
        viewModelScope.launch {
            referenceRepo.deleteCustomReference(id)
            if (_uiState.value.selectedReference?.id == id) {
                selectReferenceForComparison(null)
            }
            _uiState.update { it.copy(statusMessage = "Custom reference spectrum removed.") }
        }
    }

    fun importCustomReference(
        stream: InputStream,
        fileName: String,
        compoundName: String,
        casNumber: String?,
        chemicalFormula: String?,
        materialClass: String,
        method: String,
        notes: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val parseRes = importer.parseStream(stream, fileName)
                parseRes.onSuccess { importedSpectrum ->
                    val detectedPeaks = peakDetector.detectPeaks(importedSpectrum)
                    val matchedPeaks = fgMatcher.matchPeaks(detectedPeaks)
                    val finalSpectrum = importedSpectrum.copy(
                        name = "$compoundName (Reference)",
                        detectedPeaks = matchedPeaks
                    )

                    val ref = ReferenceSpectrum(
                        compoundName = compoundName,
                        casNumber = casNumber,
                        chemicalFormula = chemicalFormula,
                        materialClass = materialClass,
                        measurementMethod = method,
                        source = "User Imported Reference ($fileName)",
                        sampleInformation = "Imported file: $fileName",
                        notes = notes,
                        characteristicPeaks = matchedPeaks.map { it.wavenumber },
                        spectrum = finalSpectrum,
                        isDemo = false,
                        isUserCustom = true
                    )

                    referenceRepo.addCustomReference(ref)
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            statusMessage = "Added '$compoundName' to Reference Library"
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            statusMessage = "Failed to import reference: ${err.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = "Import error: ${e.message}"
                    )
                }
            }
        }
    }

    fun getStructuredAiEvidence(): StructuredAiComparisonEvidence? {
        val res = _uiState.value.numericalComparisonResult ?: return null
        return numericalMatcher.extractStructuredAiEvidence(res)
    }

    fun setPeakProminence(threshold: Double) {
        _uiState.update { it.copy(peakProminenceThreshold = threshold) }
        detectPeaks()
    }

    fun setPeakMinDistance(dist: Double) {
        _uiState.update { it.copy(peakMinDistance = dist) }
        detectPeaks()
    }

    fun setPeakMinHeight(height: Double) {
        _uiState.update { it.copy(peakMinHeight = height) }
        detectPeaks()
    }

    fun setPeakSearchWindow(win: Int) {
        _uiState.update { it.copy(peakSearchWindow = win) }
        detectPeaks()
    }

    fun selectPeak(peak: Peak?) {
        _uiState.update { it.copy(selectedPeak = peak) }
    }

    fun detectPeaks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val current = _uiState.value.currentSpectrum
            val threshold = _uiState.value.peakProminenceThreshold
            val minDist = _uiState.value.peakMinDistance
            val minHeight = _uiState.value.peakMinHeight
            val win = _uiState.value.peakSearchWindow

            val rawPeaks = withContext(Dispatchers.Default) {
                peakDetector.detectPeaks(
                    spectrum = current,
                    minProminence = threshold,
                    minDistance = minDist,
                    minHeight = minHeight,
                    searchWindow = win
                )
            }

            // Preserve any manually added peaks
            val manualPeaks = current.detectedPeaks.filter { it.isManual }
            val combined = (rawPeaks + manualPeaks).sortedByDescending { it.wavenumber }
                .mapIndexed { idx, p -> p.copy(index = idx + 1) }

            val matchedPeaks = fgMatcher.matchPeaks(combined)
            val updated = current.copy(detectedPeaks = matchedPeaks)

            _uiState.update {
                it.copy(
                    currentSpectrum = updated,
                    isProcessing = false,
                    selectedPeak = matchedPeaks.firstOrNull(),
                    statusMessage = "Detected ${matchedPeaks.size} bands (prominence ≥ ${"%.1f".format(threshold)})"
                )
            }
        }
    }

    fun addManualPeak(targetWn: Double) {
        viewModelScope.launch {
            val current = _uiState.value.currentSpectrum
            val newPeak = withContext(Dispatchers.Default) {
                peakDetector.createManualPeak(current, targetWn)
            }

            val matchedList = fgMatcher.matchPeaks(listOf(newPeak))
            val matchedPeak = matchedList.firstOrNull() ?: newPeak

            val existing = current.detectedPeaks.filterNot { kotlin.math.abs(it.wavenumber - targetWn) < 3.0 }
            val allPeaks = (existing + matchedPeak).sortedByDescending { it.wavenumber }
                .mapIndexed { idx, p -> p.copy(index = idx + 1) }

            val updated = current.copy(detectedPeaks = allPeaks)
            _uiState.update {
                it.copy(
                    currentSpectrum = updated,
                    selectedPeak = matchedPeak,
                    statusMessage = "Added peak at ${"%.1f".format(Locale.US, matchedPeak.wavenumber)} cm⁻¹"
                )
            }
        }
    }

    fun deletePeak(peakId: String) {
        val current = _uiState.value.currentSpectrum
        val filtered = current.detectedPeaks.filterNot { it.id == peakId }
            .mapIndexed { idx, p -> p.copy(index = idx + 1) }
        val updated = current.copy(detectedPeaks = filtered)
        _uiState.update {
            it.copy(
                currentSpectrum = updated,
                selectedPeak = if (it.selectedPeak?.id == peakId) null else it.selectedPeak,
                statusMessage = "Peak removed."
            )
        }
    }

    fun updatePeakNote(peakId: String, note: String, assignment: String?) {
        val current = _uiState.value.currentSpectrum
        val updatedPeaks = current.detectedPeaks.map { p ->
            if (p.id == peakId) {
                p.copy(
                    notes = note.ifBlank { null },
                    matchedFunctionalGroup = assignment ?: p.matchedFunctionalGroup
                )
            } else p
        }
        val updated = current.copy(detectedPeaks = updatedPeaks)
        _uiState.update {
            val sel = if (it.selectedPeak?.id == peakId) updatedPeaks.find { p -> p.id == peakId } else it.selectedPeak
            it.copy(
                currentSpectrum = updated,
                selectedPeak = sel,
                statusMessage = "Peak assignment updated."
            )
        }
    }

    // Baseline Actions
    fun previewBaselinePolynomial(degree: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val current = _uiState.value.currentSpectrum
            val result = withContext(Dispatchers.Default) {
                processor.baselineCorrectPolynomial(current, degree)
            }
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    estimatedBaselinePoints = result.baselinePoints,
                    previewCorrectedSpectrum = result.correctedSpectrum,
                    showEstimatedBaseline = true,
                    statusMessage = "Previewing degree-$degree polynomial baseline (amber dashed)."
                )
            }
        }
    }

    fun applyBaselinePolynomial(degree: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val current = _uiState.value.currentSpectrum
            val result = withContext(Dispatchers.Default) {
                processor.baselineCorrectPolynomial(current, degree)
            }
            _uiState.update {
                it.copy(
                    currentSpectrum = result.correctedSpectrum,
                    estimatedBaselinePoints = null,
                    previewCorrectedSpectrum = null,
                    referenceOverlay = null,
                    isComparingBaselineBeforeAfter = false,
                    isProcessing = false,
                    statusMessage = "Applied polynomial baseline (degree $degree)."
                )
            }
            detectPeaks()
        }
    }

    fun previewBaselineRubberband(segments: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val current = _uiState.value.currentSpectrum
            val result = withContext(Dispatchers.Default) {
                processor.baselineCorrectRubberband(current, segments)
            }
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    estimatedBaselinePoints = result.baselinePoints,
                    previewCorrectedSpectrum = result.correctedSpectrum,
                    showEstimatedBaseline = true,
                    statusMessage = "Previewing rubberband convex-hull baseline ($segments segments)."
                )
            }
        }
    }

    fun applyBaselineRubberband(segments: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val current = _uiState.value.currentSpectrum
            val result = withContext(Dispatchers.Default) {
                processor.baselineCorrectRubberband(current, segments)
            }
            _uiState.update {
                it.copy(
                    currentSpectrum = result.correctedSpectrum,
                    estimatedBaselinePoints = null,
                    previewCorrectedSpectrum = null,
                    referenceOverlay = null,
                    isComparingBaselineBeforeAfter = false,
                    isProcessing = false,
                    statusMessage = "Applied rubberband baseline ($segments segments)."
                )
            }
            detectPeaks()
        }
    }

    fun previewBaselineALS(lambda: Double, p: Double, maxIterations: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val current = _uiState.value.currentSpectrum
            val result = withContext(Dispatchers.Default) {
                processor.baselineCorrectALS(current, lambda, p, maxIterations)
            }
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    estimatedBaselinePoints = result.baselinePoints,
                    previewCorrectedSpectrum = result.correctedSpectrum,
                    showEstimatedBaseline = true,
                    statusMessage = "Previewing ALS baseline (λ = ${"%.0e".format(Locale.US, lambda)}, p = $p)."
                )
            }
        }
    }

    fun applyBaselineALS(lambda: Double, p: Double, maxIterations: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val current = _uiState.value.currentSpectrum
            val result = withContext(Dispatchers.Default) {
                processor.baselineCorrectALS(current, lambda, p, maxIterations)
            }
            _uiState.update {
                it.copy(
                    currentSpectrum = result.correctedSpectrum,
                    estimatedBaselinePoints = null,
                    previewCorrectedSpectrum = null,
                    referenceOverlay = null,
                    isComparingBaselineBeforeAfter = false,
                    isProcessing = false,
                    statusMessage = "Applied Asymmetric Least Squares (ALS) baseline."
                )
            }
            detectPeaks()
        }
    }

    fun applyLinearBaseline() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val current = _uiState.value.currentSpectrum
            val result = withContext(Dispatchers.Default) {
                processor.baselineCorrectLinear(current)
            }
            _uiState.update {
                it.copy(
                    currentSpectrum = result.correctedSpectrum,
                    estimatedBaselinePoints = null,
                    previewCorrectedSpectrum = null,
                    referenceOverlay = null,
                    isComparingBaselineBeforeAfter = false,
                    isProcessing = false,
                    statusMessage = "Applied linear baseline slope correction."
                )
            }
            detectPeaks()
        }
    }

    fun toggleBaselineBeforeAfterComparison() {
        val corr = _uiState.value.previewCorrectedSpectrum ?: return
        val currentComparing = _uiState.value.isComparingBaselineBeforeAfter
        if (!currentComparing) {
            _uiState.update {
                it.copy(
                    isComparingBaselineBeforeAfter = true,
                    referenceOverlay = corr.copy(name = "Corrected Preview"),
                    statusMessage = "Comparing Before (Cyan) vs After Baseline (Amber)."
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isComparingBaselineBeforeAfter = false,
                    referenceOverlay = null,
                    statusMessage = "Baseline curve preview."
                )
            }
        }
    }

    fun clearBaselinePreview() {
        _uiState.update {
            it.copy(
                estimatedBaselinePoints = null,
                previewCorrectedSpectrum = null,
                referenceOverlay = null,
                isComparingBaselineBeforeAfter = false
            )
        }
    }

    // Smoothing Actions
    fun applySavitzkyGolay(windowSize: Int = 9, polyOrder: Int = 2) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val current = _uiState.value.currentSpectrum
            val processed = withContext(Dispatchers.Default) {
                processor.smoothSavitzkyGolay(current, windowSize, polyOrder)
            }
            _uiState.update {
                it.copy(
                    currentSpectrum = processed,
                    isProcessing = false,
                    statusMessage = "Applied Savitzky-Golay filter (win=$windowSize, poly=$polyOrder)."
                )
            }
            detectPeaks()
        }
    }

    fun applyMovingAverage(windowSize: Int = 5) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val current = _uiState.value.currentSpectrum
            val processed = withContext(Dispatchers.Default) {
                processor.smoothMovingAverage(current, windowSize)
            }
            _uiState.update {
                it.copy(
                    currentSpectrum = processed,
                    isProcessing = false,
                    statusMessage = "Applied moving average (window = $windowSize)."
                )
            }
            detectPeaks()
        }
    }

    // Normalization Actions
    fun applyMinMaxNormalization(targetMin: Double = 0.0, targetMax: Double = 100.0) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val current = _uiState.value.currentSpectrum
            val processed = withContext(Dispatchers.Default) {
                processor.normalizeMinMax(current, targetMin, targetMax)
            }
            _uiState.update {
                it.copy(
                    currentSpectrum = processed,
                    isProcessing = false,
                    statusMessage = "Min-Max normalized to [${"%.1f".format(Locale.US, targetMin)}, ${"%.1f".format(Locale.US, targetMax)}]."
                )
            }
            detectPeaks()
        }
    }

    fun applyVectorNormalization() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val current = _uiState.value.currentSpectrum
            val processed = withContext(Dispatchers.Default) {
                processor.normalizeVector(current)
            }
            _uiState.update {
                it.copy(
                    currentSpectrum = processed,
                    isProcessing = false,
                    statusMessage = "Vector (Euclidean L2) normalization applied."
                )
            }
            detectPeaks()
        }
    }

    fun applyPeakNormalization(targetWn: Double? = null, targetIntensity: Double = 100.0) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val current = _uiState.value.currentSpectrum
            val processed = withContext(Dispatchers.Default) {
                processor.normalizePeak(current, targetWn, targetIntensity)
            }
            _uiState.update {
                it.copy(
                    currentSpectrum = processed,
                    isProcessing = false,
                    statusMessage = "Peak normalization applied (scaled to ${"%.1f".format(Locale.US, targetIntensity)})."
                )
            }
            detectPeaks()
        }
    }

    // Derivative Spectrum
    fun calculateDerivative(order: Int = 1, sgWindowSize: Int = 9, sgPolyOrder: Int = 2) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val current = _uiState.value.currentSpectrum
            val derived = withContext(Dispatchers.Default) {
                processor.calculateDerivative(current, order, sgWindowSize, sgPolyOrder)
            }
            val derivPeaks = withContext(Dispatchers.Default) {
                peakDetector.detectPeaks(derived, minProminence = 0.05, minDistance = 10.0)
            }
            val withPeaks = derived.copy(detectedPeaks = derivPeaks)
            _uiState.update {
                it.copy(
                    derivativeSpectrum = withPeaks,
                    isProcessing = false,
                    statusMessage = "Calculated ${order}${if (order == 1) "st" else "nd"} derivative spectrum with ${derivPeaks.size} bands."
                )
            }
        }
    }

    fun toggleViewDerivative() {
        val deriv = _uiState.value.derivativeSpectrum ?: return
        val currentlyViewing = _uiState.value.isViewingDerivative
        if (!currentlyViewing) {
            _uiState.update {
                it.copy(
                    currentSpectrum = deriv,
                    isViewingDerivative = true,
                    statusMessage = "Viewing derivative spectrum."
                )
            }
        } else {
            // Restore from raw points
            resetToOriginal()
        }
    }

    // Reset to Original
    fun resetToOriginal() {
        val current = _uiState.value.currentSpectrum
        val restored = processor.resetToOriginal(current)
        _uiState.update {
            it.copy(
                currentSpectrum = restored,
                estimatedBaselinePoints = null,
                derivativeSpectrum = null,
                isViewingDerivative = false,
                statusMessage = "Restored pristine original spectrum data."
            )
        }
        detectPeaks()
    }

    fun convertTransmittanceToAbsorbance() {
        val current = _uiState.value.currentSpectrum
        if (current.spectrumType != SpectrumType.Transmittance) {
            _uiState.update { it.copy(statusMessage = "Spectrum is already ${current.spectrumType.label}") }
            return
        }
        val converted = processor.convertTransmittanceToAbsorbance(current)
        _uiState.update {
            it.copy(currentSpectrum = converted, statusMessage = "Converted to Absorbance: A = 2 - log₁₀(%T)")
        }
        detectPeaks()
    }

    fun convertAbsorbanceToTransmittance() {
        val current = _uiState.value.currentSpectrum
        if (current.spectrumType != SpectrumType.Absorbance) {
            _uiState.update { it.copy(statusMessage = "Spectrum is already ${current.spectrumType.label}") }
            return
        }
        val converted = processor.convertAbsorbanceToTransmittance(current)
        _uiState.update {
            it.copy(currentSpectrum = converted, statusMessage = "Converted to Transmittance: %T = 10^(2 - A)")
        }
        detectPeaks()
    }

    fun saveToHistory() {
        viewModelScope.launch {
            val curr = _uiState.value
            val enriched = curr.currentSpectrum.copy(
                detectedPeaks = curr.currentSpectrum.detectedPeaks,
                metadata = curr.currentSpectrum.metadata.toMutableMap().apply {
                    if (curr.userSampleName.isNotBlank()) put("user_sample_name", curr.userSampleName)
                    if (curr.userSampleType.isNotBlank()) put("user_sample_type", curr.userSampleType)
                    if (curr.userExpectedComposition.isNotBlank()) put("user_expected_composition", curr.userExpectedComposition)
                    if (curr.userKnownComponents.isNotBlank()) put("user_known_components", curr.userKnownComponents)
                    if (curr.userMeasurementMethod.isNotBlank()) put("user_measurement_method", curr.userMeasurementMethod)
                    if (curr.userExperimentalNotes.isNotBlank()) put("user_notes", curr.userExperimentalNotes)
                    curr.aiResult?.let { ai ->
                        put("ai_summary", ai.summary)
                        put("ai_complete_interpretation", ai.completeSpectrumInterpretation)
                        put("ai_model", ai.modelName)
                    }
                }
            )
            repository.saveSpectrum(enriched)
            _uiState.update {
                it.copy(
                    currentSpectrum = enriched,
                    isSavedInHistory = true,
                    statusMessage = "Analysis '${enriched.name}' saved to local history."
                )
            }
        }
    }

    fun importFromFileStream(stream: InputStream, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val analysisRes = try {
                stream.use { s ->
                    importer.analyzeStream(s, fileName, SpectrumSourceType.FILE_IMPORT)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }

            analysisRes.onSuccess { analysis ->
                _uiState.update { it.copy(lastParsedTable = analysis.table, importError = null) }

                if (analysis.isConfidenceHigh && analysis.parsedSpectrum != null) {
                    val spec = analysis.parsedSpectrum
                    loadSpectrum(spec)
                    repository.saveSpectrum(spec)
                    _uiState.update {
                        it.copy(
                            isSavedInHistory = true,
                            statusMessage = "Imported '${spec.name}' (${spec.pointCount} points) & saved to history."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            pendingImportAnalysis = analysis,
                            statusMessage = analysis.ambiguityReason ?: "Verify column assignments for '$fileName'."
                        )
                    }
                }
            }.onFailure { err ->
                val reasonMsg = err.message ?: "File contains no valid numerical spectrum."
                val hint = when {
                    reasonMsg.contains("delimiter", ignoreCase = true) -> "Ensure the file uses comma, semicolon, tab, or whitespace separation."
                    reasonMsg.contains("zero data", ignoreCase = true) || reasonMsg.contains("empty", ignoreCase = true) -> "The file appears empty or contains only comments/headers."
                    else -> "Check that the file contains two numerical columns (Wavenumber and Transmittance/Absorbance)."
                }
                _uiState.update {
                    it.copy(
                        statusMessage = "Could not import file: $reasonMsg",
                        importError = ImportErrorNotice(
                            title = "File Import Notice",
                            message = "Could not interpret '$fileName':\n$reasonMsg",
                            recoveryHint = hint,
                            canOpenMapping = false
                        )
                    )
                }
            }
        }
    }

    fun confirmColumnMapping(mapping: ColumnMapping) {
        viewModelScope.launch(Dispatchers.IO) {
            val pending = _uiState.value.pendingImportAnalysis
            val table = pending?.table ?: _uiState.value.lastParsedTable
            if (table == null) {
                _uiState.update { it.copy(statusMessage = "No table data available to map.") }
                return@launch
            }

            val result = importer.buildSpectrumFromMapping(table, mapping, SpectrumSourceType.FILE_IMPORT)
            result.onSuccess { spec ->
                loadSpectrum(spec)
                repository.saveSpectrum(spec)
                _uiState.update {
                    it.copy(
                        pendingImportAnalysis = null,
                        importError = null,
                        isSavedInHistory = true,
                        statusMessage = "Imported '${spec.name}' (${spec.pointCount} points) & saved to history."
                    )
                }
            }.onFailure { err ->
                val reason = err.message ?: "Could not build spectrum from selected columns."
                _uiState.update {
                    it.copy(
                        statusMessage = "Mapping error: $reason",
                        importError = ImportErrorNotice(
                            title = "Column Mapping Notice",
                            message = reason,
                            recoveryHint = "Try selecting different columns or adjusting the decimal separator.",
                            canOpenMapping = true
                        )
                    )
                }
            }
        }
    }

    fun dismissImportDialog() {
        _uiState.update { it.copy(pendingImportAnalysis = null) }
    }

    fun dismissImportError() {
        _uiState.update { it.copy(importError = null) }
    }

    fun openColumnMappingForCurrentTable() {
        val table = _uiState.value.lastParsedTable
        if (table != null) {
            val analysisRes = (importer as StandardSpectrumImporter).analyzeText(
                content = table.rows.joinToString("\n") { it.joinToString(table.detectedDelimiter.toString()) },
                fileName = table.fileName
            )
            analysisRes.onSuccess { analysis ->
                _uiState.update { it.copy(pendingImportAnalysis = analysis) }
            }
        } else {
            _uiState.update { it.copy(statusMessage = "No raw tabular data for current spectrum.") }
        }
    }

    fun runAiInterpretation(customPrompt: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAiLoading = true,
                    aiWorkflowState = AiWorkflowState.PREPARING_LOCAL_DATA
                )
            }

            val current = _uiState.value.currentSpectrum

            // 1. Ensure local peaks are measured deterministically on device
            val measuredPeaks = withContext(Dispatchers.Default) {
                if (current.detectedPeaks.isNotEmpty()) {
                    current.detectedPeaks
                } else {
                    peakDetector.detectPeaks(current, _uiState.value.peakProminenceThreshold, _uiState.value.peakMinDistance, _uiState.value.peakMinHeight)
                }
            }

            // 2. Correlate local IR functional group dictionary matches
            val matchedWithDict = withContext(Dispatchers.Default) {
                fgMatcher.matchPeaks(measuredPeaks)
            }
            val dictAssignments = matchedWithDict.mapNotNull { it.matchedFunctionalGroup }.distinct()

            // 3. Compute local reference standards alignment
            val referenceMatches = withContext(Dispatchers.Default) {
                DemoSpectraProvider.getAllReferenceSpectra().mapNotNull { ref ->
                    val matchingPeaks = ref.characteristicPeaks.filter { charWn ->
                        measuredPeaks.any { kotlin.math.abs(it.wavenumber - charWn) <= 20.0 }
                    }
                    if (matchingPeaks.size >= 2) {
                        "${ref.compoundName} (${matchingPeaks.size}/${ref.characteristicPeaks.size} aligned bands)"
                    } else null
                }
            }

            // 4. Assemble structured spectroscopy payload
            val currentContext = if (_uiState.value.userSampleName.isNotBlank() ||
                _uiState.value.userExpectedComposition.isNotBlank() ||
                _uiState.value.userKnownComponents.isNotBlank() ||
                _uiState.value.userExperimentalNotes.isNotBlank()
            ) {
                com.example.domain.spectroscopy.AIUserContext(
                    sampleName = _uiState.value.userSampleName,
                    sampleType = _uiState.value.userSampleType,
                    expectedComposition = _uiState.value.userExpectedComposition,
                    knownComponents = _uiState.value.userKnownComponents,
                    measurementMethod = _uiState.value.userMeasurementMethod,
                    experimentalNotes = _uiState.value.userExperimentalNotes
                )
            } else null

            val comparisonSummary = _uiState.value.numericalComparisonResult?.let { comp ->
                val hqi = comp.allAlgorithmScores[com.example.domain.spectroscopy.SimilarityAlgorithm.HQI] ?: comp.similarityScore
                val pearson = comp.allAlgorithmScores[com.example.domain.spectroscopy.SimilarityAlgorithm.PEARSON] ?: 0.0
                val cosine = comp.allAlgorithmScores[com.example.domain.spectroscopy.SimilarityAlgorithm.COSINE] ?: 0.0
                val nrmse = comp.allAlgorithmScores[com.example.domain.spectroscopy.SimilarityAlgorithm.SPECTRAL_DISTANCE] ?: 0.0
                "Reference Comparison: ${comp.reference.compoundName} (${comp.reference.databaseIdentifier}). HQI: %.1f%%, Pearson r: %.3f, Cosine: %.3f, NRMSE: %.3f. Matched Peaks: ${comp.matchedPeaksCount}/${comp.totalUnknownPeaksCount}. Overlap Range: ${comp.commonRangeMinWn.toInt()}–${comp.commonRangeMaxWn.toInt()} cm⁻¹."
            }

            val request = com.example.domain.spectroscopy.AIInterpretationRequest(
                spectrumName = current.name,
                spectrumType = current.spectrumType.label,
                xUnit = current.xUnit,
                yUnit = current.yUnit,
                minWavenumber = current.minWavenumber,
                maxWavenumber = current.maxWavenumber,
                pointCount = current.pointCount,
                peaks = matchedWithDict,
                dictionaryMatches = dictAssignments,
                referenceMatches = referenceMatches,
                preprocessingSteps = current.preprocessingHistory.map { "${it.stepType}: ${it.description}" },
                customPrompt = customPrompt,
                userContext = currentContext,
                structuredComparisonSummary = comparisonSummary
            )

            // 5. Explicit State: SENDING_AI_REQUEST & WAITING_FOR_AI_RESPONSE
            _uiState.update {
                it.copy(aiWorkflowState = AiWorkflowState.SENDING_AI_REQUEST)
            }
            _uiState.update {
                it.copy(aiWorkflowState = AiWorkflowState.WAITING_FOR_AI_RESPONSE)
            }

            // 6. Make genuine external API request
            val result = aiService.interpretSpectrum(request)
            val diag = aiService.getDiagnosticsInfo()

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(aiWorkflowState = AiWorkflowState.AI_RESPONSE_RECEIVED)
                }
                _uiState.update {
                    it.copy(aiWorkflowState = AiWorkflowState.INTERPRETING_RESPONSE)
                }
                _uiState.update {
                    it.copy(
                        isAiLoading = false,
                        aiWorkflowState = AiWorkflowState.COMPLETE,
                        aiResult = result,
                        aiDiagnostics = diag,
                        statusMessage = "AI scientific interpretation complete."
                    )
                }
            } else {
                val failureMsg = result.errorMessage ?: result.summary
                _uiState.update {
                    it.copy(
                        isAiLoading = false,
                        aiWorkflowState = AiWorkflowState.AI_REQUEST_FAILED,
                        aiResult = result,
                        aiDiagnostics = diag,
                        statusMessage = failureMsg
                    )
                }
            }
        }
    }

    fun testAiConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true) }
            val testRes = aiService.testConnection()
            val diag = aiService.getDiagnosticsInfo()
            _uiState.update {
                it.copy(
                    isTestingConnection = false,
                    aiConnectionTestResult = testRes,
                    aiDiagnostics = diag,
                    statusMessage = if (testRes.isSuccess) "AI Connection: Connected successfully (${testRes.durationMs}ms)" else (testRes.errorMessage ?: "AI Connection Test: FAILED")
                )
            }
        }
    }

    fun setCustomApiKey(key: String?) {
        com.example.domain.spectroscopy.AIConfiguration.setKey(key)
        val diag = aiService.getDiagnosticsInfo()
        _uiState.update {
            it.copy(
                aiDiagnostics = diag,
                statusMessage = if (!key.isNullOrBlank()) "API key updated successfully." else "Custom API key cleared."
            )
        }
    }

    fun getAiService(): com.example.domain.spectroscopy.AIService = aiService

    fun updateUserContext(
        sampleName: String? = null,
        sampleType: String? = null,
        expectedComposition: String? = null,
        knownComponents: String? = null,
        measurementMethod: String? = null,
        notes: String? = null
    ) {
        _uiState.update {
            it.copy(
                userSampleName = sampleName ?: it.userSampleName,
                userSampleType = sampleType ?: it.userSampleType,
                userExpectedComposition = expectedComposition ?: it.userExpectedComposition,
                userKnownComponents = knownComponents ?: it.userKnownComponents,
                userMeasurementMethod = measurementMethod ?: it.userMeasurementMethod,
                userExperimentalNotes = notes ?: it.userExperimentalNotes
            )
        }
    }

    fun setActiveAiLayerIndex(layerIndex: Int) {
        _uiState.update { it.copy(activeAiLayerIndex = layerIndex.coerceIn(0, 2)) }
    }

    fun setPrivacyAcknowledged(acknowledged: Boolean) {
        _uiState.update { it.copy(hasAcknowledgedPrivacy = acknowledged) }
    }

    fun clearAiResult() {
        _uiState.update {
            it.copy(
                aiResult = null,
                aiWorkflowState = AiWorkflowState.IDLE
            )
        }
    }

    fun generateReportText(): String {
        return reportGen.generateTextReport(_uiState.value.currentSpectrum)
    }

    fun getLaboratoryReportData(): com.example.domain.spectroscopy.LaboratoryAnalysisReportData {
        val state = _uiState.value
        return com.example.domain.spectroscopy.LaboratoryAnalysisReportData(
            spectrum = state.currentSpectrum,
            detectedPeaks = state.currentSpectrum.detectedPeaks,
            userSampleName = state.userSampleName,
            userSampleType = state.userSampleType,
            userExpectedComposition = state.userExpectedComposition,
            userKnownComponents = state.userKnownComponents,
            userMeasurementMethod = state.userMeasurementMethod,
            userExperimentalNotes = state.userExperimentalNotes,
            aiResult = state.aiResult,
            numericalComparisonResult = state.numericalComparisonResult
        )
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
