package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversionRecordDao {

    @Query("SELECT * FROM conversion_records ORDER BY createdAt DESC")
    fun getAllRecords(): Flow<List<ConversionRecordEntity>>

    @Query("SELECT * FROM conversion_records WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteRecords(): Flow<List<ConversionRecordEntity>>

    @Query("SELECT * FROM conversion_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: String): ConversionRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ConversionRecordEntity)

    @Update
    suspend fun updateRecord(record: ConversionRecordEntity)

    @Query("UPDATE conversion_records SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE conversion_records SET sheetsJson = :sheetsJson, totalRows = :totalRows WHERE id = :id")
    suspend fun updateSheetsData(id: String, sheetsJson: String, totalRows: Int)

    @Query("DELETE FROM conversion_records WHERE id = :id")
    suspend fun deleteRecordById(id: String)

    @Query("DELETE FROM conversion_records")
    suspend fun clearAllRecords()
}
