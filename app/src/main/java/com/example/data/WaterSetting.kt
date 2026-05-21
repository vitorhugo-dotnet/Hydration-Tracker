package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_settings")
data class WaterSetting(
    @PrimaryKey val id: Int = 1,
    val dailyGoalMl: Int = 2000,
    val remindersEnabled: Boolean = true,
    val reminderIntervalMinutes: Int = 120,
    val wakeTime: String = "08:00",
    val sleepTime: String = "22:00",
    val weightKg: Int = 70,
    val wearableConnected: Boolean = false,
    val wearableService: String = "Fitbit", // Fitbit, Garmin, or Wear OS
    val wearableToken: String = "",
    val lastSyncTimestamp: Long = 0L
)
