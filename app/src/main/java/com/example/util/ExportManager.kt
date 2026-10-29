package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.domain.spectroscopy.LaboratoryAnalysisReportData
import com.example.domain.spectroscopy.LaboratoryReportService
import java.io.File
import java.io.FileOutputStream

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    PDF("PDF Scientific Report", "pdf", "application/pdf"),
    CSV("CSV Numerical Data", "csv", "text/csv"),
    JSON("JSON Analysis Structure", "json", "application/json"),
    IMAGE("High-Resolution PNG", "png", "image/png")
}

data class ExportResult(
    val file: File?,
    val format: ExportFormat,
    val success: Boolean,
    val message: String
)

object ExportManager {

    fun exportReport(
        context: Context,
        data: LaboratoryAnalysisReportData,
        format: ExportFormat,
        showPeakAnnotations: Boolean = true,
        publicationWhite: Boolean = false
    ): ExportResult {
        return try {
            val exportsDir = File(context.cacheDir, "shared_reports").apply { mkdirs() }
            val cleanName = data.effectiveSampleName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val baseFileName = "SpectroIR_${cleanName}_${data.reportId}"

            val file = when (format) {
                ExportFormat.PDF -> {
                    LaboratoryReportService.generatePdfReport(data, context)
                }

                ExportFormat.CSV -> {
                    val csvContent = LaboratoryReportService.generateCsv(data)
                    val outFile = File(exportsDir, "$baseFileName.csv")
                    outFile.writeText(csvContent)
                    outFile
                }

                ExportFormat.JSON -> {
                    val jsonContent = LaboratoryReportService.generateJson(data)
                    val outFile = File(exportsDir, "$baseFileName.json")
                    outFile.writeText(jsonContent)
                    outFile
                }

                ExportFormat.IMAGE -> {
                    val bitmap = LaboratoryReportService.generateSpectrumBitmap(
                        data = data,
                        width = 1920,
                        height = 1080,
                        showPeakAnnotations = showPeakAnnotations,
                        publicationWhite = publicationWhite
                    )
                    val outFile = File(exportsDir, "$baseFileName.png")
                    FileOutputStream(outFile).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    }
                    outFile
                }
            }

            ExportResult(
                file = file,
                format = format,
                success = true,
                message = "Exported ${format.label} (${file.length() / 1024} KB)"
            )
        } catch (e: Exception) {
            ExportResult(
                file = null,
                format = format,
                success = false,
                message = "Export failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    /**
     * Standard Android Share Sheet using FileProvider and Intent.ACTION_SEND
     */
    fun shareExportedFile(context: Context, file: File, format: ExportFormat, sampleName: String) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "SpectroIR Analysis Report: $sampleName")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Attached is the SpectroIR spectroscopic analysis report for sample '$sampleName' ($format)."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share ${format.label}")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}
