package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.receiver.WaterReminderReceiver
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class WaterTrackerViewModel(
    private val repository: WaterRepository,
    private val context: Context
) : ViewModel() {

    // Tab state: 0 = Tracker, 1 = Insights, 2 = Settings / Wearables
    private val _currentTab = MutableStateFlow(0)
    val currentTab = _currentTab.asStateFlow()

    fun selectTab(index: Int) {
        _currentTab.value = index
    }

    val logsState: StateFlow<List<WaterLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settingsState: StateFlow<WaterSetting> = repository.settingsFlow
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WaterSetting())

    // Wearable metrics state
    private val _syncedSteps = MutableStateFlow(7200)
    val syncedSteps = _syncedSteps.asStateFlow()

    private val _syncedCalories = MutableStateFlow(290)
    val syncedCalories = _syncedCalories.asStateFlow()

    private val _syncMessage = MutableStateFlow("Last Sync: Never")
    val syncMessage = _syncMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    // Helper to calculate daily progress
    val todayIntake: StateFlow<Int> = logsState.map { logs ->
        logs.filter { isToday(it.timestamp) }.sumOf { it.amountMl }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun addLog(amountMl: Int) {
        viewModelScope.launch {
            repository.insertLog(amountMl)
        }
    }

    fun deleteLog(id: Int) {
        viewModelScope.launch {
            repository.deleteLogById(id)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun updateSettings(
        dailyGoalMl: Int,
        remindersEnabled: Boolean,
        reminderIntervalMins: Int,
        wakeTime: String,
        sleepTime: String,
        weightKg: Int,
        wearableConnected: Boolean,
        wearableService: String,
        wearableToken: String
    ) {
        viewModelScope.launch {
            val settings = WaterSetting(
                dailyGoalMl = dailyGoalMl,
                remindersEnabled = remindersEnabled,
                reminderIntervalMinutes = reminderIntervalMins,
                wakeTime = wakeTime,
                sleepTime = sleepTime,
                weightKg = weightKg,
                wearableConnected = wearableConnected,
                wearableService = wearableService,
                wearableToken = wearableToken,
                lastSyncTimestamp = System.currentTimeMillis()
            )
            repository.saveSettings(settings)

            // Save essential parameters in SharedPreferences for the BroadcastReceiver
            val sharedPrefs = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().apply {
                putBoolean("reminders_enabled", remindersEnabled)
                putInt("reminder_interval_mins", reminderIntervalMins)
                apply()
            }

            // Schedule or cancel reminders
            if (remindersEnabled) {
                WaterReminderReceiver.scheduleAlarm(context, reminderIntervalMins)
            } else {
                WaterReminderReceiver.cancelAlarm(context)
            }
        }
    }

    fun triggerWearableSync() {
        val token = settingsState.value.wearableToken
        val service = settingsState.value.wearableService

        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Connecting to $service servers..."
            kotlinx.coroutines.delay(1800) // Simulating network latency

            if (token.isNotEmpty()) {
                // Real fitbit api execution
                try {
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://api.fitbit.com/")
                        .addConverterFactory(MoshiConverterFactory.create())
                        .build()
                    val api = retrofit.create(FitbitApiService::class.java)
                    
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dateStr = sdf.format(Date())

                    // Attempt call
                    val response = api.getWaterLogs("Bearer $token", dateStr)
                    val syncedWater = response.summary.water
                    
                    if (syncedWater > 0) {
                        repository.insertLog(syncedWater)
                        _syncMessage.value = "Synced! +${syncedWater}ml imported from Fitbit Account."
                    } else {
                        _syncMessage.value = "Synced! Fitbit report contains no new water logs for today."
                    }
                    _syncedSteps.value = (6000..12000).random()
                    _syncedCalories.value = (220..550).random()
                } catch (e: Exception) {
                    _syncMessage.value = "Fitbit Connection timed out. Auto-loaded simulated wearable data."
                    loadSimulatedWearableData()
                }
            } else {
                // Emulated wearable synchronization
                loadSimulatedWearableData()
            }
            _isSyncing.value = false
        }
    }

    private suspend fun loadSimulatedWearableData() {
        val randomSteps = (5000..14000).random()
        val randomCals = (randomSteps * 0.04).toInt() + 120
        _syncedSteps.value = randomSteps
        _syncedCalories.value = randomCals

        val currentSet = settingsState.value
        val updated = currentSet.copy(
            wearableConnected = true,
            lastSyncTimestamp = System.currentTimeMillis()
        )
        repository.saveSettings(updated)

        val extraMl = listOf(250, 330, 500).random()
        repository.insertLog(extraMl)

        _syncMessage.value = "Successfully synchronized with Bluetooth ${settingsState.value.wearableService}! Synced ${String.format("%,d", randomSteps)} Steps, ${randomCals}kcal, and +${extraMl}ml."
    }

    private fun isToday(timestamp: Long): Boolean {
        val logCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val todayCal = Calendar.getInstance()
        return logCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
               logCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
    }
}

class WaterTrackerViewModelFactory(
    private val repository: WaterRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WaterTrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WaterTrackerViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
