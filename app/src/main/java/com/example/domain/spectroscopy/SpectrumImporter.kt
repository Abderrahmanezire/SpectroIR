package com.example.domain.spectroscopy

import android.util.Xml
import com.example.domain.model.SignalUnit
import com.example.domain.model.Spectrum
import com.example.domain.model.SpectrumPoint
import com.example.domain.model.SpectrumSourceType
import com.example.domain.model.SpectrumType
import com.example.domain.model.WavenumberUnit
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

enum class ImportXUnit(val symbol: String, val displayName: String) {
    WAVENUMBER_CM_1("cm⁻¹", "Wavenumber (cm⁻¹)"),
    WAVELENGTH_UM("µm", "Wavelength (µm)"),
    WAVELENGTH_NM("nm", "Wavelength (nm)"),
    WAVELENGTH_CM("cm", "Wavelength (cm)")
}

data class ParsedTable(
    val fileName: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val metadata: Map<String, String>,
    val detectedDelimiter: Char,
    val detectedDecimalSeparator: Char
)

data class ColumnMapping(
    val xColumnIndex: Int,
    val yColumnIndex: Int,
    val xUnit: ImportXUnit = ImportXUnit.WAVENUMBER_CM_1,
    val spectrumType: SpectrumType = SpectrumType.Transmittance,
    val decimalSeparator: Char = '.'
)

data class ImportAnalysisResult(
    val table: ParsedTable,
    val suggestedMapping: ColumnMapping,
    val isConfidenceHigh: Boolean,
    val ambiguityReason: String? = null,
    val candidateXIndices: List<Int> = emptyList(),
    val candidateYIndices: List<Int> = emptyList(),
    val parsedSpectrum: Spectrum? = null,
    val validationErrors: List<String> = emptyList()
)

interface SpectrumImporter {
    fun analyzeStream(
        inputStream: InputStream,
        fileName: String,
        sourceType: SpectrumSourceType = SpectrumSourceType.FILE_IMPORT
    ): Result<ImportAnalysisResult>

    fun analyzeText(
        content: String,
        fileName: String,
        sourceType: SpectrumSourceType = SpectrumSourceType.FILE_IMPORT
    ): Result<ImportAnalysisResult>

    fun buildSpectrumFromMapping(
        table: ParsedTable,
        mapping: ColumnMapping,
        sourceType: SpectrumSourceType = SpectrumSourceType.FILE_IMPORT
    ): Result<Spectrum>

    fun parseStream(
        inputStream: InputStream,
        fileName: String,
        sourceType: SpectrumSourceType = SpectrumSourceType.FILE_IMPORT
    ): Result<Spectrum>

    fun parseText(
        content: String,
        fileName: String,
        sourceType: SpectrumSourceType = SpectrumSourceType.FILE_IMPORT
    ): Result<Spectrum>
}

class StandardSpectrumImporter : SpectrumImporter {

    override fun parseStream(
        inputStream: InputStream,
        fileName: String,
        sourceType: SpectrumSourceType
    ): Result<Spectrum> {
        val analysis = analyzeStream(inputStream, fileName, sourceType)
        return analysis.mapCatching { res ->
            res.parsedSpectrum ?: buildSpectrumFromMapping(res.table, res.suggestedMapping, sourceType).getOrThrow()
        }
    }

    override fun parseText(
        content: String,
        fileName: String,
        sourceType: SpectrumSourceType
    ): Result<Spectrum> {
        val analysis = analyzeText(content, fileName, sourceType)
        return analysis.mapCatching { res ->
            res.parsedSpectrum ?: buildSpectrumFromMapping(res.table, res.suggestedMapping, sourceType).getOrThrow()
        }
    }

    override fun analyzeStream(
        inputStream: InputStream,
        fileName: String,
        sourceType: SpectrumSourceType
    ): Result<ImportAnalysisResult> {
        return try {
            val bytes = inputStream.readBytes()
            val lowerName = fileName.lowercase(Locale.US)

            val table = if (lowerName.endsWith(".xlsx")) {
                parseXlsxBytes(bytes, fileName)
            } else {
                val textContent = String(bytes, Charsets.UTF_8)
                parseDelimitedText(textContent, fileName)
            }

            analyzeParsedTable(table, sourceType)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun analyzeText(
        content: String,
        fileName: String,
        sourceType: SpectrumSourceType
    ): Result<ImportAnalysisResult> {
        return try {
            val table = parseDelimitedText(content, fileName)
            analyzeParsedTable(table, sourceType)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseDelimitedText(content: String, fileName: String): ParsedTable {
        val lines = content.lines()
        val metadata = mutableMapOf<String, String>()
        val dataLines = mutableListOf<String>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            // Comment lines or metadata lines
            if (line.startsWith("#") || line.startsWith("%") || line.startsWith(";") ||
                line.startsWith("*") || line.startsWith("//") || line.startsWith("[")) {
                val comment = line.trimStart('#', '%', ';', '*', '/', '[', ']').trim()
                val colonIdx = comment.indexOf(':')
                if (colonIdx != -1) {
                    val k = comment.substring(0, colonIdx).trim()
                    val v = comment.substring(colonIdx + 1).trim()
                    if (k.isNotEmpty()) metadata[k] = v
                }
                continue
            }

            // Key: value line before any tabular numbers
            if (dataLines.isEmpty() && line.contains(":") && !line.any { it.isDigit() && line.indexOf(it) < line.indexOf(':') }) {
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    metadata[parts[0].trim()] = parts[1].trim()
                    continue
                }
            }

            dataLines.add(line)
        }

        if (dataLines.isEmpty()) {
            throw IllegalArgumentException("File contains no valid numerical spectrum data rows.")
        }

        // Detect delimiter using candidate characters: comma, semicolon, tab, pipe, whitespace
        val delimiter = detectDelimiter(dataLines)
        val decimalSeparator = detectDecimalSeparator(dataLines, delimiter)

        val splitRows = mutableListOf<List<String>>()
        for (line in dataLines) {
            val tokens = splitLine(line, delimiter)
            if (tokens.isNotEmpty()) {
                splitRows.add(tokens)
            }
        }

        if (splitRows.isEmpty()) {
            throw IllegalArgumentException("Could not extract tabular columns from file.")
        }

        // Determine if first row is a header
        val firstRow = splitRows.first()
        val hasHeader = isHeaderRow(firstRow)

        val headers: List<String>
        val rows: List<List<String>>

        if (hasHeader) {
            headers = firstRow.mapIndexed { idx, col ->
                val cleaned = col.trim().trim('"', '\'')
                if (cleaned.isNotBlank()) cleaned else "Column ${idx + 1}"
            }
            rows = splitRows.drop(1)
        } else {
            headers = firstRow.indices.map { "Column ${it + 1}" }
            rows = splitRows
        }

        return ParsedTable(
            fileName = fileName,
            headers = headers,
            rows = rows,
            metadata = metadata,
            detectedDelimiter = delimiter,
            detectedDecimalSeparator = decimalSeparator
        )
    }

    private fun detectDelimiter(lines: List<String>): Char {
        val sample = lines.take(20)
        val candidates = listOf('\t', ';', ',', '|')

        // Count occurrences per line for each candidate
        val candidatesWithConsistency = candidates.map { delim ->
            val counts = sample.map { line -> line.count { it == delim } }
            val linesWithDelim = counts.count { it > 0 }
            val nonZeroCounts = counts.filter { it > 0 }
            val minCount = nonZeroCounts.minOrNull() ?: 0
            val maxCount = nonZeroCounts.maxOrNull() ?: 0
            // Consistency score: every single line has the exact same non-zero count
            val isConsistent = linesWithDelim == sample.size && minCount == maxCount && minCount > 0
            Triple(delim, isConsistent, linesWithDelim)
        }

        // Priority 1: Perfectly consistent candidate across all sampled lines (including header)
        val consistent = candidatesWithConsistency.firstOrNull { it.second }
        if (consistent != null) {
            return consistent.first
        }

        // Priority 2: Candidates that appear in almost all lines (>= 80%)
        // Priority order: \t, ;, |, , (tab and semicolon are almost exclusively column delimiters)
        for (delim in listOf('\t', ';', '|')) {
            val countInLines = sample.count { it.contains(delim) }
            if (countInLines >= (sample.size * 0.8).toInt().coerceAtLeast(1)) {
                return delim
            }
        }

        val commaInLines = sample.count { it.contains(',') }
        if (commaInLines >= (sample.size * 0.8).toInt().coerceAtLeast(1)) {
            return ','
        }

        // Check whitespace / multiple spaces
        val spaceCounts = sample.count { it.contains(" ") }
        if (spaceCounts >= sample.size / 2) {
            return ' '
        }

        return ','
    }

    private fun detectDecimalSeparator(lines: List<String>, delimiter: Char): Char {
        // If delimiter is semicolon, tab, pipe, or space, check for European comma decimals
        val sample = lines.take(15).joinToString(" ")
        if (delimiter == ';' || delimiter == '\t' || delimiter == '|' || delimiter == ' ') {
            val commaDecCount = Regex("""\d+,\d+""").findAll(sample).count()
            val dotDecCount = Regex("""\d+\.\d+""").findAll(sample).count()
            if (commaDecCount > dotDecCount) return ','
        }
        return '.'
    }

    private fun splitLine(line: String, delimiter: Char): List<String> {
        return if (delimiter == ' ') {
            line.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        } else {
            line.split(delimiter).map { it.trim().trim('"', '\'') }
        }
    }

    private fun isHeaderRow(row: List<String>): Boolean {
        // If row has words like Wavenumber, Abs, Transmittance, or non-numeric tokens
        val alphaCount = row.count { token -> token.any { it.isLetter() } }
        return alphaCount >= 1
    }

    private fun parseXlsxBytes(bytes: ByteArray, fileName: String): ParsedTable {
        val sharedStrings = mutableListOf<String>()
        val sheetRows = mutableListOf<List<String>>()

        // Pass 1: extract shared strings
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name.equals("xl/sharedStrings.xml", ignoreCase = true)) {
                    val parser = Xml.newPullParser()
                    parser.setInput(zis, "UTF-8")
                    var eventType = parser.eventType
                    var insideT = false
                    val curStr = StringBuilder()

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (parser.name.equals("t", ignoreCase = true)) {
                                    insideT = true
                                    curStr.clear()
                                }
                            }
                            XmlPullParser.TEXT -> {
                                if (insideT) {
                                    curStr.append(parser.text)
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (parser.name.equals("t", ignoreCase = true)) {
                                    insideT = false
                                    sharedStrings.add(curStr.toString())
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                }
                entry = zis.nextEntry
            }
        }

        // Pass 2: read first sheet
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name.startsWith("xl/worksheets/sheet", ignoreCase = true) && entry.name.endsWith(".xml", ignoreCase = true)) {
                    val parser = Xml.newPullParser()
                    parser.setInput(zis, "UTF-8")
                    var eventType = parser.eventType
                    var curRow = mutableListOf<String>()
                    var curCellType = ""
                    var curVal = StringBuilder()
                    var insideV = false

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                when (parser.name.lowercase(Locale.US)) {
                                    "row" -> {
                                        curRow = mutableListOf()
                                    }
                                    "c" -> {
                                        curCellType = parser.getAttributeValue(null, "t") ?: ""
                                        curVal.clear()
                                    }
                                    "v" -> {
                                        insideV = true
                                    }
                                }
                            }
                            XmlPullParser.TEXT -> {
                                if (insideV) {
                                    curVal.append(parser.text)
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                when (parser.name.lowercase(Locale.US)) {
                                    "v" -> {
                                        insideV = false
                                        val rawVal = curVal.toString()
                                        if (curCellType == "s") {
                                            val idx = rawVal.toIntOrNull()
                                            val str = if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else rawVal
                                            curRow.add(str)
                                        } else {
                                            curRow.add(rawVal)
                                        }
                                    }
                                    "row" -> {
                                        if (curRow.isNotEmpty()) {
                                            sheetRows.add(curRow)
                                        }
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    break // First sheet parsed
                }
                entry = zis.nextEntry
            }
        }

        if (sheetRows.isEmpty()) {
            throw IllegalArgumentException("Excel sheet contains no data.")
        }

        val first = sheetRows.first()
        val hasHdr = isHeaderRow(first)
        val headers = if (hasHdr) first else first.indices.map { "Column ${it + 1}" }
        val rows = if (hasHdr) sheetRows.drop(1) else sheetRows

        return ParsedTable(
            fileName = fileName,
            headers = headers,
            rows = rows,
            metadata = mapOf("Format" to "Microsoft Excel (.xlsx)"),
            detectedDelimiter = ',',
            detectedDecimalSeparator = '.'
        )
    }

    private fun analyzeParsedTable(
        table: ParsedTable,
        sourceType: SpectrumSourceType
    ): Result<ImportAnalysisResult> {
        val headers = table.headers
        val rows = table.rows
        if (rows.isEmpty()) {
            return Result.failure(IllegalArgumentException("File contains zero data rows."))
        }

        val candidateX = mutableListOf<Int>()
        val candidateY = mutableListOf<Int>()

        // Check header names against spectroscopy patterns
        for (i in headers.indices) {
            val h = headers[i].lowercase(Locale.US)
            if (isXColumnName(h)) {
                candidateX.add(i)
            }
            if (isYColumnName(h)) {
                candidateY.add(i)
            }
        }

        // If no matches by header, check data ranges in first 20 rows
        val colCount = headers.size
        if (candidateX.isEmpty() || candidateY.isEmpty()) {
            val sampleRows = rows.take(20)
            for (col in 0 until colCount) {
                val vals = sampleRows.mapNotNull { r ->
                    if (col < r.size) parseNumber(r[col], table.detectedDecimalSeparator) else null
                }
                if (vals.size >= 5) {
                    val min = vals.minOrNull() ?: 0.0
                    val max = vals.maxOrNull() ?: 0.0

                    // Typical mid-IR wavenumber: 400 - 4500 cm⁻¹ or wavelength 2.5 - 25 µm
                    if (candidateX.isEmpty() && ((max > 300.0 && max <= 15000.0) || (min >= 1.0 && max <= 50.0))) {
                        candidateX.add(col)
                    }
                    if (candidateY.isEmpty() && col !in candidateX) {
                        candidateY.add(col)
                    }
                }
            }
        }

        // Default fallbacks
        val finalX = candidateX.firstOrNull() ?: 0
        val finalY = candidateY.firstOrNull { it != finalX } ?: if (colCount > 1) 1 else 0

        // Detect X Unit
        val xHeader = headers.getOrNull(finalX)?.lowercase(Locale.US) ?: ""
        var unitIsAmbiguous = false
        val detectedXUnit = when {
            // Priority 1: Explicit Wavenumber indicators
            xHeader.contains("cm-1") || xHeader.contains("cm⁻¹") || xHeader.contains("cm^-1") ||
            xHeader.contains("1/cm") || xHeader.contains("wavenumber") || xHeader.contains("wave number") ||
            xHeader.contains("wave_number") || xHeader.contains("kayser") || xHeader.contains("wn") -> ImportXUnit.WAVENUMBER_CM_1

            // Priority 2: Explicit Nanometers
            xHeader.contains("nm") || xHeader.contains("nanometer") || xHeader.contains("nanometre") -> ImportXUnit.WAVELENGTH_NM

            // Priority 3: Explicit Micrometers
            xHeader.contains("µm") || xHeader.contains("micron") || xHeader.contains("micrometer") ||
            xHeader.contains("(um)") || xHeader.contains("(µm)") || xHeader.contains("[um]") || xHeader.contains("[µm]") ||
            Regex("""\b(um|µm)\b""").containsMatchIn(xHeader) -> ImportXUnit.WAVELENGTH_UM

            // Priority 4: Labeled as wavelength but without unit symbol
            xHeader.contains("wavelength") || xHeader.contains("wave length") || xHeader.contains("lambda") -> {
                val sampleX = rows.take(15).mapNotNull { r ->
                    if (finalX < r.size) parseNumber(r[finalX], table.detectedDecimalSeparator) else null
                }
                val avg = if (sampleX.isNotEmpty()) sampleX.average() else 10.0
                if (avg in 150.0..50000.0) {
                    ImportXUnit.WAVELENGTH_NM
                } else {
                    ImportXUnit.WAVELENGTH_UM
                }
            }

            else -> {
                val sampleX = rows.take(15).mapNotNull { r ->
                    if (finalX < r.size) parseNumber(r[finalX], table.detectedDecimalSeparator) else null
                }
                val avg = if (sampleX.isNotEmpty()) sampleX.average() else 2000.0
                if (avg in 1.0..50.0) {
                    unitIsAmbiguous = true
                    ImportXUnit.WAVELENGTH_UM
                } else {
                    ImportXUnit.WAVENUMBER_CM_1
                }
            }
        }

        // Detect Spectrum Type
        val yHeader = headers.getOrNull(finalY)?.lowercase(Locale.US) ?: ""
        val detectedType = when {
            yHeader.contains("abs") || yHeader.contains("au") || yHeader.contains("optical") || yHeader.contains("od") -> SpectrumType.Absorbance
            yHeader.contains("trans") || yHeader.contains("%t") || yHeader.contains("t%") -> SpectrumType.Transmittance
            yHeader.contains("int") || yHeader.contains("signal") || yHeader.contains("count") -> SpectrumType.Intensity
            else -> {
                // Infer by numerical range
                val sampleY = rows.take(20).mapNotNull { r ->
                    if (finalY < r.size) parseNumber(r[finalY], table.detectedDecimalSeparator) else null
                }
                val maxY = sampleY.maxOrNull() ?: 100.0
                val minY = sampleY.minOrNull() ?: 0.0
                if (maxY <= 4.0 && minY >= 0.0) SpectrumType.Absorbance else SpectrumType.Transmittance
            }
        }

        val suggestedMapping = ColumnMapping(
            xColumnIndex = finalX,
            yColumnIndex = finalY,
            xUnit = detectedXUnit,
            spectrumType = detectedType,
            decimalSeparator = table.detectedDecimalSeparator
        )

        val isHighConfidence = (candidateX.size == 1 && candidateY.size <= 2 && colCount <= 3 && !unitIsAmbiguous)

        val ambiguityReason = when {
            candidateX.isEmpty() -> "Could not identify an X-axis column. Please select the wavenumber/wavelength column."
            candidateY.isEmpty() -> "Could not identify a Y-axis signal column. Please select the absorbance or transmittance column."
            candidateY.size > 1 -> "Multiple potential Y-axis columns were detected (${candidateY.map { headers[it] }.joinToString()}). Please select the intended signal column."
            colCount > 3 -> "File contains $colCount columns. Please verify column assignments."
            unitIsAmbiguous -> "X-axis units could not be determined with certainty. Please verify units (cm⁻¹ vs µm)."
            else -> null
        }

        // Attempt build
        val builtResult = buildSpectrumFromMapping(table, suggestedMapping, sourceType)
        val validationErrors = mutableListOf<String>()
        if (builtResult.isFailure) {
            validationErrors.add(builtResult.exceptionOrNull()?.message ?: "Parsing error")
        }

        return Result.success(
            ImportAnalysisResult(
                table = table,
                suggestedMapping = suggestedMapping,
                isConfidenceHigh = isHighConfidence && builtResult.isSuccess,
                ambiguityReason = ambiguityReason,
                candidateXIndices = if (candidateX.isNotEmpty()) candidateX else listOf(0),
                candidateYIndices = if (candidateY.isNotEmpty()) candidateY else listOf(1.coerceAtMost(colCount - 1)),
                parsedSpectrum = builtResult.getOrNull(),
                validationErrors = validationErrors
            )
        )
    }

    override fun buildSpectrumFromMapping(
        table: ParsedTable,
        mapping: ColumnMapping,
        sourceType: SpectrumSourceType
    ): Result<Spectrum> {
        return try {
            val rows = table.rows
            if (rows.isEmpty()) {
                return Result.failure(IllegalArgumentException("Table has no rows to import."))
            }

            val rawPoints = mutableListOf<SpectrumPoint>()
            var invalidRowCount = 0

            for (row in rows) {
                if (mapping.xColumnIndex >= row.size || mapping.yColumnIndex >= row.size) {
                    invalidRowCount++
                    continue
                }

                val rawX = row[mapping.xColumnIndex]
                val rawY = row[mapping.yColumnIndex]

                val xVal = parseNumber(rawX, mapping.decimalSeparator)
                val yVal = parseNumber(rawY, mapping.decimalSeparator)

                if (xVal == null || yVal == null || xVal.isNaN() || yVal.isNaN() || xVal.isInfinite() || yVal.isInfinite()) {
                    invalidRowCount++
                    continue
                }

                // Unit conversion to internal cm⁻¹
                val wavenumber = when (mapping.xUnit) {
                    ImportXUnit.WAVENUMBER_CM_1 -> xVal
                    ImportXUnit.WAVELENGTH_UM -> if (xVal > 0) 10000.0 / xVal else null
                    ImportXUnit.WAVELENGTH_NM -> if (xVal > 0) 10000000.0 / xVal else null
                    ImportXUnit.WAVELENGTH_CM -> if (xVal > 0) 1.0 / xVal else null
                }

                if (wavenumber != null && wavenumber in 1.0..100000.0) {
                    rawPoints.add(SpectrumPoint(wavenumber = wavenumber, signalValue = yVal))
                } else {
                    invalidRowCount++
                }
            }

            if (rawPoints.size < 5) {
                return Result.failure(
                    IllegalArgumentException(
                        "File contains insufficient valid numerical points (found ${rawPoints.size}, minimum 5 required)."
                    )
                )
            }

            // Deduplicate duplicate X wavenumbers by averaging signals
            val groupedByWn = rawPoints.groupBy { "%.3f".format(Locale.US, it.wavenumber) }
            val cleanedPoints = groupedByWn.map { (_, pts) ->
                SpectrumPoint(
                    wavenumber = pts.first().wavenumber,
                    signalValue = pts.map { it.signalValue }.average()
                )
            }.sortedBy { it.wavenumber }

            val yUnit = when (mapping.spectrumType) {
                SpectrumType.Transmittance -> SignalUnit.TRANSMITTANCE_PERCENT.symbol
                SpectrumType.Absorbance -> SignalUnit.ABSORBANCE.symbol
                else -> SignalUnit.ARBITRARY_INTENSITY.symbol
            }

            val meta = table.metadata.toMutableMap()
            meta["Imported File"] = table.fileName
            meta["Original Points"] = rawPoints.size.toString()
            meta["Cleaned Points"] = cleanedPoints.size.toString()
            meta["X Column"] = table.headers.getOrElse(mapping.xColumnIndex) { "Col ${mapping.xColumnIndex + 1}" }
            meta["Y Column"] = table.headers.getOrElse(mapping.yColumnIndex) { "Col ${mapping.yColumnIndex + 1}" }
            meta["Original X Unit"] = mapping.xUnit.displayName
            if (mapping.xUnit != ImportXUnit.WAVENUMBER_CM_1) {
                meta["Conversion"] = "Converted from ${mapping.xUnit.symbol} to cm⁻¹ (ν = 1/λ)"
            }

            val name = table.fileName
                .removeSuffix(".csv").removeSuffix(".txt")
                .removeSuffix(".tsv").removeSuffix(".dat")
                .removeSuffix(".xlsx").removeSuffix(".asc")

            val spectrum = Spectrum(
                id = UUID.randomUUID().toString(),
                name = name,
                sourceType = sourceType,
                xUnit = WavenumberUnit.CM_MINUS_1.symbol,
                yUnit = yUnit,
                spectrumType = mapping.spectrumType,
                points = cleanedPoints,
                rawPoints = rawPoints,
                metadata = meta,
                createdAt = System.currentTimeMillis()
            )

            Result.success(spectrum)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isXColumnName(name: String): Boolean {
        val lower = name.lowercase(Locale.US)
        return lower.contains("wavenumber") ||
               lower.contains("wave number") ||
               lower.contains("wave_number") ||
               lower.contains("cm-1") ||
               lower.contains("cm⁻¹") ||
               lower.contains("cm^-1") ||
               lower.contains("1/cm") ||
               lower.contains("kayser") ||
               lower.contains("wavelength") ||
               lower.contains("wave length") ||
               lower.contains("wave_length") ||
               lower.contains("lambda") ||
               lower.contains("µm") ||
               lower.contains("micron") ||
               lower.contains("(um)") || lower.contains("(µm)") || lower.contains("(nm)") ||
               lower.contains("[um]") || lower.contains("[µm]") || lower.contains("[nm]") ||
               Regex("""(^|[^a-z])(wn|nm|um)([^a-z]|$)""").containsMatchIn(lower)
    }

    private fun isYColumnName(name: String): Boolean {
        val lower = name.lowercase(Locale.US)
        return lower.contains("transmittance") ||
               lower.contains("transmission") ||
               lower.contains("%t") ||
               lower.contains("t%") ||
               lower.contains("absorbance") ||
               lower.contains("absorb") ||
               lower.contains("optical density") ||
               lower.contains("extinction") ||
               lower.contains("intensity") ||
               lower.contains("signal") ||
               lower.contains("counts") ||
               lower.contains("reflectance") ||
               lower.contains("%r") ||
               lower.contains("atr") ||
               Regex("""(^|[^a-z])(abs|au|od)([^a-z]|$)""").containsMatchIn(lower)
    }

    private fun parseNumber(token: String, decimalSep: Char): Double? {
        val trimmed = token.trim().trim('"', '\'')
        if (trimmed.isEmpty()) return null
        val normalized = if (decimalSep == ',') {
            trimmed.replace(".", "").replace(',', '.')
        } else {
            trimmed.replace(",", "")
        }
        return normalized.toDoubleOrNull()
    }
}
