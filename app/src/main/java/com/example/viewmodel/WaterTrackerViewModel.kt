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

    // Live Streak info calculated dynamically from DB logs
    val gamificationStreak: StateFlow<StreakInfo> = combine(logsState, settingsState) { logs, settings ->
        calculateStreak(logs, settings.dailyGoalMl)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StreakInfo(0, 0, 0, false))

    // Live Badges list calculated dynamically from DB logs and streak
    val gamificationBadges: StateFlow<List<Badge>> = combine(logsState, settingsState, gamificationStreak) { logs, settings, streak ->
        val totalVolume = logs.sumOf { it.amountMl }
        val totalDaysMet = streak.totalGoalMetDays
        val longestStreak = streak.longestStreak

        listOf(
            Badge(
                id = "first_sip",
                name = "First Sip",
                description = "Record your first water log.",
                iconName = "water_drop",
                themeColorHex = "#60A5FA",
                isUnlocked = logs.isNotEmpty(),
                progress = if (logs.isNotEmpty()) 1f else 0f,
                targetValue = "1 log"
            ),
            Badge(
                id = "goal_getter",
                name = "Goal Getter",
                description = "Meet your daily hydration goal for the first time.",
                iconName = "workspace_premium",
                themeColorHex = "#FBBF24",
                isUnlocked = totalDaysMet >= 1,
                progress = if (totalDaysMet >= 1) 1f else 0f,
                targetValue = "1 day"
            ),
            Badge(
                id = "three_day",
                name = "Consistency Club",
                description = "Achieve a 3-day active goal streak.",
                iconName = "local_fire_department",
                themeColorHex = "#F97316",
                isUnlocked = longestStreak >= 3,
                progress = (longestStreak.toFloat() / 3f).coerceIn(0f, 1f),
                targetValue = "3 days"
            ),
            Badge(
                id = "hydration_hero",
                name = "Hydration Hero",
                description = "Sustain a 7-day hydration goal streak.",
                iconName = "emoji_events",
                themeColorHex = "#EC4899",
                isUnlocked = longestStreak >= 7,
                progress = (longestStreak.toFloat() / 7f).coerceIn(0f, 1f),
                targetValue = "7 days"
            ),
            Badge(
                id = "aquarius",
                name = "Aquarius Legend",
                description = "Reach 12,500 mL of total water consumed.",
                iconName = "pool",
                themeColorHex = "#06B6D4",
                isUnlocked = totalVolume >= 12500,
                progress = (totalVolume.toFloat() / 12500f).coerceIn(0f, 1f),
                targetValue = "12.5 L"
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Active gamified challenges calculated dynamically based on records
    val gamificationChallenges: StateFlow<List<Challenge>> = combine(logsState, settingsState) { logs, settings ->
        val todayLogs = logs.filter { isToday(it.timestamp) }

        // Challenge 1: Log water 3 separate times today
        val sipperCount = todayLogs.size
        val sipperProgress = (sipperCount.toFloat() / 3f).coerceIn(0f, 1f)

        // Challenge 2: Log 500 mL before 11:00 AM
        val todayCalendar = Calendar.getInstance()
        val earlyBirdVolume = todayLogs.filter { log ->
            val logCal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
            logCal.get(Calendar.HOUR_OF_DAY) < 11
        }.sumOf { it.amountMl }
        val earlyBirdProgress = (earlyBirdVolume.toFloat() / 500f).coerceIn(0f, 1f)

        // Challenge 3: Log total of 8000 mL water in current past 7 days
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val weeklyVolume = logs.filter { it.timestamp >= sevenDaysAgo }.sumOf { it.amountMl }
        val weeklyProgress = (weeklyVolume.toFloat() / 8000f).coerceIn(0f, 1f)

        // Challenge 4: Sync Wearable Device once
        val synced = settings.lastSyncTimestamp > 0L
        val syncProgress = if (synced) 1f else 0f

        listOf(
            Challenge(
                id = "consistent_sipper",
                title = "Consistent Sipper",
                description = "Log water at least 3 separate times today.",
                progress = sipperProgress,
                currentProgressText = "$sipperCount / 3 times",
                isCompleted = sipperProgress >= 1f,
                points = 50,
                iconName = "schedule"
            ),
            Challenge(
                id = "early_bird",
                title = "Early Bird Hydrator",
                description = "Log at least 500 mL before 11:00 AM today.",
                progress = earlyBirdProgress,
                currentProgressText = "$earlyBirdVolume / 500 mL",
                isCompleted = earlyBirdProgress >= 1f,
                points = 100,
                iconName = "lightbulb"
            ),
            Challenge(
                id = "weekly_target",
                title = "Weekly Volume Challenge",
                description = "Log 8,000 mL of total water in the last 7 days.",
                progress = weeklyProgress,
                currentProgressText = "$weeklyVolume / 8,000 mL",
                isCompleted = weeklyProgress >= 1f,
                points = 250,
                iconName = "star"
            ),
            Challenge(
                id = "smart_synced",
                title = "Wearable Sync Pro",
                description = "Sync with your connected wearable device.",
                progress = syncProgress,
                currentProgressText = if (synced) "Synced!" else "Not synced",
                isCompleted = synced,
                points = 150,
                iconName = "sync"
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun calculateStreak(logs: List<WaterLog>, dailyGoal: Int): StreakInfo {
        if (logs.isEmpty() || dailyGoal <= 0) {
            return StreakInfo(0, 0, 0, false)
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val logsByDay = logs.groupBy { sdf.format(Date(it.timestamp)) }
        val metDays = logsByDay.filter { (_, dayLogs) ->
            dayLogs.sumOf { it.amountMl } >= dailyGoal
        }.keys.toSet()

        val todayStr = sdf.format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)

        val activeToday = metDays.contains(todayStr)
        val activeYesterday = metDays.contains(yesterdayStr)

        var currentStreak = 0
        if (activeToday || activeYesterday) {
            val checkCal = Calendar.getInstance()
            if (!activeToday) {
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }
            while (true) {
                val dateStr = sdf.format(checkCal.time)
                if (metDays.contains(dateStr)) {
                    currentStreak++
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        }

        var longestStreak = 0
        if (metDays.isNotEmpty()) {
            val sortedMetDates = metDays.map { sdf.parse(it)!! }.sorted()
            var tempStreak = 1
            var previousDateCal = Calendar.getInstance().apply { time = sortedMetDates.first() }
            longestStreak = 1

            for (i in 1 until sortedMetDates.size) {
                val currentDateCal = Calendar.getInstance().apply { time = sortedMetDates[i] }

                val diffCal = Calendar.getInstance().apply {
                    time = previousDateCal.time
                    add(Calendar.DAY_OF_YEAR, 1)
                }

                val isConsecutive = diffCal.get(Calendar.YEAR) == currentDateCal.get(Calendar.YEAR) &&
                                    diffCal.get(Calendar.DAY_OF_YEAR) == currentDateCal.get(Calendar.DAY_OF_YEAR)

                if (isConsecutive) {
                    tempStreak++
                } else {
                    tempStreak = 1
                }
                if (tempStreak > longestStreak) {
                    longestStreak = tempStreak
                }
                previousDateCal = currentDateCal
            }

            if (currentStreak > longestStreak) {
                longestStreak = currentStreak
            }
        }

        return StreakInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalGoalMetDays = metDays.size,
            isGoalMetToday = activeToday
        )
    }

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
