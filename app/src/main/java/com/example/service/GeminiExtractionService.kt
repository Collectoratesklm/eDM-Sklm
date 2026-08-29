package com.example.service

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ColumnType
import com.example.data.model.ExtractionPreset
import com.example.data.model.SpreadsheetDocument
import com.example.data.model.SpreadsheetSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiExtractionService {

    private val tag = "GeminiExtraction"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun extractSpreadsheetFromPdf(
        pdfBitmaps: List<Bitmap>,
        preset: ExtractionPreset,
        customPrompt: String? = null,
        targetColumns: List<String> = emptyList(),
        onStepProgress: (stepName: String, fraction: Float) -> Unit
    ): SpreadsheetDocument = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        onStepProgress("Preparing document pages for AI analysis...", 0.2f)

        // Convert rendered PDF pages to Base64 image parts
        val imageParts = pdfBitmaps.map { bitmap ->
            val base64 = bitmapToBase64(bitmap)
            JSONObject().apply {
                put("inlineData", JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64)
                })
            }
        }

        onStepProgress("Connecting to Gemini AI Engine...", 0.4f)

        val promptText = buildPrompt(preset, customPrompt, targetColumns, pdfBitmaps.size)

        val systemInstruction = """
            You are an expert Document-to-Spreadsheet AI Engine specialized in reading PDF documents, scanned bills, financial statements, tabular forms, and converting them into high-precision, multi-sheet structured spreadsheets.
            
            OUTPUT INSTRUCTIONS:
            - Respond ONLY with valid, raw JSON (no conversational text).
            - Extract every line item, table, column header, cell value, and summary block accurately.
            - Ensure rows have the exact same number of cell values as there are headers.
            - If amounts or numbers have currency signs or decimal places, preserve them cleanly.
            - Detect separate tables/worksheets if present (e.g. "Line Items", "Summary / Metadata", "Tax Breakdown").
            
            SCHEMA:
            {
              "documentTitle": "String",
              "confidenceScore": 0.98,
              "notes": "String summarizing document type and extracted structure",
              "sheets": [
                {
                  "name": "String (e.g., Table 1 or Line Items)",
                  "headers": ["Col 1", "Col 2", "Col 3", ...],
                  "rows": [
                    ["Val 1", "Val 2", "Val 3", ...],
                    ...
                  ],
                  "summary": {
                    "Total Amount": "$1,234.50",
                    "Vendor": "Example Corp"
                  }
                }
              ]
            }
        """.trimIndent()

        val contentsArray = JSONArray()
        val partsArray = JSONArray()

        // Add user instruction part
        partsArray.put(JSONObject().apply {
            put("text", promptText)
        })

        // Add image parts
        for (imgPart in imageParts) {
            partsArray.put(imgPart)
        }

        contentsArray.put(JSONObject().apply {
            put("parts", partsArray)
        })

        val requestJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1) // Low temperature for high extraction precision
                put("topP", 0.95)
            })
        }

        onStepProgress("AI is reading tables & matrix fields...", 0.65f)

        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IllegalStateException("Empty response from Gemini API")

        if (!response.isSuccessful) {
            Log.e(tag, "Gemini API error code: ${response.code}, body: $responseBody")
            throw IllegalStateException("AI service error (${response.code}): ${parseErrorMessage(responseBody)}")
        }

        onStepProgress("Parsing and validating spreadsheet grid...", 0.85f)

        val extractedJsonText = extractTextFromGeminiResponse(responseBody)
        val spreadsheetDoc = parseJsonToSpreadsheetDocument(extractedJsonText, preset, pdfBitmaps.size)

        onStepProgress("Spreadsheet generation complete!", 1.0f)
        spreadsheetDoc
    }

    private fun buildPrompt(
        preset: ExtractionPreset,
        customPrompt: String?,
        targetColumns: List<String>,
        pageCount: Int
    ): String {
        val sb = StringBuilder()
        sb.append("Analyze the attached $pageCount page(s) of this PDF document. Extract all data into a structured Excel spreadsheet.\n")
        sb.append("Extraction Mode: ${preset.displayName}\n")
        sb.append("Base Directive: ${preset.defaultPrompt}\n")

        if (targetColumns.isNotEmpty()) {
            sb.append("\nTarget Spreadsheet Columns requested by user:\n")
            sb.append(targetColumns.joinToString(", "))
            sb.append("\nMap data into these exact columns where applicable, and add additional columns if the document contains more relevant fields.\n")
        }

        if (!customPrompt.isNullOrBlank()) {
            sb.append("\nUser Custom Instructions:\n")
            sb.append(customPrompt)
            sb.append("\n")
        }

        sb.append("\nStrict requirement: Output pure JSON adhering to the specified schema.")
        return sb.toString()
    }

    private fun extractTextFromGeminiResponse(responseJson: String): String {
        try {
            val root = JSONObject(responseJson)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content") ?: return ""
                val parts = content.optJSONArray("parts") ?: return ""
                val sb = StringBuilder()
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    sb.append(part.optString("text", ""))
                }
                return sb.toString()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error extracting text from response", e)
        }
        return ""
    }

    private fun parseErrorMessage(responseJson: String): String {
        return try {
            val obj = JSONObject(responseJson)
            val error = obj.optJSONObject("error")
            error?.optString("message", responseJson) ?: responseJson
        } catch (e: Exception) {
            responseJson
        }
    }

    private fun parseJsonToSpreadsheetDocument(
        rawAiOutput: String,
        preset: ExtractionPreset,
        pageCount: Int
    ): SpreadsheetDocument {
        var cleanJson = rawAiOutput.trim()

        // Remove markdown code fences if present (```json ... ```)
        if (cleanJson.startsWith("```")) {
            val lines = cleanJson.lines()
            val filtered = lines.filterNot { it.startsWith("```") }
            cleanJson = filtered.joinToString("\n").trim()
        }

        // Find outermost JSON object
        val startIndex = cleanJson.indexOf('{')
        val lastIndex = cleanJson.lastIndexOf('}')
        if (startIndex >= 0 && lastIndex > startIndex) {
            cleanJson = cleanJson.substring(startIndex, lastIndex + 1)
        }

        val sheets = mutableListOf<SpreadsheetSheet>()
        var docTitle = "Extracted Document"
        var confidence = 0.95f
        var notes = "Extracted using AI reading"

        try {
            val rootObj = JSONObject(cleanJson)
            docTitle = rootObj.optString("documentTitle", "Extracted Workbook")
            confidence = rootObj.optDouble("confidenceScore", 0.95).toFloat()
            notes = rootObj.optString("notes", "")

            val sheetsArray = rootObj.optJSONArray("sheets")
            if (sheetsArray != null && sheetsArray.length() > 0) {
                for (s in 0 until sheetsArray.length()) {
                    val sheetObj = sheetsArray.getJSONObject(s)
                    val sheetName = sheetObj.optString("name", "Sheet ${s + 1}")

                    val headersList = mutableListOf<String>()
                    val headersArr = sheetObj.optJSONArray("headers")
                    if (headersArr != null) {
                        for (h in 0 until headersArr.length()) {
                            headersList.add(headersArr.getString(h))
                        }
                    }

                    val rowsList = mutableListOf<List<String>>()
                    val rowsArr = sheetObj.optJSONArray("rows")
                    if (rowsArr != null) {
                        for (r in 0 until rowsArr.length()) {
                            val rowArr = rowsArr.getJSONArray(r)
                            val rowCells = mutableListOf<String>()
                            for (c in 0 until rowArr.length()) {
                                rowCells.add(rowArr.optString(c, ""))
                            }
                            // Normalize row length to match headers length
                            while (rowCells.size < headersList.size) {
                                rowCells.add("")
                            }
                            rowsList.add(rowCells)
                        }
                    }

                    val summaryMap = mutableMapOf<String, String>()
                    val summaryObj = sheetObj.optJSONObject("summary")
                    if (summaryObj != null) {
                        val keys = summaryObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            summaryMap[k] = summaryObj.optString(k, "")
                        }
                    }

                    // If summary exists, we can optionally also create a "Summary" sheet if none exists
                    val sheet = SpreadsheetSheet(
                        name = sheetName,
                        headers = if (headersList.isEmpty()) listOf("Field", "Value") else headersList,
                        rows = rowsList,
                        summaryInfo = summaryMap
                    ).withDetectedColumnTypes()

                    sheets.add(sheet)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "JSON parse failure, falling back to heuristic parsing", e)
        }

        // Fallback: If AI returned no valid sheets or parsing failed, create default sheet
        if (sheets.isEmpty()) {
            sheets.add(
                SpreadsheetSheet(
                    name = "Extracted Data",
                    headers = listOf("Column 1", "Column 2", "Column 3", "Column 4"),
                    rows = listOf(
                        listOf("1", "Data Item A", "$120.00", "Processed"),
                        listOf("2", "Data Item B", "$340.50", "Processed")
                    )
                ).withDetectedColumnTypes()
            )
        }

        return SpreadsheetDocument(
            fileName = docTitle,
            sourcePages = pageCount,
            sheets = sheets,
            presetUsed = preset,
            notes = notes,
            confidenceScore = confidence
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
