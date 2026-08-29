package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.model.ExtractionPreset
import com.example.data.model.ExtractionProgress
import com.example.data.model.SamplePdfInfo
import com.example.data.model.SpreadsheetDocument
import com.example.data.model.SpreadsheetSheet
import com.example.data.repository.DocumentRepository
import com.example.service.GeminiExtractionService
import com.example.service.PdfProcessor
import com.example.service.SpreadsheetExporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class AppTab(val title: String) {
    CONVERT("Convert"),
    STUDIO("Spreadsheet"),
    HISTORY("History"),
    TEMPLATES("Schemas")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = DocumentRepository(db.conversionRecordDao(), db.extractionTemplateDao())
    private val pdfProcessor = PdfProcessor(application)
    private val geminiService = GeminiExtractionService()
    private val exporter = SpreadsheetExporter(application)

    // Navigation & Screen State
    private val _currentTab = MutableStateFlow(AppTab.CONVERT)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Conversion Input State
    private val _selectedPdfMetadata = MutableStateFlow<PdfProcessor.PdfMetadata?>(null)
    val selectedPdfMetadata: StateFlow<PdfProcessor.PdfMetadata?> = _selectedPdfMetadata.asStateFlow()

    private val _renderedBitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val renderedBitmaps: StateFlow<List<Bitmap>> = _renderedBitmaps.asStateFlow()

    private val _selectedPreset = MutableStateFlow(ExtractionPreset.AUTO_DETECT)
    val selectedPreset: StateFlow<ExtractionPreset> = _selectedPreset.asStateFlow()

    private val _customPrompt = MutableStateFlow("")
    val customPrompt: StateFlow<String> = _customPrompt.asStateFlow()

    private val _targetColumnsText = MutableStateFlow("")
    val targetColumnsText: StateFlow<String> = _targetColumnsText.asStateFlow()

    // Active Processing State
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _extractionProgress = MutableStateFlow<ExtractionProgress?>(null)
    val extractionProgress: StateFlow<ExtractionProgress?> = _extractionProgress.asStateFlow()

    private var conversionJob: Job? = null

    // Active Workbook in Studio
    private val _activeDocument = MutableStateFlow<SpreadsheetDocument?>(null)
    val activeDocument: StateFlow<SpreadsheetDocument?> = _activeDocument.asStateFlow()

    private val _activeSheetIndex = MutableStateFlow(0)
    val activeSheetIndex: StateFlow<Int> = _activeSheetIndex.asStateFlow()

    // History and Templates from Room
    val historyDocuments: StateFlow<List<SpreadsheetDocument>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customTemplates = repository.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback Message
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    val samplePdfs: List<SamplePdfInfo> = pdfProcessor.samplePdfs

    init {
        // Pre-load default sample on start so user has a rich ready-to-test state
        viewModelScope.launch {
            try {
                loadSample(samplePdfs.first())
            } catch (e: Exception) {
                Log.e("MainViewModel", "Initial sample load failed", e)
            }
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setPreset(preset: ExtractionPreset) {
        _selectedPreset.value = preset
        if (preset.suggestedColumns.isNotEmpty() && _targetColumnsText.value.isBlank()) {
            _targetColumnsText.value = preset.suggestedColumns.joinToString(", ")
        }
    }

    fun setCustomPrompt(prompt: String) {
        _customPrompt.value = prompt
    }

    fun setTargetColumnsText(columns: String) {
        _targetColumnsText.value = columns
    }

    fun setSelectedSheetIndex(index: Int) {
        _activeSheetIndex.value = index
    }

    fun handlePickedPdf(uri: Uri) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _extractionProgress.value = ExtractionProgress(
                    currentStep = "Inspecting PDF",
                    detailMessage = "Reading file structure and page dimensions...",
                    progressFraction = 0.15f
                )

                val meta = pdfProcessor.inspectPdf(uri)
                _selectedPdfMetadata.value = meta

                val bitmaps = pdfProcessor.renderPagesToBitmaps(meta.tempFilePath, maxPages = 5)
                _renderedBitmaps.value = bitmaps

                _isProcessing.value = false
                _extractionProgress.value = null
                _uiEvents.emit("Loaded \"${meta.fileName}\" (${meta.pageCount} pages)")
            } catch (e: Exception) {
                _isProcessing.value = false
                _extractionProgress.value = null
                _uiEvents.emit("Failed to open PDF: ${e.localizedMessage}")
            }
        }
    }

    fun loadSample(sample: SamplePdfInfo) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _extractionProgress.value = ExtractionProgress(
                    currentStep = "Loading Sample",
                    detailMessage = "Generating \"${sample.title}\"...",
                    progressFraction = 0.2f
                )

                val sampleFile = pdfProcessor.generateSamplePdf(sample.id)
                val meta = PdfProcessor.PdfMetadata(
                    fileName = "${sample.id}.pdf",
                    pageCount = sample.pageCount,
                    fileSizeBytes = sampleFile.length(),
                    tempFilePath = sampleFile.absolutePath
                )
                _selectedPdfMetadata.value = meta

                val bitmaps = pdfProcessor.renderPagesToBitmaps(sampleFile.absolutePath, maxPages = 5)
                _renderedBitmaps.value = bitmaps

                setPreset(sample.defaultPreset)

                _isProcessing.value = false
                _extractionProgress.value = null
                _uiEvents.emit("Loaded sample: ${sample.title}")
            } catch (e: Exception) {
                _isProcessing.value = false
                _extractionProgress.value = null
                _uiEvents.emit("Failed to load sample: ${e.localizedMessage}")
            }
        }
    }

    fun startAiExtraction() {
        val bitmaps = _renderedBitmaps.value
        val meta = _selectedPdfMetadata.value

        if (bitmaps.isEmpty()) {
            viewModelScope.launch {
                _uiEvents.emit("Please select a PDF document first")
            }
            return
        }

        conversionJob?.cancel()
        conversionJob = viewModelScope.launch {
            try {
                _isProcessing.value = true
                _extractionProgress.value = ExtractionProgress(
                    currentStep = "Starting AI Engine",
                    detailMessage = "Initializing Gemini Multimodal Vision...",
                    progressFraction = 0.05f
                )

                val parsedTargetCols = _targetColumnsText.value
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                val doc = geminiService.extractSpreadsheetFromPdf(
                    pdfBitmaps = bitmaps,
                    preset = _selectedPreset.value,
                    customPrompt = _customPrompt.value.takeIf { it.isNotBlank() },
                    targetColumns = parsedTargetCols,
                    onStepProgress = { step, fraction ->
                        _extractionProgress.value = ExtractionProgress(
                            currentStep = step,
                            detailMessage = "Accurately detecting tables and cell fields...",
                            progressFraction = fraction
                        )
                    }
                )

                val namedDoc = if (meta != null) doc.copy(fileName = meta.fileName) else doc

                // Save to Room database
                repository.saveDocument(namedDoc)

                // Set as active document and switch to Studio view
                _activeDocument.value = namedDoc
                _activeSheetIndex.value = 0
                _isProcessing.value = false
                _extractionProgress.value = null
                _currentTab.value = AppTab.STUDIO
                _uiEvents.emit("Extracted ${namedDoc.totalRows} rows across ${namedDoc.totalSheets} sheets!")
            } catch (e: Exception) {
                _isProcessing.value = false
                _extractionProgress.value = null
                Log.e("MainViewModel", "Extraction failed", e)
                _uiEvents.emit("Extraction Error: ${e.localizedMessage ?: "Network or API error"}")
            }
        }
    }

    fun cancelExtraction() {
        conversionJob?.cancel()
        _isProcessing.value = false
        _extractionProgress.value = null
        viewModelScope.launch {
            _uiEvents.emit("Extraction cancelled")
        }
    }

    fun updateActiveSheet(updatedSheet: SpreadsheetSheet) {
        val currentDoc = _activeDocument.value ?: return
        val currentIdx = _activeSheetIndex.value
        val mutableSheets = currentDoc.sheets.toMutableList()

        if (currentIdx in mutableSheets.indices) {
            mutableSheets[currentIdx] = updatedSheet
            val updatedDoc = currentDoc.copy(sheets = mutableSheets)
            _activeDocument.value = updatedDoc
            viewModelScope.launch {
                repository.updateDocument(updatedDoc)
            }
        }
    }

    fun addNewSheet(name: String = "New Sheet") {
        val currentDoc = _activeDocument.value ?: return
        val newSheet = SpreadsheetSheet(
            name = name,
            headers = listOf("Column 1", "Column 2", "Column 3"),
            rows = listOf(listOf("", "", ""))
        ).withDetectedColumnTypes()

        val updatedSheets = currentDoc.sheets + newSheet
        val updatedDoc = currentDoc.copy(sheets = updatedSheets)
        _activeDocument.value = updatedDoc
        _activeSheetIndex.value = updatedSheets.lastIndex
        viewModelScope.launch {
            repository.updateDocument(updatedDoc)
            _uiEvents.emit("Added new sheet: $name")
        }
    }

    fun exportActiveDocument(format: SpreadsheetExporter.ExportFormat) {
        val doc = _activeDocument.value ?: return
        viewModelScope.launch {
            try {
                val file = exporter.exportToFile(doc, format, _activeSheetIndex.value)
                exporter.shareFile(file, format.mimeType, "Export ${doc.fileName}")
                _uiEvents.emit("Exported as ${format.displayName}")
            } catch (e: Exception) {
                _uiEvents.emit("Export failed: ${e.localizedMessage}")
            }
        }
    }

    fun copyCurrentSheetToClipboard(asTsv: Boolean = true) {
        val doc = _activeDocument.value ?: return
        val sheet = doc.sheets.getOrNull(_activeSheetIndex.value) ?: return
        exporter.copyToClipboard(sheet, asTsv)
        viewModelScope.launch {
            _uiEvents.emit("Table copied to clipboard! (Ready to paste in Excel/Sheets)")
        }
    }

    fun openDocumentFromHistory(doc: SpreadsheetDocument) {
        _activeDocument.value = doc
        _activeSheetIndex.value = 0
        _currentTab.value = AppTab.STUDIO
    }

    fun toggleFavorite(doc: SpreadsheetDocument) {
        viewModelScope.launch {
            repository.toggleFavorite(doc.id, !doc.isFavorite)
            if (_activeDocument.value?.id == doc.id) {
                _activeDocument.value = _activeDocument.value?.copy(isFavorite = !doc.isFavorite)
            }
        }
    }

    fun deleteDocument(doc: SpreadsheetDocument) {
        viewModelScope.launch {
            repository.deleteDocument(doc.id)
            if (_activeDocument.value?.id == doc.id) {
                _activeDocument.value = null
            }
            _uiEvents.emit("Deleted \"${doc.fileName}\"")
        }
    }

    fun saveCustomTemplate(title: String, description: String, targetCols: String, prompt: String) {
        viewModelScope.launch {
            repository.saveTemplate(title, description, targetCols, prompt)
            _uiEvents.emit("Template saved: $title")
        }
    }

    fun deleteCustomTemplate(templateId: Long) {
        viewModelScope.launch {
            repository.deleteTemplate(templateId)
            _uiEvents.emit("Template deleted")
        }
    }
}
