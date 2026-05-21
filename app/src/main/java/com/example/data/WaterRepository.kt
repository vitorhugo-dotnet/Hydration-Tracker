package com.example.data

import kotlinx.coroutines.flow.Flow

class WaterRepository(private val waterDao: WaterDao) {
    val allLogs: Flow<List<WaterLog>> = waterDao.getAllLogs()
    val settingsFlow: Flow<WaterSetting?> = waterDao.getSettingsFlow()

    suspend fun getSettings(): WaterSetting {
        return waterDao.getSettings() ?: WaterSetting().also {
            waterDao.upsertSettings(it)
        }
    }

    suspend fun saveSettings(settings: WaterSetting) {
        waterDao.upsertSettings(settings)
    }

    suspend fun insertLog(amountMl: Int, timestamp: Long = System.currentTimeMillis()) {
        waterDao.insertLog(WaterLog(amountMl = amountMl, timestamp = timestamp))
    }

    suspend fun deleteLog(log: WaterLog) {
        waterDao.deleteLog(log)
    }

    suspend fun deleteLogById(logId: Int) {
        waterDao.deleteLogById(logId)
    }

    suspend fun clearHistory() {
        waterDao.clearAllLogs()
    }
}
