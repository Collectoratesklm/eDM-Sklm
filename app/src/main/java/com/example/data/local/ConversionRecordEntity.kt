package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_records")
data class ConversionRecordEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val sourcePages: Int,
    val presetId: String,
    val sheetsJson: String,
    val createdAt: Long,
    val totalRows: Int,
    val totalSheets: Int,
    val confidenceScore: Float,
    val isFavorite: Boolean = false,
    val thumbnailPath: String? = null,
    val notes: String = ""
)
