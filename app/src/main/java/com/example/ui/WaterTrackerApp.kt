package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.WaterLog
import com.example.data.WaterSetting
import com.example.ui.theme.*
import com.example.viewmodel.WaterTrackerViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

// Main Layout Wrapper supporting Dynamic Adapative Screen sizing
@Composable
fun WaterTrackerApp(
    viewModel: WaterTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val logs by viewModel.logsState.collectAsStateWithLifecycle()
    val todayIntake by viewModel.todayIntake.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600
    val isDarkTheme = isSystemInDarkTheme()

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WaterDrop,
                                contentDescription = "Water drop logo",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "HydroFlow",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.triggerWearableSync() },
                            enabled = !viewModel.isSyncing.collectAsStateWithLifecycle().value,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (viewModel.isSyncing.collectAsStateWithLifecycle().value) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Sync,
                                    contentDescription = "Sync",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        // Gradient decorative/profile circle from HTML
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF60A5FA), Color(0xFF818CF8))
                                    )
                                )
                        )
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 1.dp
                )
            }
        },
        bottomBar = {
            if (!isWideScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        icon = { Icon(if (currentTab == 0) Icons.Filled.LocalDrink else Icons.Outlined.LocalDrink, "Logs") },
                        label = { Text("Tracker") },
                        modifier = Modifier.testTag("nav_tab_tracker")
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        icon = { Icon(if (currentTab == 1) Icons.Filled.BarChart else Icons.Outlined.BarChart, "Insights") },
                        label = { Text("Insights") },
                        modifier = Modifier.testTag("nav_tab_analytics")
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { viewModel.selectTab(2) },
                        icon = { Icon(if (currentTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings, "Config") },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isWideScreen) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight().testTag("adaptive_nav_rail")
                ) {
                    Spacer(Modifier.height(32.dp))
                    Icon(
                        imageVector = Icons.Filled.WaterDrop,
                        contentDescription = "Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(32.dp))
                    NavigationRailItem(
                        selected = currentTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        icon = { Icon(if (currentTab == 0) Icons.Filled.LocalDrink else Icons.Outlined.LocalDrink, "Tracker") },
                        label = { Text("Tracker") },
                        modifier = Modifier.testTag("nav_rail_tracker")
                    )
                    NavigationRailItem(
                        selected = currentTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        icon = { Icon(if (currentTab == 1) Icons.Filled.BarChart else Icons.Outlined.BarChart, "Analytics") },
                        label = { Text("Analytics") },
                        modifier = Modifier.testTag("nav_rail_analytics")
                    )
                    NavigationRailItem(
                        selected = currentTab == 2,
                        onClick = { viewModel.selectTab(2) },
                        icon = { Icon(if (currentTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings, "Settings") },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_rail_settings")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isDarkTheme) {
                                listOf(PolishBackgroundDark, PolishBackgroundDark.copy(alpha = 0.93f))
                            } else {
                                listOf(PolishBackground, Color(0xFFF3EDF7))
                            }
                        )
                    )
            ) {
                when (currentTab) {
                    0 -> TrackerScreen(
                        todayIntake = todayIntake,
                        dailyGoal = settings.dailyGoalMl,
                        logs = logs,
                        onAddLog = { viewModel.addLog(it) },
                        onDeleteLog = { viewModel.deleteLog(it) }
                    )
                    1 -> AnalyticsScreen(
                        logs = logs,
                        dailyGoal = settings.dailyGoalMl
                    )
                    2 -> SettingsScreen(
                        settings = settings,
                        logs = logs,
                        onSaveSettings = { goal, alertEnabled, alertInterval, wake, sleep, weight, isConn, brand, token ->
                            viewModel.updateSettings(goal, alertEnabled, alertInterval, wake, sleep, weight, isConn, brand, token)
                        },
                        onTriggerSync = { viewModel.triggerWearableSync() },
                        isSyncing = viewModel.isSyncing.collectAsStateWithLifecycle().value,
                        syncMessage = viewModel.syncMessage.collectAsStateWithLifecycle().value,
                        steps = viewModel.syncedSteps.collectAsStateWithLifecycle().value,
                        calories = viewModel.syncedCalories.collectAsStateWithLifecycle().value,
                        onClearLogs = { viewModel.clearAllLogs() }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 0: TRACKER SCREEN (Circular liquid wave + Quick Log list)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    todayIntake: Int,
    dailyGoal: Int,
    logs: List<WaterLog>,
    onAddLog: (Int) -> Unit,
    onDeleteLog: (Int) -> Unit
) {
    var showCustomLogDialog by remember { mutableStateOf(false) }
    var customVolumeStr by remember { mutableStateOf("") }
    val todayLogs = remember(logs) {
        logs.filter { isToday(it.timestamp) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 28.dp, bottom = 40.dp)
    ) {
        item {
            // Header
            val localHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when (localHour) {
                in 5..11 -> "Good Morning! ☀️"
                in 12..17 -> "Good Afternoon! 🥤"
                else -> "Good Evening! 🌙"
            }
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.testTag("home_greeting")
            )
            Text(
                text = "Keep Hydrated Today",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            // Circular fluid wave animator
            val hydrationFraction = if (dailyGoal > 0) todayIntake.toFloat() / dailyGoal else 0f
            FluidProgressRing(progress = hydrationFraction)
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            // Volume Metrics display card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Logged",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "$todayIntake mL",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Daily Target",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "$dailyGoal mL",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            // Quick log grid docks header
            Text(
                text = "Quick Hydration Logs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            // Quick log selectors Dock
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                QuickLogButton(label = "Cup", amount = 150, icon = Icons.Filled.LocalCafe, onClick = { onAddLog(150) })
                QuickLogButton(label = "Glass", amount = 250, icon = Icons.Filled.LocalDrink, onClick = { onAddLog(250) })
                QuickLogButton(label = "Flask", amount = 500, icon = Icons.Filled.Layers, onClick = { onAddLog(500) })
                
                // Custom quick log form expansion activator
                Column(
                    modifier = Modifier
                        .testTag("quick_log_custom")
                        .clickable { showCustomLogDialog = true }
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Custom Log",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Custom",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }

        item {
            Text(
                text = "Today's Logs Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (todayLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SentimentDissatisfied,
                            contentDescription = "Empty logs",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No water logs recorded yet today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Tap any quick cup selector above to record intake!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        } else {
            items(todayLogs) { log ->
                TodayLogItem(log = log, onDelete = { onDeleteLog(log.id) })
            }
        }
    }

    // Modal popup to support exact custom ml input
    if (showCustomLogDialog) {
        AlertDialog(
            onDismissRequest = {
                showCustomLogDialog = false
                customVolumeStr = ""
            },
            title = { Text("Log Custom Water Intake 💧") },
            text = {
                Column {
                    Text(
                        "Enter the custom amount of logged liquid in milliliters (mL):",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = customVolumeStr,
                        onValueChange = { customVolumeStr = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Amount (mL)") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_ml_field"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    modifier = Modifier.testTag("custom_ml_submit_btn"),
                    onClick = {
                        val amount = customVolumeStr.toIntOrNull()
                        if (amount != null && amount > 0) {
                            onAddLog(amount)
                        }
                        showCustomLogDialog = false
                        customVolumeStr = ""
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCustomLogDialog = false
                    customVolumeStr = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun QuickLogButton(
    label: String,
    amount: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .testTag("quick_log_$amount")
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .padding(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "+$amount mL",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun TodayLogItem(
    log: WaterLog,
    onDelete: () -> Unit
) {
    val logTime = remember(log.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("log_item_${log.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WaterDrop,
                        contentDescription = "Water item",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "+${log.amountMl} mL",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Logged at $logTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_log_btn_${log.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete entry",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun FluidProgressRing(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_oscillation")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .padding(12.dp)
                .testTag("circular_fluid_progress")
        ) {
            val radius = size.width / 2f
            val center = size.center

            // 1. Trace hollow circle
            drawCircle(
                color = containerColor.copy(alpha = 0.25f),
                radius = radius,
                center = center,
                style = Stroke(width = 10.dp.toPx())
            )

            // 2. Liquid Wave bounds
            val clipPath = Path().apply {
                val r = radius - 5.dp.toPx()
                addOval(androidx.compose.ui.geometry.Rect(center.x - r, center.y - r, center.x + r, center.y + r))
            }

            clipPath(clipPath) {
                val clampedProgress = progress.coerceIn(0f, 1f)
                val fillHeight = (radius * 2f) * clampedProgress
                val yBase = center.y + radius - fillHeight

                // Draw Wave path 1
                val wavePath = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(0f, yBase)

                    val points = 20
                    val stepX = size.width / points
                    for (i in 0..points) {
                        val x = i * stepX
                        val angle = (x / size.width) * (2 * Math.PI.toFloat()) * 1.5f + wavePhase
                        val yOffset = sin(angle.toDouble()).toFloat() * 7.dp.toPx()
                        lineTo(x, yBase + yOffset)
                    }
                    lineTo(size.width, size.height)
                    close()
                }

                drawPath(
                    path = wavePath,
                    color = primaryColor.copy(alpha = 0.60f)
                )

                // Draw Wave path 2 (depth overlay)
                val wavePath2 = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(0f, yBase)

                    val points = 20
                    val stepX = size.width / points
                    for (i in 0..points) {
                        val x = i * stepX
                        val angle = (x / size.width) * (2 * Math.PI.toFloat()) * 1.5f + wavePhase + Math.PI.toFloat() * 0.5f
                        val yOffset = sin(angle.toDouble()).toFloat() * 5.dp.toPx()
                        lineTo(x, yBase + yOffset - 3.dp.toPx())
                    }
                    lineTo(size.width, size.height)
                    close()
                }

                drawPath(
                    path = wavePath2,
                    color = primaryColor.copy(alpha = 0.30f)
                )
            }

            // 3. Dynamic outer stroke path arcs
            val arcSize = radius * 2
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = (progress * 360f).coerceIn(0f, 360f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Hydrated",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

// -------------------------------------------------------------
// TAB 1: ANALYTICS / GRAPHICAL INSIGHTS SCREEN
// -------------------------------------------------------------
@Composable
fun AnalyticsScreen(
    logs: List<WaterLog>,
    dailyGoal: Int
) {
    val weeklyTotals = remember(logs) { getWeeklyAverages(logs) }
    
    val totalIntakeLiters = remember(logs) {
        val totalMl = logs.sumOf { it.amountMl }
        totalMl.toFloat() / 1000f
    }

    val dailyAverage = remember(weeklyTotals) {
        val totals = weeklyTotals.map { it.second }.filter { it > 0 }
        if (totals.isEmpty()) 0 else totals.average().toInt()
    }

    val activeStreak = remember(logs, dailyGoal) {
        calculateStreak(logs, dailyGoal)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 40.dp)
    ) {
        item {
            Text(
                text = "Hydration Analytics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag("analytics_title")
            )
            Text(
                text = "Track your long-term hydration habits",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            // High level Metrics Summary widgets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSummaryCard(
                    title = "Streak",
                    value = "🔥 $activeStreak Days",
                    subtitle = "Consecutive goals",
                    color = Color(0xFFF97316),
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Avg Intake",
                    value = "$dailyAverage mL",
                    subtitle = "Weekly average",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSummaryCard(
                    title = "Lifetime Logs",
                    value = "${String.format(Locale.getDefault(), "%.2f", totalIntakeLiters)} L",
                    subtitle = "Total volume tracked",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Completion",
                    value = "${if (weeklyTotals.isNotEmpty()) (weeklyTotals.filter { it.second >= dailyGoal }.size * 100 / 7) else 0}%",
                    subtitle = "Met goals past 7d",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            // Interactive custom Bar Chart
            HydrationBarChart(weeklyTotals = weeklyTotals, dailyGoal = dailyGoal)
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            // Personal Health Insights Panel
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = "Insight",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Smart Hydration Coach",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val insightContent = when {
                        activeStreak >= 3 -> "Phenomenal consistency! You are on a $activeStreak-day streak. Your body functions best when steadily supplied. Keep this active momentum!"
                        dailyAverage == 0 -> "Let's kickstart your hydration journey! Keep your drinking target near your work desk. Sips taken every 30 minutes reinforce standard metabolic health."
                        dailyAverage < dailyGoal -> "You average $dailyAverage mL which is currently under your daily $dailyGoal mL goal. Try drinking 250mL immediately upon waking up to jumpstart metabolic recovery."
                        else -> "Excellent hydration habit! You are hitting your objectives. Logging water consistently prevents mental fatigue and maintains physical endurance."
                    }

                    Text(
                        text = insightContent,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "💡 Health Fact: Mild dehydration of just 1-2% can significantly decrease cognitive alertness, memory sharpness, and physical stamina.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun MetricSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun HydrationBarChart(
    weeklyTotals: List<Pair<String, Int>>,
    dailyGoal: Int,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Last 7 Days Consumption",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 4.dp)
            ) {
                val maxVolume = (weeklyTotals.maxOfOrNull { it.second } ?: dailyGoal).coerceAtLeast(dailyGoal).coerceAtLeast(1)

                weeklyTotals.forEach { (day, amount) ->
                    val progressFraction = amount.toFloat() / maxVolume

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier
                                .weight(1f)
                                .width(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            // Render fill
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(progressFraction)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = if (amount >= dailyGoal) {
                                                listOf(Color(0xFF10B981), Color(0xFF34D399))
                                            } else {
                                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                            }
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: CONFIGURATION, WEARABLES SYNC, EXPORTS
// -------------------------------------------------------------
@Composable
fun SettingsScreen(
    settings: WaterSetting,
    logs: List<WaterLog>,
    onSaveSettings: (Int, Boolean, Int, String, String, Int, Boolean, String, String) -> Unit,
    onTriggerSync: () -> Unit,
    isSyncing: Boolean,
    syncMessage: String,
    steps: Int,
    calories: Int,
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current

    // Local controller states initialized from persistent entities
    var dailyGoalStr by remember(settings) { mutableStateOf(settings.dailyGoalMl.toString()) }
    var remindersEnabled by remember(settings) { mutableStateOf(settings.remindersEnabled) }
    var reminderIntervalMins by remember(settings) { mutableStateOf(settings.reminderIntervalMinutes) }
    var wakeTime by remember(settings) { mutableStateOf(settings.wakeTime) }
    var sleepTime by remember(settings) { mutableStateOf(settings.sleepTime) }
    var weightStr by remember(settings) { mutableStateOf(settings.weightKg.toString()) }
    
    var wearableBrand by remember(settings) { mutableStateOf(settings.wearableService) }
    var wearableToken by remember(settings) { mutableStateOf(settings.wearableToken) }

    var isEditModified by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 40.dp)
    ) {
        item {
            Text(
                text = "System Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag("settings_title")
            )
            Text(
                text = "Customize goals, reminders, and wearable links",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Section: Personal Biological Profile
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Accessibility, "Body", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Biological & Hydration Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = weightStr,
                            onValueChange = {
                                weightStr = it
                                isEditModified = true
                                // Auto calculate goal: 35ml per kg of muscle/mass!
                                val computedWeight = it.toIntOrNull() ?: 70
                                val recommendedGoal = computedWeight * 35
                                dailyGoalStr = recommendedGoal.toString()
                            },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("weight_input_field"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = dailyGoalStr,
                            onValueChange = {
                                dailyGoalStr = it
                                isEditModified = true
                            },
                            label = { Text("Goal (mL)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("goal_input_field"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 Metabolic Tip: PureFlow automatically calculates a standard target of 35mL water per kilogram. Adjust manual targets if physically active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Section: Personalized Smart Notifications
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.NotificationsActive, "Alarm", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Personalized Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = {
                                remindersEnabled = it
                                isEditModified = true
                            },
                            modifier = Modifier.testTag("reminders_switch")
                        )
                    }

                    if (remindersEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Reminder Interval",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(60, 90, 120, 180).forEach { mins ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (reminderIntervalMins == mins) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        )
                                        .clickable {
                                            reminderIntervalMins = mins
                                            isEditModified = true
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        color = if (reminderIntervalMins == mins) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = wakeTime,
                                onValueChange = {
                                    wakeTime = it
                                    isEditModified = true
                                },
                                label = { Text("Wake Time") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = sleepTime,
                                onValueChange = {
                                    sleepTime = it
                                    isEditModified = true
                                },
                                label = { Text("Bed Time") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }

        // Section: Wearable integration (Fitbit, Garmin, Wear OS)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Watch, "Wearable", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Wearable Integrations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Select Wearable Platform", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Fitbit", "Garmin", "Wear OS").forEach { service ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (wearableBrand == service) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                    )
                                    .clickable {
                                        wearableBrand = service
                                        isEditModified = true
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = service,
                                    color = if (wearableBrand == service) MaterialTheme.colorScheme.onSecondary
                                    else MaterialTheme.colorScheme.onSecondaryContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = wearableToken,
                        onValueChange = {
                            wearableToken = it
                            isEditModified = true
                        },
                        label = { Text("Developer Access Token") },
                        placeholder = { Text("Optional REST credential") },
                        modifier = Modifier.fillMaxWidth().testTag("wearable_token_field"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (settings.wearableConnected) {
                        // Visual Synced metrics display
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("👟 Synced Steps", style = MaterialTheme.typography.labelMedium)
                                    Text("${String.format(Locale.getDefault(), "%,d", steps)} Steps", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("🔥 Active Energy", style = MaterialTheme.typography.labelMedium)
                                    Text("$calories kcal", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onTriggerSync,
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f).testTag("sync_device_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                            } else {
                                Row {
                                    Icon(Icons.Filled.Sync, "Sync", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync Now")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = syncMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Section: Save controller modifications
        if (isEditModified) {
            item {
                Button(
                    onClick = {
                        val computedGoal = dailyGoalStr.toIntOrNull() ?: 2000
                        val computedWeight = weightStr.toIntOrNull() ?: 70
                        onSaveSettings(
                            computedGoal,
                            remindersEnabled,
                            reminderIntervalMins,
                            wakeTime,
                            sleepTime,
                            computedWeight,
                            settings.wearableConnected,
                            wearableBrand,
                            wearableToken
                        )
                        isEditModified = false
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).testTag("save_settings_btn")
                ) {
                    Text("Apply & Save Configurations 💾")
                }
            }
        }

        // Section: Data Export Health Reports
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.IosShare, "Export", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Export Health Metadata", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Export a formal comma-delimited CSV log record (health diary), perfect for diagnostic medical reports or wellness notes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth().testTag("export_csv_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        onClick = {
                            exportHealthReport(context, logs, settings.dailyGoalMl)
                        }
                    ) {
                        Icon(Icons.Filled.Description, "File")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share CSV Health Report 📊")
                    }
                }
            }
        }

        // Reset system history option
        item {
            TextButton(
                onClick = onClearLogs,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag("reset_logs_btn")
            ) {
                Icon(Icons.Filled.Refresh, "Reset")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Tracking History", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// INTERNAL AUXILIARY DATE & STREAK FORMULAS
// -------------------------------------------------------------
private fun isToday(timestamp: Long): Boolean {
    val logCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val todayCal = Calendar.getInstance()
    return logCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
           logCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
}

private fun getWeeklyAverages(logs: List<WaterLog>): List<Pair<String, Int>> {
    val calendarMap = LinkedHashMap<String, Int>()
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dayPairList = mutableListOf<Calendar>()
    
    // Seed last 7 days (including today)
    for (i in 0..6) {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
        dayPairList.add(cal)
    }
    dayPairList.reverse()

    dayPairList.forEach { cal ->
        val dayName = dayFormat.format(cal.time)
        calendarMap[dayName] = 0
    }

    logs.forEach { log ->
        val logCal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
        dayPairList.forEach { cal ->
            if (logCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                logCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)) {
                val dayName = dayFormat.format(cal.time)
                calendarMap[dayName] = (calendarMap[dayName] ?: 0) + log.amountMl
            }
        }
    }

    return calendarMap.toList()
}

private fun calculateStreak(logs: List<WaterLog>, dailyGoal: Int): Int {
    if (logs.isEmpty() || dailyGoal <= 0) return 0

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dailyConsumptions = LinkedHashMap<String, Int>()
    
    logs.forEach { log ->
        val dayStr = sdf.format(Date(log.timestamp))
        dailyConsumptions[dayStr] = (dailyConsumptions[dayStr] ?: 0) + log.amountMl
    }

    var streak = 0
    val checkCal = Calendar.getInstance()

    while (true) {
        val checkDayStr = sdf.format(checkCal.time)
        val consumed = dailyConsumptions[checkDayStr] ?: 0
        if (consumed >= dailyGoal) {
            streak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            // If the day is today and they haven't achieved goal, streak might still count yesterday's achievements
            if (streak == 0 && sdf.format(Date()) == checkDayStr) {
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
                continue
            }
            break
        }
    }

    return streak
}

private fun exportHealthReport(context: Context, logs: List<WaterLog>, goalMl: Int) {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val csvContent = StringBuilder().apply {
        append("Log ID,Date & Time,Amount (mL),Daily Goal (mL),Status\n")
        logs.forEach { log ->
            val dateStr = sdf.format(Date(log.timestamp))
            val status = if (log.amountMl >= goalMl) "Goal Achieved" else "Log Entry"
            append("${log.id},$dateStr,${log.amountMl},$goalMl,$status\n")
        }
    }.toString()

    try {
        val file = File(context.cacheDir, "hydration_health_report.csv")
        file.writeText(csvContent)

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Hydration Progress Report")
            putExtra(Intent.EXTRA_TEXT, "Attached is my Hydration health history and progress report.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val shareIntent = Intent.createChooser(sendIntent, "Export Health Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
