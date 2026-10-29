package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.demo.DemoSpectraProvider
import com.example.domain.model.SpectrumType
import com.example.domain.spectroscopy.ColumnMapping
import com.example.domain.spectroscopy.ImportXUnit
import com.example.domain.spectroscopy.StandardFunctionalGroupMatcher
import com.example.domain.spectroscopy.StandardPeakDetector
import com.example.domain.spectroscopy.StandardReportGenerator
import com.example.domain.spectroscopy.StandardSpectrumImporter
import com.example.domain.spectroscopy.StandardSpectrumProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.runBlocking
import com.example.domain.spectroscopy.StandardAIService
import com.example.domain.spectroscopy.AIInterpretationRequest
import com.example.domain.spectroscopy.AIConfiguration
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SpectroIR", appName)
    }

    @Test
    fun `launch MainActivity without crash`() {
        org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    }

    @Test
    fun `verify demo spectra generation and peak detection`() {
        val polystyrene = DemoSpectraProvider.createPolystyreneDemo()
        assertTrue("Spectrum points should not be empty", polystyrene.points.isNotEmpty())
        assertEquals("cm⁻¹", polystyrene.xUnit)

        val detector = StandardPeakDetector()
        val peaks = detector.detectPeaks(polystyrene, minProminence = 10.0)
        assertTrue("Should detect peaks in polystyrene standard", peaks.isNotEmpty())

        val matcher = StandardFunctionalGroupMatcher()
        val matchedPeaks = matcher.matchPeaks(peaks)
        assertTrue("Peaks should be evaluated by functional group matcher", matchedPeaks.isNotEmpty())
    }

    @Test
    fun `verify spectrum processor conversions`() {
        val polystyrene = DemoSpectraProvider.createPolystyreneDemo()
        val processor = StandardSpectrumProcessor()

        val absorbance = processor.convertTransmittanceToAbsorbance(polystyrene)
        assertEquals(SpectrumType.Absorbance, absorbance.spectrumType)

        val backToTransmittance = processor.convertAbsorbanceToTransmittance(absorbance)
        assertEquals(SpectrumType.Transmittance, backToTransmittance.spectrumType)
    }

    @Test
    fun `import CSV with comma delimiter and headers`() {
        val importer = StandardSpectrumImporter()
        val csv = """
            Wavenumber,Transmittance
            4000.0,98.2
            3500.0,72.4
            3000.0,45.1
            1715.0,12.3
            1000.0,65.8
            400.0,85.0
        """.trimIndent()

        val result = importer.parseText(csv, "sample_transmittance.csv")
        assertTrue("Import should succeed", result.isSuccess)
        val spectrum = result.getOrThrow()

        assertEquals("sample_transmittance", spectrum.name)
        assertEquals(6, spectrum.pointCount)
        assertEquals(SpectrumType.Transmittance, spectrum.spectrumType)
        assertEquals("%T", spectrum.yUnit)
        assertEquals(400.0, spectrum.minWavenumber, 0.01)
        assertEquals(4000.0, spectrum.maxWavenumber, 0.01)
    }

    @Test
    fun `import CSV with semicolon delimiter and European decimal commas`() {
        val importer = StandardSpectrumImporter()
        val csv = """
            Wavenumber;Absorbance
            4000,0;0,05
            3000,0;0,85
            1715,0;1,42
            1500,0;0,35
            1000,0;0,78
            500,0;0,12
        """.trimIndent()

        val result = importer.parseText(csv, "european_sample.csv")
        assertTrue("Import with semicolon and decimal comma should succeed", result.isSuccess)
        val spectrum = result.getOrThrow()

        assertEquals(6, spectrum.pointCount)
        assertEquals(SpectrumType.Absorbance, spectrum.spectrumType)
        assertEquals(0.05, spectrum.points.first { it.wavenumber == 4000.0 }.signalValue, 0.001)
        assertEquals(1.42, spectrum.points.first { it.wavenumber == 1715.0 }.signalValue, 0.001)
    }

    @Test
    fun `import TSV tab-delimited file`() {
        val importer = StandardSpectrumImporter()
        val tsv = "Wavenumber\t%T\n4000.0\t99.1\n3000.0\t60.2\n2000.0\t92.3\n1000.0\t75.4\n400.0\t80.0"

        val result = importer.parseText(tsv, "instrument_data.tsv")
        assertTrue("TSV import should succeed", result.isSuccess)
        val spectrum = result.getOrThrow()

        assertEquals(5, spectrum.pointCount)
        assertEquals(SpectrumType.Transmittance, spectrum.spectrumType)
    }

    @Test
    fun `import file with metadata headers and comments`() {
        val importer = StandardSpectrumImporter()
        val text = """
            # Instrument: PerkinElmer Spectrum Two
            # Sample: Polystyrene Standard Film
            # Resolution: 4.0 cm-1
            # Scans: 16
            Wavenumber,Transmittance
            4000.0,95.0
            3000.0,50.0
            2000.0,90.0
            1500.0,40.0
            1000.0,70.0
            500.0,85.0
        """.trimIndent()

        val result = importer.parseText(text, "cal_standard.csv")
        assertTrue(result.isSuccess)
        val spectrum = result.getOrThrow()

        assertEquals("Polystyrene Standard Film", spectrum.metadata["Sample"])
        assertEquals("PerkinElmer Spectrum Two", spectrum.metadata["Instrument"])
        assertEquals(6, spectrum.pointCount)
    }

    @Test
    fun `import reversed wavenumber order is correctly sorted`() {
        val importer = StandardSpectrumImporter()
        // FTIR spectra exported descending: 4000 down to 400 cm⁻¹
        val text = """
            Wavenumber,Transmittance
            4000.0,95.0
            3000.0,50.0
            2000.0,90.0
            1000.0,70.0
            400.0,85.0
        """.trimIndent()

        val result = importer.parseText(text, "descending.csv")
        assertTrue(result.isSuccess)
        val spectrum = result.getOrThrow()

        // Cleaned internal points are sorted ascending
        assertEquals(400.0, spectrum.points.first().wavenumber, 0.01)
        assertEquals(4000.0, spectrum.points.last().wavenumber, 0.01)
        // Raw points preserve the un-reversed input order
        assertEquals(4000.0, spectrum.rawPoints.first().wavenumber, 0.01)
    }

    @Test
    fun `import wavelength in micrometers and convert to wavenumber`() {
        val importer = StandardSpectrumImporter()
        val content = """
            Wavelength (um),Absorbance
            2.5,0.05
            3.3333,0.85
            5.8309,1.45
            10.0,0.60
            20.0,0.20
        """.trimIndent()

        val analysis = importer.analyzeText(content, "nir_spectrum.csv").getOrThrow()
        assertEquals(ImportXUnit.WAVELENGTH_UM, analysis.suggestedMapping.xUnit)

        val spectrum = analysis.parsedSpectrum
        assertNotNull("Spectrum should be parsed", spectrum)
        // 2.5 µm -> 10000 / 2.5 = 4000 cm⁻¹
        // 10.0 µm -> 10000 / 10.0 = 1000 cm⁻¹
        // 20.0 µm -> 10000 / 20.0 = 500 cm⁻¹
        val p4000 = spectrum!!.points.find { abs(it.wavenumber - 4000.0) < 1.0 }
        val p1000 = spectrum.points.find { abs(it.wavenumber - 1000.0) < 1.0 }
        val p500 = spectrum.points.find { abs(it.wavenumber - 500.0) < 1.0 }

        assertNotNull("Point at 4000 cm⁻¹ should exist", p4000)
        assertNotNull("Point at 1000 cm⁻¹ should exist", p1000)
        assertNotNull("Point at 500 cm⁻¹ should exist", p500)
    }

    @Test
    fun `import multi-column file with custom column mapping`() {
        val importer = StandardSpectrumImporter()
        val content = """
            Wavenumber,Blank_Abs,Sample_Abs,Transmittance
            4000.0,0.01,0.06,87.1
            3000.0,0.02,0.92,12.0
            2000.0,0.01,0.05,89.1
            1715.0,0.03,1.45,3.5
            1000.0,0.02,0.65,22.4
            500.0,0.01,0.18,66.1
        """.trimIndent()

        val analysis = importer.analyzeText(content, "multiplex.csv").getOrThrow()
        // Multiple candidate Y columns (Blank_Abs, Sample_Abs, Transmittance) -> not high confidence
        assertTrue(analysis.candidateYIndices.size >= 2)

        // User maps Col 0 (Wavenumber) and Col 2 (Sample_Abs)
        val customMapping = ColumnMapping(
            xColumnIndex = 0,
            yColumnIndex = 2,
            xUnit = ImportXUnit.WAVENUMBER_CM_1,
            spectrumType = SpectrumType.Absorbance
        )

        val built = importer.buildSpectrumFromMapping(analysis.table, customMapping).getOrThrow()
        assertEquals(SpectrumType.Absorbance, built.spectrumType)
        assertEquals(6, built.pointCount)
        assertEquals(1.45, built.points.first { it.wavenumber == 1715.0 }.signalValue, 0.001)
    }

    @Test
    fun `data validation handles missing values and duplicate wavenumbers`() {
        val importer = StandardSpectrumImporter()
        val content = """
            Wavenumber,Transmittance
            4000.0,95.0
            4000.0,97.0
            3000.0,invalid_number
            2000.0,88.0
            ,missing_x
            1500.0,
            1000.0,75.0
            500.0,80.0
        """.trimIndent()

        val result = importer.parseText(content, "noisy_data.csv")
        assertTrue(result.isSuccess)
        val spectrum = result.getOrThrow()

        // 4000.0 was duplicated with 95.0 and 97.0 -> averaged to 96.0!
        val p4000 = spectrum.points.first { it.wavenumber == 4000.0 }
        assertEquals(96.0, p4000.signalValue, 0.01)

        // Invalid and missing rows were filtered out safely
        assertTrue(spectrum.points.all { !it.signalValue.isNaN() && it.wavenumber > 0 })
    }

    @Test
    fun `synthetic spectrum with known peaks verifies position, prominence, width, FWHM and area`() {
        val detector = StandardPeakDetector()

        // Generate synthetic spectrum: baseline = 0.05, peak at 1715 cm⁻¹ with amplitude 1.5, sigma = 10
        // FWHM = 2 * sqrt(2 * ln(2)) * sigma ≈ 23.55 cm⁻¹
        val points = mutableListOf<com.example.domain.model.SpectrumPoint>()
        val startWn = 400.0
        val endWn = 4000.0
        val step = 1.0
        var wn = startWn
        while (wn <= endWn) {
            val d1 = (wn - 1715.0) / 10.0
            val peak1 = 1.50 * kotlin.math.exp(-0.5 * d1 * d1)

            val d2 = (wn - 2960.0) / 15.0
            val peak2 = 0.85 * kotlin.math.exp(-0.5 * d2 * d2)

            val d3 = (wn - 1050.0) / 8.0
            val peak3 = 0.60 * kotlin.math.exp(-0.5 * d3 * d3)

            val signal = 0.05 + peak1 + peak2 + peak3
            points.add(com.example.domain.model.SpectrumPoint(wn, signal))
            wn += step
        }

        val synthetic = com.example.domain.model.Spectrum(
            name = "Synthetic Test Spectrum",
            spectrumType = SpectrumType.Absorbance,
            points = points
        )

        val peaks = detector.detectPeaks(synthetic, minProminence = 0.2, minDistance = 20.0)
        assertEquals(3, peaks.size)

        // Find the 1715 cm⁻¹ peak
        val carbonyl = peaks.find { abs(it.wavenumber - 1715.0) <= 2.0 }
        assertNotNull("Should detect 1715 cm⁻¹ peak", carbonyl)
        carbonyl?.let { p ->
            assertEquals(1715.0, p.wavenumber, 1.0)
            assertEquals(1.55, p.signalValue, 0.05) // baseline 0.05 + amplitude 1.5
            assertEquals(1.50, p.prominence, 0.08)  // prominence above baseline
            assertEquals(23.55, p.fwhm, 2.5)        // theoretical FWHM ≈ 23.55 cm⁻¹
            assertTrue("Base width should be larger than FWHM", p.width > p.fwhm)
            assertTrue("Peak area should be strictly positive", p.area > 5.0)
            assertEquals(100.0, p.relativeIntensity, 0.1) // Carbonyl is highest peak
        }

        // Test manual peak addition
        val manualPeak = detector.createManualPeak(synthetic, 2960.0)
        assertEquals(2960.0, manualPeak.wavenumber, 1.0)
        assertTrue(manualPeak.isManual)
        assertTrue(manualPeak.prominence > 0.5)
    }

    @Test
    fun `response to noise and noise thresholding in peak detection`() {
        val detector = StandardPeakDetector()
        val points = mutableListOf<com.example.domain.model.SpectrumPoint>()
        var wn = 400.0
        while (wn <= 4000.0) {
            val d = (wn - 1715.0) / 10.0
            val peak = 1.20 * kotlin.math.exp(-0.5 * d * d)
            // Deterministic high-frequency noise ripple: amplitude 0.03
            val noise = 0.03 * kotlin.math.sin(wn * 1.5)
            points.add(com.example.domain.model.SpectrumPoint(wn, 0.1 + peak + noise))
            wn += 1.0
        }

        val noisySpectrum = com.example.domain.model.Spectrum(
            name = "Noisy Synthetic",
            spectrumType = SpectrumType.Absorbance,
            points = points
        )

        // With low threshold (0.01), noise ripples are detected
        val noisyPeaks = detector.detectPeaks(noisySpectrum, minProminence = 0.01, minDistance = 2.0)
        assertTrue("Low threshold detects noise ripples", noisyPeaks.size > 5)

        // With appropriate threshold (0.25), only the true peak is retained
        val filteredPeaks = detector.detectPeaks(noisySpectrum, minProminence = 0.25, minDistance = 20.0)
        assertEquals(1, filteredPeaks.size)
        assertEquals(1715.0, filteredPeaks.first().wavenumber, 2.0)
    }

    @Test
    fun `response to Savitzky-Golay smoothing preserves peak position and reduces noise`() {
        val processor = StandardSpectrumProcessor()
        val detector = StandardPeakDetector()

        val points = mutableListOf<com.example.domain.model.SpectrumPoint>()
        var wn = 1500.0
        while (wn <= 1900.0) {
            val d = (wn - 1715.0) / 8.0
            val peak = 1.0 * kotlin.math.exp(-0.5 * d * d)
            val noise = 0.04 * kotlin.math.sin(wn * 2.0)
            points.add(com.example.domain.model.SpectrumPoint(wn, peak + noise))
            wn += 1.0
        }

        val noisy = com.example.domain.model.Spectrum(
            name = "Noisy Carbonyl",
            spectrumType = SpectrumType.Absorbance,
            points = points
        )

        val smoothed = processor.smoothSavitzkyGolay(noisy, windowSize = 9, polyOrder = 2)
        assertEquals(noisy.pointCount, smoothed.pointCount)
        assertEquals(1, smoothed.preprocessingHistory.size)
        assertEquals("Savitzky-Golay", smoothed.preprocessingHistory.first().parameters["method"])

        // Peak detector on smoothed spectrum finds the true peak with high accuracy
        val peaks = detector.detectPeaks(smoothed, minProminence = 0.5)
        assertEquals(1, peaks.size)
        assertEquals(1715.0, peaks.first().wavenumber, 1.0)
    }

    @Test
    fun `verify baseline correction methods on tilted spectrum`() {
        val processor = StandardSpectrumProcessor()

        // Create spectrum with strong upward linear tilt: y = 0.0002 * (wn - 400)
        val points = mutableListOf<com.example.domain.model.SpectrumPoint>()
        var wn = 400.0
        while (wn <= 4000.0) {
            val tilt = 0.0002 * (wn - 400.0)
            val peak = if (abs(wn - 1715.0) < 30.0) 1.0 else 0.0
            points.add(com.example.domain.model.SpectrumPoint(wn, tilt + peak))
            wn += 2.0
        }

        val tilted = com.example.domain.model.Spectrum(
            name = "Tilted Spectrum",
            spectrumType = SpectrumType.Absorbance,
            points = points
        )

        // 1. Polynomial baseline
        val polyRes = processor.baselineCorrectPolynomial(tilted, degree = 2)
        assertNotNull(polyRes.baselinePoints)
        assertEquals(tilted.pointCount, polyRes.baselinePoints.size)
        // Baseline corrected spectrum should have baseline near 0 at high wavenumbers
        val p3800 = polyRes.correctedSpectrum.points.find { abs(it.wavenumber - 3800.0) < 3.0 }
        assertTrue("Baseline corrected signal should be near 0", (p3800?.signalValue ?: 10.0) < 0.2)

        // 2. Rubberband baseline
        val rubberRes = processor.baselineCorrectRubberband(tilted, numSegments = 20)
        assertEquals(tilted.pointCount, rubberRes.correctedSpectrum.pointCount)

        // 3. ALS baseline
        val alsRes = processor.baselineCorrectALS(tilted, lambda = 100000.0, p = 0.01, maxIterations = 5)
        assertEquals(tilted.pointCount, alsRes.correctedSpectrum.pointCount)
    }

    @Test
    fun `verify spectral normalization methods`() {
        val processor = StandardSpectrumProcessor()
        val polystyrene = DemoSpectraProvider.createPolystyreneDemo()

        // 1. Min-Max normalization
        val minMax = processor.normalizeMinMax(polystyrene, 0.0, 100.0)
        assertEquals(0.0, minMax.minSignal, 0.01)
        assertEquals(100.0, minMax.maxSignal, 0.01)

        // 2. Vector L2 normalization
        val vectorNorm = processor.normalizeVector(polystyrene)
        var sumSq = 0.0
        for (pt in vectorNorm.points) {
            sumSq += pt.signalValue * pt.signalValue
        }
        assertEquals(1.0, kotlin.math.sqrt(sumSq), 0.01)

        // 3. Peak normalization (on Absorbance spectrum where peak maximum = 100.0)
        val absorbance = processor.convertTransmittanceToAbsorbance(polystyrene)
        val peakNorm = processor.normalizePeak(absorbance, targetWn = null, targetIntensity = 100.0)
        assertEquals(100.0, peakNorm.maxSignal, 0.01)
    }

    @Test
    fun `verify first and second derivatives calculation without destroying original`() {
        val processor = StandardSpectrumProcessor()
        val polystyrene = DemoSpectraProvider.createPolystyreneDemo()
        val originalPointsCount = polystyrene.pointCount

        // 1st derivative
        val deriv1 = processor.calculateDerivative(polystyrene, order = 1, sgWindowSize = 9, sgPolyOrder = 2)
        assertEquals(originalPointsCount, deriv1.pointCount)
        assertEquals("dY/dν", deriv1.yUnit)
        assertEquals(1, deriv1.preprocessingHistory.size)

        // 2nd derivative
        val deriv2 = processor.calculateDerivative(polystyrene, order = 2, sgWindowSize = 9, sgPolyOrder = 2)
        assertEquals(originalPointsCount, deriv2.pointCount)
        assertEquals("d²Y/dν²", deriv2.yUnit)

        // Original spectrum points remain untouched
        assertEquals(originalPointsCount, polystyrene.rawPoints.size)

        // Reset restores original state
        val reset = processor.resetToOriginal(deriv1)
        assertEquals(0, reset.preprocessingHistory.size)
        assertEquals(polystyrene.rawPoints, reset.points)
    }

    @Test
    fun `real FTIR spectrum produces complete professional peak table`() {
        val polystyrene = DemoSpectraProvider.createPolystyreneDemo()
        val detector = StandardPeakDetector()
        val matcher = StandardFunctionalGroupMatcher()

        val peaks = detector.detectPeaks(polystyrene, minProminence = 5.0, minDistance = 15.0)
        val matched = matcher.matchPeaks(peaks)

        assertTrue("Should detect at least 6 standard polystyrene peaks", matched.size >= 6)

        // Verify each peak contains full scientific metrics
        for (peak in matched) {
            assertTrue("Peak index should be assigned", peak.index > 0)
            assertTrue("Wavenumber in IR range", peak.wavenumber in 400.0..4000.0)
            assertTrue("Prominence should be >= 5.0", peak.prominence >= 5.0)
            assertTrue("Relative intensity in 0-100%", peak.relativeIntensity in 0.0..100.0)
            assertTrue("Peak width should be non-negative", peak.width >= 0.0)
        }

        // Verify report generation
        val reportGen = StandardReportGenerator()
        val specWithPeaks = polystyrene.copy(detectedPeaks = matched)
        val reportText = reportGen.generateTextReport(specWithPeaks)
        assertTrue(reportText.contains("FWHM"))
        assertTrue(reportText.contains("Area"))
        assertTrue(reportText.contains("Prom."))
    }

    @Test
    fun `verify AI analysis does not produce fake response when API key is missing`() = runBlocking {
        AIConfiguration.customApiKey = "EMPTY"
        val aiService = StandardAIService()
        val polystyrene = DemoSpectraProvider.createPolystyreneDemo()
        val detector = StandardPeakDetector()
        val peaks = detector.detectPeaks(polystyrene, minProminence = 10.0)

        val request = AIInterpretationRequest(
            spectrumName = polystyrene.name,
            spectrumType = polystyrene.spectrumType.label,
            xUnit = polystyrene.xUnit,
            yUnit = polystyrene.yUnit,
            minWavenumber = polystyrene.minWavenumber,
            maxWavenumber = polystyrene.maxWavenumber,
            pointCount = polystyrene.pointCount,
            peaks = peaks,
            dictionaryMatches = listOf("Aromatic C-H stretch (3100-3000 cm⁻¹)", "Monosubstituted benzene (750, 695 cm⁻¹)"),
            referenceMatches = listOf("Polystyrene standard (FTIR-PS-4001)"),
            preprocessingSteps = listOf("Baseline: None", "Smoothing: None")
        )

        val result = aiService.interpretSpectrum(request)
        // Must NOT succeed with a fake simulated analysis
        assertFalse("AI interpretation must fail without valid API key", result.isSuccess)
        assertFalse("Must not claim to be from real API", result.isFromRealApi)
        assertTrue("Error summary must mention unavailable or API key", result.summary.contains("unavailable") || result.summary.contains("API key"))
        // Local peak measurements must remain attached and unmodified
        assertEquals(peaks.size, result.measuredPeaksCount)
        assertEquals(2, result.dictionaryMatchesCount)
        assertEquals(1, result.referenceMatchesCount)
        AIConfiguration.customApiKey = null
    }

    @Test
    fun `verify AI analysis with invalid key reports failure instead of fake interpretation`() = runBlocking {
        // Set an invalid key to test genuine external API failure handling
        AIConfiguration.customApiKey = "AIzaSy_TEST_INVALID_KEY_9999"
        val aiService = StandardAIService()
        val polystyrene = DemoSpectraProvider.createPolystyreneDemo()
        val detector = StandardPeakDetector()
        val peaks = detector.detectPeaks(polystyrene, minProminence = 10.0)

        val request = AIInterpretationRequest(
            spectrumName = polystyrene.name,
            spectrumType = polystyrene.spectrumType.label,
            xUnit = polystyrene.xUnit,
            yUnit = polystyrene.yUnit,
            minWavenumber = polystyrene.minWavenumber,
            maxWavenumber = polystyrene.maxWavenumber,
            pointCount = polystyrene.pointCount,
            peaks = peaks
        )

        val result = aiService.interpretSpectrum(request)
        // External call with invalid key must fail gracefully without generating fake responses
        assertFalse("AI interpretation must fail on invalid credentials", result.isSuccess)
        assertTrue("Must state that API request failed", result.summary.contains("API request failed") || result.summary.contains("unavailable"))
        AIConfiguration.customApiKey = null
    }

    @Test
    fun `verify spectroscopy dictionary contains major chemical bonds and wavenumber ranges`() {
        val database = com.example.domain.spectroscopy.SpectroscopyDatabase.STANDARD_ASSIGNMENTS
        assertTrue("Database must contain comprehensive assignments", database.size >= 25)

        // Verify major bonds coverage: O-H, N-H, C-H, C=O, C=C, C≡C, C≡N, C-O, C-X, Si-O, P-O, S=O
        val bonds = database.map { it.bond }.toSet()
        assertTrue("Must cover O-H", bonds.contains("O-H"))
        assertTrue("Must cover N-H", bonds.contains("N-H"))
        assertTrue("Must cover C-H", bonds.contains("C-H"))
        assertTrue("Must cover C=O", bonds.contains("C=O"))
        assertTrue("Must cover C=C", bonds.contains("C=C"))
        assertTrue("Must cover C≡C", bonds.contains("C≡C"))
        assertTrue("Must cover C≡N", bonds.contains("C≡N"))
        assertTrue("Must cover C-O", bonds.contains("C-O"))
        assertTrue("Must cover C-F or C-Cl", bonds.any { it.startsWith("C-") })
        assertTrue("Must cover Si-O", bonds.contains("Si-O"))
        assertTrue("Must cover P-O", bonds.contains("P-O"))
        assertTrue("Must cover S=O", bonds.contains("S=O"))

        // Ensure every assignment has valid ranges
        for (a in database) {
            assertTrue("min < max for ${a.id}", a.minWavenumber < a.maxWavenumber)
            assertTrue("typical within range for ${a.id}", a.typicalWavenumber in a.minWavenumber..a.maxWavenumber)
            assertTrue("notes must not be empty", a.notes.isNotBlank())
            assertTrue("compound classes must not be empty", a.possibleCompoundClasses.isNotEmpty())
        }
    }

    @Test
    fun `verify wavenumber search returns compatible assignments without hardcoded single values`() {
        val engine = com.example.domain.spectroscopy.StandardManualInterpretationEngine()

        // 1715 cm⁻¹ search (Ketone, Aldehyde, Acid, Ester)
        val carbonylMatches = engine.searchByWavenumber(1715.0)
        assertTrue("1715 cm⁻¹ must find carbonyl matches", carbonylMatches.isNotEmpty())
        assertTrue("Must contain C=O bond", carbonylMatches.any { it.bond == "C=O" })

        // 3350 cm⁻¹ search (Alcohol O-H, Amine N-H)
        val hydrogenMatches = engine.searchByWavenumber(3350.0)
        assertTrue("3350 cm⁻¹ must find hydrogen bonded bands", hydrogenMatches.isNotEmpty())
        assertTrue("Must match O-H or N-H", hydrogenMatches.any { it.bond == "O-H" || it.bond == "N-H" })

        // 2240 cm⁻¹ search (Nitrile C≡N)
        val nitrileMatches = engine.searchByWavenumber(2240.0)
        assertTrue("2240 cm⁻¹ must match Nitrile", nitrileMatches.any { it.bond == "C≡N" })
    }

    @Test
    fun `verify multi-peak evidence corroboration and ambiguity detection`() {
        val engine = com.example.domain.spectroscopy.StandardManualInterpretationEngine()

        // Test multi-peak: 1740 cm⁻¹ (C=O ester) + 1240 cm⁻¹ (C-O ester stretch) + 2980 cm⁻¹ (sp3 C-H)
        val esterPeaks = listOf(1740.0, 1240.0, 2980.0)
        val esterResult = engine.analyzeMultiplePeaks(esterPeaks)

        assertEquals(3, esterResult.queryPeaksCount)
        assertTrue("Should detect compatible compound classes", esterResult.compatibleCompoundClasses.isNotEmpty())
        assertTrue(
            "Must corroborate Esters with high evidence or probability",
            esterResult.compatibleCompoundClasses.any { it.compoundClass.contains("Ester", ignoreCase = true) }
        )

        // Verify supporting evidence was linked to the carbonyl ester assignment
        val topInterp = esterResult.topInterpretations.firstOrNull { it.assignment.functionalGroup.contains("Ester") }
        assertNotNull("Should identify ester interpretation", topInterp)
        assertTrue("Must find supporting C-O partner peak", topInterp!!.supportingEvidenceFound.isNotEmpty())
        assertTrue("Corroboration score must be elevated when companion peaks exist", topInterp.corroborationScore > 0.5)

        // Ambiguities check: carbonyl present without broad O-H must mention ester/ketone distinction
        assertTrue("Ambiguity warning should explain environmental context", esterResult.potentialAmbiguities.isNotEmpty())
    }

    @Test
    fun `verify image digitization reconstructs numerical spectrum from plot`() {
        val processor = com.example.domain.spectroscopy.StandardSpectrumImageProcessor()

        // Create a synthetic 400x200 bitmap simulating a plotted FTIR spectrum with a dip
        val bmp = android.graphics.Bitmap.createBitmap(400, 200, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        canvas.drawColor(android.graphics.Color.WHITE)

        // Draw a simulated spectrum curve (dark line)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 3f
        }
        for (x in 0 until 400) {
            // Draw a curve with a trough at x=200
            val y = 50f + 80f * (1f - kotlin.math.exp(-((x - 200) * (x - 200)).toFloat() / 1500f))
            canvas.drawPoint(x.toFloat(), y, paint)
        }

        val xLeft = com.example.domain.spectroscopy.CalibrationPoint(pixelX = 0f, wavenumber = 4000.0)
        val xRight = com.example.domain.spectroscopy.CalibrationPoint(pixelX = 400f, wavenumber = 400.0)
        val yCal = com.example.domain.spectroscopy.YCalibration(pixelYTop = 0f, signalTop = 100.0, pixelYBottom = 200f, signalBottom = 0.0)

        val result = processor.digitizeGraph(
            graphBitmap = bmp,
            xCalibrationLeft = xLeft,
            xCalibrationRight = xRight,
            yCalibration = yCal,
            spectrumType = SpectrumType.Transmittance,
            spectrumName = "Simulated Plotted Graph"
        )

        assertNotNull("Digitization result must not be null", result)
        assertTrue("Should extract points from curve", result.extractedPoints.isNotEmpty())
        assertEquals(com.example.domain.model.SpectrumSourceType.DIGITIZED, result.spectrum.sourceType)
        assertTrue("Spectrum metadata must document digitized nature", result.spectrum.metadata.containsKey("Notice"))
        assertTrue("Quality confidence must be non-zero measurable score", result.confidenceScore > 0.0)
        assertTrue("X axis must be cm⁻¹", result.spectrum.xUnit == "cm⁻¹")
        assertEquals(com.example.domain.spectroscopy.WavenumberAxisDirection.HIGH_TO_LOW, result.estimatedAxisDirection)
    }

    @Test
    fun `verify reference library provides authentic demonstration dataset with full metadata`() {
        val references = DemoSpectraProvider.getAllReferenceSpectra()
        assertTrue("Must provide demo reference standards", references.size >= 5)

        // Verify Polystyrene calibration standard
        val ps = references.find { it.id == "ref_polystyrene" }
        assertNotNull("Polystyrene reference must exist", ps)
        assertEquals("(C₈H₈)ₙ", ps!!.chemicalFormula)
        assertEquals("9003-53-6", ps.casNumber)
        assertEquals("Polymer", ps.materialClass)
        assertTrue("Must be marked as demo standard", ps.isDemo)
        assertFalse("Must not be user custom", ps.isUserCustom)
        assertTrue("Must have characteristic peaks", ps.characteristicPeaks.contains(1601.0))
        assertEquals("cm⁻¹", ps.xAxisUnit)
        assertNotNull("Must contain spectrum points", ps.spectrum)
        assertTrue("Must have non-empty points", ps.spectrum.pointCount > 100)

        // Verify Acetone standard
        val acetone = references.find { it.id == "ref_acetone" }
        assertNotNull("Acetone reference must exist", acetone)
        assertEquals("C₃H₆O", acetone!!.chemicalFormula)
        assertEquals("67-64-1", acetone.casNumber)
        assertTrue("Must include carbonyl peak ~1715 cm⁻¹", acetone.characteristicPeaks.contains(1715.0))
    }

    @Test
    fun `verify numerical spectrum matching calculates HQI, Pearson, difference spectrum, and peak matching`() {
        val matcher = com.example.domain.spectroscopy.StandardNumericalSpectrumMatcher()
        val references = DemoSpectraProvider.getAllReferenceSpectra()
        val psRef = references.first { it.id == "ref_polystyrene" }

        // Test matching polystyrene unknown against polystyrene reference (near identity)
        val unknown = DemoSpectraProvider.createPolystyreneDemo()
        val result = matcher.compareSpectra(
            unknown = unknown,
            reference = psRef,
            primaryAlgorithm = com.example.domain.spectroscopy.SimilarityAlgorithm.HQI,
            peakToleranceCm = 15.0
        )

        // Verify similarity score
        assertTrue("Self-match similarity score must be very high (> 95%)", result.similarityScore > 95.0)
        assertTrue("HQI must be high", (result.allAlgorithmScores[com.example.domain.spectroscopy.SimilarityAlgorithm.HQI] ?: 0.0) > 95.0)
        assertTrue("Pearson r must be high", (result.allAlgorithmScores[com.example.domain.spectroscopy.SimilarityAlgorithm.PEARSON] ?: 0.0) > 95.0)
        assertTrue("Cosine similarity must be high", (result.allAlgorithmScores[com.example.domain.spectroscopy.SimilarityAlgorithm.COSINE] ?: 0.0) > 95.0)

        // Verify Difference Spectrum
        assertNotNull("Difference spectrum must be computed", result.differenceSpectrum)
        assertTrue("Difference points must exist", result.differenceSpectrum.points.isNotEmpty())
        assertTrue("RMS difference must be very small for self-match", result.differenceSpectrum.rmsDifference < 5.0)

        // Verify Peak Matching
        assertTrue("Must find matched peak pairs", result.matchedPeaksCount > 5)
        val matchedPair = result.peakMatches.find { it.status == com.example.domain.spectroscopy.PeakMatchStatus.MATCHED && it.unknownPeakWn != null && abs(it.unknownPeakWn - 1601.0) < 5.0 }
        assertNotNull("Must match 1601 cm⁻¹ aromatic peak", matchedPair)
        assertNotNull("Difference in cm⁻¹ must be recorded", matchedPair!!.differenceCm)

        // Verify Scientific Caveat is clearly stated
        assertTrue("Must state scientific caveat that similarity is not proof of identity", result.scientificCaveat.contains("CRITICAL"))

        // Verify downstream AI evidence structure
        val aiEvidence = matcher.extractStructuredAiEvidence(result)
        assertNotNull("AI evidence bundle must be generated", aiEvidence)
        assertEquals(psRef.compoundName, aiEvidence.referenceCompoundName)
        assertTrue("Must record matched peaks summary", aiEvidence.matchedPeaksSummary.isNotEmpty())
    }

    @Test
    fun `verify numerical spectrum matching flags poor match against different compound`() {
        val matcher = com.example.domain.spectroscopy.StandardNumericalSpectrumMatcher()
        val references = DemoSpectraProvider.getAllReferenceSpectra()
        val acetoneRef = references.first { it.id == "ref_acetone" }

        // Compare Polystyrene unknown against Acetone reference
        val psUnknown = DemoSpectraProvider.createPolystyreneDemo()
        val result = matcher.compareSpectra(
            unknown = psUnknown,
            reference = acetoneRef,
            primaryAlgorithm = com.example.domain.spectroscopy.SimilarityAlgorithm.HQI,
            peakToleranceCm = 15.0
        )

        // Verify distinct spectra have significantly lower similarity score
        assertTrue("HQI between Polystyrene and Acetone should be distinctly lower than self-match", result.similarityScore < 85.0)
        // Verify unmatched peaks exist
        assertTrue("Must detect unmatched peaks between different chemical classes", result.peakMatches.any { it.status == com.example.domain.spectroscopy.PeakMatchStatus.UNMATCHED_UNKNOWN })
    }

    @Test
    fun `verify AIRequestBuilder structures local numerical measurements and user context without sending raw array`() {
        val acetone = DemoSpectraProvider.createAcetoneDemo()
        val detector = StandardPeakDetector()
        val peaks = detector.detectPeaks(acetone, minProminence = 10.0)

        val request = AIInterpretationRequest(
            spectrumName = acetone.name,
            spectrumType = acetone.spectrumType.label,
            xUnit = acetone.xUnit,
            yUnit = acetone.yUnit,
            minWavenumber = acetone.minWavenumber,
            maxWavenumber = acetone.maxWavenumber,
            pointCount = acetone.pointCount,
            peaks = peaks,
            dictionaryMatches = listOf("Carbonyl (Ketone)", "Aliphatic C-H"),
            referenceMatches = listOf("Acetone (5/5 aligned bands)"),
            userContext = com.example.domain.spectroscopy.AIUserContext(
                sampleName = "Unknown Solvent A",
                sampleType = "Neat Liquid",
                expectedComposition = "Volatile carbonyl cleaning solvent",
                knownComponents = "Trace water",
                measurementMethod = "ATR-FTIR (Diamond Crystal)",
                experimentalNotes = "Measured neat at 21°C"
            )
        )

        val prompt = com.example.domain.spectroscopy.AIRequestBuilder.buildPromptPayload(request)

        // Core principle verification
        assertTrue("Must include core principle statement", prompt.contains("The AI is an interpreter and scientific assistant, NOT the measurement instrument"))
        assertTrue("Must include physical organic chemistry guidance", prompt.contains("Distinguish physical observations from interpretations"))
        assertTrue("Must command scientifically cautious language", prompt.contains("consistent with") && prompt.contains("compatible with"))
        assertTrue("Must forbid definitive certainty without proof", prompt.contains("NEVER state \"definitely this compound\""))
        assertTrue("Must include locally measured peaks", prompt.contains("locallyMeasuredPeaks"))
        assertTrue("Must include user context as supporting information only", prompt.contains("userContext") && prompt.contains("Unknown Solvent A"))
        assertTrue("Must include expected schema with AI/database confidence estimate", prompt.contains("AI/database confidence estimate"))
    }

    @Test
    fun `verify AIResponseParser parses valid structured JSON into complete scientific fields`() {
        val json = """
            {
              "spectrumSummary": {
                "spectrumType": "Absorbance",
                "range": "4000 - 400 cm⁻¹",
                "peakCount": 3,
                "functionalGroupRegionSummary": "Intense sharp carbonyl band at 1715 cm⁻¹; aliphatic C-H stretch envelope at 2960 cm⁻¹",
                "fingerprintRegionSummary": "C-C skeletal deformations at 1365 cm⁻¹ and 1220 cm⁻¹"
              },
              "majorPeakAssignments": [
                {
                  "wavenumber": 1715.0,
                  "observedFeature": "Sharp intense band",
                  "possibleBond": "C=O",
                  "functionalGroup": "Aliphatic Ketone Carbonyl",
                  "vibrationalMode": "C=O stretch",
                  "supportingEvidence": "Absence of broad 3300 cm⁻¹ band excludes carboxylic acid or alcohol",
                  "alternativeExplanations": "Could be ester carbonyl if accompanied by 1250 cm⁻¹ C-O band"
                }
              ],
              "completeSpectrumInterpretation": "Spectral profile exhibits strong characteristics compatible with low-molecular-weight dialkyl ketones.",
              "referenceComparison": "High alignment observed with reference acetone library profile (HQI 98.4%).",
              "uncertainties": [
                "Overlapping skeletal modes in fingerprint region do not exclude trace higher homologous ketones"
              ],
              "additionalInformationNeeded": [
                "1H NMR to confirm alkyl methyl groups and purity"
              ],
              "confidenceEstimate": {
                "label": "AI/database confidence estimate",
                "level": "High",
                "basis": "Sharp 1715 cm⁻¹ carbonyl matched with characteristic aliphatic C-H modes"
              },
              "candidateCompounds": [
                "Compatible with aliphatic ketones (e.g. acetone)"
              ]
            }
        """.trimIndent()

        val request = AIInterpretationRequest(
            spectrumName = "Acetone Test",
            spectrumType = "Absorbance",
            xUnit = "cm⁻¹",
            yUnit = "Abs",
            minWavenumber = 400.0,
            maxWavenumber = 4000.0,
            pointCount = 500,
            peaks = emptyList()
        )

        val result = com.example.domain.spectroscopy.AIResponseParser.parse(
            rawText = json,
            request = request,
            modelName = "gemini-3.1-pro-preview",
            durationMs = 850L,
            statusCode = 200
        )

        assertTrue("Parser must succeed", result.isSuccess)
        assertNotNull("Spectrum summary must be populated", result.spectrumSummary)
        assertEquals("4000 - 400 cm⁻¹", result.spectrumSummary?.range)
        assertEquals(1, result.majorPeakAssignments.size)
        assertEquals("C=O", result.majorPeakAssignments.first().possibleBond)
        assertEquals("Aliphatic Ketone Carbonyl", result.majorPeakAssignments.first().functionalGroup)
        assertTrue("Uncertainties must be parsed", result.uncertainties.isNotEmpty())
        assertTrue("Additional information needed must be parsed", result.additionalInformationNeeded.isNotEmpty())
        assertEquals("AI/database confidence estimate", result.confidenceEstimate?.label)
        assertEquals("High", result.confidenceEstimate?.level)
    }

    @Test
    fun `verify AIResponseParser handles malformed responses gracefully with heuristic fallback without crashing`() {
        val malformedText = """
            Here is the analysis:
            - Spectrum indicates strong carbonyl absorption near 1715 cm⁻¹.
            - Compatible with ketone structural motifs.
            - Ambiguity remains regarding aliphatic chain length.
        """.trimIndent()

        val request = AIInterpretationRequest(
            spectrumName = "Raw Response Test",
            spectrumType = "Transmittance",
            xUnit = "cm⁻¹",
            yUnit = "%T",
            minWavenumber = 400.0,
            maxWavenumber = 4000.0,
            pointCount = 500,
            peaks = emptyList()
        )

        val result = com.example.domain.spectroscopy.AIResponseParser.parse(
            rawText = malformedText,
            request = request,
            modelName = "gemini-3.1-pro-preview",
            durationMs = 920L,
            statusCode = 200
        )

        assertTrue("Fallback must succeed without crashing", result.isSuccess)
        assertNotNull("Summary must contain raw response text", result.summary)
        assertTrue("Candidate extraction must handle fallback text", result.candidateCompounds.isNotEmpty())
        assertEquals("AI/database confidence estimate", result.confidenceEstimate?.label)
    }

    @Test
    fun `test laboratory CSV export generation`() {
        val polystyrene = DemoSpectraProvider.createPolystyreneDemo()
        val peaks = StandardPeakDetector().detectPeaks(polystyrene, minProminence = 10.0)
        val data = com.example.domain.spectroscopy.LaboratoryAnalysisReportData(
            spectrum = polystyrene,
            detectedPeaks = peaks,
            userSampleName = "Polystyrene Standard #102",
            userSampleType = "Polymer Calibration Film",
            userMeasurementMethod = "Transmission",
            userExpectedComposition = "Polystyrene homopolymer",
            userKnownComponents = "Styrene units",
            userExperimentalNotes = "Calibration standard NIST-traceable"
        )
        val csv = com.example.domain.spectroscopy.LaboratoryReportService.generateCsv(data)
        assertTrue(csv.contains("# SpectroIR Laboratory FTIR Spectroscopy Export"))
        assertTrue(csv.contains("# Sample Name: Polystyrene Standard #102"))
        assertTrue(csv.contains("Wavenumber (cm-1),Original Signal,Processed Signal"))
        assertTrue(csv.contains("Polystyrene homopolymer"))
    }

    @Test
    fun `test laboratory JSON export generation`() {
        val polystyrene = DemoSpectraProvider.createPolystyreneDemo()
        val peaks = StandardPeakDetector().detectPeaks(polystyrene, minProminence = 10.0)
        val data = com.example.domain.spectroscopy.LaboratoryAnalysisReportData(
            spectrum = polystyrene,
            detectedPeaks = peaks,
            userSampleName = "Polystyrene Standard #102"
        )
        val jsonStr = com.example.domain.spectroscopy.LaboratoryReportService.generateJson(data)
        val jsonObj = org.json.JSONObject(jsonStr)
        assertTrue(jsonObj.has("reportMetadata"))
        assertTrue(jsonObj.has("sampleInformation"))
        assertTrue(jsonObj.has("spectralProperties"))
        assertTrue(jsonObj.has("peakTable"))
        assertTrue(jsonObj.has("scientificLimitations"))
    }

    @Test
    fun `test laboratory spectrum bitmap generation`() {
        val polystyrene = DemoSpectraProvider.createPolystyreneDemo()
        val peaks = StandardPeakDetector().detectPeaks(polystyrene, minProminence = 10.0)
        val data = com.example.domain.spectroscopy.LaboratoryAnalysisReportData(
            spectrum = polystyrene,
            detectedPeaks = peaks,
            userSampleName = "Polystyrene Standard #102"
        )
        val bitmap = com.example.domain.spectroscopy.LaboratoryReportService.generateSpectrumBitmap(
            data = data,
            width = 800,
            height = 450,
            showPeakAnnotations = true,
            publicationWhite = true
        )
        assertNotNull(bitmap)
        assertEquals(800, bitmap.width)
        assertEquals(450, bitmap.height)
    }

    @Test
    fun `test laboratory PDF report generation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val polystyrene = DemoSpectraProvider.createPolystyreneDemo()
        val peaks = StandardPeakDetector().detectPeaks(polystyrene, minProminence = 10.0)
        val data = com.example.domain.spectroscopy.LaboratoryAnalysisReportData(
            spectrum = polystyrene,
            detectedPeaks = peaks,
            userSampleName = "Polystyrene Test Film"
        )
        val pdfFile = com.example.domain.spectroscopy.LaboratoryReportService.generatePdfReport(data, context)
        assertNotNull(pdfFile)
        assertTrue(pdfFile.exists())
        assertTrue("PDF file must have non-zero size", pdfFile.length() > 0)
    }

    @Test
    fun `test ExportManager export all 4 formats`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val polystyrene = DemoSpectraProvider.createPolystyreneDemo()
        val data = com.example.domain.spectroscopy.LaboratoryAnalysisReportData(
            spectrum = polystyrene,
            userSampleName = "Multi-Format Test"
        )

        for (fmt in com.example.util.ExportFormat.entries) {
            val res = com.example.util.ExportManager.exportReport(context, data, fmt)
            assertTrue("Export of ${fmt.name} must succeed: ${res.message}", res.success)
            assertNotNull("File for ${fmt.name} must not be null", res.file)
            assertTrue("File for ${fmt.name} must exist", res.file!!.exists())
        }
    }

    @Test
    fun `import wavelength in nanometers converts accurately to wavenumber`() {
        val importer = StandardSpectrumImporter()
        // 2500 nm = 2.5 µm = 4000 cm⁻¹
        // 5000 nm = 5.0 µm = 2000 cm⁻¹
        // 10000 nm = 10.0 µm = 1000 cm⁻¹
        // 25000 nm = 25.0 µm = 400 cm⁻¹
        val content = """
            Wavelength_nm,Transmittance
            2500.0,95.5
            5000.0,78.2
            10000.0,42.0
            20000.0,88.5
            25000.0,91.0
        """.trimIndent()

        val mapping = ColumnMapping(
            xColumnIndex = 0,
            yColumnIndex = 1,
            xUnit = ImportXUnit.WAVELENGTH_NM,
            spectrumType = SpectrumType.Transmittance
        )

        val table = importer.parseDelimitedText(content, "nm_spectrum.csv")
        val spectrum = importer.buildSpectrumFromMapping(table, mapping).getOrThrow()

        assertEquals(5, spectrum.pointCount)
        assertEquals(SpectrumType.Transmittance, spectrum.spectrumType)

        val p4000 = spectrum.points.find { abs(it.wavenumber - 4000.0) < 1.0 }
        val p2000 = spectrum.points.find { abs(it.wavenumber - 2000.0) < 1.0 }
        val p1000 = spectrum.points.find { abs(it.wavenumber - 1000.0) < 1.0 }
        val p400 = spectrum.points.find { abs(it.wavenumber - 400.0) < 1.0 }

        assertNotNull("2500 nm should convert to 4000 cm⁻¹", p4000)
        assertNotNull("5000 nm should convert to 2000 cm⁻¹", p2000)
        assertNotNull("10000 nm should convert to 1000 cm⁻¹", p1000)
        assertNotNull("25000 nm should convert to 400 cm⁻¹", p400)
    }

    @Test
    fun `data persistence round-trip preserves all spectrum fields, peaks, FWHM, and metadata`() {
        val polystyrene = DemoSpectraProvider.createPolystyreneDemo()
        val peaks = StandardPeakDetector().detectPeaks(polystyrene, minProminence = 10.0)
        val enriched = polystyrene.copy(
            name = "Lab Batch 2026-X",
            detectedPeaks = peaks,
            metadata = mapOf(
                "user_sample_name" to "Lab Batch 2026-X",
                "user_sample_type" to "Polymer Standard",
                "user_measurement_method" to "Diamond ATR",
                "user_notes" to "Room temp, 32 scans"
            )
        )

        // Convert to Room entity
        val entity = com.example.data.local.SpectrumEntity.fromDomainSpectrum(enriched)
        assertEquals(enriched.id, entity.id)
        assertEquals("Lab Batch 2026-X", entity.name)

        // Convert back to domain spectrum
        val restored = entity.toDomainSpectrum()
        assertEquals(enriched.id, restored.id)
        assertEquals(enriched.pointCount, restored.pointCount)
        assertEquals(enriched.spectrumType, restored.spectrumType)
        assertEquals(enriched.detectedPeaks.size, restored.detectedPeaks.size)
        assertEquals("Lab Batch 2026-X", restored.metadata["user_sample_name"])
        assertEquals("Diamond ATR", restored.metadata["user_measurement_method"])

        // Check peak metrics preservation (wavenumber, signal, FWHM, area)
        for (i in enriched.detectedPeaks.indices) {
            val orig = enriched.detectedPeaks[i]
            val rest = restored.detectedPeaks[i]
            assertEquals(orig.wavenumber, rest.wavenumber, 0.01)
            assertEquals(orig.signalValue, rest.signalValue, 0.01)
            assertEquals(orig.fwhm, rest.fwhm, 0.01)
            assertEquals(orig.area, rest.area, 0.01)
        }
    }

    @Test
    fun `room database repository supports save, rename, duplicate, and delete`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.local.AppDatabase.getDatabase(context)
        val repo = com.example.data.repository.SpectrumRepositoryImpl(db.spectrumDao())

        val testSpec = DemoSpectraProvider.createAcetoneDemo().copy(
            id = "test_crud_spec_1",
            name = "CRUD Test Acetone"
        )

        // 1. Save
        repo.saveSpectrum(testSpec)
        val retrieved = repo.getSpectrumById("test_crud_spec_1")
        assertNotNull("Saved spectrum should be retrieved", retrieved)
        assertEquals("CRUD Test Acetone", retrieved?.name)

        // 2. Rename
        repo.renameSpectrum("test_crud_spec_1", "Renamed Acetone Sample")
        val renamed = repo.getSpectrumById("test_crud_spec_1")
        assertEquals("Renamed Acetone Sample", renamed?.name)

        // 3. Duplicate
        val duplicated = repo.duplicateSpectrum("test_crud_spec_1", "Acetone Duplicate Copy")
        assertNotNull("Duplicated spectrum should exist", duplicated)
        assertTrue("Duplicated id should be new", duplicated!!.id != "test_crud_spec_1")
        assertEquals("Acetone Duplicate Copy", duplicated.name)

        // 4. Delete
        repo.deleteSpectrum("test_crud_spec_1")
        val deleted = repo.getSpectrumById("test_crud_spec_1")
        assertEquals(null, deleted)

        // Duplicated copy still exists
        val dupRetrieved = repo.getSpectrumById(duplicated.id)
        assertNotNull(dupRetrieved)
        repo.deleteSpectrum(duplicated.id)
    }

    @Test
    fun `robustness test for empty and corrupted inputs fails gracefully without crashing`() {
        val importer = StandardSpectrumImporter()

        // 1. Empty string
        val emptyResult = importer.parseText("", "empty.csv")
        assertFalse("Empty file must fail gracefully", emptyResult.isSuccess)

        // 2. Only headers, no data
        val noDataResult = importer.parseText("Wavenumber,Transmittance\n", "no_data.csv")
        assertFalse("Header-only file must fail gracefully", noDataResult.isSuccess)

        // 3. Corrupted non-numerical content
        val corruptedText = """
            Header1,Header2
            abc,def
            ghi,jkl
            !@#,$%^
        """.trimIndent()
        val corruptResult = importer.parseText(corruptedText, "corrupt.csv")
        assertFalse("Corrupted file must fail gracefully", corruptResult.isSuccess)
    }

    @Test
    fun `verify moving average smoothing reduces high-frequency baseline ripple`() {
        val processor = StandardSpectrumProcessor()
        val detector = StandardPeakDetector()

        // Generate synthetic spectrum with high-frequency noise
        val points = mutableListOf<com.example.domain.model.SpectrumPoint>()
        var wn = 1000.0
        while (wn <= 2000.0) {
            val signal = 50.0 + 5.0 * kotlin.math.sin(wn * 0.8)
            points.add(com.example.domain.model.SpectrumPoint(wn, signal))
            wn += 1.0
        }

        val noisy = com.example.domain.model.Spectrum(
            name = "Ripple Spectrum",
            spectrumType = SpectrumType.Transmittance,
            points = points
        )

        val smoothed = processor.smoothMovingAverage(noisy, windowSize = 7)
        assertEquals(noisy.pointCount, smoothed.pointCount)
        assertEquals(1, smoothed.preprocessingHistory.size)
        assertEquals("Moving Average", smoothed.preprocessingHistory.first().parameters["method"])

        // Smoothed signals have reduced variance
        val origVariance = noisy.points.map { it.signalValue }.let { list ->
            val avg = list.average()
            list.map { (it - avg) * (it - avg) }.average()
        }
        val smoothedVariance = smoothed.points.map { it.signalValue }.let { list ->
            val avg = list.average()
            list.map { (it - avg) * (it - avg) }.average()
        }
        assertTrue("Moving average must reduce variance of ripple", smoothedVariance <= origVariance)
    }
}




