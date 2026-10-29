package com.example.domain.spectroscopy

import com.example.domain.model.Spectrum
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface ReportGenerator {
    fun generateTextReport(spectrum: Spectrum): String
    fun generateCsvExport(spectrum: Spectrum): String
}

class StandardReportGenerator : ReportGenerator {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    override fun generateTextReport(spectrum: Spectrum): String {
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("               SPECTRO-IR ANALYSIS REPORT         ")
        sb.appendLine("==================================================")
        sb.appendLine("Sample Name:       ${spectrum.name}")
        sb.appendLine("Source:            ${spectrum.sourceType.label}")
        sb.appendLine("Spectrum Type:     ${spectrum.spectrumType.label}")
        sb.appendLine("Wavenumber Range:  ${"%.1f".format(spectrum.minWavenumber)} - ${"%.1f".format(spectrum.maxWavenumber)} ${spectrum.xUnit}")
        sb.appendLine("Data Points:       ${spectrum.pointCount}")
        sb.appendLine("Created At:        ${dateFormat.format(Date(spectrum.createdAt))}")
        sb.appendLine("--------------------------------------------------")

        if (spectrum.metadata.isNotEmpty()) {
            sb.appendLine("METADATA:")
            spectrum.metadata.forEach { (k, v) ->
                sb.appendLine("  $k: $v")
            }
            sb.appendLine("--------------------------------------------------")
        }

        if (spectrum.preprocessingHistory.isNotEmpty()) {
            sb.appendLine("PREPROCESSING TRAIL:")
            spectrum.preprocessingHistory.forEachIndexed { idx, step ->
                sb.appendLine("  ${idx + 1}. [${step.stepType}] ${step.description}")
            }
            sb.appendLine("--------------------------------------------------")
        }

        sb.appendLine("DETECTED PEAKS (${spectrum.detectedPeaks.size}):")
        sb.appendLine(String.format("%-10s | %-8s | %-8s | %-8s | %-8s | %-20s", "Wavenumber", "Signal", "Prom.", "FWHM", "Area", "Assignment"))
        sb.appendLine("----------------------------------------------------------------------")
        for (peak in spectrum.detectedPeaks) {
            val fg = peak.matchedFunctionalGroup ?: "Unassigned"
            sb.appendLine(
                String.format(
                    Locale.US,
                    "%-8.1f cm⁻¹ | %-8.2f | %-8.2f | %-8.1f | %-8.1f | %-20s",
                    peak.wavenumber,
                    peak.signalValue,
                    peak.prominence,
                    peak.fwhm,
                    peak.area,
                    fg
                )
            )
        }
        sb.appendLine("==================================================")
        return sb.toString()
    }

    override fun generateCsvExport(spectrum: Spectrum): String {
        val sb = StringBuilder()
        sb.appendLine("# SpectroIR Export: ${spectrum.name}")
        sb.appendLine("# Wavenumber (${spectrum.xUnit}),Signal (${spectrum.yUnit})")
        for (pt in spectrum.points) {
            sb.appendLine("${"%.4f".format(Locale.US, pt.wavenumber)},${"%.4f".format(Locale.US, pt.signalValue)}")
        }
        return sb.toString()
    }
}
