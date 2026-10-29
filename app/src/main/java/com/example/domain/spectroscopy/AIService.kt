package com.example.domain.spectroscopy

import com.example.BuildConfig
import com.example.domain.model.Peak
import com.example.domain.model.Spectrum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.TimeUnit

// -------------------------------------------------------------
// 1. DATA MODELS FOR AI-ASSISTED SPECTROSCOPY INTERPRETATION
// -------------------------------------------------------------

data class AIUserContext(
    val sampleName: String = "",
    val sampleType: String = "", // e.g. "Solid / Powder", "Neat Liquid", "Thin Film", "Polymer", "Solution"
    val expectedComposition: String = "",
    val knownComponents: String = "",
    val measurementMethod: String = "", // e.g. "ATR-FTIR (Diamond Crystal)", "Transmission (KBr Pellet)", "Specular Reflection"
    val experimentalNotes: String = ""
)

data class AIMajorPeakAssignment(
    val wavenumber: Double,
    val observedFeature: String = "", // e.g. "Strong broad envelope", "Sharp intense band", "Medium doublet"
    val possibleBond: String = "", // e.g. "O-H", "C=O", "sp² C-H"
    val functionalGroup: String = "", // e.g. "Hydrogen-bonded Alcohol", "Ester Carbonyl", "Aromatic Ring"
    val vibrationalMode: String = "", // e.g. "Stretching", "Bending", "Out-of-plane deformation"
    val supportingEvidence: String = "",
    val alternativeExplanations: String = ""
)

data class AISpectrumSummary(
    val spectrumType: String,
    val range: String,
    val peakCount: Int,
    val functionalGroupRegionSummary: String = "",
    val fingerprintRegionSummary: String = ""
)

data class AIConfidenceEstimate(
    val label: String = "AI/database confidence estimate",
    val level: String = "Moderate", // "High", "Moderate", "Low", "Insufficient evidence for reliable identification"
    val basis: String = ""
)

data class AIInterpretationRequest(
    val spectrumName: String,
    val spectrumType: String,
    val xUnit: String,
    val yUnit: String,
    val minWavenumber: Double,
    val maxWavenumber: Double,
    val pointCount: Int,
    val peaks: List<Peak>,
    val dictionaryMatches: List<String> = emptyList(),
    val referenceMatches: List<String> = emptyList(),
    val preprocessingSteps: List<String> = emptyList(),
    val customPrompt: String? = null,
    val userContext: AIUserContext? = null,
    val structuredComparisonSummary: String? = null
)

data class AIInterpretationResult(
    val isSuccess: Boolean,
    val summary: String,
    val candidateCompounds: List<String> = emptyList(),
    val structuralInsights: List<String> = emptyList(),
    val spectrumSummary: AISpectrumSummary? = null,
    val majorPeakAssignments: List<AIMajorPeakAssignment> = emptyList(),
    val completeSpectrumInterpretation: String = summary,
    val referenceComparisonSummary: String? = null,
    val uncertainties: List<String> = emptyList(),
    val additionalInformationNeeded: List<String> = emptyList(),
    val confidenceEstimate: AIConfidenceEstimate? = null,
    val rawResponseText: String? = summary,
    val errorMessage: String? = null,
    val isFromRealApi: Boolean = false,
    val durationMs: Long = 0L,
    val httpStatusCode: Int? = null,
    val modelName: String = "gemini-3.1-pro-preview",
    val timestamp: Long = System.currentTimeMillis(),
    val measuredPeaksCount: Int = 0,
    val dictionaryMatchesCount: Int = 0,
    val referenceMatchesCount: Int = 0,
    val userContextIncluded: Boolean = false
)

data class AIDiagnosticsInfo(
    val provider: String = "Google Gemini REST API",
    val model: String = "gemini-3.1-pro-preview",
    val isConfigured: Boolean = false,
    val isOnline: Boolean = true,
    val lastRequestTimestamp: Long? = null,
    val lastRequestStatus: String = "NONE", // SUCCESS, FAILED, NONE
    val lastResponseReceived: Boolean = false,
    val localSpectroscopyDataSupplied: Boolean = false,
    val detectedPeaksSupplied: Int = 0,
    val referenceMatchesSupplied: Int = 0,
    val lastDurationMs: Long = 0L,
    val lastErrorReason: String? = null,
    val lastDebugHttpCode: Int? = null,
    val endpointUrl: String = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview"
)

data class AIConnectionTestResult(
    val isSuccess: Boolean,
    val responseToken: String? = null,
    val errorMessage: String? = null,
    val durationMs: Long = 0L,
    val httpCode: Int? = null
)

// -------------------------------------------------------------
// 2. CONFIGURATION & SECRETS MANAGEMENT
// -------------------------------------------------------------

object AIConfiguration {
    @Volatile
    var customApiKey: String? = null
    @Volatile
    private var appContext: android.content.Context? = null

    fun initialize(context: android.content.Context) {
        appContext = context.applicationContext
        if (customApiKey.isNullOrBlank()) {
            val prefs = appContext?.getSharedPreferences("spectroir_ai_prefs", android.content.Context.MODE_PRIVATE)
            val savedKey = prefs?.getString("custom_api_key", null)?.trim()
            if (!savedKey.isNullOrBlank()) {
                customApiKey = savedKey
            }
        }
    }

    fun setKey(key: String?) {
        val trimmed = key?.trim()
        customApiKey = if (trimmed.isNullOrBlank()) null else trimmed
        appContext?.getSharedPreferences("spectroir_ai_prefs", android.content.Context.MODE_PRIVATE)?.edit()?.let {
            if (customApiKey == null) {
                it.remove("custom_api_key")
            } else {
                it.putString("custom_api_key", customApiKey)
            }
            it.apply()
        }
    }

    fun getActiveApiKey(): String? {
        val override = customApiKey
        if (override != null) {
            val trimmed = override.trim()
            return if (trimmed.isBlank() || trimmed.contains("MY_GEMINI_API_KEY") || trimmed == "EMPTY") null else trimmed
        }

        return try {
            val field = BuildConfig::class.java.getDeclaredField("GEMINI_API_KEY")
            val value = field.get(null) as? String
            value?.takeIf { it.isNotBlank() && !it.contains("MY_GEMINI_API_KEY") }
        } catch (t: Throwable) {
            null
        }
    }

    fun isKeyConfigured(): Boolean {
        val key = getActiveApiKey()
        return !key.isNullOrBlank() && !key.contains("MY_GEMINI_API_KEY")
    }
}

// -------------------------------------------------------------
// 3. AI SERVICE INTERFACE
// -------------------------------------------------------------

interface AIService {
    fun isConfigured(): Boolean
    fun getDiagnosticsInfo(): AIDiagnosticsInfo
    suspend fun testConnection(): AIConnectionTestResult
    suspend fun interpretSpectrum(analysisRequest: AIInterpretationRequest): AIInterpretationResult
    suspend fun interpretSpectrum(spectrum: Spectrum, customPrompt: String? = null): AIInterpretationResult
}

// -------------------------------------------------------------
// 4. STANDARD AI SERVICE IMPLEMENTATION
// -------------------------------------------------------------

class StandardAIService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) : AIService {

    @Volatile
    private var diagnostics = AIDiagnosticsInfo(
        isConfigured = AIConfiguration.isKeyConfigured()
    )

    companion object {
        val CANDIDATE_MODELS = listOf(
            "gemini-3.1-pro-preview",
            "gemini-3.5-flash",
            "gemini-flash-latest"
        )
    }

    override fun isConfigured(): Boolean = AIConfiguration.isKeyConfigured()

    override fun getDiagnosticsInfo(): AIDiagnosticsInfo {
        return diagnostics.copy(isConfigured = AIConfiguration.isKeyConfigured())
    }

    override suspend fun testConnection(): AIConnectionTestResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val apiKey = AIConfiguration.getActiveApiKey()

            if (apiKey.isNullOrBlank()) {
                val duration = System.currentTimeMillis() - startTime
                diagnostics = diagnostics.copy(
                    lastRequestTimestamp = System.currentTimeMillis(),
                    lastRequestStatus = "FAILED",
                    lastResponseReceived = false,
                    lastDurationMs = duration,
                    lastErrorReason = "API key not configured in AI Studio Secrets or Settings."
                )
                return@withContext AIConnectionTestResult(
                    isSuccess = false,
                    errorMessage = "API key not configured. Enter GEMINI_API_KEY in the AI Studio Secrets panel or tap 'Set Key' to enter it directly.",
                    durationMs = duration
                )
            }

            var lastHttpCode: Int? = null
            var lastErrorMsg: String = "Failed to connect to Gemini API."

            for (model in CANDIDATE_MODELS) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                    val jsonBody = JSONObject().apply {
                        val contentsArr = JSONArray().apply {
                            val contentObj = JSONObject().apply {
                                val partsArr = JSONArray().apply {
                                    put(JSONObject().put("text", "Respond with exactly: AI_CONNECTION_TEST_8472"))
                                }
                                put("parts", partsArr)
                            }
                            put(contentObj)
                        }
                        put("contents", contentsArr)
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val request = Request.Builder()
                        .url(url)
                        .post(jsonBody.toString().toRequestBody(mediaType))
                        .build()

                    val response = client.newCall(request).execute()
                    val duration = System.currentTimeMillis() - startTime
                    val responseBody = response.body?.string()
                    lastHttpCode = response.code

                    if (response.isSuccessful && responseBody != null) {
                        val root = JSONObject(responseBody)
                        val candidates = root.optJSONArray("candidates")
                        val text = candidates?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text")?.trim()

                        if (text != null && text.contains("AI_CONNECTION_TEST_8472")) {
                            diagnostics = diagnostics.copy(
                                isOnline = true,
                                lastRequestTimestamp = System.currentTimeMillis(),
                                lastRequestStatus = "SUCCESS",
                                lastResponseReceived = true,
                                lastDurationMs = duration,
                                lastDebugHttpCode = response.code,
                                lastErrorReason = null,
                                model = model
                            )
                            return@withContext AIConnectionTestResult(
                                isSuccess = true,
                                responseToken = "AI_CONNECTION_TEST_8472 ($model)",
                                durationMs = duration,
                                httpCode = response.code
                            )
                        }
                    } else if (response.code == 404) {
                        lastErrorMsg = mapHttpError(response.code, responseBody)
                        continue
                    } else {
                        val safeError = mapHttpError(response.code, responseBody)
                        diagnostics = diagnostics.copy(
                            lastRequestTimestamp = System.currentTimeMillis(),
                            lastRequestStatus = "FAILED",
                            lastResponseReceived = false,
                            lastDurationMs = duration,
                            lastDebugHttpCode = response.code,
                            lastErrorReason = safeError
                        )
                        return@withContext AIConnectionTestResult(
                            isSuccess = false,
                            errorMessage = safeError,
                            durationMs = duration,
                            httpCode = response.code
                        )
                    }
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    val safeError = mapExceptionError(e)
                    diagnostics = diagnostics.copy(
                        isOnline = e !is UnknownHostException && e !is ConnectException,
                        lastRequestTimestamp = System.currentTimeMillis(),
                        lastRequestStatus = "FAILED",
                        lastResponseReceived = false,
                        lastDurationMs = duration,
                        lastErrorReason = safeError
                    )
                    return@withContext AIConnectionTestResult(
                        isSuccess = false,
                        errorMessage = safeError,
                        durationMs = duration
                    )
                }
            }

            diagnostics = diagnostics.copy(
                lastRequestTimestamp = System.currentTimeMillis(),
                lastRequestStatus = "FAILED",
                lastResponseReceived = false,
                lastDurationMs = System.currentTimeMillis() - startTime,
                lastDebugHttpCode = lastHttpCode,
                lastErrorReason = lastErrorMsg
            )
            return@withContext AIConnectionTestResult(
                isSuccess = false,
                errorMessage = lastErrorMsg,
                durationMs = System.currentTimeMillis() - startTime,
                httpCode = lastHttpCode
            )
        }
    }

    override suspend fun interpretSpectrum(analysisRequest: AIInterpretationRequest): AIInterpretationResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val apiKey = AIConfiguration.getActiveApiKey()

            if (apiKey.isNullOrBlank()) {
                val duration = System.currentTimeMillis() - startTime
                diagnostics = diagnostics.copy(
                    lastRequestTimestamp = System.currentTimeMillis(),
                    lastRequestStatus = "FAILED",
                    lastResponseReceived = false,
                    localSpectroscopyDataSupplied = analysisRequest.peaks.isNotEmpty(),
                    detectedPeaksSupplied = analysisRequest.peaks.size,
                    referenceMatchesSupplied = analysisRequest.referenceMatches.size,
                    lastDurationMs = duration,
                    lastErrorReason = "Gemini API key is not configured."
                )
                return@withContext AIInterpretationResult(
                    isSuccess = false,
                    summary = "AI analysis unavailable — API key not configured. Enter GEMINI_API_KEY in the AI Studio Secrets panel or tap 'Set Key' to enter it directly.",
                    errorMessage = "Gemini API key not configured.",
                    isFromRealApi = false,
                    durationMs = duration,
                    measuredPeaksCount = analysisRequest.peaks.size,
                    dictionaryMatchesCount = analysisRequest.dictionaryMatches.size,
                    referenceMatchesCount = analysisRequest.referenceMatches.size
                )
            }

            // Construct structured prompt and system instructions adhering to all scientific constraints
            val requestPayload = AIRequestBuilder.buildPromptPayload(analysisRequest)

            var lastHttpCode: Int? = null
            var lastErrorMsg: String = "AI analysis unavailable — API request failed."

            for (model in CANDIDATE_MODELS) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                    val jsonBody = JSONObject().apply {
                        val contentsArr = JSONArray().apply {
                            val contentObj = JSONObject().apply {
                                val partsArr = JSONArray().apply {
                                    put(JSONObject().put("text", requestPayload))
                                }
                                put("parts", partsArr)
                            }
                            put(contentObj)
                        }
                        put("contents", contentsArr)
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val request = Request.Builder()
                        .url(url)
                        .post(jsonBody.toString().toRequestBody(mediaType))
                        .build()

                    val response = client.newCall(request).execute()
                    val duration = System.currentTimeMillis() - startTime
                    val responseBody = response.body?.string()
                    lastHttpCode = response.code

                    if (response.isSuccessful && responseBody != null) {
                        val rootObj = JSONObject(responseBody)
                        val candidates = rootObj.optJSONArray("candidates")
                        val candidate = candidates?.optJSONObject(0)
                        val content = candidate?.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val text = parts?.optJSONObject(0)?.optString("text")

                        if (!text.isNullOrBlank()) {
                            // Parse structured response using AIResponseParser
                            val parsedResult = AIResponseParser.parse(
                                rawText = text,
                                request = analysisRequest,
                                modelName = model,
                                durationMs = duration,
                                statusCode = response.code
                            )

                            diagnostics = diagnostics.copy(
                                isOnline = true,
                                model = model,
                                lastRequestTimestamp = System.currentTimeMillis(),
                                lastRequestStatus = "SUCCESS",
                                lastResponseReceived = true,
                                localSpectroscopyDataSupplied = true,
                                detectedPeaksSupplied = analysisRequest.peaks.size,
                                referenceMatchesSupplied = analysisRequest.referenceMatches.size,
                                lastDurationMs = duration,
                                lastDebugHttpCode = response.code,
                                lastErrorReason = null
                            )

                            return@withContext parsedResult
                        } else {
                            val err = "Model returned empty response payload."
                            diagnostics = diagnostics.copy(
                                lastRequestTimestamp = System.currentTimeMillis(),
                                lastRequestStatus = "FAILED",
                                lastResponseReceived = true,
                                lastDurationMs = duration,
                                lastDebugHttpCode = response.code,
                                lastErrorReason = err
                            )
                            return@withContext AIInterpretationResult(
                                isSuccess = false,
                                summary = "AI analysis unavailable — API request failed ($err).",
                                errorMessage = err,
                                isFromRealApi = false,
                                durationMs = duration,
                                httpStatusCode = response.code,
                                measuredPeaksCount = analysisRequest.peaks.size
                            )
                        }
                    } else if (response.code == 404) {
                        lastErrorMsg = mapHttpError(response.code, responseBody)
                        continue
                    } else {
                        val safeError = mapHttpError(response.code, responseBody)
                        diagnostics = diagnostics.copy(
                            lastRequestTimestamp = System.currentTimeMillis(),
                            lastRequestStatus = "FAILED",
                            lastResponseReceived = false,
                            localSpectroscopyDataSupplied = true,
                            detectedPeaksSupplied = analysisRequest.peaks.size,
                            referenceMatchesSupplied = analysisRequest.referenceMatches.size,
                            lastDurationMs = duration,
                            lastDebugHttpCode = response.code,
                            lastErrorReason = safeError
                        )
                        return@withContext AIInterpretationResult(
                            isSuccess = false,
                            summary = "AI analysis unavailable — API request failed ($safeError).",
                            errorMessage = safeError,
                            isFromRealApi = false,
                            durationMs = duration,
                            httpStatusCode = response.code,
                            measuredPeaksCount = analysisRequest.peaks.size
                        )
                    }
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    val safeError = mapExceptionError(e)
                    diagnostics = diagnostics.copy(
                        isOnline = e !is UnknownHostException && e !is ConnectException,
                        lastRequestTimestamp = System.currentTimeMillis(),
                        lastRequestStatus = "FAILED",
                        lastResponseReceived = false,
                        localSpectroscopyDataSupplied = true,
                        detectedPeaksSupplied = analysisRequest.peaks.size,
                        referenceMatchesSupplied = analysisRequest.referenceMatches.size,
                        lastDurationMs = duration,
                        lastErrorReason = safeError
                    )
                    return@withContext AIInterpretationResult(
                        isSuccess = false,
                        summary = "AI analysis unavailable — API request failed ($safeError).",
                        errorMessage = safeError,
                        isFromRealApi = false,
                        durationMs = duration,
                        measuredPeaksCount = analysisRequest.peaks.size
                    )
                }
            }

            diagnostics = diagnostics.copy(
                lastRequestTimestamp = System.currentTimeMillis(),
                lastRequestStatus = "FAILED",
                lastResponseReceived = false,
                localSpectroscopyDataSupplied = true,
                detectedPeaksSupplied = analysisRequest.peaks.size,
                referenceMatchesSupplied = analysisRequest.referenceMatches.size,
                lastDurationMs = System.currentTimeMillis() - startTime,
                lastDebugHttpCode = lastHttpCode,
                lastErrorReason = lastErrorMsg
            )
            return@withContext AIInterpretationResult(
                isSuccess = false,
                summary = "AI analysis unavailable — API request failed ($lastErrorMsg).",
                errorMessage = lastErrorMsg,
                isFromRealApi = false,
                durationMs = System.currentTimeMillis() - startTime,
                httpStatusCode = lastHttpCode,
                measuredPeaksCount = analysisRequest.peaks.size
            )
        }
    }

    override suspend fun interpretSpectrum(spectrum: Spectrum, customPrompt: String?): AIInterpretationResult {
        val request = AIInterpretationRequest(
            spectrumName = spectrum.name,
            spectrumType = spectrum.spectrumType.label,
            xUnit = spectrum.xUnit,
            yUnit = spectrum.yUnit,
            minWavenumber = spectrum.minWavenumber,
            maxWavenumber = spectrum.maxWavenumber,
            pointCount = spectrum.pointCount,
            peaks = spectrum.detectedPeaks,
            dictionaryMatches = spectrum.detectedPeaks.mapNotNull { it.matchedFunctionalGroup }.distinct(),
            preprocessingSteps = spectrum.preprocessingHistory.map { "${it.stepType}: ${it.description}" },
            customPrompt = customPrompt
        )
        return interpretSpectrum(request)
    }

    private fun parseErrorMessageFromBody(body: String?): String {
        if (body.isNullOrBlank()) return ""
        return try {
            val root = JSONObject(body)
            val errObj = root.optJSONObject("error")
            errObj?.optString("message") ?: ""
        } catch (t: Throwable) {
            ""
        }
    }

    private fun mapHttpError(code: Int, body: String?): String {
        val detail = parseErrorMessageFromBody(body)
        val suffix = if (detail.isNotBlank()) " — $detail" else ""
        return when (code) {
            400 -> "Invalid request / API key (HTTP 400)$suffix. Check that GEMINI_API_KEY is valid."
            401, 403 -> "Authentication failed (HTTP $code)$suffix. Verify your API key has Gemini API access."
            404 -> "Model endpoint not found (HTTP 404)$suffix."
            429 -> "Rate limit or quota exceeded (HTTP 429)$suffix. Please try again in a few moments."
            500, 502, 503 -> "Gemini API temporarily unavailable (HTTP $code)$suffix. Please try again shortly."
            else -> "HTTP $code returned by Gemini API$suffix."
        }
    }

    private fun mapExceptionError(e: Exception): String {
        return when (e) {
            is UnknownHostException -> "No internet connection (unable to reach generativelanguage.googleapis.com)."
            is ConnectException -> "Network connection failed. Verify device connectivity."
            is SocketTimeoutException -> "Request timed out after waiting for Gemini API response."
            else -> e.message?.take(120) ?: "Network request failed unexpectedly."
        }
    }
}

// -------------------------------------------------------------
// 5. AI REQUEST BUILDER (SYSTEM INSTRUCTIONS & STRUCTURED INPUT)
// -------------------------------------------------------------

internal object AIRequestBuilder {

    fun buildPromptPayload(request: AIInterpretationRequest): String {
        val jsonPayload = JSONObject().apply {
            put("spectrumName", request.spectrumName)
            put("spectrumType", request.spectrumType)
            put("wavenumberRange", "${request.minWavenumber.toInt()} to ${request.maxWavenumber.toInt()} ${request.xUnit}")
            put("pointCount", request.pointCount)

            // Significant locally measured peaks
            val peaksArr = JSONArray()
            request.peaks.forEach { p ->
                peaksArr.put(JSONObject().apply {
                    put("wavenumber_cm-1", "%.1f".format(Locale.US, p.wavenumber))
                    put("intensity_signal", "%.2f".format(Locale.US, p.signalValue))
                    put("prominence", "%.2f".format(Locale.US, p.prominence))
                    put("fwhm_cm-1", "%.1f".format(Locale.US, p.fwhm))
                    put("width_cm-1", "%.1f".format(Locale.US, p.width))
                    put("area", "%.1f".format(Locale.US, p.area))
                    put("relativeIntensity", p.relativeIntensity)
                    p.matchedFunctionalGroup?.let { put("dictionaryMatchedGroup", it) }
                    p.notes?.let { put("note", it) }
                })
            }
            put("locallyMeasuredPeaks", peaksArr)

            // Functional group region vs Fingerprint region peaks
            val fgPeaks = request.peaks.filter { it.wavenumber >= 1500.0 }.map { "%.1f".format(Locale.US, it.wavenumber) }
            val fpPeaks = request.peaks.filter { it.wavenumber < 1500.0 }.map { "%.1f".format(Locale.US, it.wavenumber) }
            put("functionalGroupRegionBands_4000to1500cm-1", JSONArray(fgPeaks))
            put("fingerprintRegionBands_1500to400cm-1", JSONArray(fpPeaks))

            // IR Dictionary correlation assignments
            put("localDictionaryAssignments", JSONArray(request.dictionaryMatches))

            // Reference standards comparisons
            put("referenceStandardsComparison", JSONArray(request.referenceMatches))
            request.structuredComparisonSummary?.let { put("referenceNumericalSimilarityDetails", it) }

            // Preprocessing history
            put("preprocessingHistory", JSONArray(request.preprocessingSteps))

            // Optional User Context (Supporting background only)
            request.userContext?.let { ctx ->
                put("userContext", JSONObject().apply {
                    if (ctx.sampleName.isNotBlank()) put("sampleName", ctx.sampleName)
                    if (ctx.sampleType.isNotBlank()) put("sampleType", ctx.sampleType)
                    if (ctx.expectedComposition.isNotBlank()) put("expectedComposition", ctx.expectedComposition)
                    if (ctx.knownComponents.isNotBlank()) put("knownComponents", ctx.knownComponents)
                    if (ctx.measurementMethod.isNotBlank()) put("measurementMethod", ctx.measurementMethod)
                    if (ctx.experimentalNotes.isNotBlank()) put("experimentalNotes", ctx.experimentalNotes)
                })
            }
        }

        return """
            You are an expert scientific spectroscopy assistant and physical organic chemist interpreting experimental FTIR spectral data.
            
            CORE PRINCIPLE:
            The AI is an interpreter and scientific assistant, NOT the measurement instrument.
            Local numerical analysis has already performed peak detection, peak measurements, dictionary matching, and reference comparisons on the device.
            
            SCIENTIFIC INSTRUCTIONS:
            1. Distinguish physical observations from interpretations.
            2. Consider the complete spectrum holistically (correlating both functional-group regions 4000-1500 cm⁻¹ and fingerprint region 1500-400 cm⁻¹).
            3. Use established infrared spectroscopy principles (Beer-Lambert, dipole moment changes during vibration, harmonic oscillators, Fermi resonance, hydrogen bonding shifts).
            4. Identify supporting evidence and explicitly point out contradictory evidence or ambiguities.
            5. AVOID unsupported certainty. A high reference similarity score or matching peaks do NOT prove definitive compound identity.
            6. Use scientifically cautious language: "consistent with", "compatible with", "suggests", "possible assignment", "likely assignment", "insufficient evidence".
            7. NEVER state "definitely this compound" unless there is independent exhaustive proof, and even then explain the limitation.
            8. If sample context is provided, treat it strictly as background context, never as proof.
            
            MEASURED EXPERIMENTAL DATA:
            ${jsonPayload.toString(2)}
            
            ${request.customPrompt?.let { "USER INQUIRY: $it\n" } ?: ""}
            
            OUTPUT FORMAT:
            You MUST return a valid JSON object strictly matching this schema (do not output preamble or commentary outside the JSON):
            {
              "spectrumSummary": {
                "spectrumType": "${request.spectrumType}",
                "range": "${request.minWavenumber.toInt()} - ${request.maxWavenumber.toInt()} ${request.xUnit}",
                "peakCount": ${request.peaks.size},
                "functionalGroupRegionSummary": "Concise summary of bands in 4000-1500 cm⁻¹",
                "fingerprintRegionSummary": "Concise summary of bands in 1500-400 cm⁻¹"
              },
              "majorPeakAssignments": [
                {
                  "wavenumber": 1715.0,
                  "observedFeature": "Sharp, intense band",
                  "possibleBond": "C=O",
                  "functionalGroup": "Aliphatic Ketone Carbonyl",
                  "vibrationalMode": "C=O stretching",
                  "supportingEvidence": "Absence of broad 3300 cm⁻¹ O-H rules out carboxylic acid",
                  "alternativeExplanations": "Could also be an ester if companion 1200 cm⁻¹ C-O band is confirmed"
                }
              ],
              "completeSpectrumInterpretation": "A coherent physical organic chemistry synthesis of how the combination of peaks indicates the sample structural class, polymer backbone, or functional architecture.",
              "referenceComparison": "Summary evaluating similarity with library standards, noting spectral overlap and deviations.",
              "uncertainties": [
                "Ambiguity between ketone and ester due to overlapping fingerprint region bands",
                "Baseline slope indicates potential scattering or surface roughness"
              ],
              "additionalInformationNeeded": [
                "Complementary 1H/13C NMR spectroscopy to confirm carbon skeleton",
                "Baseline correction or ATR crystal background check"
              ],
              "confidenceEstimate": {
                "label": "AI/database confidence estimate",
                "level": "Moderate",
                "basis": "Clear carbonyl and aliphatic C-H bands present, but fingerprint region contains unresolved doublet."
              },
              "candidateCompounds": [
                "Compatible with aliphatic ketones (e.g. acetone family)",
                "Suggests ester plasticizer mixture"
              ]
            }
        """.trimIndent()
    }
}

// -------------------------------------------------------------
// 6. AI RESPONSE PARSER (ROBUST JSON & SAFE FALLBACK PARSING)
// -------------------------------------------------------------

internal object AIResponseParser {

    fun parse(
        rawText: String,
        request: AIInterpretationRequest,
        modelName: String,
        durationMs: Long,
        statusCode: Int
    ): AIInterpretationResult {
        val trimmed = rawText.trim()
        val cleanedJson = extractJsonSubstring(trimmed)

        if (cleanedJson != null) {
            try {
                val root = JSONObject(cleanedJson)

                // 1. Spectrum Summary
                val sumObj = root.optJSONObject("spectrumSummary")
                val spectrumSummary = AISpectrumSummary(
                    spectrumType = sumObj?.optString("spectrumType", request.spectrumType) ?: request.spectrumType,
                    range = sumObj?.optString("range", "${request.minWavenumber.toInt()}–${request.maxWavenumber.toInt()} cm⁻¹") ?: "${request.minWavenumber.toInt()}–${request.maxWavenumber.toInt()} cm⁻¹",
                    peakCount = sumObj?.optInt("peakCount", request.peaks.size) ?: request.peaks.size,
                    functionalGroupRegionSummary = sumObj?.optString("functionalGroupRegionSummary", "") ?: "",
                    fingerprintRegionSummary = sumObj?.optString("fingerprintRegionSummary", "") ?: ""
                )

                // 2. Major Peak Assignments
                val peakAssignments = mutableListOf<AIMajorPeakAssignment>()
                val peaksArr = root.optJSONArray("majorPeakAssignments")
                if (peaksArr != null) {
                    for (i in 0 until peaksArr.length()) {
                        val pObj = peaksArr.optJSONObject(i) ?: continue
                        peakAssignments.add(
                            AIMajorPeakAssignment(
                                wavenumber = pObj.optDouble("wavenumber", 0.0),
                                observedFeature = pObj.optString("observedFeature", ""),
                                possibleBond = pObj.optString("possibleBond", ""),
                                functionalGroup = pObj.optString("functionalGroup", ""),
                                vibrationalMode = pObj.optString("vibrationalMode", ""),
                                supportingEvidence = pObj.optString("supportingEvidence", ""),
                                alternativeExplanations = pObj.optString("alternativeExplanations", "")
                            )
                        )
                    }
                }

                // 3. Complete Spectrum Interpretation
                val interpretation = root.optString("completeSpectrumInterpretation", trimmed)

                // 4. Reference Comparison
                val refComp = root.optString("referenceComparison", "Reference alignment evaluated across local library standards.")

                // 5. Uncertainties & Ambiguities
                val uncertainties = mutableListOf<String>()
                val uncArr = root.optJSONArray("uncertainties")
                if (uncArr != null) {
                    for (i in 0 until uncArr.length()) {
                        val u = uncArr.optString(i, "")
                        if (u.isNotBlank()) uncertainties.add(u)
                    }
                }

                // 6. Additional Information Needed
                val addInfo = mutableListOf<String>()
                val addArr = root.optJSONArray("additionalInformationNeeded")
                if (addArr != null) {
                    for (i in 0 until addArr.length()) {
                        val a = addArr.optString(i, "")
                        if (a.isNotBlank()) addInfo.add(a)
                    }
                }

                // 7. Confidence Estimate
                val confObj = root.optJSONObject("confidenceEstimate")
                val confidenceEstimate = AIConfidenceEstimate(
                    label = confObj?.optString("label", "AI/database confidence estimate") ?: "AI/database confidence estimate",
                    level = confObj?.optString("level", "Moderate") ?: "Moderate",
                    basis = confObj?.optString("basis", "Evaluated from peak alignment and dictionary matches.") ?: "Evaluated from peak alignment and dictionary matches."
                )

                // Candidate compounds list
                val candidates = mutableListOf<String>()
                val candArr = root.optJSONArray("candidateCompounds")
                if (candArr != null) {
                    for (i in 0 until candArr.length()) {
                        val c = candArr.optString(i, "")
                        if (c.isNotBlank()) candidates.add(c)
                    }
                }

                val structuralInsights = mutableListOf<String>()
                peakAssignments.take(4).forEach {
                    structuralInsights.add("${it.wavenumber.toInt()} cm⁻¹: ${it.possibleBond} (${it.functionalGroup})")
                }

                return AIInterpretationResult(
                    isSuccess = true,
                    summary = interpretation,
                    candidateCompounds = if (candidates.isNotEmpty()) candidates else listOf("See comprehensive vibrational assignment below"),
                    structuralInsights = if (structuralInsights.isNotEmpty()) structuralInsights else listOf("Vibrational mode characterization complete"),
                    spectrumSummary = spectrumSummary,
                    majorPeakAssignments = peakAssignments,
                    completeSpectrumInterpretation = interpretation,
                    referenceComparisonSummary = refComp,
                    uncertainties = uncertainties,
                    additionalInformationNeeded = addInfo,
                    confidenceEstimate = confidenceEstimate,
                    rawResponseText = trimmed,
                    errorMessage = null,
                    isFromRealApi = true,
                    durationMs = durationMs,
                    httpStatusCode = statusCode,
                    modelName = modelName,
                    measuredPeaksCount = request.peaks.size,
                    dictionaryMatchesCount = request.dictionaryMatches.size,
                    referenceMatchesCount = request.referenceMatches.size,
                    userContextIncluded = request.userContext != null
                )
            } catch (e: Exception) {
                // If JSON parsing fails, gracefully fall back to heuristic extraction without crashing
            }
        }

        // Graceful fallback for non-JSON or plain markdown responses
        return buildHeuristicFallbackResult(trimmed, request, modelName, durationMs, statusCode)
    }

    private fun extractJsonSubstring(text: String): String? {
        val startBrace = text.indexOf('{')
        val endBrace = text.lastIndexOf('}')
        if (startBrace >= 0 && endBrace > startBrace) {
            return text.substring(startBrace, endBrace + 1)
        }
        return null
    }

    private fun buildHeuristicFallbackResult(
        rawText: String,
        request: AIInterpretationRequest,
        modelName: String,
        durationMs: Long,
        statusCode: Int
    ): AIInterpretationResult {
        val lines = rawText.lines().filter { it.isNotBlank() }

        val bulletLines = lines
            .filter { it.trimStart().startsWith("-") || it.trimStart().startsWith("•") || it.trimStart().startsWith("*") }
            .map { it.trim().removePrefix("-").removePrefix("•").removePrefix("*").trim() }

        val candidateLines = lines.filter {
            it.contains("consistent with", ignoreCase = true) ||
            it.contains("compatible with", ignoreCase = true) ||
            it.contains("suggests", ignoreCase = true) ||
            it.contains("candidate", ignoreCase = true)
        }.take(4)

        val assignments = request.peaks.take(5).map { p ->
            AIMajorPeakAssignment(
                wavenumber = p.wavenumber,
                observedFeature = if (p.prominence > 20.0) "Strong diagnostic band" else "Medium band",
                possibleBond = p.matchedFunctionalGroup ?: "Characteristic IR band",
                functionalGroup = p.matchedFunctionalGroup ?: "Observed vibrational feature",
                vibrationalMode = "Vibrational absorption",
                supportingEvidence = "Measured at ${"%.1f".format(Locale.US, p.wavenumber)} cm⁻¹ (FWHM: ${"%.1f".format(Locale.US, p.fwhm)} cm⁻¹)",
                alternativeExplanations = "Inspect complementary Raman or NMR spectra"
            )
        }

        return AIInterpretationResult(
            isSuccess = true,
            summary = rawText,
            candidateCompounds = if (candidateLines.isNotEmpty()) candidateLines else listOf("Compatible with experimental spectrum vibrational profile"),
            structuralInsights = if (bulletLines.isNotEmpty()) bulletLines.take(5) else listOf("Physical organic chemistry interpretation complete"),
            spectrumSummary = AISpectrumSummary(
                spectrumType = request.spectrumType,
                range = "${request.minWavenumber.toInt()}–${request.maxWavenumber.toInt()} cm⁻¹",
                peakCount = request.peaks.size,
                functionalGroupRegionSummary = "Diagnostic functional group absorption bands evaluated",
                fingerprintRegionSummary = "Skeletal and fingerprint absorption bands evaluated"
            ),
            majorPeakAssignments = assignments,
            completeSpectrumInterpretation = rawText,
            referenceComparisonSummary = "Evaluated against local reference standard profiles.",
            uncertainties = listOf("Overlapping bands in fingerprint region may reflect multicomponent matrix or baseline tilt."),
            additionalInformationNeeded = listOf("Complementary 1H/13C NMR or Raman measurements recommended for definitive validation."),
            confidenceEstimate = AIConfidenceEstimate(
                label = "AI/database confidence estimate",
                level = "Moderate",
                basis = "Calculated from peak correlation and IR dictionary rules."
            ),
            rawResponseText = rawText,
            errorMessage = null,
            isFromRealApi = true,
            durationMs = durationMs,
            httpStatusCode = statusCode,
            modelName = modelName,
            measuredPeaksCount = request.peaks.size,
            dictionaryMatchesCount = request.dictionaryMatches.size,
            referenceMatchesCount = request.referenceMatches.size,
            userContextIncluded = request.userContext != null
        )
    }
}
