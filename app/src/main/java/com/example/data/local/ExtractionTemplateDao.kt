package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtractionTemplateDao {

    @Query("SELECT * FROM extraction_templates ORDER BY createdAt DESC")
    fun getAllTemplates(): Flow<List<ExtractionTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: ExtractionTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: ExtractionTemplateEntity)

    @Query("DELETE FROM extraction_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Long)
}
