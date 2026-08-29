package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extraction_templates")
data class ExtractionTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val targetColumns: String, // Comma-separated or JSON list
    val customPrompt: String,
    val createdAt: Long = System.currentTimeMillis()
)
