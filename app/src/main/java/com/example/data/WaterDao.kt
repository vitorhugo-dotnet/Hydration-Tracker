package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<WaterLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WaterLog)

    @Delete
    suspend fun deleteLog(log: WaterLog)

    @Query("DELETE FROM water_logs WHERE id = :logId")
    suspend fun deleteLogById(logId: Int)

    @Query("DELETE FROM water_logs")
    suspend fun clearAllLogs()

    @Query("SELECT * FROM water_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<WaterSetting?>

    @Query("SELECT * FROM water_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): WaterSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: WaterSetting)
}
