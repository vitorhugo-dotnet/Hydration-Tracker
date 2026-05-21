package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String, // String representation so it passes across cleanly
    val themeColorHex: String, // Hex shade for beautiful Material 3 styled rings
    val isUnlocked: Boolean,
    val progress: Float = 0f,
    val targetValue: String = ""
)

data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalGoalMetDays: Int,
    val isGoalMetToday: Boolean
)

data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val progress: Float, // 0f to 1f
    val currentProgressText: String,
    val isCompleted: Boolean,
    val points: Int,
    val iconName: String
)
