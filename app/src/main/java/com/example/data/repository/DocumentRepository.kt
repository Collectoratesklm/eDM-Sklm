package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.ConversionRecordDao
import com.example.data.local.ConversionRecordEntity
import com.example.data.local.Converters
import com.example.data.local.ExtractionTemplateDao
import com.example.data.local.ExtractionTemplateEntity
import com.example.data.model.ExtractionPreset
import com.example.data.model.SpreadsheetDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DocumentRepository(
    private val recordDao: ConversionRecordDao,
    private val templateDao: ExtractionTemplateDao
) {

    val allDocuments: Flow<List<SpreadsheetDocument>> = recordDao.getAllRecords().map { list ->
        list.map { entityToDocument(it) }
    }

    val favoriteDocuments: Flow<List<SpreadsheetDocument>> = recordDao.getFavoriteRecords().map { list ->
        list.map { entityToDocument(it) }
    }

    val allTemplates: Flow<List<ExtractionTemplateEntity>> = templateDao.getAllTemplates()

    suspend fun saveDocument(document: SpreadsheetDocument) {
        val entity = ConversionRecordEntity(
            id = document.id,
            fileName = document.fileName,
            sourcePages = document.sourcePages,
            presetId = document.presetUsed.id,
            sheetsJson = Converters.sheetsToJson(document.sheets),
            createdAt = document.createdAt,
            totalRows = document.totalRows,
            totalSheets = document.totalSheets,
            confidenceScore = document.confidenceScore,
            isFavorite = document.isFavorite,
            notes = document.notes
        )
        recordDao.insertRecord(entity)
    }

    suspend fun updateDocument(document: SpreadsheetDocument) {
        val entity = ConversionRecordEntity(
            id = document.id,
            fileName = document.fileName,
            sourcePages = document.sourcePages,
            presetId = document.presetUsed.id,
            sheetsJson = Converters.sheetsToJson(document.sheets),
            createdAt = document.createdAt,
            totalRows = document.totalRows,
            totalSheets = document.totalSheets,
            confidenceScore = document.confidenceScore,
            isFavorite = document.isFavorite,
            notes = document.notes
        )
        recordDao.updateRecord(entity)
    }

    suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        recordDao.setFavorite(id, isFavorite)
    }

    suspend fun deleteDocument(id: String) {
        recordDao.deleteRecordById(id)
    }

    suspend fun getDocumentById(id: String): SpreadsheetDocument? {
        val entity = recordDao.getRecordById(id) ?: return null
        return entityToDocument(entity)
    }

    suspend fun saveTemplate(title: String, description: String, targetColumns: String, customPrompt: String) {
        templateDao.insertTemplate(
            ExtractionTemplateEntity(
                title = title,
                description = description,
                targetColumns = targetColumns,
                customPrompt = customPrompt
            )
        )
    }

    suspend fun deleteTemplate(id: Long) {
        templateDao.deleteTemplateById(id)
    }

    private fun entityToDocument(entity: ConversionRecordEntity): SpreadsheetDocument {
        val preset = ExtractionPreset.values().firstOrNull { it.id == entity.presetId } ?: ExtractionPreset.AUTO_DETECT
        val sheets = Converters.sheetsFromJson(entity.sheetsJson)
        return SpreadsheetDocument(
            id = entity.id,
            fileName = entity.fileName,
            sourcePages = entity.sourcePages,
            sheets = sheets,
            createdAt = entity.createdAt,
            presetUsed = preset,
            notes = entity.notes,
            confidenceScore = entity.confidenceScore,
            isFavorite = entity.isFavorite
        )
    }
}
