package com.example.domain.spectroscopy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class PdfPageInfo(
    val pageIndex: Int,
    val totalPages: Int,
    val previewBitmap: Bitmap,
    val hasLikelySpectrum: Boolean
)

interface PdfSpectrumExtractor {
    /**
     * Inspect PDF report: Renders pages and checks for presence of graph curves.
     */
    suspend fun inspectPdfPages(context: Context, pdfUri: Uri): Result<List<PdfPageInfo>>

    /**
     * Render high-resolution bitmap of chosen PDF page for cropping and digitization.
     */
    suspend fun renderPageHighRes(context: Context, pdfUri: Uri, pageIndex: Int): Result<Bitmap>
}

class StandardPdfSpectrumExtractor : PdfSpectrumExtractor {

    override suspend fun inspectPdfPages(context: Context, pdfUri: Uri): Result<List<PdfPageInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val tempFile = copyUriToTempFile(context, pdfUri, "inspect.pdf")
                val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val pageCount = renderer.pageCount
                val pages = mutableListOf<PdfPageInfo>()

                for (i in 0 until pageCount.coerceAtMost(10)) {
                    val page = renderer.openPage(i)
                    val width = page.width
                    val height = page.height

                    // Render preview (scaled down for memory safety)
                    val scale = 500f / maxOf(width, height)
                    val previewWidth = (width * scale).toInt().coerceAtLeast(100)
                    val previewHeight = (height * scale).toInt().coerceAtLeast(100)
                    val bitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    // Heuristic check: does page have wide dark regions/curves consistent with a plot?
                    val hasGraph = evaluateIfPageHasGraph(bitmap)

                    pages.add(
                        PdfPageInfo(
                            pageIndex = i,
                            totalPages = pageCount,
                            previewBitmap = bitmap,
                            hasLikelySpectrum = hasGraph
                        )
                    )
                }

                renderer.close()
                pfd.close()
                tempFile.delete()

                Result.success(pages)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun renderPageHighRes(context: Context, pdfUri: Uri, pageIndex: Int): Result<Bitmap> =
        withContext(Dispatchers.IO) {
            try {
                val tempFile = copyUriToTempFile(context, pdfUri, "render_page.pdf")
                val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)

                val validIdx = pageIndex.coerceIn(0, renderer.pageCount - 1)
                val page = renderer.openPage(validIdx)
                val width = page.width
                val height = page.height

                // Render at 2x resolution (up to 1800px) for crisp graph extraction
                val scale = minOf(1800f / maxOf(width, height), 2.5f)
                val renderW = (width * scale).toInt()
                val renderH = (height * scale).toInt()
                val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()
                tempFile.delete()

                Result.success(bitmap)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun evaluateIfPageHasGraph(bitmap: Bitmap): Boolean {
        var darkPixelCount = 0
        val total = (bitmap.width * bitmap.height)
        val step = 4
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val p = bitmap.getPixel(x, y)
                val lum = (android.graphics.Color.red(p) * 299 +
                        android.graphics.Color.green(p) * 587 +
                        android.graphics.Color.blue(p) * 114) / 1000
                if (lum < 110) darkPixelCount++
            }
        }
        val sampledRatio = darkPixelCount.toDouble() / (total / (step * step))
        // Typical spectroscopy reports have between 2% and 25% curve/ink density
        return sampledRatio in 0.02..0.30
    }

    private fun copyUriToTempFile(context: Context, uri: Uri, tempName: String): File {
        val tempFile = File(context.cacheDir, tempName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }
}
